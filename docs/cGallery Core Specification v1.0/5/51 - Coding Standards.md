# 51 - Coding Standards

## Purpose
To maintain a high-quality, maintainable, and consistent codebase across the cGallery project.

## Language: Kotlin
*   Follow the [Official Kotlin Style Guide](https://kotlinlang.org/docs/coding-conventions.html).
*   Use **KMP (Kotlin Multiplatform)** ready structures where possible (e.g., separating logic from Android-specific APIs).

## Architecture: Clean Architecture + MVI
*   **Layers:**
    *   `data`: DAOs, Repositories, File System implementation.
    *   `domain`: UseCases, State Machines, Domain Models (Pure Kotlin).
    *   `ui`: ViewModels, Compose Screens, UI State.
*   **MVI (Model-View-Intent):** Unidirectional data flow. UI sends `Intent`, ViewModel updates `State`.

## Dependency Injection (DI)
*   Use **Hilt** (Dagger) for DI.
*   Scopes: `@Singleton` for Engines, `@ViewModelScoped` for UI logic.

## Asynchrony
*   Prefer **Coroutines** and **Flow** over RxJava or Callbacks.
*   Never use `Thread.sleep()`.
*   Always specify dispatchers in the `constructor` for testability (e.g., `private val ioDispatcher: CoroutineDispatcher`).

## Error Handling
*   Use `Result<T>` or a custom `sealed class Either<L, R>` for expected failures.
*   Throw exceptions only for "unrecoverable" programmer errors.
*   Use `Timber` for logging with clear tags.

## Naming Conventions
*   **Interfaces:** `MediaManager`, not `IMediaManager`.
*   **Implementations:** `PhysicalMediaManager` or `RoomMediaManager`.
*   **UI Components:** `GalleryScreen`, `AlbumThumbnail`.
*   **Variables:** Use `camelCase`. Constants use `SCREAMING_SNAKE_CASE`.

## Compose Specifics
*   Keep Composables small and focused.
*   Pass only the required data, not entire ViewModels.
*   Use `@Preview` for every UI component.

## Documentation
*   **KDoc:** Required for all `public` methods and classes in the `domain` and `data` layers.
*   **Comments:** Explain "Why," not "What."

## Performance Guardrails
*   No nested `LazyRows` inside `LazyColumns` without fixed heights.
*   Minimize use of `State` objects in hot loops; use `derivedStateOf`.
