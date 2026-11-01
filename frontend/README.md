# Marketline frontend

React, TypeScript, Vite, Tailwind CSS, Axios, and React Router frontend for the Marketplace backend.

## Start locally

```powershell
cd frontend
Copy-Item .env.example .env
npm.cmd install
npm.cmd run dev
```

The development server is available at `http://localhost:5173`.

## API configuration

Set `VITE_API_BASE_URL` in `.env`. The default local backend address is:

```text
VITE_API_BASE_URL=http://localhost:8080
```

The backend must allow the frontend origin through its CORS configuration.
