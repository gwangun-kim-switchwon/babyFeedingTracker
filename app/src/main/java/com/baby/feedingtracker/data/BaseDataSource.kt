package com.baby.feedingtracker.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

abstract class BaseDataSource<T : BaseRecord>(
    private val firestore: FirebaseFirestore,
    private val uid: String,
    protected val currentUserUid: String = uid  // 현재 로그인한 사용자 uid
) {
    abstract val collectionName: String
    abstract fun DocumentSnapshot.toRecord(): T

    protected val recordsCollection
        get() = firestore.collection("users").document(uid).collection(collectionName)

    fun getRecent(sinceTimestamp: Long): Flow<List<T>> {
        return recordsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .whereGreaterThanOrEqualTo("timestamp", sinceTimestamp)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { doc ->
                    doc.toRecord()
                }
            }
    }

    suspend fun loadOlderRecords(beforeTimestamp: Long, limit: Long = 20): List<T> {
        val snapshot = recordsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .whereLessThan("timestamp", beforeTimestamp)
            .limit(limit)
            .get()
            .await()
        return snapshot.documents.map { it.toRecord() }
    }

    suspend fun insert(data: Map<String, Any?>): DataResult<String> {
        return try {
            val docRef = recordsCollection.add(data).await()
            DataResult.Success(docRef.id)
        } catch (e: Exception) {
            DataResult.Error("저장에 실패했습니다")
        }
    }

    suspend fun delete(recordId: String): DataResult<Unit> {
        return try {
            recordsCollection.document(recordId).delete().await()
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error("삭제에 실패했습니다")
        }
    }

    suspend fun updateTimestamp(recordId: String, timestamp: Long): DataResult<Unit> {
        return try {
            recordsCollection.document(recordId).update("timestamp", timestamp).await()
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error("수정에 실패했습니다")
        }
    }

    suspend fun updateNote(recordId: String, note: String?): DataResult<Unit> {
        return try {
            recordsCollection.document(recordId).update("note", note).await()
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error("수정에 실패했습니다")
        }
    }

    suspend fun updateFields(recordId: String, fields: Map<String, Any?>): DataResult<Unit> {
        return try {
            recordsCollection.document(recordId).update(fields).await()
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error("수정에 실패했습니다")
        }
    }
}
