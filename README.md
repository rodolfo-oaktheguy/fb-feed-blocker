# Feed Blocker

Kicks you out of Facebook and FB Lite when you keep scrolling the feed or stay too long,
then locks them for a cooldown. Settings are in the app.

## Get the APK without Android Studio (free, ~5 min)
1. Make a GitHub account, then create a new repository (any name, Public or Private).
2. On the repo page: "uploading an existing file" → drag in EVERYTHING from this folder,
   including the hidden `.github` folder (on Windows, unzip first; if `.github` doesn't show,
   create the file `.github/workflows/build.yml` by hand with "Add file → Create new file"
   and paste its contents). Commit.
3. Open the Actions tab → "Build APK" → wait for the green check (~3 min).
4. Open that run → Artifacts → FeedBlocker-apk → download, unzip → app-debug.apk.
5. Send the APK to your phone, open it, allow "Install unknown apps".
6. Open Feed Blocker → turn it on in Accessibility. If greyed out (Android 13+):
   Settings → Apps → Feed Blocker → ⋮ → Allow restricted settings, then try again.
