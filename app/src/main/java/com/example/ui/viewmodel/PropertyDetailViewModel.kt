package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChatRepository
import com.example.data.PremiumSchedule
import com.example.data.PropertyRepository
import com.example.data.UiState
import com.example.data.extractFirestoreErrorMessage
import com.example.data.model.PaymentMethod
import com.example.data.model.PropertyContact
import com.example.data.model.PropertyListing
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PropertyDetailViewModel(
  private val propertyRepository: PropertyRepository,
  private val chatRepository: ChatRepository,
  private val currentUserId: String,
  private val propertyId: String,
) : ViewModel() {

  val propertyState: StateFlow<UiState<PropertyListing?>> =
    propertyRepository
      .observeProperty(propertyId)
      .map<PropertyListing?, UiState<PropertyListing?>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val root = firestoreError ?: error
        Log.w(TAG, "Error observing property $propertyId", root)
        emit(UiState.Error(extractFirestoreErrorMessage(root, "Failed to load property.")))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val hasContactAccess: StateFlow<Boolean> =
    combine(
        propertyState,
        propertyRepository.observeUnlockedPropertyIds(currentUserId).catch { emit(emptySet()) },
        propertyRepository.observeMembership(currentUserId).catch { emit(null) },
      ) { propState, unlocks, pass ->
        val prop = (propState as? UiState.Success)?.data
        val isOwner = prop?.ownerId == currentUserId
        val isUnlocked = propertyId in unlocks
        val hasPass =
          pass?.premiumActive == true && pass.expiresAtMillis > System.currentTimeMillis()
        PremiumSchedule.canViewContact(
          isOwner = isOwner,
          isUnlocked = isUnlocked,
          hasActivePremium = hasPass,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = false,
      )

  private val _protectedContact = MutableStateFlow<PropertyContact?>(null)
  val protectedContact: StateFlow<PropertyContact?> = _protectedContact.asStateFlow()

  private val _contactLoading = MutableStateFlow(false)
  val contactLoading: StateFlow<Boolean> = _contactLoading.asStateFlow()

  private val _statusMessage = MutableStateFlow<String?>(null)
  val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

  private val _previewBuyerMode = MutableStateFlow(false)
  val previewBuyerMode: StateFlow<Boolean> = _previewBuyerMode.asStateFlow()

  init {
    viewModelScope.launch {
      hasContactAccess.collect { unlocked ->
        if (unlocked) {
          fetchProtectedContact()
        } else {
          _protectedContact.value = null
        }
      }
    }
  }

  fun toggleBuyerPreview() {
    _previewBuyerMode.value = !_previewBuyerMode.value
  }

  fun clearStatusMessage() {
    _statusMessage.value = null
  }

  fun fetchProtectedContact() {
    viewModelScope.launch {
      _contactLoading.value = true
      propertyRepository
        .getProtectedContact(propertyId)
        .onSuccess { contact -> _protectedContact.value = contact }
        .onFailure { err ->
          _statusMessage.value =
            extractFirestoreErrorMessage(err, "Contact locked in security vault.")
        }
      _contactLoading.value = false
    }
  }

  fun unlockSingleContact(method: PaymentMethod) {
    viewModelScope.launch {
      _contactLoading.value = true
      propertyRepository
        .unlockPropertyContact(currentUserId, propertyId, method)
        .onSuccess { ref ->
          _statusMessage.value = "Contact unlocked! Receipt: $ref"
          fetchProtectedContact()
        }
        .onFailure { err ->
          _statusMessage.value = extractFirestoreErrorMessage(err, "Unlock failed.")
        }
      _contactLoading.value = false
    }
  }

  fun activatePremiumPass(method: PaymentMethod) {
    viewModelScope.launch {
      _contactLoading.value = true
      propertyRepository
        .activatePremiumPass(currentUserId, method)
        .onSuccess { ref ->
          _statusMessage.value = "7-Day Premium Pass active! Ref: $ref"
          fetchProtectedContact()
        }
        .onFailure { err ->
          _statusMessage.value = extractFirestoreErrorMessage(err, "Pass activation failed.")
        }
      _contactLoading.value = false
    }
  }

  fun openChatWithOwner(
    property: PropertyListing,
    buyerName: String,
    onChatReady: (String) -> Unit,
  ) {
    viewModelScope.launch {
      chatRepository
        .getOrCreateChat(property, currentUserId, buyerName)
        .onSuccess { chatId -> onChatReady(chatId) }
        .onFailure { err ->
          _statusMessage.value = extractFirestoreErrorMessage(err, "Unable to start chat.")
        }
    }
  }

  private companion object {
    const val TAG = "PropertyDetailVM"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
