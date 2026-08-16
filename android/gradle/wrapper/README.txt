The real gradle-wrapper.jar binary isn't included here (this repo was
authored in a sandbox with no network access to fetch it). Two ways to fix:

1. Run `gradle wrapper --gradle-version 9.7` once, locally or in Android
   Studio, on a machine with Gradle + network — this generates the real
   gradle-wrapper.jar and gradlew.bat and you commit them.
2. Or just don't worry about it: GitHub's gradle/actions/setup-gradle
   action (used in .github/workflows/*.yml) detects the missing/broken
   wrapper and transparently provisions Gradle itself instead — which is
   exactly what's been happening in CI. gradle-wrapper.properties is kept
   in sync with whatever version that resolves to (currently 9.7) so
   local `./gradlew` runs (once you've done step 1) match CI.
