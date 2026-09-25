# DroidMentor

DroidMentor is an Android application developed for ISEL's **Programação em Dispositivos Móveis (PDM)** course. It acts as a BYOK client for Gemini and is designed to provide an Android-development mentor experience while keeping conversation state locally on the device.

## Team

| Member | GitHub | Student number | Email |
|---|---|---|---|
| Miguel Alves | [MAlves4018](https://github.com/MAlves4018) | 51650 | A51650@alunos.isel.pt |
| Martim Gomes | [MarsGomes](https://github.com/MarsGomes) | 51684 | A51684@alunos.isel.pt |

## Project management

Planning and execution are tracked in the linked GitHub Project **DroidMentor — 2026/27 Delivery**.

The Project is created once from the version-controlled definition under `.github/project/` using the manual **Bootstrap GitHub Project** workflow. After setup, the GitHub Project is the operational source of truth for roadmap, ownership, dependencies and task status. Ordinary code pushes do not rebuild or rewrite it.

## Technology baseline

The project uses the course stack and keeps the architecture intentionally small:

- Kotlin
- Jetpack Compose
- `Application` as the manual Service Locator / composition point
- Ktor for HTTP
- Kotlinx Serialization for JSON
- Room for conversation persistence
- DataStore for the user-provided Gemini API key
- Gemini REST `generateContent`

Hilt and Dagger are not used.

## Repository structure

```text
DroidMentor/
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/isel/dei/pdm/droidmentor/
│       │   ├── MainActivity.kt
│       │   ├── DroidMentorApplication.kt
│       │   ├── ui/
│       │   ├── domain/
│       │   ├── data/
│       │   └── platform/
│       └── res/
├── .github/
│   ├── ISSUE_TEMPLATE/
│   └── PULL_REQUEST_TEMPLATE.md
├── docs/
│   └── ARCHITECTURE.md
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
└── gradlew.bat
```
## Development workflow

1. Pick the next coherent task from the GitHub Project.
2. Move it to **In Progress**.
3. Create a focused branch.
4. Implement the task outcome rather than splitting implementation steps into extra issues.
5. Run the verification described in the task.
6. Open a pull request and link the Project item / issue.
7. Review the change with the other team member.
8. Merge and move the task to **Done** only when its stated outcome is actually complete.

A task should represent a result that makes sense to implement, review and test as a unit. Small actions, individual buttons, tiny composables or single test cases normally belong inside the task description/checklist rather than becoming separate tasks.

## Build and run

Open the repository in Android Studio, sync the Gradle project and run the `app` configuration on an emulator or Android device supported by the project.

Use the included Gradle Wrapper when running Gradle from the command line.

## Secrets

Do not commit Gemini API keys, Personal Access Tokens or other credentials. Runtime user configuration belongs in the application and local developer-only configuration belongs in ignored local files.
