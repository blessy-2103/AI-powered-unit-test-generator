const API_BASE = "/api/testcases";

// ---------------- Auth guard ----------------
const authToken = localStorage.getItem("authToken");
const authUsername = localStorage.getItem("authUsername");

if (!authToken) {
  window.location.href = "login.html";
}

function authHeaders(extra = {}) {
  return { ...extra, "Authorization": `Bearer ${authToken}` };
}

function handleAuthFailure(response) {
  if (response.status === 401 || response.status === 403) {
    localStorage.removeItem("authToken");
    localStorage.removeItem("authUsername");
    window.location.href = "login.html";
    return true;
  }
  return false;
}

document.getElementById("userBadge").textContent = authUsername || "—";
document.getElementById("logoutBtn").addEventListener("click", async () => {
  try {
    await fetch("/api/auth/logout", { method: "POST", headers: authHeaders() });
  } catch (e) {
    // ignore network errors on logout — clear the token client-side regardless
  }
  localStorage.removeItem("authToken");
  localStorage.removeItem("authUsername");
  window.location.href = "login.html";
});

const sourceCodeInput = document.getElementById("sourceCodeInput");
const languageSelect = document.getElementById("languageSelect");
const frameworkSelect = document.getElementById("frameworkSelect");
const testStyleSelect = document.getElementById("testStyleSelect");
const classNameHint = document.getElementById("classNameHint");
const generateBtn = document.getElementById("generateBtn");
const generateBtnText = document.getElementById("generateBtnText");
const generateSpinner = document.getElementById("generateSpinner");

const outputEmpty = document.getElementById("outputEmpty");
const outputCode = document.getElementById("outputCode");
const outputCodeContent = document.getElementById("outputCodeContent");
const errorBanner = document.getElementById("errorBanner");
const copyBtn = document.getElementById("copyBtn");
const downloadBtn = document.getElementById("downloadBtn");

const ledgerList = document.getElementById("ledgerList");
const ledgerEmpty = document.getElementById("ledgerEmpty");
const searchInput = document.getElementById("searchInput");

const statTotal = document.getElementById("statTotal");
const statClasses = document.getElementById("statClasses");

const modalOverlay = document.getElementById("modalOverlay");
const modalTitle = document.getElementById("modalTitle");
const modalSource = document.getElementById("modalSource");
const modalTests = document.getElementById("modalTests");
const modalClose = document.getElementById("modalClose");

let currentGeneratedTests = "";
let currentClassName = "test";

// ---------------- Generate ----------------
generateBtn.addEventListener("click", async () => {
  const sourceCode = sourceCodeInput.value.trim();
  if (!sourceCode) {
    showError("Please paste some source code first.");
    return;
  }

  setLoading(true);
  hideError();

  try {
    const response = await fetch(`${API_BASE}/generate`, {
      method: "POST",
      headers: authHeaders({ "Content-Type": "application/json" }),
      body: JSON.stringify({
        sourceCode,
        language: languageSelect.value,
        framework: frameworkSelect.value,
        testStyle: testStyleSelect.value,
        className: classNameHint.value.trim() || null
      })
    });

    if (handleAuthFailure(response)) return;

    const data = await response.json();

    if (!response.ok || data.success === false) {
      showError(data.errorMessage || "Generation failed. Check server logs and your Groq API key.");
      return;
    }

    currentGeneratedTests = data.generatedTests;
    currentClassName = data.className || "test";
    renderOutput(currentGeneratedTests);
    await loadLedger();

  } catch (err) {
    console.error(err);
    showError("Could not reach the backend. Is the Spring Boot app running on port 8080?");
  } finally {
    setLoading(false);
  }
});

function setLoading(isLoading) {
  generateBtn.disabled = isLoading;
  generateSpinner.classList.toggle("hidden", !isLoading);
  generateBtnText.textContent = isLoading ? "Generating..." : "Generate Tests";
}

function renderOutput(code) {
  outputEmpty.classList.add("hidden");
  outputCode.classList.remove("hidden");
  outputCodeContent.textContent = code;
  copyBtn.disabled = false;
  downloadBtn.disabled = false;
}

function showError(message) {
  errorBanner.textContent = message;
  errorBanner.classList.remove("hidden");
}

function hideError() {
  errorBanner.classList.add("hidden");
}

// ---------------- Copy / Download ----------------
copyBtn.addEventListener("click", () => {
  navigator.clipboard.writeText(currentGeneratedTests).then(() => {
    copyBtn.textContent = "Copied!";
    setTimeout(() => (copyBtn.textContent = "Copy"), 1500);
  });
});

downloadBtn.addEventListener("click", () => {
  const ext = extensionForLanguage(languageSelect.value);
  const filename = `${currentClassName}Test${ext}`;
  const blob = new Blob([currentGeneratedTests], { type: "text/plain" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
});

function extensionForLanguage(lang) {
  switch (lang) {
    case "Java": return ".java";
    case "Python": return ".py";
    case "JavaScript": return ".js";
    case "C#": return ".cs";
    default: return ".txt";
  }
}

// ---------------- Ledger (history) ----------------
async function loadLedger(query) {
  try {
    const url = query ? `${API_BASE}/search?query=${encodeURIComponent(query)}` : API_BASE;
    const response = await fetch(url, { headers: authHeaders() });
    if (handleAuthFailure(response)) return;
    const entries = await response.json();
    renderLedger(entries);
    updateStats(entries);
  } catch (err) {
    console.error("Failed to load ledger", err);
  }
}

function renderLedger(entries) {
  ledgerList.querySelectorAll(".ledger-entry").forEach(el => el.remove());

  if (!entries || entries.length === 0) {
    ledgerEmpty.classList.remove("hidden");
    return;
  }
  ledgerEmpty.classList.add("hidden");

  entries.forEach(entry => {
    const row = document.createElement("div");
    row.className = "ledger-entry";
    row.innerHTML = `
      <span class="ledger-dot"></span>
      <span class="ledger-class">${escapeHtml(entry.className)}</span>
      <span class="ledger-meta">${escapeHtml(entry.language)} · ${escapeHtml(entry.framework)} · ${styleLabel(entry.testStyle)} · ${formatDate(entry.createdAt)}</span>
      <span class="ledger-tag">saved</span>
      <button class="ledger-delete" data-id="${entry.id}">Delete</button>
    `;
    row.addEventListener("click", (e) => {
      if (e.target.classList.contains("ledger-delete")) return;
      openModal(entry);
    });
    row.querySelector(".ledger-delete").addEventListener("click", async (e) => {
      e.stopPropagation();
      const delResponse = await fetch(`${API_BASE}/${entry.id}`, { method: "DELETE", headers: authHeaders() });
      if (handleAuthFailure(delResponse)) return;
      loadLedger(searchInput.value.trim());
    });
    ledgerList.appendChild(row);
  });
}

function updateStats(entries) {
  statTotal.textContent = entries.length;
  const uniqueClasses = new Set(entries.map(e => e.className));
  statClasses.textContent = uniqueClasses.size;
}

function formatDate(isoString) {
  if (!isoString) return "";
  const d = new Date(isoString);
  return d.toLocaleString(undefined, { month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" });
}

function styleLabel(testStyle) {
  const labels = {
    COMPREHENSIVE: "Comprehensive",
    HAPPY_PATH: "Happy Path",
    EDGE_CASES: "Edge Cases",
    BOUNDARY_VALUES: "Boundary Values"
  };
  return labels[testStyle] || "Comprehensive";
}

function escapeHtml(str) {
  if (!str) return "";
  return str.replace(/[&<>"']/g, (c) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
  }[c]));
}

let searchTimeout;
searchInput.addEventListener("input", () => {
  clearTimeout(searchTimeout);
  searchTimeout = setTimeout(() => loadLedger(searchInput.value.trim()), 300);
});

// ---------------- Modal ----------------
function openModal(entry) {
  modalTitle.textContent = entry.className;
  modalSource.textContent = entry.sourceCode;
  modalTests.textContent = entry.generatedTests;
  modalOverlay.classList.remove("hidden");
}
modalClose.addEventListener("click", () => modalOverlay.classList.add("hidden"));
modalOverlay.addEventListener("click", (e) => {
  if (e.target === modalOverlay) modalOverlay.classList.add("hidden");
});

// ---------------- Init ----------------
loadLedger();
