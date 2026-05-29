# Built from the REPO ROOT context (see docker-compose.prod.yml: `context: .`), because this is an
# npm-workspaces monorepo: the package-lock.json lives at the repo root and the frontend depends on
# the @kazka/shared workspace package (packages/shared). A ./frontend-only context can see neither,
# which is why `npm ci` failed there.
FROM node:20-alpine AS build
WORKDIR /app

# Workspace manifests + root lockfile first, for layer caching. mobile/package.json is copied only
# so the workspace set matches the lockfile; its dependencies are not installed (`-w frontend`).
COPY package.json package-lock.json ./
COPY packages/shared/package.json ./packages/shared/
COPY frontend/package.json ./frontend/
COPY mobile/package.json ./mobile/
RUN npm ci -w frontend --include-workspace-root

# Root tsconfig files: the frontend and shared tsconfigs extend ../../tsconfig.base.json, which
# tsc/Vite resolve at /app — so the root tsconfigs must be present in the build.
COPY tsconfig.base.json tsconfig.json ./

# Source for the shared package (consumed as TypeScript) and the frontend app.
COPY packages/shared ./packages/shared
COPY frontend ./frontend

# VITE_* values are read by Vite at build time and baked into the static bundle. They MUST be
# present before the build — setting them as runtime env on the nginx stage does nothing. Coolify
# passes these as --build-arg (mark them build-time).
ARG VITE_APPLE_WEB_CLIENT_ID
ARG VITE_APPLE_WEB_REDIRECT_URI
ARG VITE_PADDLE_CLIENT_TOKEN
ARG VITE_PADDLE_ENVIRONMENT
ENV VITE_APPLE_WEB_CLIENT_ID=$VITE_APPLE_WEB_CLIENT_ID \
    VITE_APPLE_WEB_REDIRECT_URI=$VITE_APPLE_WEB_REDIRECT_URI \
    VITE_PADDLE_CLIENT_TOKEN=$VITE_PADDLE_CLIENT_TOKEN \
    VITE_PADDLE_ENVIRONMENT=$VITE_PADDLE_ENVIRONMENT

RUN npm run build -w frontend

FROM nginx:alpine
COPY --from=build /app/frontend/dist /usr/share/nginx/html
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
