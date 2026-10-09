<template>
  <div>

    <!-- ═══════════════════════════════════════════════════════════════════
         TYPE SELECTOR
    ════════════════════════════════════════════════════════════════════════ -->
    <Teleport to="body">
    <div v-if="showTypeSelector" class="rs-overlay" @click.self="showTypeSelector = false">
      <div class="ts-card">
        <div class="ts-header">
          <div class="ts-title-row">
            <div class="ts-logo">N</div>
            <div>
              <h3>New Radiology Report</h3>
              <p>Choose the scan modality for this session</p>
            </div>
          </div>
          <button class="icon-btn" @click="showTypeSelector = false">✕</button>
        </div>

        <div class="ts-patient-bar">
          <div class="tsb-avatar">{{ initials(pendingApt.patientName) }}</div>
          <span class="tsb-name">{{ pendingApt.patientName }}</span>
          <span class="tsb-divider">·</span>
          <span class="tsb-label">APT</span>
          <span class="tsb-id">#{{ pendingApt.id }}</span>
          <span v-if="pendingApt.department" class="tsb-dept">{{ pendingApt.department }}</span>
        </div>

        <div class="type-grid">
          <button v-for="t in analysisTypes" :key="t.key" class="type-card" :class="'tc-' + t.key" @click="selectType(t.key)">
            <div class="tc-top">
              <span class="tc-icon">{{ t.icon }}</span>
              <span class="tc-modality">{{ t.modality }}</span>
            </div>
            <span class="tc-name">{{ t.label }}</span>
            <span class="tc-desc">{{ t.desc }}</span>
            <div class="tc-pills">
              <span v-for="f in t.features" :key="f">{{ f }}</span>
            </div>
            <div class="tc-cta">Select →</div>
          </button>
        </div>
      </div>
    </div>
    </Teleport>

    <!-- ═══════════════════════════════════════════════════════════════════
         EDITOR MODAL
    ════════════════════════════════════════════════════════════════════════ -->
    <Teleport to="body">
    <div v-if="showEditor" class="rs-overlay">
      <div class="editor-card">

        <!-- Header bar -->
        <div class="ed-header" :class="'edh-' + reportForm.analysisType">
          <div class="ed-header-left">
            <span class="ed-type-chip" :class="'chip-' + reportForm.analysisType">
              {{ currentTypeConfig.icon }} {{ currentTypeConfig.label }}
            </span>
            <div>
              <h3>{{ isEditing ? 'Edit Report' : 'New Diagnostic Report' }}</h3>
              <span class="ed-sub">{{ currentApt.patientName }} · APT-{{ currentApt.id }}</span>
            </div>
          </div>
          <div class="ed-header-right">
            <button class="ghost-btn" @click="previewReport">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
              Preview
            </button>
            <button class="icon-btn light" @click="showEditor = false">✕</button>
          </div>
        </div>

        <!-- Step pills -->
        <div class="step-bar">
          <button v-for="(s, i) in editorSteps" :key="i" class="step-btn"
            :class="{ active: activeStep === i, done: activeStep > i }"
            @click="activeStep = i">
            <span class="step-circle">{{ activeStep > i ? '✓' : i + 1 }}</span>
            {{ s }}
          </button>
        </div>

        <!-- Two-column layout -->
        <div class="ed-body">

          <!-- LEFT SIDEBAR -->
          <aside class="ed-sidebar">

            <!-- Patient info card -->
            <div class="patient-card">
              <div class="pt-avatar">{{ initials(currentApt.patientName) }}</div>
              <div class="pt-meta">
                <strong>{{ currentApt.patientName }}</strong>
                <div class="pt-row"><span>Gender</span><b>{{ currentApt.patientGender || '—' }}</b></div>
                <div class="pt-row"><span>DOB</span><b>{{ currentApt.patientBirthDate || '—' }}</b></div>
                <div class="pt-row"><span>Dept</span><b>{{ currentApt.department || 'Radiology' }}</b></div>
              </div>
            </div>

            <!-- Type badge + change -->
            <div class="type-badge-row" :class="'tbr-' + reportForm.analysisType">
              <span class="tbr-icon">{{ currentTypeConfig.icon }}</span>
              <div class="tbr-text">
                <span class="tbr-top">Analysis Type</span>
                <span class="tbr-val">{{ currentTypeConfig.label }}</span>
              </div>
              <button class="tbr-change" @click="changeType" title="Change type">⟳</button>
            </div>

            <!-- Image panels -->
            <div class="img-section">
              <div class="img-section-title">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21,15 16,10 5,21"/></svg>
                Scan Images
              </div>

              <template v-if="reportForm.analysisType === 'brain'">
                <div class="img-group-lbl">MRI Slice Views</div>
                <div v-for="v in ['axial','coronal','sagittal']" :key="v" class="img-slot">
                  <div class="is-header">
                    <span>{{ v }}</span>
                    <label class="is-btn upload">
                      {{ reportForm[v + 'Image'] ? '✎' : '+' }}
                      <input type="file" hidden @change="e => handleUpload(e, v + 'Image')" accept="image/*">
                    </label>
                    <button v-if="reportForm[v + 'Image']" class="is-btn clear" @click="reportForm[v + 'Image'] = ''">✕</button>
                  </div>
                  <div class="is-preview" @click="reportForm[v + 'Image'] && openLightbox(reportForm[v + 'Image'])">
                    <img v-if="reportForm[v + 'Image']" :src="reportForm[v + 'Image']" />
                    <div v-else class="is-empty"><span>🧠</span><small>{{ v }}</small></div>
                  </div>
                </div>
                <div class="img-group-lbl">3D Segmentation</div>
                <div class="img-slot">
                  <div class="is-header">
                    <span>3D Mesh</span>
                    <label class="is-btn upload">
                      {{ reportForm.mesh3dImage ? '✎' : '+' }}
                      <input type="file" hidden @change="e => handleUpload(e, 'mesh3dImage')" accept="image/*">
                    </label>
                    <button v-if="reportForm.mesh3dImage" class="is-btn clear" @click="reportForm.mesh3dImage = ''">✕</button>
                  </div>
                  <div class="is-preview" @click="reportForm.mesh3dImage && openLightbox(reportForm.mesh3dImage)">
                    <img v-if="reportForm.mesh3dImage" :src="reportForm.mesh3dImage" />
                    <div v-else class="is-empty"><span>🔮</span><small>3D brain</small></div>
                  </div>
                </div>
              </template>

              <template v-else-if="reportForm.analysisType === 'spleen'">
                <div class="img-group-lbl">CT Slice Views</div>
                <div v-for="v in ['axial','coronal','sagittal']" :key="v" class="img-slot">
                  <div class="is-header">
                    <span>{{ v }}</span>
                    <label class="is-btn upload">
                      {{ reportForm[v + 'Image'] ? '✎' : '+' }}
                      <input type="file" hidden @change="e => handleUpload(e, v + 'Image')" accept="image/*">
                    </label>
                    <button v-if="reportForm[v + 'Image']" class="is-btn clear" @click="reportForm[v + 'Image'] = ''">✕</button>
                  </div>
                  <div class="is-preview" @click="reportForm[v + 'Image'] && openLightbox(reportForm[v + 'Image'])">
                    <img v-if="reportForm[v + 'Image']" :src="reportForm[v + 'Image']" />
                    <div v-else class="is-empty"><span>🫀</span><small>{{ v }}</small></div>
                  </div>
                </div>
                <div class="img-group-lbl">3D Spleen Mesh</div>
                <div class="img-slot">
                  <div class="is-header">
                    <span>3D Render</span>
                    <label class="is-btn upload">
                      {{ reportForm.mesh3dImage ? '✎' : '+' }}
                      <input type="file" hidden @change="e => handleUpload(e, 'mesh3dImage')" accept="image/*">
                    </label>
                    <button v-if="reportForm.mesh3dImage" class="is-btn clear" @click="reportForm.mesh3dImage = ''">✕</button>
                  </div>
                  <div class="is-preview" @click="reportForm.mesh3dImage && openLightbox(reportForm.mesh3dImage)">
                    <img v-if="reportForm.mesh3dImage" :src="reportForm.mesh3dImage" />
                    <div v-else class="is-empty"><span>🔮</span><small>3D spleen</small></div>
                  </div>
                </div>
              </template>

              <template v-else-if="reportForm.analysisType === 'lungs'">
                <div class="img-group-lbl">Chest CT Views</div>
                <div v-for="v in ['axial','coronal','sagittal']" :key="v" class="img-slot">
                  <div class="is-header">
                    <span>{{ v }}</span>
                    <label class="is-btn upload">
                      {{ reportForm[v + 'Image'] ? '✎' : '+' }}
                      <input type="file" hidden @change="e => handleUpload(e, v + 'Image')" accept="image/*">
                    </label>
                    <button v-if="reportForm[v + 'Image']" class="is-btn clear" @click="reportForm[v + 'Image'] = ''">✕</button>
                  </div>
                  <div class="is-preview" @click="reportForm[v + 'Image'] && openLightbox(reportForm[v + 'Image'])">
                    <img v-if="reportForm[v + 'Image']" :src="reportForm[v + 'Image']" />
                    <div v-else class="is-empty"><span>🫁</span><small>{{ v }}</small></div>
                  </div>
                </div>
                <div class="img-group-lbl">Lobe Segmentation</div>
                <div class="img-slot">
                  <div class="is-header">
                    <span>Lobe Map</span>
                    <label class="is-btn upload">
                      {{ reportForm.mesh3dImage ? '✎' : '+' }}
                      <input type="file" hidden @change="e => handleUpload(e, 'mesh3dImage')" accept="image/*">
                    </label>
                    <button v-if="reportForm.mesh3dImage" class="is-btn clear" @click="reportForm.mesh3dImage = ''">✕</button>
                  </div>
                  <div class="is-preview" @click="reportForm.mesh3dImage && openLightbox(reportForm.mesh3dImage)">
                    <img v-if="reportForm.mesh3dImage" :src="reportForm.mesh3dImage" />
                    <div v-else class="is-empty"><span>🗺️</span><small>lobe overlay</small></div>
                  </div>
                </div>
              </template>

            </div>
          </aside>

          <!-- MAIN FORM -->
          <div class="ed-main">

            <!-- STEP 0: DIAGNOSIS -->
            <div v-show="activeStep === 0" class="step-pane">
              <div class="pane-header">
                <div class="pane-num">1</div>
                <div>
                  <h4>Clinical Diagnosis</h4>
                  <p>Enter AI-assisted findings and primary diagnosis</p>
                </div>
              </div>

              <!-- BRAIN fields -->
              <div v-if="reportForm.analysisType === 'brain'" class="findings-grid">
                <div class="fg-field">
                  <label>Tumor Classification <span class="req">*</span></label>
                  <select v-model="reportForm.brainTumorType" class="f-select">
                    <option value="">— Select —</option>
                    <option>Glioblastoma Multiforme (GBM)</option>
                    <option>Low-grade Glioma</option>
                    <option>Meningioma</option>
                    <option>Metastatic Lesion</option>
                    <option>No Tumor Detected</option>
                    <option>Other</option>
                  </select>
                </div>
                <div class="fg-field">
                  <label>WHO Grade</label>
                  <select v-model="reportForm.brainWhoGrade" class="f-select">
                    <option value="">— Select —</option>
                    <option>Grade I</option><option>Grade II</option>
                    <option>Grade III</option><option>Grade IV</option><option>N/A</option>
                  </select>
                </div>
                <div class="fg-field">
                  <label>Tumor Location</label>
                  <input v-model="reportForm.brainLocation" class="f-input" placeholder="e.g. Left frontal lobe" />
                </div>
                <div class="fg-field">
                  <label>Estimated Volume (cm³)</label>
                  <input v-model="reportForm.brainVolume" class="f-input" type="number" placeholder="e.g. 12.4" />
                </div>
                <div class="fg-field full">
                  <label>Enhancing Characteristics</label>
                  <input v-model="reportForm.brainEnhancing" class="f-input" placeholder="e.g. Ring-enhancing lesion with central necrosis" />
                </div>
              </div>

              <!-- SPLEEN fields -->
              <div v-else-if="reportForm.analysisType === 'spleen'" class="findings-grid">
                <div class="fg-field">
                  <label>Spleen Size <span class="req">*</span></label>
                  <select v-model="reportForm.spleenSize" class="f-select">
                    <option value="">— Select —</option>
                    <option>Normal (9–13 cm)</option>
                    <option>Mildly Enlarged (13–16 cm)</option>
                    <option>Moderately Enlarged (16–20 cm)</option>
                    <option>Massively Enlarged (&gt;20 cm)</option>
                    <option>Small / Atrophic</option>
                  </select>
                </div>
                <div class="fg-field">
                  <label>Measured Length (cm)</label>
                  <input v-model="reportForm.spleenLength" class="f-input" type="number" step="0.1" placeholder="e.g. 11.2" />
                </div>
                <div class="fg-field">
                  <label>ICH Finding</label>
                  <select v-model="reportForm.ichFinding" class="f-select">
                    <option value="">— Select —</option>
                    <option>No hemorrhage</option>
                    <option>Subcapsular hematoma</option>
                    <option>Intraparenchymal hemorrhage</option>
                    <option>Active bleeding suspected</option>
                  </select>
                </div>
                <div class="fg-field">
                  <label>Parenchyma</label>
                  <input v-model="reportForm.spleenParenchyma" class="f-input" placeholder="e.g. Homogeneous, no focal lesion" />
                </div>
              </div>

              <!-- LUNGS fields -->
              <div v-else-if="reportForm.analysisType === 'lungs'" class="findings-grid">
                <div class="fg-field">
                  <label>Primary Finding <span class="req">*</span></label>
                  <select v-model="reportForm.lungFinding" class="f-select">
                    <option value="">— Select —</option>
                    <option>No significant finding</option>
                    <option>Pulmonary Nodule(s)</option>
                    <option>Mass / Consolidation</option>
                    <option>Emphysema</option>
                    <option>Pleural Effusion</option>
                    <option>Ground-glass Opacity</option>
                    <option>Atelectasis</option>
                    <option>Other</option>
                  </select>
                </div>
                <div class="fg-field">
                  <label>Lobes Affected</label>
                  <input v-model="reportForm.lungLobes" class="f-input" placeholder="e.g. RUL, LLL" />
                </div>
                <div class="fg-field">
                  <label>Nodule Size (mm)</label>
                  <input v-model="reportForm.lungNoduleSize" class="f-input" type="number" step="0.5" placeholder="e.g. 8.5" />
                </div>
                <div class="fg-field">
                  <label>Pleural Status</label>
                  <input v-model="reportForm.lungPleural" class="f-input" placeholder="e.g. No effusion" />
                </div>
                <div class="fg-field full">
                  <label>AI Lobe Segmentation Summary</label>
                  <input v-model="reportForm.lungLobeSummary" class="f-input" placeholder="e.g. 5 lobes segmented, RUL shows reduced volume" />
                </div>
              </div>

              <div class="f-group mt">
                <label>Full Clinical Diagnosis <span class="req">*</span></label>
                <textarea v-model="reportForm.diagnosis" class="f-textarea" rows="5" :placeholder="diagnosisPlaceholder"></textarea>
                <span class="f-hint">{{ reportForm.diagnosis?.length || 0 }} chars</span>
              </div>

              <div class="step-nav">
                <span></span>
                <button class="btn-next" @click="activeStep = 1">Treatment Plan →</button>
              </div>
            </div>

            <!-- STEP 1: TREATMENT -->
            <div v-show="activeStep === 1" class="step-pane">
              <div class="pane-header">
                <div class="pane-num">2</div>
                <div>
                  <h4>Treatment Plan</h4>
                  <p>Prescription, medications and follow-up</p>
                </div>
              </div>

              <div class="f-group">
                <label>Severity / Priority</label>
                <div class="sev-row">
                  <button v-for="s in severities" :key="s.key"
                    class="sev-btn" :class="['sev-' + s.key, { active: reportForm.severity === s.key }]"
                    @click="reportForm.severity = s.key">
                    {{ s.icon }} {{ s.label }}
                  </button>
                </div>
              </div>

              <div class="f-group mt">
                <label>Prescription / Medication</label>
                <textarea v-model="reportForm.prescription" class="f-textarea" rows="4" placeholder="Rx: Drug name, dose, frequency, duration..."></textarea>
              </div>

              <div class="f-group mt">
                <label>Advice & Recommendations</label>
                <textarea v-model="reportForm.advice" class="f-textarea" rows="4" placeholder="Follow-up instructions, lifestyle advice, referrals..."></textarea>
              </div>

              <div class="f-group mt">
                <label>Follow-up Date</label>
                <input v-model="reportForm.followUpDate" type="date" class="f-input" />
              </div>

              <div class="step-nav">
                <button class="btn-back" @click="activeStep = 0">← Back</button>
                <button class="btn-next" @click="activeStep = 2">Review →</button>
              </div>
            </div>

            <!-- STEP 2: REVIEW -->
            <div v-show="activeStep === 2" class="step-pane">
              <div class="pane-header">
                <div class="pane-num">3</div>
                <div>
                  <h4>Review & Finalize</h4>
                  <p>Confirm everything before signing</p>
                </div>
              </div>

              <div class="review-grid">
                <div class="rv-tile">
                  <span class="rv-lbl">Type</span>
                  <span class="rv-val">{{ currentTypeConfig.icon }} {{ currentTypeConfig.label }}</span>
                </div>
                <div class="rv-tile">
                  <span class="rv-lbl">Patient</span>
                  <span class="rv-val">{{ currentApt.patientName }}</span>
                </div>
                <div class="rv-tile">
                  <span class="rv-lbl">Severity</span>
                  <span class="rv-val" :class="'sev-t-' + reportForm.severity">
                    {{ severities.find(s => s.key === reportForm.severity)?.icon }}
                    {{ severities.find(s => s.key === reportForm.severity)?.label }}
                  </span>
                </div>
                <div class="rv-tile">
                  <span class="rv-lbl">Images</span>
                  <span class="rv-val">{{ imageCount }} / {{ maxImages }}</span>
                </div>
              </div>

              <div class="rv-diag">
                <span class="rv-lbl">Diagnosis Preview</span>
                <p>{{ reportForm.diagnosis || '— Not entered —' }}</p>
              </div>

              <div class="checklist">
                <div class="cl-row" :class="{ ok: reportForm.diagnosis }">
                  <span>{{ reportForm.diagnosis ? '✓' : '○' }}</span> Diagnosis entered
                </div>
                <div class="cl-row" :class="{ ok: hasTypeSpecificFields }">
                  <span>{{ hasTypeSpecificFields ? '✓' : '○' }}</span> {{ currentTypeConfig.label }} fields filled
                </div>
                <div class="cl-row" :class="{ ok: reportForm.axialImage }">
                  <span>{{ reportForm.axialImage ? '✓' : '○' }}</span> Axial scan image attached
                </div>
                <div class="cl-row" :class="{ ok: reportForm.mesh3dImage }">
                  <span>{{ reportForm.mesh3dImage ? '✓' : '○' }}</span> 3D segmentation image attached
                </div>
              </div>

              <div class="step-nav">
                <button class="btn-back" @click="activeStep = 1">← Back</button>
                <div class="final-btns">
                  <button class="btn-preview" @click="previewReport">👁 Preview</button>
                  <button class="btn-finalize" @click="saveReport" :disabled="isSaving || !reportForm.diagnosis">
                    <span v-if="isSaving" class="spin"></span>
                    {{ isSaving ? 'Saving...' : (isEditing ? '✓ Update Report' : '✍ Finalize & Sign') }}
                  </button>
                </div>
              </div>
            </div>

          </div><!-- /ed-main -->
        </div><!-- /ed-body -->
      </div>
    </div>
    </Teleport>

    <!-- ═══════════════════════════════════════════════════════════════════
         REPORT VIEWER — PRINTABLE A4 PAPER
    ════════════════════════════════════════════════════════════════════════ -->
    <Teleport to="body">
    <div v-if="showViewer" class="rs-overlay viewer-overlay" id="rs-viewer-overlay">

      <!-- Floating toolbar -->
      <div class="viewer-toolbar" id="rs-no-print">
        <div class="vt-left">
          <div class="vt-logo">N</div>
          <span class="vt-title">Report Preview</span>
          <span class="vt-type-chip" :class="'vtc-' + normalizeType(currentReport.analysisType)">
            {{ getTypeConfig(currentReport.analysisType).icon }}
            {{ getTypeConfig(currentReport.analysisType).label }}
          </span>
        </div>
        <div class="vt-right">
          <!-- Edit button: doctors only — hidden for patients -->
          <button v-if="props.role !== 'patient'" class="vt-btn edit" @click="enableEditMode">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
            Edit Report
          </button>
          <button class="vt-btn print" @click="handlePrint">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6,9 6,2 18,2 18,9"/><path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"/><rect x="6" y="14" width="12" height="8"/></svg>
            Print / PDF
          </button>
          <button class="vt-btn close" @click="showViewer = false">✕</button>
        </div>
      </div>

      <!-- A4 Paper -->
      <div class="paper-scroll">
        <div class="paper" id="rs-paper">

          <!-- HOSPITAL HEADER -->
          <header class="ph-header">
            <div class="ph-brand">
              <div class="ph-logo"><span>N</span></div>
              <div>
                <h1 class="ph-name">NeuroPACS <span>Medical Center</span></h1>
                <p class="ph-tagline">AI-Powered Diagnostic Radiology System</p>
              </div>
            </div>
            <div class="ph-meta">
              <div class="ph-type-badge" :class="'ptb-' + normalizeType(currentReport.analysisType)">
                {{ getTypeConfig(currentReport.analysisType).icon }}
                {{ getTypeConfig(currentReport.analysisType).label }} Report
              </div>
              <table class="ph-info-table">
                <tr><td>Report ID</td><td><strong>#{{ currentReport.id || 'PREVIEW' }}</strong></td></tr>
                <tr><td>Date</td><td><strong>{{ formatDate(currentReport.reportDate || currentReport.createdAt || new Date()) }}</strong></td></tr>
                <tr><td>Status</td><td><strong class="ph-status">FINALIZED</strong></td></tr>
              </table>
            </div>
          </header>

          <div class="divider thick"></div>

          <!-- PATIENT INFO -->
          <section class="patient-info-grid">
            <div class="pig-col">
              <span class="pig-lbl">Patient Name</span>
              <span class="pig-val">{{ currentReport.patientName || '—' }}</span>
            </div>
            <div class="pig-col">
              <span class="pig-lbl">Patient ID</span>
              <span class="pig-val mono">{{ currentReport.patientId || '—' }}</span>
            </div>
            <div class="pig-col">
              <span class="pig-lbl">Gender / DOB</span>
              <span class="pig-val">{{ currentReport.patientGender || '—' }} / {{ currentReport.patientBirthDate || '—' }}</span>
            </div>
            <div class="pig-col">
              <span class="pig-lbl">Referring Doctor</span>
              <span class="pig-val">Dr. {{ currentReport.doctorName || '—' }}</span>
            </div>
            <div class="pig-col">
              <span class="pig-lbl">Modality</span>
              <span class="pig-val">{{ getTypeConfig(currentReport.analysisType).modality }}</span>
            </div>
            <div class="pig-col">
              <span class="pig-lbl">Follow-up Date</span>
              <span class="pig-val">{{ currentReport.followUpDate || 'As advised' }}</span>
            </div>
            <div class="pig-col">
              <span class="pig-lbl">Severity</span>
              <span class="pig-val sev-badge" :class="'svb-' + (currentReport.severity || 'routine')">
                {{ severityLabel(currentReport.severity) }}
              </span>
            </div>
            <div class="pig-col">
              <span class="pig-lbl">DICOM ID</span>
              <span class="pig-val mono">{{ currentReport.dicomPatientId || 'N/A' }}</span>
            </div>
          </section>

          <div class="divider thin"></div>

          <!-- AI FINDINGS -->
          <section class="ai-section">
            <div class="section-title">
              <div class="st-icon" :class="'sti-' + normalizeType(currentReport.analysisType)">
                {{ getTypeConfig(currentReport.analysisType).icon }}
              </div>
              <div>
                <span class="st-label">AI Analysis Summary</span>
                <span class="st-sub">{{ getTypeConfig(currentReport.analysisType).label }} · {{ getTypeConfig(currentReport.analysisType).modality }}</span>
              </div>
            </div>

            <!-- BRAIN -->
            <div v-if="normalizeType(currentReport.analysisType) === 'brain'" class="findings-table">
              <div class="ft-row"><span>Tumor Classification</span><strong>{{ currentReport.brainTumorType || '—' }}</strong></div>
              <div class="ft-row"><span>WHO Grade</span><strong>{{ currentReport.brainWhoGrade || '—' }}</strong></div>
              <div class="ft-row"><span>Location</span><strong>{{ currentReport.brainLocation || '—' }}</strong></div>
              <div class="ft-row"><span>Estimated Volume</span><strong>{{ currentReport.brainVolume ? currentReport.brainVolume + ' cm³' : '—' }}</strong></div>
              <div class="ft-row full"><span>Enhancing Characteristics</span><strong>{{ currentReport.brainEnhancing || '—' }}</strong></div>
            </div>

            <!-- SPLEEN -->
            <div v-else-if="normalizeType(currentReport.analysisType) === 'spleen'" class="findings-table">
              <div class="ft-row"><span>Spleen Size Classification</span><strong>{{ currentReport.spleenSize || '—' }}</strong></div>
              <div class="ft-row"><span>Measured Length</span><strong>{{ currentReport.spleenLength ? currentReport.spleenLength + ' cm' : '—' }}</strong></div>
              <div class="ft-row"><span>ICH Finding</span><strong>{{ currentReport.ichFinding || '—' }}</strong></div>
              <div class="ft-row"><span>Parenchyma</span><strong>{{ currentReport.spleenParenchyma || '—' }}</strong></div>
            </div>

            <!-- LUNGS -->
            <div v-else-if="normalizeType(currentReport.analysisType) === 'lungs'" class="findings-table">
              <div class="ft-row"><span>Primary Finding</span><strong>{{ currentReport.lungFinding || '—' }}</strong></div>
              <div class="ft-row"><span>Lobes Affected</span><strong>{{ currentReport.lungLobes || '—' }}</strong></div>
              <div class="ft-row"><span>Nodule Size</span><strong>{{ currentReport.lungNoduleSize ? currentReport.lungNoduleSize + ' mm' : '—' }}</strong></div>
              <div class="ft-row"><span>Pleural Status</span><strong>{{ currentReport.lungPleural || '—' }}</strong></div>
              <div class="ft-row full"><span>AI Lobe Segmentation Summary</span><strong>{{ currentReport.lungLobeSummary || '—' }}</strong></div>
            </div>

            <div v-else class="findings-table">
              <div class="ft-row full"><span>Report Type</span><strong>General Radiology Report</strong></div>
            </div>
          </section>

          <!-- IMAGING SECTION -->
          <section class="imaging-section" v-if="currentReport.axialImage || currentReport.coronalImage || currentReport.sagittalImage || currentReport.mesh3dImage">
            <div class="section-title plain">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#1e3a8a" stroke-width="2.5"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21,15 16,10 5,21"/></svg>
              Diagnostic Imaging — Scan Views
            </div>

            <div class="scan-grid">
              <div class="scan-box" v-if="currentReport.axialImage">
                <img :src="currentReport.axialImage" alt="Axial View" />
                <div class="scan-label">
                  <span class="scan-plane">AXIAL</span>
                  <span>{{ scanPlaneDesc('axial', currentReport.analysisType) }}</span>
                </div>
              </div>
              <div class="scan-box" v-if="currentReport.coronalImage">
                <img :src="currentReport.coronalImage" alt="Coronal View" />
                <div class="scan-label">
                  <span class="scan-plane">CORONAL</span>
                  <span>{{ scanPlaneDesc('coronal', currentReport.analysisType) }}</span>
                </div>
              </div>
              <div class="scan-box" v-if="currentReport.sagittalImage">
                <img :src="currentReport.sagittalImage" alt="Sagittal View" />
                <div class="scan-label">
                  <span class="scan-plane">SAGITTAL</span>
                  <span>{{ scanPlaneDesc('sagittal', currentReport.analysisType) }}</span>
                </div>
              </div>
              <div class="scan-box scan-3d" v-if="currentReport.mesh3dImage">
                <img :src="currentReport.mesh3dImage" alt="3D Volume Render" />
                <div class="scan-label">
                  <span class="scan-plane">3D VOL</span>
                  <span>{{ scan3dDesc(currentReport.analysisType) }}</span>
                </div>
              </div>
              <!-- placeholders for missing images -->
              <template v-for="n in placeholderCount" :key="'ph'+n">
                <div class="scan-box scan-empty">
                  <div class="scan-placeholder">No image</div>
                  <div class="scan-label"><span class="scan-plane">—</span></div>
                </div>
              </template>
            </div>
          </section>

          <div class="divider thin"></div>

          <!-- CLINICAL FINDINGS -->
          <section class="clinical-section">
            <div class="section-title plain">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#1e3a8a" stroke-width="2.5"><path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/></svg>
              Clinical Findings & Recommendations
            </div>

            <div class="clinical-block">
              <div class="cb-label">
                <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                Diagnosis
              </div>
              <p class="cb-text">{{ currentReport.diagnosis || '—' }}</p>
            </div>

            <div class="clinical-block" v-if="currentReport.prescription">
              <div class="cb-label">
                <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M9 3H5a2 2 0 0 0-2 2v4m6-6h10a2 2 0 0 1 2 2v4M9 3v11m0 0H5m4 0h10m-10 0v5a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2v-5m0 0H9"/></svg>
                Prescription & Medication
              </div>
              <p class="cb-text">{{ currentReport.prescription }}</p>
            </div>

            <div class="clinical-block" v-if="currentReport.advice">
              <div class="cb-label">
                <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4"/><path d="M12 8h.01"/></svg>
                Advice & Recommendations
              </div>
              <p class="cb-text">{{ currentReport.advice }}</p>
            </div>
          </section>

          <!-- FOOTER / SIGNATURE -->
          <footer class="paper-footer">
            <div class="pf-sig">
              <div class="sig-line"></div>
              <p class="sig-name">Dr. {{ currentReport.doctorName }}</p>
              <p class="sig-role">{{ getTypeConfig(currentReport.analysisType).label }} Specialist</p>
              <p class="sig-org">NeuroPACS Medical Center</p>
            </div>
            <div class="pf-qr">
              <div class="qr-box">
                <div class="qr-inner">
                  <span class="qr-id">#{{ String(currentReport.id || 'PREV').slice(-6).padStart(6,'0') }}</span>
                </div>
              </div>
              <span class="qr-hint">Report verification</span>
            </div>
            <div class="pf-legal">
              <p>Electronically signed via NeuroPACS AI Imaging System</p>
              <p>Valid without physical signature</p>
              <p>Generated: {{ formatDate(new Date()) }}</p>
              <p>NeuroPACS Medical · AI Diagnostic Division</p>
            </div>
          </footer>

        </div><!-- /paper -->
      </div><!-- /paper-scroll -->
    </div>
    </Teleport>

    <!-- LIGHTBOX -->
    <Teleport to="body">
    <div v-if="lightboxSrc" class="lightbox" @click="lightboxSrc = null">
      <img :src="lightboxSrc" />
      <button @click="lightboxSrc = null">✕</button>
    </div>
    </Teleport>

  </div>
</template>

<script setup>
import { JAVA_API_URL } from '../config.js'
import { ref, reactive, computed } from 'vue'
import axios from 'axios'

const props = defineProps({
  user: { type: Object, required: true },
  // Pass role="patient" from PatientDashboard to hide the Edit button
  role: { type: String, default: 'doctor' }
})
const emit  = defineEmits(['refresh-data'])

const API_BASE = JAVA_API_URL

// ── State ─────────────────────────────────────────────────────────────
const showTypeSelector = ref(false)
const showEditor       = ref(false)
const showViewer       = ref(false)
const isSaving         = ref(false)
const isEditing        = ref(false)
const lightboxSrc      = ref(null)
const activeStep       = ref(0)

const pendingApt    = ref({})
const currentApt    = ref({})
const currentReport = ref({})

const editorSteps = ['Diagnosis', 'Treatment', 'Review']

const reportForm = reactive({
  analysisType: 'brain',
  diagnosis: '', prescription: '', advice: '',
  severity: 'routine', followUpDate: '', reportDate: '',
  axialImage: '', coronalImage: '', sagittalImage: '', mesh3dImage: '',
  brainTumorType: '', brainWhoGrade: '', brainLocation: '', brainVolume: '', brainEnhancing: '',
  spleenSize: '', spleenLength: '', ichFinding: '', spleenParenchyma: '',
  lungFinding: '', lungLobes: '', lungNoduleSize: '', lungPleural: '', lungLobeSummary: '',
})

// ── Config ────────────────────────────────────────────────────────────
const analysisTypes = [
  {
    key: 'brain', icon: '🧠', label: 'Brain MRI',
    desc: 'Glioblastoma & tumor segmentation',
    modality: 'MRI · MONAI BraTS',
    features: ['Tumor classification', 'WHO grade', '3D mesh', 'Multi-modal'],
  },
  {
    key: 'spleen', icon: '🫀', label: 'CT Spleen / ICH',
    desc: 'Spleen segmentation & hemorrhage detection',
    modality: 'CT · nnUNet',
    features: ['Size measurement', 'ICH detection', '3D PLY mesh', 'CT windowing'],
  },
  {
    key: 'lungs', icon: '🫁', label: 'Lung CT',
    desc: 'Lobe segmentation & nodule analysis',
    modality: 'CT · TotalSegmentator',
    features: ['5 lobe mapping', 'Nodule sizing', 'Lobe overlay', 'NRRD export'],
  },
]

const severities = [
  { key: 'routine',  icon: '🟢', label: 'Routine' },
  { key: 'urgent',   icon: '🟡', label: 'Urgent' },
  { key: 'critical', icon: '🔴', label: 'Critical' },
]

// ── FIX: normalizeType — handles null/undefined/"LUNGS"/"Lung CT"/etc ──
function normalizeType(t) {
  if (!t) return 'brain'
  const s = String(t).toLowerCase().trim()
  if (s === 'spleen' || s.includes('spleen') || s.includes('ich')) return 'spleen'
  if (s === 'lungs'  || s.includes('lung')  || s === 'lung ct')   return 'lungs'
  if (s === 'brain'  || s.includes('brain') || s.includes('mri'))  return 'brain'
  return 'brain' // safe default
}

function getTypeConfig(key) {
  const k = normalizeType(key)
  return analysisTypes.find(t => t.key === k) || analysisTypes[0]
}

const currentTypeConfig = computed(() => getTypeConfig(reportForm.analysisType))

const imageCount = computed(() =>
  [reportForm.axialImage, reportForm.coronalImage, reportForm.sagittalImage, reportForm.mesh3dImage].filter(Boolean).length
)
const maxImages = 4

// FIX: placeholder count for print grid
const placeholderCount = computed(() => {
  const have = [currentReport.value.axialImage, currentReport.value.coronalImage, currentReport.value.sagittalImage, currentReport.value.mesh3dImage].filter(Boolean).length
  return Math.max(0, 4 - have)
})

const hasTypeSpecificFields = computed(() => {
  const t = normalizeType(reportForm.analysisType)
  if (t === 'brain')  return !!reportForm.brainTumorType
  if (t === 'spleen') return !!reportForm.spleenSize
  if (t === 'lungs')  return !!reportForm.lungFinding
  return false
})

const diagnosisPlaceholder = computed(() => {
  const t = normalizeType(reportForm.analysisType)
  if (t === 'brain')  return 'MRI Brain with contrast reveals a 12.4 cm³ ring-enhancing lesion in the left frontal lobe with surrounding vasogenic edema, consistent with high-grade glioma (GBM WHO Grade IV)...'
  if (t === 'spleen') return 'CT abdomen shows moderately enlarged spleen (15.2 cm) with no focal lesions. No intraperitoneal hemorrhage identified. Parenchyma appears homogeneous...'
  return 'HRCT chest reveals an 8.5 mm spiculated nodule in the right upper lobe with surrounding ground-glass opacity. All 5 lung lobes segmented by TotalSegmentator...'
})

function severityLabel(s) {
  const map = { routine: '● Routine', urgent: '▲ Urgent', critical: '■ Critical' }
  return map[(s || 'routine').toLowerCase()] || '● Routine'
}

function scanPlaneDesc(plane, type) {
  const t = normalizeType(type)
  if (t === 'brain')  return { axial: 'T1-CE slice', coronal: 'T2 view', sagittal: 'FLAIR view' }[plane] || plane
  if (t === 'spleen') return { axial: 'Abdominal CT', coronal: 'Coronal CT', sagittal: 'Sagittal CT' }[plane] || plane
  return { axial: 'Chest CT', coronal: 'Coronal view', sagittal: 'Sagittal view' }[plane] || plane
}

function scan3dDesc(type) {
  const t = normalizeType(type)
  if (t === 'brain')  return 'Tumor 3D mesh'
  if (t === 'spleen') return 'Spleen 3D render'
  return 'Lobe segmentation map'
}

// ── FIX: normalizeReportFields handles Java camelCase mismatches ──────
function normalizeReportFields(r) {
  if (!r) return {}
  return {
    ...r,
    mesh3dImage:      r.mesh3dImage      || r.mesh3DImage      || r.mesh3dimage      || r.meshImage     || '',
    axialImage:       r.axialImage       || r.axialimage        || '',
    coronalImage:     r.coronalImage     || r.coronalimage      || '',
    sagittalImage:    r.sagittalImage    || r.sagittalimage     || '',
    analysisType:     normalizeType(r.analysisType || r.analysis_type || r.scanType || ''),
    brainTumorType:   r.brainTumorType   || r.brain_tumor_type  || '',
    brainWhoGrade:    r.brainWhoGrade    || r.brain_who_grade   || '',
    brainLocation:    r.brainLocation    || r.brain_location    || '',
    brainVolume:      r.brainVolume      || r.brain_volume      || '',
    brainEnhancing:   r.brainEnhancing   || r.brain_enhancing   || '',
    spleenSize:       r.spleenSize       || r.spleen_size       || '',
    spleenLength:     r.spleenLength     || r.spleen_length     || '',
    ichFinding:       r.ichFinding       || r.ich_finding       || '',
    spleenParenchyma: r.spleenParenchyma || r.spleen_parenchyma || '',
    lungFinding:      r.lungFinding      || r.lung_finding      || '',
    lungLobes:        r.lungLobes        || r.lung_lobes        || '',
    lungNoduleSize:   r.lungNoduleSize   || r.lung_nodule_size  || '',
    lungPleural:      r.lungPleural      || r.lung_pleural      || '',
    lungLobeSummary:  r.lungLobeSummary  || r.lung_lobe_summary || '',
    patientGender:    r.patientGender    || r.gender            || '',
    patientBirthDate: r.patientBirthDate || r.birthDate         || r.dob || '',
    severity:         (r.severity        || 'routine').toLowerCase(),
    reportDate:       r.reportDate       || r.createdAt         || r.created_at || new Date(),
    doctorName:       r.doctorName       || r.doctor_name       || '',
    patientName:      r.patientName      || r.patient_name      || '',
  }
}

// ── Public API ────────────────────────────────────────────────────────
const openEditor = (appointment, existingReport = null) => {
  if (!existingReport) {
    pendingApt.value = appointment
    showTypeSelector.value = true
    return
  }
  currentApt.value    = appointment
  isEditing.value     = true
  currentReport.value = existingReport
  populateForm(existingReport)
  activeStep.value    = 0
  showEditor.value    = true
  showViewer.value    = false
}

// FIX: openViewer now normalizes all field names before displaying
const openViewer = (report, appointmentContext) => {
  const r = normalizeReportFields(report)
  currentReport.value = { ...r }
  if (appointmentContext) {
    if (!currentReport.value.patientGender)    currentReport.value.patientGender    = appointmentContext.patientGender    || ''
    if (!currentReport.value.patientBirthDate) currentReport.value.patientBirthDate = appointmentContext.patientBirthDate || ''
    if (!currentReport.value.dicomPatientId)   currentReport.value.dicomPatientId   = appointmentContext.dicomPatientId   || ''
    if (!currentReport.value.patientName)      currentReport.value.patientName      = appointmentContext.patientName      || ''
    currentApt.value = appointmentContext
  }
  showViewer.value = true
  showEditor.value = false
}

const openFromAnalysis = async (analysisType, appointment, images = {}) => {
  pendingApt.value = appointment
  currentApt.value = appointment
  isEditing.value  = false
  resetForm(analysisType)

  const toBase64 = async (src) => {
    if (!src) return ''
    if (src.startsWith('data:')) return src
    try {
      const res = await fetch(src)
      const blob = await res.blob()
      return await new Promise(resolve => {
        const fr = new FileReader()
        fr.onload = () => resolve(fr.result)
        fr.readAsDataURL(blob)
      })
    } catch { return src }
  }

  reportForm.axialImage    = await toBase64(images.axialImage)
  reportForm.coronalImage  = await toBase64(images.coronalImage)
  reportForm.sagittalImage = await toBase64(images.sagittalImage)
  reportForm.mesh3dImage   = await toBase64(images.mesh3dImage)

  showTypeSelector.value = false
  activeStep.value = 0
  showEditor.value = true
  showViewer.value = false
}

defineExpose({ openEditor, openViewer, openFromAnalysis })

// ── Internal methods ──────────────────────────────────────────────────
function selectType(type) {
  currentApt.value = pendingApt.value
  isEditing.value  = false
  resetForm(type)
  showTypeSelector.value = false
  activeStep.value = 0
  showEditor.value = true
}

function changeType() {
  pendingApt.value = currentApt.value
  showEditor.value = false
  showTypeSelector.value = true
}

function resetForm(type = 'brain') {
  Object.assign(reportForm, {
    analysisType: type, diagnosis: '', prescription: '', advice: '',
    severity: 'routine', followUpDate: '', reportDate: new Date().toISOString(),
    axialImage: '', coronalImage: '', sagittalImage: '', mesh3dImage: '',
    brainTumorType: '', brainWhoGrade: '', brainLocation: '', brainVolume: '', brainEnhancing: '',
    spleenSize: '', spleenLength: '', ichFinding: '', spleenParenchyma: '',
    lungFinding: '', lungLobes: '', lungNoduleSize: '', lungPleural: '', lungLobeSummary: '',
  })
}

function populateForm(r) {
  const n = normalizeReportFields(r)
  Object.assign(reportForm, {
    analysisType:    n.analysisType    || 'brain',
    diagnosis:       n.diagnosis       || '',
    prescription:    n.prescription    || '',
    advice:          n.advice          || '',
    severity:        n.severity        || 'routine',
    followUpDate:    n.followUpDate    || '',
    reportDate:      n.reportDate      || '',
    axialImage:      n.axialImage      || '',
    coronalImage:    n.coronalImage    || '',
    sagittalImage:   n.sagittalImage   || '',
    mesh3dImage:     n.mesh3dImage     || '',
    brainTumorType:  n.brainTumorType  || '',
    brainWhoGrade:   n.brainWhoGrade   || '',
    brainLocation:   n.brainLocation   || '',
    brainVolume:     n.brainVolume     || '',
    brainEnhancing:  n.brainEnhancing  || '',
    spleenSize:      n.spleenSize      || '',
    spleenLength:    n.spleenLength    || '',
    ichFinding:      n.ichFinding      || '',
    spleenParenchyma:n.spleenParenchyma|| '',
    lungFinding:     n.lungFinding     || '',
    lungLobes:       n.lungLobes       || '',
    lungNoduleSize:  n.lungNoduleSize  || '',
    lungPleural:     n.lungPleural     || '',
    lungLobeSummary: n.lungLobeSummary || '',
  })
}

function handleUpload(e, field) {
  const file = e.target.files?.[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = evt => { reportForm[field] = evt.target.result }
  reader.readAsDataURL(file)
  e.target.value = ''
}

async function saveReport() {
  if (!reportForm.diagnosis) { alert('Please enter a diagnosis before saving.'); return }
  isSaving.value = true
  const payload = {
    id:            isEditing.value ? currentReport.value.id : null,
    appointmentId: currentApt.value.id,
    patientId:     currentApt.value.patientId,
    doctorId:      props.user.id || 999,
    patientName:   currentApt.value.patientName,
    doctorName:    props.user.username || 'Dr. Unknown',
    reportDate:    new Date().toISOString(),
    ...Object.fromEntries(Object.entries(reportForm)),
  }
  try {
    const saveRes = await axios.post(`${API_BASE}/reports/save`, payload)
    await axios.put(`${API_BASE}/appointments/${currentApt.value.id}/status`, { status: 'COMPLETED' })
    showEditor.value = false
    emit('refresh-data')
    // FIX: Show viewer immediately after saving with the saved data
    const savedReport = saveRes.data || payload
    openViewer({ ...savedReport, ...payload }, currentApt.value)
  } catch (e) {
    alert('Failed to save report: ' + (e.response?.data?.message || e.message))
  } finally {
    isSaving.value = false
  }
}

function previewReport() {
  currentReport.value = normalizeReportFields({
    ...Object.fromEntries(Object.entries(reportForm)),
    patientName:      currentApt.value.patientName,
    patientId:        currentApt.value.patientId,
    patientGender:    currentApt.value.patientGender,
    patientBirthDate: currentApt.value.patientBirthDate,
    dicomPatientId:   currentApt.value.dicomPatientId,
    doctorName:       props.user.username || 'Dr. Unknown',
    reportDate:       new Date(),
    id:               currentReport.value?.id || 'PREVIEW',
  })
  showViewer.value = true
}

function enableEditMode() {
  const apt = (currentApt.value && currentApt.value.id) ? currentApt.value : { ...currentReport.value }
  showViewer.value = false
  openEditor(apt, currentReport.value)
}

// FIX: Print via injected style tag — bypasses Vue scoped CSS and modal z-index issues
function handlePrint() {
  const existingStyle = document.getElementById('rs-print-inject')
  if (existingStyle) existingStyle.remove()

  const style = document.createElement('style')
  style.id = 'rs-print-inject'
  style.textContent = `
    @media print {
      body > *:not(#rs-viewer-overlay) { display: none !important; visibility: hidden !important; }
      #rs-viewer-overlay {
        display: block !important; visibility: visible !important;
        position: static !important; background: white !important;
        padding: 0 !important; overflow: visible !important;
        height: auto !important; width: 100% !important;
        backdrop-filter: none !important;
      }
      #rs-no-print, .viewer-toolbar { display: none !important; }
      .paper-scroll {
        overflow: visible !important; height: auto !important;
        padding: 0 !important; background: white !important;
      }
      #rs-paper {
        box-shadow: none !important; border-radius: 0 !important;
        margin: 0 auto !important; max-width: 100% !important;
        padding: 15mm 18mm !important;
      }
      * {
        -webkit-print-color-adjust: exact !important;
        print-color-adjust: exact !important;
        color-adjust: exact !important;
      }
      .scan-grid     { page-break-inside: avoid !important; }
      .paper-footer  { page-break-inside: avoid !important; }
      .clinical-block { page-break-inside: avoid !important; }
      .section-title { page-break-after: avoid !important; }
      .divider.thick { border-top: 2px solid #1e3a8a !important; }
      .divider.thin  { border-top: 1px solid #e5e7eb !important; }
      .ptb-brain  { background: #dbeafe !important; color: #1d4ed8 !important; border: 1px solid #93c5fd !important; }
      .ptb-spleen { background: #fce7f3 !important; color: #9d174d !important; border: 1px solid #f9a8d4 !important; }
      .ptb-lungs  { background: #d1fae5 !important; color: #065f46 !important; border: 1px solid #6ee7b7 !important; }
      .svb-routine  { color: #15803d !important; }
      .svb-urgent   { color: #b45309 !important; }
      .svb-critical { color: #dc2626 !important; }
      .scan-box img { max-height: 150px !important; object-fit: contain !important; }
      .ph-logo { background: linear-gradient(135deg, #1e3a8a, #2563eb) !important; }
      .section-title { background: #f1f5f9 !important; border-left: 4px solid #1e3a8a !important; }
      .findings-table .ft-row:nth-child(even) { background: #fafafa !important; }
      .pig-col { background: #fafafa !important; }
      .pig-col:nth-child(even) { background: #fff !important; }
    }
  `
  document.head.appendChild(style)
  window.print()
  setTimeout(() => {
    const s = document.getElementById('rs-print-inject')
    if (s) s.remove()
  }, 3000)
}

function openLightbox(src) { lightboxSrc.value = src }

function formatDate(d) {
  try { return new Date(d).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' }) }
  catch { return '—' }
}

function initials(name) {
  return (name || 'P').split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2)
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Syne:wght@600;700;800&family=DM+Mono:wght@400;500&family=DM+Sans:wght@300;400;500;600;700&display=swap');

:root {
  --c-brain:      #818cf8;
  --c-brain-bg:   rgba(129,140,248,.12);
  --c-brain-bdr:  rgba(129,140,248,.3);
  --c-spleen:     #f472b6;
  --c-spleen-bg:  rgba(244,114,182,.12);
  --c-spleen-bdr: rgba(244,114,182,.3);
  --c-lungs:      #34d399;
  --c-lungs-bg:   rgba(52,211,153,.12);
  --c-lungs-bdr:  rgba(52,211,153,.3);
}

/* OVERLAY */
.rs-overlay {
  position: fixed; inset: 0;
  background: rgba(4,10,22,.88);
  display: flex; align-items: center; justify-content: center;
  z-index: 9999;
  backdrop-filter: blur(8px);
  padding: 16px;
}
.viewer-overlay {
  align-items: flex-start;
  overflow-y: auto;
  padding: 0;
  flex-direction: column;
}

/* ═══════════ TYPE SELECTOR ═══════════ */
.ts-card {
  background: #0b1525;
  border: 1px solid rgba(255,255,255,.07);
  border-radius: 20px;
  width: 780px; max-width: 100%;
  box-shadow: 0 40px 100px rgba(0,0,0,.7);
  overflow: hidden;
}
.ts-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 22px 26px 16px;
  border-bottom: 1px solid rgba(255,255,255,.06);
}
.ts-title-row { display: flex; align-items: center; gap: 14px; }
.ts-logo {
  width: 44px; height: 44px; border-radius: 12px;
  background: linear-gradient(135deg, #1e3a8a, #3b82f6);
  display: flex; align-items: center; justify-content: center;
  font-family: 'Syne', sans-serif; font-weight: 800; font-size: 1.3rem; color: white;
}
.ts-title-row h3 { margin: 0; font-family: 'Syne', sans-serif; font-size: 1.1rem; color: #f1f5f9; }
.ts-title-row p  { margin: 3px 0 0; font-size: 0.78rem; color: #475569; }

.ts-patient-bar {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 26px;
  background: rgba(255,255,255,.025);
  border-bottom: 1px solid rgba(255,255,255,.05);
  font-size: 0.81rem;
}
.tsb-avatar {
  width: 28px; height: 28px; border-radius: 8px;
  background: linear-gradient(135deg, #2563eb, #7c3aed);
  color: white; display: flex; align-items: center; justify-content: center;
  font-size: 0.7rem; font-weight: 700;
}
.tsb-name    { color: #e2e8f0; font-weight: 600; }
.tsb-divider { color: #1e3a5f; }
.tsb-label   { color: #334155; font-size: 0.66rem; font-weight: 700; text-transform: uppercase; letter-spacing: .5px; }
.tsb-id      { color: #60a5fa; font-family: 'DM Mono', monospace; font-size: 0.73rem; }
.tsb-dept    { margin-left: auto; background: rgba(37,99,235,.12); color: #60a5fa; font-size: 0.68rem; padding: 2px 8px; border-radius: 99px; }

.type-grid { display: grid; grid-template-columns: repeat(3,1fr); gap: 14px; padding: 18px 22px 26px; }
.type-card {
  background: #0e1d30; border: 1.5px solid rgba(255,255,255,.06); border-radius: 14px;
  padding: 18px 14px 14px; cursor: pointer; text-align: left;
  display: flex; flex-direction: column; gap: 6px;
  transition: all .2s; position: relative; overflow: hidden;
}
.type-card::after { content: ''; position: absolute; inset: 0; opacity: 0; transition: opacity .2s; }
.tc-brain::after  { background: radial-gradient(circle at 20% 20%, var(--c-brain-bg), transparent 70%); }
.tc-spleen::after { background: radial-gradient(circle at 20% 20%, var(--c-spleen-bg), transparent 70%); }
.tc-lungs::after  { background: radial-gradient(circle at 20% 20%, var(--c-lungs-bg), transparent 70%); }
.type-card:hover { transform: translateY(-4px); }
.type-card:hover::after { opacity: 1; }
.tc-brain:hover  { border-color: var(--c-brain); }
.tc-spleen:hover { border-color: var(--c-spleen); }
.tc-lungs:hover  { border-color: var(--c-lungs); }
.tc-top { display: flex; justify-content: space-between; align-items: center; }
.tc-icon { font-size: 2rem; }
.tc-modality { font-size: 0.6rem; color: #334155; background: rgba(255,255,255,.05); padding: 2px 7px; border-radius: 4px; }
.tc-name { font-family: 'Syne', sans-serif; font-size: 0.92rem; font-weight: 700; color: #f1f5f9; }
.tc-desc { font-size: 0.72rem; color: #64748b; line-height: 1.4; }
.tc-pills { display: flex; flex-wrap: wrap; gap: 4px; }
.tc-pills span { font-size: 0.6rem; color: #94a3b8; background: rgba(255,255,255,.05); border: 1px solid rgba(255,255,255,.07); border-radius: 4px; padding: 2px 6px; }
.tc-cta { font-size: 0.7rem; font-weight: 700; color: #334155; margin-top: auto; transition: color .2s; }
.type-card:hover .tc-cta { color: #60a5fa; }

/* ═══════════ EDITOR ═══════════ */
.editor-card {
  background: #08111e; border: 1px solid rgba(255,255,255,.06);
  border-radius: 18px; width: 97vw; max-width: 1520px; height: 93vh;
  box-shadow: 0 40px 100px rgba(0,0,0,.7);
  display: flex; flex-direction: column; overflow: hidden;
}
.ed-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 14px 22px; flex-shrink: 0;
  border-bottom: 1px solid rgba(255,255,255,.06);
}
.edh-brain  { background: linear-gradient(90deg, #0f172a 0%, #1e1b4b 100%); }
.edh-spleen { background: linear-gradient(90deg, #0f172a 0%, #1c0e28 100%); }
.edh-lungs  { background: linear-gradient(90deg, #0f172a 0%, #092515 100%); }
.ed-header-left { display: flex; align-items: center; gap: 14px; }
.ed-type-chip {
  font-family: 'Syne', sans-serif; font-size: 0.8rem; font-weight: 700;
  padding: 5px 12px; border-radius: 8px;
  background: rgba(255,255,255,.1); color: #f1f5f9; white-space: nowrap;
}
.chip-brain  { background: var(--c-brain-bg);  border: 1px solid var(--c-brain-bdr);  color: var(--c-brain); }
.chip-spleen { background: var(--c-spleen-bg); border: 1px solid var(--c-spleen-bdr); color: var(--c-spleen); }
.chip-lungs  { background: var(--c-lungs-bg);  border: 1px solid var(--c-lungs-bdr);  color: var(--c-lungs); }
.ed-header h3 { margin: 0; color: #f1f5f9; font-size: 0.96rem; font-weight: 600; }
.ed-sub       { font-size: 0.74rem; color: #475569; }
.ed-header-right { display: flex; align-items: center; gap: 8px; }

.step-bar {
  display: flex; background: #060e1b;
  border-bottom: 1px solid rgba(255,255,255,.06);
  padding: 0 22px; flex-shrink: 0;
}
.step-btn {
  display: flex; align-items: center; gap: 8px;
  padding: 11px 18px; background: none; border: none;
  border-bottom: 2px solid transparent;
  font-size: 0.78rem; font-weight: 600; color: #334155;
  cursor: pointer; transition: all .2s;
}
.step-btn.active { color: #60a5fa; border-bottom-color: #2563eb; }
.step-btn.done   { color: #22c55e; }
.step-circle {
  width: 20px; height: 20px; border-radius: 50%; background: rgba(255,255,255,.07);
  display: flex; align-items: center; justify-content: center; font-size: 0.7rem;
}
.step-btn.active .step-circle { background: #2563eb; color: white; }
.step-btn.done   .step-circle { background: #22c55e; color: white; }

.ed-body { display: flex; flex: 1; overflow: hidden; }

.ed-sidebar {
  width: 286px; flex-shrink: 0;
  background: #0b1829; border-right: 1px solid rgba(255,255,255,.06);
  overflow-y: auto; padding: 12px 10px;
  display: flex; flex-direction: column; gap: 10px;
}
.ed-sidebar::-webkit-scrollbar { width: 3px; }
.ed-sidebar::-webkit-scrollbar-thumb { background: #1e3a5f; }

.patient-card {
  background: #0f1f35; border: 1px solid rgba(255,255,255,.06);
  border-radius: 12px; padding: 12px;
  display: flex; gap: 10px; align-items: flex-start;
}
.pt-avatar {
  width: 36px; height: 36px; border-radius: 9px; flex-shrink: 0;
  background: linear-gradient(135deg, #2563eb, #7c3aed);
  color: white; display: flex; align-items: center; justify-content: center;
  font-weight: 700; font-size: 0.82rem;
}
.pt-meta strong { display: block; font-size: 0.84rem; color: #f1f5f9; margin-bottom: 5px; }
.pt-row { display: flex; justify-content: space-between; font-size: 0.7rem; }
.pt-row span { color: #334155; }
.pt-row b    { color: #64748b; }

.type-badge-row {
  border-radius: 10px; padding: 9px 12px;
  display: flex; align-items: center; gap: 9px;
  border: 1px solid rgba(255,255,255,.07);
}
.tbr-brain  { background: var(--c-brain-bg);  border-color: var(--c-brain-bdr); }
.tbr-spleen { background: var(--c-spleen-bg); border-color: var(--c-spleen-bdr); }
.tbr-lungs  { background: var(--c-lungs-bg);  border-color: var(--c-lungs-bdr); }
.tbr-icon { font-size: 1.4rem; }
.tbr-top  { display: block; font-size: 0.6rem; color: #334155; font-weight: 700; text-transform: uppercase; letter-spacing: .5px; }
.tbr-val  { display: block; font-size: 0.8rem; font-weight: 700; color: #f1f5f9; }
.tbr-change {
  margin-left: auto; background: rgba(255,255,255,.07); border: none;
  color: #64748b; width: 26px; height: 26px; border-radius: 6px; cursor: pointer; font-size: 1rem;
  display: flex; align-items: center; justify-content: center; transition: all .15s;
}
.tbr-change:hover { background: rgba(255,255,255,.14); color: #f1f5f9; }

.img-section { display: flex; flex-direction: column; gap: 7px; }
.img-section-title {
  display: flex; align-items: center; gap: 5px;
  font-size: 0.7rem; font-weight: 700; color: #475569;
  text-transform: uppercase; letter-spacing: .5px;
}
.img-group-lbl {
  font-size: 0.6rem; font-weight: 700; text-transform: uppercase;
  letter-spacing: .5px; color: #1e3a5f; margin-top: 3px;
}
.img-slot {
  background: #0d1e33; border: 1px solid rgba(255,255,255,.06); border-radius: 7px; overflow: hidden;
}
.is-header {
  display: flex; align-items: center; gap: 5px; padding: 5px 8px;
  background: rgba(255,255,255,.02); border-bottom: 1px solid rgba(255,255,255,.04);
}
.is-header span { flex: 1; font-size: 0.68rem; font-weight: 700; color: #475569; text-transform: capitalize; }
.is-btn {
  font-size: 0.65rem; cursor: pointer; padding: 2px 5px;
  border-radius: 4px; border: none; font-weight: 700; transition: background .15s;
}
.is-btn.upload { background: rgba(37,99,235,.15); color: #60a5fa; }
.is-btn.upload:hover { background: rgba(37,99,235,.3); }
.is-btn.clear  { background: rgba(239,68,68,.12); color: #f87171; }
.is-btn.clear:hover { background: rgba(239,68,68,.25); }
.is-preview {
  height: 90px; background: #060e1b;
  display: flex; align-items: center; justify-content: center;
  cursor: zoom-in; overflow: hidden;
}
.is-preview img { width: 100%; height: 100%; object-fit: contain; }
.is-empty { display: flex; flex-direction: column; align-items: center; gap: 3px; color: #1e3a5f; }
.is-empty span:first-child { font-size: 1.5rem; opacity: .35; }
.is-empty small { font-size: 0.62rem; }

.ed-main {
  flex: 1; overflow-y: auto; padding: 24px 28px; background: #060e1b;
}
.ed-main::-webkit-scrollbar { width: 4px; }
.ed-main::-webkit-scrollbar-thumb { background: #1e3a5f; }

.step-pane { display: flex; flex-direction: column; gap: 16px; min-height: 100%; }
.pane-header { display: flex; align-items: flex-start; gap: 14px; }
.pane-num {
  width: 36px; height: 36px; border-radius: 10px; flex-shrink: 0;
  background: linear-gradient(135deg, #1d4ed8, #7c3aed);
  color: white; font-family: 'Syne', sans-serif; font-size: 1rem; font-weight: 800;
  display: flex; align-items: center; justify-content: center;
}
.pane-header h4 { margin: 0; color: #f1f5f9; font-size: 0.95rem; font-weight: 600; }
.pane-header p  { margin: 3px 0 0; font-size: 0.76rem; color: #475569; }

.findings-grid {
  display: grid; grid-template-columns: 1fr 1fr; gap: 11px;
  background: #0b1829; border: 1px solid rgba(255,255,255,.06);
  border-radius: 12px; padding: 16px;
}
.fg-field { display: flex; flex-direction: column; gap: 5px; }
.fg-field.full { grid-column: 1/-1; }
.fg-field label { font-size: 0.7rem; font-weight: 700; color: #475569; text-transform: uppercase; letter-spacing: .4px; }
.req { color: #f87171; }

.f-select, .f-input {
  background: #0d1e33; border: 1px solid rgba(255,255,255,.08);
  border-radius: 7px; color: #e2e8f0;
  padding: 8px 10px; font-size: 0.83rem;
  font-family: 'DM Sans', sans-serif; width: 100%; transition: border-color .15s;
}
.f-select:focus, .f-input:focus { outline: none; border-color: #2563eb; box-shadow: 0 0 0 3px rgba(37,99,235,.12); }
.f-select option { background: #0d1e33; }

.f-group { display: flex; flex-direction: column; gap: 6px; }
.f-group label { font-size: 0.78rem; font-weight: 700; color: #64748b; }
.f-group.mt { margin-top: 6px; }
.f-hint { font-size: 0.66rem; color: #1e3a5f; text-align: right; }

.f-textarea {
  background: #0d1e33; border: 1px solid rgba(255,255,255,.08); border-radius: 10px;
  color: #e2e8f0; padding: 11px 13px; font-size: 0.85rem;
  font-family: 'DM Sans', sans-serif; line-height: 1.6;
  resize: vertical; width: 100%; box-sizing: border-box; transition: border-color .15s;
}
.f-textarea:focus { outline: none; border-color: #2563eb; box-shadow: 0 0 0 3px rgba(37,99,235,.12); }

.sev-row { display: flex; gap: 8px; }
.sev-btn {
  flex: 1; padding: 9px 10px; border-radius: 8px;
  border: 1.5px solid rgba(255,255,255,.07); background: #0d1e33;
  color: #64748b; font-size: 0.78rem; font-weight: 600; cursor: pointer; transition: all .15s;
}
.sev-btn:hover { border-color: rgba(255,255,255,.15); color: #94a3b8; }
.sev-routine.active  { border-color: #22c55e; background: rgba(34,197,94,.1);  color: #4ade80; }
.sev-urgent.active   { border-color: #eab308; background: rgba(234,179,8,.1);  color: #fbbf24; }
.sev-critical.active { border-color: #ef4444; background: rgba(239,68,68,.1);  color: #f87171; }

.review-grid { display: grid; grid-template-columns: repeat(4,1fr); gap: 10px; }
.rv-tile {
  background: #0b1829; border: 1px solid rgba(255,255,255,.06);
  border-radius: 10px; padding: 12px 14px;
  display: flex; flex-direction: column; gap: 4px;
}
.rv-lbl { font-size: 0.63rem; text-transform: uppercase; color: #334155; font-weight: 700; }
.rv-val { font-size: 0.88rem; font-weight: 600; color: #f1f5f9; }
.sev-t-routine  { color: #4ade80; }
.sev-t-urgent   { color: #fbbf24; }
.sev-t-critical { color: #f87171; }

.rv-diag {
  background: #0b1829; border: 1px solid rgba(255,255,255,.06);
  border-radius: 10px; padding: 14px;
}
.rv-diag .rv-lbl { margin-bottom: 8px; }
.rv-diag p { margin: 0; font-size: 0.83rem; color: #64748b; line-height: 1.6; }

.checklist { display: flex; flex-direction: column; gap: 8px; }
.cl-row {
  display: flex; align-items: center; gap: 10px;
  font-size: 0.78rem; color: #334155; padding: 7px 12px;
  background: rgba(255,255,255,.02); border-radius: 7px;
}
.cl-row.ok { color: #64748b; background: rgba(34,197,94,.04); }
.cl-row span {
  width: 20px; height: 20px; border-radius: 50%;
  background: rgba(255,255,255,.05); display: flex; align-items: center;
  justify-content: center; font-size: 0.68rem; flex-shrink: 0;
}
.cl-row.ok span { background: rgba(34,197,94,.2); color: #22c55e; }

.step-nav {
  display: flex; justify-content: space-between; align-items: center;
  padding-top: 16px; border-top: 1px solid rgba(255,255,255,.06); margin-top: auto;
}
.final-btns { display: flex; gap: 10px; }

/* Buttons */
.icon-btn {
  background: rgba(255,255,255,.07); border: none; color: #94a3b8;
  width: 32px; height: 32px; border-radius: 8px; cursor: pointer;
  display: flex; align-items: center; justify-content: center; font-size: 1rem;
  transition: all .15s;
}
.icon-btn:hover { background: rgba(239,68,68,.2); color: #f87171; }
.icon-btn.light { color: #e2e8f0; }
.ghost-btn {
  background: rgba(255,255,255,.07); border: 1px solid rgba(255,255,255,.1);
  color: #94a3b8; padding: 6px 14px; border-radius: 8px;
  font-size: 0.74rem; font-weight: 600; cursor: pointer;
  display: flex; align-items: center; gap: 6px; transition: all .15s;
}
.ghost-btn:hover { background: rgba(255,255,255,.13); color: #f1f5f9; }
.btn-back {
  background: none; border: 1px solid rgba(255,255,255,.1); color: #334155;
  padding: 9px 18px; border-radius: 8px; font-size: 0.78rem; cursor: pointer; transition: all .15s;
}
.btn-back:hover { border-color: rgba(255,255,255,.2); color: #64748b; }
.btn-next {
  background: #2563eb; border: none; color: white;
  padding: 9px 22px; border-radius: 8px;
  font-family: 'Syne', sans-serif; font-size: 0.82rem; font-weight: 700; cursor: pointer; transition: all .2s;
}
.btn-next:hover { background: #1d4ed8; transform: translateY(-1px); }
.btn-preview {
  background: #0d1e33; border: 1px solid #1e3a5f; color: #475569;
  padding: 9px 18px; border-radius: 8px; font-size: 0.78rem; font-weight: 600; cursor: pointer; transition: all .15s;
}
.btn-preview:hover { border-color: #2563eb; color: #60a5fa; }
.btn-finalize {
  background: linear-gradient(135deg, #16a34a, #15803d); border: none; color: white;
  padding: 10px 26px; border-radius: 9px;
  font-family: 'Syne', sans-serif; font-size: 0.84rem; font-weight: 700; cursor: pointer;
  display: flex; align-items: center; gap: 8px;
  box-shadow: 0 4px 14px rgba(22,163,74,.3); transition: all .2s;
}
.btn-finalize:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(22,163,74,.4); }
.btn-finalize:disabled { opacity: .5; cursor: not-allowed; transform: none; box-shadow: none; }
.spin {
  width: 14px; height: 14px;
  border: 2px solid rgba(255,255,255,.3); border-top-color: white;
  border-radius: 50%; animation: rspin .7s linear infinite;
}
@keyframes rspin { to { transform: rotate(360deg); } }

/* ═══════════ VIEWER TOOLBAR ═══════════ */
.viewer-toolbar {
  position: sticky; top: 0; z-index: 10;
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 24px;
  background: #060e1b;
  border-bottom: 1px solid rgba(255,255,255,.08);
  width: 100%; box-sizing: border-box; flex-shrink: 0;
}
.vt-left { display: flex; align-items: center; gap: 12px; }
.vt-logo {
  width: 32px; height: 32px; border-radius: 8px;
  background: linear-gradient(135deg, #1e3a8a, #3b82f6);
  display: flex; align-items: center; justify-content: center;
  font-family: 'Syne', sans-serif; font-weight: 800; color: white; font-size: 1rem;
}
.vt-title { font-family: 'Syne', sans-serif; font-weight: 700; color: #f1f5f9; font-size: 0.88rem; }
.vt-type-chip { font-size: 0.7rem; font-weight: 700; padding: 3px 10px; border-radius: 5px; }
.vtc-brain  { background: var(--c-brain-bg);  color: var(--c-brain);  border: 1px solid var(--c-brain-bdr); }
.vtc-spleen { background: var(--c-spleen-bg); color: var(--c-spleen); border: 1px solid var(--c-spleen-bdr); }
.vtc-lungs  { background: var(--c-lungs-bg);  color: var(--c-lungs);  border: 1px solid var(--c-lungs-bdr); }
.vt-right { display: flex; gap: 8px; }
.vt-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 7px 14px; border-radius: 8px; border: none;
  font-size: 0.74rem; font-weight: 700; cursor: pointer; transition: all .15s;
}
.vt-btn.edit  { background: rgba(234,179,8,.12); color: #fbbf24; border: 1px solid rgba(234,179,8,.2); }
.vt-btn.print { background: rgba(37,99,235,.12);  color: #60a5fa;  border: 1px solid rgba(37,99,235,.2); }
.vt-btn.close { background: rgba(239,68,68,.12);  color: #f87171;  border: 1px solid rgba(239,68,68,.2); }
.vt-btn:hover { filter: brightness(1.25); transform: translateY(-1px); }

/* ═══════════ A4 PAPER ═══════════ */
.paper-scroll {
  flex: 1; overflow-y: auto; padding: 32px 24px 60px;
  background: #0a1424; width: 100%;
}
.paper {
  background: #ffffff; max-width: 860px; margin: 0 auto;
  padding: 44px 52px; box-shadow: 0 30px 80px rgba(0,0,0,.5);
  border-radius: 4px; font-family: 'DM Sans', Georgia, serif;
  color: #111; font-size: 13px; line-height: 1.55;
}

/* Paper header */
.ph-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 20px; }
.ph-brand  { display: flex; align-items: center; gap: 14px; }
.ph-logo {
  width: 52px; height: 52px; border-radius: 12px;
  background: linear-gradient(135deg, #1e3a8a, #2563eb);
  display: flex; align-items: center; justify-content: center;
}
.ph-logo span { font-family: 'Syne', sans-serif; font-size: 1.6rem; font-weight: 800; color: white; }
.ph-name { margin: 0; font-family: 'Syne', sans-serif; font-size: 1.3rem; font-weight: 800; color: #0f172a; }
.ph-name span { font-weight: 400; color: #64748b; font-size: 0.95rem; }
.ph-tagline { margin: 3px 0 0; font-size: 10.5px; color: #94a3b8; letter-spacing: .3px; }

.ph-meta  { text-align: right; display: flex; flex-direction: column; align-items: flex-end; gap: 8px; }
.ph-type-badge {
  font-family: 'Syne', sans-serif; font-weight: 700; font-size: 0.78rem;
  padding: 5px 14px; border-radius: 6px; border: 1px solid;
}
.ptb-brain  { background: #dbeafe; color: #1d4ed8; border-color: #93c5fd; }
.ptb-spleen { background: #fce7f3; color: #9d174d; border-color: #f9a8d4; }
.ptb-lungs  { background: #d1fae5; color: #065f46; border-color: #6ee7b7; }

.ph-info-table { border-collapse: collapse; }
.ph-info-table td { font-size: 11.5px; padding: 2px 0 2px 12px; color: #374151; }
.ph-info-table td:first-child { color: #9ca3af; min-width: 70px; }
.ph-info-table td strong { color: #111827; }
.ph-status { color: #15803d; font-weight: 700; }

.divider { border: none; margin: 16px 0; }
.divider.thick { border-top: 2px solid #1e3a8a; }
.divider.thin  { border-top: 1px solid #e5e7eb; }

/* Patient info grid */
.patient-info-grid {
  display: grid; grid-template-columns: repeat(4,1fr);
  gap: 0; margin-bottom: 14px;
  border: 1px solid #e5e7eb; border-radius: 8px; overflow: hidden;
}
.pig-col {
  padding: 10px 14px; border-right: 1px solid #f3f4f6;
  display: flex; flex-direction: column; gap: 2px; background: #fafafa;
}
.pig-col:nth-child(even) { background: #fff; }
.pig-col:last-child { border-right: none; }
.pig-lbl { font-size: 9px; text-transform: uppercase; letter-spacing: .5px; color: #9ca3af; font-weight: 700; }
.pig-val { font-size: 12.5px; color: #111827; font-weight: 500; }
.pig-val.mono { font-family: 'DM Mono', monospace; font-size: 11.5px; }
.sev-badge { font-weight: 700; }
.svb-routine  { color: #15803d; }
.svb-urgent   { color: #b45309; }
.svb-critical { color: #dc2626; }

/* Section titles */
.section-title {
  display: flex; align-items: center; gap: 10px;
  background: #f1f5f9; border-left: 4px solid #1e3a8a;
  padding: 7px 14px; margin: 0 0 12px; border-radius: 0 6px 6px 0;
}
.section-title.plain { gap: 8px; }
.st-icon {
  width: 26px; height: 26px; border-radius: 7px;
  display: flex; align-items: center; justify-content: center; font-size: 0.95rem;
}
.sti-brain  { background: #dbeafe; }
.sti-spleen { background: #fce7f3; }
.sti-lungs  { background: #d1fae5; }
.st-label { font-family: 'Syne', sans-serif; font-size: 10.5px; font-weight: 800; text-transform: uppercase; letter-spacing: .6px; color: #1e293b; display: block; }
.st-sub   { font-size: 9.5px; color: #94a3b8; display: block; }

/* AI Findings */
.ai-section { margin-bottom: 16px; }
.findings-table {
  display: grid; grid-template-columns: 1fr 1fr;
  border: 1px solid #e5e7eb; border-radius: 8px; overflow: hidden;
}
.ft-row {
  display: flex; flex-direction: column; gap: 2px;
  padding: 9px 14px; border-bottom: 1px solid #f3f4f6; background: #fff;
}
.ft-row:nth-child(even) { background: #fafafa; }
.ft-row.full { grid-column: 1/-1; }
.ft-row span   { font-size: 9px; text-transform: uppercase; letter-spacing: .4px; color: #9ca3af; font-weight: 700; }
.ft-row strong { font-size: 12.5px; color: #111827; font-weight: 500; }

/* Imaging section */
.imaging-section { margin-bottom: 16px; }
.scan-grid { display: grid; grid-template-columns: repeat(4,1fr); gap: 10px; }
.scan-box { border: 1px solid #e5e7eb; border-radius: 6px; overflow: hidden; display: flex; flex-direction: column; }
.scan-box img { width: 100%; height: 155px; object-fit: contain; background: #000; display: block; }
.scan-3d { border-color: #bfdbfe; }
.scan-placeholder { height: 155px; background: #f9fafb; display: flex; align-items: center; justify-content: center; font-size: 10px; color: #d1d5db; }
.scan-empty { border-style: dashed; border-color: #f0f0f0; }
.scan-label { display: flex; justify-content: space-between; align-items: center; padding: 5px 8px; background: #f9fafb; }
.scan-plane { font-size: 8.5px; font-weight: 800; font-family: 'DM Mono', monospace; color: #475569; letter-spacing: .5px; }
.scan-label span:last-child { font-size: 9.5px; color: #9ca3af; }

/* Clinical section */
.clinical-section { margin-bottom: 16px; }
.clinical-block {
  margin-bottom: 10px; padding: 11px 15px;
  border: 1px solid #f3f4f6; border-radius: 6px; background: #fafafa;
}
.cb-label {
  display: flex; align-items: center; gap: 6px;
  font-family: 'Syne', sans-serif; font-size: 9.5px; font-weight: 800;
  text-transform: uppercase; letter-spacing: .5px; color: #1e293b; margin-bottom: 6px;
}
.cb-text { margin: 0; font-size: 12.5px; color: #1e293b; line-height: 1.65; white-space: pre-wrap; }

/* Footer */
.paper-footer {
  display: flex; justify-content: space-between; align-items: flex-end;
  border-top: 1px solid #e5e7eb; padding-top: 22px; margin-top: 40px; gap: 20px;
}
.pf-sig { text-align: center; min-width: 180px; }
.sig-line { height: 36px; border-bottom: 1.5px solid #334155; margin-bottom: 7px; }
.sig-name { margin: 0; font-weight: 700; font-size: 13px; color: #111827; }
.sig-role { margin: 2px 0 0; font-size: 10.5px; color: #64748b; }
.sig-org  { margin: 1px 0 0; font-size: 9.5px; color: #9ca3af; }
.pf-qr { text-align: center; }
.qr-box {
  width: 62px; height: 62px; border: 2px solid #e5e7eb; border-radius: 8px;
  display: flex; align-items: center; justify-content: center; background: #f9fafb; margin: 0 auto 4px;
}
.qr-inner { text-align: center; }
.qr-id { font-family: 'DM Mono', monospace; font-size: 8.5px; color: #6b7280; }
.qr-hint { font-size: 9px; color: #9ca3af; }
.pf-legal { font-size: 9.5px; color: #9ca3af; max-width: 42%; text-align: right; line-height: 1.7; }
.pf-legal p { margin: 0; }

/* LIGHTBOX */
.lightbox {
  position: fixed; inset: 0; background: rgba(0,0,0,.93);
  z-index: 99999; display: flex; align-items: center; justify-content: center; cursor: zoom-out;
}
.lightbox img { max-width: 92vw; max-height: 92vh; object-fit: contain; border-radius: 6px; }
.lightbox button {
  position: absolute; top: 18px; right: 22px;
  background: rgba(255,255,255,.1); border: none; color: white;
  width: 36px; height: 36px; border-radius: 50%; font-size: 1rem; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
}
</style>