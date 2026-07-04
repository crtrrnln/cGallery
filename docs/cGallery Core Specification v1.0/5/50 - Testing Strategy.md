# 50 - Testing Strategy

## Purpose
This document outlines the testing approach to ensure the reliability and correctness of cGallery's complex engine interactions.

## 1. Unit Testing (Junit 5 / MockK)
*   **Target:** Business logic in ViewModels, UseCases, and Engines.
*   **Requirement:** 80%+ coverage for the **Workflow Engine** state machine logic.
*   **Mocks:** Mock the Database and File System to test pure logic transitions.

## 2. Integration Testing (Robolectric / Room In-Memory)
*   **Target:** Database DAOs and Repository layer.
*   **Focus:** Ensure SQL queries return the expected results and foreign key constraints behave correctly.
*   **Scenario:** "Ingest 10 files, move them to an album, and verify DB state."

## 3. UI Testing (Compose Test Rule / Espresso)
*   **Target:** User flows and visual states.
*   **Requirement:** 
    *   Test the "Decision Swipe" in the Inbox.
    *   Test navigation between Gallery and Album Detail.
    *   Test "Empty States" rendering.

## 4. Engine Stress Testing (Instrumented Tests)
*   **Target:** Detection Engine and Enforcement Engine.
*   **Scenarios:**
    *   **The "Massive Import":** Inject 1,000 files and monitor memory/CPU.
    *   **The "Permission Revoke":** Revoke file permissions mid-move and verify recovery.
    *   **The "Duplicate Bomb":** Ingest 10 identical files and verify de-duplication logic.

## 5. End-to-End (E2E) Testing
*   **Target:** Real device behavior.
*   **Tools:** Maestro or UI Automator.
*   **Flow:** Cold start -> Ingest Camera photo -> Verify Inbox notification -> Accept Grouping -> Verify File moved on Disk.

## Test Data Management
*   **Mock Assets:** A library of small (100x100) images with specific EXIF headers (Date, GPS, Burst Markers) used for deterministic engine testing.
*   **Corrupt Samples:** Images with malformed headers to test error recovery.

## Continuous Integration (CI)
*   **Lints:** Run `ktlint` and `detekt` on every PR.
*   **Unit Tests:** Run on every commit.
*   **Instrumented Tests:** Run on every merge to `main`.

## Beta Testing (The "Dogfood" Phase)
*   Internal releases to test with real, messy user libraries.
*   Analytics focus: Inference accuracy and crash rates.
