<h1 align="center">🧾 Doc Extractor</h1>

<p align="center">
  A Spring Boot REST API that turns invoice images into structured data using the Gemini AI API.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white" alt="Java 21">
  <img src="https://img.shields.io/badge/Spring%20Boot-4-6DB33F?logo=springboot&logoColor=white" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Maven-build-C71A36?logo=apachemaven&logoColor=white" alt="Maven">
  <img src="https://img.shields.io/badge/Gemini-API-4285F4?logo=googlegemini&logoColor=white" alt="Gemini API">
  <img src="https://img.shields.io/badge/Database-H2-blue" alt="H2">
</p>

---

## ✨ Overview

Upload a photo of an invoice and the service returns the **vendor**, **invoice date**, **total amount** and **currency** as JSON. The result is also saved to a database so it can be retrieved later.

## 🚀 Features

- 📤 Upload an invoice image and get structured data back
- 🤖 AI-powered extraction with Google Gemini
- 💾 Persistence with Spring Data JPA (H2, in-memory)
- 🔎 REST endpoints to list and fetch saved documents

## 🛠️ Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Java 21 |
| Framework | Spring Boot 4 |
| Persistence | Spring Data JPA, H2 |
| AI | Google Gemini API |
| Build | Maven |

## 🔄 How It Works

```
Client ──(image)──▶ DocumentExtractController
                          │
                          ▼
                   ExtractionService ──(Base64 image + prompt)──▶ Gemini API
                          │                                           │
                          │◀────────────── JSON response ─────────────┘
                          ▼
                  DocumentRepository ──▶ H2 database
                          │
                          ▼
                 Saved document (JSON) ──▶ Client
```

## 📡 API

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/documents/extract` | Upload an image (form field `file`) and extract its data |
| `GET` | `/api/documents` | List all saved documents |
| `GET` | `/api/documents/{id}` | Get a single document |

**Example response**

```json
{
  "id": 1,
  "fileName": "invoice.png",
  "vendor": "Cohen Supplies Ltd",
  "invoiceDate": "2026-09-15",
  "total": 1250.50,
  "currency": "ILS"
}
```

## ▶️ Running Locally

**Prerequisites:** Java 21 and a Gemini API key from [Google AI Studio](https://aistudio.google.com/).

1. Clone the repository
```bash
   git clone https://github.com/RivkaGlass/doc-extractor.git
   cd doc-extractor
```
2. Set your API key as an environment variable (never commit it)
```powershell
   $env:GEMINI_API_KEY = "your-key"
```
3. Start the server
```powershell
   .\mvnw.cmd spring-boot:run
```
4. Try it
```powershell
   curl.exe -F "file=@invoice.png;type=image/png" http://localhost:8081/api/documents/extract
```

## ⚙️ Configuration

Set in `src/main/resources/application.properties`:

| Property | Description |
|----------|-------------|
| `server.port` | Server port (default `8081`) |
| `gemini.model` | Gemini model name |
| `GEMINI_API_KEY` | API key, provided as an environment variable |

## 🗺️ Roadmap

- [ ] PDF support
- [ ] Persistent database (PostgreSQL)
- [ ] Unit and integration tests
- [ ] Docker support
