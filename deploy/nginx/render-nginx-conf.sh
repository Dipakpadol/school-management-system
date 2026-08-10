#!/bin/sh
set -eu

ssl_cert="/etc/letsencrypt/live/${LETSENCRYPT_DOMAIN}/fullchain.pem"
ssl_key="/etc/letsencrypt/live/${LETSENCRYPT_DOMAIN}/privkey.pem"
template="/etc/nginx/template-candidates/default.http.conf.template"

if [ -s "$ssl_cert" ] && [ -s "$ssl_key" ]; then
    template="/etc/nginx/template-candidates/default.ssl.conf.template"
fi

envsubst '${NGINX_SERVER_NAME} ${LETSENCRYPT_DOMAIN}' \
    < "$template" \
    > /etc/nginx/conf.d/default.conf
