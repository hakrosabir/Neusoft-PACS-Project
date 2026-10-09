# Known limitations and engineering roadmap

This assessment comes from static source review. The application, training notebook, and inference pipelines were not executed during portfolio preparation.

## Authentication and data isolation

- `SecurityConfig` permits API requests, including admin operations. Login returns a string beginning `JWT-`; it is not a signed and verified JWT.
- Account approval and BCrypt password hashing are implemented, but they do not establish server-side role or patient ownership checks.
- Login and some user endpoints serialize user entities containing password hashes. Response DTOs must exclude them.
- Patient chat accepts a supplied patient ID. Record access must be derived from authenticated identity.
- PDF retrieval uses a shared in-memory vector store without per-doctor document isolation.
- Patient chat history uses patient IDs as keys, and guest requests can share a default session. Prompt instructions do not enforce confidentiality.

## Imaging fidelity

- The BraTS runner orders modalities T1, T1ce, T2, FLAIR. The original local model metadata declares T1c, T1, T2, FLAIR. Channel order must be verified against the selected checkpoint.
- The brain mesh path assumes scalar labels 4 then 1. The bundle metadata describes three tumor-region channels; conversion and label interpretation need validation.
- Brain mesh generation scales voxel coordinates by spacing without applying the full NIfTI affine. This is an approximation, not proof of correct medical-space orientation.
- Slice/overlay alignment, anisotropic spacing, and mask-to-surface behavior need reference testing.
- The spleen workflow segments spleen; “ICH” appears in historical naming and report fields without a hemorrhage-detection model.
- Lung-lobe masks do not establish lung-nodule detection or malignancy classification.

## Jobs and file handling

- Brain and lung jobs live in process memory and background threads. Restart loses job state; there is no durable queue or scheduling limit.
- Brain inference writes a shared `configs/datalist.json`, so concurrent cases can interfere.
- Spleen processing blocks the request until inference completes.
- ZIP extraction, user-supplied filenames, filesystem paths, and result-download path resolution need consistent containment and input validation.
- Python CORS is permissive. Loopback binding and disabled debug reduce accidental exposure but do not replace authentication.
- Java uploads allow 50 MB; Python allows 2 GB. Workflows crossing both services have different limits.

## Demo behavior and measurements

- Nurse worklist failures can fall back to sample names. Its PACS-query action is a placeholder alert.
- Some dashboard status values and upload progress are fixed or simulated; they should not be presented as measured latency or monitoring.
- The repository does not contain Orthanc networking, DICOM SR attachment, MinIO, Docker orchestration, or an authenticated production deployment.
- The custom GGUF is absent. Downloaded MONAI revisions and the captured Python dependency set need clean-environment validation.
- No project-specific segmentation benchmark, clinical validation, load test, held-out LLM evaluation, or validated throughput measurement is included.

## Roadmap

1. Verified authentication, response DTOs, patient ownership checks, and document isolation.
2. Safe file handling, authenticated downloads, and consistent upload limits.
3. Durable job orchestration and per-case inference configuration.
4. Reference tests for modality ordering, mask channels, previews, and medical coordinates.
5. Held-out evaluations for segmentation and grounded chat, with hardware and model revisions recorded.
6. Deployment packaging and observability after the above gaps are addressed.
