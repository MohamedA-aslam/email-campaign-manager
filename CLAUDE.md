# BECMS — Claude Code Project Guide

## Project Overview

**BECMS** (Email Campaign Management System) is a Spring Boot 3.2.5 web application for creating, scheduling, and sending email marketing campaigns. It features AI-powered email content generation via the Anthropic Claude API.

- **Entry point:** `src/main/java/com/campaign/BecmsApplication.java`
- **Runs on:** `http://localhost:8080`
- **Default landing page:** `/dashboard`
- **Database:** SQLite file `campaign.db` (auto-created in project root on first run)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.2.5 |
| Language | Java 21 (preview features enabled via `--enable-preview` in pom.xml) |
| Build | Maven |
| ORM | Hibernate + Spring Data JPA |
| Database | SQLite via `org.xerial:sqlite-jdbc:3.45.3.0` + `hibernate-community-dialects` |
| Views | Thymeleaf (server-side, no JS framework) |
| Email | Spring Mail — Gmail SMTP (port 587, STARTTLS) |
| CSV parsing | OpenCSV 5.9 |
| AI | Anthropic Claude API (`claude-sonnet-4-20250514`) via Java `HttpClient` |
| Boilerplate | Lombok 1.18.36 |
| Scheduler | Spring `@Scheduled` |

---

## Running the Project

### Required Environment Variables

The app **will not start** without these:

```bash
MAIL_USERNAME=yourgmail@gmail.com
MAIL_PASSWORD=your-gmail-app-password   # Generate at myaccount.google.com → Security → App passwords
ANTHROPIC_API_KEY=sk-ant-...            # Get at console.anthropic.com
```

Set in VS Code via `.vscode/launch.json` under the `"env"` key (already created).

### Start Commands

```bash
# Via Maven
mvn spring-boot:run

# Or build and run JAR
mvn clean package
java -jar target/email-campaign-0.0.1-SNAPSHOT.jar
```

### VS Code
Use **Run & Debug** (Ctrl+Shift+D) → **"Run BECMS Application"**

---

## Project Structure

```
src/main/java/com/campaign/
├── BecmsApplication.java               # @SpringBootApplication + @EnableScheduling
│
├── controller/
│   ├── CampaignController.java         # GET/POST /campaigns/**
│   ├── RecipientController.java        # GET/POST /recipients/**
│   ├── DashboardController.java        # GET / and /dashboard
│   └── AiContentController.java        # POST /campaigns/generate-content (@RestController)
│
├── service/
│   ├── CampaignService.java            # Campaign CRUD + executeCampaign()
│   ├── RecipientService.java           # Recipient CRUD + CSV bulk upload
│   ├── EmailService.java               # JavaMailSender wrapper
│   └── CampaignSchedulerService.java   # @Scheduled(fixedDelay=10000) — runs every 10s
│
├── repository/
│   ├── CampaignRepository.java         # JpaRepository<Campaign, Long>
│   ├── RecipientRepository.java        # JpaRepository<Recipient, Long>
│   └── DeliveryLogRepository.java      # JpaRepository<DeliveryLog, Long>
│
├── model/
│   ├── Campaign.java                   # @Entity campaigns table — has CampaignStatus enum
│   ├── Recipient.java                  # @Entity recipients table — has SubscriptionStatus enum
│   └── DeliveryLog.java                # @Entity delivery_logs table — has DeliveryStatus enum
│
└── dto/
    ├── CampaignDTO.java                # Campaign + delivery stats (sentCount, failedCount, totalRecipients)
    ├── DashboardDTO.java               # Aggregated dashboard metrics + campaign list
    ├── GenerateContentRequest.java     # record(String subject) — AI endpoint request body
    └── GenerateContentResponse.java    # record(String content) — AI endpoint response body

src/main/resources/
├── application.properties              # All configuration
└── templates/
    ├── dashboard.html                  # / and /dashboard
    ├── campaign-list.html              # /campaigns
    ├── campaign-form.html              # /campaigns/new and /campaigns/{id}/edit
    ├── campaign-detail.html            # /campaigns/{id}/detail
    ├── recipients.html                 # /recipients (list + add form + CSV upload)
    └── recipient-edit.html             # /recipients/{id}/edit
```

---

## Data Models

### Campaign (`campaigns` table)
```
id              BIGINT PK AUTO_INCREMENT
name            VARCHAR NOT NULL
subject         VARCHAR NOT NULL
content         TEXT (LOB) NOT NULL
scheduled_time  DATETIME (nullable)
status          VARCHAR NOT NULL  -- DRAFT | SCHEDULED | IN_PROGRESS | COMPLETED
created_at      DATETIME NOT NULL (set on create, not updatable)
```
- `@OneToMany(cascade = ALL)` → `DeliveryLog` (deleting a Campaign deletes its logs)
- Status auto-set to `SCHEDULED` when `scheduledTime` is provided on save

### Recipient (`recipients` table)
```
id                  BIGINT PK AUTO_INCREMENT
name                VARCHAR NOT NULL
email               VARCHAR NOT NULL UNIQUE
subscription_status VARCHAR NOT NULL  -- SUBSCRIBED | UNSUBSCRIBED
created_at          DATETIME NOT NULL
```
- Unique constraint on `email` at both DB and entity level

### DeliveryLog (`delivery_logs` table)
```
id              BIGINT PK AUTO_INCREMENT
campaign_id     BIGINT FK → campaigns.id NOT NULL
recipient_email VARCHAR NOT NULL
recipient_name  VARCHAR
status          VARCHAR NOT NULL  -- SENT | FAILED
failure_reason  VARCHAR (nullable)
sent_at         DATETIME NOT NULL
```

---

## Key Workflows

### Campaign Execution Flow
1. Campaign saved with `scheduledTime` → status = `SCHEDULED`
2. `CampaignSchedulerService` polls every 10 seconds for campaigns where `status = SCHEDULED AND scheduled_time <= now`
3. Calls `CampaignService.executeCampaign()` which:
   - Sets status → `IN_PROGRESS`, saves
   - Fetches all `SUBSCRIBED` recipients
   - For each recipient: calls `EmailService.sendEmail()`, creates `DeliveryLog` (SENT or FAILED)
   - Sets status → `COMPLETED`, saves
4. "Send Now" button on campaign detail page triggers the same `executeCampaign()` immediately

### CSV Bulk Upload Flow
- POST `/recipients/upload` with multipart `file`
- `RecipientService.uploadFromCsv()` reads CSV with OpenCSV
- Expected columns: `name, email, subscription_status` (header row is skipped)
- Validates email format with regex, skips duplicates, returns `BulkUploadResult` record
- `subscription_status` defaults to `SUBSCRIBED` if column missing or invalid

### AI Content Generation Flow
- User types subject line in campaign form, clicks "✨ Generate with AI"
- JS `fetch()` POSTs `{ "subject": "..." }` to `/campaigns/generate-content`
- `AiContentController` calls `https://api.anthropic.com/v1/messages` using Java `HttpClient`
- Model: `claude-sonnet-4-20250514`, max_tokens: 1024
- Response `content[0].text` is returned as `{ "content": "..." }`
- JS pastes result into the email content textarea

---

## API Endpoints

### Web (Controller — returns HTML views)
| Method | Path | Handler | Description |
|---|---|---|---|
| GET | `/` or `/dashboard` | DashboardController | Dashboard with KPIs |
| GET | `/campaigns` | CampaignController | List all campaigns |
| GET | `/campaigns/new` | CampaignController | New campaign form |
| POST | `/campaigns` | CampaignController | Create campaign |
| GET | `/campaigns/{id}/edit` | CampaignController | Edit form |
| POST | `/campaigns/{id}` | CampaignController | Update campaign |
| POST | `/campaigns/{id}/delete` | CampaignController | Delete campaign |
| GET | `/campaigns/{id}/detail` | CampaignController | Campaign detail + logs |
| POST | `/campaigns/{id}/send` | CampaignController | Send campaign immediately |
| GET | `/recipients` | RecipientController | List recipients + add form |
| POST | `/recipients` | RecipientController | Add single recipient |
| POST | `/recipients/upload` | RecipientController | CSV bulk upload |
| GET | `/recipients/{id}/edit` | RecipientController | Edit recipient form |
| POST | `/recipients/{id}` | RecipientController | Update recipient |
| POST | `/recipients/{id}/delete` | RecipientController | Delete recipient |

### REST (AiContentController — returns JSON)
| Method | Path | Body | Response |
|---|---|---|---|
| POST | `/campaigns/generate-content` | `{ "subject": "..." }` | `{ "content": "..." }` |

---

## Configuration Reference (`application.properties`)

```properties
# Server
spring.application.name=email-campaign
server.port=8080

# SQLite
spring.datasource.url=jdbc:sqlite:campaign.db
spring.datasource.driver-class-name=org.sqlite.JDBC
spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect
spring.jpa.hibernate.ddl-auto=update          # Auto-creates/updates schema on startup
spring.jpa.show-sql=true                      # Logs all SQL — turn off in production
spring.jpa.properties.hibernate.format_sql=true

# Thymeleaf
spring.thymeleaf.cache=false                  # Dev mode — disable for production

# Gmail SMTP
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# Scheduler thread pool
spring.task.scheduling.pool.size=5

# Anthropic API
anthropic.api.key=${ANTHROPIC_API_KEY}
```

---

## Repository Custom Methods

### CampaignRepository
```java
findByStatus(CampaignStatus status)
findByStatusAndScheduledTimeBefore(CampaignStatus status, LocalDateTime time)
```

### RecipientRepository
```java
findByEmail(String email)                               // returns Optional<Recipient>
existsByEmail(String email)                             // returns boolean
findBySubscriptionStatus(SubscriptionStatus status)
```

### DeliveryLogRepository
```java
findByCampaignId(Long campaignId)
countByCampaignIdAndStatus(Long campaignId, DeliveryStatus status)
countByCampaignId(Long campaignId)
countByStatus(DeliveryStatus status)
```

---

## Known Issues & Limitations

### No Authentication
The entire application is open — any URL is accessible without login. There is no Spring Security dependency. Do not expose this app publicly without adding authentication first.

### No Pagination
`findAll()` is called on campaigns and recipients with no limit. Will become slow with large datasets.

### DashboardController Bypasses Service Layer
`DashboardController` directly injects `RecipientRepository` and `DeliveryLogRepository` instead of going through services. Acceptable for now but inconsistent.

### Double `findAll()` in DashboardController
`campaignService.findAll()` is called twice in the dashboard handler (once for count, once for the list). Should be called once and reused.

### No `@Transactional` on CSV Upload
`RecipientService.uploadFromCsv()` saves recipients one by one without a transaction. A failure mid-upload leaves partial data.

### Scheduler Comment Mismatch
`CampaignSchedulerService` has a comment saying "every minute" but `fixedDelay = 10000` runs every 10 seconds.

### `--enable-preview` Enabled
The Maven compiler has `--enable-preview` for Java 21 but no preview features are actually used in the code.

### Emails are Plain Text Only
`EmailService` uses `SimpleMailMessage` which sends plain text only. HTML email templates are not supported.

---

## Lombok Annotations Used

All models use the standard set:
```java
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
```
Services and controllers use:
```java
@RequiredArgsConstructor   // constructor injection for final fields
@Slf4j                     // injects `log` logger
```

`@Builder.Default` is required on fields with default values (e.g., `status`, `createdAt`, `deliveryLogs`) to prevent Lombok builder from ignoring the field initializer.

---

## Adding New Features — Patterns to Follow

### New Service Method
- Add to the appropriate service class
- Use `@Transactional` if the method writes to the database
- Throw `NoSuchElementException` for not-found cases (consistent with existing services)

### New Web Endpoint
- Add `@GetMapping` / `@PostMapping` to the relevant controller
- Use `RedirectAttributes.addFlashAttribute()` for success/error messages on redirects
- Use `BindingResult` + `@Valid` for form validation

### New REST Endpoint
- Add to `AiContentController` or create a new `@RestController`
- Return `ResponseEntity<YourResponseDTO>`
- Use Java record DTOs for request/response bodies

### New Entity
- Annotate with `@Entity`, `@Table(name = "...")`
- Add `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`
- Create a `JpaRepository` interface in `repository/`
- Hibernate will auto-create the table (`ddl-auto=update`)

### New Template
- Place in `src/main/resources/templates/`
- Include the standard `<nav>` block (copy from any existing template)
- Use `th:object`, `th:field`, `th:errors` for form binding
- Use `th:if="${success}"` / `th:if="${error}"` for flash messages

---

## Security Checklist (Before Any Production Deployment)

- [ ] Add Spring Security with login/authentication
- [ ] Set `spring.jpa.show-sql=false`
- [ ] Set `spring.thymeleaf.cache=true`
- [ ] Move all secrets to environment variables (already done for mail and API key)
- [ ] Add CSRF token handling on forms
- [ ] Add rate limiting on `/campaigns/generate-content`
- [ ] Add proper error pages (`/error`, 404, 500)
- [ ] Add pagination to campaign and recipient list queries
- [ ] Add `@Transactional` to `RecipientService.uploadFromCsv()`
