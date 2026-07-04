# 42 - Threading

## Purpose
This document defines the threading model for cGallery, ensuring thread safety and UI responsiveness.

## Threading Principles
1.  **UI Thread is for Rendering Only:** No DB access, no file I/O, and no heavy computation on `Dispatchers.Main`.
2.  **Explicit Scopes:** Use `CoroutineScope` tied to lifecycles (ViewModelScope, ProcessLifecycleScope).
3.  **Engine Isolation:** Each engine should have its own designated dispatcher to prevent bottlenecks.

## Dispatcher Mapping

| Work Type | Dispatcher | Description |
| :--- | :--- | :--- |
| **UI Updates** | `Dispatchers.Main.immediate` | Reacting to DB changes, navigation. |
| **DB Operations** | `Dispatchers.IO` (Shared) | Room operations, simple metadata reads. |
| **File I/O** | `Dispatchers.IO` (Dedicated) | Moving files, writing EXIF, generating thumbnails. |
| **ML Inference** | `Dispatchers.Default` | Heavy CPU work (Embeddings, Clustering). |
| **State Orchestration** | `SingleThreadDispatcher` | **Workflow Engine** to ensure serial state transitions. |

## Concurrency Patterns

### 1. The Reactive Flow
```kotlin
// In ViewModel
val mediaList: StateFlow<List<MediaItem>> = repository.observeManagedMedia()
    .flowOn(Dispatchers.IO)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
```

### 2. The Engine Loop
Engines should use a `Channel` or `SharedFlow` to receive tasks:
```kotlin
private val taskChannel = Channel<AnalysisTask>(capacity = Channel.BUFFERED)

suspend fun processQueue() {
    for (task in taskChannel) {
        withContext(Dispatchers.Default) {
            analyze(task)
        }
    }
}
```

### 3. Mutual Exclusion
For critical sections (e.g., file move + DB update), use a `Mutex`:
```kotlin
private val enforcementMutex = Mutex()

suspend fun executeCommand(command: Command) {
    enforcementMutex.withLock {
        // Atomic operation
    }
}
```

## Safety Measures
*   **Avoid GlobalScope:** Always use a supervised scope that can be cancelled.
*   **Timeout:** Every background task must have a `withTimeout` block (e.g., 30s for a single file move).
*   **Cancellation:** Check `isActive` in long-running loops (e.g., while extracting metadata from 1,000 files).

## Error Propagation
*   Exceptions in background threads must be caught and logged via the **Error Recovery** module.
*   Critical failures (DB corruption) should trigger a UI alert.
