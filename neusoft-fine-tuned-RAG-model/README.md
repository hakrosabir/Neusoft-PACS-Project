# Local assistant fine-tuning

[fine_tuning.ipynb](fine_tuning.ipynb) contains the Unsloth/LoRA training and GGUF export recipe for a Llama 3.1 8B hospital assistant. Notebook outputs are cleared for publication. See the [model card](../docs/MODEL_CARD.md) for configuration inconsistencies and evaluation limits.

`Modelfile` imports `./Neusoft_PACS.gguf`. That artifact is not included. Obtain the separate export before using `ollama create neusoft-ai -f Modelfile`.

The application can use an upstream model through `PATIENT_CHAT_MODEL` for an integration demonstration. This does not reproduce the fine-tuned model. Segmentation bundles are separate third-party models, not products of this notebook.
