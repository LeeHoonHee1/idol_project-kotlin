package com.example.idolproject.data.repository

import com.example.idolproject.UI.Mission.MissionRewardManager
import com.example.idolproject.domain.policy.BadgePolicy
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import com.google.firebase.auth.FirebaseAuth

class MissionRepository @Inject constructor() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    suspend fun isTodayAttendanceCompleted(uid: String): Boolean {
        val dateKey = getTodayDateKey()
        val attendanceDocId = "${uid}_${dateKey}"

        val snapshot = db.collection("attendance")
            .document(attendanceDocId)
            .get()
            .await()

        return snapshot.exists()
    }

    suspend fun completeDailyAttendance(uid: String): Result<Unit> {
        return try {
            val dateKey = getTodayDateKey()
            val attendanceDocId = "${uid}_${dateKey}"

            db.runTransaction { transaction ->

                val attendanceRef = db.collection("attendance").document(attendanceDocId)
                val userRef = db.collection("users").document(uid)

                val attendanceSnapshot = transaction.get(attendanceRef)
                if (attendanceSnapshot.exists()) {
                    throw IllegalStateException("이미 오늘 출석을 완료했습니다.")
                }

                val userSnapshot = transaction.get(userRef)
                if (!userSnapshot.exists()) {
                    throw IllegalStateException("유저 정보를 찾을 수 없습니다.")
                }

                val currentLevel = (userSnapshot.getLong("level") ?: 1L).toInt()
                val currentExp = (userSnapshot.getLong("exp") ?: 0L).toInt()

                val rewardResult = MissionRewardManager.applyExpReward(
                    currentLevel = currentLevel,
                    currentExp = currentExp,
                    rewardExp = MissionRewardManager.DAILY_ATTENDANCE_REWARD_EXP
                )

                val attendanceData = hashMapOf(
                    "uid" to uid,
                    "dateKey" to dateKey,
                    "type" to "daily_attendance",
                    "rewardExp" to MissionRewardManager.DAILY_ATTENDANCE_REWARD_EXP,
                    "createdAt" to FieldValue.serverTimestamp()
                )

                transaction.set(attendanceRef, attendanceData)

                transaction.update(
                    userRef,
                    mapOf(
                        "level" to rewardResult.newLevel,
                        "exp" to rewardResult.newExp,
                        "badgeId" to rewardResult.newBadgeId,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                null
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getTodayDateKey(): String {
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        return sdf.format(Date())
    }

    suspend fun getWeeklyAttendanceCount(uid: String): Int {
        val (startDateKey, endDateKey) = getCurrentWeekRange()

        val snapshot = db.collection("attendance")
            .whereEqualTo("uid", uid)
            .whereEqualTo("type", "daily_attendance")
            .whereGreaterThanOrEqualTo("dateKey", startDateKey)
            .whereLessThanOrEqualTo("dateKey", endDateKey)
            .get()
            .await()

        return snapshot.size()
    }

    private fun getCurrentWeekRange(): Pair<String, String> {
        val calendar = Calendar.getInstance()

        // 월요일 시작 기준
        calendar.firstDayOfWeek = Calendar.MONDAY

        val today = calendar.clone() as Calendar

        val dayOfWeek = today.get(Calendar.DAY_OF_WEEK)
        val diffToMonday = when (dayOfWeek) {
            Calendar.SUNDAY -> -6
            else -> Calendar.MONDAY - dayOfWeek
        }

        val startCal = today.clone() as Calendar
        startCal.add(Calendar.DAY_OF_MONTH, diffToMonday)

        val endCal = startCal.clone() as Calendar
        endCal.add(Calendar.DAY_OF_MONTH, 6)

        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val startDateKey = sdf.format(startCal.time)
        val endDateKey = sdf.format(endCal.time)

        return Pair(startDateKey, endDateKey)
    }

    fun getCurrentWeekKey(): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR)
        return "${year}_W${weekOfYear}"
    }

    suspend fun hasClaimedWeeklyReward(uid: String): Boolean {
        val weekKey = getCurrentWeekKey()
        val rewardDocId = "${uid}_${weekKey}"

        val snapshot = db.collection("weekly_rewards")
            .document(rewardDocId)
            .get()
            .await()

        return snapshot.exists()
    }

    suspend fun claimWeeklyReward(uid: String): Result<Unit> {
        return try {
            val weeklyCount = getWeeklyAttendanceCount(uid)
            if (weeklyCount < 7) {
                return Result.failure(IllegalStateException("아직 주간 미션을 완료하지 않았습니다."))
            }

            val weekKey = getCurrentWeekKey()
            val rewardDocId = "${uid}_${weekKey}"

            db.runTransaction { transaction ->
                val rewardRef = db.collection("weekly_rewards").document(rewardDocId)
                val userRef = db.collection("users").document(uid)

                val rewardSnapshot = transaction.get(rewardRef)
                if (rewardSnapshot.exists()) {
                    throw IllegalStateException("이번 주 보상은 이미 받았습니다.")
                }

                val userSnapshot = transaction.get(userRef)
                if (!userSnapshot.exists()) {
                    throw IllegalStateException("유저 정보를 찾을 수 없습니다.")
                }

                val currentLevel = (userSnapshot.getLong("level") ?: 1L).toInt()
                val currentExp = (userSnapshot.getLong("exp") ?: 0L).toInt()

                val rewardResult = MissionRewardManager.applyExpReward(
                    currentLevel = currentLevel,
                    currentExp = currentExp,
                    rewardExp = MissionRewardManager.WEEKLY_ATTENDANCE_REWARD_EXP
                )

                val rewardData = hashMapOf(
                    "uid" to uid,
                    "weekKey" to weekKey,
                    "type" to "weekly_attendance_reward",
                    "rewardExp" to MissionRewardManager.WEEKLY_ATTENDANCE_REWARD_EXP,
                    "createdAt" to FieldValue.serverTimestamp()
                )

                transaction.set(rewardRef, rewardData)

                transaction.update(
                    userRef,
                    mapOf(
                        "level" to rewardResult.newLevel,
                        "exp" to rewardResult.newExp,
                        "badgeId" to rewardResult.newBadgeId,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                null
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    //테스터용 미션
    suspend fun grantTestExp(uid: String, rewardExp: Int = 10): Result<Unit> {
        return try {
            db.runTransaction { transaction ->
                val userRef = db.collection("users").document(uid)
                val userSnapshot = transaction.get(userRef)

                if (!userSnapshot.exists()) {
                    throw IllegalStateException("유저 정보를 찾을 수 없습니다.")
                }

                val currentLevel = (userSnapshot.getLong("level") ?: 1L).toInt()
                val currentExp = (userSnapshot.getLong("exp") ?: 0L).toInt()

                val rewardResult = MissionRewardManager.applyExpReward(
                    currentLevel = currentLevel,
                    currentExp = currentExp,
                    rewardExp = rewardExp
                )

                transaction.update(
                    userRef,
                    mapOf(
                        "level" to rewardResult.newLevel,
                        "exp" to rewardResult.newExp,
                        "badgeId" to rewardResult.newBadgeId,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                null
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeMissionGrowthProfile(): Flow<MissionGrowthProfile> = callbackFlow {
        val uid = auth.currentUser?.uid

        if (uid.isNullOrBlank()) {
            close(IllegalStateException("로그인이 필요합니다."))
            return@callbackFlow
        }

        val listener = db.collection("users")
            .document(uid)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snap == null || !snap.exists()) {
                    close(IllegalStateException("사용자 정보를 찾을 수 없습니다."))
                    return@addSnapshotListener
                }

                val level = (snap.getLong("level") ?: 1L).toInt()
                val exp = (snap.getLong("exp") ?: 0L).toInt()

                val needExp = 100
                val percent = ((exp * 100) / needExp).coerceIn(0, 100)

                val badgeIdFromDb = snap.getString("badgeId").orEmpty()
                val finalBadgeId = BadgePolicy.resolveBadgeId(
                    level = level,
                    badgeIdFromDb = badgeIdFromDb
                )

                trySend(
                    MissionGrowthProfile(
                        level = level,
                        exp = exp,
                        needExp = needExp,
                        expPercent = percent,
                        badgeId = finalBadgeId
                    )
                )
            }

        awaitClose {
            listener.remove()
        }
    }
}

data class MissionGrowthProfile(
    val level: Int = 1,
    val exp: Int = 0,
    val needExp: Int = 100,
    val expPercent: Int = 0,
    val badgeId: String = "bronze"
)