package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChatRepository
import com.example.data.PropertyRepository
import com.example.data.UiState
import com.example.data.extractFirestoreErrorMessage
import com.example.data.model.ListingStatus
import com.example.data.model.ListingType
import com.example.data.model.PaymentMethod
import com.example.data.model.PremiumMembership
import com.example.data.model.PropertyCategory
import com.example.data.model.PropertyChat
import com.example.data.model.PropertyListing
import com.example.data.model.PropertySearchMatcher
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MarketplaceFilter(
  val searchQuery: String = "",
  val listingType: ListingType? = null,
  val category: PropertyCategory? = null,
  val minPrice: Long? = null,
  val maxPrice: Long? = null,
  val favoritesOnly: Boolean = false,
)

class MarketplaceViewModel(
  private val propertyRepository: PropertyRepository,
  private val chatRepository: ChatRepository,
  val currentUserId: String,
) : ViewModel() {

  private val _filter = MutableStateFlow(MarketplaceFilter())
  val filter: StateFlow<MarketplaceFilter> = _filter.asStateFlow()

  private val _actionBanner = MutableStateFlow<String?>(null)
  val actionBanner: StateFlow<String?> = _actionBanner.asStateFlow()

  private val _isSubmitting = MutableStateFlow(false)
  val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

  val userProfile: StateFlow<UserProfile?> =
    propertyRepository
      .observeUserProfile(currentUserId)
      .catch { error ->
        unwrapFirestoreOrThrow(error, "userProfile")
        emit(null)
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = null,
      )

  val activePropertiesState: StateFlow<UiState<List<PropertyListing>>> =
    propertyRepository
      .observeActiveProperties()
      .map<List<PropertyListing>, UiState<List<PropertyListing>>> { UiState.Success(it) }
      .catch { error ->
        val root = unwrapFirestoreOrThrow(error, "activeProperties")
        emit(UiState.Error(extractFirestoreErrorMessage(root, "Failed to load properties.")))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val myPropertiesState: StateFlow<UiState<List<PropertyListing>>> =
    propertyRepository
      .observeMyProperties(currentUserId)
      .map<List<PropertyListing>, UiState<List<PropertyListing>>> { UiState.Success(it) }
      .catch { error ->
        val root = unwrapFirestoreOrThrow(error, "myProperties")
        emit(UiState.Error(extractFirestoreErrorMessage(root, "Failed to load your listings.")))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val favoriteIds: StateFlow<Set<String>> =
    propertyRepository
      .observeFavoriteIds(currentUserId)
      .catch { error ->
        unwrapFirestoreOrThrow(error, "favorites")
        emit(emptySet())
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptySet(),
      )

  val unlockedPropertyIds: StateFlow<Set<String>> =
    propertyRepository
      .observeUnlockedPropertyIds(currentUserId)
      .catch { error ->
        unwrapFirestoreOrThrow(error, "unlocks")
        emit(emptySet())
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptySet(),
      )

  val membership: StateFlow<PremiumMembership?> =
    propertyRepository
      .observeMembership(currentUserId)
      .catch { error ->
        unwrapFirestoreOrThrow(error, "membership")
        emit(null)
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = null,
      )

  val myChatsState: StateFlow<UiState<List<PropertyChat>>> =
    chatRepository
      .observeMyChats(currentUserId)
      .map<List<PropertyChat>, UiState<List<PropertyChat>>> { UiState.Success(it) }
      .catch { error ->
        val root = unwrapFirestoreOrThrow(error, "chats")
        emit(UiState.Error(extractFirestoreErrorMessage(root, "Failed to load chats.")))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val filteredPropertiesState: StateFlow<UiState<List<PropertyListing>>> =
    combine(activePropertiesState, _filter, favoriteIds) { state, filterVal, favs ->
        when (state) {
          is UiState.Loading -> UiState.Loading
          is UiState.Error -> state
          is UiState.Success -> {
            val filtered =
              state.data.filter { item ->
                val matchesType =
                  filterVal.listingType == null || item.listingType == filterVal.listingType.name
                val matchesCat =
                  filterVal.category == null || item.propertyCategory == filterVal.category.name
                val matchesMin = filterVal.minPrice == null || item.price >= filterVal.minPrice
                val matchesMax = filterVal.maxPrice == null || item.price <= filterVal.maxPrice
                val matchesFav = !filterVal.favoritesOnly || item.id in favs
                val matchesQuery = PropertySearchMatcher.matches(item, filterVal.searchQuery)
                matchesType && matchesCat && matchesMin && matchesMax && matchesFav && matchesQuery
              }
            UiState.Success(filtered)
          }
        }
      }
      .flowOn(Dispatchers.Default)
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  fun ensureProfile(user: FirebaseUser, preferredDisplayName: String? = null) {
    viewModelScope.launch { propertyRepository.ensureUserProfile(user, preferredDisplayName) }
  }

  fun updateSearchQuery(query: String) {
    _filter.value = _filter.value.copy(searchQuery = query)
  }

  fun setListingTypeFilter(type: ListingType?) {
    _filter.value = _filter.value.copy(listingType = type)
  }

  fun setCategoryFilter(category: PropertyCategory?) {
    _filter.value = _filter.value.copy(category = category)
  }

  fun setPriceRange(minPrice: Long?, maxPrice: Long?) {
    _filter.value = _filter.value.copy(minPrice = minPrice, maxPrice = maxPrice)
  }

  fun toggleFavoritesOnly() {
    _filter.value = _filter.value.copy(favoritesOnly = !_filter.value.favoritesOnly)
  }

  fun setFavoritesOnly(enabled: Boolean) {
    _filter.value = _filter.value.copy(favoritesOnly = enabled)
  }

  fun resetFilters() {
    _filter.value = MarketplaceFilter()
  }

  fun clearBanner() {
    _actionBanner.value = null
  }

  fun toggleFavorite(propertyId: String) {
    val isFav = propertyId in favoriteIds.value
    viewModelScope.launch {
      propertyRepository.toggleFavorite(currentUserId, propertyId, isFav).onFailure { err ->
        _actionBanner.value = extractFirestoreErrorMessage(err, "Unable to update bookmark.")
      }
    }
  }

  fun updateUserRole(role: UserRole) {
    viewModelScope.launch {
      propertyRepository
        .updateUserRole(currentUserId, role)
        .onSuccess { _actionBanner.value = "Workspace mode updated to ${role.label}." }
        .onFailure { err ->
          _actionBanner.value = extractFirestoreErrorMessage(err, "Unable to update role.")
        }
    }
  }

  fun seedStarterListings(ownerName: String, ownerEmail: String) {
    if (_isSubmitting.value) return
    viewModelScope.launch {
      _isSubmitting.value = true
      propertyRepository
        .seedStarterShowcaseListings(ownerName, ownerEmail)
        .onSuccess { count ->
          _actionBanner.value = "Added $count verified showcase properties to your catalog."
        }
        .onFailure { err ->
          _actionBanner.value =
            extractFirestoreErrorMessage(err, "Could not add showcase listings.")
        }
      _isSubmitting.value = false
    }
  }

  fun createProperty(
    title: String,
    description: String,
    price: Long,
    location: String,
    ownerName: String,
    contactPhone: String,
    contactEmail: String,
    whatsappEnabled: Boolean,
    listingType: ListingType,
    category: PropertyCategory,
    bedrooms: Long,
    areaSqFt: Long,
    photoKeys: List<String>,
    videoLabel: String,
    totalMediaBytes: Long,
    latitude: Double? = null,
    longitude: Double? = null,
    floor: String = "",
    depositAmount: Long? = null,
    monthlyRent: Long? = null,
    buildingAge: String = "",
    hasLift: Boolean? = null,
    onCreated: (String) -> Unit,
  ) {
    if (_isSubmitting.value) return
    viewModelScope.launch {
      _isSubmitting.value = true
      propertyRepository
        .createProperty(
          title = title,
          description = description,
          price = price,
          location = location,
          ownerName = ownerName,
          contactPhone = contactPhone,
          contactEmail = contactEmail,
          whatsappEnabled = whatsappEnabled,
          listingType = listingType,
          category = category,
          bedrooms = bedrooms,
          areaSqFt = areaSqFt,
          photoKeys = photoKeys,
          videoLabel = videoLabel,
          totalMediaBytes = totalMediaBytes,
          latitude = latitude,
          longitude = longitude,
          floor = floor,
          depositAmount = depositAmount,
          monthlyRent = monthlyRent,
          buildingAge = buildingAge,
          hasLift = hasLift,
        )
        .onSuccess { propertyId ->
          _actionBanner.value = "Property published with PII Vault protection."
          onCreated(propertyId)
        }
        .onFailure { err ->
          _actionBanner.value = extractFirestoreErrorMessage(err, "Failed to publish property.")
        }
      _isSubmitting.value = false
    }
  }

  fun togglePropertyStatus(property: PropertyListing) {
    val next =
      if (property.status == ListingStatus.ACTIVE.name) ListingStatus.CLOSED
      else ListingStatus.ACTIVE
    viewModelScope.launch {
      propertyRepository
        .updatePropertyStatus(property.id, next)
        .onSuccess { _actionBanner.value = "Listing marked as ${next.label}." }
        .onFailure { err ->
          _actionBanner.value = extractFirestoreErrorMessage(err, "Failed to update listing.")
        }
    }
  }

  fun deleteProperty(propertyId: String) {
    viewModelScope.launch {
      propertyRepository
        .deleteProperty(propertyId)
        .onSuccess { _actionBanner.value = "Listing removed permanently." }
        .onFailure { err ->
          _actionBanner.value = extractFirestoreErrorMessage(err, "Failed to delete listing.")
        }
    }
  }

  fun activatePremiumPass(method: PaymentMethod, onSuccess: (String) -> Unit = {}) {
    if (_isSubmitting.value) return
    viewModelScope.launch {
      _isSubmitting.value = true
      propertyRepository
        .activatePremiumPass(currentUserId, method)
        .onSuccess { ref ->
          _actionBanner.value = "7-Day Premium Pass activated ($ref)."
          onSuccess(ref)
        }
        .onFailure { err ->
          _actionBanner.value = extractFirestoreErrorMessage(err, "Failed to activate pass.")
        }
      _isSubmitting.value = false
    }
  }

  suspend fun deleteUserAccountData(): Result<Unit> {
    return propertyRepository.deleteUserAccountData(currentUserId)
  }

  fun showMessage(message: String) {
    _actionBanner.value = message
  }

  private fun unwrapFirestoreOrThrow(error: Throwable, stream: String): Throwable {
    val firestoreError =
      generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    val root = firestoreError ?: error
    Log.w(TAG, "Error observing $stream", root)
    return root
  }

  private companion object {
    const val TAG = "MarketplaceVM"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
