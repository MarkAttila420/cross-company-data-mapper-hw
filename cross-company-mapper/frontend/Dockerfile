# Frontend Dockerfile (Vite + React + TypeScript)
FROM node:18-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM node:18-alpine
WORKDIR /app
COPY --from=build /app/dist ./dist
EXPOSE 3000
CMD ["npx", "vite", "preview", "--port", "3000", "--host"]
