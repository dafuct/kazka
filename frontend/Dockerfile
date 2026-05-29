FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .

# VITE_* values are read by Vite at build time and baked into the static bundle.
# They MUST be present before `npm run build` — setting them as runtime env on the
# nginx stage does nothing. Coolify passes these as --build-arg (mark them build-time).
ARG VITE_APPLE_WEB_CLIENT_ID
ARG VITE_APPLE_WEB_REDIRECT_URI
ARG VITE_PADDLE_CLIENT_TOKEN
ARG VITE_PADDLE_ENVIRONMENT
ENV VITE_APPLE_WEB_CLIENT_ID=$VITE_APPLE_WEB_CLIENT_ID \
    VITE_APPLE_WEB_REDIRECT_URI=$VITE_APPLE_WEB_REDIRECT_URI \
    VITE_PADDLE_CLIENT_TOKEN=$VITE_PADDLE_CLIENT_TOKEN \
    VITE_PADDLE_ENVIRONMENT=$VITE_PADDLE_ENVIRONMENT

RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
