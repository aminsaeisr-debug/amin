# seed-mas 4.1.2 — Build & Validation Notes

## Toolchain

- Android Gradle Plugin: 9.4.1
- Gradle Wrapper distribution: 9.6.1
- JDK: 17
- compileSdk: 36
- minSdk: 26
- targetSdk: 35
- applicationId: com.srooyesh.seedcounter
- versionCode: 12
- versionName: 4.1.2

## GitHub Actions

The workflow is `.github/workflows/build.yml`. It supports `workflow_dispatch` with `release` (default) or `debug`. Release requires the production signing keystore secrets so an update can be installed over the existing production app. The workflow validates the package/version, verifies release signing, writes one APK to a flat `release/` directory, and uploads one APK artifact named `seed-mas-apk`.

## Checks completed in the authoring environment

- All Android resource XML files parse successfully.
- All Kotlin `R.string` references resolve to declared strings.
- All Kotlin `R.id` references resolve to declared layout IDs.
- No `.bat`, `.cmd`, or `.ps1` helper scripts are included.
- Exactly one base `values/styles.xml` exists; `values-v35/styles.xml` is a legitimate API-qualified override.
- The XLSX relationships XML generator contains exactly one XML declaration.
- OCR candidate extraction tests pass for irregular 2/3/4/mixed digit groups and Persian/Arabic digit normalization.
- Smart OCR preserves recognized separators for the visible result while storage remains canonical digits-only.

## Environment limitation

A full Gradle build could not be executed in this authoring environment because outbound DNS access to Gradle distribution/dependency hosts is unavailable. This is an environment limitation, not a claim that the project was locally built. GitHub Actions is the authoritative clean-network build path for the repository.
