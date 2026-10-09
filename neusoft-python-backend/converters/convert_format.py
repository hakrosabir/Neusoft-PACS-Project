import os
import SimpleITK as sitk

def convert_medical_image(src_path, target_format):
    img = sitk.ReadImage(src_path)

    root, _ = os.path.splitext(src_path)
    if target_format == 'nrrd':
        out_path = root + '.nrrd'
    elif target_format == 'nii':
        out_path = root + '.nii'
    elif target_format == 'nii.gz':
        out_path = root + '.nii.gz'
    else:
        raise ValueError(f"Unsupported format: {target_format}")

    sitk.WriteImage(img, out_path)
    return out_path