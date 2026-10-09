# API guide

Paths are derived from the current controllers and Flask routes. They are a source-level reference, not a tested contract. Java and Python are separate services; both use some `/api/brain` paths with different responsibilities.

## Spring Boot — default port 8081

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/auth/register` | Register user; doctor registration requires approval |
| POST | `/auth/login` | Validate BCrypt password and return prototype token/user |
| GET | `/appointments/verified-doctors` | List approved doctors |
| POST | `/appointments/book` | Create appointment |
| GET | `/appointments/patient/{patientId}` | Patient appointments |
| GET | `/appointments/doctor/{doctorId}` | Doctor appointments |
| PUT | `/appointments/{id}/status` | Update appointment status |
| PUT | `/appointments/{id}/complete-scan` | Save scan completion details |
| GET | `/patientProfiles/list` | Patient selection list |
| POST | `/reports/save` | Save or update report |
| GET | `/reports/patient/{patientId}` | Patient reports |
| GET | `/reports/appointment/{appointmentId}` | Report for appointment |
| POST | `/api/brain/link` | Link Python case ID to patient |
| GET | `/api/brain/patient/{patientId}` | Linked brain-analysis history |
| POST | `/api/chat/ask` | Guest/patient assistant |
| POST | `/api/rag/upload` | Ingest PDF multipart field `file` |
| POST | `/api/rag/ask` | Ask question about ingested documents |
| GET | `/admin/users` | Admin user list |
| PUT | `/admin/users/{id}/approve` | Approve doctor |
| PUT | `/admin/users/{id}/status` | Enable/disable account |
| GET | `/admin/system/logs` | System log list |
| GET | `/admin/system/db-stats` | Database dashboard counts |
| GET | `/admin/system/ai-status` | Python-engine status |

Guest chat request:

```json
{"message": "How do I register?", "patientId": null, "sessionId": "portfolio-demo"}
```

Chat response shape:

```json
{"reply": "Response text", "action": "", "actionId": ""}
```

A parsed report command can return `action: "SHOW_REPORT"` and a report ID. Server authorization must be added before using supplied patient/report IDs with sensitive records.

## Flask — default port 5000

| Method | Path | Input / result |
| --- | --- | --- |
| GET | `/health` | Engine and GPU availability |
| POST | `/upload` | Multipart `file` |
| POST | `/upload-folder` | Multipart repeated `files` |
| POST | `/upload_scan` | `patient_id`, `target_path`, repeated `files` |
| GET | `/list-items` | Local upload files/folders |
| POST | `/convert` | JSON `path`, `format` |
| POST | `/viewer/init` | JSON image/segmentation paths; see source |
| POST | `/viewer/slice` | Slice/window parameters; see source |
| POST | `/viewer/seg3d` | JSON `seg_path`, returns vertices/faces |
| POST | `/api/totalseg_start` | Multipart `file`; returns `case_id` |
| GET | `/api/totalseg_status/{case_id}` | Background lung-job status |
| GET | `/api/totalseg_download/{case_id}` | ZIP containing NRRD lung masks |
| POST | `/api/brain/start` | Multipart `file`: ZIP with four MRI modalities |
| GET | `/api/brain/status/{case_id}` | Background brain-job status |
| GET | `/api/case/{case_id}/download/{relpath}` | Brain-case artifacts |
| POST | `/api/ct-spleen/submit` | Multipart single-volume NIfTI/ZIP; synchronous inference |
| GET | `/api/case/{workflow}/{case_id}/download/{relpath}` | Workflow artifacts |

Brain filenames are detected heuristically through `t1`, `t1ce`/`t1c`/`t1gd`/`t1g`, `t2`, and `flair`. A spleen ZIP must contain exactly one NIfTI. Lung processing additionally accepts NRRD and DICOM ZIP.

Job status responses include `status` and optionally `error`. Python artifact paths refer to the server filesystem. They are not uploadable browser-local paths. [Known limitations](LIMITATIONS.md) cover input safety, concurrency, and authentication.
