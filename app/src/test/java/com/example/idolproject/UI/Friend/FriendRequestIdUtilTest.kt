package com.example.idolproject.UI.Friend

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class FriendRequestIdUtilTest {

    @Test
    fun `request id combines sender and receiver uid`() {
        val result = FriendRequestIdUtil.requestId(
            senderUid = "sender123",
            receiverUid = "receiver456"
        )

        assertEquals("sender123_receiver456", result)
    }

    @Test
    fun `request id depends on request direction`() {
        val requestFromAtoB = FriendRequestIdUtil.requestId(
            senderUid = "userA",
            receiverUid = "userB"
        )

        val requestFromBtoA = FriendRequestIdUtil.requestId(
            senderUid = "userB",
            receiverUid = "userA"
        )

        assertNotEquals(requestFromAtoB, requestFromBtoA)
        assertEquals("userA_userB", requestFromAtoB)
        assertEquals("userB_userA", requestFromBtoA)
    }

    @Test
    fun `request id preserves uid text as given`() {
        val result = FriendRequestIdUtil.requestId(
            senderUid = "  sender  ",
            receiverUid = "receiver"
        )

        assertEquals("  sender  _receiver", result)
    }
}