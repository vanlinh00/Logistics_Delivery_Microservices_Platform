# ==============================================================================
# Multi-Stage Production Dockerfile for Frontend (React + Vite + Tailwind)
# ==============================================================================

# Stage 1: Build static assets
FROM node:20-alpine AS builder

WORKDIR /app

# Cache package dependencies
COPY package.json package-lock.json* bun.lock* ./
RUN npm ci || npm install

# Build static bundle
COPY . .
RUN npm run build

# Stage 2: NGINX Alpine Web Server
FROM nginx:1.25-alpine AS runner

# Remove default nginx configs
RUN rm -rf /etc/nginx/conf.d/* /usr/share/nginx/html/*

# Copy built assets
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy custom production nginx configuration with SPA fallback & security headers
COPY infrastructure/nginx/frontend-nginx.conf /etc/nginx/conf.d/default.conf

# Security: Run as non-root nginx user
USER nginx

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget -q --spider http://localhost:80/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
