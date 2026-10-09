<template>
  <div class="nurse-layout">
    
    <aside class="sidebar">
      <div class="brand-section">
        <div class="logo-box">T</div>
        <div class="brand-info">
          <span class="brand-name">NeuroPACS</span>
          <span class="brand-sub">Tech Console</span>
        </div>
      </div>

      <nav class="nav-links">
        <button 
          v-for="tab in tabs" 
          :key="tab.id" 
          @click="currentTab = tab.id"
          :class="['nav-item', { active: currentTab === tab.id }]"
        >
          <span class="icon">{{ tab.icon }}</span>
          <span class="label">{{ tab.name }}</span>
        </button>
      </nav>

      <div class="user-section">
        <div class="user-info">
          <div class="avatar-circle nurse">N</div>
          <div class="user-text">
            <span class="dr-name">{{ user.username || 'Nurse Joy' }}</span>
            <span class="dr-role">Technician</span>
          </div>
        </div>
        <button @click="logout" class="logout-btn" title="Sign Out">➜</button>
      </div>
    </aside>

    <main class="main-content">
      <header class="top-header">
        <div class="header-left">
          <h1>Exam Control</h1>
          <div class="floor-status">
            <span class="status-pill green">MRI Room 1: <strong>READY</strong></span>
            <span class="status-pill orange">CT Room 2: <strong>IN USE</strong></span>
          </div>
        </div>
        <div class="header-right">
          <span class="shift-timer">Shift Time: 04:12:00</span>
          <button class="refresh-btn" @click="fetchWorklist">↻ Refresh List</button>
        </div>
      </header>

      <div class="content-body">

        <div v-if="currentTab === 'worklist'" class="tab-view fade-in">
          
          <div class="kpi-row">
            <div class="kpi-card">
              <div class="kpi-icon">📋</div>
              <div><h3>{{ worklist.length }}</h3><span>Waiting Patients</span></div>
            </div>
            <div class="kpi-card">
              <div class="kpi-icon">⚡</div>
              <div><h3>{{ urgentCount }}</h3><span>STAT / Urgent</span></div>
            </div>
            <div class="kpi-card">
              <div class="kpi-icon">✅</div>
              <div><h3>12</h3><span>Completed Today</span></div>
            </div>
          </div>

          <div class="list-container">
            <div class="list-header">
              <h3>Today's Schedule</h3>
              <input type="text" v-model="searchQuery" placeholder="Search Patient ID..." class="search-input">
            </div>

            <table>
              <thead>
                <tr>
                  <th>Time</th>
                  <th>Patient</th>
                  <th>Exam Type</th>
                  <th>Priority</th>
                  <th>Status</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="filteredWorklist.length === 0"><td colspan="6" class="empty-cell">No patients in queue.</td></tr>
                <tr v-for="item in filteredWorklist" :key="item.id" :class="{ 'urgent-row': item.priority === 'URGENT' }">
                  <td class="mono">{{ item.timeSlot }}</td>
                  <td>
                    <div class="pat-cell">
                      <div class="pat-avatar">{{ item.patientName.charAt(0) }}</div>
                      <div>
                        <span class="pat-name">{{ item.patientName }}</span>
                        <span class="pat-id">ID: {{ item.patientId }}</span>
                      </div>
                    </div>
                  </td>
                  <td>{{ item.department }}</td>
                  <td>
                    <span v-if="item.priority === 'URGENT'" class="badge red">⚡ STAT</span>
                    <span v-else class="badge gray">Routine</span>
                  </td>
                  <td>
                    <span :class="['status-dot', getStatusColor(item.status)]"></span> {{ item.status }}
                  </td>
                  <td>
                    <button v-if="['ACCEPTED', 'CHECKED_IN'].includes(item.status)" @click="openExamModal(item)" class="action-btn start">
                      ▶ Start Exam
                    </button>
                    <button v-else-if="item.status === 'SCANNED'" disabled class="action-btn done">
                      ✔ Sent to Dr.
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div v-if="currentTab === 'schedule'" class="tab-view fade-in">
          <div class="schedule-grid">
            <div v-for="hour in 9" :key="hour" class="time-slot">
              <div class="time-label">{{ 8 + hour }}:00</div>
              <div class="slot-content">
                <div v-if="getAptAt(8+hour)" class="apt-block">
                  {{ getAptAt(8+hour).patientName }} - {{ getAptAt(8+hour).department }}
                </div>
                <div v-else class="free-slot">Available</div>
              </div>
            </div>
          </div>
        </div>

      </div>
    </main>

    <div v-if="showExamModal" class="modal-overlay">
      <div class="modal-card">
        <div class="modal-header">
          <h3>Perform Examination</h3>
          <button @click="showExamModal = false" class="close-btn">×</button>
        </div>
        
        <div class="modal-body">
          <div class="patient-banner">
            <div class="banner-info">
              <h4>{{ selectedPatient.patientName }}</h4>
              <span>{{ selectedPatient.department }} • {{ selectedPatient.priority || 'Routine' }}</span>
            </div>
            <div class="banner-id">ID: {{ selectedPatient.patientId }}</div>
          </div>

          <div class="section-block">
            <h4>1. Vitals & Intake</h4>
            <div class="vitals-grid">
              <div class="input-wrap">
                <label>BP (mmHg)</label>
                <input v-model="examData.bp" placeholder="120/80">
              </div>
              <div class="input-wrap">
                <label>Heart Rate</label>
                <input v-model="examData.hr" placeholder="72">
              </div>
              <div class="input-wrap">
                <label>Weight (kg)</label>
                <input v-model="examData.weight" placeholder="70">
              </div>
              <div class="input-wrap">
                <label>Notes</label>
                <input v-model="examData.notes" placeholder="Patient anxious...">
              </div>
            </div>
          </div>

          <div class="section-block">
            <h4>2. Image Acquisition</h4>
            
            <div class="upload-source-toggle">
              <button 
                :class="['toggle-btn', { active: uploadSource === 'local' }]" 
                @click="uploadSource = 'local'"
              >
                📂 Local Upload
              </button>
              <button 
                :class="['toggle-btn', { active: uploadSource === 'pacs' }]" 
                @click="uploadSource = 'pacs'"
              >
                🏥 PACS Query
              </button>
            </div>

            <div v-if="uploadSource === 'local'">
              <div 
                class="drop-zone" 
                @click="triggerFileSelect"
                :class="{ 'has-file': uploadFiles && uploadFiles.length > 0 }"
              >
                <input 
                  type="file" 
                  ref="fileInput" 
                  hidden 
                  @change="handleFileSelect" 
                  multiple
                  webkitdirectory
                >
                
                <div v-if="!uploadFiles || uploadFiles.length === 0">
                  <span class="icon">☁️</span>
                  <p>Click to upload scan folder (DICOM/NIfTI)</p>
                  <small class="sub-text">Supported: Folder with .dcm series, or .nii/.png files</small>
                </div>
                
                <div v-else class="file-info">
                  <span class="icon">📂</span>
                  <p>{{ uploadFiles.length }} File(s) Selected</p>
                  <small>Ready to transfer</small>
                </div>
              </div>
            </div>

            <div v-else class="pacs-panel">
              <div class="input-wrap">
                <label>Study Instance UID / Accession #</label>
                <input v-model="pacsQueryId" placeholder="1.2.840.10008...">
              </div>
              <button class="btn-secondary" @click="mockPacsQuery" :disabled="!pacsQueryId">
                🔍 Query Node
              </button>
            </div>

            <div v-if="isUploading" class="progress-bar-container">
              <div class="progress-info">
                <span>Transferring...</span>
                <span>{{ uploadProgress }}%</span>
              </div>
              <div class="progress-bar">
                <div class="fill" :style="{ width: uploadProgress + '%' }"></div>
              </div>
            </div>
          </div>

        </div>

        <div class="modal-footer">
          <button @click="showExamModal = false" class="btn-cancel">Cancel</button>
          <button @click="submitExam" class="btn-submit" :disabled="isUploading || (!uploadFiles && uploadSource === 'local')">
            {{ isUploading ? 'Processing...' : 'Complete Exam & Send' }}
          </button>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { JAVA_API_URL, PYTHON_API_URL, SCAN_INPUT_DIR } from '../config.js'
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()
const user = ref(JSON.parse(localStorage.getItem('user') || '{}'))
const currentTab = ref('worklist')
const searchQuery = ref('')
const worklist = ref([])

// Modal State
const showExamModal = ref(false)
const selectedPatient = ref({})
const examData = ref({ bp: '', hr: '', weight: '', notes: '' })

// Upload State
const uploadSource = ref('local')
const uploadFiles = ref(null)     
const pacsQueryId = ref('')
const isUploading = ref(false)
const uploadProgress = ref(0)
const fileInput = ref(null)

const tabs = [
  { id: 'worklist', name: 'Worklist', icon: '📋' },
  { id: 'schedule', name: 'Schedule', icon: '📅' },
]

const API_BASE = JAVA_API_URL;
const PYTHON_BASE = PYTHON_API_URL;

// ** TARGET FOLDER CONFIGURATION **
// Note: We send this path string to Python. Python must use it to save the files.
const TARGET_PATH = SCAN_INPUT_DIR;

// --- ACTIONS ---

async function fetchWorklist() {
  try {
    const res = await axios.get(`${API_BASE}/appointments/all`);
    worklist.value = res.data.filter(a => ['ACCEPTED', 'CHECKED_IN', 'SCANNED'].includes(a.status));
  } catch (e) {
    console.error("Fetch failed", e);
    mockWorklist();
  }
}

function mockWorklist() {
  worklist.value = [
    { id: 101, timeSlot: '09:00', patientName: 'John Doe', patientId: 'P-101', department: 'MRI / Brain', status: 'ACCEPTED', priority: 'ROUTINE' },
    { id: 102, timeSlot: '09:30', patientName: 'Jane Smith', patientId: 'P-102', department: 'CT / Chest', status: 'ACCEPTED', priority: 'URGENT' },
  ];
}

function openExamModal(item) {
  selectedPatient.value = item;
  examData.value = { bp: '', hr: '', weight: '', notes: '' };
  uploadFiles.value = null;
  pacsQueryId.value = '';
  uploadProgress.value = 0;
  showExamModal.value = true;
}

const triggerFileSelect = () => fileInput.value.click();
const handleFileSelect = (e) => { 
  uploadFiles.value = e.target.files; 
};

function mockPacsQuery() {
  alert("PACS Query Initiated...");
}

// --- SUBMIT FUNCTION (Fixed for Folder Upload) ---
async function submitExam() {
  if (uploadSource.value === 'local' && (!uploadFiles.value || uploadFiles.value.length === 0)) {
    return alert("Please select a folder or files.");
  }
  
  isUploading.value = true;
  
  try {
    const fd = new FormData();
    
    // 1. Send Target Path to Python
    fd.append('target_path', TARGET_PATH);
    fd.append('patient_id', selectedPatient.value.patientId);

    if (uploadSource.value === 'local') {
      // Append all files from the selected folder
      for(let i = 0; i < uploadFiles.value.length; i++){
        fd.append('files', uploadFiles.value[i]);
      }
      
      // Progress Simulation
      const interval = setInterval(() => { if(uploadProgress.value < 90) uploadProgress.value += 10; }, 200);

      // Call the Python Endpoint (Must match your ImageConverter's logic)
      // Using /upload-folder logic because scans are often folders
      await axios.post(`${PYTHON_BASE}/upload_scan`, fd, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      
      clearInterval(interval);
      uploadProgress.value = 100;
    } 
    
    // 2. Update Java Backend with Metadata
    const fileNameRef = (uploadFiles.value && uploadFiles.value.length > 0) ? uploadFiles.value[0].name : 'PACS-DATA';
    
    const payload = {
      scanFile: fileNameRef, // Just a reference name for the DB
      nurseNotes: `BP: ${examData.value.bp}, HR: ${examData.value.hr}, Weight: ${examData.value.weight}. Notes: ${examData.value.notes}`
    };

    await axios.put(`${API_BASE}/appointments/${selectedPatient.value.id}/complete-scan`, payload);

    alert("Exam Completed. Data sent to Radiologist.");
    showExamModal.value = false;
    fetchWorklist(); 

  } catch (e) {
    console.error(e);
    alert("Upload Failed: " + (e.response?.data?.message || e.message));
  } finally {
    isUploading.value = false;
  }
}

function logout() { localStorage.removeItem('user'); router.push('/'); }

// --- COMPUTED ---
const filteredWorklist = computed(() => {
  return worklist.value.filter(item => 
    item.patientName.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
    item.patientId.toLowerCase().includes(searchQuery.value.toLowerCase())
  );
});

const urgentCount = computed(() => worklist.value.filter(i => i.priority === 'URGENT' && i.status !== 'SCANNED').length);

const getStatusColor = (s) => {
  if(s === 'ACCEPTED' || s === 'CHECKED_IN') return 'yellow';
  if(s === 'SCANNED') return 'green';
  return 'gray';
}

const getAptAt = (hour) => {
  return worklist.value.find(a => parseInt(a.timeSlot.split(':')[0]) === hour);
}

onMounted(() => fetchWorklist());
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap');
@import url('https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400&display=swap');

/* LAYOUT */
.nurse-layout { display: flex; height: 100vh; background-color: #f0fdfa; font-family: 'Inter', sans-serif; color: #0f172a; }
.sidebar { width: 260px; background-color: #115e59; color: white; display: flex; flex-direction: column; } 
.brand-section { height: 70px; display: flex; align-items: center; padding: 0 24px; gap: 12px; background: rgba(0,0,0,0.1); }
.logo-box { width: 36px; height: 36px; background: white; color: #115e59; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-weight: 800; font-size: 1.2rem; }
.brand-info { display: flex; flex-direction: column; }
.brand-name { font-weight: 700; font-size: 1rem; }
.brand-sub { font-size: 0.7rem; opacity: 0.8; letter-spacing: 1px; text-transform: uppercase; }

.nav-links { flex: 1; padding: 20px 0; }
.nav-item { display: flex; align-items: center; width: 100%; padding: 14px 24px; background: none; border: none; color: #ccfbf1; cursor: pointer; transition: 0.2s; font-size: 0.95rem; text-align: left; gap: 12px; }
.nav-item:hover { background: rgba(255,255,255,0.1); color: white; }
.nav-item.active { background: #0d9488; color: white; border-right: 4px solid white; }

.user-section { padding: 20px; border-top: 1px solid rgba(255,255,255,0.1); display: flex; align-items: center; justify-content: space-between; }
.avatar-circle.nurse { width: 40px; height: 40px; background: white; color: #115e59; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: bold; }
.user-text { display: flex; flex-direction: column; margin-left: 10px; color: white; }
.dr-name { font-weight: 600; font-size: 0.9rem; }
.dr-role { font-size: 0.7rem; opacity: 0.8; }
.logout-btn { background: none; border: none; color: #ccfbf1; cursor: pointer; font-size: 1.2rem; }

.main-content { flex: 1; display: flex; flex-direction: column; overflow: hidden; background: #f0fdfa; }
.top-header { height: 70px; background: white; border-bottom: 1px solid #e2e8f0; display: flex; justify-content: space-between; align-items: center; padding: 0 30px; }
.header-left h1 { margin: 0; font-size: 1.5rem; font-weight: 700; color: #115e59; }
.floor-status { display: flex; gap: 10px; margin-top: 5px; }
.status-pill { font-size: 0.75rem; padding: 2px 8px; border-radius: 12px; border: 1px solid; }
.status-pill.green { background: #dcfce7; color: #166534; border-color: #86efac; }
.status-pill.orange { background: #ffedd5; color: #9a3412; border-color: #fdba74; }

.header-right { display: flex; align-items: center; gap: 20px; }
.shift-timer { font-family: 'JetBrains Mono', monospace; font-size: 0.9rem; color: #64748b; }
.refresh-btn { background: #f0f9ff; color: #0284c7; border: 1px solid #bae6fd; padding: 8px 16px; border-radius: 6px; cursor: pointer; font-weight: 600; transition: 0.2s; }
.refresh-btn:hover { background: #e0f2fe; }

.content-body { flex: 1; overflow-y: auto; padding: 30px; }

/* WORKLIST */
.kpi-row { display: flex; gap: 20px; margin-bottom: 30px; }
.kpi-card { flex: 1; background: white; padding: 20px; border-radius: 12px; display: flex; align-items: center; gap: 15px; box-shadow: 0 2px 5px rgba(0,0,0,0.05); border: 1px solid #e2e8f0; }
.kpi-icon { width: 50px; height: 50px; background: #ccfbf1; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 1.5rem; }
.kpi-card h3 { margin: 0; font-size: 1.8rem; color: #115e59; }
.kpi-card span { color: #64748b; font-size: 0.9rem; }

.list-container { background: white; border-radius: 12px; padding: 5px; border: 1px solid #e2e8f0; }
.list-header { padding: 15px 20px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #e2e8f0; }
.search-input { padding: 8px 12px; border: 1px solid #cbd5e1; border-radius: 6px; width: 250px; outline: none; }

table { width: 100%; border-collapse: collapse; }
th { text-align: left; padding: 15px 20px; background: #f8fafc; color: #64748b; font-size: 0.85rem; text-transform: uppercase; font-weight: 600; }
td { padding: 15px 20px; border-bottom: 1px solid #e2e8f0; }
.urgent-row { background: #fff7ed; border-left: 4px solid #f97316; }

.pat-cell { display: flex; align-items: center; gap: 12px; }
.pat-avatar { width: 36px; height: 36px; background: #e2e8f0; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: 700; color: #475569; }
.pat-name { font-weight: 600; display: block; color: #0f172a; }
.pat-id { font-size: 0.8rem; color: #64748b; }
.badge { padding: 4px 10px; border-radius: 20px; font-size: 0.75rem; font-weight: 700; }
.badge.red { background: #fee2e2; color: #ef4444; }
.badge.gray { background: #f1f5f9; color: #64748b; }
.status-dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; margin-right: 5px; }
.status-dot.yellow { background: #eab308; } .status-dot.green { background: #22c55e; } .status-dot.gray { background: #cbd5e1; }

.action-btn { padding: 8px 16px; border-radius: 6px; border: none; font-weight: 600; cursor: pointer; transition: 0.2s; font-size: 0.85rem; }
.action-btn.start { background: #0d9488; color: white; } .action-btn.start:hover { background: #115e59; }
.action-btn.done { background: #e2e8f0; color: #94a3b8; cursor: not-allowed; }

/* MODAL */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal-card { background: white; width: 600px; border-radius: 12px; box-shadow: 0 10px 40px rgba(0,0,0,0.2); overflow: hidden; display: flex; flex-direction: column; max-height: 90vh; }
.modal-header { padding: 20px; border-bottom: 1px solid #e2e8f0; display: flex; justify-content: space-between; align-items: center; background: #f8fafc; }
.close-btn { background: none; border: none; font-size: 1.5rem; cursor: pointer; }
.modal-body { padding: 25px; overflow-y: auto; }

.patient-banner { background: #f0fdfa; border: 1px solid #ccfbf1; padding: 15px; border-radius: 8px; display: flex; justify-content: space-between; align-items: center; margin-bottom: 25px; }
.banner-info h4 { margin: 0; color: #115e59; }
.banner-id { font-family: 'JetBrains Mono', monospace; font-weight: 700; color: #0d9488; }

.section-block { margin-bottom: 25px; }
.section-block h4 { border-bottom: 2px solid #0d9488; display: inline-block; padding-bottom: 5px; margin-bottom: 15px; color: #0f172a; }

.vitals-grid { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 15px; }
.vitals-grid .input-wrap:last-child { grid-column: span 3; }
.input-wrap label { display: block; font-size: 0.85rem; font-weight: 500; margin-bottom: 5px; color: #475569; }
.input-wrap input { width: 100%; padding: 10px; border: 1px solid #cbd5e1; border-radius: 6px; outline: none; }
.input-wrap input:focus { border-color: #0d9488; }

/* NEW UPLOAD STYLES */
.upload-source-toggle { display: flex; gap: 10px; margin-bottom: 15px; }
.toggle-btn { flex: 1; padding: 10px; border: 1px solid #cbd5e1; background: white; border-radius: 6px; cursor: pointer; font-weight: 600; color: #64748b; }
.toggle-btn.active { background: #0d9488; color: white; border-color: #0d9488; }

.pacs-panel { background: #f8fafc; padding: 15px; border-radius: 8px; border: 1px solid #e2e8f0; }
.pacs-status { margin: 10px 0; font-size: 0.9rem; }
.text-green { color: #166534; font-weight: 600; }
.text-blue { color: #0284c7; font-weight: 600; }
.btn-secondary { background: white; border: 1px solid #cbd5e1; padding: 8px 12px; border-radius: 6px; cursor: pointer; }

.drop-zone { border: 2px dashed #cbd5e1; border-radius: 8px; padding: 30px; text-align: center; cursor: pointer; transition: 0.2s; background: #f8fafc; }
.drop-zone:hover { border-color: #0d9488; background: #f0fdfa; }
.drop-zone.has-file { border-style: solid; border-color: #0d9488; background: #f0fdfa; }
.icon { font-size: 2rem; display: block; margin-bottom: 10px; }
.sub-text { color: #94a3b8; display: block; margin-top: 5px; font-size: 0.8rem; }

.progress-bar-container { margin-top: 20px; }
.progress-info { display: flex; justify-content: space-between; font-size: 0.85rem; margin-bottom: 5px; color: #0f172a; font-weight: 600; }
.progress-bar { height: 8px; background: #e2e8f0; border-radius: 4px; overflow: hidden; }
.fill { height: 100%; background: #0d9488; transition: width 0.2s; }

.modal-footer { padding: 20px; border-top: 1px solid #e2e8f0; display: flex; justify-content: flex-end; gap: 10px; }
.btn-cancel { background: white; border: 1px solid #cbd5e1; padding: 10px 20px; border-radius: 6px; cursor: pointer; }
.btn-submit { background: #0d9488; color: white; border: none; padding: 10px 20px; border-radius: 6px; cursor: pointer; font-weight: 600; }
.btn-submit:disabled { background: #9ca3af; cursor: not-allowed; }

/* SCHEDULE */
.schedule-grid { display: grid; gap: 10px; }
.time-slot { display: flex; border: 1px solid #e2e8f0; border-radius: 8px; overflow: hidden; background: white; }
.time-label { background: #f1f5f9; padding: 15px; width: 80px; font-weight: 700; color: #64748b; display: flex; align-items: center; justify-content: center; }
.slot-content { padding: 10px; flex: 1; display: flex; align-items: center; }
.apt-block { background: #ccfbf1; color: #115e59; padding: 8px 12px; border-radius: 6px; font-weight: 600; width: 100%; }
.free-slot { color: #94a3b8; font-style: italic; }

.fade-in { animation: fadeIn 0.4s ease-out; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
</style>