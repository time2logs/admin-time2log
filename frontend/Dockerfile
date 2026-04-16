FROM node:25.9.0-alpine AS build

ARG BUILD_CONFIGURATION=production
ARG API_BASE_URL

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci --legacy-peer-deps

COPY . .
RUN npm run build -- --configuration=$BUILD_CONFIGURATION --define "ENV_API_BASE_URL=\"$API_BASE_URL\""

FROM nginx:alpine

RUN rm -rf /usr/share/nginx/html/*

COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
