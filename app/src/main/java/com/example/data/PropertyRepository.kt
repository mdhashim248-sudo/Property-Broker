package com.example.data

import android.content.Context
import com.example.R
import com.example.data.model.ContactUnlock
import com.example.data.model.FavoriteRef
import com.example.data.model.ListingStatus
import com.example.data.model.ListingType
import com.example.data.model.PaymentMethod
import com.example.data.model.PremiumMembership
import com.example.data.model.PropertyCategory
import com.example.data.model.PropertyContact
import com.example.data.model.PropertyListing
import com.example.data.model.PropertyMediaLimits
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class PropertyRepository(
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
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

  suspend fun ensureUserProfile(
    user: FirebaseUser,
    preferredDisplayName: String? = null,
  ): Result<Unit> {
    val uid = user.uid
    val path = "users/$uid"
    return try {
      val docRef = db.collection("users").document(uid)
      val existingSnap = docRef.get().await()
      val cleanPreferred = preferredDisplayName?.trim()?.takeIf { it.isNotEmpty() }?.take(80)
      if (!existingSnap.exists()) {
        val displayName =
          cleanPreferred
            ?: user.displayName?.trim()?.takeIf { it.isNotEmpty() }?.take(80)
            ?: user.email?.substringBefore("@")?.take(80)
            ?: "Verified Member"
        val payload =
          mapOf(
            "userId" to uid,
            "displayName" to displayName,
            "email" to (user.email ?: "").take(160),
            "photoUrl" to user.photoUrl?.toString()?.take(600),
            "role" to UserRole.BOTH.name,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ).filterValues { it != null }
        docRef.set(payload).await()
      } else if (cleanPreferred != null) {
        docRef
          .update(
            mapOf(
              "displayName" to cleanPreferred,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  fun observeUserProfile(userId: String): Flow<UserProfile?> =
    db.collection("users")
      .document(userId)
      .snapshots()
      .map { snap ->
        snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
      .catch { error ->
        logSnapshotErrorIfSignedIn(error, OperationType.GET, "users/$userId", userId)
        throw error
      }

  suspend fun updateUserRole(userId: String, role: UserRole): Result<Unit> {
    val path = "users/$userId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
      db.collection("users")
        .document(userId)
        .update(mapOf("role" to role.name, "updatedAt" to FieldValue.serverTimestamp()))
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun deleteUserAccountData(userId: String): Result<Unit> {
    var path = "users/$userId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Signed-in user changed; refusing cross-user delete." }
      // Delete user's own listings & private contacts
      val ownListings = db.collection("properties").whereEqualTo("ownerId", userId).get().await()
      for (doc in ownListings.documents) {
        runCatching { doc.reference.collection("private").document("contact").delete().await() }
        runCatching { doc.reference.delete().await() }
      }
      // Delete user's saved favorites
      val favs = db.collection("users").document(userId).collection("favorites").get().await()
      for (fav in favs.documents) {
        runCatching { fav.reference.delete().await() }
      }
      // Delete user profile document
      path = "users/$userId"
      db.collection("users").document(userId).delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, path)
      Result.failure(e)
    }
  }

  fun observeActiveProperties(): Flow<List<PropertyListing>> = flow {
    val uid = requireUserId()
    val path = "properties"
    emit(emptyList())
    db.collection(path)
      .whereEqualTo("status", ListingStatus.ACTIVE.name)
      .snapshots()
      .map { snap ->
        snap
          .toObjects(PropertyListing::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .catch { error ->
        logSnapshotErrorIfSignedIn(error, OperationType.LIST, path, uid)
        throw error
      }
      .collect { emit(it) }
  }

  fun observeMyProperties(userId: String): Flow<List<PropertyListing>> =
    db.collection("properties")
      .whereEqualTo("ownerId", userId)
      .snapshots()
      .map { snap ->
        snap
          .toObjects(PropertyListing::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .catch { error ->
        logSnapshotErrorIfSignedIn(error, OperationType.LIST, "properties", userId)
        throw error
      }

  fun observeProperty(propertyId: String): Flow<PropertyListing?> = flow {
    val uid = requireUserId()
    val path = "properties/$propertyId"
    db.collection("properties")
      .document(propertyId)
      .snapshots()
      .map { snap ->
        snap.toObject(PropertyListing::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
      .catch { error ->
        logSnapshotErrorIfSignedIn(error, OperationType.GET, path, uid)
        throw error
      }
      .collect { emit(it) }
  }

  fun observeFavoriteIds(userId: String): Flow<Set<String>> =
    db.collection("users")
      .document(userId)
      .collection("favorites")
      .snapshots()
      .map { snap ->
        snap
          .toObjects(FavoriteRef::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .map { it.propertyId.ifBlank { it.id } }
          .toSet()
      }
      .catch { error ->
        logSnapshotErrorIfSignedIn(
          error,
          OperationType.LIST,
          "users/$userId/favorites",
          userId,
        )
        throw error
      }

  suspend fun toggleFavorite(
    userId: String,
    propertyId: String,
    currentlyFavorite: Boolean,
  ): Result<Unit> {
    val path = "users/$userId/favorites/$propertyId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Refusing cross-user favorite update" }
      val docRef = db.collection("users").document(userId).collection("favorites").document(propertyId)
      if (currentlyFavorite) {
        docRef.delete().await()
      } else {
        docRef
          .set(
            mapOf(
              "userId" to uid,
              "propertyId" to propertyId,
              "createdAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  fun observeUnlockedPropertyIds(userId: String): Flow<Set<String>> =
    db.collection("users")
      .document(userId)
      .collection("unlocks")
      .snapshots()
      .map { snap ->
        snap
          .toObjects(ContactUnlock::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .map { it.propertyId.ifBlank { it.id } }
          .toSet()
      }
      .catch { error ->
        logSnapshotErrorIfSignedIn(error, OperationType.LIST, "users/$userId/unlocks", userId)
        throw error
      }

  fun observeMembership(userId: String): Flow<PremiumMembership?> =
    db.collection("users")
      .document(userId)
      .collection("private")
      .document("membership")
      .snapshots()
      .map { snap ->
        snap.toObject(
          PremiumMembership::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        )
      }
      .catch { error ->
        logSnapshotErrorIfSignedIn(
          error,
          OperationType.GET,
          "users/$userId/private/membership",
          userId,
        )
        throw error
      }

  suspend fun createProperty(
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
  ): Result<String> {
    var path = "properties"
    return try {
      val uid = requireUserId()
      val cleanTitle = title.trim()
      val cleanLocation = location.trim()
      val cleanOwner = ownerName.trim()
      val cleanPhone = contactPhone.trim()
      val cleanDesc = description.trim()
      val cleanEmail = contactEmail.trim()
      val cleanFloor = floor.trim().take(40)
      val cleanBuildingAge = buildingAge.trim().take(40)
      val cleanPhotos =
        photoKeys.map { it.trim().take(500) }.filter { it.isNotEmpty() }.take(PropertyMediaLimits.MAX_PHOTOS)
      val cleanVideo = videoLabel.trim().take(300)

      require(cleanTitle.length in 3..100) { "Title must be 3–100 characters." }
      require(cleanLocation.length in 2..120) { "Location must be 2–120 characters." }
      require(cleanOwner.length in 1..80) { "Owner name is required." }
      require(cleanPhone.length in 7..20) { "Contact phone must be 7–20 digits." }
      require(price in 1L..10_000_000_000L) { "Enter a valid property price." }
      require(bedrooms in 0L..20L) { "Bedrooms must be between 0 and 20." }
      require(areaSqFt in 0L..1_000_000L) { "Area must be between 0 and 1,000,000 sq.ft." }
      require(cleanPhotos.size in PropertyMediaLimits.MIN_PHOTOS..PropertyMediaLimits.MAX_PHOTOS) {
        "Upload between ${PropertyMediaLimits.MIN_PHOTOS} and ${PropertyMediaLimits.MAX_PHOTOS} photos."
      }
      require(totalMediaBytes in 0L..PropertyMediaLimits.MAX_TOTAL_BYTES) {
        "Combined photos & video must not exceed 8 MB."
      }
      if (latitude != null) {
        require(latitude.isFinite() && latitude in -90.0..90.0) { "Invalid latitude coordinate." }
      }
      if (longitude != null) {
        require(longitude.isFinite() && longitude in -180.0..180.0) { "Invalid longitude coordinate." }
      }
      if (depositAmount != null) {
        require(depositAmount in 0L..10_000_000_000L) { "Enter a valid deposit amount." }
      }
      if (monthlyRent != null) {
        require(monthlyRent in 0L..10_000_000_000L) { "Enter a valid monthly rent." }
      }

      val docRef = db.collection("properties").document()
      path = docRef.path
      val propertyPayload =
        mapOf(
          "ownerId" to uid,
          "ownerName" to cleanOwner,
          "title" to cleanTitle,
          "description" to cleanDesc.take(1200),
          "price" to price,
          "location" to cleanLocation,
          "listingType" to listingType.name,
          "propertyCategory" to category.name,
          "bedrooms" to bedrooms,
          "areaSqFt" to areaSqFt,
          "photoKeys" to cleanPhotos,
          "hasVideo" to cleanVideo.isNotEmpty(),
          "videoLabel" to cleanVideo,
          "totalMediaBytes" to totalMediaBytes,
          "verifiedListing" to true,
          "status" to ListingStatus.ACTIVE.name,
          "latitude" to latitude,
          "longitude" to longitude,
          "floor" to cleanFloor.takeIf { it.isNotEmpty() },
          "depositAmount" to if (listingType == ListingType.RENT) depositAmount else null,
          "monthlyRent" to if (listingType == ListingType.HEAVY_DEPOSIT) monthlyRent else null,
          "buildingAge" to cleanBuildingAge.takeIf { it.isNotEmpty() },
          "hasLift" to hasLift,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ).filterValues { it != null }
      docRef.set(propertyPayload).await()

      val contactRef = docRef.collection("private").document("contact")
      path = contactRef.path
      val contactPayload =
        mapOf(
          "ownerId" to uid,
          "contactPhone" to cleanPhone,
          "contactEmail" to cleanEmail.take(160),
          "whatsappEnabled" to whatsappEnabled,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      contactRef.set(contactPayload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun updatePropertyStatus(propertyId: String, status: ListingStatus): Result<Unit> {
    val path = "properties/$propertyId"
    return try {
      requireUserId()
      db.collection("properties")
        .document(propertyId)
        .update(mapOf("status" to status.name, "updatedAt" to FieldValue.serverTimestamp()))
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun deleteProperty(propertyId: String): Result<Unit> {
    var path = "properties/$propertyId/private/contact"
    return try {
      requireUserId()
      val propRef = db.collection("properties").document(propertyId)
      runCatching { propRef.collection("private").document("contact").delete().await() }
      path = propRef.path
      propRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, path)
      Result.failure(e)
    }
  }

  suspend fun unlockPropertyContact(
    userId: String,
    propertyId: String,
    paymentMethod: PaymentMethod,
  ): Result<String> {
    val path = "users/$userId/unlocks/$propertyId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Refusing cross-user unlock write." }
      val refCode = "PB-${UUID.randomUUID().toString().take(8).uppercase()}"
      db.collection("users")
        .document(userId)
        .collection("unlocks")
        .document(propertyId)
        .set(
          mapOf(
            "userId" to uid,
            "propertyId" to propertyId,
            "amountPaid" to 1000L,
            "paymentMethod" to paymentMethod.name,
            "referenceCode" to refCode,
            "unlockedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(refCode)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun activatePremiumPass(
    userId: String,
    paymentMethod: PaymentMethod,
  ): Result<String> {
    val path = "users/$userId/private/membership"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Refusing cross-user membership write." }
      val refCode = "PASS-${UUID.randomUUID().toString().take(8).uppercase()}"
      val expiresAt = System.currentTimeMillis() + (7L * 24L * 60L * 60L * 1000L)
      val docRef =
        db.collection("users").document(userId).collection("private").document("membership")
      val existingSnap = docRef.get().await()
      if (existingSnap.exists()) {
        docRef
          .update(
            mapOf(
              "planName" to "7-Day Premium Pass (${paymentMethod.label})",
              "amountPaid" to 999L,
              "premiumActive" to true,
              "referenceCode" to refCode,
              "expiresAtMillis" to expiresAt,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      } else {
        docRef
          .set(
            mapOf(
              "userId" to uid,
              "planName" to "7-Day Premium Pass (${paymentMethod.label})",
              "amountPaid" to 999L,
              "premiumActive" to true,
              "referenceCode" to refCode,
              "expiresAtMillis" to expiresAt,
              "activatedAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
      Result.success(refCode)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  suspend fun getProtectedContact(propertyId: String): Result<PropertyContact?> {
    val path = "properties/$propertyId/private/contact"
    return try {
      requireUserId()
      val snap =
        db.collection("properties")
          .document(propertyId)
          .collection("private")
          .document("contact")
          .get()
          .await()
      Result.success(
        snap.toObject(
          PropertyContact::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        )
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, path)
      Result.failure(e)
    }
  }

  suspend fun seedStarterShowcaseListings(
    ownerName: String,
    ownerEmail: String,
  ): Result<Int> {
    return try {
      requireUserId()
      val starterSpecs =
        listOf(
          StarterSpec(
            title = "3BHK Sea-View Sky Residence",
            description =
              "Sunlit corner residence with floor-to-ceiling coastal views, private deck, biometric lobby access, and 2 covered EV parking bays.",
            price = 34500000L,
            location = "Worli Sea Face, Mumbai",
            listingType = ListingType.SALE,
            category = PropertyCategory.APARTMENT,
            bedrooms = 3L,
            areaSqFt = 1850L,
            photoKeys = listOf("preset:apartment", "preset:keys"),
            phone = "+919820112233",
            latitude = 19.0176,
            longitude = 72.8156,
            buildingAge = "2 Years",
            hasLift = true,
          ),
          StarterSpec(
            title = "Minimalist Designer Loft Studio",
            description =
              "Fully furnished loft featuring custom oak cabinetry, dedicated WFH bay, smart keypad entry, and walking distance to metro.",
            price = 52000L,
            location = "Bandra West, Mumbai",
            listingType = ListingType.RENT,
            category = PropertyCategory.STUDIO,
            bedrooms = 1L,
            areaSqFt = 640L,
            photoKeys = listOf("preset:studio"),
            phone = "+919820445566",
            latitude = 19.0596,
            longitude = 72.8295,
            floor = "4th Floor",
            depositAmount = 150000L,
            buildingAge = "4 Years",
            hasLift = true,
          ),
          StarterSpec(
            title = "4BHK Tropical Courtyard Villa",
            description =
              "Private lap pool, landscaped courtyard, double-height living pavilion, 24x7 perimeter security, and solar backup.",
            price = 145000L,
            location = "Assagao, North Goa",
            listingType = ListingType.RENT,
            category = PropertyCategory.VILLA,
            bedrooms = 4L,
            areaSqFt = 3400L,
            photoKeys = listOf("preset:villa", "preset:apartment"),
            phone = "+919820778899",
            latitude = 15.5937,
            longitude = 73.7640,
            floor = "Ground + 1",
            depositAmount = 300000L,
            buildingAge = "New",
            hasLift = false,
          ),
        )
      var created = 0
      for (spec in starterSpecs) {
        val result =
          createProperty(
            title = spec.title,
            description = spec.description,
            price = spec.price,
            location = spec.location,
            ownerName = ownerName.ifBlank { "Verified Owner" },
            contactPhone = spec.phone,
            contactEmail = ownerEmail,
            whatsappEnabled = true,
            listingType = spec.listingType,
            category = spec.category,
            bedrooms = spec.bedrooms,
            areaSqFt = spec.areaSqFt,
            photoKeys = spec.photoKeys,
            videoLabel = "",
            totalMediaBytes = spec.photoKeys.size * PropertyMediaLimits.PRESET_ESTIMATED_BYTES,
            latitude = spec.latitude,
            longitude = spec.longitude,
            floor = spec.floor,
            depositAmount = spec.depositAmount,
            monthlyRent = spec.monthlyRent,
            buildingAge = spec.buildingAge,
            hasLift = spec.hasLift,
          )
        if (result.isSuccess) created++
      }
      Result.success(created)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  private data class StarterSpec(
    val title: String,
    val description: String,
    val price: Long,
    val location: String,
    val listingType: ListingType,
    val category: PropertyCategory,
    val bedrooms: Long,
    val areaSqFt: Long,
    val photoKeys: List<String>,
    val phone: String,
    val latitude: Double,
    val longitude: Double,
    val floor: String = "",
    val depositAmount: Long? = null,
    val monthlyRent: Long? = null,
    val buildingAge: String = "",
    val hasLift: Boolean? = null,
  )

  private fun logSnapshotErrorIfSignedIn(
    error: Throwable,
    op: OperationType,
    path: String,
    uid: String,
  ) {
    val firestoreError =
      generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
    if (firestoreError == null && error is CancellationException) return
    val exceptionToLog = firestoreError ?: (error as? Exception)
    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
      handleFirestoreError(exceptionToLog, op, path)
    }
  }
}
