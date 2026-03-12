# Personal Finance Tracker

A full-stack personal finance management application. The backend is a secure, production-ready **Spring Boot** REST API and the frontend is a responsive **Angular 19** SPA. Together they provide transaction tracking, budget management, CSV/PDF bank statement import, email alerts, and rich data visualizations.

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [Project Structure](#project-structure)
- [Configuration](#configuration)
- [Application Routes](#application-routes)
- [API Reference](#api-reference)
- [Authentication & Security](#authentication--security)
- [Features](#features)
- [Frontend Components & Services](#frontend-components--services)
- [Scheduling & Background Jobs](#scheduling--background-jobs)
- [Caching](#caching)
- [Testing](#testing)
- [Environment Variables Reference](#environment-variables-reference)

---

## Architecture Overview

```
┌─────────────────────────────┐        ┌──────────────────────────────────┐
│   Angular 19 Frontend        │        │   Spring Boot 4.0.3 Backend     │
│   localhost:4200             │◄──────►│   localhost:8080                │
│                              │  REST  │                                 │
│  • Bootstrap 5 UI            │  +     │  • Spring Security + JWT        │
│  • Chart.js visualizations   │  HTTP  │  • MySQL / Spring Data JPA      │
│  • HttpOnly cookie auth      │  Only  │  • CSV / PDF import             │
│  • Reactive RxJS state       │ Cookies│  • Email alerts (Thymeleaf)     │
└─────────────────────────────┘        └──────────────────────────────────┘
```

JWT access tokens (15 min) and refresh tokens (7 days) are stored exclusively in **HttpOnly cookies** — they are never accessible to JavaScript, preventing XSS-based token theft.

---

## Tech Stack

### Backend

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0.3 |
| Security | Spring Security + JWT (JJWT 0.13) |
| Persistence | Spring Data JPA + Hibernate |
| Database | MySQL |
| Email | Spring Mail + Thymeleaf HTML templates |
| File Parsing | Apache Commons CSV, Apache PDFBox 2.0 |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Rate Limiting | Bucket4j 8.0 |
| Input Validation | Spring Validation + OWASP Java Encoder |
| Build Tool | Maven (via `mvnw` wrapper) |

### Frontend

| Technology | Version | Purpose |
|---|---|---|
| Angular | 19.2 | SPA framework |
| TypeScript | ~5.7 | Language |
| Bootstrap | 5.3.8 | UI / responsive layout |
| Bootstrap Icons | 1.13.1 | Icon library |
| Chart.js | 4.5 | Data visualization |
| RxJS | ~7.8 | Reactive programming |
| Angular CLI | 19.2.9 | Build tooling |
| Karma + Jasmine | 6.4 / 5.6 | Unit testing |

---

## Getting Started

### Prerequisites

- Java 17+
- MySQL 8+
- Node.js 18+ and npm
- Angular CLI 19: `npm install -g @angular/cli`
- Maven (or use the included `mvnw` wrapper)

### 1. Clone the repository

```bash
git clone <repository-url>
cd personal-finance-tracker
```

### 2. Set up the database

```sql
CREATE DATABASE personal_finance_tracker;
```

### 3. Configure the backend

Create `env.properties` at `backend/personal-finance-tracker-spring-boot/` (same level as `pom.xml`). This file is gitignored:

```properties
DB_URL=jdbc:mysql://localhost:3306/personal_finance_tracker
DB_USERNAME=your_db_user
DB_PASSWORD=your_db_password
JWT_SECRET=your_base64_encoded_512bit_secret
EMAIL_USERNAME=your_gmail@gmail.com
EMAIL_PASSWORD=your_gmail_app_password
APP_BASE_URL=http://localhost:4200
```

> **JWT Secret:** Generate a secure key by running the included `JwtSecretMakerTest` test, or with `openssl rand -base64 64`.

### 4. Start the backend

```bash
cd backend/personal-finance-tracker-spring-boot
./mvnw spring-boot:run
```

The API starts at `http://localhost:8080`. Swagger UI is available at `http://localhost:8080/swagger-ui/index.html`.

### 5. Install and start the frontend

```bash
cd frontend/angular-personal-finance-tracker
npm install
ng serve
```

Navigate to `http://localhost:4200/`. The app hot-reloads on file changes.

---

## Project Structure

### Backend

```
src/main/java/com/petruth/personal_finance_tracker/
├── controller/           # REST controllers (Auth, User, Transaction, Budget, Category, Profile, Import, Email)
├── dto/                  # Data Transfer Objects (request/response shapes)
├── entity/               # JPA entities (User, Transaction, Budget, Category, RefreshToken, ImportedFile)
├── jwt/                  # JWT generation and validation (JwtUtil)
├── repository/           # Spring Data JPA repositories
├── scheduler/            # Scheduled tasks (budget alerts, token cleanup)
├── security/             # Security config, filters, rate limiting, SecurityUtil
├── service/              # Business logic interfaces + implementations
├── specifications/       # JPA Specifications for dynamic transaction filtering
├── utils/                # Mappers (Transaction, Budget, Category) and file utilities
└── validation/           # Custom validators (SQL injection prevention)

src/main/resources/
├── application.properties
└── templates/
    ├── budget-alert.html           # Thymeleaf email template
    └── email-verification.html     # Thymeleaf email template
```

### Frontend

```
src/
├── app/
│   ├── common/                    # Data models / interfaces
│   │   ├── transaction.ts
│   │   ├── budget-dto.ts
│   │   ├── budget-with-spending.ts
│   │   ├── category.ts
│   │   ├── user.ts
│   │   └── user-response.ts
│   │
│   ├── components/                # Feature components
│   │   ├── dashboard/             # Overview with charts and stats
│   │   ├── transaction/           # CRUD with filters and pagination
│   │   ├── budget/                # Budget management with progress tracking
│   │   ├── profile/               # Account settings and data export
│   │   ├── bank-statement-import/ # CSV and PDF import wizard
│   │   ├── login/                 # Authentication
│   │   ├── register/              # User registration
│   │   ├── email-verification/    # Email token verification
│   │   ├── line-chart/            # Reusable financial trends chart
│   │   ├── pie-chart/             # Reusable distribution chart
│   │   ├── loading-spinner/       # Shared loading indicator
│   │   ├── error-message/         # Dismissible error alert
│   │   └── toast-container/       # Toast notification system
│   │
│   ├── services/                  # Injectable services
│   │   ├── auth.service.ts        # Login, register, auto-login, token refresh
│   │   ├── user.service.ts        # User data and chart data
│   │   ├── transaction.service.ts # Transaction CRUD with pagination
│   │   ├── budget.service.ts      # Budget CRUD with spending data
│   │   ├── category.service.ts    # Category list
│   │   ├── profile.service.ts     # Account stats, email, password, export
│   │   ├── csv-import.service.ts  # CSV upload
│   │   ├── pdf-import.service.ts  # PDF upload
│   │   ├── data-refresh.service.ts# Cross-component refresh signaling
│   │   └── toast.service.ts       # Toast notification management
│   │
│   ├── validators/
│   │   └── my-custom-validator.ts # notOnlyWhitespace validator
│   │
│   ├── auth.guard.ts              # Route protection with auto-login fallback
│   ├── auth.interceptor.ts        # Attaches withCredentials + handles 401 refresh
│   ├── error.interceptor.ts       # Global HTTP error handling with toasts
│   ├── app.component.ts           # Root layout with sidebar navigation
│   ├── app.routes.ts              # Route definitions
│   └── app.config.ts              # Application providers
│
├── environments/
│   └── environment.ts             # API base URL config
│
├── styles.css                     # Global styles and CSS variables
└── index.html
```

---

## Configuration

### Backend

All sensitive configuration is injected via `env.properties` (imported by `application.properties`). Key settings:

| Property | Description |
|---|---|
| `spring.jpa.hibernate.ddl-auto=validate` | Schema is validated, not auto-created. Run migrations manually. |
| `server.error.include-message=never` | Prevents leaking error details to clients |
| `spring.servlet.multipart.max-file-size=5MB` | Upload limit for CSV/PDF files |
| `app.email-verification.token-expiry-hours=24` | Verification link lifetime |
| `app.data-retention.import-history=90` | Days before import records are auto-deleted |

### Frontend

Edit `src/environments/environment.ts` to point to your backend:

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api'
};
```

For production, create `src/environments/environment.prod.ts`:

```typescript
export const environment = {
  production: true,
  apiUrl: 'https://your-production-domain.com/api'
};
```

---

## Application Routes

| Path | Component | Guard | Description |
|---|---|---|---|
| `/login` | `LoginComponent` | — | User login |
| `/register` | `RegisterComponent` | — | New account registration |
| `/verify-email` | `EmailVerificationComponent` | — | Email token verification |
| `/dashboard` | `DashboardComponent` | `AuthGuard` | Financial overview |
| `/transactions` | `TransactionComponent` | `AuthGuard` | Transaction CRUD |
| `/budgets` | `BudgetComponent` | `AuthGuard` | Budget management |
| `/profile` | `ProfileComponent` | `AuthGuard` | Account settings |
| `/import` | `BankStatementImportComponent` | `AuthGuard` | CSV / PDF import |
| `**` | — | — | Redirects to `/login` |

---

## API Reference

### Base URL: `/api`

#### Authentication — `/api/auth`

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `POST` | `/register` | Register a new user, sends verification email | No |
| `POST` | `/login` | Login; sets `jwt_token` and `refresh_token` HttpOnly cookies | No |
| `POST` | `/refresh` | Exchange refresh token for a new access token | No |
| `POST` | `/logout` | Revokes refresh token and clears cookies | No |

#### Email Verification — `/api/email`

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/verify?token=...` | Verify email address using token from email |
| `POST` | `/resend` | Resend verification email (`{ "email": "..." }`) |

#### Users — `/api/users`

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/me` | Get the currently authenticated user |
| `GET` | `/{userId}/transactions` | Get all transactions (with optional filters) |
| `GET` | `/{userId}/transactions/paginated` | Paginated transactions with sorting |
| `GET` | `/{userId}/transactions/chart` | Transactions by type, ordered by date (for charts) |
| `GET` | `/{userId}/categories` | All categories (global + user-specific) |
| `GET` | `/{userId}/budgets` | All budgets with live spending data |

**Transaction filter query parameters:** `type`, `fromDate`, `toDate`, `categoryId`, `minAmount`, `maxAmount`

**Pagination parameters:** `page`, `size`, `sortBy`, `sortDirection`

#### Transactions — `/api/transactions`

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/` | Create a new transaction |
| `PUT` | `/` | Update an existing transaction |
| `DELETE` | `/{id}` | Delete a transaction (owner-only) |

#### Budgets — `/api/budgets`

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/` | Create a budget |
| `PUT` | `/{id}` | Update a budget (owner-only) |
| `DELETE` | `/{id}` | Delete a budget (owner-only) |
| `POST` | `/check-alerts` | Manually trigger budget alert check |

#### Categories — `/api/categories`

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/` | List all global (predefined) categories |
| `POST` | `/` | Create a custom category |
| `DELETE` | `/{id}` | Delete a category |

#### Profile — `/api/profile`

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/stats` | Account statistics (member since, transaction count, etc.) |
| `PUT` | `/email` | Update email address (requires password confirmation) |
| `PUT` | `/password` | Change password (invalidates all refresh tokens) |
| `POST` | `/logout-all` | Revoke all sessions across all devices |
| `GET` | `/export` | Download all user data as JSON (GDPR) |
| `DELETE` | `/account` | Permanently delete account (requires password) |

#### Import — `/api/import`

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/csv` | Import transactions from a CSV file |
| `POST` | `/pdf` | Import transactions from a bank PDF statement |

**CSV parameters:** `file`, `dateColumn` (default: `Date`), `amountColumn` (default: `Amount`), `descriptionColumn` (default: `Description`), `typeColumn` (optional)

**PDF parameters:** `file`, `bankType` — one of `BCR`, `BRD`, `ING`, `REVOLUT`, `RAIFFEISEN`, `CEC`, `AUTO`

---

## Authentication & Security

### JWT + HttpOnly Cookies

- **Access token**: 15-minute lifetime, stored in `jwt_token` HttpOnly cookie
- **Refresh token**: 7-day lifetime, stored in `refresh_token` HttpOnly cookie, persisted in the database
- Tokens are validated on every request by `JwtAuthFilter`. The `userId` claim is extracted from the JWT and used for all resource ownership checks.

### Frontend Auth Flow

On app startup, `AppComponent.ngOnInit()` calls `authService.autoLogin()`, which hits `GET /api/users/me`. If a valid session cookie exists, the user is restored without requiring a login page visit. `AuthGuard` applies the same flow so that refreshing a protected page works correctly.

`auth.interceptor.ts` catches `401` responses on non-auth endpoints and automatically calls `POST /api/auth/refresh`. If the refresh succeeds, the original request is retried transparently. If it fails, the user is cleared and redirected to `/login`. The `AuthService` handles concurrent refresh requests — if a refresh is already in progress, subsequent 401s queue up and wait for the single refresh call to resolve.

### Ownership Enforcement

Every protected resource (transaction, budget) is checked against the authenticated user's ID via `SecurityUtil.validateResourceOwnership()`. Attempting to access another user's data returns `403 Forbidden`.

### Rate Limiting

`RateLimitFilter` (Bucket4j) allows **100 requests per hour per IP address**. Clients exceeding this receive `429 Too Many Requests` with a `X-Rate-Limit-Retry-After-Seconds` header.

### Security Headers

The following HTTP security headers are applied globally:

- `X-Frame-Options: DENY` (clickjacking prevention)
- `X-XSS-Protection: 1; mode=block`
- `X-Content-Type-Options: nosniff`
- `Strict-Transport-Security` (HSTS, 1 year)
- `Content-Security-Policy` (restrictive default-src)
- `Referrer-Policy: strict-origin-when-cross-origin`
- `Permissions-Policy: geolocation=(), microphone=(), camera=()`

### CORS

Allowed origins (configurable in `SecurityConfiguration`):
- `http://localhost:4200` (Angular dev)
- `http://localhost:3000` (React dev)
- `https://yourdomain.com` (production)

---

## Features

### CSV Import

Upload a `.csv` bank export and map column names to the standard fields. The importer parses dates in multiple formats (DD.MM.YYYY, YYYY-MM-DD, Romanian month names, etc.), handles amount formats with comma/dot separators, auto-categorizes transactions by keyword matching on the description, and tracks each import as an `ImportedFile` batch record.

### PDF Bank Statement Import

Upload a PDF bank statement from a supported Romanian bank. The importer extracts raw text via **Apache PDFBox**, applies bank-specific regex parsers for **BCR, BRD, ING, Revolut, Raiffeisen, CEC**, and falls back to `AUTO` mode which tries all parsers and returns the best result. Duplicate transactions are detected using an MD5 hash of `amount + date + description`. Descriptions are sanitized by redacting IBANs, card numbers, and reference codes.

### Budget Alerts

When a budget's spending reaches the configured threshold (default 80%) or exceeds 100%, an HTML email alert is sent. The alert includes the spending vs. budget amounts, a visual progress bar, and a direct link to the budgets page. Only users with verified email addresses receive alerts.

### Email Verification

New users receive a verification email with a 24-hour token. The frontend `EmailVerificationComponent` calls `GET /api/email/verify?token=...` and displays a success or error state accordingly.

---

## Frontend Components & Services

### Dashboard

Displays totals for all time — income, expenses, net savings, and savings rate. Contains two embedded Chart.js charts and a recent 5-transaction list. `UserService.getChart()` fetches all transactions and the component computes summaries client-side.

### Transactions

A paginated, filterable list with a desktop table view and a mobile card view. Filters (type, category, date range, amount range) are debounced 300ms and reset pagination to page 0 on change. Page size is configurable (10/20/50/100 per page). Pagination is 0-based internally but displayed as 1-based in the UI.

### Budgets

Budget cards with visual progress bars and status badges: **On Track** (green), **Warning** (yellow, ≥80%), **Over Budget** (red, >100%). A summary alert strip appears at the top when any budgets need attention. Create and edit use a Bootstrap modal-style overlay form.

### Profile

Tabbed layout with four sections — **Overview** (account info and stats), **Security** (email change, password change with auto-logout, logout all devices), **Preferences** (placeholder for future settings), and **Data & Privacy** (JSON export and account deletion requiring `DELETE MY ACCOUNT` confirmation plus password).

### Bank Statement Import

A two-mode wizard for CSV and PDF imports. On success, displays a summary of imported/duplicate/error counts and auto-redirects to `/transactions` after 3 seconds if there are no errors. Includes collapsible step-by-step download instructions for BCR, BRD, ING, and Revolut.

### Charts

`LineChartComponent` — weekly/monthly/yearly grouped line chart with income, expenses, and an optional net savings line. `PieChartComponent` — toggles between a summary doughnut (income vs. expenses) and a by-category doughnut for the current month (top 8 categories). Both are standalone and reusable.

### HTTP Interceptors

`authInterceptor` clones every request with `withCredentials: true` and handles token refresh on 401. `errorInterceptor` catches all HTTP errors globally and displays a `ToastService` notification with a user-friendly message; on 401/403 it also redirects to `/login`.

### Core Services Reference

| Service | Key Methods |
|---|---|
| `AuthService` | `autoLogin()`, `login()`, `logout()`, `refreshToken()`, `isAuthenticated()` |
| `TransactionService` | `getTransactionsPaginated()`, `createTransaction()`, `updateTransaction()`, `deleteTransaction()` |
| `BudgetService` | `getBudgetsWithSpending()`, `createBudget()`, `updateBudget()`, `deleteBudget()` |
| `ProfileService` | `getAccountStats()`, `updateEmail()`, `changePassword()`, `logoutAllDevices()`, `exportData()`, `deleteAccount()` |
| `DataRefreshService` | `triggerRefresh()` — Subject-based signal bus for post-import cache busting |
| `ToastService` | `success()`, `error()`, `warning()`, `info()` — auto-dismissing toast notifications |

---

## Scheduling & Background Jobs

| Job | Schedule | Description |
|---|---|---|
| Budget Alert Check | Daily at 08:00 | Checks all active budgets for all verified users and sends alerts |
| Refresh Token Cleanup | Daily at 03:00 | Deletes expired refresh tokens from the database |
| Import History Cleanup | Daily at 02:00 | Deletes `ImportedFile` records older than 90 days |

---

## Caching

Spring Cache (in-memory) is applied to reduce redundant database queries:

| Cache Name | Cached Data | Evicted When |
|---|---|---|
| `transactions` | Transaction lists per user | Any save or delete |
| `categories` | Categories per user | Any save or delete |
| `budgets` | Budget lists per user | Any save, update, or delete |
| `users` | User entities by username/id | On user save |

> **Note:** This uses the default in-memory `ConcurrentMapCacheManager`. For production with multiple instances, replace with a distributed cache (e.g., Redis).

---

## Testing

### Backend

```bash
cd backend/personal-finance-tracker-spring-boot
./mvnw test
```

| Test Class | What it covers |
|---|---|
| `CategoryServiceCacheTest` | Verifies `@Cacheable` caches on first call and `@CacheEvict` clears on write |
| `PersonalFinanceTrackerApplicationTests` | Spring context loads without errors |
| `JwtSecretMakerTest` | Utility to generate a new HS512 JWT secret key |

### Frontend

```bash
cd frontend/angular-personal-finance-tracker
ng test
```

Each component and service has a corresponding `.spec.ts` file generated alongside it. Launch configurations for Chrome debugging are provided in `.vscode/launch.json`.

---

## Environment Variables Reference

| Variable | Required | Description |
|---|---|---|
| `DB_URL` | Yes | JDBC connection URL for MySQL |
| `DB_USERNAME` | Yes | Database username |
| `DB_PASSWORD` | Yes | Database password |
| `JWT_SECRET` | Yes | Base64-encoded HMAC-SHA key (minimum 512 bits) |
| `EMAIL_USERNAME` | Yes | SMTP sender address (Gmail) |
| `EMAIL_PASSWORD` | Yes | Gmail App Password (not your account password) |
| `APP_BASE_URL` | No | Frontend base URL for email links (default: `http://localhost:4200`) |
