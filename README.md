# 🏥 Neusoft PACS (Picture Archiving and Communication System)

Welcome to the Neusoft PACS Project! This is a complete, full-stack medical imaging and patient management system featuring a Vue3 3D viewer frontend, a Spring Boot core backend, a Python service, and a custom fine-tuned local AI model for Retrieval-Augmented Generation (RAG).

## 📁 Project Directory Structure
* `/neusoft-project-frontend` - Vue.js & Three.js frontend for UI and 3D visualization.
* `/neusoft-spring-boot-backend` - Java Spring Boot core API and database management.
* `/neusoft-python-backend` - Python service handling auxiliary AI processing.
* `/neusoft-mysql-database` - Contains the MySQL export file (`neusoft_db.sql`).
* `/neusoft-fine-tuned-RAG-model` - Contains the custom `.gguf` weights, `Modelfile`, and training results for the RAG model.

---

## ⚙️ 1. System Prerequisites
Please ensure the following are installed on your environment before running the project:
* **Java:** JDK 21
* **Node.js:** v20+
* **Python:** 3.10+
* **MySQL Server:** 8.0+ 
* **Ollama:** Required for local AI capabilities ([Download Ollama](https://ollama.com/download))

---

## 🗄️ 2. Step 1: Database Setup (MySQL)
The Spring Boot backend is configured to connect to a local MySQL instance using specific credentials.

1. Open your database management tool (e.g., Navicat, MySQL Workbench).
2. Ensure your local MySQL uses the following credentials:
   * **Host/Port:** `localhost:3306`
   * **Username:** `root`
   * **Password:** `hakro123`
3. Create a new database named exactly: `neusoft_db`
4. Import the provided SQL file located at `neusoft-mysql-database/neusoft_db.sql`.

---

## 🧠 3. Step 2: Local AI Model Setup (Ollama)
This system relies on a custom fine-tuned model (`neusoft-ai`) specifically trained for medical RAG context, and an embedding model for document processing.

**A. Build the Custom Medical Model:**
Open a terminal, navigate to the fine-tuned model folder, and build the custom AI using the provided `.gguf` weights and Modelfile:
\`\`\`bash
cd neusoft-fine-tuned-RAG-model
ollama create neusoft-ai -f Modelfile
\`\`\`
*(Optional Test: Run `ollama run neusoft-ai "How do I register as a doctor?"` to verify. Type `/bye` to exit. You can also view the `neusoft_training_results.png` in this folder for model performance metrics).*

**B. Pull the Embedding Model:**
Open your terminal and pull the embedding model used for reading large document data:
\`\`\`bash
ollama pull mxbai-embed-large
\`\`\`
*(Ensure Ollama remains running in the background at `http://localhost:11434` before proceeding to the next steps).*

**Hardware & Performance Notes:**
* The custom model was trained and deployed on an **RTX 5060 (Lenovo Legion 7000y)**.
* **File Size:** ~4.8 GB | **Context Window:** 4096 tokens | **Inference Speed:** ~20 tok/s.

---

## 🚀 4. Step 3: Running the Application Services
To run the full system, open **three separate terminal windows**:

### Terminal 1: Python AI Backend
Handles specialized Python AI tasks and integrations.
\`\`\`bash
cd neusoft-python-backend
pip install -r requirements.txt
python app.py
\`\`\`

### Terminal 2: Spring Boot Backend
The core backend processing database logic, file uploads (configured for large 50MB files), and the AI ChatController.
\`\`\`bash
cd neusoft-spring-boot-backend
./mvnw clean install
./mvnw spring-boot:run
\`\`\`
*(Note: The Spring Boot server is configured to run on **Port 8081**).*

### Terminal 3: Vue3 Frontend
The main user interface featuring the Three.js medical viewer and chat UI.
\`\`\`bash
cd neusoft-project-frontend
npm install
npm run dev
\`\`\`
*(The frontend will be accessible in your browser at **`http://localhost:5173`**).*

---

## 🛠️ 5. Architectural Highlight: How the AI Integration Works (For Grading)
The Spring Boot controller (`/api/chat/ask`) handles advanced AI logic to connect the LLM with the frontend UI seamlessly:
1. **Context Injection:** It fetches the patient's real medical records from the MySQL database and dynamically injects them into the AI's System Prompt.
2. **Strict Guardrails:** The model is strictly instructed to *only* answer using the provided patient records to prevent AI hallucinations.
3. **UI Automation:** If the patient asks to view a specific document or scan, the AI is trained to output a specific command (`SHOW_REPORT:<id>`). Spring Boot intercepts this command, removes it from the chat text, and sends an `action` payload to the Vue3 frontend to automatically open the requested medical scan.