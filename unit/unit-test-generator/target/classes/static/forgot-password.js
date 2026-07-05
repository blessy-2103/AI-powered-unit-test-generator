const API_BASE = "/api/auth";

const forgotForm = document.getElementById("forgotForm");
const submitBtn = document.getElementById("submitBtn");
const authError = document.getElementById("authError");
const authSuccess = document.getElementById("authSuccess");
const emailInput = document.getElementById("email");

forgotForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  hideMessages();

  const email = emailInput.value.trim();

  submitBtn.disabled = true;
  submitBtn.textContent = "Sending...";

  try {
    const response = await fetch(`${API_BASE}/forgot-password`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email })
    });

    const data = await response.json();

    if (!response.ok || data.success === false) {
      showError(data.errorMessage || "Something went wrong. Please try again.");
      return;
    }

    showSuccess(data.errorMessage || "If that email is registered, a reset link has been sent. Check your inbox.");
    forgotForm.reset();

  } catch (err) {
    console.error(err);
    showError("Could not reach the server. Please try again.");
  } finally {
    submitBtn.disabled = false;
    submitBtn.textContent = "Send Reset Link";
  }
});

function showError(message) {
  authError.textContent = message;
  authError.classList.remove("hidden");
}
function showSuccess(message) {
  authSuccess.textContent = message;
  authSuccess.classList.remove("hidden");
}
function hideMessages() {
  authError.classList.add("hidden");
  authSuccess.classList.add("hidden");
}
