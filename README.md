# TrackMyFinance

A personal finance tracker. Log daily expenses, see where your money goes on a dashboard,
and get AI-generated spending suggestions.

- **Backend:** Java 17, Spring Boot, Spring Data JPA, Flyway, MySQL 8
- **Frontend:** React (Vite), TanStack Query, Recharts
- **AI:** Google Gemini API (free tier), behind a swappable provider interface, with a
  rule-based fallback when the API is unavailable
- **Auth:** JWT (stateless), multi-user ready (every row has a `user_id`)

## Repository layout

```
backend/track/   Spring Boot API (run Maven commands from here)
frontend/        React app
compose.yaml     MySQL for local development
.env.example     Template for your local secrets (copy to .env, never commit .env)
```

## Prerequisites

- JDK 17 or newer
- Node 20.19+ (or 22.12+)
- Docker (for MySQL, and for the tests)
- A Gemini API key from Google AI Studio (optional: without it you get rule-based tips)

## Setup

1. Copy the template and fill it in with your own values:
```bash
   cp .env.example .env
```
   Generate random values for the passwords and the JWT secret:
```bash
   openssl rand -hex 16   # DB_PASSWORD and DB_ROOT_PASSWORD
   openssl rand -hex 32   # JWT_SECRET
```
   Put your Gemini key in `GEMINI_API_KEY` by editing the file in an editor.
   `.env` is gitignored. Never commit it or paste it anywhere.

2. Start MySQL:
```bash
   docker compose up -d
```

3. Start the backend (Flyway creates the schema on first start):
```bash
   cd backend/track
   ./mvnw spring-boot:run
```

4. Start the frontend in a second terminal:
```bash
   cd frontend
   npm install
   npm run dev
```

5. Open http://localhost:5173 and register. Registration is controlled by
   `APP_REGISTRATION_ENABLED` in `.env`.

## Configuration (`.env`)

| Variable | Purpose |
|---|---|
| `DB_NAME`, `DB_PORT`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DB_ROOT_PASSWORD` | MySQL connection and the container's credentials |
| `JWT_SECRET` | Signs login tokens. At least 32 characters. |
| `APP_REGISTRATION_ENABLED` | `true` to allow new sign-ups. Defaults to `false` if unset. |
| `APP_CORS_ALLOWED_ORIGINS` | Browser origins allowed to call the API |
| `GEMINI_API_KEY` | Google AI Studio key. Leave empty to use rule-based tips only. |
| `GEMINI_MODEL` | Model name, e.g. `gemini-3.1-flash-lite`. Model names and free quotas change, so check AI Studio. |

AI behaviour (cooldown, retries, limits) is configured under `app.ai.*` and `app.gemini.*`
in `backend/track/src/main/resources/application.properties`.

## Tests

```bash
cd backend/track && ./mvnw test      # needs Docker (Testcontainers starts a throwaway MySQL)
cd frontend && npm run lint && npm run build
```

## API overview (`/api/v1`, JWT required except register and login)

| Area | Endpoints |
|---|---|
| Auth | `POST /auth/register`, `POST /auth/login`, `GET /auth/me` |
| Categories | `GET /categories` |
| Expenses | `POST/GET /expenses`, `GET/PUT/DELETE /expenses/{id}` |
| Dashboard | `GET /dashboard/summary?from&to` |
| AI | `POST /ai/suggestions`, `GET /ai/suggestions/latest` |

## How the AI part works

Only aggregated numbers (totals, category shares, daily totals) are sent to Gemini. Never
your email, name, IDs or expense descriptions. Results are cached by a hash of the data, so
repeated clicks cost nothing. If Gemini is unavailable or the quota is used up, you see the
last saved result or rule-based tips, never a bare error.

## Security notes

- Secrets live only in `.env` (gitignored) or real environment variables.
- Passwords are stored as BCrypt hashes. API responses use DTOs, never database entities.
- Every query is scoped to the user in the token. Another user's data returns 404.
- Login has no brute-force throttling yet. Add one before exposing this publicly.
- The JWT is kept in `sessionStorage`, and logout deletes it from the browser. Tokens are
  stateless, so a copied token stays valid until it expires (60 minutes by default).

## Troubleshooting

- **App fails with "Could not resolve placeholder":** a variable is missing from `.env`.
- **Cannot find `.env`:** the file name must be exactly `.env`, with no spaces.
- **403 "Invalid CORS request":** add your frontend's origin to `APP_CORS_ALLOWED_ORIGINS`.
- **AI shows "Basic tips":** fewer than 10 expenses in the range, or Gemini is failing.
  The notice on the page says which. Check the backend log.
