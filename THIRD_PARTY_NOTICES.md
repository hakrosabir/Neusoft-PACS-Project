# Third-party attributions

Sabir Ali built the original application as an AI Engineer internship project. Pretrained segmentation models and libraries are separate upstream work.

| Component | Upstream | Use |
| --- | --- | --- |
| MONAI BraTS MRI segmentation | [Model source](https://huggingface.co/MONAI/brats_mri_segmentation) | Multimodal MRI segmentation |
| MONAI spleen CT segmentation | [Model source](https://huggingface.co/MONAI/spleen_ct_segmentation) | Spleen CT segmentation |
| TotalSegmentator | [Original project](https://github.com/wasserth/TotalSegmentator) | Lung-lobe segmentation |
| Llama 3.1 | [Meta model source](https://huggingface.co/meta-llama/Meta-Llama-3.1-8B-Instruct) | Base-model family for fine-tuning |
| Unsloth | [Original project](https://github.com/unslothai/unsloth) | Quantized loading and LoRA |
| HealthCareMagic dataset | [Notebook dataset source](https://huggingface.co/datasets/wangrongsheng/HealthCareMagic-100k-en) | Sampled training questions |
| MedQuAD dataset | [Notebook dataset source](https://huggingface.co/datasets/AnonymousSub/MedQuAD_47441_Question_Answer_Pairs) | Sampled training questions |

Weights, external datasets, and vendored MONAI checkouts are not redistributed. Consult upstream sources for applicable licenses, usage restrictions, and model documentation before downloading or redistributing artifacts. Dataset references in code do not grant redistribution rights.

Dependency versions are recorded in `package-lock.json`, `pom.xml`, and Python `requirements.txt`. Dependency licenses remain with their authors. A repository-wide open-source license has not been selected for the original application.
