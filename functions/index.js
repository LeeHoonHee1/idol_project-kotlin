const {onDocumentCreated} = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();

exports.sendChatNotification = onDocumentCreated(
  "group_chats/{groupId}/messages/{messageId}",
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) return;

    const messageData = snapshot.data();
    const groupId = event.params.groupId;

    const senderUid = messageData.senderUid || "";
    const senderName = messageData.senderName || "알 수 없음";
    const text = messageData.message || "";
    const roomName = `${groupId} 오픈채팅`;

    const db = admin.firestore();

    try {
      const membersSnap = await db
        .collection("group_chats")
        .doc(groupId)
        .collection("members")
        .get();

      const memberUids = membersSnap.docs
        .map((doc) => doc.id)
        .filter((uid) => uid && uid !== senderUid);

      if (memberUids.length === 0) {
        console.log("보낼 대상 없음");
        return;
      }

      const userDocs = await Promise.all(
        memberUids.map((uid) => db.collection("users").doc(uid).get())
      );

      const tokens = userDocs
        .map((doc) => doc.get("fcmToken"))
        .filter((token) => typeof token === "string" && token.length > 0);

      if (tokens.length === 0) {
        console.log("유효한 토큰 없음");
        return;
      }

      const sendPromises = tokens.map((token) =>
        admin.messaging().send({
          token,
          data: {
            title: roomName,
            body: `${senderName}: ${text}`,
            groupId: groupId,
            roomName: roomName,
            senderUid: senderUid,
          },
          android: {
            priority: "high",
          },
        })
      );

      const results = await Promise.allSettled(sendPromises);

      results.forEach((result, index) => {
        if (result.status === "rejected") {
          console.error(`전송 실패 token[${index}]`, result.reason);
        }
      });

      console.log(`푸시 전송 완료: ${tokens.length}건`);
    } catch (error) {
      console.error("sendChatNotification error", error);
    }
  }
);