from pathlib import Path
import numpy as np
import nibabel as nib
from skimage.measure import marching_cubes
import trimesh

def seg_to_glb(seg_path: Path, out_glb_path: Path, label_value: int = 1):
    img = nib.load(str(seg_path))
    data = img.get_fdata().astype(np.uint8)

    # Extract a binary mask for the chosen label
    mask = (data == label_value).astype(np.uint8)
    if mask.max() == 0:
        raise ValueError(f"No voxels found for label={label_value} in {seg_path.name}")

    # marching cubes expects values in {0,1} and level=0.5
    verts, faces, normals, _ = marching_cubes(mask, level=0.5)

    # Convert voxel coordinates to real-world spacing using affine
    # We can apply spacing from header zooms as a simple approximation.
    zooms = img.header.get_zooms()[:3]
    verts = verts * np.array(zooms)[None, :]

    mesh = trimesh.Trimesh(vertices=verts, faces=faces, vertex_normals=normals, process=False)

    out_glb_path.parent.mkdir(parents=True, exist_ok=True)
    mesh.export(str(out_glb_path))
    return str(out_glb_path)
