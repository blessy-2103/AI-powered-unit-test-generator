const API_BASE = "/api/auth";

const registerForm = document.getElementById("registerForm");
const submitBtn = document.getElementById("submitBtn");
const authError = document.getElementById("authError");
const usernameInput = document.getElementById("username");
const emailInput = document.getElementById("email");
const passwordInput = document.getElementById("password");
const confirmPasswordInput = document.getElementById("confirmPassword");

if (localStorage.getItem("authToken")) {
  window.location.href = "index.html";
}

registerForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  hideError();

  const username = usernameInput.value.trim();
  const email = emailInput.value.trim();
  const password = passwordInput.value;
  const confirmPassword = confirmPasswordInput.value;

  if (password !== confirmPassword) {
    showError("Passwords do not match.");
    return;
  }
  if (password.length < 6) {
    showError("Password must be at least 6 characters.");
    return;
  }

  submitBtn.disabled = true;
  submitBtn.textContent = "Creating account...";

  try {
    const response = await fetch(`${API_BASE}/register`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, email, password })
    });

    const data = await response.json();

    if (!response.ok || data.success === false) {
      showError(data.errorMessage || "Could not create account. Please try again.");
      return;
    }

    localStorage.setItem("authToken", data.token);
    localStorage.setItem("authUsername", data.username);
    window.location.href = "index.html";

  } catch (err) {
    console.error(err);
    showError("Could not reach the server. Please try again.");
  } finally {
    submitBtn.disabled = false;
    submitBtn.textContent = "Create Account";
  }
});

function showError(message) {
  authError.textContent = message;
  authError.classList.remove("hidden");
}
function hideError() {
  authError.classList.add("hidden");
}
