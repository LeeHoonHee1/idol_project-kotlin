package com.example.idolproject

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object UserSession {

    private const val PREF = "user_session"
    private const val KEY_NICK = "nickname"
    private const val KEY_ROLE = "role"
    private const val KEY_POINTS = "points_total"

    suspend fun syncFromFirestore(context: Context): Boolean {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: return false

        return runCatching {
            val doc = FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .await()

            if (!doc.exists()) {
                return false
            }

            val nick = doc.getString("nickname") ?: ""
            val role = doc.getString("role") ?: "user"
            val points = doc.getLong("points_total") ?: 0L

            val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            sp.edit()
                .putString(KEY_NICK, nick)
                .putString(KEY_ROLE, role)
                .putLong(KEY_POINTS, points)
                .apply()

            true
        }.getOrElse {
            false
        }
    }

    fun nickname(context: Context): String =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY_NICK, "") ?: ""

    fun role(context: Context): String =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY_ROLE, "user") ?: "user"

    fun pointsTotal(context: Context): Long =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getLong(KEY_POINTS, 0L)

    fun clear(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}