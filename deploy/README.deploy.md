# EC2 Deploy Guide (Nginx -> Spring -> FastAPI -> Chroma)

## 1. Directory layout on EC2
- `/opt/randi/env`
- `/opt/randi/data/chroma/law`
- `/opt/randi/data/chroma/strategy`
- `/opt/randi/data/mysql`
- `/opt/randi/data/redis`
- `/opt/randi/data/modeling/output`
- `/opt/randi/data/modeling/tmp`
- `/opt/randi/frontend/dist`
- `/opt/randi/backup`

Copy example env files from `backend/deploy/env/*.example` to `/opt/randi/env/*.env` and fill real values.

## 2. Frontend build artifact
Run on EC2 (or CI artifact copy):

```bash
cd /path/to/frontend/frontend
npm ci
npm run build
sudo rsync -av --delete dist/ /opt/randi/frontend/dist/
```

## 3. Start DB layer first

```bash
cd /path/to/backend/deploy
docker compose up -d mysql redis chroma_law chroma_strategy
```

## 4. Restore MySQL dump manually
Use dump file from `/opt/randi/backup/<dump>.sql`.

```bash
cd /path/to/backend/deploy
docker compose exec -T mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < /opt/randi/backup/<dump>.sql
```

If the shell does not expand `MYSQL_*` in your context, replace them with literal values.

## 5. Start application layer

```bash
cd /path/to/backend/deploy
docker compose up -d fastapi spring nginx
```

## 6. Health checks
- Nginx: `curl -f http://<EC2_PUBLIC_IP>/healthz`
- Spring: `curl -f http://<EC2_PUBLIC_IP>/api/actuator/health`
- FastAPI via Spring path: execute any Step API from backend path.

## 7. Port exposure policy
- Public: `80` (nginx only)
- Internal only: Spring/FastAPI/Chroma/MySQL/Redis (`expose` only)

## 8. Rollout notes
- TLS is intentionally out of scope in this deployment.
- Restart does not auto-restore DB dump. Restore is manual by design.

## 9. Modeling output retention (recommended)
To prevent disk pressure from generated PPT artifacts, run periodic cleanup.

```bash
sudo chmod +x /opt/randi/src/backend/deploy/cleanup_modeling_output.sh
sudo /opt/randi/src/backend/deploy/cleanup_modeling_output.sh
```

Daily cron (03:30):

```bash
(crontab -l 2>/dev/null; echo "30 3 * * * /opt/randi/src/backend/deploy/cleanup_modeling_output.sh >> /var/log/randi-modeling-cleanup.log 2>&1") | crontab -
```
