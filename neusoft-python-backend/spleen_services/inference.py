from pathlib import Path
import os
import shutil
import subprocess
import sys
import tempfile
import nibabel as nib
import numpy as np
from skimage.filters import gaussian

class InferenceError(RuntimeError):
    pass

def run_ct_ich_inference(ct_path: Path, out_mask_path: Path) -> Path:
    default_bundle = Path(__file__).resolve().parents[1] / "bundles" / "spleen_ct_segmentation"
    bundle_dir = Path(os.environ.get("PACS_SPLEEN_BUNDLE_DIR", str(default_bundle))).resolve()
    infer_cfg_rel = Path("configs/inference.json")  # relative to bundle_dir

    if not (bundle_dir / infer_cfg_rel).exists():
        raise InferenceError(f"Missing inference config: {bundle_dir / infer_cfg_rel}")

    out_dir = out_mask_path.parent
    out_dir.mkdir(parents=True, exist_ok=True)

    # Create the folder structure the bundle expects: dataset_dir/imagesTs/*.nii.gz
    with tempfile.TemporaryDirectory(prefix="monai_spleen_") as td:
        td_path = Path(td)
        images_ts = td_path / "imagesTs"
        images_ts.mkdir(parents=True, exist_ok=True)

        # IMPORTANT: write a REAL gzipped nifti (don't just rename .nii to .nii.gz)
        tmp_ct = images_ts / "ct.nii.gz"
        img = nib.load(str(ct_path))
        nib.save(img, str(tmp_ct))

        cmd = [
            sys.executable, "-m", "monai.bundle", "run",
            "--config_file", str(infer_cfg_rel),

            # overrides to point the bundle at our single uploaded scan + our output folder
            f"--dataset_dir={td_path.as_posix()}",
            f"--output_dir={out_dir.as_posix()}",
            "--separate_folder=False",

            # windows-safe
            "--dataloader#num_workers=0",
        ]

        proc = subprocess.run(cmd, capture_output=True, text=True, cwd=str(bundle_dir))
        if proc.returncode != 0:
            raise InferenceError(
                f"MONAI bundle run failed (rc={proc.returncode}).\nSTDOUT:\n{proc.stdout}\nSTDERR:\n{proc.stderr}"
            )

    # Search recursively (bundle may still nest outputs)
    candidates = list(out_dir.rglob("*.nii")) + list(out_dir.rglob("*.nii.gz"))
    if not candidates:
        raise InferenceError(f"No NIfTI produced in {out_dir}")

    newest = max(candidates, key=lambda p: p.stat().st_mtime)
    if newest != out_mask_path:
        if out_mask_path.exists():
            out_mask_path.unlink()
        shutil.move(str(newest), str(out_mask_path))

    return out_mask_path


def _normalize_to_uint8(x: np.ndarray, p_low=1.0, p_high=99.0) -> np.ndarray:
    """
    Robust windowing for previews: percentile clip + scale to 0..255.
    """
    x = np.asarray(x, dtype=np.float32)
    lo, hi = np.percentile(x, [p_low, p_high])
    if hi <= lo:
        hi = lo + 1.0
    x = np.clip(x, lo, hi)
    x = (x - lo) / (hi - lo)
    return (x * 255.0).astype(np.uint8)


def generate_ct_previews(ct_path: Path, preview_dir: Path, n_slices: int = 24) -> dict:
    """
    Saves a small set of axial/coronal/sagittal PNG previews.
    Returns dict with lists of created relative filenames.
    """
    from PIL import Image

    img = nib.load(str(ct_path))
    img = nib.as_closest_canonical(img)         
    vol = img.get_fdata().astype(np.float32)
    sx, sy, sz = img.header.get_zooms()[:3]

    print("ORIG shape:", nib.load(str(ct_path)).shape)
    print("CANON shape:", vol.shape)
    print("zooms:", (sx, sy, sz))


    preview_dir.mkdir(parents=True, exist_ok=True)

    def save_plane(plane: str, slices: list[np.ndarray]) -> list[str]:
        out_files = []
        for i, sl in enumerate(slices):
            sl8 = _normalize_to_uint8(sl)

            arr = np.rot90(sl8)
            im = Image.fromarray(arr)

            if plane == "axial":
                sp_w, sp_h = sx, sy
            elif plane == "coronal":
                sp_w, sp_h = sx, sz
            else:  # sagittal
                sp_w, sp_h = sy, sz

            base = min(sp_w, sp_h)
            scale_w = sp_w / base
            scale_h = sp_h / base

            w, h = im.size
            new_w = max(1, int(round(w * scale_w)))
            new_h = max(1, int(round(h * scale_h)))
            im = im.resize((new_w, new_h), resample=Image.BILINEAR)

            fn = f"{plane}_{i:03d}.png"
            im.save(preview_dir / fn)
            out_files.append(fn)

        return out_files




    # pick evenly spaced indices per axis
    z = vol.shape[2]
    y = vol.shape[1]
    x = vol.shape[0]

    z_idx = np.linspace(0, z - 1, n_slices, dtype=int).tolist()
    y_idx = np.linspace(0, y - 1, n_slices, dtype=int).tolist()
    x_idx = np.linspace(0, x - 1, n_slices, dtype=int).tolist()

    axial = [vol[:, :, k] for k in z_idx]        # XY
    coronal = [vol[:, j, :] for j in y_idx]      # XZ
    sagittal = [vol[i, :, :] for i in x_idx]     # YZ

    return {
        "axial": save_plane("axial", axial),
        "coronal": save_plane("coronal", coronal),
        "sagittal": save_plane("sagittal", sagittal),
    }


def mask_to_mesh(mask_path: Path, out_mesh_path: Path, level: float = 0.5) -> Path:
    """
    Converts a binary mask NIfTI to a surface mesh using marching cubes.
    Writes ASCII PLY (easy to load in Three.js).
    """
    from skimage import measure

    img = nib.load(str(mask_path))
    mask = img.get_fdata().astype(np.float32)

    if mask.max() <= 0:
        # nothing segmented; still create a tiny empty-ish mesh file for consistency
        out_mesh_path.parent.mkdir(parents=True, exist_ok=True)
        out_mesh_path.write_text(
            "ply\nformat ascii 1.0\nelement vertex 0\nproperty float x\nproperty float y\nproperty float z\n"
            "element face 0\nproperty list uchar int vertex_indices\nend_header\n"
        )
        return out_mesh_path

    # spacing from header (voxel sizes)
    zooms = img.header.get_zooms()[:3]
    verts, faces, _, _ = measure.marching_cubes(mask, level=level, spacing=zooms)

    out_mesh_path.parent.mkdir(parents=True, exist_ok=True)
    _write_ply_ascii(out_mesh_path, verts, faces)
    return out_mesh_path


def _write_ply_ascii(path: Path, verts: np.ndarray, faces: np.ndarray) -> None:
    with path.open("w", encoding="utf-8") as f:
        f.write("ply\n")
        f.write("format ascii 1.0\n")
        f.write(f"element vertex {len(verts)}\n")
        f.write("property float x\nproperty float y\nproperty float z\n")
        f.write(f"element face {len(faces)}\n")
        f.write("property list uchar int vertex_indices\n")
        f.write("end_header\n")
        for v in verts:
            f.write(f"{v[0]} {v[1]} {v[2]}\n")
        for tri in faces:
            f.write(f"3 {tri[0]} {tri[1]} {tri[2]}\n")
