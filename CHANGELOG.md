# Changelog

## 4.1.2

- Fixed missing runtime string resources that blocked Android resource compilation.
- Fixed duplicate XML declaration in XLSX workbook relationships.
- Excel metadata now follows the actual app version.
- Improved safe replacement of generated Excel/backup files.
- GitHub Actions now uses current artifact upload action and stronger APK validation.

## 4.1.1

- Added SIMPLE / SMART number scan modes.
- SMART mode joins irregular digit groups and recognizes supported spaces, commas and separators.
- SMART results can display the recognized separators while saving canonical digits for validation and storage.
- Preserved `applicationId` and database schema version for in-place upgrades.
- Updated build toolchain to AGP 9.4.1 and Gradle 9.6.1.
- Updated GitHub Actions to current checkout/setup-java/setup-gradle actions.
- Release workflow now requires explicit production signing secrets and never silently substitutes Debug for Release.
- Flat APK artifact output with a single APK file.
