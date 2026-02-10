# Rollback Runbook

## Links
- [admin-time2log/frontend](https://github.com/time2logs/admin-time2log/pkgs/container/admin-time2log%2Ffrontend)

- [admin-time2log/backend](https://github.com/time2logs/admin-time2log/pkgs/container/admin-time2log%2Fbackend)
## Ziel
Rollback auf eine vorherige, stabile Version der Services via Docker Compose.

---

## Rollback durchführen

### 1. Auf den Server einloggen
```
ssh root@46.225.107.173
```

---

### 2. In das Produktionsverzeichnis wechseln
```
cd /opt/admin-time2log/ops/compose/prd
```
---

### 3. Version prüfen oder anpassen
```
nano docker-compose.yml
```
Version anpassen, falls nötig, z.B. auf eine vorherige Version:

bsp.

`image: ghcr.io/time2logs/admin-time2log/backend:latest`

zu

`image: ghcr.io/time2logs/admin-time2log/backend:v0.1.0`


---
### 4. Images neu ziehen
```
docker-compose -f docker-compose.yml pull
```
---
### 5. Services neu starten
```
docker-compose -f docker-compose.yml up -d
```

