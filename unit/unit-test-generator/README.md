# TestLedger — AI-Powered Unit Test Generator

A web-based tool that uses LLM-based reasoning (via the **Groq API**) to automatically
generate unit test cases from source code. A **Spring Boot** backend persists every
generated test case to **MySQL** for tracking and reuse, and a lightweight
HTML/CSS/JavaScript frontend lets you paste code, generate tests, and browse history.

---

## Tech Stack

| Layer      | Technology                          |
|------------|--------------------------------------|
| Backend    | Java 17, Spring Boot 3.3 (Web, Data JPA, Validation) |
| Database   | MySQL                                |
| LLM        | Groq API (OpenAI-compatible, `llama-3.3-70b-versatile`) |
| Frontend   | HTML, CSS, vanilla JavaScript        |
| Build tool | Maven                                |

---

## Project Structure

```
unit-test-generator/
├── pom.xml
├── sql/
│   └── schema.sql                 # optional manual schema (Hibernate can auto-create it)
├── src/main/java/com/testgen/
│   ├── TestGenApplication.java     # main entry point
│   ├── config/AppConfig.java       # RestTemplate + CORS config
│   ├── controller/
│   │   ├── TestCaseController.java # REST endpoints
│   │   └── GlobalExceptionHandler.java
│   ├── service/
│   │   ├── GroqService.java        # calls the Groq LLM API
│   │   └── TestCaseService.java    # business logic + persistence
│   ├── repository/TestCaseRepository.java
│   ├── model/TestCase.java         # JPA entity
│   └── dto/
│       ├── GenerateRequest.java
│       └── GenerateResponse.java
└── src/main/resources/
    ├── application.properties      # DB + Groq config
    └── static/                     # frontend (served directly by Spring Boot)
        ├── index.html
        ├── style.css
        └── script.js
```

---

## 1. Prerequisites

- Java 17+
- Maven 3.8+
- MySQL 8.x running locally (or reachable)
- A free Groq API key from **https://console.groq.com**

---

## 2. Database setup

Hibernate is configured with `spring.jpa.hibernate.ddl-auto=update`, so it will
create the `test_cases` table automatically the first time you run the app —
you only need the database itself to exist:

```sql
CREATE DATABASE testgen_db;
```

If you'd rather create the table yourself, run `sql/schema.sql` instead.

---

## 3. Configure `application.properties`

Open `src/main/resources/application.properties` and update:

```properties
spring.datasource.username=root
spring.datasource.password=your_mysql_password
```

For the Groq key, **don't hardcode it** — set it as an environment variable
before running the app:

```bash
# macOS / Linux
export GROQ_API_KEY=gsk_your_key_here

# Windows (PowerShell)
$env:GROQ_API_KEY="gsk_your_key_here"
```

The property `groq.api.key=${GROQ_API_KEY:your_groq_api_key_here}` will pick
this up automatically. If you skip this step it falls back to the placeholder
string and every generation request will fail with an auth error from Groq.

---

## 4. Build and run

```bash
cd unit-test-generator
mvn clean install
mvn spring-boot:run
```

Then open **http://localhost:8080** in your browser. The frontend is served
directly by Spring Boot from `src/main/resources/static`, so there's no
separate frontend server to run.

---

## 5. How it works

1. You paste source code into the **Source** pane, pick a language and test
   framework, and click **Generate Tests**.
2. The backend (`GroqService`) sends your code to Groq's chat completion
   endpoint with a system prompt instructing the model to return only a
   fenced code block of test code.
3. The response is parsed with a **layered fallback**: extract the fenced
   code block → fall back to raw model text → fall back to a clear
   placeholder if the API response itself is malformed. This keeps the tool
   usable even when the LLM's formatting is inconsistent.
4. The generated tests, along with the original source, class name, language,
   and framework, are saved to MySQL via Spring Data JPA.
5. The **Ledger** panel at the bottom lists every past run (most recent
   first), supports searching by class name, and lets you reopen or delete
   any entry.

---

## 6. Authentication

Auth is **stateless JWT**-based, spread across dedicated pages:

- `login.html` — log in
- `register.html` — create an account (username, email, password)
- `forgot-password.html` — request a password reset link by email
- `reset-password.html` — set a new password (reached via the emailed link, `?token=...`)

Backend endpoints:
- `POST /api/auth/register` — create an account, returns a JWT
- `POST /api/auth/login` — returns a JWT
- `POST /api/auth/logout` — stateless no-op (JWTs can't be server-invalidated
  without a blocklist; the frontend just deletes its stored token)
- `POST /api/auth/forgot-password` — always returns a generic success message
  (whether or not the email exists) so the endpoint can't be used to discover
  registered emails; sends a reset email if the account exists
- `POST /api/auth/reset-password` — takes the emailed token + new password,
  tokens expire after 30 minutes and are single-use

The frontend stores the token in `localStorage` and sends it as
`Authorization: Bearer <token>` on every request to `/api/testcases/**`.
If a request comes back `401`/`403`, the frontend clears the token and
redirects to `login.html` automatically.

Every saved test case is scoped to the user who generated it
(`owner_username` column), so each person's Ledger only shows their own
history.

### Configuring password reset emails

Password reset links are sent via SMTP (Spring Mail). To use Gmail:
1. Enable 2-Step Verification on the Gmail account
2. Create an **App Password** at https://myaccount.google.com/apppasswords
3. Set these environment variables:
   ```
   MAIL_USERNAME=your_email@gmail.com
   MAIL_PASSWORD=your_16_char_app_password
   APP_BASE_URL=http://localhost:8080   # or your deployed URL
   ```
Any other SMTP provider (Mailtrap for testing, SendGrid, etc.) works the same
way — just point `MAIL_HOST`/`MAIL_PORT` at it.

**Before deploying**, also set a real `JWT_SECRET` environment variable (32+
characters) — the fallback in `application.properties` is dev-only and
should never be used in production:
```
JWT_SECRET=some-long-random-string-at-least-32-characters-long
```

---

## 7. Test styles

When generating tests, you can choose what the LLM prioritizes:

| Style              | What it focuses on                                      |
|---------------------|-----------------------------------------------------------|
| Comprehensive (default) | Normal cases + edge cases + invalid/exception inputs, well-rounded |
| Happy Path Only     | Only normal, expected inputs — no error/edge handling      |
| Exhaustive Edge Cases | Nulls, invalid types, exceptions, unusual combinations — skips simple happy-path calls |
| Boundary Values     | Min/max valid values, off-by-one conditions, collection/string length edges |

This is picked per-generation from a dropdown next to the language/framework
selects, and is stored with each ledger entry so you can see which style
produced which tests later.

---

## 8. REST API reference

| Method | Endpoint                          | Description                          |
|--------|-------------------------------------|---------------------------------------|
| POST   | `/api/auth/register`              | Create a new account, returns a JWT   |
| POST   | `/api/auth/login`                 | Log in, returns a JWT                 |
| POST   | `/api/auth/logout`                | Stateless no-op (frontend clears token) |
| POST   | `/api/auth/forgot-password`       | Request a password reset email        |
| POST   | `/api/auth/reset-password`        | Set a new password using an emailed token |
| POST   | `/api/testcases/generate`         | Generate tests for given source code and save to DB *(requires auth)* |
| GET    | `/api/testcases`                  | List current user's saved test cases (newest first) *(requires auth)* |
| GET    | `/api/testcases/search?query=X`   | Search current user's test cases by class name *(requires auth)* |
| GET    | `/api/testcases/{id}`             | Fetch one saved test case *(requires auth, must be owner)* |
| DELETE | `/api/testcases/{id}`             | Delete a saved test case *(requires auth, must be owner)* |

**Example request body for `/api/testcases/generate`:**
```json
{
  "sourceCode": "public class Calculator { public int divide(int a, int b) { return a / b; } }",
  "language": "Java",
  "framework": "JUnit 5",
  "testStyle": "EDGE_CASES",
  "className": "Calculator"
}
```

**Example request body for `/api/auth/login`:**
```json
{ "username": "blessy", "password": "yourpassword" }
```
Response:
```json
{ "token": "eyJhbGciOi...", "username": "blessy", "success": true }
```
Use the returned token as `Authorization: Bearer <token>` on all `/api/testcases/**` calls.

---

## 9. Deploying it live

Since the frontend is served directly by Spring Boot (no separate frontend
host needed), you only need to deploy **one backend service** plus a
**MySQL database**. The easiest free option for a student project is
**Railway**, since it hosts both the app and MySQL in one place.

### Option A: Railway (recommended — easiest, free tier)

1. Push this project to a GitHub repo (make sure `.gitignore` excludes
   `target/` — already set up for you).
2. Go to **railway.app** → sign in with GitHub → **New Project** →
   **Deploy from GitHub repo** → select your repo.
3. Railway auto-detects the `Dockerfile` and builds it. If it doesn't,
   set the build method to "Dockerfile" in the service settings.
4. Add a database: in the same project, click **New** → **Database** →
   **Add MySQL**. Railway provisions it and gives you connection
   variables (`MYSQLHOST`, `MYSQLPORT`, `MYSQLDATABASE`, `MYSQLUSER`,
   `MYSQLPASSWORD`).
5. In your **app service** → **Variables**, add:
   ```
   DB_URL=jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?useSSL=false&serverTimezone=UTC
   DB_USERNAME=${{MySQL.MYSQLUSER}}
   DB_PASSWORD=${{MySQL.MYSQLPASSWORD}}
   GROQ_API_KEY=gsk_your_key_here
   ```
   (Railway lets you reference another service's variables with the
   `${{ServiceName.VAR}}` syntax shown above.)
6. Click **Deploy**. Railway gives you a public URL like
   `https://your-app.up.railway.app` — that's your live demo link.

### Option B: Render (free web service + external MySQL)

Render's free tier doesn't include MySQL, so pair it with a free MySQL
host like **Aiven** or **Railway's MySQL** (used standalone):

1. Create the MySQL database on Aiven/Railway and note its host, port,
   user, password, and database name.
2. On **render.com** → **New** → **Web Service** → connect your GitHub repo.
3. Environment: **Docker** (Render detects the `Dockerfile` automatically).
4. Under **Environment Variables**, add `DB_URL`, `DB_USERNAME`,
   `DB_PASSWORD`, `GROQ_API_KEY` as above, pointing at your external
   MySQL host.
5. Deploy. Render gives you a `https://your-app.onrender.com` URL.
   Note: Render's free tier spins down after inactivity, so the first
   request after idling will be slow (~30–60s cold start).

### Option C: Run it on a VPS / AWS EC2 (more control, more setup)

1. Install Java 17, MySQL, and Git on the instance.
2. `git clone` your repo, create the database, set the same environment
   variables (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `GROQ_API_KEY`) in
   `/etc/environment` or a systemd service file.
3. `mvn clean package` then `java -jar target/*.jar`, or use the provided
   `Dockerfile` with `docker build` + `docker run`.
4. Put Nginx in front for a domain name and HTTPS (via Let's Encrypt/Certbot).

### Building and running the Docker image locally (sanity check)

```bash
docker build -t testledger .
docker run -p 8080:8080 \
  -e DB_URL="jdbc:mysql://host.docker.internal:3306/testgen_db?useSSL=false&serverTimezone=UTC" \
  -e DB_USERNAME=root \
  -e DB_PASSWORD=your_mysql_password \
  -e GROQ_API_KEY=gsk_your_key_here \
  testledger
```

### Security note before deploying

Never commit real credentials to GitHub. `application.properties` now reads
`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `GROQ_API_KEY` from environment
variables with local-only fallback defaults — always set the real values as
environment variables/secrets on whatever platform you deploy to.

---

## 10. Notes on extending this project

- **Batch mode**: add an endpoint that accepts a whole file/folder of classes
  and generates tests for each one in sequence.
- **Coverage estimate**: after generating tests, ask the LLM (or a static
  analysis pass) to estimate branch/line coverage and store it alongside
  the test case.
- **Multiple LLM providers**: `GroqService` is isolated behind a single
  class, so swapping in another OpenAI-compatible provider only requires
  changing the base URL/model and (if needed) the request/response shape.
- **Auth**: there's no login/user model yet — every test case is global.
  Adding a `User` entity and scoping `TestCase` rows to a user would be a
  natural next step for a multi-user deployment.
