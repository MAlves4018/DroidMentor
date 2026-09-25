# Architecture

DroidMentor intentionally starts with a single Gradle module (`:app`). The project is small enough that separation through clear Kotlin packages is easier to understand and maintain than premature multi-module architecture.

## Package responsibilities

```text
isel.dei.pdm.droidmentor
├── ui/        Compose screens, reusable UI components, UI state and ViewModels
├── domain/    Product models and behaviour that should not depend on Android UI
├── data/      Room, DataStore, Gemini/Ktor and repository implementations
└── platform/  Android-specific services such as connectivity or file access
```

More specific packages should be added only when the corresponding code exists, for example:

```text
ui/chat/
ui/history/
data/local/
data/remote/gemini/
```

The package structure is a guide, not a requirement to create empty architectural layers.

## Main dependency direction

The intended flow is deliberately simple:

```text
Compose screen
    ↓ events / state
ViewModel
    ↓
Repository or service contract
    ↓
Room / DataStore / Gemini implementation
```

UI composables should not know about Room entities, HTTP DTOs or API-key storage details.

## Application composition

`DroidMentorApplication` is the composition point required by the course. As implementation grows, it will own/create the small set of shared services the application needs and make them available to the Android presentation layer without Hilt or Dagger.

Do not create abstractions only for symmetry. Introduce an interface or additional layer when it gives a concrete benefit such as testability, replacing a data source, or isolating provider/database details.

## UI components

Reusable design components belong in the UI layer when they are actually reused. Examples may include headers, message bubbles, buttons, loading/error surfaces and a message composer.

Feature-specific components should remain close to the feature unless they become genuinely transversal. A single button or composable is normally part of a larger screen task, not an independent project-management task.

## Persistence

Room is responsible for persisted conversations/messages. DataStore is responsible for the small amount of user configuration required by the application, including the BYOK Gemini key.

The first implementation should prefer the minimum schema and repository surface necessary for the required product behaviour. Migrations and additional abstractions should be introduced when a real schema change or requirement creates that need.

## Gemini integration

Gemini integration should remain understandable:

```text
Active Chat
  → ViewModel
  → Gemini service/repository
  → Ktor + Kotlinx Serialization
  → generateContent
```

The application owns conversation state. Each request builds the required local conversation context and the Android mentor `system_instruction`. Connectivity and external failures should result in controlled UI state rather than crashes.

## Testing principle

Tests should prove behaviour, not architecture for its own sake. Examples:

- a screen renders the expected state and emits the expected action;
- an empty message cannot be submitted;
- the Gemini request contains the expected conversation history;
- offline state prevents a remote request;
- Room can save, list and reopen a conversation;
- editing an old message produces the required conversation rewrite behaviour.

Additional tests should be added when a real bug, risk or requirement justifies them.
