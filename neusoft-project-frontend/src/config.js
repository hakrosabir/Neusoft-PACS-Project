// Vite exposes these public settings to the browser. Never put secrets here.
const baseUrl = (value, fallback) => (value || fallback).replace(/\/+$/, '')

export const JAVA_API_URL = baseUrl(import.meta.env.VITE_JAVA_API_URL, 'http://localhost:8081')
export const PYTHON_API_URL = baseUrl(import.meta.env.VITE_PYTHON_API_URL, 'http://localhost:5000')
export const OLLAMA_API_URL = baseUrl(import.meta.env.VITE_OLLAMA_API_URL, 'http://localhost:11434')
export const SCAN_INPUT_DIR = import.meta.env.VITE_SCAN_INPUT_DIR || 'upload'
