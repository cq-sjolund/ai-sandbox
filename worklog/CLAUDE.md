# Consultant Worklog Application

## Project Overview

A modern timesheet/worklog tracking application built for consultants to manage their time entries, projects, and generate AI-powered summaries. Supports importing time entries from Microsoft Dynamics 365.

## Tech Stack

### Backend
- **Framework**: Spring Boot 3.x (Java 17+)
- **Database**: PostgreSQL 16
- **Security**: Spring Security with JWT authentication
- **AI Integration**: OpenAI API (GPT-4 models)
- **Build Tool**: Maven
- **ORM**: JPA/Hibernate with Flyway migrations

### Frontend
- **Framework**: React 18 with Vite
- **UI Library**: Adobe React Spectrum
- **State Management**: React Context API
- **HTTP Client**: Axios
- **Date Handling**: @internationalized/date
- **Routing**: React Router v6

### Infrastructure
- **Containerization**: Docker & Docker Compose
- **Development Port**:
  - Frontend: http://localhost:3001
  - Backend API: http://localhost:8081
  - PostgreSQL: localhost:5433

## Architecture

### Backend Structure
```
backend/src/main/java/com/consultant/worklog/
├── config/          # Spring configuration (Security, CORS)
├── controller/      # REST API endpoints
├── dto/            # Data Transfer Objects
├── model/          # JPA entities
├── repository/     # Spring Data repositories
├── security/       # JWT utilities, filters, user details
└── service/        # Business logic layer
```

### Frontend Structure
```
frontend/src/
├── api/            # API client configuration
├── components/     # React components by feature
│   ├── Admin/      # User management (admin only)
│   ├── AI/         # AI summary panel
│   ├── Auth/       # Login page
│   ├── Calendar/   # Calendar and list views
│   ├── Dashboard/  # Main dashboard layout
│   ├── Dynamics/   # Dynamics 365 import
│   ├── Entry/      # Time entry dialogs
│   ├── Projects/   # Project management
│   └── Routes/     # Route guards
└── contexts/       # React Context providers
```

## Key Features

### Core Functionality
- **Time Entry Management**: Create, edit, delete time entries with date, hours, summary, description
- **Project Management**: Create projects with colors (AI-suggested), track entries per project
- **Calendar Views**: Both calendar grid and list view with month-based pagination
- **User Authentication**: JWT-based auth with role-based access (USER, ADMIN)
- **Multi-user Support**: Each user has private entries and projects

### AI-Powered Features
- **Summary Generation**: Generate period summaries from multiple entries with custom prompts
- **Project Color Suggestions**: AI suggests appropriate colors for new projects
- **Auto-complete**: Description completion suggestions based on context
- **Ask AI**: Query your worklog data with natural language questions

### Microsoft Dynamics 365 Integration
- **Manual Import**: Copy-paste JSON data from Dynamics browser console
- **AI Project Mapping**: Intelligently maps Dynamics projects to existing worklog projects
- **Duplicate Prevention**: Skips importing days that already have entries
- **Smart Naming**: Keeps more descriptive project names when merging
- **Import Flow**: input → analyze with AI → confirm mappings → import

## Development Workflow

### Starting the Application
```bash
cd /Users/sjolund/projects/ai-sandbox/worklog
docker-compose up --build
```

### Database Migrations
- Flyway migrations in `backend/src/main/resources/db/migration/`
- Naming: `V{number}__{description}.sql` (e.g., `V1__Initial_schema.sql`)
- Auto-run on application start

### Rebuilding After Changes
```bash
# Backend changes (Java)
docker-compose down
docker-compose up --build backend

# Frontend changes (React)
docker-compose restart frontend

# Full rebuild
docker-compose down
docker-compose up --build
```

### Database Access
```bash
# Connect to PostgreSQL
docker-compose exec postgres psql -U worklog_user -d worklog_db

# View all entries
docker-compose exec postgres psql -U worklog_user -d worklog_db -c "SELECT * FROM worklog_entries;"
```

## Important Conventions

### Code Style
- **Java**: Follow Spring Boot conventions, use Lombok annotations
- **React**: Functional components with hooks, Adobe Spectrum components
- **Naming**: camelCase for Java methods, kebab-case for CSS/files
- **No Emojis**: Avoid emojis in code unless explicitly requested

### API Patterns
- **REST Endpoints**: `/api/{resource}` (e.g., `/api/entries`, `/api/projects`)
- **Authentication**: `Authorization: Bearer {jwt_token}` header
- **Response Format**: JSON with consistent structure
- **Error Handling**: Return appropriate HTTP status codes

### Frontend Patterns
- **Context Providers**: WorklogContext, ProjectContext, AuthContext
- **API Client**: Centralized in `src/api/client.js` with interceptors
- **Protected Routes**: Use ProtectedRoute and AdminRoute components
- **Date Format**: ISO 8601 (YYYY-MM-DD) for consistency

### Database Conventions
- **Primary Keys**: `id BIGSERIAL PRIMARY KEY`
- **Timestamps**: `created_at`, `updated_at` (auto-managed by JPA)
- **Foreign Keys**: Named `{table}_id` (e.g., `project_id`, `user_id`)
- **Indexes**: Add for foreign keys and frequently queried columns

## Environment Variables

### Backend (.env or docker-compose.yml)
```
POSTGRES_DB=worklog_db
POSTGRES_USER=worklog_user
POSTGRES_PASSWORD=worklog_password
JWT_SECRET=your-256-bit-secret-key
OPENAI_API_KEY=sk-...
OPENAI_MODEL=gpt-4-turbo-preview
```

### Frontend (.env)
```
VITE_API_BASE_URL=http://localhost:8081/api
```

## User Roles & Permissions

### USER Role
- Create, read, update, delete own time entries
- Create, read, update, delete own projects
- Use all AI features
- Import from Dynamics

### ADMIN Role
- All USER permissions
- View all users
- Create new users
- Delete users (cascade deletes their data)

### Default Admin Account
- Username: `admin`
- Password: `admin` (change in production!)
- Created by migration `V4__Create_users_table.sql`

## Common Tasks

### Adding a New Feature
1. **Backend**: Create model → repository → service → controller → DTO
2. **Frontend**: Create API method → context/hook → component
3. **Database**: Add migration if schema changes needed
4. **Testing**: Restart services and test end-to-end

### Adding an AI Feature
1. Add method to `OpenAIService.java`
2. Create endpoint in `AIController.java`
3. Add API method to `aiAPI` in `client.js`
4. Create/update React component
5. Configure prompt and parameters (temperature, max_tokens)

### Debugging
- **Backend Logs**: `docker-compose logs -f backend`
- **Frontend Logs**: Browser console (F12)
- **Database**: Connect via psql (see Database Access above)
- **API Requests**: Check browser Network tab or backend logs

### Importing from Dynamics 365
1. User clicks "Import from Dynamics" button
2. Opens dialog with JavaScript fetch script
3. User runs script in Dynamics browser console
4. Copies JSON output and pastes in dialog
5. Backend analyzes with AI and suggests project mappings
6. User confirms/adjusts mappings
7. Backend imports entries, avoiding duplicate days

## Security Notes

- **JWT Tokens**: Stored in `localStorage`, expires after 24 hours
- **Password Hashing**: BCrypt with strength 12
- **CORS**: Configured to allow frontend origin
- **SQL Injection**: Protected by JPA parameterized queries
- **XSS**: React escapes content by default
- **Authentication**: All `/api/**` endpoints require valid JWT (except `/api/auth/**`)

## Known Limitations

- No entry editing while offline (requires backend)
- Dynamics import is manual (copy-paste JSON) - no direct API integration
- AI features require OpenAI API key and usage quota
- Single timezone support (dates stored as LocalDate without timezone)
- Import skips entire days (not individual entries) if any entry exists for that day

## Future Enhancements

- Export to Excel/PDF
- Recurring time entries
- Time entry templates
- Mobile app
- Offline mode with sync
- Calendar integrations
- Bulk edit operations
- Advanced reporting and analytics

## Troubleshooting

### Frontend not updating after code changes
```bash
docker-compose restart frontend
# Or for persistent issues:
docker-compose down
docker-compose up --build frontend
```

### Database schema out of sync
```bash
# Check migration status in logs
docker-compose logs backend | grep Flyway

# Reset database (WARNING: deletes all data)
docker-compose down -v
docker-compose up --build
```

### "401 Unauthorized" errors
- Check JWT token exists in localStorage
- Verify token hasn't expired (24h lifetime)
- Check backend logs for authentication errors
- Try logging out and back in

### AI features not working
- Verify `OPENAI_API_KEY` is set correctly
- Check backend logs for OpenAI API errors
- Verify API key has sufficient quota
- Check if using correct model name

## Getting Help

- Check backend logs: `docker-compose logs -f backend`
- Check database: Connect with psql (see Database Access)
- Review Flyway migrations: `backend/src/main/resources/db/migration/`
- Check API routes: All controllers in `backend/src/main/java/com/consultant/worklog/controller/`
