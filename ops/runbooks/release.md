# Release Runbook

---

## Release erstellen

1. Sicherstellen, dass alles committed ist
```
git status
```
---
2. Erstellen eines annotierten Tags
```
git tag -a v0.1.1 -m "Release v0.1.1"
```
---
3. Release veröffentlichen (Tag pushen)
```
git push origin v0.1.1
```
---