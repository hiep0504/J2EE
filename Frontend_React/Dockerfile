FROM node:22-alpine AS builder
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .

ARG VITE_API_BASE_URL=/api
ARG VITE_WS_URL=/ws-chat
ARG VITE_GOOGLE_CLIENT_ID=

ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
ENV VITE_WS_URL=${VITE_WS_URL}
ENV VITE_GOOGLE_CLIENT_ID=${VITE_GOOGLE_CLIENT_ID}

RUN npm run build

FROM nginx:1.27-alpine

# SPA fallback + reverse proxy to backend.
RUN printf "server {\n  listen 80;\n  server_name _;\n\n  root /usr/share/nginx/html;\n  index index.html;\n\n  location / {\n    try_files \\$uri \\$uri/ /index.html;\n  }\n\n  location /api/ {\n    proxy_pass http://backend:8080/api/;\n    proxy_set_header Host \\$host;\n    proxy_set_header X-Real-IP \\$remote_addr;\n    proxy_set_header X-Forwarded-For \\$proxy_add_x_forwarded_for;\n    proxy_set_header X-Forwarded-Proto \\$scheme;\n  }\n\n  location /ws-chat {\n    proxy_pass http://backend:8080/ws-chat;\n    proxy_http_version 1.1;\n    proxy_set_header Upgrade \\$http_upgrade;\n    proxy_set_header Connection \"upgrade\";\n    proxy_set_header Host \\$host;\n    proxy_set_header X-Forwarded-For \\$proxy_add_x_forwarded_for;\n    proxy_set_header X-Forwarded-Proto \\$scheme;\n  }\n}\n" > /etc/nginx/conf.d/default.conf

COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
