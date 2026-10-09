import json
import subprocess
import sys
from pathlib import Path

def run_brats_bundle_inference(bundle_dir: Path, case_unzipped_dir: Path, case_output_dir: Path):
    config_file = bundle_dir / "configs" / "inference.json"
    if not config_file.exists():
        raise FileNotFoundError(f"Missing bundle config: {config_file}")

    case_output_dir.mkdir(parents=True, exist_ok=True)

    # Read manifest created during upload
    manifest_path = case_unzipped_dir.parent / "input" / "manifest.json"
    if not manifest_path.exists():
        raise FileNotFoundError(f"Missing manifest: {manifest_path}")
    manifest = json.loads(manifest_path.read_text())

    # Bundle expects configs/datalist.json — we generate it on the fly
    datalist_path = bundle_dir / "configs" / "datalist.json"

    datalist = {
        "testing": [
            {
                "image": [
                    Path(manifest["t1"]).relative_to(case_unzipped_dir).as_posix(),
                    Path(manifest["t1ce"]).relative_to(case_unzipped_dir).as_posix(),
                    Path(manifest["t2"]).relative_to(case_unzipped_dir).as_posix(),
                    Path(manifest["flair"]).relative_to(case_unzipped_dir).as_posix(),
                ]
            }
        ]
    }

    # Write (and later we can optionally delete it)
    datalist_path.write_text(json.dumps(datalist, indent=2))

    cmd = [
        sys.executable,
        "-m", "monai.bundle",
        "run",
        "--config_file", str(config_file),

        "--dataset_dir", str(case_unzipped_dir),
        "--output_dir", str(case_output_dir),

        "--checkpointloader#map_location", "cpu",

        "--amp", "false",
        "--evaluator#amp", "false",
]


    proc = subprocess.run(cmd, cwd=str(bundle_dir), capture_output=True, text=True)

    if proc.returncode != 0:
        raise RuntimeError(
            "MONAI bundle inference failed.\n"
            f"PY: {sys.executable}\n"
            f"CMD: {' '.join(cmd)}\n"
            f"STDOUT:\n{proc.stdout}\n"
            f"STDERR:\n{proc.stderr}\n"
        )

    return {"stdout": proc.stdout}
