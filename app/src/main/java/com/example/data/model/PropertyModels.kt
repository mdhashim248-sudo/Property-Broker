package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.NumberFormat
import java.util.Locale

enum class ListingType(val label: String) {
  SALE("For Sale"),
  RENT("For Rent"),
  HEAVY_DEPOSIT("Heavy Deposit");

  companion object {
    fun fromRaw(raw: String?): ListingType =
      entries.firstOrNull { it.name == raw } ?: SALE
  }
}

enum class PropertyCategory(val label: String, val isResidential: Boolean = false) {
  APARTMENT("Apartment", isResidential = true),
  VILLA("Villa", isResidential = true),
  SHOP("Shop"),
  GALA("Gala"),
  HOUSE("House / Bungalow", isResidential = true),
  OFFICE("Office"),
  STUDIO("Studio", isResidential = true),
  COMMERCIAL("Commercial"),
  PLOT("Plot / Land");

  companion object {
    fun fromRaw(raw: String?): PropertyCategory =
      entries.firstOrNull { it.name == raw } ?: APARTMENT
  }
}

enum class UserRole(val label: String, val subtitle: String) {
  BUYER("Buyer", "Search & unlock verified sale listings"),
  OWNER("Property Owner", "Post & manage sale or rental listings"),
  RENTER("Renter", "Browse verified rental homes & flats"),
  BOTH("Broker / All", "Full access to buy, sell & rent");

  companion object {
    fun fromRaw(raw: String?): UserRole =
      entries.firstOrNull { it.name == raw } ?: BUYER
  }
}

enum class ListingStatus(val label: String) {
  ACTIVE("Active"),
  CLOSED("Closed");

  companion object {
    fun fromRaw(raw: String?): ListingStatus =
      entries.firstOrNull { it.name == raw } ?: ACTIVE
  }
}

enum class PaymentMethod(val label: String) {
  UPI_VERIFIED("UPI Verified"),
  CARD_TOKEN("Card Token"),
  DEMO_PASS("Verified Pass");

  companion object {
    fun fromRaw(raw: String?): PaymentMethod =
      entries.firstOrNull { it.name == raw } ?: UPI_VERIFIED
  }
}

enum class PriceBracketPreset(
  val label: String,
  val minPrice: Long?,
  val maxPrice: Long?,
  val tagSuffix: String,
) {
  ALL("All", null, null, "all"),
  UNDER_50L("< 50L", null, 5_000_000L, "under_50l"),
  BETWEEN_50L_1CR("50L - 1Cr", 5_000_000L, 10_000_000L, "50l_1cr"),
  ABOVE_1CR("> 1Cr", 10_000_000L, null, "above_1cr"),
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "",
  val email: String = "",
  val photoUrl: String? = null,
  val role: String = UserRole.BUYER.name,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class PropertyListing(
  @DocumentId val id: String = "",
  val ownerId: String = "",
  val ownerName: String = "",
  val title: String = "",
  val description: String = "",
  val price: Long = 0L,
  val location: String = "",
  val listingType: String = ListingType.SALE.name,
  val propertyCategory: String = PropertyCategory.APARTMENT.name,
  val bedrooms: Long = 1L,
  val areaSqFt: Long = 0L,
  val photoKeys: List<String> = emptyList(),
  val hasVideo: Boolean = false,
  val videoLabel: String = "",
  val totalMediaBytes: Long = 0L,
  val verifiedListing: Boolean = true,
  val status: String = ListingStatus.ACTIVE.name,
  val latitude: Double? = null,
  val longitude: Double? = null,
  val floor: String = "",
  val depositAmount: Long = 0L,
  val monthlyRent: Long = 0L,
  val buildingAge: String = "",
  val hasLift: Boolean? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

object PropertyMediaLimits {
  const val MIN_PHOTOS = 1
  const val MAX_PHOTOS = 10
  const val MAX_VIDEOS = 1
  const val MAX_TOTAL_BYTES = 8L * 1024L * 1024L
  const val PRESET_ESTIMATED_BYTES = 180_000L
}

@Keep
@IgnoreExtraProperties
data class PropertyContact(
  @DocumentId val id: String = "",
  val ownerId: String = "",
  val contactPhone: String = "",
  val contactEmail: String = "",
  val whatsappEnabled: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class FavoriteRef(
  @DocumentId val id: String = "",
  val userId: String = "",
  val propertyId: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class ContactUnlock(
  @DocumentId val id: String = "",
  val userId: String = "",
  val propertyId: String = "",
  val amountPaid: Long = 1000L,
  val paymentMethod: String = PaymentMethod.UPI_VERIFIED.name,
  val referenceCode: String = "",
  @ServerTimestamp val unlockedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class PremiumMembership(
  @DocumentId val id: String = "",
  val userId: String = "",
  val planName: String = "7-Day Premium Pass",
  val amountPaid: Long = 999L,
  val premiumActive: Boolean = false,
  val referenceCode: String = "",
  val expiresAtMillis: Long = 0L,
  @ServerTimestamp val activatedAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class PropertyChat(
  @DocumentId val id: String = "",
  val propertyId: String = "",
  val propertyTitle: String = "",
  val buyerId: String = "",
  val buyerName: String = "",
  val ownerId: String = "",
  val ownerName: String = "",
  val participantUids: List<String> = emptyList(),
  val lastMessage: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
  @DocumentId val id: String = "",
  val chatId: String = "",
  val senderId: String = "",
  val senderName: String = "",
  val participantUids: List<String> = emptyList(),
  val text: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

object PropertyFormatters {
  private val indianLocale = Locale.Builder().setLanguage("en").setRegion("IN").build()
  private val numberFormatCache =
    object : ThreadLocal<NumberFormat>() {
      override fun initialValue(): NumberFormat = NumberFormat.getNumberInstance(indianLocale)
    }

  private fun formatNumber(value: Long): String =
    numberFormatCache.get()?.format(value) ?: value.toString()

  fun formatInrPrice(price: Long, listingType: String): String {
    val formatted =
      when {
        price >= 10_000_000L -> {
          val cr = price / 10_000_000.0
          String.format(indianLocale, "₹%.2f Cr", cr)
        }
        price >= 100_000L &&
          (listingType == ListingType.SALE.name || listingType == ListingType.HEAVY_DEPOSIT.name) -> {
          val lakhs = price / 100_000.0
          String.format(indianLocale, "₹%.2f L", lakhs)
        }
        else -> "₹${formatNumber(price)}"
      }
    return when (listingType) {
      ListingType.RENT.name -> "$formatted/mo"
      ListingType.HEAVY_DEPOSIT.name -> "$formatted (Deposit)"
      else -> formatted
    }
  }

  fun formatAreaSqFt(areaSqFt: Long): String = "${formatNumber(areaSqFt)} sq.ft"

  fun formatFullInr(price: Long): String = "₹${formatNumber(price)}"

  fun formatCompactPinPrice(price: Long, listingType: String): String {
    val base =
      when {
        price >= 10_000_000L -> {
          val cr = price / 10_000_000.0
          val text = String.format(indianLocale, "%.2f", cr).trimEnd('0').trimEnd('.')
          "₹${text}Cr"
        }
        price >= 100_000L -> {
          val lakhs = price / 100_000.0
          val text = String.format(indianLocale, "%.1f", lakhs).trimEnd('0').trimEnd('.')
          "₹${text}L"
        }
        price >= 1_000L -> {
          val k = price / 1_000.0
          val text = String.format(indianLocale, "%.1f", k).trimEnd('0').trimEnd('.')
          "₹${text}K"
        }
        else -> "₹$price"
      }
    return if (listingType == ListingType.RENT.name) "$base/m" else base
  }

  fun formatPriceRangeLabel(minPrice: Long?, maxPrice: Long?): String {
    return when {
      minPrice == null && maxPrice == null -> "Any Price"
      minPrice == null && maxPrice != null ->
        "Under ${formatCompactPinPrice(maxPrice, ListingType.SALE.name)}"
      minPrice != null && maxPrice == null ->
        "Above ${formatCompactPinPrice(minPrice, ListingType.SALE.name)}"
      minPrice != null && maxPrice != null ->
        "${formatCompactPinPrice(minPrice, ListingType.SALE.name)} – ${formatCompactPinPrice(maxPrice, ListingType.SALE.name)}"
      else -> "Any Price"
    }
  }

  fun maskPhone(phone: String): String {
    val cleaned = phone.trim()
    if (cleaned.length <= 4) return "••••"
    val tail = cleaned.takeLast(3)
    return "+91 ••••••$tail"
  }
}

object PropertySearchMatcher {
  private val priceTokenRegex = Regex("""(\d+(?:\.\d+)?)\s*(cr|crore|crores|l|lac|lacs|lakh|lakhs|k|thousand)?""")

  fun matches(item: PropertyListing, rawQuery: String): Boolean {
    val q = rawQuery.trim().lowercase(Locale.ROOT)
    if (q.isEmpty()) return true

    val categoryLabel = PropertyCategory.fromRaw(item.propertyCategory).label.lowercase(Locale.ROOT)
    val listingLabel = ListingType.fromRaw(item.listingType).label.lowercase(Locale.ROOT)
    val formattedInr = PropertyFormatters.formatInrPrice(item.price, item.listingType).lowercase(Locale.ROOT)
    val compactPrice = PropertyFormatters.formatCompactPinPrice(item.price, item.listingType).lowercase(Locale.ROOT)
    val rawDigits = item.price.toString()

    val matchesText =
      item.title.lowercase(Locale.ROOT).contains(q) ||
        item.location.lowercase(Locale.ROOT).contains(q) ||
        item.description.lowercase(Locale.ROOT).contains(q) ||
        item.ownerName.lowercase(Locale.ROOT).contains(q) ||
        item.floor.lowercase(Locale.ROOT).contains(q) ||
        item.buildingAge.lowercase(Locale.ROOT).contains(q) ||
        (item.hasLift == true && q == "lift") ||
        categoryLabel.contains(q) ||
        listingLabel.contains(q) ||
        formattedInr.contains(q) ||
        compactPrice.contains(q) ||
        rawDigits.contains(q.replace(",", "").replace("₹", "").trim())

    if (matchesText) return true

    // Smart price expression matching: "< 50L", "under 50L", "> 1Cr", "50L - 1Cr", "50L", "35K", "8500000"
    val cleanedPriceQuery = q.replace("₹", "").replace("rs.", "").replace("rs", "").replace(",", "").trim()
    val rangeParts = cleanedPriceQuery.split(Regex("""\s*(?:-|–|to)\s*""")).filter { it.isNotBlank() }
    if (rangeParts.size == 2) {
      val minVal = parseIndianAmount(rangeParts[0])
      val maxVal = parseIndianAmount(rangeParts[1])
      if (minVal != null && maxVal != null) {
        val low = minOf(minVal, maxVal)
        val high = maxOf(minVal, maxVal)
        return item.price in low..high
      }
    }

    val isUnder =
      cleanedPriceQuery.startsWith("<") ||
        cleanedPriceQuery.startsWith("under ") ||
        cleanedPriceQuery.startsWith("below ") ||
        cleanedPriceQuery.startsWith("max ")
    if (isUnder) {
      val maxVal = parseIndianAmount(cleanedPriceQuery.removePrefix("<").removePrefix("under").removePrefix("below").removePrefix("max").trim())
      if (maxVal != null) return item.price <= maxVal
    }

    val isAbove =
      cleanedPriceQuery.startsWith(">") ||
        cleanedPriceQuery.startsWith("above ") ||
        cleanedPriceQuery.startsWith("over ") ||
        cleanedPriceQuery.startsWith("min ")
    if (isAbove) {
      val minVal = parseIndianAmount(cleanedPriceQuery.removePrefix(">").removePrefix("above").removePrefix("over").removePrefix("min").trim())
      if (minVal != null) return item.price >= minVal
    }

    val exactAmount = parseIndianAmount(cleanedPriceQuery)
    if (exactAmount != null) {
      val low = (exactAmount * 0.75).toLong()
      val high = (exactAmount * 1.25).toLong()
      return item.price in low..high
    }

    return false
  }

  fun parseIndianAmount(token: String): Long? {
    val clean = token.trim().lowercase(Locale.ROOT)
    val match = priceTokenRegex.matchEntire(clean) ?: return null
    val number = match.groupValues[1].toDoubleOrNull() ?: return null
    val unit = match.groupValues[2]
    val multiplier =
      when (unit) {
        "cr", "crore", "crores" -> 10_000_000.0
        "l", "lac", "lacs", "lakh", "lakhs" -> 100_000.0
        "k", "thousand" -> 1_000.0
        "" -> if (number >= 1_000.0) 1.0 else return null
        else -> return null
      }
    return (number * multiplier).toLong().takeIf { it > 0L }
  }
}
