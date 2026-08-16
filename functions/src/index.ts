import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();

const db = admin.firestore();
const messaging = admin.messaging();

/**
 * Cloud Function triggered whenever a new SMS document is created under
 * `sms/{hostUid}/messages/{messageId}`.
 *
 * It looks up the Host's FCM device token from `users/{hostUid}` and delivers
 * an immediate high-priority data & notification push payload.
 */
export const onNewSmsForward = functions.firestore
  .document("sms/{hostUid}/messages/{messageId}")
  .onCreate(async (snapshot, context) => {
    const hostUid = context.params.hostUid;
    const messageId = context.params.messageId;
    const smsData = snapshot.data();

    if (!smsData) {
      console.error(`Empty snapshot data for message ${messageId}`);
      return null;
    }

    const sender = smsData.sender || "SMS Forwarder";
    const body = smsData.body || "";
    const receivedAt = String(smsData.receivedAt || Date.now());
    const clientDeviceName = smsData.clientDeviceName || "Client Phone";

    console.log(`Processing SMS from ${sender} for Host UID: ${hostUid}`);

    try {
      // 1. Fetch Host user profile
      const hostUserDoc = await db.collection("users").doc(hostUid).get();
      if (!hostUserDoc.exists) {
        console.error(`Host user ${hostUid} document does not exist.`);
        return null;
      }

      const hostData = hostUserDoc.data();
      const fcmToken = hostData?.fcmToken;

      if (!fcmToken) {
        console.warn(`No FCM token registered for Host user ${hostUid}`);
        return null;
      }

      // 2. Build High-Priority FCM Payload
      const message: admin.messaging.Message = {
        token: fcmToken,
        notification: {
          title: `📩 ${sender}`,
          body: body.length > 100 ? `${body.substring(0, 97)}...` : body,
        },
        data: {
          type: "new_sms",
          msgId: messageId,
          sender: sender,
          body: body,
          receivedAt: receivedAt,
          clientDeviceName: clientDeviceName,
        },
        android: {
          priority: "high",
          notification: {
            channelId: "sms_notifications",
            sound: "default",
            priority: "max",
            defaultVibrateTimings: true,
            visibility: "public",
          },
        },
      };

      // 3. Send message via Firebase Admin SDK
      const response = await messaging.send(message);
      console.log(`Successfully sent FCM notification for SMS ${messageId}: ${response}`);
      return response;
    } catch (error) {
      console.error(`Failed to deliver FCM notification for SMS ${messageId}:`, error);
      return null;
    }
  });
