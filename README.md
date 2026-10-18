# doc-extractor

A Spring Boot service that extracts structured data from invoices using the Google Gemini API.
Upload an invoice image or PDF, get back the vendor, date, total and currency, and keep everything in PostgreSQL.

![Screenshot](docs/screenshot.png)

## Features

- Upload an invoice (PNG, JPEG, WEBP or PDF) through a simple web page, by click or drag and drop
- Structured extraction with Gemini using a JSON response schema and `temperature: 0`
- Handles Hebrew invoices (RTL text, DD/MM/YYYY dates, ILS currency)
- Stores the extracted fields and the original file in PostgreSQL
- List, search by vendor, view the original file, and delete invoices
- Tolerant parsing: accepts a single JSON object or an array from the model

## Tech stack

- Java 21, Spring Boot 4, Spring Data JPA (Hibernate)
- PostgreSQL 16 (Docker Compose)
- Google Gemini API (`generateContent`)
- Plain HTML and JavaScript front end (no build step)

## Getting started

### Prerequisites

- JDK 21
- Docker Desktop
- A Gemini API key from [Google AI Studio](https://aistudio.google.com/)

### 1. Start the database

```bash
docker compose up -d
```

PostgreSQL runs on `localhost:5433` (database `docdb`, user `postgres`).

### 2. Set environment variables

PowerShell:

```powershell
setx GEMINI_API_KEY "your-key"
setx DB_PASSWORD "postgres"
```

Restart your terminal or IDE afterwards. `DB_PASSWORD` is optional and defaults to `postgres` for local development.

### 3. Run the application

```bash
./mvnw spring-boot:run
```

On Windows machines behind a corporate proxy or antivirus that re-signs TLS, add this JVM argument if you get `PKIX path building failed`:

```
-Djavax.net.ssl.trustStoreType=WINDOWS-ROOT
```

Open <http://localhost:8081>.

## API

| Method | Path | Description |
|---|---|---|
| POST | `/api/documents/extract` | Upload a file (`multipart/form-data`, field `file`), extract and save |
| GET | `/api/documents` | List all invoices |
| GET | `/api/documents/search?vendor=...` | Search by vendor name (case-insensitive) |
| GET | `/api/documents/{id}` | Get one invoice |
| GET | `/api/documents/{id}/image` | View the original file |
| DELETE | `/api/documents/{id}` | Delete an invoice |

Example:

```bash
curl -F "file=@invoice.pdf" http://localhost:8081/api/documents/extract
```

## Configuration

| Property | Default | Description |
|---|---|---|
| `server.port` | `8081` | HTTP port |
| `gemini.model` | `gemini-3.5-flash-lite` | Gemini model name |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5433/docdb` | Database URL |
| `spring.servlet.multipart.max-file-size` | `10MB` | Maximum upload size |

## Known limitations

- Extraction accuracy depends on image quality and the model; unclear fields are returned as `null` instead of guessed
- Schema is managed with `ddl-auto=update`, which is suitable for development only (Flyway would be the next step)
- No authentication

## Roadmap

- Automated tests for response parsing
- Validation and a "needs review" status for low-confidence results
- Flyway migrations
- Dockerfile for the application itself

## Author

Rivka Glass - [github.com/RivkaGlass](https://github.com/RivkaGlass)
