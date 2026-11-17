# Dev server (from `../pfm`, read-only)

`docker compose up --build` inside `../pfm` fails on this machine (the image build cannot trust the corporate TLS
certificate), so `scripts/server-up.sh` does this instead, without touching `../pfm`:

1. `git archive HEAD` of `../pfm` into `.server/src` (git-ignored copy), built with `gradlew --offline installDist`.
2. A private PostgreSQL 18 container `pfm-insect-db` (UTF-8, `127.0.0.1:15433`, volume `pfm-insect-db`).
   Random DB password and JWT secret are generated once into `.server/local.env` (git-ignored).
3. Runs the server with `java -cp "lib/*" io.ktor.server.netty.EngineMain` on port 8080
   (the generated `pfm.bat` fails on Windows with "input line is too long").
4. `adb reverse tcp:8080 tcp:8080` so the physical device reaches it at `http://localhost:8080`.

Check: `curl localhost:8080/health` -> `{"status":"ok",...}`; Swagger UI `http://localhost:8080/openapi`.

## Verified (2026-11-17)
- Flyway migrated to V9; `/health` ok; `POST /v1/auth/register` -> 201; `POST /v1/auth/login` -> tokens.
- OpenAPI 3.1.1 snapshot saved in `docs/api/openapi.yaml` (refresh with the script when pfm changes).
- Test device: Samsung SM-G991N, Android 15 (API 35).
