# pfm-insect — rules for Claude

- Android app (`hopper.pfm.insect`), offline-first personal expense manager. Backend is the sibling project `../pfm`.
- **`../pfm` is READ-ONLY.** Never edit, build inside, or write files into it. Read its docs/source for reference only.
- **Git: commit locally only. NEVER `git remote add`, `git push`, or open PRs** unless the user explicitly allows it.
  Identity is set locally (mxtuananh). **No `Co-Authored-By` lines** in commits. No CI/CD for now.
- Design in Penpot (file `pfm-insect`, via `penpot-mcp`), **light mode only**, using Material3 `MaterialTheme` color roles
  so the app adapts to the system theme. **Stop for the user's approval after each module's design before coding it.**
- minSdk 26, targetSdk/compileSdk 37 (revision 1 = platform `android-37.1`).
- Server for dev: see docs/SERVER.md (`scripts/server-up.sh`). Test device: first entry of `adb devices` (physical).
- Gradle may need `--offline` on this machine (corporate TLS interception); check before assuming a network failure.
- Docs and user-facing talk in Vietnamese; code, identifiers and commit messages in English.
