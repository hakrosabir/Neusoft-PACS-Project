# 🏥 Neusoft PACS — Intelligent Medical Imaging & Diagnosis System

[![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Vue.js](https://img.shields.io/badge/Vue.js-3-4FC08D?logo=vuedotjs)](https://vuejs.org/)
[![Python](https://img.shields.io/badge/Python-3.10-3776AB?logo=python)](https://www.python.org/)
[![PyTorch](https://img.shields.io/badge/PyTorch-2.0-EE4C2C?logo=pytorch)](https://pytorch.org/)
[![Ollama](https://img.shields.io/badge/AI-Ollama-000000?logo=ollama)](https://ollama.com/)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?logo=docker)](https://www.docker.com/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**A full-stack, AI-powered medical imaging platform combining a Vue 3 3D viewer, Spring Boot microservices, a Python AI inference engine, and a custom fine-tuned LLM for Retrieval-Augmented Generation (RAG).**

> 🩻 Upload, view, segment, and diagnose – all inside one secure, role‑based hospital interface.

<p align="center">
  <em>(Place your demo GIF or screenshot here)</em><br>
  <img src="assets/screenshots/dashboard-overview.png" width="700">
</p>

---

## 🧠 What This System Does

The Neusoft PACS is a **complete radiology workflow solution** built during a 6‑month industry internship. It integrates:

- **Patient & Appointment Management** – role‑based dashboards (Admin, Doctor, Nurse, Patient)
- **Medical Image Handling** – DICOM / NIfTI upload, parsing, 3D reconstruction
- **AI‑Powered Segmentation** – lung, brain, and spleen segmentation using nnU‑Net, SegResNet, and 3D U‑Net
- **Multi‑Agent AI Chatbot** – fine‑tuned Llama 3.1 8B with RAG, hallucination safeguards, and UI‑triggered actions
- **PACS Server Integration** – C‑FIND, C‑STORE, C‑MOVE via Orthanc
- **Interactive 2D/3D Viewer** – axial/coronal/sagittal views, ROI tools, 3D mesh overlays

---

## 🏗️ System Architecture

```mermaid
graph TD
    A[Vue3 Frontend<br/>port 5173] -->|REST| B[Spring Boot API<br/>port 8081]
    B --> C[(MySQL)]
    B --> D[Python Flask AI<br/>port 5000]
    D --> E[Segmentation Models<br/>nnU-Net / MONAI]
    B --> F[Ollama<br/>neusoft-ai LLM]
    F --> G[GGUF Q4_K_M<br/>4.8 GB model]
    B --> H[Orthanc PACS<br/>DICOMweb]
