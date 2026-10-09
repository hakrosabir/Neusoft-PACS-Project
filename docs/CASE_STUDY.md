# AI engineering case study

## Project and ownership

**Author:** Sabir Ali  
**Role:** AI Engineer intern at Neusoft  
**Scope:** Independently built the application and its integrations, including frontend, Java API, Python imaging services, database workflow, and LLM fine-tuning notebook.

The pretrained segmentation models come from MONAI and TotalSegmentator. They are integrated dependencies, not models trained from scratch for this project.

## Problem

An imaging model's output is one part of a useful review workflow. A user also needs to upload the right inputs, see processing status, inspect segmentation results, attach them to a patient record, and retrieve the resulting report. This project explores those steps in a web application with local-model assistants.

## What I built

- A Vue interface connecting appointments, scan completion, doctor review, reporting, and patient access.
- A Flask imaging engine that accepts medical volumes, invokes segmentation tools, creates previews and surface meshes, and exposes result downloads.
- MONAI inference wrappers for multimodal brain MRI and single-volume spleen CT, plus a TotalSegmentator lung-lobe workflow with a CPU fallback.
- Java APIs and MySQL persistence for users, appointments, reports, analysis records, and system logs.
- A patient assistant that includes selected database fields in a local-model prompt and translates a report-viewing command into a frontend action.
- A PDF RAG assistant using Spring AI document reading, chunking, embeddings, and similarity retrieval.
- An Unsloth/LoRA fine-tuning notebook for a Llama 3.1 8B hospital-assistant workflow, with GGUF export for Ollama.

## Engineering choices and tradeoffs

**Separate Java and Python responsibilities.** Java owns application records and document chat. Python owns the imaging stack. This avoids moving volume-processing dependencies into Java, at the cost of coordinating paths and response contracts.

**Background processing for heavy inference.** Brain MRI and lung segmentation start background threads and expose status polling. Jobs are held in memory, so durable queues and restart recovery remain future work. The spleen endpoint processes synchronously.

**Local AI inference.** Ollama serves the patient assistant, doctor model, and embeddings. Local execution reduces reliance on hosted inference APIs, but access controls and prompt-only instructions require hardening.

**Visual outputs alongside masks.** Previews and GLB/PLY meshes let a browser inspect results without loading the Python imaging stack. Coordinate and label fidelity need reference validation before clinical interpretation.

## Evidence and evaluation

The repository contains source, schema, a notebook with outputs cleared, and documentation. Portfolio preparation used static checks and did not launch the application, train models, or execute inference.

The notebook records a training recipe and four qualitative inference scenarios. It does not provide a held-out protocol proving medical accuracy or refusal reliability. Historic training-loss plots are not used as accuracy evidence. No project-specific Dice score, latency benchmark, or production deployment claim is made.

## Next steps

1. Enforce authentication, role restrictions, patient ownership, and document isolation.
2. Store jobs durably and generate a separate BraTS datalist per case.
3. Validate channel order and mask-to-mesh mappings against trusted reference outputs.
4. Evaluate grounding, refusals, and report-action correctness on a held-out set.
5. Record model revisions, checksums, environment versions, and measured performance.

## Resume-ready project bullets

- Independently built a medical-imaging AI prototype during a Neusoft internship, integrating Vue 3, Spring Boot, Flask, and MySQL for appointments, imaging review, and reporting.
- Integrated MONAI MRI/CT segmentation and TotalSegmentator lung-lobe inference with background job polling, GPU-to-CPU fallback, multiplanar previews, and interactive Three.js meshes.
- Developed local AI assistants with Ollama and Spring AI, including patient-record context, report-viewing actions, and PDF retrieval-augmented generation.
- Created a Llama 3.1 8B fine-tuning workflow using Unsloth, 4-bit loading, LoRA, supervised fine-tuning, and GGUF export for local deployment.
