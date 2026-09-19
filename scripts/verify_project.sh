#!/usr/bin/env bash
set -euo pipefail
[[ -f app/src/main/AndroidManifest.xml ]]
rg -q 'android.intent.category.HOME' app/src/main/AndroidManifest.xml
rg -q 'compileSdk = 36' app/build.gradle.kts
rg -q 'targetSdk = 36' app/build.gradle.kts
rg -q 'AppWidgetHost' app/src/main/java/com/fluidhome/MainActivity.java
rg -q 'postInvalidateOnAnimation' app/src/main/java/com/fluidhome/FluidWorkspace.java
rg -q 'android-actions/setup-android@v4' .github/workflows/android.yml
rg -q "cmdline-tools-version: '13114758'" .github/workflows/android.yml
rg -q 'platforms;android-36' .github/workflows/android.yml
rg -q 'build-tools;36.0.0' .github/workflows/android.yml
rg -q "gradle-version: '8.11.1'" .github/workflows/android.yml
rg -q 'name: fluid-home-debug-apk' .github/workflows/android.yml
rg -q 'workflow_dispatch:' .github/workflows/android.yml
rg -q 'pull_request:' .github/workflows/android.yml
rg -q 'push:' .github/workflows/android.yml
git diff --check
