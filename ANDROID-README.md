# Beißindex Android APK

Android-App für https://eko-beratung.github.io/beissindex/.

## APK herunterladen

1. Unter [GitHub Actions](https://github.com/eko-beratung/beissindex/actions) den Workflow **Build Beissindex Android APK** öffnen.
2. Den erfolgreichen Durchlauf im Branch `apk-android` auswählen.
3. Unten auf **Beissindex-Android-APK** klicken, ZIP herunterladen und entpacken.
4. Die Datei **app-debug.apk** auf dem Android-Smartphone installieren.

Hinweis: Nur für Android. Die APK ist eine Debug-Version zum privaten Testen; sie ist noch nicht als Play-Store-Release signiert.

## Android Studio

Den Ordner `android-app` als Projekt öffnen und über **Build > Build APK(s)** bauen.

## Wichtige Details

- App startet die bestehende Online-Webseite; Updates der Webseite sind automatisch sichtbar.
- Erfragt nur bei Verwendung eine Standortberechtigung.
- Import von Fangbuch-Sicherungen über Android-Dateiauswahl.
- CSV- und JSON-Export wird über Androids **Datei speichern**-Dialog abgewickelt.
- Das bestehende Fangbuch im Browser wird **nicht** automatisch ins Android-WebView übertragen. Vorher Browser-Sicherung exportieren und in der APK importieren.
- Internet wird für Live-Wetter und Karten benötigt.
- Die bestehende Webseite im Branch `main` bleibt unverändert.
