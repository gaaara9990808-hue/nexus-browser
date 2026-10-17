# NEXUS Browser — Android

Native Android Kotlin WebView browser. This is source code, not a compiled or device-tested APK.

## Implemented
- URL/search address bar, navigation, progress, multiple tabs
- Bookmark list (local SharedPreferences)
- Android DownloadManager downloads
- Share URL, desktop user-agent, clear cache/cookies, HTML file chooser
- External HTTP/HTTPS link handling
- TLS errors rejected; no insecure file access

## Not implemented
- Private tabs, advanced content blocking, full browsing history UI, Room, sophisticated session restore, video fullscreen, permission prompts for camera/microphone.

## Build without a PC
Upload contents to a GitHub repository with branch main, open Actions -> Build NEXUS APK -> Run workflow. Download artifact `NEXUS-Browser-APK`, extract and install `app-debug.apk`. Android may ask to allow installation from your file manager.

Requires Android SDK 35 and Java 17 in GitHub Actions. Google WebView updates are supplied by Android.

No build or device test was performed in this environment.
