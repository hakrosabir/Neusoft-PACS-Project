# Neusoft PACS: Intelligent Medical Imaging & Diagnostic Platform

This repository contains the complete source code for a full-stack medical imaging and patient management system. The platform integrates a Vue 3 3D visualization frontend, a Spring Boot core backend, a Python AI microservice, and a custom fine-tuned local AI model for Retrieval-Augmented Generation (RAG).

The system is designed with a strong emphasis on clinical workflow integration, DICOM standard compliance, and reproducible containerized deployment.

## 📺 Project Demonstration
A complete walkthrough of the system architecture, clinical workflow, and AI integration can be viewed here: 
**[Video Demonstration on YouTube](https://youtu.be/B7_dxOoNZW0)**

---

## 📁 Repository Structure
- `/neusoft-project-frontend` - Vue.js & Three.js frontend for UI and 3D visualization.
- `/neusoft-spring-boot-backend` - Java Spring Boot core API and database management.
- `/neusoft-python-backend` - Python service handling auxiliary AI processing.
- `/neusoft-mysql-database` - Contains the MySQL export file (`neusoft_db.sql`).
- `/neusoft-fine-tuned-RAG-model` - Contains the custom `.gguf` weights, `Modelfile`, and training results for the RAG model.

---

## ⚙️ System Prerequisites
Ensure the following dependencies are installed before deploying the system:
- **Java:** JDK 21
- **Node.js:** v20+
- **Python:** 3.10+
- **MySQL Server:** 8.0+
- **Ollama:** Required for local AI capabilities (Download from [ollama.com](https://ollama.com/download))

---

## 🗄️ Step 1: Database Setup (MySQL)
The Spring Boot backend connects to a local MySQL instance. 

1. Open your database management tool (e.g., MySQL Workbench, DBeaver).
2. Create a new database named exactly: `neusoft_db`
3. Import the provided SQL file located at `neusoft-mysql-database/neusoft_db.sql`.
4. Configure your local MySQL credentials (host, port, username, password) in the Spring Boot `application.properties` file. 

---

## 🧠 Step 2: Local AI Model Setup (Ollama)
This system relies on a custom fine-tuned model (`neusoft-ai`) specifically trained for medical RAG context, alongside an embedding model for document processing.

**A. Build the Custom Medical Model:**
Navigate to the fine-tuned model folder and build the custom AI using the provided `.gguf` weights and Modelfile:
```bash
cd neusoft-fine-tuned-RAG-model
ollama create neusoft-ai -f Modelfile
