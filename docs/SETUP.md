# Local setup guide

These instructions are for a future local demonstration. Portfolio preparation does not start any service, install dependencies, run training, or run inference. The commands below have not been validated end to end on a clean machine.

## Requirements

| Component | Requirement from this source |
| --- | --- |
| Java | JDK 21 (`pom.xml`) |
| Node.js | `^20.19.0` or `>=22.12.0` (`package.json`) |
| Python | Use a dedicated Python 3.11 or 3.12 environment as a starting point; the captured dependency set needs clean-install validation |
| Database | MySQL 8; the schema uses MySQL 8 collations |
| Local AI | Ollama with the chat and embedding models described below |
| Imaging | MONAI bundle artifacts; TotalSegmentator may download weights on first use |

Imaging dependencies and models require substantial storage and memory. CUDA-enabled PyTorch is needed for GPU acceleration; use the [official PyTorch installation guidance](https://pytorch.org/get-started/locally/) for your hardware. CPU fallback exists for lung segmentation but is slower. This repository provides no validated minimum-hardware benchmark.

Clone the source:

```bash
git clone https://github.com/hakrosabir/Neusoft-PACS-Project.git
cd Neusoft-PACS-Project
```

## 1. Database

Create a new empty development database, then import `neusoft-mysql-database/schema.sql`. The file contains seven table definitions and no accounts, records, or report images. It intentionally does not drop existing tables.

In a MySQL client:

```sql
CREATE DATABASE neusoft_db CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE neusoft_db;
SOURCE neusoft-mysql-database/schema.sql;
```

Create a local database user with access to this database and choose your own password. Configure the Java process with `DB_USERNAME` and `DB_PASSWORD`; no preset password or seeded admin account is provided.

For Windows PowerShell, use a password prompt instead of writing a secret in command history:

```powershell
$env:DB_USERNAME = 'neusoft_dev'
$dbCredential = Get-Credential -UserName $env:DB_USERNAME -Message 'Local MySQL credentials'
$env:DB_PASSWORD = $dbCredential.GetNetworkCredential().Password
```

Set those variables in the terminal that will launch Java. For other shells, use your shell or IDE's environment-variable configuration.

Alternatively, create ignored `neusoft-spring-boot-backend/src/main/resources/application-local.properties` containing your local `spring.datasource.username` and `spring.datasource.password`, and activate it with `SPRING_PROFILES_ACTIVE=local`. Do not commit that file.

The empty schema means dashboards initially have no records. Use synthetic data for a demonstration. Registration and the `/admin` route support the prototype account-approval flow, but server-side role authorization is not enforced. See [limitations](LIMITATIONS.md).

## 2. Local AI models

The two chat paths use separate model settings:

| Setting | Default | Purpose |
| --- | --- | --- |
| `PATIENT_CHAT_MODEL` | `neusoft-ai` | Guest/patient chat via `/api/chat/ask` |
| `DOCTOR_CHAT_MODEL` | `mistral` | Spring AI doctor PDF assistant |
| `EMBEDDING_MODEL` | `mxbai-embed-large` | PDF embeddings |
| `OLLAMA_BASE_URL` | `http://localhost:11434` | Local Ollama endpoint |

For a demonstration using upstream models:

```bash
ollama pull mistral
ollama pull mxbai-embed-large
```

Set `PATIENT_CHAT_MODEL=mistral` in the Java process to use an upstream fallback. That demonstrates the integration, not the custom fine-tuned model's behavior.

To use the custom model, first obtain your separately exported `Neusoft_PACS.gguf`; it is absent from this repository. Place it beside the Modelfile, then:

```bash
cd neusoft-fine-tuned-RAG-model
ollama create neusoft-ai -f Modelfile
```

Ollama must be available at the configured URL. See [official GGUF import instructions](https://docs.ollama.com/import) and the [model card](MODEL_CARD.md). The training notebook is a separate GPU workflow, not an application setup step.

## 3. Python environment and model artifacts

From the repository root, on Windows:

```powershell
cd neusoft-python-backend
py -3.11 -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -r requirements.txt
```

On Linux/macOS:

```bash
cd neusoft-python-backend
python3.11 -m venv .venv
source .venv/bin/activate
python -m pip install -r requirements.txt
```

The requirements file is a captured development environment, including optional/transitive packages. It is not a proven portable lockfile. The application eagerly imports TotalSegmentator and imaging dependencies, so they are needed even to start the engine.

Obtain the full bundles from their official sources:

- [MONAI BraTS MRI bundle](https://huggingface.co/MONAI/brats_mri_segmentation) → `neusoft-python-backend/bundles/brats_mri_segmentation/`
- [MONAI spleen CT bundle](https://huggingface.co/MONAI/spleen_ct_segmentation) → `neusoft-python-backend/bundles/spleen_ct_segmentation/`

One download option, after dependency installation, from the Python backend directory:

```bash
python -c "from huggingface_hub import snapshot_download; snapshot_download(repo_id='MONAI/brats_mri_segmentation', local_dir='bundles/brats_mri_segmentation')"
python -c "from huggingface_hub import snapshot_download; snapshot_download(repo_id='MONAI/spleen_ct_segmentation', local_dir='bundles/spleen_ct_segmentation')"
```

These commands download artifacts and should only be used when setting up a demonstration. Review upstream terms and record the resolved revisions. The local source originally referenced bundle metadata versions BraTS `0.5.4` and spleen `0.6.1`; downloaded revisions may differ, so check runner compatibility. Each bundle must provide `configs/inference.json` and the checkpoints it references. Bundles and weights are ignored by Git.

Defaults store uploads under `upload/` and cases under `cases/` within the Python backend. Set `PACS_UPLOAD_DIR`, `PACS_CASES_DIR`, `PACS_BRATS_BUNDLE_DIR`, or `PACS_SPLEEN_BUNDLE_DIR` to override them. Relative overrides resolve against the launch directory; prefer absolute paths when launching from an IDE.

## 4. Frontend configuration

```bash
cd neusoft-project-frontend
npm ci
```

From the repository root, copy the frontend example file to `neusoft-project-frontend/.env.local` if defaults need changing. Vite loads `.env.local` automatically. Settings are public browser values and must not contain credentials.

| Setting | Default |
| --- | --- |
| `VITE_JAVA_API_URL` | `http://localhost:8081` |
| `VITE_PYTHON_API_URL` | `http://localhost:5000` |
| `VITE_OLLAMA_API_URL` | `http://localhost:11434` |
| `VITE_SCAN_INPUT_DIR` | `upload` on the Python server |

Java and Python `.env.example` files document process settings; copying them to `.env` does **not** load them automatically. Export variables or use an IDE environment configuration. Java CORS currently permits the local frontend origin; remote deployment requires further configuration and security work.

## 5. Start services only when you want a demonstration

Use separate terminals, with Ollama already available. The following commands start the application and were not executed during portfolio preparation.

Python, from `neusoft-python-backend`, with the environment activated:

```bash
python app.py
```

Java, from `neusoft-spring-boot-backend`:

```powershell
.\mvnw.cmd spring-boot:run
```

On Linux/macOS use `./mvnw spring-boot:run`. The wrapper downloads Maven when needed.

Frontend, from `neusoft-project-frontend`:

```bash
npm run dev
```

Open `http://localhost:5173`. Python defaults to loopback with debug disabled. This is a local prototype; do not expose the current APIs as a public clinical service.

## Static checks without application startup

From the repository root:

```bash
python scripts/verify_portfolio.py
```

This checks the publication file selection, Python syntax, JSON/XML syntax, notebook output removal, schema content, local documentation links, and obvious credential patterns. It does not import the application, download models, connect to MySQL, or start services. It is not an end-to-end test.
