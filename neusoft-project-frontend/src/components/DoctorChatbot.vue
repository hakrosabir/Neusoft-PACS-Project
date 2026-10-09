<template>
  <div>
    <button class="chat-fab doctor-fab" @click="toggleChat">
      <span v-if="!isOpen">🩺 AI Copilot</span>
      <span v-else>✕</span>
    </button>

    <div v-if="isOpen" class="chat-interface animate-slide-up">
      
      <div class="chat-header doctor-header">
        <div class="bot-icon">🧬</div>
        <div class="header-info">
          <h4>Diagnostic Assistant</h4>
          <span class="secure-badge">
            <span v-if="activePatient">Context: {{ activePatient.patientName }}</span>
            <span v-else>RAG + TotalSegmentator</span>
          </span>
        </div>
      </div>
      
      <div class="chat-messages" ref="chatBodyRef">
        <div v-for="(msg, i) in messages" :key="i" :class="['message', msg.sender]">
          <div class="bubble" v-html="formatMessage(msg.text)"></div>
          <div v-if="msg.file" class="file-attachment">
            📎 {{ msg.file.name }} uploaded
          </div>
        </div>
        
        <div v-if="isTyping" class="message bot">
          <div class="bubble typing">
            <span v-if="isProcessingFile">{{ processStatusText }}</span>
            <span v-else>Thinking...</span>
          </div>
        </div>
      </div>

      <div class="chat-input-area">
        <label for="chat-file-upload" class="attach-btn" title="Upload Scan or PDF">📎</label>
        <input 
          id="chat-file-upload" 
          type="file" 
          @change="handleFileUpload" 
          hidden 
          accept=".pdf,.nrrd,.dcm,.png,.jpg,.jpeg,.zip,.nii,.nii.gz"
        >
        
        <input 
          v-model="userQuery" 
          @keyup.enter="sendToAI" 
          type="text" 
          :placeholder="getInputPlaceholder()" 
          :disabled="isTyping"
        />
        
        <button @click="sendToAI" :disabled="(!userQuery && !selectedFile) || isTyping">➤</button>
      </div>
      
      <div v-if="selectedFile" class="file-preview">
        <span>📄 {{ selectedFile.name }}</span>
        <button @click="clearFile" class="clear-file">×</button>
      </div>

    </div>
  </div>
</template>

<script setup>
import { JAVA_API_URL, PYTHON_API_URL } from '../config.js'
import { ref, nextTick, onBeforeUnmount } from 'vue';
import axios from 'axios';

// --- PROPS ---
const props = defineProps({
  user: { type: Object, required: true },
  activePatient: { type: Object, default: null } 
});

// --- CONFIG ---
const API_BASE = JAVA_API_URL; // Spring Boot (RAG)
const PYTHON_BASE = PYTHON_API_URL; // Python (Segmentation)

// --- STATE ---
const isOpen = ref(false);
const messages = ref([]);
const userQuery = ref('');
const isTyping = ref(false);
const isProcessingFile = ref(false);
const processStatusText = ref("Processing...");
const chatBodyRef = ref(null);
const selectedFile = ref(null);
const caseId = ref("");
let pollTimer = null;

// --- LOGIC ---

function toggleChat() {
  isOpen.value = !isOpen.value;
  if (isOpen.value && messages.value.length === 0) {
    messages.value.push({ sender: 'bot', text: "Hello Doctor. Upload a <b>PDF</b> to ask questions, or a <b>CT Scan (NII/NRRD)</b> for segmentation." });
  }
  scrollToBottom();
}

function handleFileUpload(event) {
  if (event.target.files && event.target.files[0]) {
    selectedFile.value = event.target.files[0];
  }
}

function clearFile() {
  selectedFile.value = null;
  // Reset file input value so same file can be selected again if needed
  const fileInput = document.getElementById('chat-file-upload');
  if (fileInput) fileInput.value = '';
}

function getInputPlaceholder() {
  if (selectedFile.value) {
    return selectedFile.value.name.endsWith('.pdf') ? "PDF ready. Press enter to ingest." : "Scan ready. Press enter to segment.";
  }
  return "Ask about the uploaded PDF...";
}

const formatMessage = (text) => text.replace(/\*\*(.*?)\*\*/g, '<b>$1</b>').replace(/\n/g, '<br>');

const scrollToBottom = () => nextTick(() => {
  if (chatBodyRef.value) chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight;
});

// --- MAIN AI HANDLER ---
async function sendToAI() {
  if (!userQuery.value.trim() && !selectedFile.value) return;

  const text = userQuery.value;
  const fileToSend = selectedFile.value;

  // 1. Add User Message to Chat
  messages.value.push({ 
    sender: 'user', 
    text: text || (fileToSend ? `Processing ${fileToSend.name}` : ''), 
    file: fileToSend 
  });
  
  // Clear inputs
  userQuery.value = '';
  clearFile();
  
  isTyping.value = true;
  scrollToBottom();

  // 2. ROUTING LOGIC
  
  // CASE A: File is PDF -> Send to Spring Boot RAG
  if (fileToSend && fileToSend.name.toLowerCase().endsWith('.pdf')) {
    await ingestPDF(fileToSend);
    return;
  }

  // CASE B: File is Medical Scan -> Send to Python TotalSegmentator
  if (fileToSend) {
    await startSegmentation(fileToSend);
    return;
  }

  // CASE C: Text Only -> Ask Spring Boot RAG
  if (text) {
    await askRAG(text);
  }
}

// --- JAVA BACKEND (RAG) FUNCTIONS ---

async function ingestPDF(file) {
  isProcessingFile.value = true;
  processStatusText.value = "Reading PDF...";
  
  try {
    const formData = new FormData();
    formData.append("file", file);
    
    // Call the new Spring AI endpoint
    await axios.post(`${API_BASE}/api/rag/upload`, formData);
    
    messages.value.push({ sender: 'bot', text: "✅ PDF Analyzed. You can now ask me questions about this document." });
  } catch (err) {
    messages.value.push({ sender: 'bot', text: "❌ Error reading PDF. Make sure Spring Boot is running." });
    console.error(err);
  } finally {
    isTyping.value = false;
    isProcessingFile.value = false;
    scrollToBottom();
  }
}

async function askRAG(question) {
  try {
    const payload = { message: question };
    
    // Call the new Spring AI endpoint
    const res = await axios.post(`${API_BASE}/api/rag/ask`, payload);
    
    const aiReply = res.data.reply || "I couldn't find an answer in the document.";
    messages.value.push({ sender: 'bot', text: aiReply });
  } catch (err) {
    messages.value.push({ sender: 'bot', text: "❌ Error connecting to AI Service." });
    console.error(err);
  } finally {
    isTyping.value = false;
    scrollToBottom();
  }
}

// --- PYTHON BACKEND (SEGMENTATION) FUNCTIONS ---

async function startSegmentation(file) {
  isProcessingFile.value = true;
  processStatusText.value = "Starting TotalSegmentator...";
  
  try {
    const formData = new FormData();
    formData.append("file", file);
    const startRes = await axios.post(`${PYTHON_BASE}/api/totalseg_start`, formData);
    caseId.value = startRes.data.case_id;
    
    if (!caseId.value) throw new Error("No Case ID returned.");

    processStatusText.value = `Job Started (ID: ${caseId.value}). Processing...`;
    pollTimer = setInterval(checkSegmentationStatus, 3000);
  } catch (err) {
    messages.value.push({ sender: 'bot', text: "Error starting segmentation: " + (err.message || "Backend unreachable.") });
    isTyping.value = false;
    isProcessingFile.value = false;
  }
}

async function checkSegmentationStatus() {
  if (!caseId.value) return;
  try {
    const res = await axios.get(`${PYTHON_BASE}/api/totalseg_status/${caseId.value}`);
    const status = res.data.status;
    
    if (res.data.error) {
      messages.value.push({ sender: 'bot', text: `Segmentation Failed: ${res.data.error}` });
      stopPolling();
    } else if (status === "finished") {
      processStatusText.value = "Downloading results...";
      await downloadSegmentationResult();
      messages.value.push({ sender: 'bot', text: `<b>Segmentation Complete!</b><br>Case ID: ${caseId.value}<br>Result ZIP has been downloaded.` });
      stopPolling();
    } else {
      processStatusText.value = `Processing... (${status})`;
    }
  } catch (err) {
    stopPolling();
  }
}

async function downloadSegmentationResult() {
  try {
    const res = await axios.get(`${PYTHON_BASE}/api/totalseg_download/${caseId.value}`, { responseType: "blob" });
    const blob = new Blob([res.data]);
    const link = document.createElement("a");
    link.href = URL.createObjectURL(blob);
    link.download = `Results_${caseId.value}.zip`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  } catch (err) { console.error(err); }
}

function stopPolling() {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null; }
  isTyping.value = false;
  isProcessingFile.value = false;
  caseId.value = "";
  scrollToBottom();
}

onBeforeUnmount(() => stopPolling());
</script>

<style scoped>
/* Copied Chat Styles - No Changes Needed Here */
.chat-fab { position: fixed; bottom: 30px; right: 30px; background: #2563eb; color: white; padding: 12px 20px; border-radius: 30px; border: none; font-weight: 600; cursor: pointer; box-shadow: 0 4px 15px rgba(37, 99, 235, 0.4); z-index: 1000; }
.chat-interface { position: fixed; bottom: 90px; right: 30px; width: 380px; height: 500px; background: white; border-radius: 16px; box-shadow: 0 10px 40px rgba(0,0,0,0.2); display: flex; flex-direction: column; overflow: hidden; z-index: 999; border: 1px solid #e2e8f0; }
.doctor-header { background: #4f46e5; color: white; padding: 15px; display: flex; align-items: center; gap: 12px; }
.bot-icon { font-size: 1.5rem; background: rgba(255,255,255,0.2); width: 40px; height: 40px; display: flex; align-items: center; justify-content: center; border-radius: 50%; }
.header-info h4 { margin: 0; font-size: 1rem; }
.secure-badge { font-size: 0.7rem; background: rgba(255,255,255,0.2); padding: 2px 8px; border-radius: 10px; margin-top: 4px; display: inline-block; }
.chat-messages { flex: 1; padding: 15px; overflow-y: auto; background: #f8fafc; display: flex; flex-direction: column; gap: 10px; }
.message .bubble { padding: 10px 14px; border-radius: 12px; max-width: 85%; font-size: 0.9rem; line-height: 1.5; }
.message.user { align-self: flex-end; }
.message.user .bubble { background: #2563eb; color: white; border-bottom-right-radius: 2px; }
.message.bot { align-self: flex-start; }
.message.bot .bubble { background: white; border: 1px solid #e2e8f0; color: #334155; border-bottom-left-radius: 2px; box-shadow: 0 1px 2px rgba(0,0,0,0.05); }
.file-attachment { font-size: 0.8rem; color: #2563eb; background: #e0e7ff; padding: 4px 8px; border-radius: 4px; margin-top: 5px; display: inline-block; border: 1px solid #c7d2fe; }
.chat-input-area { padding: 15px; background: white; border-top: 1px solid #e2e8f0; display: flex; gap: 10px; align-items: center; }
.chat-input-area input { flex: 1; padding: 12px; border: 1px solid #cbd5e1; border-radius: 20px; outline: none; background: #f8fafc; }
.chat-input-area input:focus { border-color: #4f46e5; background: white; }
.chat-input-area button { background: #4338ca; color: white; border: none; width: 42px; height: 42px; border-radius: 50%; cursor: pointer; display: flex; align-items: center; justify-content: center; }
.attach-btn { font-size: 1.5rem; cursor: pointer; color: #64748b; padding: 0 5px; }
.file-preview { padding: 8px 15px; background: #eef2ff; border-top: 1px solid #dbeafe; display: flex; justify-content: space-between; align-items: center; }
.clear-file { background: none; border: none; font-size: 1.2rem; cursor: pointer; color: #ef4444; }
.typing span { animation: blink 1.4s infinite both; }
.animate-slide-up { animation: slideUp 0.3s ease-out; }
@keyframes slideUp { from { transform: translateY(20px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }
@keyframes blink { 0% { opacity: 0.2; } 20% { opacity: 1; } 100% { opacity: 0.2; } }
</style>