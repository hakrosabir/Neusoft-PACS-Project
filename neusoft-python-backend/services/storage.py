from pathlib import Path

def get_case_dir(cases_dir: Path, case_id: str) -> Path:
    return cases_dir / case_id

def ensure_case_dirs(cases_dir: Path, case_id: str):
    case_dir = get_case_dir(cases_dir, case_id)
    input_dir = case_dir / "input"
    unzipped_dir = case_dir / "unzipped"
    output_dir = case_dir / "output"
    mesh_dir = output_dir / "mesh"
    preview_dir = output_dir / "preview"

    for d in [input_dir, unzipped_dir, output_dir, mesh_dir, preview_dir]:
        d.mkdir(parents=True, exist_ok=True)

    return {
        "case": case_dir,
        "input": input_dir,
        "unzipped": unzipped_dir,
        "output": output_dir,
        "mesh": mesh_dir,
        "preview": preview_dir,
    }
