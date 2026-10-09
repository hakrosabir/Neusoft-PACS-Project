import zipfile
from pathlib import Path
import shutil

def _is_nifti(name: str) -> bool:
    n = name.lower()
    return n.endswith(".nii") or n.endswith(".nii.gz")

def save_single_ct_from_upload(upload_path: Path, input_dir: Path) -> Path:
    """
    Accept:
      - .nii / .nii.gz directly
      - .zip containing exactly ONE .nii/.nii.gz (any nesting allowed)
    Output:
      - standardized file: input_dir/ct.nii.gz (or ct.nii)
    Returns:
      Path to saved CT file.
    """
    input_dir.mkdir(parents=True, exist_ok=True)
    name = upload_path.name.lower()

    # direct nifti
    if name.endswith(".nii") or name.endswith(".nii.gz"):
        out_path = input_dir / ("ct.nii.gz" if name.endswith(".nii.gz") else "ct.nii")
        shutil.copy2(upload_path, out_path)
        return out_path

    # zip with exactly one nifti
    if name.endswith(".zip"):
        with zipfile.ZipFile(upload_path, "r") as z:
            members = [m for m in z.namelist() if _is_nifti(m) and not m.endswith("/")]
            if len(members) == 0:
                raise ValueError("ZIP contains no .nii/.nii.gz file.")
            if len(members) > 1:
                raise ValueError(f"ZIP must contain exactly ONE NIfTI, found {len(members)}.")

            member = members[0]
            extracted = Path(z.extract(member, path=input_dir))

            out_path = input_dir / ("ct.nii.gz" if extracted.name.lower().endswith(".nii.gz") else "ct.nii")
            if extracted.resolve() != out_path.resolve():
                out_path.parent.mkdir(parents=True, exist_ok=True)
                shutil.move(str(extracted), str(out_path))

            return out_path

    raise ValueError("Unsupported upload type. Please upload .nii, .nii.gz, or .zip.")
