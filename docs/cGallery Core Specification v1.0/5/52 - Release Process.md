# 52 - Release Process

## Purpose
This document defines the steps to take a feature from development to the hands of users, ensuring stability at every stage.

## 1. Versioning
*   Follow [Semantic Versioning 2.0.0](https://semver.org/).
*   **Format:** `MAJOR.MINOR.PATCH` (e.g., 1.2.0).
*   Version Code: Incrementing integer for Play Store.

## 2. Release Channels
*   **Debug:** Daily builds for developers. Full logging enabled.
*   **Alpha (Internal):** Continuous integration builds for the core team.
*   **Beta (Public):** Stable features for early adopters. Analytics and crash reporting active.
*   **Production:** The final stable release for all users.

## 3. The Release Checklist
Before any production release:
1.  **Regression Test:** Run full suite of unit and instrumented tests.
2.  **Performance Audit:** Verify that "Time to First Image" hasn't regressed.
3.  **Migration Test:** Verify that DB migrations from the previous version work without data loss.
4.  **Translation Check:** Ensure all UI strings are localized (if applicable).
5.  **Security Scan:** Check for vulnerable dependencies.

## 4. Deployment Pipeline (GitHub Actions)
1.  **Build:** `gradle assembleRelease`.
2.  **Sign:** Apply the production keystore.
3.  **Obfuscate:** Run `R8` for code shrinking and optimization.
4.  **Artifacts:** Store the APK/AAB and mapping files.
5.  **Upload:** Deploy to Google Play Console (Internal/Beta/Production).

## 5. Post-Release Monitoring
*   **Crash Rate:** Must be < 0.1% for production status.
*   **User Feedback:** Monitor store reviews for "Engine Errors" or "Missing Photos."
*   **Hotfix Policy:** If a critical bug (Data Loss) is found, a `PATCH` release must be issued within 24 hours.

## 6. Communication
*   **Changelog:** Automatically generated from conventional commits.
*   **Release Notes:** User-friendly summary of the "Brain's" new capabilities.
