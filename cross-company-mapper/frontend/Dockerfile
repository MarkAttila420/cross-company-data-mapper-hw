### Multi-stage Dockerfile for React (Create React App)
# Stage 1 - build the React app
FROM node:18 AS build

# Set working directory
WORKDIR /app

# Copy package manifests first for better caching
COPY package*.json ./

# Install dependencies
# Use `npm install` in the image to avoid CI failing when package-lock.json
# and package.json are out-of-sync. For reproducible builds, regenerate and
# commit an up-to-date package-lock.json locally and switch back to `npm ci`.
RUN npm install --no-audit --no-fund --prefer-offline --no-progress

# Copy the rest of the source
COPY . .

# Build the app
RUN npm run build


# Stage 2 - serve with nginx
FROM nginx:alpine

# Remove default nginx HTML if present
RUN rm -rf /usr/share/nginx/html/*

# Copy built static files from previous stage
COPY --from=build /app/build /usr/share/nginx/html

# Expose port 80
EXPOSE 80

# Run nginx in foreground
CMD ["nginx", "-g", "daemon off;"]
