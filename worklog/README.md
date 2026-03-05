# Worklog - Consultant Time Tracking Application

A full-stack consultant worklog application for tracking daily activities across different projects and clients with AI-powered summaries.

## Features

- **📅 Calendar View** - Visual calendar interface to log daily work summaries
- **🔢 Multiple Entries Per Day** - Create separate entries for different projects on the same day
- **⏱️ Hours Tracking** - Track time spent with decimal precision (0.5, 3.5, 8.0 hours)
- **🎨 Color-Coded Projects** - Visual project indicators with customizable colors
- **🤖 AI Summaries** - Generate weekly/monthly summaries using OpenAI
- **🎨 Adobe Spectrum Design** - Professional UI with Adobe's design system
- **🐳 Docker Deployment** - Complete containerized setup with persistent data
- **🔒 Secure API Key Management** - Environment-based configuration

## Technology Stack

- **Frontend**: React + Vite + Adobe Spectrum Design System
- **Backend**: Java Spring Boot 3.2 + Maven
- **Database**: PostgreSQL 16 with Flyway migrations
- **AI Integration**: OpenAI API (GPT-4o-mini)
- **Containerization**: Docker Compose

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

Edit `.env` and add your OpenAI API key:

```env
POSTGRES_DB=worklog_db
POSTGRES_USER=worklog_user
POSTGRES_PASSWORD=your_secure_password_here

# IMPORTANT: Add your OpenAI API key here
OPENAI_API_KEY=sk-proj-your-actual-api-key-here

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

### 5. Verify Setup

```bash
# Check all containers are running
docker-compose ps

# View logs
docker-compose logs -f

# Check backend health
curl http://localhost:8081/actuator/health

# Check projects endpoint
curl http://localhost:8081/api/projects
```

## Usage Guide

### Managing Projects

1. Navigate to the **Projects** section in the sidebar
2. Click the **+** button to add a new project
3. Enter project name, color code (hex format: #FF5733), and description
4. Projects appear with color indicators for easy identification

**Default Projects** (seeded on first run):
- Enablement (#1473E6) - Internal training and development
- Client A (#E34850) - Client consulting work
- Client B (#44B556) - Client consulting work

### Creating Worklog Entries

1. **From Calendar**: Click on any date in the calendar
   - If the date has no entries, the entry dialog opens
   - If the date has existing entries, the entry list opens
2. **From Button**: Click "Add Entry" button in calendar header
3. Fill in the entry form:
   - **Date**: Auto-filled from calendar selection (can be changed)
   - **Summary**: Brief description (max 255 characters)
   - **Hours**: Time spent (supports decimals: 0.5, 3.5, 8.0)
   - **Project**: Select from dropdown with color indicators
   - **Description**: Detailed work notes
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

1. Click on a date with entries to see the entry list
2. Each entry shows:
   - Project color indicator
   - Summary and hours
   - Detailed description
   - Edit and Delete buttons
3. Click **Edit** to modify an entry
4. Click **Delete** to remove an entry (with confirmation)

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

### Projects API

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/projects` | List all projects |
| GET | `/api/projects/{id}` | Get project by ID |
| POST | `/api/projects` | Create new project |
| PUT | `/api/projects/{id}` | Update project |

### Worklog Entries API

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/entries` | List all entries |
| GET | `/api/entries/{id}` | Get entry by ID |
| GET | `/api/entries/date/{date}` | Get entries for specific date |
| GET | `/api/entries/range?start={date}&end={date}` | Get entries in date range |
| POST | `/api/entries` | Create new entry |
| PUT | `/api/entries/{id}` | Update entry |
| DELETE | `/api/entries/{id}` | Delete entry |

### AI Summary API

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/ai/summary` | Generate AI summary |

**Request Body**:
```json
{
  "dateRangeStart": "2026-03-01",
  "dateRangeEnd": "2026-03-31",
  "projectIds": [1, 2],
  "customPrompt": "Focus on achievements"
}
```

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
- `V3__Add_seed_data.sql` - Seed data

Migrations run automatically on application startup.

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

- ✅ OpenAI API key stored in environment variables (not committed to git)
- ✅ Backend proxies AI requests (API key never exposed to browser)
- ✅ CORS configured for specific origins
- ✅ Database credentials in environment variables
- ✅ Non-root user in Docker containers
- ✅ Input validation on all API endpoints
- ✅ SQL injection prevention via JPA/Hibernate

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
