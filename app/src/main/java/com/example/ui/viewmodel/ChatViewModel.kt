package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChatRepository
import com.example.data.UiState
import com.example.data.extractFirestoreErrorMessage
import com.example.data.model.ChatMessage
import com.example.data.model.PropertyChat
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(
  private val chatRepository: ChatRepository,
  val currentUserId: String,
  private val chatId: String,
) : ViewModel() {

  val chatState: StateFlow<PropertyChat?> =
    chatRepository
      .observeChat(chatId, currentUserId)
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        Log.w(TAG, "Error observing chat $chatId", firestoreError ?: error)
        emit(null)
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = null,
      )

  val messagesState: StateFlow<UiState<List<ChatMessage>>> =
    chatRepository
      .observeMessages(chatId, currentUserId)
      .map<List<ChatMessage>, UiState<List<ChatMessage>>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val root = firestoreError ?: error
        Log.w(TAG, "Error observing messages $chatId", root)
        emit(UiState.Error(extractFirestoreErrorMessage(root, "Failed to load messages.")))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  private val _isSending = MutableStateFlow(false)
  val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

  private val _errorBanner = MutableStateFlow<String?>(null)
  val errorBanner: StateFlow<String?> = _errorBanner.asStateFlow()

  fun clearError() {
    _errorBanner.value = null
  }

  fun sendMessage(senderName: String, text: String, onSent: () -> Unit = {}) {
    val chat = chatState.value ?: return
    val clean = text.trim()
    if (clean.isEmpty() || _isSending.value) return
    viewModelScope.launch {
      _isSending.value = true
      chatRepository
        .sendMessage(
          chatId = chatId,
          senderId = currentUserId,
          senderName = senderName,
          participantUids = chat.participantUids,
          text = clean,
        )
        .onSuccess { onSent() }
        .onFailure { err ->
          _errorBanner.value = extractFirestoreErrorMessage(err, "Failed to send message.")
        }
      _isSending.value = false
    }
  }

  private companion object {
    const val TAG = "ChatVM"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
