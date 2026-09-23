# 🚀 SMS Forwarder (Bridge) - Mandatory Release & Signing Protocol

> **CRITICAL INSTRUCTION FOR ALL DEVELOPERS & AI AGENTS**:
> Read and follow this protocol before releasing any update to ensure users never experience the "App not installed" error.

---

## ⚠️ Mandatory Release Rules (Zero-Tolerance Checklist)

### 1. NEVER Output or Upload an Unsigned APK
- **Issue**: Android package installer **refuses** to install unsigned APKs (`app-release-unsigned.apk`) and displays `"App not installed"`.
- **Rule**: In `app/build.gradle.kts`, the `release` build type **MUST ALWAYS** have `signingConfig` defined. When `my-upload-key.jks` is not present, it must fall back to `signingConfigs.getByName("debug")`:
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
- **Check**: Ensure the file generated in `app\build\outputs\apk\release` is **`app-release.apk`**, **NEVER** `app-release-unsigned.apk`!

---

### 2. Mandatory Certificate Signature Verification Before Uploading
Before uploading the APK to GitHub, **ALWAYS** run the signature verification command:
```powershell
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0\apksigner.bat" verify --verbose --print-certs app\build\outputs\apk\release\app-release.apk
```

**Required output:**
```
Verifies
Verified using v2 scheme (APK Signature Scheme v2): true
Number of signers: 1
Signer #1 certificate DN: C=US, O=Android, CN=Android Debug
Signer #1 certificate SHA-256 digest: 3ddef8eea188f5abfff9e8958842483703c05a196253d4e172836f41a1ddee1f
```
The certificate SHA-256 **MUST MATCH** `3ddef8eea188f5abfff9e8958842483703c05a196253d4e172836f41a1ddee1f`. If the certificate does not match, existing installations cannot be updated!

---

### 3. Only Attach the Single Official Signed Release APK to GitHub Releases
- **Rule**: Only attach `SMS-Forwarder-vX.Y.Z.apk` to the GitHub release.
- **NEVER** attach `-debug.apk` alongside it, so the in-app updater always downloads the official signed release APK.

---

## 📋 Full Release Execution Sequence

```powershell
# 1. Bump version in app/build.gradle.kts (versionCode + 1, versionName)

# 2. Compile release APK
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleRelease --no-daemon

# 3. Verify signature
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0\apksigner.bat" verify --verbose --print-certs app\build\outputs\apk\release\app-release.apk

# 4. Copy to root release artifact
Copy-Item app\build\outputs\apk\release\app-release.apk -Destination SMS-Forwarder-vX.Y.Z.apk

# 5. Commit and push to Git
git add app/build.gradle.kts app/src/
git commit -m "release: bump version to X.Y.Z"
git push origin main

# 6. Publish GitHub release (Single APK asset only)
gh release create vX.Y.Z SMS-Forwarder-vX.Y.Z.apk --title "vX.Y.Z - <Title>" --notes "<Changelog>"

# 7. Verify live API response
Invoke-RestMethod -Uri "https://api.github.com/repos/SUBHOJITPAUL797/SMS-FORWARDER/releases/latest" -Headers @{"User-Agent"="SMS-Forwarder-Update-Checker"} | Select-Object tag_name, name
```
