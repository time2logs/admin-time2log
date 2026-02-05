#!/usr/bin/env bash
set -euo pipefail

ENV="${1:?Usage: deploy.sh <dev|prd>}"

if [[ "$ENV" != "dev" && "$ENV" != "prd" ]]; then
  echo "ERROR: environment must be 'dev' or 'prd'" >&2
  exit 1
fi

REPO_DIR="/opt/admin-time2log"

echo "==> Pulling latest changes …"
cd "$REPO_DIR"
git pull --ff-only

echo "==> Deploying $ENV …"
cd "ops/compose/$ENV"
docker compose pull
docker compose up -d --remove-orphans

echo "==> Deploy $ENV done."
