import zipfile
from pathlib import Path

NIFTI_EXTS = (".nii", ".nii.gz")

def _is_nifti(name: str) -> bool:
    name = name.lower()
    return name.endswith(".nii") or name.endswith(".nii.gz")

def unzip_and_validate_modalities(zip_path: Path, out_dir: Path):
    out_dir.mkdir(parents=True, exist_ok=True)

    with zipfile.ZipFile(zip_path, "r") as z:
        z.extractall(out_dir)

    # Find nifti files anywhere inside
    nii_files = [p for p in out_dir.rglob("*") if p.is_file() and _is_nifti(p.name)]
    if not nii_files:
        raise ValueError("No NIfTI files (.nii/.nii.gz) found inside the ZIP")

    # Heuristic modality detection from filenames
    found = {}
    for p in nii_files:
        n = p.name.lower()

        if "flair" in n:
            found["flair"] = str(p)
        elif "t1ce" in n or "t1c" in n or "t1gd" in n or "t1g" in n:
            found["t1ce"] = str(p)
        elif "t2" in n:
            found["t2"] = str(p)
        elif "t1" in n:

            found["t1"] = str(p)

    required = ["t1", "t1ce", "t2", "flair"]
    missing = [k for k in required if k not in found]
    if missing:
        raise ValueError(
            "Missing required modalities. Expected filenames containing: "
            "t1, t1ce/t1gd/t1c, t2, flair. Missing: " + ", ".join(missing)
        )

    return found
