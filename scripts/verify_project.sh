#!/usr/bin/env bash
set -euo pipefail
[[ -f app/src/main/AndroidManifest.xml ]]
rg -q 'android.intent.category.HOME' app/src/main/AndroidManifest.xml
rg -q 'compileSdk = 36' app/build.gradle.kts
rg -q 'AppWidgetHost' app/src/main/java/com/fluidhome/MainActivity.java
rg -q 'postInvalidateOnAnimation' app/src/main/java/com/fluidhome/FluidWorkspace.java
rg -q "gradle-version: '8.11.1'" .github/workflows/android.yml
rg -q 'name: fluid-home-debug-apk' .github/workflows/android.yml
git diff --check
