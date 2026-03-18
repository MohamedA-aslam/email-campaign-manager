# 📧 Email Campaign Manager

> A Java-based web application for creating, scheduling, and tracking email marketing campaigns.

![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.5-6DB33F?style=flat&logo=spring-boot)
![Java](https://img.shields.io/badge/Java-21_LTS-ED8B00?style=flat&logo=openjdk)
![SQLite](https://img.shields.io/badge/SQLite-Hibernate-003B57?style=flat&logo=sqlite)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-Frontend-005F0F?style=flat)

---

## Table of Contents

- [Overview](#overview)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Setup & Installation](#setup--installation)
- [Usage Guide](#usage-guide)
- [Architecture](#architecture)
- [Database](#database)
- [Troubleshooting](#troubleshooting)
- [Sample CSV Reference](#sample-csv-reference)

---

## Overview

Email Campaign Manager is a full-stack Spring Boot web application that enables admin to manage email campaigns. It covers from campaign creation and recipient management to automated scheduled delivery and per-email delivery tracking.

### Key Features

- Create and manage campaigns with `DRAFT`, `SCHEDULED`, `IN_PROGRESS`, and `COMPLETED` statuses
- Bulk upload recipients via CSV with email format validation and duplicate detection
- Automatic campaign execution based on a scheduled time (polls every 60 seconds)
- Manual **Send Now** option to trigger campaigns immediately from the UI
- Detailed delivery logs per campaign showing `SENT` / `FAILED` status and failure reasons
- Admin dashboard contains: total campaigns, recipients, sent count, and failed count

---

## Technology Stack

| Layer | Technology | Purpose |
|---|---|---|
| Backend Framework | Spring Boot 3.2.5 | Web layer, DI, scheduling |
| Database | Hibernate + SQLite | Persistence via JPA |
| Frontend | Thymeleaf | Server-side HTML rendering |
| Email Delivery | Spring Mail (JavaMailSender) | SMTP email dispatch |
| CSV Parsing | OpenCSV 5.9 | Bulk recipient upload |
| Build Tool | Maven | Dependency management |
| Boilerplate Reduction | Lombok | Getters, setters, builders |
| Java Version | Java 21 | Runtime environment |

---

## Project Structure

```
email-campaign/
├── src/main/java/com/campaign/
│   ├── BecmsApplication.java         # App entry point + @EnableScheduling
│   ├── controller/
│   │   ├── CampaignController.java           # Campaign CRUD + send now
│   │   ├── RecipientController.java          # Recipient management + CSV upload
│   │   └── DashboardController.java          # Dashboard stats view
│   ├── model/
│   │   ├── Campaign.java                     # Campaign entity (status enum inside)
│   │   ├── Recipient.java                    # Recipient entity (unique email)
│   │   └── DeliveryLog.java                  # Per-email delivery record
│   ├── repository/
│   │   ├── CampaignRepository.java
│   │   ├── RecipientRepository.java
│   │   └── DeliveryLogRepository.java
│   ├── service/
│   │   ├── CampaignService.java              # Core campaign logic + execution
│   │   ├── RecipientService.java             # CSV parsing + validation
│   │   ├── EmailService.java                 # SMTP dispatch wrapper
│   │   └── CampaignSchedulerService.java     # @Scheduled poller (every 60s)
│   └── dto/
│       ├── CampaignDTO.java                  # View-layer campaign data
│       └── DashboardDTO.java                 # View-layer dashboard stats
├── src/main/resources/
│   ├── application.properties
│   └── templates/
│       ├── dashboard.html
│       ├── campaign-list.html
│       ├── campaign-form.html
│       ├── campaign-detail.html
│       └── recipients.html
├── sample-recipients.csv                     # Sample data for testing
├── campaign.db                               # Auto-created SQLite DB on first run
└── pom.xml
```

---

## Setup & Installation

### Prerequisites

| Requirement | Version |
|---|---|
| Java JDK | **21** (required) |
| IntelliJ IDEA | Any edition |
| Maven | 3.8+ (bundled with IntelliJ) |
| Gmail Account | For SMTP sending |

---

### Step 1 — Open the Project

1. Open your IDE
2. **File → Open** → select the `email-campaign` folder
3. Wait for Maven to download all dependencies (bottom progress bar)

---

### Step 2 — Configure Gmail SMTP

Open `src/main/resources/application.properties` and update:

```properties
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}
```

**In GitHub:**

1. Go to Repository Settings

   `Settings → Secrets and variables → Actions`

2. Add Secrets

   `MAIL_USERNAME → your email`
   
   `MAIL_PASSWORD → your app password`

> 💡 **App Password Setup:** Google Account → Security → 2-Step Verification → App Passwords → Generate for "Mail". Use the generated 16-character password — **not** your regular Gmail password.

---

### Step 3 — IntelliJ Configuration


**Enable Annotation Processing (Lombok):**
1. `File → Settings → Build → Compiler → Annotation Processors`
2. ✅ Check **Enable annotation processing**
3. Apply → OK

---

### Step 4 — Run the Application

1. Open `BecmsApplication.java`
2. Click the green ▶ **Run** button (or `Shift+F10`)
3. Wait for the console to show:
   ```
   Started BecmsApplication in X.XXX seconds
   ```
4. Open your browser and go to **http://localhost:8080**

---

## Usage Guide

### Page Reference

| URL | Page | Description |
|---|---|---|
| `http://localhost:8080/` | Dashboard | System overview and stats |
| `http://localhost:8080/campaigns` | Campaign List | View, edit, delete campaigns |
| `http://localhost:8080/campaigns/new` | New Campaign | Create a campaign |
| `http://localhost:8080/campaigns/{id}/detail` | Campaign Detail | View delivery logs, send now |
| `http://localhost:8080/recipients` | Recipients | Manage and upload recipients |

---

### Creating a Campaign

1. Go to **New Campaign**
2. Fill in: **Campaign Name**, **Subject Line**, **Email Content**
3. Optionally set a **Scheduled Time** — the system will auto-send at that time
4. Leave Scheduled Time blank to save as `DRAFT`
5. Click **Save Campaign**

---

### Uploading Recipients via CSV



Steps:
1. Go to the **Recipients** page
2. Click **Choose File** and select your `.csv` file
3. Click **Upload**
4. The system validates emails, skips duplicates, and reports results

> 📝 `subscription_status` must be `SUBSCRIBED` or `UNSUBSCRIBED` (case-insensitive). Invalid values default to `SUBSCRIBED`. Duplicate emails are silently skipped with a count in the upload summary.

The CSV must have this format (header row required):

```csv
name,email,subscription_status
Alice Johnson,alice@example.com,SUBSCRIBED
Bob Smith,bob@example.com,UNSUBSCRIBED

```
---

### Sending a Campaign

**Automatic (Scheduled):**
- Set a Scheduled Time when creating the campaign
- The scheduler polls every 60 seconds and sends automatically when the time arrives

**Manual (Send Now):**
- Open any campaign's detail page
- Click the **▶ Send Now** button
- Emails are dispatched immediately to all `SUBSCRIBED` recipients

---

## Architecture

### Layer Responsibilities

| Layer | Class(es) | Responsibility |
|---|---|---|
| Model | `Campaign`, `Recipient`, `DeliveryLog` | JPA entities mapped to SQLite via Hibernate |
| Repository | `*Repository` interfaces | Spring Data JPA — zero boilerplate SQL |
| Service | `CampaignService`, `RecipientService`, `EmailService`, `CampaignSchedulerService` | Business logic, email sending, CSV parsing, scheduling |
| Controller | `CampaignController`, `RecipientController`, `DashboardController` | HTTP routing, form binding, redirect handling |
| DTO | `CampaignDTO`, `DashboardDTO` | View-layer data transfer objects |
| Templates | Thymeleaf HTML files | Server-side rendered UI |

---

### Data Model

| Entity | Key Fields | Relationships |
|---|---|---|
| `Campaign` | `name`, `subject`, `content`, `scheduledTime`, `status` (enum) | One-to-Many → `DeliveryLog` |
| `Recipient` | `name`, `email` (unique), `subscriptionStatus` (enum) | Standalone |
| `DeliveryLog` | `recipientEmail`, `status` (SENT/FAILED), `failureReason`, `sentAt` | Many-to-One → `Campaign` |

---

### Campaign Status Flow

```
  DRAFT ──(schedule set)──▶ SCHEDULED
                                 │
                        (scheduler fires or Send Now)
                                 │
                                 ▼
                           IN_PROGRESS
                                 │
                        (all emails processed)
                                 │
                                 ▼
                            COMPLETED
```

---

## Database

The application uses **SQLite** with **Hibernate ORM**. The database file `campaign.db` is automatically created in the project root on first run. No manual database setup is required.

```properties
# From application.properties
spring.datasource.url=jdbc:sqlite:campaign.db
spring.jpa.hibernate.ddl-auto=update
```

> `ddl-auto=update` means Hibernate automatically creates and migrates tables based on your entity classes.

### Tables

| Table | Description |
|---|---|
| `campaigns` | All campaign records including content, scheduled time, and status |
| `recipients` | Recipient details; `email` column has a unique constraint |
| `delivery_logs` | One row per email attempt, linked to a campaign via `campaign_id` |

---

## Troubleshooting

| Error | Cause | Fix |
|---|---|---|
| `TypeTag::UNKNOWN` | Lombok incompatible with JDK 25 | Switch to **JDK 21**. Download from [adoptium.net](https://adoptium.net) |
| `No static resource dashboard` | Thymeleaf can't find templates | Ensure `templates/` is inside `src/main/resources/` and the folder is marked as **Resources Root** |
| Mail send failure | Wrong SMTP credentials | Use a Gmail **App Password**, not your account password |
| Port 8080 in use | Another process on the port | Change `server.port=8081` in `application.properties` |
| `campaign.db locked` | Multiple app instances running | Stop all instances — SQLite only allows one writer at a time |
| Duplicate email skipped | Email already exists in DB | Expected behavior — duplicates are counted and reported in the upload result |

---

## Sample CSV Reference

A `sample-recipients.csv` file is included in the project root for quick testing:

```csv
name,email,subscription_status
Alice Johnson,alice.johnson@example.com,SUBSCRIBED
Bob Smith,bob.smith@example.com,SUBSCRIBED
Carol White,carol.white@example.com,SUBSCRIBED
David Brown,david.brown@example.com,UNSUBSCRIBED
Eva Martinez,eva.martinez@example.com,SUBSCRIBED
Frank Lee,frank.lee@example.com,SUBSCRIBED
Grace Kim,grace.kim@example.com,SUBSCRIBED
Henry Wilson,henry.wilson@example.com,UNSUBSCRIBED
Isabel Chen,isabel.chen@example.com,SUBSCRIBED
James Taylor,james.taylor@example.com,SUBSCRIBED
```

---

*Built with Spring Boot · SQLite · Hibernate · Thymeleaf*
