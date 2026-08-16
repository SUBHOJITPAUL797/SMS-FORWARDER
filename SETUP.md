# SMS Bridge — Setup & Deployment Guide

This guide provides step-by-step instructions for deploying and configuring the **SMS Bridge** application and its Firebase backend.

---

## 1. Firebase Project Setup

1. Go to the [Firebase Console](https://console.firebase.google.com/) and click **Add Project**. Name it `sms-bridge` (or your preferred name).
2. **Add Android App**:
   - Package name: `com.aistudio.smsbridge.vpmq`
   - Download the generated `google-services.json` file.
   - Place `google-services.json` inside the `app/` directory of the project.
3. **Enable Firebase Authentication**:
   - In the Firebase Console, go to **Build > Authentication > Sign-in method**.
   - Enable **Email/Password**.
4. **Enable Cloud Firestore**:
   - Go to **Build > Firestore Database > Create database**.
   - Select your preferred region (e.g., `nam5 (us-central)`).
   - Start in **Production mode**.
5. **Enable Cloud Messaging (FCM)**:
   - Go to **Project Settings > Cloud Messaging**.
   - Ensure the Firebase Cloud Messaging API (V1) is enabled.

---

## 2. Cloud Firestore Security Rules

Deploy the following security rules to Cloud Firestore:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Users Collection: users can read/write their own profile
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }

    // Pairings: Temporary 6-character handshake documents
    match /pairings/{pairingCode} {
      allow read, create, update: if request.auth != null;
      allow delete: if request.auth != null;
    }

    // Links: Active device pairing records
    match /links/{linkId} {
      allow read, write: if request.auth != null &&
        (request.auth.uid == resource.data.hostUid || request.auth.uid == resource.data.clientUid ||
         request.auth.uid == request.resource.data.hostUid || request.auth.uid == request.resource.data.clientUid);
    }

    // SMS Messages: Host can read/update, Client can write
    match /sms/{hostUid}/messages/{messageId} {
      allow read, update: if request.auth != null && request.auth.uid == hostUid;
      allow create: if request.auth != null;
    }
  }
}
```

---

## 3. Cloud Functions Deployment (Automatic FCM Delivery)

The Cloud Function watches for new documents created in `sms/{hostUid}/messages/{messageId}` and triggers instant FCM pushes to the Host.

### Deployment Steps:
1. Ensure you have the Firebase CLI installed:
   ```bash
   npm install -g firebase-tools
   ```
2. Log in to Firebase:
   ```bash
   firebase login
   ```
3. Initialize and deploy from the `functions/` folder:
   ```bash
   cd functions
   npm install
   firebase deploy --only functions
   ```

---

## 4. Building and Installing the Android APK

1. Build the release or debug APK:
   ```bash
   gradle assembleDebug
   ```
2. Install the **exact same APK** on both devices:
   - **Device A (Host phone)**: The phone where you want to receive notifications and view messages.
   - **Device B (Client phone)**: The phone that receives the physical SMS via SIM card.

---

## 5. Pairing Flow

1. **Log in or Register**:
   - On **Client Device**, log in with your account. Select **"I am the CLIENT"**.
   - On **Host Device**, log in with your account (or the same shared account). Select **"I am the HOST"**.
2. **Link via 6-Digit Code**:
   - The Client phone displays a unique 6-character code (e.g. `K9X2B4`) with a 10-minute timer.
   - On the Host phone, enter this code and tap **"Pair Device"**.
   - Both devices immediately link in Firestore and transition to their active home dashboards.

---

## 6. Client Phone Reliability & OEM Battery Optimization

To ensure zero-miss delivery when the Client screen is off:
- In the app, tap **"Fix Permissions & Exempt Battery"**.
- Grant **Receive SMS**, **Read SMS**, and **Notifications**.
- Allow **Ignore Battery Optimization** (`Don't optimize`).
- On devices with aggressive background app killers (Xiaomi MIUI/HyperOS, Samsung OneUI, Huawei, Oppo/Realme):
  - **Auto-start**: Allow app to auto-start.
  - **App lock**: Lock the app in the recent apps task switcher.
  - **Background activity**: Set to "No restrictions".
