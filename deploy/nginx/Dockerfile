# syntax=docker/dockerfile:1.7

FROM debian:bookworm-slim AS flutter-build

ARG FLUTTER_CHANNEL=stable
ARG API_BASE_URL=/api

ENV FLUTTER_ROOT=/opt/flutter
ENV PATH="${FLUTTER_ROOT}/bin:${FLUTTER_ROOT}/bin/cache/dart-sdk/bin:${PATH}"

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        ca-certificates \
        curl \
        git \
        libglu1-mesa \
        unzip \
        xz-utils \
        zip \
    && rm -rf /var/lib/apt/lists/*

RUN git clone --depth 1 --branch "${FLUTTER_CHANNEL}" https://github.com/flutter/flutter.git "${FLUTTER_ROOT}" \
    && git config --global --add safe.directory "${FLUTTER_ROOT}" \
    && flutter config --enable-web \
    && flutter precache --web

WORKDIR /app

COPY frontend/pubspec.yaml frontend/pubspec.lock ./
RUN flutter pub get

COPY frontend/ ./
RUN flutter build web --release --dart-define=API_BASE_URL="${API_BASE_URL}"

FROM nginx:stable-alpine

COPY deploy/nginx/default.http.conf.template /etc/nginx/template-candidates/default.http.conf.template
COPY deploy/nginx/default.ssl.conf.template /etc/nginx/template-candidates/default.ssl.conf.template
COPY deploy/nginx/render-nginx-conf.sh /usr/local/bin/render-nginx-conf
COPY --from=flutter-build /app/build/web /usr/share/nginx/html
