# Gujarati Calendar (ગુજરાતી કેલેન્ડર)

Package id is already com.rahul.gujaraticalendar. To change it, find-replace it in
app/build.gradle.kts and in every .kt file, and rename the folder app/src/main/java/com/rahul.

Push to GitHub -> Actions -> Build -> download artifact "gujaraticalendar" (.apk for testing, .aab for Play Store).
Signing for Play Store: same secrets as before (KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD).
