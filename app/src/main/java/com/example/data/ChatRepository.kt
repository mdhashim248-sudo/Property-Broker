package com.example.data

import android.content.Context
import com.example.R
import com.example.data.model.ChatMessage
import com.example.data.model.PropertyChat
import com.example.data.model.PropertyListing
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ChatRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(
    context: Context
  ) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before chatting.")

  fun observeMyChats(userId: String): Flow<List<PropertyChat>> =
    db.collection("chats")
      .whereArrayContains("participantUids", userId)
      .snapshots()
      .map { snap ->
        snap
          .toObjects(PropertyChat::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.updatedAt?.seconds ?: 0L }
      }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == userId) {
          handleFirestoreError(exceptionToLog, OperationType.LIST, "chats")
        }
        throw error
      }

  fun observeChat(chatId: String, userId: String): Flow<PropertyChat?> =
    db.collection("chats")
      .document(chatId)
      .snapshots()
      .map { snap ->
        snap.toObject(PropertyChat::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == userId) {
          handleFirestoreError(exceptionToLog, OperationType.GET, "chats/$chatId")
        }
        throw error
      }

  fun observeMessages(chatId: String, userId: String): Flow<List<ChatMessage>> =
    db.collection("chats")
      .document(chatId)
      .collection("messages")
      .whereArrayContains("participantUids", userId)
      .snapshots()
      .map { snap ->
        snap
          .toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedBy { it.createdAt?.seconds ?: Long.MAX_VALUE }
      }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == userId) {
          handleFirestoreError(exceptionToLog, OperationType.LIST, "chats/$chatId/messages")
        }
        throw error
      }

  suspend fun getOrCreateChat(
    property: PropertyListing,
    buyerId: String,
    buyerName: String,
  ): Result<String> {
    val safeBuyer = buyerId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
    val safeProp = property.id.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
    val chatId = "${safeProp}_${safeBuyer}".take(120)
    val path = "chats/$chatId"
    return try {
      val uid = requireUserId()
      check(uid == buyerId) { "Refusing chat creation for mismatched user." }
      val docRef = db.collection("chats").document(chatId)
      val existing = docRef.get().await()
      if (!existing.exists()) {
        val participants = listOf(buyerId, property.ownerId)
        val payload =
          mapOf(
            "propertyId" to property.id,
            "propertyTitle" to property.title.take(120),
            "buyerId" to buyerId,
            "buyerName" to buyerName.ifBlank { "Verified Buyer" }.take(80),
            "ownerId" to property.ownerId,
            "ownerName" to property.ownerName.ifBlank { "Property Owner" }.take(80),
            "participantUids" to participants,
            "lastMessage" to "Inquiry started for ${property.title}".take(500),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        docRef.set(payload).await()
      }
      Result.success(chatId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun sendMessage(
    chatId: String,
    senderId: String,
    senderName: String,
    participantUids: List<String>,
    text: String,
  ): Result<Unit> {
    var path = "chats/$chatId/messages"
    return try {
      val uid = requireUserId()
      check(uid == senderId) { "Refusing message send for mismatched user." }
      val cleanText = text.trim()
      require(cleanText.isNotEmpty() && cleanText.length <= 1000) {
        "Message must be 1–1000 characters."
      }
      require(participantUids.size == 2 && uid in participantUids) {
        "Invalid chat participants."
      }
      val chatRef = db.collection("chats").document(chatId)
      val msgRef = chatRef.collection("messages").document()
      path = msgRef.path
      msgRef
        .set(
          mapOf(
            "chatId" to chatId,
            "senderId" to uid,
            "senderName" to senderName.ifBlank { "Member" }.take(80),
            "participantUids" to participantUids,
            "text" to cleanText,
            "createdAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()

      path = chatRef.path
      chatRef
        .update(
          mapOf(
            "lastMessage" to cleanText.take(500),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }
}
