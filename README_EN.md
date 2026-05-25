# InternPilot

[中文 README](README.md) | [Demo Script](docs/47-demo-video-and-scoring-evidence.md)

InternPilot is an AI-powered internship preparation platform for university students. It combines resume management, job description management, AI resume-job matching, job recommendation, interview question generation, RAG knowledge retrieval, application tracking, user feedback, an admin console, and Docker-based deployment.

Current stable demo version: `v1.3.1`  
Online demo: `http://43.136.182.179`  
Release: `https://github.com/wan719/intern-pilot/releases/tag/v1.3.1`

## Highlights

- AI resume-job analysis with match score, strengths, gaps, improvement suggestions, interview preparation advice, and risk tips.
- DeepSeek API integration with Mock AI reserved for test and CI environments.
- AI model routing and prompt template versioning for resume analysis, job recommendation, interview question generation, RAG Q&A, and resume optimization.
- Robust AI output parsing, JSON sanitizing, retry, fallback model support, and AI cache keys that include scenario, model, prompt version, and prompt hash.
- WebSocket-based AI progress updates and a global AI task center.
- RAG job knowledge base with document management, chunking, mock embedding, retrieval, and context-enhanced AI generation.
- RBAC admin console for users, roles, permissions, operation logs, RAG knowledge, feedback, and dashboards.
- AI report print page and browser PDF export.
- Spring Boot engineering enhancements: Actuator, Validation, global exception handling, AOP execution time logs, operation log masking, Redis key standardization, cleanup tasks, and Docker healthchecks.
- Frontend performance improvements: route lazy loading, Vite manual chunks, optimized brand image, Nginx gzip, and long-term asset caching.

## Tech Stack

| Layer | Technologies |
| --- | --- |
| Backend | Spring Boot 3, Spring Security, JWT, MyBatis-Plus, MySQL, Redis, WebSocket, DeepSeek API |
| Frontend | Vue 3, TypeScript, Vite, Pinia, Vue Router, Element Plus, ECharts, Axios |
| AI | DeepSeek API, Mock AI for tests, prompt versioning, model routing, RAG retrieval |
| Testing | JUnit 5, Mockito, MockMvc, Spring Security Test, H2, Vue type checking, Vite build |
| Deployment | Docker, Docker Compose, Nginx, GitHub Actions, Actuator healthcheck |

## Main Modules

| Module | Description |
| --- | --- |
| Authentication | Email verification code registration/login, JWT authentication, current user profile |
| Resume | Upload, parse, preview, version management, comparison, AI optimization |
| Job | Job description CRUD, company, city, job type, skill requirements, JD content |
| AI Analysis | Resume-job matching report with scores, strengths, weaknesses, suggestions, and PDF export |
| AI Task Center | Global long-running task state, progress, completion reminders, result entry |
| Recommendation | Job recommendation batches and recommendation reasons |
| Interview | AI-generated interview questions with category, difficulty, answer, follow-up, and keywords |
| RAG | Admin-managed job knowledge base and retrieval-enhanced generation |
| Admin | Users, roles, permissions, operation logs, feedback, dashboard, and RAG management |
| Deployment | Docker Compose for frontend, backend, MySQL, and Redis |

## Screenshots

The Chinese README contains the full screenshot gallery:

- Login
- User dashboard
- Resume list
- Job list
- AI analysis progress
- AI report
- Interview question list and detail
- Admin user management
- Admin RAG knowledge base

## Quick Start

### Backend

```powershell
cd backend/intern-pilot-backend
.\gradlew.bat bootRun --no-daemon
```

The backend uses the `dev` profile by default when launched through Gradle `bootRun`. Local environment variables can be loaded from `.env` or `deploy/.env`.

### Frontend

```powershell
cd frontend/intern-pilot-frontend
npm install
npm run dev
```

### Docker Compose

```bash
cd deploy
cp .env.example .env
# Edit .env before production deployment.
docker compose --env-file .env -f docker-compose.yml up -d --build
docker compose --env-file .env -f docker-compose.yml ps
```

Production secrets such as `DEEPSEEK_API_KEY`, `JWT_SECRET`, `MAIL_PASSWORD`, MySQL password, and Redis password must be provided through environment variables and must not be committed.

## API Documentation

Knife4j / Swagger is available after the backend starts:

```text
http://localhost:8080/doc.html
```

## Health Checks

```bash
curl http://localhost:8080/api/health
curl http://localhost:8080/actuator/health
```

In Docker deployment, Nginx proxies `/api`, `/uploads`, `/ws`, and `/actuator` to the backend. Production Actuator exposure is limited to `health` and `info`.

## Tests and Coverage

Backend tests:

```powershell
cd backend/intern-pilot-backend
.\gradlew.bat test jacocoTestReport --no-daemon --max-workers=1
```

JaCoCo reports:

```text
backend/intern-pilot-backend/build/reports/jacoco/test/html/index.html
backend/intern-pilot-backend/build/reports/jacoco/test/jacocoTestReport.xml
```

Frontend build:

```powershell
cd frontend/intern-pilot-frontend
npm run build
```

## Development and Git History

The project follows a `main / dev / feature/*` branch workflow. Feature branches were used for authentication, RBAC, RAG, WebSocket AI progress, AI task center, AI model routing, Spring Boot engineering enhancement, PDF export, and frontend performance optimization.

The repository contains more than 100 commits across the development period, version tags from `v1.0.0` to `v1.3.1`, and GitHub Actions for backend tests, frontend builds, and Docker build checks.

## License

This project is released under the MIT License.
