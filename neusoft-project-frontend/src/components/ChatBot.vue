<template>
  <div>
    <button class="chat-head-btn pulse-animation" @click="toggleChat">
      <svg v-if="!isChatOpen" width="32" height="32" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path d="M12 2C10.8954 2 10 2.89543 10 4H14C14 2.89543 13.1046 2 12 2Z" fill="white"/>
        <path fill-rule="evenodd" clip-rule="evenodd" d="M6 7C4.89543 7 4 7.89543 4 9V17C4 18.1046 4.89543 19 6 19H18C19.1046 19 20 18.1046 20 17V9C20 7.89543 19.1046 7 18 7H6ZM8.5 14C9.32843 14 10 13.3284 10 12.5C10 11.6716 9.32843 11 8.5 11C7.67157 11 7 11.6716 7 12.5C7 13.3284 7.67157 14 8.5 14ZM15.5 14C16.3284 14 17 13.3284 17 12.5C17 11.6716 16.3284 11 15.5 11C14.6716 11 14 11.6716 14 12.5C14 13.3284 14.6716 14 15.5 14Z" fill="white"/>
      </svg>
      <span v-else style="font-size: 24px; font-weight: 700;">✕</span>
    </button>

    <div v-if="isChatOpen" class="chat-window animate-pop">
      <div class="chat-header">
        <div class="bot-avatar">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path fill-rule="evenodd" clip-rule="evenodd" d="M6 7C4.89543 7 4 7.89543 4 9V17C4 18.1046 4.89543 19 6 19H18C19.1046 19 20 18.1046 20 17V9C20 7.89543 19.1046 7 18 7H6ZM8.5 14C9.32843 14 10 13.3284 10 12.5C10 11.6716 9.32843 11 8.5 11C7.67157 11 7 11.6716 7 12.5C7 13.3284 7.67157 14 8.5 14ZM15.5 14C16.3284 14 17 13.3284 17 12.5C17 11.6716 16.3284 11 15.5 11C14.6716 11 14 11.6716 14 12.5C14 13.3284 14.6716 14 15.5 14Z" fill="white"/>
          </svg>
        </div>
        <div>
          <h4>Neusoft Assistant</h4>
          <span class="status-dot"></span> <small>Online (Guest Support)</small>
        </div>
      </div>

      <div class="chat-body" ref="chatBodyRef">
        <div v-for="(msg, index) in chatMessages" :key="index" :class="['chat-bubble', msg.sender]">
          <p v-html="formatMessage(msg.text)"></p>
        </div>
        <div v-if="isBotTyping" class="chat-bubble bot typing">
          <span>.</span><span>.</span><span>.</span>
        </div>
      </div>

      <div class="chat-footer">
        <input 
          v-model="userQuery" 
          @keyup.enter="sendToAI" 
          type="text" 
          placeholder="Ask about registration, doctors..." 
          :disabled="isBotTyping"
        />
        <button @click="sendToAI" :disabled="!userQuery || isBotTyping">➤</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { JAVA_API_URL } from '../config.js'
import { ref, nextTick } from 'vue';
import axios from 'axios';

const isChatOpen = ref(false);
const userQuery = ref('');
const isBotTyping = ref(false);
const chatBodyRef = ref(null);

const chatMessages = ref([
  { sender: 'bot', text: 'Hello! I am the Neusoft Support Bot. Ask me about registration, doctors, or our services!' }
]);

const toggleChat = () => {
  isChatOpen.value = !isChatOpen.value;
  scrollToBottom();
};

const scrollToBottom = () => {
  nextTick(() => {
    if (chatBodyRef.value) {
      chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight;
    }
  });
};

const formatMessage = (text) => {
  return text.replace(/\*\*(.*?)\*\*/g, '<b>$1</b>').replace(/\n/g, '<br>');
};

const sendToAI = async () => {
  if (!userQuery.value.trim()) return;

  const text = userQuery.value;
  chatMessages.value.push({ sender: 'user', text });
  userQuery.value = '';
  isBotTyping.value = true;
  scrollToBottom();

  try {
    const res = await axios.post(`${JAVA_API_URL}/api/chat/ask`, {
      message: text,
      patientId: null 
    });

    let aiReply = res.data.reply || "I couldn't process that.";
    // Clean up <think> tags if your AI returns them
    aiReply = aiReply.replace(/<think>[\s\S]*?<\/think>/g, '');
    
    chatMessages.value.push({ sender: 'bot', text: aiReply.trim() });
  } catch (error) {
    chatMessages.value.push({ sender: 'bot', text: "Sorry, I'm having trouble connecting to the server." });
  } finally {
    isBotTyping.value = false;
    scrollToBottom();
  }
};
</script>

<style scoped>
/* Imported Inter font for consistency */
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap');

/* Chat Specific Styles */
.chat-head-btn { position: fixed; bottom: 25px; right: 25px; width: 65px; height: 65px; background: linear-gradient(135deg, #2563eb, #7c3aed); color: white; border: none; border-radius: 50%; cursor: pointer; box-shadow: 0 8px 25px rgba(37, 99, 235, 0.4); z-index: 1000; display: flex; align-items: center; justify-content: center; transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275); }
.chat-head-btn:hover { transform: scale(1.1) rotate(5deg); box-shadow: 0 12px 30px rgba(124, 58, 237, 0.5); }
.pulse-animation { animation: pulse-glow 3s infinite; }
@keyframes pulse-glow { 0% { box-shadow: 0 0 0 0 rgba(37, 99, 235, 0.6); } 70% { box-shadow: 0 0 0 15px rgba(37, 99, 235, 0); } 100% { box-shadow: 0 0 0 0 rgba(37, 99, 235, 0); } }

.chat-window { position: fixed; bottom: 100px; right: 25px; width: 360px; height: 480px; background: white; border-radius: 16px; box-shadow: 0 10px 40px rgba(0,0,0,0.15); display: flex; flex-direction: column; z-index: 999; overflow: hidden; border: 1px solid #e2e8f0; font-family: 'Inter', sans-serif; }
.animate-pop { animation: popIn 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275); }
@keyframes popIn { from { opacity: 0; transform: scale(0.9) translateY(20px); } to { opacity: 1; transform: scale(1) translateY(0); } }

.chat-header { background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%); color: white; padding: 18px; display: flex; align-items: center; gap: 12px; border-bottom: 1px solid rgba(255,255,255,0.1); }
.bot-avatar { width: 38px; height: 38px; background: rgba(255,255,255,0.1); border-radius: 50%; display: flex; align-items: center; justify-content: center; border: 1px solid rgba(255,255,255,0.2); }
.status-dot { display: inline-block; width: 8px; height: 8px; background: #22c55e; border-radius: 50%; margin-right: 4px; box-shadow: 0 0 5px #22c55e; }
.chat-header h4 { margin: 0; font-size: 1.05rem; font-weight: 600; }
.chat-header small { opacity: 0.8; font-size: 0.85rem; }

.chat-body { flex: 1; padding: 15px; overflow-y: auto; background: #f8fafc; display: flex; flex-direction: column; gap: 10px; }
.chat-bubble { max-width: 80%; padding: 12px 16px; border-radius: 12px; font-size: 0.95rem; line-height: 1.5; word-wrap: break-word; box-shadow: 0 1px 2px rgba(0,0,0,0.05); }
.chat-bubble.bot { align-self: flex-start; background: white; border: 1px solid #e2e8f0; color: #334155; border-bottom-left-radius: 2px; }
.chat-bubble.user { align-self: flex-end; background: #2563eb; color: white; border-bottom-right-radius: 2px; }

.typing span { animation: blink 1.4s infinite both; font-size: 20px; line-height: 10px; margin: 0 1px; }
.typing span:nth-child(2) { animation-delay: 0.2s; }
.typing span:nth-child(3) { animation-delay: 0.4s; }
@keyframes blink { 0% { opacity: 0.2; } 20% { opacity: 1; } 100% { opacity: 0.2; } }

.chat-footer { padding: 15px; border-top: 1px solid #e2e8f0; display: flex; gap: 10px; background: white; }
.chat-footer input { flex: 1; padding: 12px 16px; border: 1px solid #e2e8f0; border-radius: 24px; outline: none; font-size: 0.95rem; background: #f8fafc; transition: all 0.2s; }
.chat-footer input:focus { border-color: #2563eb; background: white; box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1); }
.chat-footer button { background: #0f172a; color: white; border: none; width: 42px; height: 42px; border-radius: 50%; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: 0.2s; }
.chat-footer button:disabled { background: #e2e8f0; cursor: not-allowed; }
.chat-footer button:hover:not(:disabled) { background: #2563eb; transform: translateY(-2px); }
</style>