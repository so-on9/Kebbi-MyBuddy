# Kebbi / MyBuddy Android App

Android application for the Nuwa Kebbi robot. The app provides account login,
face recognition, guided conversation topics, OpenAI Responses API interaction
through a PHP proxy, chat history, profiles, and achievements.

## Local setup

1. Copy the relevant entries from `local.properties.example` into the local
   `local.properties` file.
2. Set `KEBBI_API_BASE_URL` for Debug builds.
3. Set `KEBBI_RELEASE_API_BASE_URL` to an HTTPS endpoint before building a
   Release APK.
4. Open the project in Android Studio and build the `app` module.

Production credentials belong on the PHP server. Do not place OpenAI keys,
database passwords, signing keys, Firebase files, or production credentials in
this repository.

## Build

```powershell
.\gradlew.bat :app:assembleDebug
```

The Nuwa SDK AAR files under `app/libs` are required by the robot integration.
Confirm that their redistribution is permitted before making the repository
public.
