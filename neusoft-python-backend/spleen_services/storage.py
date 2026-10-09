from pathlib import Path

def ensure_case_dirs(cases_root: Path, workflow: str, case_id: str) -> dict:
    case_root = cases_root / workflow / case_id
    dirs = {
        "case": case_root,
        "input": case_root / "input",
        "output": case_root / "output",
        "mesh": case_root / "mesh",
        "preview": case_root / "output" / "preview",
    }
    for p in dirs.values():
        p.mkdir(parents=True, exist_ok=True)
    return dirs

def get_case_dir(cases_root: Path, workflow: str, case_id: str) -> Path:
    return cases_root / workflow / case_id
