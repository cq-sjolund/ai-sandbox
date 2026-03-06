# Worklog - Consultant Time Tracking Application

A full-stack consultant worklog application for tracking daily activities across different projects and clients with AI-powered features and Microsoft Dynamics 365 integration.

## Features

### Core Functionality
- **📅 Calendar & List Views** - Visual calendar and month-based list view to browse entries
- **🔢 Multiple Entries Per Day** - Create separate entries for different projects on the same day
- **⏱️ Hours Tracking** - Track time spent with decimal precision (0.5, 3.5, 8.0 hours)
- **🎨 Color-Coded Projects** - Visual project indicators with AI-suggested colors
- **👥 Multi-User Support** - Each user has private entries and projects with role-based access
- **🔐 JWT Authentication** - Secure login with JWT tokens and user management

### AI-Powered Features
- **🤖 AI Summaries** - Generate period summaries with custom prompts
- **🎨 Smart Colors** - AI suggests appropriate colors for new projects
- **✍️ Auto-Complete** - AI-powered description completion
- **💬 Ask AI** - Query your worklog data with natural language

### Microsoft Dynamics 365 Integration
- **📥 Manual Import** - Import time entries via copy-paste JSON
- **🧠 AI Project Mapping** - Intelligently maps and merges similar projects
- **🚫 Duplicate Prevention** - Skips importing days that already have entries
- **🏷️ Smart Naming** - Keeps more descriptive project names when merging

### User Experience
- **🎨 Adobe Spectrum Design** - Professional UI with Adobe's design system
- **🐳 Docker Deployment** - Complete containerized setup with persistent data
- **🔒 Secure Configuration** - Environment-based secrets management

## Technology Stack

- **Frontend**: React 18 + Vite + Adobe Spectrum + React Router
- **Backend**: Spring Boot 3.x (Java 17+) + Maven + Spring Security
- **Database**: PostgreSQL 16 with Flyway migrations
- **Authentication**: JWT tokens with BCrypt password hashing
- **AI Integration**: OpenAI API (GPT-4 models)
- **Containerization**: Docker + Docker Compose

## Architecture

```
┌─────────────────────────────────────────┐
│  Frontend (nginx:alpine)                │
│  React + Adobe Spectrum                 │
│  Port: 3001 → 80                        │
└───────────────┬─────────────────────────┘
                │ REST API
┌───────────────▼─────────────────────────┐
│  Backend (eclipse-temurin:21-jre)      │
│  Spring Boot + JPA                      │
│  Port: 8081                             │
└───────────────┬─────────────────────────┘
                │ JDBC
┌───────────────▼─────────────────────────┐
│  PostgreSQL (postgres:16-alpine)       │
│  Named Volume: postgres_data            │
│  Port: 5433                             │
└─────────────────────────────────────────┘
```

## Prerequisites

- Docker 20.10+
- Docker Compose 2.0+
- OpenAI API Key ([Get one here](https://platform.openai.com/api-keys))

## Quick Start

### 1. Clone and Navigate to Directory

```bash
cd worklog
```

### 2. Configure Environment Variables

Copy the example environment file and add your OpenAI API key:

```bash
# The .env file should already exist, but if not:
cp .env.example .env
```

Edit `.env` and configure:

```env
POSTGRES_DB=worklog_db
POSTGRES_USER=worklog_user
POSTGRES_PASSWORD=your_secure_password_here

# IMPORTANT: Add your OpenAI API key here
OPENAI_API_KEY=sk-proj-your-actual-api-key-here

# JWT secret for authentication (use a strong random string in production)
JWT_SECRET=your-256-bit-secret-key-change-this-in-production

SPRING_PROFILE=prod
```

⚠️ **IMPORTANT**: Never commit the `.env` file to git. It's already in `.gitignore`.

### 3. Start the Application

```bash
docker-compose up -d
```

This will:
- Download required Docker images
- Build frontend and backend containers
- Start PostgreSQL with persistent storage
- Run database migrations
- Seed initial project data

### 4. Access the Application

- **Frontend**: http://localhost:3001
- **Backend API**: http://localhost:8081/api
- **Health Check**: http://localhost:8081/actuator/health

### 5. Login

On first access, you'll be redirected to the login page.

**Default Admin Credentials:**
- Username: `admin`
- Password: `admin`

⚠️ **IMPORTANT**: Change the admin password in production!

After login, you can:
- Create additional users (admin only)
- Manage your time entries and projects
- Import from Dynamics 365
- Generate AI summaries

### 6. Verify Setup

```bash
# Check all containers are running
docker-compose ps

# View logs
docker-compose logs -f

# Check backend health
curl http://localhost:8081/actuator/health

# Login and get JWT token
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'

# Check projects endpoint (requires authentication)
# Replace YOUR_JWT_TOKEN with the token from login response
curl http://localhost:8081/api/projects \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## Usage Guide

### Managing Projects

1. Navigate to the **Projects** section in the sidebar
2. Click **New Project** to add a project
3. Enter project name - AI will suggest an appropriate color!
4. Optionally add a description
5. Projects are color-coded for easy identification
6. Each user has their own private projects

**Note:** No default projects are seeded. Create your own projects to get started!

### Creating Worklog Entries

**List View**:
1. Click "Add Entry" button
2. Fill in the entry form:
   - **Date**: Select date (defaults to today)
   - **Summary**: Brief description (max 255 characters)
   - **Hours**: Time spent (supports decimals: 0.5, 3.5, 8.0)
   - **Project**: Select from dropdown with color indicators
   - **Description**: Detailed work notes (supports AI auto-complete)
3. Click **Save**

**Calendar View**:
1. Click on any date in the calendar
   - If empty: Entry dialog opens for that date
   - If has entries: Entry list opens
2. Click on individual entries to edit them directly
3. Fill in or modify the entry form
4. Click **Save**

### Multiple Entries Per Day

You can create multiple entries for the same day:

**Example: March 5th**
- Entry 1: 3 hours on "Client A" - "Requirements gathering"
- Entry 2: 5 hours on "Enablement" - "React training"

**Calendar Display**:
- Shows two colored dots (one for each project)
- Displays total hours: "8.0h"
- Click date to see all entries

### Viewing and Editing Entries

**List View** (default):
- Browse entries by month using Previous/Next Month buttons
- Shows all entries for the selected month
- Each entry displays: project color, summary, hours, description
- Click **Edit** to modify, **Delete** to remove

**Calendar View**:
- Toggle to "Calendar View" to see entries in a calendar grid
- Days with entries show colored dots for each project
- Click on a date to see all entries for that day
- Click directly on individual entries to edit them
- Navigate months with Previous/Next/Today buttons

### Generating AI Summaries

1. Navigate to the **AI Summary** panel in the sidebar
2. Select date range using one of:
   - Quick buttons: Last Week, Last Month, Last Quarter
   - Custom date pickers for Start and End dates
3. Click **Generate Summary**
4. Wait for AI to process (typically 5-15 seconds)
5. View the formatted summary including:
   - High-level overview of work completed
   - Key achievements and deliverables
   - Hours breakdown by project
   - Notable patterns and observations

**Example Summary Output**:
```
Period: Feb 26, 2026 to Mar 5, 2026
Total Hours Logged: 42.5

## Summary
During this period, significant progress was made across multiple projects...

## Enablement (18.5 hours)
- Completed React training modules
- Set up development environment
...

## Client A (24 hours)
- Requirements gathering and analysis
- Initial architecture design
...

## Key Achievements
- Successfully delivered feature X
- Reduced deployment time by 40%
...
```

### Importing from Microsoft Dynamics 365

The application supports importing time entries from Dynamics 365:

1. Click "Import from Dynamics" button in the dashboard header
2. Follow the wizard instructions:
   - Open your Dynamics 365 instance in a new tab
   - Run the provided JavaScript in the browser console (F12)
   - Copy the JSON output
3. Paste the JSON into the import dialog
4. Click "Analyze & Import"
5. AI will analyze your Dynamics projects and suggest mappings to existing worklog projects
6. Review and adjust mappings if needed
7. Click "Confirm & Import"

**Features:**
- **AI Project Mapping**: Intelligently matches Dynamics projects to your existing projects
- **Smart Merging**: Keeps more descriptive project names (e.g., "DNB Bank ASA | DR3730612" instead of "DNB")
- **Duplicate Prevention**: Skips importing days that already have entries
- **Bulk Import**: Supports importing thousands of entries at once with pagination

### Managing Users (Admin Only)

Administrators can manage users:

1. Click "Manage Users" in the dashboard header
2. View all users with their roles and creation dates
3. Click "Create User" to add a new user
4. Enter username, password, and select role (USER or ADMIN)
5. Delete users if needed (cascades to their entries and projects)

**Roles:**
- **USER**: Can manage own entries and projects, use AI features, import from Dynamics
- **ADMIN**: All USER permissions + user management

## Data Persistence

All data is stored in a Docker named volume (`postgres_data`) which persists across container restarts:

```bash
# Data survives normal shutdown
docker-compose down
docker-compose up -d  # Data is still there

# To completely remove data (WARNING: Destructive)
docker-compose down -v
```

## API Documentation

**Note:** All API endpoints (except `/api/auth/**`) require JWT authentication via `Authorization: Bearer {token}` header.

### Authentication API

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/login` | Login with username/password, returns JWT token |
| GET | `/api/auth/me` | Get current user info |

### Users API (Admin Only)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/users` | List all users |
| GET | `/api/users/{id}` | Get user by ID |
| POST | `/api/users` | Create new user |
| DELETE | `/api/users/{id}` | Delete user (cascades) |

### Projects API

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/projects` | List current user's projects |
| GET | `/api/projects/{id}` | Get project by ID |
| POST | `/api/projects` | Create new project |
| PUT | `/api/projects/{id}` | Update project |
| DELETE | `/api/projects/{id}` | Delete project |

### Worklog Entries API

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/entries` | List current user's entries |
| GET | `/api/entries/{id}` | Get entry by ID |
| GET | `/api/entries/date/{date}` | Get entries for specific date |
| GET | `/api/entries/range?start={date}&end={date}` | Get entries in date range |
| POST | `/api/entries` | Create new entry |
| PUT | `/api/entries/{id}` | Update entry |
| DELETE | `/api/entries/{id}` | Delete entry |

### AI API

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/ai/summary` | Generate period summary |
| POST | `/api/ai/suggest-color` | Suggest color for project name |
| POST | `/api/ai/complete-description` | Auto-complete entry description |
| POST | `/api/ai/ask` | Ask natural language questions about worklog data |

**AI Summary Request**:
```json
{
  "dateRangeStart": "2026-03-01",
  "dateRangeEnd": "2026-03-31",
  "projectIds": [1, 2],
  "customPrompt": "Focus on achievements"
}
```

### Dynamics Integration API

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/dynamics/analyze-mappings` | Analyze Dynamics projects with AI |
| POST | `/api/dynamics/import` | Import entries with project mappings |

## Development

### Running Backend Locally

```bash
cd backend

# Set environment variables
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/worklog_db
export SPRING_DATASOURCE_USERNAME=worklog_user
export SPRING_DATASOURCE_PASSWORD=your_password
export OPENAI_API_KEY=your_key

# Run with Maven
./mvnw spring-boot:run
```

### Running Frontend Locally

```bash
cd frontend

# Install dependencies
npm install

# Start development server
npm run dev

# Access at http://localhost:3001
```

### Database Migrations

Flyway migrations are located in `backend/src/main/resources/db/migration/`:
- `V1__Create_projects_table.sql` - Projects schema
- `V2__Create_worklog_entries_table.sql` - Worklog entries schema
- `V3__Add_seed_data.sql` - Seed data (REMOVED - no longer seeds projects)
- `V4__Create_users_table.sql` - Users and authentication
- `V5__Add_user_id_to_projects.sql` - Multi-user project ownership
- `V6__Add_dynamics_integration.sql` - Dynamics sync fields

Migrations run automatically on application startup. A default admin user is created with username/password: `admin`/`admin`.

## Troubleshooting

### Container Won't Start

```bash
# Check logs for errors
docker-compose logs backend
docker-compose logs frontend
docker-compose logs postgres

# Rebuild containers
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

### Database Connection Issues

```bash
# Check PostgreSQL is healthy
docker-compose ps postgres

# Connect to database directly
docker-compose exec postgres psql -U worklog_user -d worklog_db

# Check if migrations ran
docker-compose exec postgres psql -U worklog_user -d worklog_db -c "\dt"
```

### AI Summary Not Working

1. Verify your OpenAI API key is correct in `.env`
2. Check backend logs for OpenAI API errors:
   ```bash
   docker-compose logs backend | grep -i openai
   ```
3. Ensure you have API credits available in your OpenAI account
4. Test API key manually:
   ```bash
   curl https://api.openai.com/v1/chat/completions \
     -H "Authorization: Bearer $OPENAI_API_KEY" \
     -H "Content-Type: application/json" \
     -d '{"model":"gpt-4o-mini","messages":[{"role":"user","content":"Hello"}]}'
   ```

### Frontend Not Loading

1. Check nginx logs:
   ```bash
   docker-compose logs frontend
   ```
2. Verify build completed successfully:
   ```bash
   docker-compose build frontend
   ```
3. Check browser console for JavaScript errors

### Authentication Issues

**Can't login / 401 Unauthorized:**
1. Verify default admin user exists:
   ```bash
   docker-compose exec postgres psql -U worklog_user -d worklog_db -c "SELECT * FROM users WHERE username='admin';"
   ```
2. Check JWT_SECRET is set in `.env`
3. Try clearing browser localStorage and cookies
4. Check backend logs for authentication errors:
   ```bash
   docker-compose logs backend | grep -i auth
   ```

**Token expired:**
- JWT tokens expire after 24 hours
- Simply log out and log back in to get a new token

**Forgot admin password:**
1. Connect to database:
   ```bash
   docker-compose exec postgres psql -U worklog_user -d worklog_db
   ```
2. Reset password (BCrypt hash of "admin"):
   ```sql
   UPDATE users SET password='$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYzpLHJ7MYe' WHERE username='admin';
   ```

### Dynamics Import Issues

**Script errors in Dynamics console:**
1. Ensure you're logged into Dynamics 365
2. Make sure you have access to time entries
3. Check browser console for CORS or network errors
4. Verify the date range in the script is valid

**AI mapping not working:**
1. Verify OPENAI_API_KEY is set correctly
2. Check backend logs for OpenAI API errors
3. Ensure API key has sufficient quota

**Import not creating entries:**
1. Check backend logs:
   ```bash
   docker-compose logs backend | grep -i dynamics
   ```
2. Verify JSON format is correct (starts with `{"value": [...]`)
3. Check if days already have entries (import skips those days)

## Maintenance

### Backing Up Data

```bash
# Backup PostgreSQL data
docker-compose exec postgres pg_dump -U worklog_user worklog_db > backup.sql

# Restore from backup
cat backup.sql | docker-compose exec -T postgres psql -U worklog_user -d worklog_db
```

### Viewing Database

```bash
# Connect to PostgreSQL
docker-compose exec postgres psql -U worklog_user -d worklog_db

# Useful queries
SELECT * FROM projects;
SELECT * FROM worklog_entries ORDER BY entry_date DESC LIMIT 10;
SELECT project_id, SUM(hours) FROM worklog_entries GROUP BY project_id;
```

### Updating Dependencies

**Backend**:
```bash
cd backend
# Update versions in pom.xml
docker-compose build backend
```

**Frontend**:
```bash
cd frontend
npm update
docker-compose build frontend
```

## Security Considerations

- ✅ **JWT Authentication**: Secure token-based authentication with 24-hour expiration
- ✅ **Password Hashing**: BCrypt with strength 12 for secure password storage
- ✅ **Role-Based Access**: USER and ADMIN roles with permission enforcement
- ✅ **Multi-User Isolation**: Users can only access their own data
- ✅ **OpenAI API Key**: Stored in environment variables, never exposed to browser
- ✅ **Backend Proxy**: All AI requests go through backend (API key protected)
- ✅ **CORS**: Configured for specific origins only
- ✅ **Database Security**: Credentials in environment variables, parameterized queries
- ✅ **Input Validation**: All API endpoints validate and sanitize input
- ✅ **SQL Injection Prevention**: Protected via JPA/Hibernate
- ✅ **XSS Prevention**: React escapes content by default
- ✅ **Container Security**: Non-root users in Docker containers

**Production Recommendations:**
- Change default admin password immediately
- Use strong JWT_SECRET (256+ bit random string)
- Enable HTTPS/TLS for API and frontend
- Rotate JWT tokens regularly
- Implement rate limiting for API endpoints
- Regular security updates for dependencies

## License

MIT License - See LICENSE file for details

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

---

Built using React, Spring Boot, and Adobe Spectrum
