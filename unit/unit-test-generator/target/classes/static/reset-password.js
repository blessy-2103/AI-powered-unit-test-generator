const API_BASE = "/api/auth";

const resetForm = document.getElementById("resetForm");
const submitBtn = document.getElementById("submitBtn");
const authError = document.getElementById("authError");
const authSuccess = document.getElementById("authSuccess");
const newPasswordInput = document.getElementById("newPassword");
const confirmPasswordInput = document.getElementById("confirmPassword");

const urlParams = new URLSearchParams(window.location.search);
const token = urlParams.get("token");

if (!token) {
  showError("This reset link is missing its token. Please request a new one from the Forgot Password page.");
  submitBtn.disabled = true;
}

resetForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  hideMessages();

  const newPassword = newPasswordInput.value;
  const confirmPassword = confirmPasswordInput.value;

  if (newPassword !== confirmPassword) {
    showError("Passwords do not match.");
    return;
  }

  submitBtn.disabled = true;
  submitBtn.textContent = "Resetting...";

  try {
    const response = await fetch(`${API_BASE}/reset-password`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ token, newPassword })
    });

    const data = await response.json();

    if (!response.ok || data.success === false) {
      showError(data.errorMessage || "Could not reset password. The link may have expired.");
      return;
    }

    showSuccess("Password reset! Redirecting to log in...");
    setTimeout(() => (window.location.href = "login.html"), 1800);

  } catch (err) {
    console.error(err);
    showError("Could not reach the server. Please try again.");
  } finally {
    submitBtn.disabled = false;
    submitBtn.textContent = "Reset Password";
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
