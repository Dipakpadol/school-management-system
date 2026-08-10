# Production Deployment

This deployment runs:

- Nginx on ports `80` and `443`
- Flutter web at `/`
- Spring Boot at `/api`
- PostgreSQL on an internal Docker network only
- Certbot with Let's Encrypt certificates and renewal

## 1. Prepare DNS

Create an `A` record for your domain pointing to the Ubuntu server public IP.

Example:

```bash
school.example.com  A  203.0.113.10
```

Wait until the DNS resolves:

```bash
getent hosts school.example.com
```

## 2. Install Docker on a Fresh Ubuntu Server

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl git ufw

sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

sudo usermod -aG docker "$USER"
newgrp docker
docker version
docker compose version
```

## 3. Open Firewall Ports

```bash
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw --force enable
sudo ufw status
```

## 4. Upload or Clone the Project

```bash
sudo mkdir -p /opt/school-erp
sudo chown "$USER:$USER" /opt/school-erp

git clone <your-repository-url> /opt/school-erp
cd /opt/school-erp
```

If you are uploading a local copy instead of cloning, put the project contents in `/opt/school-erp`.

## 5. Create Production Environment File

```bash
cp .env.production.example .env.production
nano .env.production
```

Set at least these values:

```dotenv
DOMAIN=school.example.com
WWW_DOMAIN=
NGINX_SERVER_NAME=school.example.com
LETSENCRYPT_EMAIL=admin@example.com

POSTGRES_PASSWORD=<long-random-password>
JWT_SECRET=<long-random-secret>
APP_CORS_ALLOWED_ORIGINS=https://school.example.com

BOOTSTRAP_SUPER_ADMIN_ENABLED=true
BOOTSTRAP_SUPER_ADMIN_EMAIL=admin@school.example.com
BOOTSTRAP_SUPER_ADMIN_PASSWORD=<temporary-admin-password>
```

Generate strong secrets on the server:

```bash
openssl rand -base64 32
openssl rand -base64 48
```

If you use both root and `www` domains:

```dotenv
DOMAIN=school.example.com
WWW_DOMAIN=www.school.example.com
NGINX_SERVER_NAME=school.example.com www.school.example.com
APP_CORS_ALLOWED_ORIGINS=https://school.example.com,https://www.school.example.com
```

## 6. Start HTTP First

```bash
cd /opt/school-erp
docker compose --env-file .env.production -f compose.production.yml up -d --build postgres backend nginx
```

## 7. Get the Initial Let's Encrypt Certificate

Nginx serves the HTTP ACME challenge from `/var/www/certbot`.

```bash
set -a; . ./.env.production; set +a
CERT_DOMAINS=(-d "$DOMAIN")
if [ -n "${WWW_DOMAIN:-}" ]; then CERT_DOMAINS+=(-d "$WWW_DOMAIN"); fi

docker compose --env-file .env.production -f compose.production.yml --profile certbot run --rm certbot \
  certonly \
  --webroot \
  -w /var/www/certbot \
  --agree-tos \
  --no-eff-email \
  --email "$LETSENCRYPT_EMAIL" \
  "${CERT_DOMAINS[@]}"

docker compose --env-file .env.production -f compose.production.yml exec nginx render-nginx-conf
docker compose --env-file .env.production -f compose.production.yml exec nginx nginx -s reload
```

## 8. Build and Start the Stack

```bash
docker compose --env-file .env.production -f compose.production.yml up -d --build
```

Check status:

```bash
docker compose --env-file .env.production -f compose.production.yml ps
docker compose --env-file .env.production -f compose.production.yml logs -f --tail=100 backend
docker compose --env-file .env.production -f compose.production.yml logs -f --tail=100 nginx
```

## 9. Verify the Deployment

```bash
curl -I "https://$DOMAIN/"
curl -fsS "https://$DOMAIN/api/actuator/health"
```

Open:

```text
https://school.example.com/
https://school.example.com/api/swagger-ui.html
```

## 10. Renew SSL Certificates

The `certbot-renew` container checks renewal twice per day. Nginx reloads every 6 hours so renewed certificates are picked up automatically.

Manual renewal test:

```bash
docker compose --env-file .env.production -f compose.production.yml --profile certbot run --rm certbot \
  renew --webroot -w /var/www/certbot --dry-run
```

## 11. Update the App

```bash
cd /opt/school-erp
git pull
docker compose --env-file .env.production -f compose.production.yml up -d --build
docker image prune -f
```

## 12. Backup and Restore PostgreSQL

Backup:

```bash
set -a
. ./.env.production
set +a

docker compose --env-file .env.production -f compose.production.yml exec -T postgres \
  pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB" > "school_erp_$(date +%F).sql"
```

Restore:

```bash
set -a
. ./.env.production
set +a

docker compose --env-file .env.production -f compose.production.yml exec -T postgres \
  psql -U "$POSTGRES_USER" "$POSTGRES_DB" < school_erp_YYYY-MM-DD.sql
```

## 13. Useful Operations

Restart:

```bash
docker compose --env-file .env.production -f compose.production.yml restart
```

Stop:

```bash
docker compose --env-file .env.production -f compose.production.yml down
```

Stop and remove database data and certificates:

```bash
docker compose --env-file .env.production -f compose.production.yml down -v
```
