# Claude skills for CarHome

Five of Google's official **Android Skills**, copied from
[github.com/android/skills](https://github.com/android/skills) (commit `7f7b217`, 24 Nov 2026).
Claude Code loads skills from this folder in every session on this repository.

| Skill | Use it for |
|---|---|
| `jetpack-compose-performance` | Smooth home screens on the box's weak hardware (recomposition, dropped frames) |
| `anr-audit-and-fix` | A launcher must never freeze: find and fix ANR risks |
| `android-intent-security` | Exported activities, services and receivers; incoming Intents |
| `android-permissions-security` | Permissions and runtime permission flows |
| `testing-setup` | Unit, UI and screenshot tests |

**CarHome rules still come first** (see the project overview): no third-party libraries beyond
AndroidX and Kotlin coroutines unless Ahmed agrees, so don't add testing or DI libraries
(Hilt, Roborazzi, etc.) suggested by a skill without asking.

**Updating:** copy the same folders again from the latest `android/skills`, or run
`android skills update --all` with the Android CLI.

**License:** Apache License 2.0, Google LLC. Full text in `LICENSE.txt`. The skill files are unchanged.
