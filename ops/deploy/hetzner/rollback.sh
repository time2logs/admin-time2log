#!/usr/bin/env bash
set -euo pipefail

ENV="${1:?Usage: rollback.sh <dev|prd> <target>  (target = commit SHA for dev, version tag for prd)}"
TARGET="${2:?Usage: rollback.sh <dev|prd> <target>  (target = commit SHA for dev, version tag for prd)}"

if [[ "$ENV" != "dev" && "$ENV" != "prd" ]]; then
  echo "ERROR: environment must be 'dev' or 'prd'" >&2
  exit 1
fi

REPO_DIR="/opt/admin-time2log"
REGISTRY="ghcr.io/time2logs/admin-time2log"

if [[ "$ENV" == "dev" ]]; then
  SRC_TAG="dev-${TARGET}"
  DST_TAG="dev-latest"
else
  SRC_TAG="${TARGET}"
  DST_TAG="latest"
fi

echo "==> Rolling back $ENV to $SRC_TAG …"

echo "==> Pulling images …"
docker pull "${REGISTRY}/backend:${SRC_TAG}"
docker pull "${REGISTRY}/frontend:${SRC_TAG}"

echo "==> Re-tagging as $DST_TAG …"
docker tag "${REGISTRY}/backend:${SRC_TAG}" "${REGISTRY}/backend:${DST_TAG}"
docker tag "${REGISTRY}/frontend:${SRC_TAG}" "${REGISTRY}/frontend:${DST_TAG}"

echo "==> Restarting containers …"
cd "${REPO_DIR}/ops/compose/${ENV}"
docker compose up -d --remove-orphans

echo "==> Rollback $ENV to $SRC_TAG done."
