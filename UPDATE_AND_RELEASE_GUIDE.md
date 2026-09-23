# 🚀 SMS Forwarder (Bridge) - Update, Release & Architecture Guide

This documentation serves as the **mandatory release standard** for building, signing, verifying, and releasing updates for **SMS Forwarder (Bridge)**.

---

## ⚠️ MANDATORY RELEASE RULES (MUST READ BEFORE EVERY RELEASE)

### 1. NEVER Release an Unsigned APK (`app-release-unsigned.apk`)
Android package installer will **REJECT** unsigned APKs with `App not installed` (`INSTALL_PARSE_FAILED_NO_CERTIFICATES`).
In `app/build.gradle.kts`, `release` build type **MUST ALWAYS** have `signingConfig` configured (falling back to `signingConfigs.getByName("debug")` if an external keystore is omitted).
The output MUST be `app-release.apk`, **NEVER** `app-release-unsigned.apk`!

### 2. MANDATORY Certificate Signature Verification
Before uploading any APK to GitHub, you **MUST** run:
```powershell
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0\apksigner.bat" verify --verbose --print-certs app\build\outputs\apk\release\app-release.apk
```
**Verify that:**
- `Verifies` is `true`
- `Verified using v2 scheme` is `true`
- `Signer #1 certificate SHA-256 digest` matches the installed app's signature:
  `3ddef8eea188f5abfff9e8958842483703c05a196253d4e172836f41a1ddee1f`

### 3. NEVER Attach Debug APKs to GitHub Releases
Only upload the single, official signed Release APK (`SMS-Forwarder-vX.Y.Z.apk`) to GitHub Releases. Never upload `-debug.apk` alongside it, so the in-app updater always downloads the official signed release APK.

---

## 📌 Standard Step-by-Step Release Pipeline

Follow these exact steps for every new update:

### 🔹 Step 1: Bump Version in `app/build.gradle.kts`
```kotlin
android {
    defaultConfig {
        versionCode = 29          // Increment integer by 1
        versionName = "1.0.28"    // Increment semantic version
    }
}
```

### 🔹 Step 2: Ensure Signing is Configured in `app/build.gradle.kts`
```kotlin
  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      if (file(keystorePath).exists()) {
        signingConfig = signingConfigs.getByName("release")
      } else {
        signingConfig = signingConfigs.getByName("debug")
      }
    }
  }
```

### 🔹 Step 3: Compile the Release APK
```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleRelease --no-daemon
```
Verify that Gradle generated `app\build\outputs\apk\release\app-release.apk` (and **NOT** `app-release-unsigned.apk`).

### 🔹 Step 4: Verify Signature with `apksigner`
```powershell
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0\apksigner.bat" verify --verbose --print-certs app\build\outputs\apk\release\app-release.apk
```
Ensure output displays `Verifies` and `Signer #1 certificate SHA-256 digest: 3ddef8eea188f5abfff9e8958842483703c05a196253d4e172836f41a1ddee1f`.

### 🔹 Step 5: Copy & Commit to Git
```powershell
Copy-Item app\build\outputs\apk\release\app-release.apk -Destination SMS-Forwarder-vX.Y.Z.apk
git add app/build.gradle.kts app/src/
git commit -m "release: vX.Y.Z - <description>"
git push origin main
```

### 🔹 Step 6: Publish GitHub Release
```powershell
gh release create vX.Y.Z SMS-Forwarder-vX.Y.Z.apk --title "vX.Y.Z - <Title>" --notes "<Changelog>"
```

### 🔹 Step 7: Verify Live GitHub Release API
```powershell
Invoke-RestMethod -Uri "https://api.github.com/repos/SUBHOJITPAUL797/SMS-FORWARDER/releases/latest" -Headers @{"User-Agent"="SMS-Forwarder-Update-Checker"} | Select-Object tag_name, name
```

---

## 2. In-App GitHub Auto-Updater Architecture

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant App as SMS Forwarder App
    participant GH as GitHub Releases API
    participant Installer as Android Package Installer

    App->>GH: GET /repos/SUBHOJITPAUL797/SMS-FORWARDER/releases/latest
    GH-->>App: Return latest release (tag_name, changelog, .apk asset)
    App->>App: Compare version (Current vs Latest)
    
    alt Newer version found
        App->>User: Display InAppUpdateDialog with Changelog & Size
        User->>App: Clicks "Update Now"
        App->>GH: Stream download APK (with live % progress)
        App->>Installer: Trigger FileProvider intent (ACTION_VIEW)
        Installer->>User: Displays "Do you want to update this app?"
        User->>Installer: Taps "Install / Update"
        Installer->>App: App updated successfully!
    else Version is up-to-date
        App-->>User: "You are using the latest version"
    end
```

### Key Updater Features:
1. **Automatic Startup Check**: Whenever the app opens, it silently checks for updates in the background.
2. **Manual Check**: Users can tap the **Update Icon** in the top bar of both Host & Client screens.
3. **In-App Direct Download**: Streams the APK bytes with a live progress bar, downloaded MB / total MB indicator, and non-blocking coroutines.
4. **Smart Release Prioritization**: Automatically filters out debug APKs and selects official release APKs.
5. **Automatic System Install Trigger**: Once the APK is downloaded into cache, Android's `FileProvider` (`REQUEST_INSTALL_PACKAGES`) triggers the package installer.
