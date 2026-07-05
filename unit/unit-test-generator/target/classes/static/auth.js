const API_BASE = "/api/auth";

const loginForm = document.getElementById("loginForm");
const submitBtn = document.getElementById("submitBtn");
const authError = document.getElementById("authError");
const usernameInput = document.getElementById("username");
const passwordInput = document.getElementById("password");

// If already logged in, skip straight to the app
if (localStorage.getItem("authToken")) {
  window.location.href = "index.html";
}

loginForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  hideError();

  const username = usernameInput.value.trim();
  const password = passwordInput.value;

  submitBtn.disabled = true;
  submitBtn.textContent = "Logging in...";

  try {
    const response = await fetch(`${API_BASE}/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, password })
    });

    const data = await response.json();

    if (!response.ok || data.success === false) {
      showError(data.errorMessage || "Invalid username or password.");
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
    submitBtn.textContent = "Log In";
  }
});

function showError(message) {
  authError.textContent = message;
  authError.classList.remove("hidden");
}
function hideError() {
  authError.classList.add("hidden");
}
