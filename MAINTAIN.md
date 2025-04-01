# Maintaining Quad9 Connect

## Release

When preparing a new release, make sure to bump version in `app/build.gradle` and update `metadata/changelogs` directory before tagging. When everything is ready, build and sign APKs:

```
./gradlew clean
./gradlew assembleFdroidRelease
./gradlew assembleGoogleplayRelease
# apksigner from Android SDK build tools
apksigner sign --ks ... --ks-key-alias ... --ks-pass pass:... --key-pass pass:...  --in app/build/outputs/apk/fdroid/release/quad9-aegis-fdroid-release-unsigned.apk --out quad9-aegis-fdroid-release.apk --alignment-preserved
apksigner sign --ks ... --ks-key-alias ... --ks-pass pass:... --key-pass pass:...  --in app/build/outputs/apk/googleplay/release/quad9-aegis-googleplay-release-unsigned.apk --out quad9-aegis-googleplay-release.apk --alignment-preserved
```

Create a new release on GitHub and attach signed APKs. F-Droid should automatically be updated based on this release.
