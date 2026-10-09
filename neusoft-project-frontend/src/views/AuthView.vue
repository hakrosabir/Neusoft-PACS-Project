You are absolutely right. The logo badge was sitting in its own block above the text. I have fixed this by wrapping them in a flex container so the **"+" Logo** and **"Neusoft PACS"** title sit perfectly **side-by-side** on the same line.

Here is the fixed code:

```vue
<template>
  <div class="split-screen">
    
    <div class="brand-panel">
      <div class="brand-content">
        
        <div class="brand-header">
          <div class="logo-badge">✚</div>
          <h1>Neusoft PACS</h1>
        </div>
        
        <p class="tagline">Next-Generation AI Diagnostics</p>
        
        <div class="feature-list">
          <div class="feature-item">
            <span class="icon-box">🧠</span>
            <div class="text">
              <strong>Brain Analysis</strong>
              <span>Advanced Neural Mapping</span>
            </div>
          </div>
          <div class="feature-item">
            <span class="icon-box">🫁</span>
            <div class="text">
              <strong>Lungs Analysis</strong>
              <span>Precision Pulmonary CT</span>
            </div>
          </div>
          <div class="feature-item">
            <span class="icon-box">🛡️</span>
            <div class="text">
              <strong>Enterprise Security</strong>
              <span>HIPAA Compliant Storage</span>
            </div>
          </div>
        </div>
      </div>
      <div class="bg-circle c1"></div>
      <div class="bg-circle c2"></div>
    </div>

    <div class="form-panel">
      <div class="form-wrapper">
        
        <div class="form-header">
          <h2>{{ isLogin ? 'Welcome Back' : 'Join the Network' }}</h2>
          <p class="subtitle">{{ isLogin ? 'Access your dashboard securely.' : 'Register for medical access.' }}</p>
        </div>

        <div class="toggle-container">
          <div class="toggle-bg" :style="{ left: isLogin ? '4px' : '50%' }"></div>
          <button @click="toggleMode(true)" :class="{ active: isLogin }">Login</button>
          <button @click="toggleMode(false)" :class="{ active: !isLogin }">Register</button>
        </div>

        <form v-if="isLogin" @submit.prevent="handleLogin" class="animate-fade">
          <div class="input-group">
            <label>Username</label>
            <div class="field-wrap">
              <i class="icon-user">👤</i>
              <input v-model="form.username" type="text" placeholder="Enter your username" required />
            </div>
          </div>

          <div class="input-group">
            <label>Password</label>
            <div class="field-wrap">
              <i class="icon-lock">🔒</i>
              <input v-model="form.password" type="password" placeholder="••••••••" required />
            </div>
          </div>

          <div class="form-extras">
            <label class="custom-checkbox">
              <input type="checkbox">
              <span class="checkmark"></span>
              <span class="label-text">Remember me</span>
            </label>
            <a href="#" class="forgot-link">Forgot Password?</a>
          </div>

          <button class="primary-btn" type="submit" :disabled="isLoading">
            <span v-if="isLoading" class="loader"></span>
            <span v-else>Sign In</span>
          </button>
        </form>

        <form v-else @submit.prevent="handleRegister" class="animate-fade">
          
          <div class="role-grid">
            <div 
              v-for="role in ['PATIENT', 'DOCTOR', 'TECHNICIAN']" 
              :key="role"
              :class="['role-card', { selected: form.role === role }]"
              @click="form.role = role"
            >
              <div class="role-dot"></div>
              <span>{{ role.charAt(0) + role.slice(1).toLowerCase() }}</span>
            </div>
          </div>

          <div class="row">
            <div class="input-group">
              <label>Username</label>
              <input v-model="form.username" placeholder="Username" required />
            </div>
            <div class="input-group">
              <label>Password</label>
              <input v-model="form.password" type="password" placeholder="Password" required />
            </div>
          </div>

          <div class="row">
            <div class="input-group">
              <label>Email</label>
              <input v-model="form.email" type="email" placeholder="user@email.com" required />
            </div>
            <div class="input-group">
              <label>Phone</label>
              <input v-model="form.phoneNumber" type="tel" placeholder="+1 234 567 8900" required />
            </div>
          </div>

          <div v-if="form.role === 'DOCTOR'" class="dynamic-section">
            <div class="row">
              <div class="input-group">
                <label>Specialization</label>
                <div class="select-wrap">
                  <select v-model="form.specialization" required>
                    <option value="" disabled selected>Select Analysis Type</option>
                    <option value="Brain Analysis">Brain Analysis (Neuro)</option>
                    <option value="Lungs Analysis">Lungs Analysis (Pulmo)</option>
                  </select>
                </div>
              </div>
              <div class="input-group">
                <label>License ID</label>
                <input v-model="form.licenseNumber" placeholder="MD-XXXXXX" required />
              </div>
            </div>
          </div>

          <div v-if="form.role === 'TECHNICIAN'" class="dynamic-section">
             <div class="input-group">
              <label>Assigned Department</label>
              <div class="select-wrap">
                <select v-model="form.specialization" required>
                    <option value="" disabled selected>Select Department</option>
                    <option value="Brain Analysis">Brain Analysis Unit</option>
                    <option value="Lungs Analysis">Lungs Analysis Unit</option>
                </select>
              </div>
            </div>
          </div>

          <div v-if="form.role === 'PATIENT'" class="dynamic-section">
            <div class="input-group">
              <label>Insurance ID <span class="opt">(Optional)</span></label>
              <input v-model="form.insuranceId" placeholder="Provider ID" />
            </div>
          </div>

          <button class="primary-btn" type="submit" :disabled="isLoading">
            <span v-if="isLoading" class="loader"></span>
            <span v-else>Create Account</span>
          </button>
        </form>

        <transition name="fade">
          <div v-if="message" class="alert-box" :class="msgType">
            <span class="icon">{{ msgType === 'success' ? '✔' : '⚠' }}</span>
            <span>{{ message }}</span>
          </div>
        </transition>

      </div>
      <p class="footer-copy">© 2025 Neusoft Medical Systems.</p>
    </div>

    <button class="chat-fab" @click="toggleChat">
      <span v-if="!isChatOpen">💬</span>
      <span v-else>✕</span>
    </button>

    <div v-if="isChatOpen" class="chat-window">
      <div class="chat-header">
        <div class="status-dot"></div>
        <span>AI Support Agent</span>
      </div>
      <div class="chat-body" ref="chatBodyRef">
        <div v-for="(msg, index) in chatMessages" :key="index" :class="['chat-bubble', msg.sender]">
          <p v-html="formatMessage(msg.text)"></p>
        </div>
        <div v-if="isBotTyping" class="chat-bubble bot typing">...</div>
      </div>
      <div class="chat-input">
        <input v-model="userQuery" @keyup.enter="sendToAI" placeholder="Type a message..." :disabled="isBotTyping" />
        <button @click="sendToAI" :disabled="!userQuery">➜</button>
      </div>
    </div>

  </div>
</template>

<script setup>
import { JAVA_API_URL } from '../config.js'
import { ref, reactive, nextTick } from 'vue';
import axios from 'axios';
import { useRouter } from 'vue-router';

// --- LOGIC ---
const router = useRouter();
const isLogin = ref(true);
const isLoading = ref(false);
const message = ref('');
const msgType = ref('');

const form = reactive({
  username: '',
  password: '',
  email: '',
  phoneNumber: '',
  role: 'PATIENT',
  specialization: '', 
  licenseNumber: '',
  insuranceId: ''
});

const toggleMode = (loginMode) => {
  isLogin.value = loginMode;
  message.value = '';
  if(!loginMode) {
    form.username = '';
    form.password = '';
    form.email = '';
    form.phoneNumber = '';
    form.specialization = '';
  }
};

const handleLogin = async () => {
  isLoading.value = true;
  message.value = "";
  try {
    const res = await axios.post(`${JAVA_API_URL}/auth/login`, {
      username: form.username,
      password: form.password
    });
    
    const result = res.data; 

    if (result.code !== "200" && result.code !== 200) {
        throw new Error(result.msg || "Login failed");
    }

    const { token, user } = result.data;

    if (user.enabled === false) throw new Error("Account Locked by Admin.");
    if (user.role === 'DOCTOR' && user.verified === false) throw new Error("Pending Verification.");

    localStorage.setItem('token', token);
    localStorage.setItem('user', JSON.stringify(user));

    msgType.value = "success";
    message.value = "Login Successful";
    
    setTimeout(() => {
        if (user.role === 'ADMIN') router.push('/admin');
        else if (user.role === 'DOCTOR') router.push('/doctor-dashboard');
        else if (user.role === 'TECHNICIAN' || user.role === 'NURSE') router.push('/nurse-dashboard');
        else router.push('/patient-dashboard');
    }, 1000);

  } catch (error) {
    message.value = error.message || "Connection Failed";
    msgType.value = "error";
  } finally {
    isLoading.value = false;
  }
};

const handleRegister = async () => {
  isLoading.value = true;
  message.value = "";
  try {
    const payload = {
      username: form.username,
      password: form.password,
      email: form.email,
      phoneNumber: form.phoneNumber,
      role: form.role,
      specialization: (form.role === 'DOCTOR' || form.role === 'TECHNICIAN') ? form.specialization : null,
      licenseNumber: form.role === 'DOCTOR' ? form.licenseNumber : null,
      insuranceId: form.role === 'PATIENT' ? form.insuranceId : null
    };
    
    const res = await axios.post(`${JAVA_API_URL}/auth/register`, payload);
    const result = res.data;

    if (result.code !== "200" && result.code !== 200) throw new Error(result.msg);
    
    message.value = form.role === 'DOCTOR' 
      ? "Registration successful! Awaiting approval." 
      : "Registration successful! Loading login...";
    
    msgType.value = "success";
    setTimeout(() => {
        isLogin.value = true;
        form.password = ''; 
        message.value = '';
    }, 2500);
  } catch (error) {
    message.value = error.message || "Registration Failed";
    msgType.value = "error";
  } finally {
    isLoading.value = false;
  }
};

// --- CHAT ---
const isChatOpen = ref(false);
const userQuery = ref('');
const isBotTyping = ref(false);
const chatBodyRef = ref(null);
const chatMessages = ref([{ sender: 'bot', text: 'Welcome to Neusoft PACS support.' }]);

const toggleChat = () => { isChatOpen.value = !isChatOpen.value; scrollToBottom(); };
const scrollToBottom = () => { nextTick(() => { if (chatBodyRef.value) chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight; }); };
const formatMessage = (text) => text.replace(/\*\*(.*?)\*\*/g, '<b>$1</b>').replace(/\n/g, '<br>');
const sendToAI = async () => {
  if (!userQuery.value.trim()) return;
  const text = userQuery.value;
  chatMessages.value.push({ sender: 'user', text });
  userQuery.value = '';
  isBotTyping.value = true;
  scrollToBottom();
  try {
    const res = await axios.post(`${JAVA_API_URL}/api/chat/ask`, { message: text, patientId: null });
    let aiReply = res.data.reply || "Error.";
    aiReply = aiReply.replace(/<think>[\s\S]*?<\/think>/g, '');
    chatMessages.value.push({ sender: 'bot', text: aiReply.trim() });
  } catch (e) { chatMessages.value.push({ sender: 'bot', text: "Service Error." }); } 
  finally { isBotTyping.value = false; scrollToBottom(); }
};
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700&display=swap');

:root {
  --primary: #3b82f6;
  --primary-dark: #2563eb;
  --dark: #0f172a;
  --light: #f8fafc;
  --border: #e2e8f0;
}

* { box-sizing: border-box; }

.split-screen {
  display: flex; height: 100vh; width: 100vw;
  font-family: 'Plus Jakarta Sans', sans-serif;
  background: white; overflow: hidden;
}

/* --- LEFT PANEL --- */
.brand-panel {
  flex: 0.8;
  background: linear-gradient(135deg, #0f172a 0%, #1e3a8a 100%);
  color: white;
  position: relative;
  display: flex; flex-direction: column; justify-content: center;
  padding: 80px; overflow: hidden;
}

/* --- FIXED HEADER ALIGNMENT --- */
.brand-header {
  display: flex;
  align-items: center;
  gap: 15px; /* Space between logo and text */
  margin-bottom: 20px;
}

.logo-badge {
  width: 50px; height: 50px; background: #3b82f6; 
  border-radius: 12px; display: flex; align-items: center; justify-content: center;
  font-size: 28px; font-weight: bold;
  box-shadow: 0 10px 30px rgba(59, 130, 246, 0.4);
  flex-shrink: 0; /* Prevents logo squishing */
}

.brand-panel h1 { 
  font-size: 3.5rem; 
  margin: 0; 
  font-weight: 800; 
  letter-spacing: -1px; 
  line-height: 1; /* Ensures text height aligns with logo */
}

.tagline { font-size: 1.25rem; color: #93c5fd; margin-bottom: 50px; }

.feature-list { display: flex; flex-direction: column; gap: 20px; }
.feature-item { 
  display: flex; align-items: center; gap: 15px; 
  background: rgba(255,255,255,0.05); padding: 15px; border-radius: 12px;
  border: 1px solid rgba(255,255,255,0.1); backdrop-filter: blur(10px);
}
.icon-box { font-size: 1.5rem; }
.text strong { display: block; font-size: 1rem; }
.text span { font-size: 0.85rem; color: #cbd5e1; }

.bg-circle { position: absolute; border-radius: 50%; filter: blur(80px); opacity: 0.3; }
.c1 { width: 400px; height: 400px; background: #3b82f6; top: -100px; right: -100px; }
.c2 { width: 300px; height: 300px; background: #60a5fa; bottom: -50px; left: -50px; }

/* --- RIGHT PANEL --- */
.form-panel {
  flex: 1.2;
  display: flex; flex-direction: column; justify-content: center; align-items: center;
  background: white; padding: 20px;
}

.form-wrapper { width: 100%; max-width: 460px; }

.form-header { margin-bottom: 30px; }
.form-header h2 { font-size: 2rem; color: #1e293b; margin: 0 0 5px 0; font-weight: 700; }
.subtitle { color: #64748b; font-size: 1rem; }

/* TOGGLE */
.toggle-container {
  background: #f1f5f9; padding: 4px; border-radius: 50px;
  display: flex; position: relative; margin-bottom: 30px;
  height: 52px;
}
.toggle-bg {
  position: absolute; top: 4px; bottom: 4px; width: calc(50% - 4px);
  background: white; border-radius: 50px;
  box-shadow: 0 4px 10px rgba(0,0,0,0.05);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.toggle-container button {
  flex: 1; position: relative; z-index: 2; border: none; background: none;
  font-weight: 600; color: #64748b; cursor: pointer; transition: color 0.3s;
}
.toggle-container button.active { color: #0f172a; }

/* INPUTS */
.input-group { margin-bottom: 20px; }
.input-group label { display: block; margin-bottom: 8px; font-size: 0.9rem; font-weight: 600; color: #334155; }
.field-wrap { position: relative; }
.field-wrap i { position: absolute; left: 14px; top: 50%; transform: translateY(-50%); color: #94a3b8; font-style: normal; }
input, select {
  width: 100%; padding: 14px 16px; border: 1px solid #e2e8f0;
  border-radius: 10px; font-size: 0.95rem; background: #fff;
  transition: all 0.2s; color: #0f172a; font-family: inherit;
}
.field-wrap input { padding-left: 40px; }
input:focus, select:focus { border-color: #3b82f6; outline: none; box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.1); }
.row { display: flex; gap: 15px; } .row .input-group { flex: 1; }

/* CHECKBOX (Stylized) */
.form-extras { display: flex; justify-content: space-between; align-items: center; margin-bottom: 25px; }
.custom-checkbox {
  display: flex; align-items: center; cursor: pointer; position: relative; padding-left: 28px;
  font-size: 0.9rem; color: #64748b; user-select: none;
}
.custom-checkbox input { position: absolute; opacity: 0; cursor: pointer; height: 0; width: 0; }
.checkmark {
  position: absolute; top: 0; left: 0; height: 20px; width: 20px;
  background-color: #fff; border: 2px solid #cbd5e1; border-radius: 6px;
  transition: all 0.2s;
}
.custom-checkbox:hover .checkmark { border-color: #3b82f6; }
.custom-checkbox input:checked ~ .checkmark { background-color: #3b82f6; border-color: #3b82f6; }
.checkmark:after {
  content: ""; position: absolute; display: none;
  left: 6px; top: 2px; width: 5px; height: 10px;
  border: solid white; border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}
.custom-checkbox input:checked ~ .checkmark:after { display: block; }
.forgot-link { color: #3b82f6; text-decoration: none; font-size: 0.9rem; font-weight: 600; }

/* BUTTON (Vibrant Gradient) */
.primary-btn {
  width: 100%; padding: 16px;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: white; border: none; border-radius: 12px;
  font-size: 1rem; font-weight: 600; cursor: pointer;
  box-shadow: 0 8px 20px -6px rgba(37, 99, 235, 0.5);
  transition: transform 0.2s, box-shadow 0.2s;
}
.primary-btn:hover { transform: translateY(-2px); box-shadow: 0 12px 25px -8px rgba(37, 99, 235, 0.6); }
.primary-btn:active { transform: translateY(0); }
.primary-btn:disabled { background: #94a3b8; cursor: not-allowed; box-shadow: none; }

/* ROLE SELECTOR */
.role-grid { display: flex; gap: 10px; margin-bottom: 25px; }
.role-card {
  flex: 1; padding: 12px; border: 1px solid #e2e8f0; border-radius: 10px;
  text-align: center; cursor: pointer; transition: all 0.2s;
  display: flex; flex-direction: column; align-items: center; gap: 6px;
}
.role-card:hover { border-color: #3b82f6; background: #f0f9ff; }
.role-card.selected { border-color: #3b82f6; background: #eff6ff; color: #3b82f6; font-weight: 700; }
.role-dot { width: 10px; height: 10px; border-radius: 50%; background: #cbd5e1; }
.role-card.selected .role-dot { background: #3b82f6; }

/* ALERTS & FOOTER */
.alert-box {
  margin-top: 20px; padding: 12px; border-radius: 8px;
  font-size: 0.9rem; display: flex; align-items: center; gap: 10px;
}
.alert-box.success { background: #ecfdf5; color: #047857; border: 1px solid #d1fae5; }
.alert-box.error { background: #fef2f2; color: #b91c1c; border: 1px solid #fee2e2; }
.footer-copy { margin-top: 30px; color: #94a3b8; font-size: 0.8rem; }

/* CHAT */
.chat-fab {
  position: fixed; bottom: 30px; right: 30px; width: 60px; height: 60px;
  background: #3b82f6; color: white; border: none; border-radius: 50%;
  box-shadow: 0 10px 30px rgba(59, 130, 246, 0.4); cursor: pointer;
  font-size: 24px; transition: transform 0.2s; z-index: 100;
}
.chat-fab:hover { transform: scale(1.1); }
.chat-window {
  position: fixed; bottom: 100px; right: 30px; width: 340px; height: 450px;
  background: white; border-radius: 16px; box-shadow: 0 20px 50px rgba(0,0,0,0.15);
  display: flex; flex-direction: column; overflow: hidden; z-index: 99;
}
.chat-header { background: #1e293b; color: white; padding: 15px; font-weight: 600; display: flex; align-items: center; gap: 10px; }
.status-dot { width: 8px; height: 8px; background: #22c55e; border-radius: 50%; }
.chat-body { flex: 1; padding: 15px; background: #f8fafc; overflow-y: auto; }
.chat-bubble { padding: 10px 14px; border-radius: 12px; margin-bottom: 10px; max-width: 85%; font-size: 0.9rem; }
.chat-bubble.bot { background: white; border: 1px solid #e2e8f0; color: #334155; }
.chat-bubble.user { background: #3b82f6; color: white; margin-left: auto; }
.chat-input { padding: 10px; border-top: 1px solid #e2e8f0; display: flex; gap: 5px; }
.chat-input input { border: none; background: #f1f5f9; border-radius: 20px; padding: 10px 15px; flex: 1; }
.chat-input button { background: #3b82f6; color: white; border: none; width: 38px; height: 38px; border-radius: 50%; cursor: pointer; }

/* ANIMATION */
.animate-fade { animation: fadeIn 0.4s ease-out; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }

@media (max-width: 900px) {
  .split-screen { flex-direction: column; overflow-y: auto; }
  .brand-panel { min-height: 200px; padding: 40px; flex: none; }
  .form-panel { padding: 20px; }
}
</style>

