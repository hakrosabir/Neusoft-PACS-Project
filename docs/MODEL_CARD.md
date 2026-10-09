# Model and training card

## Scope and artifacts

The notebook adapts a Llama 3.1 8B instruction model for hospital-application assistance. This is separate from the pretrained MONAI/TotalSegmentator imaging models and from runtime PDF retrieval.

Included: [notebook](../neusoft-fine-tuned-RAG-model/fine_tuning.ipynb) and [Ollama Modelfile](../neusoft-fine-tuned-RAG-model/Modelfile). Excluded: training outputs, external datasets, checkpoints, and GGUF weights. `Neusoft_PACS.gguf` is not available in this source snapshot.

## Recipe visible in the notebook

| Setting | Code value |
| --- | --- |
| Base checkpoint | `unsloth/Meta-Llama-3.1-8B-Instruct-bnb-4bit` |
| Loading | 4-bit, float16 |
| Adaptation | LoRA `r=8`, `lora_alpha=16`, dropout `0.05` |
| Target modules | q/k/v/o projections plus gate/up/down projections |
| Trainer | TRL `SFTTrainer` / `SFTConfig` |
| Steps | 300 |
| Batch / accumulation | 2 / 4 |
| Learning rate | `2e-4`, cosine scheduler, 10 warmup steps |
| Optimizer / decay | `adamw_8bit` / `0.01` |
| Seed | 3407 |
| Export | GGUF `q4_k_m` |
| Ollama context setting | 4096 tokens |

**Reproducibility gaps:** the model loader sets sequence length 512 while the trainer sets 2048. Some notebook prose and plot labels describe older `r=16 / alpha=32` settings. Treat executable configuration as the recipe, and reconcile those inconsistencies before retraining. The notebook upgrades Unsloth dynamically and does not record a pinned training environment or immutable dataset/model revisions. Its Kaggle export cell includes cleanup commands; review them before executing.

## Data

The recipe samples 1,500 rows each from HealthCareMagic and MedQuAD using a fixed sampling seed, and appends custom application-navigation, patient, doctor, refusal, and troubleshooting QA pairs. Actual retained counts depend on upstream columns and filtering. The two external datasets are referenced in [attributions](../THIRD_PARTY_NOTICES.md); their data is not redistributed.

The training examples are not a comprehensive clinical corpus. Some custom answers contain operational assumptions such as approval turnaround times and feature descriptions; these require review against the current application before retraining.

## Evaluation status

The notebook defines four qualitative prompts covering registration, report viewing, diagnosis refusal, and brain-workflow navigation. No held-out split, automated grounding score, refusal benchmark, clinical evaluation, or measured production latency is established by this repository.

Training loss and `exp(loss)` plots are training diagnostics, not proof of medical accuracy. Historical graph annotations and approximately 20-token/second deployment notes are not reproduced benchmarks. No numerical performance claim is made in the portfolio.

## Runtime use

Patient chat calls the configurable `PATIENT_CHAT_MODEL`; doctor PDF RAG uses `DOCTOR_CHAT_MODEL`, defaulting to `mistral`. Embeddings use `mxbai-embed-large`. Prompt instructions request grounded responses, but do not guarantee them or enforce access control.

## Imaging models

| Pipeline | Third-party model/tool | Integration output |
| --- | --- | --- |
| Brain MRI | MONAI BraTS SegResNet bundle | Segmentation files, preview index, GLB mesh |
| Spleen CT | MONAI spleen UNet bundle | Mask, PNG previews, PLY mesh |
| Lung CT | TotalSegmentator `total` task, lung-lobe subset | NRRD lung masks in ZIP |

Upstream model scores belong to their original evaluation datasets and are not project-specific results. The historical CT “ICH” label does not correspond to a validated hemorrhage model. See [limitations](LIMITATIONS.md) before interpreting outputs.
