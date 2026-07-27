package com.example.idolproject.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.idolproject.data.local.UserSessionData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserSessionRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    val sessionFlow: Flow<UserSessionData> = dataStore.data
        .catch {
            emit(androidx.datastore.preferences.core.emptyPreferences())
        }
        .map { prefs ->
            UserSessionData(
                uid = prefs[KEY_UID].orEmpty(),
                nickname = prefs[KEY_NICKNAME].orEmpty(),
                role = prefs[KEY_ROLE] ?: DEFAULT_ROLE,
                pointsTotal = prefs[KEY_POINTS_TOTAL] ?: 0L
            )
        }

    suspend fun getSessionOnce(): UserSessionData {
        return sessionFlow.first()
    }

    suspend fun syncFromFirestore(): Boolean {
        val uid = auth.currentUser?.uid
            ?: return false

        return runCatching {
            val doc = db.collection("users")
                .document(uid)
                .get()
                .await()

            if (!doc.exists()) {
                return false
            }

            val nickname = doc.getString("nickname").orEmpty()
            val role = doc.getString("role") ?: DEFAULT_ROLE
            val pointsTotal = doc.getLong("points_total") ?: 0L

            saveSession(
                UserSessionData(
                    uid = uid,
                    nickname = nickname,
                    role = role,
                    pointsTotal = pointsTotal
                )
            )

            true
        }.getOrElse {
            false
        }
    }

    suspend fun saveSession(session: UserSessionData) {
        dataStore.edit { prefs ->
            prefs[KEY_UID] = session.uid
            prefs[KEY_NICKNAME] = session.nickname
            prefs[KEY_ROLE] = session.role
            prefs[KEY_POINTS_TOTAL] = session.pointsTotal
        }
    }

    suspend fun clearSession() {
        dataStore.edit { prefs ->
            prefs.clear()
        }
    }

    companion object {
        private const val DEFAULT_ROLE = "user"

        private val KEY_UID = stringPreferencesKey("uid")
        private val KEY_NICKNAME = stringPreferencesKey("nickname")
        private val KEY_ROLE = stringPreferencesKey("role")
        private val KEY_POINTS_TOTAL = longPreferencesKey("points_total")
    }
}