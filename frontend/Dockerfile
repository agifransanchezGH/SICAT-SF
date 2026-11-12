# Imagen de una app Angular. Uso:
#   docker build --build-arg APP=admin -t sicat-front-admin frontend/
# En tiempo de ejecución, API_UPSTREAM indica a qué API se reenvía /api (p. ej. http://api-admin:8081).
FROM node:22-alpine AS build
ARG APP=admin
WORKDIR /src
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npx ng build ${APP} --configuration production

FROM nginx:1.27-alpine
ARG APP=admin
ENV API_UPSTREAM=http://localhost:8081
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /src/dist/${APP}/browser /usr/share/nginx/html
