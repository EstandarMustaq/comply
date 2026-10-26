# syntax=docker/dockerfile:1

FROM gradle:9.8.2-jdk21-alpine AS api-build
WORKDIR /workspace
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY apps/api/build.gradle.kts ./apps/api/build.gradle.kts
COPY apps/api/src ./apps/api/src
RUN chmod +x gradlew && ./gradlew --no-daemon :api:bootJar

FROM eclipse-temurin:25-jre-alpine AS api
WORKDIR /app
RUN addgroup -S comply && adduser -S comply -G comply \
    && mkdir -p /var/lib/comply/imports \
    && chown -R comply:comply /var/lib/comply
COPY --from=api-build --chown=comply:comply /workspace/apps/api/build/libs/comply-api-*.jar app.jar
USER comply
EXPOSE 8080
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=70.0 -XX:InitialRAMPercentage=40.0 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

FROM node:26.12.1-alpine AS web-base
ENV PNPM_HOME=/pnpm
ENV PATH=$PNPM_HOME:$PATH
RUN npm install --global pnpm@12.8.1
WORKDIR /workspace

FROM web-base AS web-deps
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
COPY apps/ui/package.json ./apps/ui/package.json
COPY packages/api-clients/package.json ./packages/api-clients/package.json
COPY packages/config/eslint-config/package.json ./packages/config/eslint-config/package.json
COPY packages/config/typescript-config/package.json ./packages/config/typescript-config/package.json
COPY packages/ui/package.json ./packages/ui/package.json
RUN pnpm install --frozen-lockfile --filter @comply/web...

FROM web-base AS web-build
COPY --from=web-deps /workspace/node_modules ./node_modules
COPY --from=web-deps /workspace/apps/ui/node_modules ./apps/ui/node_modules
COPY --from=web-deps /workspace/packages/api-clients/node_modules ./packages/api-clients/node_modules
COPY --from=web-deps /workspace/packages/config/eslint-config/node_modules ./packages/config/eslint-config/node_modules
COPY --from=web-deps /workspace/packages/ui/node_modules ./packages/ui/node_modules
COPY . .
ARG NEXT_PUBLIC_API_URL=/api
ARG NEXT_PUBLIC_APP_NAME=Comply
ENV NEXT_PUBLIC_API_URL=$NEXT_PUBLIC_API_URL
ENV NEXT_PUBLIC_APP_NAME=$NEXT_PUBLIC_APP_NAME
ENV NEXT_TELEMETRY_DISABLED=1
ENV NODE_ENV=production
RUN pnpm --filter @comply/web build

FROM node:26.12.1-alpine AS web
WORKDIR /app
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
ENV PORT=3000
ENV HOSTNAME=0.0.0.0
RUN addgroup --system --gid 1001 nodejs && adduser --system --uid 1001 nextjs
COPY --from=web-build --chown=nextjs:nodejs /workspace/apps/ui/.next/standalone/ ./
COPY --from=web-build --chown=nextjs:nodejs /workspace/apps/ui/.next/static ./apps/ui/.next/static
COPY --from=web-build --chown=nextjs:nodejs /workspace/apps/ui/public ./apps/ui/public
USER nextjs
EXPOSE 3000
CMD ["node", "apps/ui/server.js"]
