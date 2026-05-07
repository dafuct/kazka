# Kazka Deployment

This is the production deployment checklist. Local dev uses `docker compose up` with values from `.env`; production reads secrets from your hosting platform's secret store, never from a checked-in file.

## Pre-deploy checklist

Before pushing to any production environment, make sure you have:

- [ ] A registered domain (e.g. `kazka.app`).
- [ ] DNS pointed at your host.
- [ ] HTTPS — Let's Encrypt via your host, or a managed cert.
- [ ] A transactional email provider with your domain verified (Resend / Mailgun / SES / Postmark).
- [ ] A separate Google Cloud OAuth client for prod, with `https://<your-domain>/login/oauth2/code/google` in **Authorized redirect URIs** and a non-localhost JavaScript origin.
- [ ] A managed MySQL 8 (your host's, RDS, PlanetScale, etc.) — not the docker-compose one.
- [ ] A managed Redis (your host's, Upstash, ElastiCache, etc.).
- [ ] A way to run the schema migration once on the production DB (manually pipe `schema.sql` or temporarily set `spring.sql.init.mode=always` for one boot).
- [ ] An admin email + strong password chosen.

## Production env vars

These are the values to set in your platform's secret store. Never commit them.

| Variable | Dev value | Prod value |
|---|---|---|
| `APP_BASE_URL` | `http://localhost` | `https://your-domain.app` |
| `COOKIE_SECURE` | `false` | `true` |
| `SPRING_PROFILES_ACTIVE` | (unset) | `prod` (or unset; default works) |
| `DB_URL` | local MySQL | managed MySQL JDBC URL |
| `DB_USER` / `DB_PASS` | `kazkar/kazkar` | strong, rotated, set in secret store |
| `SPRING_DATA_REDIS_HOST` | `redis` | managed Redis host |
| `SPRING_DATA_REDIS_PORT` | `6379` | managed Redis port (often `6380` for TLS) |
| `MAIL_HOST` | `smtp.gmail.com` | `smtp.resend.com` (or chosen provider) |
| `MAIL_USERNAME` | your Gmail | `resend` (or provider's auth user) |
| `MAIL_PASSWORD` | Gmail app password | provider API key |
| `MAIL_FROM` | your Gmail | `noreply@your-domain.app` |
| `GOOGLE_CLIENT_ID` | dev OAuth client | prod OAuth client |
| `GOOGLE_CLIENT_SECRET` | dev OAuth secret | prod OAuth secret |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | optional | strong, rotated, set in secret store |
| `HUGGINGFACE_API_TOKEN` | dev token | prod token (separate or quota-tracked) |

## Fly.io — recommended start

Fly's CLI handles deploy + secrets + Postgres + Redis with the lowest friction.

### One-time setup

```bash
brew install flyctl
fly auth signup    # or: fly auth login
cd /path/to/kazka
fly launch         # generates fly.toml; pick app name = your domain prefix; skip Postgres/Redis prompts (we'll add them ourselves)
```

When asked about deploy now, say **No** — we have to set secrets first.

### Provision MySQL + Redis on Fly

Fly provides Postgres directly; for MySQL the easiest path is a managed external (PlanetScale free tier or any cloud DB). Redis is best via **Upstash** (Fly's marketplace integration) or your own managed Redis.

```bash
# Upstash Redis — one-line provisioning, returns connection URL
fly redis create --name kazka-redis

# MySQL — easiest is PlanetScale (free tier), gives you a JDBC-compatible URL
# Sign up at https://planetscale.com → create database → Connect → "Java/JDBC" → copy URL
```

### Set production secrets

```bash
fly secrets set \
  APP_BASE_URL=https://your-domain.app \
  COOKIE_SECURE=true \
  DB_URL='jdbc:mysql://aws.connect.psdb.cloud/kazka?sslMode=VERIFY_IDENTITY' \
  DB_USER='your-planetscale-user' \
  DB_PASS='pscale_pw_...' \
  SPRING_DATA_REDIS_HOST='your-redis-host.upstash.io' \
  SPRING_DATA_REDIS_PORT='6380' \
  MAIL_HOST=smtp.resend.com \
  MAIL_PORT=587 \
  MAIL_USERNAME=resend \
  MAIL_PASSWORD='re_LIVE_API_KEY' \
  MAIL_FROM='noreply@your-domain.app' \
  GOOGLE_CLIENT_ID='your-prod-oauth-client.apps.googleusercontent.com' \
  GOOGLE_CLIENT_SECRET='GOCSPX-...' \
  ADMIN_EMAIL=admin@your-domain.app \
  ADMIN_PASSWORD='strong-rotated-pass' \
  HUGGINGFACE_API_TOKEN='hf_...'
```

### Run the schema once

```bash
fly ssh console -a kazka-backend
# inside the container, run schema.sql against the prod DB:
mysql -h <db-host> -u <user> -p<password> kazka < /app/schema.sql
exit
```

Or, simpler: temporarily set `SPRING_SQL_INIT_MODE=always`, deploy, watch logs to confirm tables created, then unset:

```bash
fly secrets set SPRING_SQL_INIT_MODE=always
fly deploy
fly logs    # confirm "Executed SQL script ..."
fly secrets unset SPRING_SQL_INIT_MODE
fly deploy
```

### Deploy

```bash
fly deploy
fly logs -a kazka-backend
```

Visit `https://your-domain.app`, sign up, verify, sign in. If anything fails, `fly logs` shows the exact error.

### Custom domain + HTTPS

```bash
fly certs add your-domain.app
# Add the DNS records Fly shows (an A or CNAME) at your registrar.
fly certs check your-domain.app    # poll until "Issued" appears
```

## Render — dashboard-driven alternative

If you prefer a UI:

1. New → Web Service → connect GitHub repo.
2. Environment: Docker → Dockerfile path: `backend/Dockerfile`.
3. **Environment variables** tab → add every row from the table above as a "Secret" (encrypted).
4. Add a Render Postgres or external MySQL + Redis; paste their connection URLs in.
5. Deploy.
6. Repeat for the frontend (Static Site → `frontend/`, build command `npm run build`, publish dir `dist`).

## Bare VPS (Hetzner / DigitalOcean / Linode)

Cheapest, more manual.

```bash
# On the VPS, as root:
mkdir -p /etc/kazka
nano /etc/kazka/secrets.env       # paste prod env vars (no quotes, KEY=VALUE per line)
chown root:root /etc/kazka/secrets.env
chmod 0600 /etc/kazka/secrets.env

# Clone the repo somewhere root-owned (don't put secrets in there):
git clone https://github.com/.../kazka.git /opt/kazka
cd /opt/kazka

# Run docker compose loading the env file from /etc/kazka/secrets.env:
docker compose --env-file /etc/kazka/secrets.env up -d
```

Front this with **Caddy** or **Nginx** for HTTPS termination (Caddy is one config file and gets Let's Encrypt automatically).

`/etc/caddy/Caddyfile`:
```
your-domain.app {
    reverse_proxy localhost:80
}
```

`systemctl reload caddy`. Done.

## Running schema migrations between deploys

The project uses `spring.sql.init` with `schema.sql` — it does NOT run automatically (`mode: never` in `application.yml`). You must opt-in for the first boot:

```bash
SPRING_SQL_INIT_MODE=always docker compose up -d
# verify tables created
SPRING_SQL_INIT_MODE=never docker compose up -d   # default; just `docker compose up -d`
```

For schema changes after launch, generate idempotent ALTER scripts and run them manually against the DB. (Adopting Flyway is a future option if migrations get complex — currently out of scope.)

## Going-live smoke test

After deploy, run through these in a real browser:

- [ ] `https://your-domain.app` loads.
- [ ] Sign up with a fresh address → verification email arrives → click link → "Email verified".
- [ ] Sign in with that account → user dropdown appears.
- [ ] Sign in with Google → redirected to Google → consent → land back on `/?auth=ok` cleanly.
- [ ] Create a story → succeeds.
- [ ] Sign out → user dropdown becomes Sign in / Sign up.
- [ ] As another browser/incognito window, sign in as the seeded admin → "Admin → Users" link appears.
- [ ] `/admin/users` table renders.
- [ ] Forgot password → email arrives → reset works.
- [ ] All cookies have `Secure` flag and `SameSite=Lax`.
- [ ] HTTPS only — `http://` redirects.
- [ ] No mixed-content warnings in browser console.

## Operational checklist (after launch)

- [ ] Monitor `docker logs` / `fly logs` for `WARN`/`ERROR` daily.
- [ ] Watch transactional-email dashboard (Resend) for bounce rate < 1% and complaint rate < 0.1%.
- [ ] Back up the MySQL database daily (`mysqldump` to S3 or your provider's automated backups).
- [ ] Rotate `ADMIN_PASSWORD`, `MAIL_PASSWORD`, `GOOGLE_CLIENT_SECRET` quarterly or on suspected leak.
- [ ] If `users` or `stories` tables grow large (> 1M rows), check the indexes still serve common queries (`EXPLAIN ANALYZE` on the slow query log).

## Out of scope for v1, follow-ups for later

- Rate limiting on `/api/auth/*` (currently spec-deferred).
- Per-user account deletion endpoint.
- 2FA / TOTP for admin accounts.
- Email-enumeration mitigation beyond the existing 204-on-everything pattern.
- Centralised log aggregation (Loki / Grafana / Datadog).
- Error tracking (Sentry / Honeybadger).
