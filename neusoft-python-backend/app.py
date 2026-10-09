from flask import Flask, request, jsonify, send_file, send_from_directory
from flask_cors import CORS
import os
import io
import base64
import uuid
import shutil
import tempfile
import zipfile
import threading
import json
import traceback
from pathlib import Path

import numpy as np
import SimpleITK as sitk
import pydicom
from skimage import measure
from PIL import Image

import dicom2nifti
from totalsegmentator.python_api import totalsegmentator

# ------------------------------------------------------
#  BRAIN / BRATS SERVICES IMPORTS
# ------------------------------------------------------
from services.mesh import seg_to_glb
from services.storage import ensure_case_dirs, get_case_dir
from services.unzip_validate import unzip_and_validate_modalities
from services.monai_runner import run_brats_bundle_inference
from services.preview import generate_previews_t1ce_all_planes

# ------------------------------------------------------
#  SPLEEN / CT-ICH SERVICES IMPORTS
#  (from spleen-ct-app-backend/services/)
# ------------------------------------------------------
from spleen_services.storage import (
    ensure_case_dirs as spleen_ensure_case_dirs,
    get_case_dir as spleen_get_case_dir,
)
from spleen_services.ct_input import save_single_ct_from_upload
from spleen_services.inference import (
    run_ct_ich_inference,
    generate_ct_previews,
    mask_to_mesh,
)

# Try nibabel for NIfTI
try:
    import nibabel as nib
    HAVE_NIB = True
except Exception:
    HAVE_NIB = False

# Try torch for GPU detection
try:
    import torch
    HAS_TORCH = True
except ImportError:
    HAS_TORCH = False

# ------------------------------------------------------
#  FLASK APP
# ------------------------------------------------------
app = Flask(__name__)
CORS(app)

# Limit upload size: 2 GB
app.config["MAX_CONTENT_LENGTH"] = 2 * 1024 * 1024 * 1024

# ------------------------------------------------------
#  CONFIG: SHARED PATHS
# ------------------------------------------------------
BASE_DIR = Path(__file__).resolve().parent
BASE_UPLOAD_DIR = str(Path(os.environ.get("PACS_UPLOAD_DIR", str(BASE_DIR / "upload"))).resolve())
UPLOAD_FOLDER   = BASE_UPLOAD_DIR
UPLOAD_DIR      = BASE_UPLOAD_DIR
RESULT_BASE_DIR = os.path.join(BASE_UPLOAD_DIR, "result")

BASE_DIR    = Path(__file__).resolve().parent
CASES_DIR   = Path(os.environ.get("PACS_CASES_DIR", str(BASE_DIR / "cases"))).resolve()
BUNDLE_DIR  = Path(os.environ.get("PACS_BRATS_BUNDLE_DIR", str(BASE_DIR / "bundles" / "brats_mri_segmentation"))).resolve()

# Spleen cases live in a sub-workflow folder: cases/ct-spleen/<case_id>/
SPLEEN_CASES_DIR = CASES_DIR  # spleen storage uses workflow sub-folder internally

# Ensure base dirs exist
os.makedirs(UPLOAD_FOLDER,   exist_ok=True)
os.makedirs(RESULT_BASE_DIR, exist_ok=True)
os.makedirs(CASES_DIR,       exist_ok=True)


# ------------------------------------------------------
#  GLOBAL CORS
# ------------------------------------------------------
@app.after_request
def apply_cors(resp):
    resp.headers["Access-Control-Allow-Origin"]  = "*"
    resp.headers["Access-Control-Allow-Headers"] = "Content-Type, Authorization"
    resp.headers["Access-Control-Allow-Methods"] = "GET, POST, OPTIONS"
    return resp


# ==============================================================================
#  BASIC UTILITIES  (CT windowing / slice helpers)
# ==============================================================================

def window_image(img, ww=400, wl=40):
    img   = img.astype(np.float32)
    low   = wl - ww / 2.0
    high  = wl + ww / 2.0
    img   = np.clip(img, low, high)
    img   = (img - low) / (high - low + 1e-6)
    return (img * 255.0).astype(np.uint8)


def slice_to_png_base64(slice_2d, ww=400, wl=40):
    arr     = window_image(slice_2d, ww, wl)
    pil_img = Image.fromarray(arr)
    buf     = io.BytesIO()
    pil_img.save(buf, format="PNG")
    return base64.b64encode(buf.getvalue()).decode("utf-8")


def mask_to_overlay_png_base64(mask_2d):
    mask = (mask_2d > 0).astype(np.uint8)
    h, w = mask.shape
    rgba = np.zeros((h, w, 4), dtype=np.uint8)
    rgba[mask > 0, 1] = 255   # green channel
    rgba[mask > 0, 3] = 160   # alpha
    pil_img = Image.fromarray(rgba, mode="RGBA")
    buf = io.BytesIO()
    pil_img.save(buf, format="PNG")
    return base64.b64encode(buf.getvalue()).decode("utf-8")


# ==============================================================================
#  LOADERS
# ==============================================================================

def load_dicom_series(folder):
    reader = sitk.ImageSeriesReader()
    files  = reader.GetGDCMSeriesFileNames(folder)
    if len(files) == 0:
        raise ValueError(f"No DICOM series found in: {folder}")
    reader.SetFileNames(files)
    img = reader.Execute()
    return sitk.GetArrayFromImage(img).astype(np.float32)


def load_ct_volume(path):
    if not os.path.exists(path):
        raise FileNotFoundError(f"CT path does not exist: {path}")

    if os.path.isdir(path):
        files      = os.listdir(path)
        npy_files  = [f for f in files if f.lower().endswith(".npy")]
        nii_files  = [f for f in files if f.lower().endswith((".nii", ".nii.gz"))]
        nrrd_files = [f for f in files if f.lower().endswith(".nrrd")]
        dcm_files  = [f for f in files if f.lower().endswith(".dcm")]

        if npy_files:
            return np.load(os.path.join(path, npy_files[0])).astype(np.float32)
        if nii_files and HAVE_NIB:
            return nib.load(os.path.join(path, nii_files[0])).get_fdata().astype(np.float32)
        if nrrd_files:
            return sitk.GetArrayFromImage(sitk.ReadImage(os.path.join(path, nrrd_files[0]))).astype(np.float32)
        if dcm_files:
            return load_dicom_series(path)
        raise ValueError("No CT volume found in folder.")

    low = path.lower()
    if low.endswith(".npy"):
        return np.load(path).astype(np.float32)
    if low.endswith((".nii", ".nii.gz")) and HAVE_NIB:
        return nib.load(path).get_fdata().astype(np.float32)
    if low.endswith(".nrrd"):
        return sitk.GetArrayFromImage(sitk.ReadImage(path)).astype(np.float32)
    if low.endswith(".dcm"):
        ds        = pydicom.dcmread(path)
        arr       = ds.pixel_array.astype(np.float32)
        slope     = getattr(ds, "RescaleSlope",     1)
        intercept = getattr(ds, "RescaleIntercept", 0)
        return (arr * slope + intercept)[np.newaxis, ...]
    raise ValueError("Unsupported CT format.")


def load_seg_volume(path):
    if not path:
        raise ValueError("Empty segmentation path.")
    if not os.path.exists(path):
        raise FileNotFoundError(f"Segmentation path does not exist: {path}")

    seg = None
    if os.path.isdir(path):
        files      = os.listdir(path)
        npy_files  = [f for f in files if f.lower().endswith(".npy")]
        nii_files  = [f for f in files if f.lower().endswith((".nii", ".nii.gz"))]
        nrrd_files = [f for f in files if f.lower().endswith(".nrrd")]
        dcm_files  = [f for f in files if f.lower().endswith(".dcm")]

        if npy_files:
            seg = np.load(os.path.join(path, npy_files[0]))
        elif nii_files and HAVE_NIB:
            seg = nib.load(os.path.join(path, nii_files[0])).get_fdata()
        elif nrrd_files:
            seg = sitk.GetArrayFromImage(sitk.ReadImage(os.path.join(path, nrrd_files[0])))
        elif dcm_files:
            seg = sitk.GetArrayFromImage(sitk.ReadImage(os.path.join(path, dcm_files[0])))
        else:
            raise ValueError("No segmentation file in folder.")
    else:
        low = path.lower()
        if low.endswith(".npy"):
            seg = np.load(path)
        elif low.endswith((".nii", ".nii.gz")) and HAVE_NIB:
            seg = nib.load(path).get_fdata()
        elif low.endswith(".nrrd"):
            seg = sitk.GetArrayFromImage(sitk.ReadImage(path))
        elif low.endswith(".dcm"):
            seg = sitk.GetArrayFromImage(sitk.ReadImage(path))
        else:
            raise ValueError("Unsupported segmentation format.")

    seg = np.asarray(seg)
    if seg.ndim == 4:
        seg = (seg > 0).any(axis=0).astype(np.uint8)
    else:
        seg = (seg > 0).astype(np.uint8)
    return seg


# ==============================================================================
#  VIEWER ENDPOINTS
# ==============================================================================

@app.route("/viewer/init", methods=["POST"])
def viewer_init():
    try:
        data     = request.get_json()
        ct_path  = data["path"]
        seg_path = data.get("seg_path", "").strip()
        ww       = data.get("ww", 400)
        wl       = data.get("wl", 40)

        vol      = load_ct_volume(ct_path)
        D, H, W  = vol.shape
        mid      = {"z": D // 2, "y": H // 2, "x": W // 2}

        axial_b64    = slice_to_png_base64(vol[mid["z"], :, :],    ww, wl)
        sagittal_b64 = slice_to_png_base64(vol[:, :, mid["x"]],    ww, wl)
        coronal_b64  = slice_to_png_base64(vol[:, mid["y"], :],    ww, wl)

        axial_seg_b64 = sagittal_seg_b64 = coronal_seg_b64 = None
        if seg_path:
            seg = load_seg_volume(seg_path)
            if seg.shape != vol.shape:
                raise ValueError(f"Segmentation shape {seg.shape} != CT {vol.shape}")
            axial_seg_b64    = mask_to_overlay_png_base64(seg[mid["z"], :, :])
            sagittal_seg_b64 = mask_to_overlay_png_base64(seg[:, :, mid["x"]])
            coronal_seg_b64  = mask_to_overlay_png_base64(seg[:, mid["y"], :])

        return jsonify({
            "shape":       {"depth": D, "height": H, "width": W},
            "mid_indices": mid,
            "axial_png":       axial_b64,
            "sagittal_png":    sagittal_b64,
            "coronal_png":     coronal_b64,
            "axial_seg_png":   axial_seg_b64,
            "sagittal_seg_png": sagittal_seg_b64,
            "coronal_seg_png": coronal_seg_b64,
        })
    except Exception as e:
        return jsonify({"error": str(e)}), 400


@app.route("/viewer/slice", methods=["POST"])
def viewer_slice():
    try:
        data     = request.get_json()
        ct_path  = data["path"]
        seg_path = data.get("seg_path", "").strip()
        axis     = data["axis"]
        index    = int(data["index"])
        ww       = data.get("ww", 400)
        wl       = data.get("wl", 40)

        vol      = load_ct_volume(ct_path)
        D, H, W  = vol.shape

        if axis == "axial":
            index    = max(0, min(D - 1, index))
            ct_slice = vol[index, :, :]
        elif axis == "sagittal":
            index    = max(0, min(W - 1, index))
            ct_slice = vol[:, :, index]
        elif axis == "coronal":
            index    = max(0, min(H - 1, index))
            ct_slice = vol[:, index, :]
        else:
            return jsonify({"error": "Invalid axis"}), 400

        ct_b64  = slice_to_png_base64(ct_slice, ww, wl)
        seg_b64 = None

        if seg_path:
            seg = load_seg_volume(seg_path)
            if seg.shape != vol.shape:
                raise ValueError(f"Segmentation shape {seg.shape} != CT {vol.shape}")
            if axis == "axial":
                mask_slice = seg[index, :, :]
            elif axis == "sagittal":
                mask_slice = seg[:, :, index]
            else:
                mask_slice = seg[:, index, :]
            seg_b64 = mask_to_overlay_png_base64(mask_slice)

        return jsonify({"png_ct": ct_b64, "png_seg": seg_b64})
    except Exception as e:
        return jsonify({"error": str(e)}), 400


@app.route("/viewer/seg3d", methods=["POST"])
def viewer_seg3d():
    try:
        data     = request.get_json()
        seg_path = data["seg_path"]
        seg      = load_seg_volume(seg_path)
        if seg.max() == 0:
            return jsonify({"error": "Segmentation is empty (all zeros)."}), 400
        verts, faces, _, _ = measure.marching_cubes(seg, level=0.5)
        if verts.shape[0] == 0:
            return jsonify({"error": "Marching cubes produced no vertices."}), 400
        return jsonify({"vertices": verts.tolist(), "faces": faces.tolist()})
    except Exception as e:
        return jsonify({"error": str(e)}), 400


# ==============================================================================
#  UPLOAD & LIST ENDPOINTS
# ==============================================================================

@app.route("/upload-folder", methods=["POST"])
def upload_folder():
    if "files" not in request.files:
        return jsonify({"error": "No files sent"}), 400
    files  = request.files.getlist("files")
    stored = []
    for f in files:
        save_path = os.path.join(UPLOAD_FOLDER, f.filename)
        os.makedirs(os.path.dirname(save_path), exist_ok=True)
        f.save(save_path)
        stored.append(save_path)
    return jsonify({"message": f"Uploaded {len(stored)} files"})


@app.route("/upload", methods=["POST"])
def upload_file():
    if "file" not in request.files:
        return jsonify({"error": "No file"}), 400
    f         = request.files["file"]
    save_path = os.path.join(UPLOAD_FOLDER, f.filename)
    os.makedirs(os.path.dirname(save_path), exist_ok=True)
    f.save(save_path)
    return jsonify({"message": "File uploaded"})


@app.route("/list-items", methods=["GET"])
def list_items():
    items = []
    for root, dirs, files in os.walk(UPLOAD_FOLDER):
        for d in dirs:
            items.append({"path": os.path.join(root, d), "type": "folder"})
        for f in files:
            items.append({"path": os.path.join(root, f), "type": "file"})
    return jsonify({"items": items})


@app.route("/upload_scan", methods=["POST"])
def upload_scan():
    """Nurse Dashboard upload endpoint."""
    try:
        patient_id      = request.form.get("patient_id")
        target_path_raw = request.form.get("target_path")

        if not patient_id or not target_path_raw:
            return jsonify({"error": "Missing patient_id or target_path"}), 400

        patient_folder = os.path.join(target_path_raw, patient_id)
        os.makedirs(patient_folder, exist_ok=True)

        files       = request.files.getlist("files")
        saved_count = 0

        if not files:
            return jsonify({"message": "No files received"}), 400

        for file in files:
            if file.filename:
                save_path = os.path.join(patient_folder, file.filename)
                os.makedirs(os.path.dirname(save_path), exist_ok=True)
                file.save(save_path)
                saved_count += 1

        return jsonify({
            "status":  "success",
            "message": "Upload successful",
            "folder":  patient_folder,
            "count":   saved_count,
        }), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 500


# ==============================================================================
#  TOTALSEGMENTATOR — LUNG PIPELINE
# ==============================================================================

JOBS      = {}
JOBS_LOCK = threading.Lock()

LUNG_LOBE_CLASSES = [
    "lung_upper_lobe_left",
    "lung_lower_lobe_left",
    "lung_upper_lobe_right",
    "lung_middle_lobe_right",
    "lung_lower_lobe_right",
]


def update_job_status(case_id: str, status: str, error: str | None = None):
    with JOBS_LOCK:
        if case_id not in JOBS:
            JOBS[case_id] = {}
        JOBS[case_id]["status"] = status
        if error is not None:
            JOBS[case_id]["error"] = error


def convert_nrrd_to_nifti(input_path: str, tmp_dir: str) -> str:
    img        = sitk.ReadImage(input_path)
    nifti_path = os.path.join(tmp_dir, "input_converted.nii.gz")
    sitk.WriteImage(img, nifti_path)
    return nifti_path


def convert_nii_to_nrrd(nii_path: str, out_dir: str) -> str:
    img  = sitk.ReadImage(nii_path)
    base = os.path.basename(nii_path)
    if base.endswith(".nii.gz"):
        base = base[:-7]
    elif base.endswith(".nii"):
        base = base[:-4]
    out_path = os.path.join(out_dir, base + ".nrrd")
    sitk.WriteImage(img, out_path)
    return out_path


def run_totalseg_lung(input_nii: str, case_result_dir: str, case_id: str):
    gpu_error_msg = None
    if HAS_TORCH and torch.cuda.is_available():
        update_job_status(case_id, "Running lung segmentation on GPU (low-memory mode)...")
        try:
            totalsegmentator(
                input=input_nii, output=case_result_dir, task="total",
                roi_subset=LUNG_LOBE_CLASSES, fast=True, body_seg=True,
                force_split=True, preview=False, device="gpu", verbose=True,
            )
            return
        except Exception as e:
            gpu_error_msg = str(e)
            update_job_status(case_id, "GPU failed — falling back to CPU...")

    update_job_status(case_id, "Running lung segmentation on CPU (10–30 min)...")
    try:
        totalsegmentator(
            input=input_nii, output=case_result_dir, task="total",
            roi_subset=LUNG_LOBE_CLASSES, fast=True, body_seg=True,
            force_split=True, preview=False, device="cpu", verbose=True,
        )
        update_job_status(case_id, "Lung segmentation on CPU completed.")
    except Exception as cpu_err:
        msg = f"GPU error: {gpu_error_msg}; CPU error: {cpu_err}" if gpu_error_msg else str(cpu_err)
        raise RuntimeError(msg)


def process_case(case_id: str, uploaded_path: str, original_filename: str):
    tmp_dir         = tempfile.mkdtemp(prefix=f"totalseg_{case_id}_")
    case_result_dir = os.path.join(RESULT_BASE_DIR, case_id)
    os.makedirs(case_result_dir, exist_ok=True)

    try:
        update_job_status(case_id, "Preparing input...")
        fn_lower = original_filename.lower()

        if fn_lower.endswith((".nii", ".nii.gz")):
            input_nii = uploaded_path
        elif fn_lower.endswith(".nrrd"):
            update_job_status(case_id, "Converting NRRD to NIfTI...")
            input_nii = convert_nrrd_to_nifti(uploaded_path, tmp_dir)
        elif fn_lower.endswith(".zip"):
            update_job_status(case_id, "Extracting DICOM ZIP and converting to NIfTI...")
            dicom_dir = os.path.join(tmp_dir, "dicom")
            os.makedirs(dicom_dir, exist_ok=True)
            with zipfile.ZipFile(uploaded_path, "r") as zf:
                zf.extractall(dicom_dir)
            nifti_dir = os.path.join(tmp_dir, "nifti")
            os.makedirs(nifti_dir, exist_ok=True)
            dicom2nifti.convert_directory(dicom_dir, nifti_dir, reorient=True)
            nii_files = [f for f in os.listdir(nifti_dir) if f.endswith((".nii.gz", ".nii"))]
            if not nii_files:
                raise RuntimeError("DICOM→NIfTI conversion failed: no NIfTI files found.")
            input_nii = os.path.join(nifti_dir, nii_files[0])
        else:
            raise RuntimeError("Unsupported file type. Upload .nii / .nii.gz / .nrrd / .zip (DICOM).")

        run_totalseg_lung(input_nii, case_result_dir, case_id)

        update_job_status(case_id, "Converting lung masks to NRRD...")
        nrrd_dir = os.path.join(case_result_dir, "nrrd_masks")
        os.makedirs(nrrd_dir, exist_ok=True)
        for root, _, files in os.walk(case_result_dir):
            for f in files:
                if not (f.endswith(".nii") or f.endswith(".nii.gz")):
                    continue
                if not os.path.basename(f).startswith("lung_"):
                    continue
                convert_nii_to_nrrd(os.path.join(root, f), nrrd_dir)

        update_job_status(case_id, "Creating ZIP file...")
        zip_path = os.path.join(case_result_dir, f"{case_id}_lungs_nrrd.zip")
        with zipfile.ZipFile(zip_path, "w", zipfile.ZIP_DEFLATED) as zipf:
            for f in os.listdir(nrrd_dir):
                zipf.write(os.path.join(nrrd_dir, f), arcname=f)

        if not os.path.exists(zip_path):
            raise RuntimeError("ZIP file creation failed.")

        with JOBS_LOCK:
            JOBS[case_id]["zip_path"] = zip_path
        update_job_status(case_id, "finished")

    except Exception as e:
        update_job_status(case_id, "error", str(e))
    finally:
        shutil.rmtree(tmp_dir, ignore_errors=True)


@app.route("/api/totalseg_start", methods=["POST"])
def totalseg_start():
    if "file" not in request.files:
        return jsonify({"error": "No file uploaded"}), 400
    file = request.files["file"]
    if file.filename == "":
        return jsonify({"error": "Empty filename"}), 400

    case_id          = str(uuid.uuid4())[:8]
    original_name    = file.filename
    case_upload_dir  = os.path.join(UPLOAD_DIR, case_id)
    os.makedirs(case_upload_dir, exist_ok=True)
    uploaded_path    = os.path.join(case_upload_dir, original_name)
    file.save(uploaded_path)

    with JOBS_LOCK:
        JOBS[case_id] = {"status": "started", "error": None, "zip_path": None, "original_name": original_name}

    threading.Thread(target=process_case, args=(case_id, uploaded_path, original_name), daemon=True).start()
    return jsonify({"case_id": case_id, "status": "started"}), 200


@app.route("/api/totalseg_status/<case_id>", methods=["GET"])
def totalseg_status(case_id):
    with JOBS_LOCK:
        job = JOBS.get(case_id)
    if not job:
        return jsonify({"error": "Job not found"}), 404
    return jsonify({"case_id": case_id, "status": job.get("status"), "error": job.get("error")})


@app.route("/api/totalseg_download/<case_id>", methods=["GET"])
def totalseg_download(case_id):
    with JOBS_LOCK:
        job = JOBS.get(case_id)
    if not job:
        return jsonify({"error": "Job not found"}), 404
    if job.get("status") != "finished":
        return jsonify({"error": "Job not finished yet"}), 400
    zip_path = job.get("zip_path")
    if not zip_path or not os.path.exists(zip_path):
        return jsonify({"error": "ZIP file not found"}), 500
    base_name     = job.get("original_name", case_id).rsplit(".", 1)[0]
    download_name = f"{base_name}_lungs_nrrd.zip"
    return send_file(zip_path, as_attachment=True, download_name=download_name)


# ==============================================================================
#  IMAGE CONVERTER ENDPOINT
# ==============================================================================

@app.route("/convert", methods=["POST"])
def convert_image():
    try:
        data        = request.get_json()
        input_path  = data.get("path")
        target_fmt  = data.get("format", "png").lower()

        if not input_path or not os.path.exists(input_path):
            return jsonify({"error": "File or folder not found."}), 404

        if os.path.isdir(input_path):
            reader = sitk.ImageSeriesReader()
            reader.SetFileNames(reader.GetGDCMSeriesFileNames(input_path))
            image  = reader.Execute()
        else:
            image  = sitk.ReadImage(input_path)

        base_name       = os.path.basename(input_path).split(".")[0]
        output_filename = f"{base_name}_converted.{target_fmt}"
        output_path     = os.path.join(os.path.dirname(input_path), output_filename)

        if target_fmt in ["nii", "nii.gz", "nrrd"]:
            sitk.WriteImage(image, output_path)
        elif target_fmt in ["png", "jpg", "jpeg"]:
            arr = sitk.GetArrayFromImage(image)
            if arr.ndim == 3:
                slice_2d = arr[arr.shape[0] // 2, :, :]
            elif arr.ndim == 4:
                slice_2d = arr[0, arr.shape[1] // 2, :, :]
            else:
                slice_2d = arr

            if slice_2d.max() <= 1.0:
                img_8bit = (slice_2d * 255).astype(np.uint8)
            else:
                wl, ww   = -500, 1500
                upper, lower = wl + ww // 2, wl - ww // 2
                img_8bit = ((np.clip(slice_2d, lower, upper) - lower) / (upper - lower) * 255).astype(np.uint8)

            pil_img = Image.fromarray(img_8bit)
            if target_fmt in ["jpg", "jpeg"]:
                pil_img = pil_img.convert("RGB")
            pil_img.save(output_path)

        return jsonify({"message": "Conversion successful!", "output_path": output_path})

    except Exception as e:
        return jsonify({"error": str(e)}), 500


# ==============================================================================
#  BRAIN / BRATS PIPELINE
# ==============================================================================

BRAIN_JOBS = {}
BRAIN_LOCK = threading.Lock()


def update_brain_status(case_id, status, error=None):
    with BRAIN_LOCK:
        if case_id not in BRAIN_JOBS:
            BRAIN_JOBS[case_id] = {}
        BRAIN_JOBS[case_id]["status"] = status
        if error:
            BRAIN_JOBS[case_id]["error"] = error


def run_brain_pipeline(case_id, zip_path, case_dir):
    try:
        update_brain_status(case_id, "processing")
        print(f"\n[BRAIN] Started: {case_id}")

        try:
            modalities = unzip_and_validate_modalities(zip_path, case_dir["unzipped"])
            (case_dir["input"] / "manifest.json").write_text(json.dumps(modalities, indent=2))
        except Exception as e:
            raise RuntimeError(f"Validation failed: {e}")

        print(f"[BRAIN] Running MONAI Inference...")
        try:
            run_brats_bundle_inference(
                bundle_dir=BUNDLE_DIR,
                case_unzipped_dir=case_dir["unzipped"],
                case_output_dir=case_dir["output"],
            )
        except Exception as e:
            raise RuntimeError(f"Inference failed: {e}")

        print(f"[BRAIN] Generating Mesh...")
        seg_files = list(case_dir["output"].rglob("*_seg.nii.gz")) or \
                    list(case_dir["output"].rglob("*_seg.nii"))
        if not seg_files:
            raise RuntimeError("No segmentation file (*_seg.nii.gz) found after inference.")

        seg_path  = seg_files[0]
        mesh_path = case_dir["mesh"] / "brain_mesh.glb"
        case_dir["mesh"].mkdir(parents=True, exist_ok=True)

        try:
            seg_to_glb(seg_path, mesh_path, label_value=4)
        except Exception as e1:
            print(f"[BRAIN] Mesh label-4 failed: {e1}. Trying label-1...")
            try:
                seg_to_glb(seg_path, mesh_path, label_value=1)
            except Exception as e2:
                raise RuntimeError(f"Mesh creation failed: {e2}")

        print(f"[BRAIN] Generating 2D Previews...")
        try:
            generate_previews_t1ce_all_planes(
                t1ce_path=Path(modalities["t1ce"]),
                seg_path=seg_path,
                preview_dir=case_dir["preview"],
            )
        except Exception as e:
            raise RuntimeError(f"Preview generation failed: {e}")

        update_brain_status(case_id, "finished")
        print(f"[BRAIN] SUCCESS: {case_id}")

    except Exception as e:
        print(f"[BRAIN] ERROR: {e}")
        traceback.print_exc()
        update_brain_status(case_id, "error", str(e))


@app.post("/api/brain/start")
def brain_start():
    if "file" not in request.files:
        return jsonify({"error": "No file field named 'file'"}), 400
    f = request.files["file"]
    if not f.filename.lower().endswith(".zip"):
        return jsonify({"error": "Only .zip uploads are supported for Brain MRI"}), 400

    case_id  = str(uuid.uuid4())
    case_dir = ensure_case_dirs(CASES_DIR, case_id)
    zip_path = case_dir["input"] / "upload.zip"
    f.save(zip_path)

    with BRAIN_LOCK:
        BRAIN_JOBS[case_id] = {"status": "starting", "error": None}

    threading.Thread(target=run_brain_pipeline, args=(case_id, zip_path, case_dir), daemon=True).start()
    return jsonify({"case_id": case_id, "message": "Brain analysis started successfully."})


@app.get("/api/brain/status/<case_id>")
def brain_get_status(case_id):
    with BRAIN_LOCK:
        job = BRAIN_JOBS.get(case_id)
    if not job:
        return jsonify({"error": "Job not found"}), 404
    return jsonify(job)


@app.get("/api/case/<case_id>/download/<path:relpath>")
def download_brain_case_file(case_id: str, relpath: str):
    """Robust download for Brain case files — checks 7 possible locations."""
    case_dir = get_case_dir(CASES_DIR, case_id)

    possible_paths = [
        case_dir / relpath,
        case_dir / "output" / relpath,
        case_dir / relpath.replace("output/", "", 1),
        case_dir / "mesh"   / relpath,
        case_dir / "output" / "mesh"    / relpath,
        case_dir / "preview" / relpath,
        case_dir / "output" / "preview" / relpath,
    ]

    if relpath == "brain_mesh.glb":
        possible_paths.insert(0, case_dir / "mesh"   / "brain_mesh.glb")
        possible_paths.insert(1, case_dir / "output" / "mesh" / "brain_mesh.glb")

    print(f"\n[DOWNLOAD-BRAIN] '{relpath}' for case {case_id}")
    for p in possible_paths:
        if p.exists() and p.is_file():
            print(f" → FOUND: {p}")
            return send_from_directory(p.parent, p.name)

    print(f" → NOT FOUND (searched {len(possible_paths)} locations)")
    return jsonify({"error": "File not found"}), 404


# ==============================================================================
#  SPLEEN / CT-ICH PIPELINE  (merged from spleen-ct-app-backend/app.py)
# ==============================================================================

@app.post("/api/ct-spleen/submit")
def ct_spleen_submit():
    if "file" not in request.files:
        return jsonify({"error": "No file field named 'file'"}), 400

    f        = request.files["file"]
    filename = (f.filename or "").lower()

    if not (filename.endswith(".nii") or filename.endswith(".nii.gz") or filename.endswith(".zip")):
        return jsonify({"error": "Upload must be .nii, .nii.gz, or .zip"}), 400

    workflow = "ct-spleen"
    case_id  = str(uuid.uuid4())
    case_dir = spleen_ensure_case_dirs(SPLEEN_CASES_DIR, workflow, case_id)

    suffix      = ".zip" if filename.endswith(".zip") else (".nii.gz" if filename.endswith(".nii.gz") else ".nii")
    upload_path = case_dir["input"] / f"upload{suffix}"
    f.save(upload_path)

    def file_url(relpath: str) -> str:
        return f"/api/case/{workflow}/{case_id}/download/{relpath}"

    try:
        ct_path   = save_single_ct_from_upload(upload_path, case_dir["input"])
        mask_path = case_dir["output"] / "mask.nii.gz"
        run_ct_ich_inference(ct_path, mask_path)

        previews  = generate_ct_previews(ct_path, case_dir["preview"], n_slices=24)
        mesh_path = case_dir["mesh"] / "mask.ply"
        mask_to_mesh(mask_path, mesh_path)

        preview_urls = {
            plane: [file_url(f"output/preview/{fn}") for fn in files]
            for plane, files in previews.items()
        }
    except Exception as e:
        return jsonify({"error": f"Processing failed: {e}"}), 400

    return jsonify({
        "workflow": workflow,
        "case_id":  case_id,
        "message":  "CT-Spleen inference complete.",
        "outputs": {
            "ct_url":   file_url(ct_path.relative_to(case_dir["case"]).as_posix()),
            "mask_url": file_url(mask_path.relative_to(case_dir["case"]).as_posix()),
            "mesh_url": file_url(mesh_path.relative_to(case_dir["case"]).as_posix()),
            "previews": preview_urls,
        },
    })


@app.get("/api/case/<workflow>/<case_id>/download/<path:relpath>")
def download_spleen_case_file(workflow: str, case_id: str, relpath: str):
    """Download endpoint for Spleen (and any workflow-namespaced) case files."""
    case_dir = spleen_get_case_dir(SPLEEN_CASES_DIR, workflow, case_id)
    target   = case_dir / relpath

    if not target.exists():
        return jsonify({"error": "File not found"}), 404

    # Path-traversal guard
    try:
        target.relative_to(case_dir)
    except ValueError:
        return jsonify({"error": "Invalid path"}), 400  

    return send_from_directory(target.parent, target.name, as_attachment=True)


# ==============================================================================
#  HEALTH CHECK
# ==============================================================================

@app.route("/health", methods=["GET"])
def health_check():
    gpu_status = "Available" if HAS_TORCH and torch.cuda.is_available() else "CPU Only"
    return jsonify({
        "status":  "ONLINE",
        "backend": "NeuroPACS Python AI Engine (Brain + Lung + Spleen)",
        "gpu":     gpu_status,
    })


# ==============================================================================
#  ENTRY POINT
# ==============================================================================

if __name__ == "__main__":
    print("=" * 60)
    print("  NeuroPACS Python AI Engine")
    print(f"  Uploads : {BASE_UPLOAD_DIR}")
    print(f"  Cases   : {CASES_DIR.resolve()}")
    print("  Pipelines: Brain (MONAI/BraTS) | Lung (TotalSegmentator) | Spleen (CT-ICH)")
    print("=" * 60)
    app.run(
        host=os.environ.get("PACS_HOST", "127.0.0.1"),
        port=int(os.environ.get("PACS_PORT", "5000")),
        debug=os.environ.get("PACS_DEBUG", "false").lower() == "true",
    )
