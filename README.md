# Marketplace Backend

A RESTful marketplace backend built with Java and Spring Boot. The project demonstrates account registration, JWT-based authentication, role-based authorization, product management, database migrations, and automated testing.

## Tech Stack

- Java 21
- Spring Boot 4.1.1
- Spring Web MVC
- Spring Data JPA
- Spring Security and OAuth2 Resource Server
- JWT bearer-token authentication
- MySQL 8
- Flyway database migrations
- Maven Wrapper
- Jakarta Bean Validation

## Features

- Account registration and login
- JWT access tokens
- Role-based authorization for product management
- Customer carts, checkout, and order history
- Administrative user and product management
- Product creation, listing, retrieval, updating, and deletion
- Request validation
- Consistent API response format
- Version-controlled database schema migrations
- Automated unit and integration tests

## Architecture

The application follows a layered structure:

```text
HTTP Client
    |
    v
REST Controllers
    |
    v
Service Layer
    |
    v
Spring Data JPA Repositories
    |
    v
MySQL Database

Spring Security validates bearer tokens
and enforces endpoint authorization.
Flyway manages database migrations.
```

## Prerequisites

Install or configure:

- JDK 21
- MySQL 8
- Git (optional for local execution; required for version control)

The included Maven Wrapper can be used without a separate Maven installation.

## Local Setup

### 1. Clone the repository

After this project is published, clone it with:

```bash
git clone <repository-url>
cd <repository-folder>
```

Alternatively, open your existing local project directory.

### 2. Create the database

Create a dedicated MySQL database for this application:

```sql
CREATE DATABASE marketplace_backend;
```

Do not use a database containing unrelated or existing application data.

### 3. Configure local environment variables

The local configuration file `src/main/resources/application-local.properties` is intentionally excluded from version control.

Configure the following environment variables in your terminal:

- `MARKETPLACE_DB_PASSWORD` — password for the configured local MySQL account
- `MARKETPLACE_JWT_SECRET_BASE64` — Base64-encoded secret containing at least 32 decoded bytes
- `MARKETPLACE_JWT_ISSUER` — expected JWT issuer; use `http://localhost:8080` for local development

For PowerShell, enter your database password securely:

```powershell
$secure = Read-Host "MySQL password" -AsSecureString
$env:MARKETPLACE_DB_PASSWORD = [System.Net.NetworkCredential]::new("", $secure).Password
```

Generate a random JWT signing secret:

```powershell
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$env:MARKETPLACE_JWT_SECRET_BASE64 = [Convert]::ToBase64String($bytes)
$rng.Dispose()

$env:MARKETPLACE_JWT_ISSUER = "http://localhost:8080"
```

These environment variables apply to the current PowerShell session. Set them again in a new terminal when needed. Never commit passwords, tokens, or signing secrets.

### 4. Run the application

From the project root:

```powershell
.\mvnw.cmd spring-boot:run
```

The default local configuration uses port `8080`. Flyway applies pending migrations when the application starts.

Base URL:

```text
http://localhost:8080
```

## API Reference

All successful and error responses use the general structure:

```json
{
  "success": true,
  "message": "Example message",
  "data": {}
}
```

### Authentication

| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/auth/register` | Public |
| POST | `/api/auth/login` | Public |

Register an account:

```http
POST /api/auth/register
Content-Type: application/json
```

```json
{
  "email": "customer@example.com",
  "password": "replace-with-a-strong-password"
}
```

Log in:

```http
POST /api/auth/login
Content-Type: application/json
```

```json
{
  "email": "customer@example.com",
  "password": "replace-with-a-strong-password"
}
```

The login response contains an access token, token type, expiration duration, account ID, email, role, and account status.

Use the returned token for protected endpoints:

```http
Authorization: Bearer <access-token>
```

### Health

| Method | Endpoint | Access |
|---|---|---|
| GET | `/api/health` | Public |

### Products

| Method | Endpoint | Access |
|---|---|---|
| GET | `/api/products` | Public |
| GET | `/api/products/{id}` | Public |
| POST | `/api/products` | VENDOR or ADMIN |
| PUT | `/api/products/{id}` | VENDOR or ADMIN |
| DELETE | `/api/products/{id}` | VENDOR or ADMIN |

Example product request:

```http
POST /api/products
Content-Type: application/json
Authorization: Bearer <vendor-or-admin-token>
```

```json
{
  "name": "Wireless Mouse",
  "description": "Wireless computer mouse",
  "price": 799.00,
  "stockQuantity": 25,
  "category": "Electronics",
  "active": true
}
```

Product request validation includes:

- Name: required, maximum 150 characters
- Description: maximum 5,000 characters
- Price: required and at least `0.01`
- Stock quantity: required and non-negative
- Category: required, maximum 100 characters

The product response includes its ID, product details, active status, and creation/update timestamps.

## Database Migrations

Flyway migration scripts are located in:

```text
src/main/resources/db/migration/
```

The current migration files are:

- `V1__initial_schema.sql`
- `V2__add_product_vendor.sql`
- `V3__add_cart_and_order_tables.sql`

Flyway records applied migrations in `flyway_schema_history`. Hibernate schema validation is enabled; schema changes should be introduced through migrations.

## Running Tests

Run the full test suite:

```powershell
.\mvnw.cmd clean test
```

## Frontend

The React customer frontend is isolated in [`frontend/`](frontend/). It uses Vite, TypeScript, Tailwind CSS, Axios, and React Router.

```powershell
cd frontend
Copy-Item .env.example .env
npm.cmd install
npm.cmd run dev
```

The frontend reads `VITE_API_BASE_URL` from `frontend/.env`; its local default is `http://localhost:8080`.

## Production deployment

The backend production profile is `prod`, configured in `src/main/resources/application-prod.properties`. For Render, set `SPRING_PROFILES_ACTIVE=prod`; the profile uses Render's `PORT` environment variable and falls back to port `8080` when it is absent.

Configure these backend environment variables in Render (do not put their values in source control):

- `SPRING_PROFILES_ACTIVE=prod`
- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD`
- `MARKETPLACE_JWT_SECRET_BASE64` and `MARKETPLACE_JWT_ISSUER`
- `MARKETPLACE_CORS_ALLOWED_ORIGIN` — the exact deployed Netlify site origin, for example `https://your-site.netlify.app` (no trailing slash)

The frontend can be deployed from the `frontend/` directory on Netlify. Set `VITE_API_BASE_URL` to the deployed backend URL. The `frontend/public/_redirects` rule serves the SPA entry point for client-side routes.

## Security Notes

- Never commit local environment files or credentials.
- Use a strong, randomly generated JWT signing secret.
- Use HTTPS and appropriately managed secrets in production.
- Do not use development secrets in a deployed environment.
- Keep database credentials out of source code and documentation.

## Current Scope

The backend supports authentication, health checks, product and administrative management, customer carts, checkout, and order history. Payment processing is not implemented.

## License

No license has been selected yet. Add a license before granting others explicit permission to reuse this project.
