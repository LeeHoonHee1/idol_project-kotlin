package com.example.idolproject.data.repository

import com.example.idolproject.UI.Ranking.GroupRank
import com.example.idolproject.UI.Ranking.UserRank
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class RankingRepository @Inject constructor() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    fun observeUserRanking(): Flow<List<UserRank>> = callbackFlow {
        val listener = db.collection("users")
            .orderBy("level", Query.Direction.DESCENDING)
            .orderBy("exp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val users = snapshot?.documents.orEmpty().mapNotNull { document ->
                    document.toObject(UserRank::class.java)?.copy(
                        uid = document.id
                    )
                }

                trySend(users)
            }

        awaitClose {
            listener.remove()
        }
    }

    fun observeGroupRanking(): Flow<List<GroupRank>> = callbackFlow {
        val listener = db.collection("groups")
            .orderBy("likeCount", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val groups = snapshot?.documents.orEmpty().mapNotNull { document ->
                    document.toObject(GroupRank::class.java)?.copy(
                        groupId = document.id
                    )
                }

                trySend(groups)
            }

        awaitClose {
            listener.remove()
        }
    }
}