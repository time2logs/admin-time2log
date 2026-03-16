FROM node:25.8.0-alpine AS build

ARG BUILD_CONFIGURATION=production

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build -- --configuration=$BUILD_CONFIGURATION

FROM nginx:alpine

RUN rm -rf /usr/share/nginx/html/*

COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/templates/default.conf.template

EXPOSE ${PORT}
CMD ["nginx", "-g", "daemon off;"]
