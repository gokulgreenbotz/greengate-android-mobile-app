# GreenGate (Android resident app)

Jetpack Compose resident app, package `com.greenbotz.greengate`. Signs in with SMS OTP against the onboarding service (`app-web-ui-client-onboarding`, `docs/RESIDENT_APP_API.md`) and receives FCM push notifications from the bms-admin Notify popup.

## Commands

Gradle needs JDK 17+. If the default JVM is older, set `JAVA_HOME` to Android Studio's bundled `jbr`.

```bash
./gradlew installDebug   # build and install on the connected emulator or device
```

## Guardrails

1. **Never commit `app/google-services.json`.** It is the Firebase client config for project `greengate-dc9f7` (API key, app ids). It is gitignored. Don't `git add -f` it, don't remove the ignore rule, and don't paste its contents into code, docs, issues or chat. Each developer copies it from the Firebase console (Project settings, Your apps) into `app/`.
2. **Never commit other secrets:** Firebase service-account JSON, `keystore`/`*.jks`, `local.properties`, signing passwords, and API keys for Twilio, Vonage or any other provider. Service-account keys belong to the backend only, never to this app.
3. **Don't hard-code credentials in source.** `SharedCallConfig.kt` already carries call-provider values. Don't add more; move them to untracked config instead.
4. **Resident data is personal data (PDPA).** Don't log phone numbers, names, tokens or FCM tokens (`Log.*`, crash reports).
5. **Cleartext HTTP is allowed only for `10.0.2.2`** (the Local dev server), through `network_security_config.xml`. Don't widen it.
