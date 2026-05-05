# ================================================================
# Multi-stage build for the React frontend.
#
# Stage 1 (build): Node.js builds the production bundle with Vite.
# Stage 2 (run):   Nginx serves the static files — no Node needed.
#
# Why Nginx and not "npm run preview"?
# Vite's preview server is for local testing only — not production-grade.
# Nginx handles gzip, caching headers, and the SPA fallback properly.
# ================================================================

# ── Stage 1: Build ────────────────────────────────────────────
FROM node:20-alpine AS build

WORKDIR /app

# Copy package files first — same caching trick as the backend.
# If package.json hasn't changed, npm ci is skipped on next build.
COPY package*.json ./

# "npm ci" (clean install) is preferred over "npm install" in CI/CD:
#   - Installs exact versions from package-lock.json (reproducible)
#   - Fails if lock file is out of sync with package.json
#   - Faster than npm install for fresh installs
RUN npm ci

COPY . .

# Build the production bundle — output goes to /app/dist
RUN npm run build

# ── Stage 2: Serve ────────────────────────────────────────────
FROM nginx:1.25-alpine

# Copy the built static files into Nginx's serve directory
COPY --from=build /app/dist /usr/share/nginx/html

# Copy our custom Nginx config (handles SPA routing + API proxy)
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
