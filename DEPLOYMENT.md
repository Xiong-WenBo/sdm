# Deployment Notes

## Backend environment variables

Set these variables before starting the backend service:

```bash
DB_URL=jdbc:mysql://localhost:3306/sdm?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
DB_USERNAME=root
DB_PASSWORD=your_mysql_password
JWT_SECRET=replace-with-a-long-random-secret-at-least-32-chars
JWT_EXPIRATION=86400000
```

## Frontend environment variables

Create `frontend/.env.local` locally or inject the variable in CI/CD:

```bash
VITE_API_BASE_URL=http://localhost:8080
```

## Recommended release checklist

- Do not use `root` as the production database user
- Replace the default JWT secret before deployment
- Keep `.env` and `.env.local` out of git
- Build the frontend with the correct API base URL
- Re-run backend tests and frontend build before release
