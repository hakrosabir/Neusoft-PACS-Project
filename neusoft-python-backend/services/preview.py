from pathlib import Path
import json
import numpy as np
import nibabel as nib
import cv2

def _normalize_to_uint8(slice2d: np.ndarray) -> np.ndarray:
    x = slice2d.astype(np.float32)
    lo, hi = np.percentile(x, (1, 99))
    if hi <= lo:
        lo, hi = float(x.min()), float(x.max() if x.max() > x.min() else x.min() + 1.0)
    x = np.clip((x - lo) / (hi - lo), 0.0, 1.0)
    return (x * 255.0).astype(np.uint8)

def _write_plane(
    t1: np.ndarray,
    seg: np.ndarray,
    plane: str,
    out_dir: Path,
    overlay_color_bgr=(80, 220, 80),
):
   
    plane_dir = out_dir / plane
    plane_dir.mkdir(parents=True, exist_ok=True)

    if plane == "axial":
        n = t1.shape[2]
        get_slice = lambda i: (t1[:, :, i], seg[:, :, i])
    elif plane == "coronal":
        n = t1.shape[1]
        get_slice = lambda i: (t1[:, i, :], seg[:, i, :])
    elif plane == "sagittal":
        n = t1.shape[0]
        get_slice = lambda i: (t1[i, :, :], seg[i, :, :])
    else:
        raise ValueError(f"Unknown plane: {plane}")

    for i in range(n):
        t1_slice, seg_slice = get_slice(i)

        base_u8 = _normalize_to_uint8(t1_slice)
        base_bgr = cv2.cvtColor(base_u8, cv2.COLOR_GRAY2BGR)

        mask = (seg_slice > 0)
        overlay_only = np.zeros_like(base_bgr)
        overlay_only[mask] = overlay_color_bgr


        base_bgr = np.rot90(base_bgr, k=1)
        overlay_only = np.rot90(overlay_only, k=1)

        cv2.imwrite(str(plane_dir / f"img_{i:03d}.png"), base_bgr)
        cv2.imwrite(str(plane_dir / f"overlay_{i:03d}.png"), overlay_only)

    return int(n)

def generate_previews_t1ce_all_planes(
    t1ce_path: Path,
    seg_path: Path,
    preview_dir: Path,
    overlay_color_bgr=(80, 220, 80),
):

    preview_dir.mkdir(parents=True, exist_ok=True)

    t1_img = nib.load(str(t1ce_path))
    seg_img = nib.load(str(seg_path))

    t1 = t1_img.get_fdata()
    seg = seg_img.get_fdata().astype(np.uint8)

    if t1.shape[:3] != seg.shape[:3]:
        raise ValueError(f"Shape mismatch: t1ce {t1.shape} vs seg {seg.shape}")

    planes = {}
    for plane in ["axial", "coronal", "sagittal"]:
        planes[plane] = {
            "num_slices": _write_plane(
                t1=t1,
                seg=seg,
                plane=plane,
                out_dir=preview_dir,
                overlay_color_bgr=overlay_color_bgr,
            )
        }

    index = {
        "modality": "t1ce",
        "ext": ".png",
        "zero_pad": 3,
        "img_prefix": "img_",
        "overlay_prefix": "overlay_",
        "planes": planes,
    }
    (preview_dir / "index.json").write_text(json.dumps(index, indent=2))
    return index
