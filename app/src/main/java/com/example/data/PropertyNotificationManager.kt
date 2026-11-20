package com.example.data

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.ListingType
import com.example.data.model.PropertyCategory
import com.example.data.model.PropertyChat
import com.example.data.model.PropertyFormatters
import com.example.data.model.PropertyListing
import com.example.data.model.PropertySearchMatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class SavedPropertyAlert(
  val id: String = "alert_${UUID.randomUUID().toString().take(8)}",
  val name: String,
  val listingType: ListingType? = null,
  val category: PropertyCategory? = null,
  val locationQuery: String = "",
  val minPrice: Long? = null,
  val maxPrice: Long? = null,
  val enabled: Boolean = true,
  val createdAtMillis: Long = System.currentTimeMillis(),
)

data class AppNotificationRecord(
  val id: String = "notif_${UUID.randomUUID().toString().take(8)}",
  val type: String, // "PROPERTY_MATCH" or "CHAT_MESSAGE"
  val title: String,
  val message: String,
  val targetId: String,
  val timestampMillis: Long = System.currentTimeMillis(),
  val isRead: Boolean = false,
)

data class NotificationSettings(
  val propertyAlertsEnabled: Boolean = true,
  val chatAlertsEnabled: Boolean = true,
)

data class NotificationDeepLink(
  val type: String, // "PROPERTY" or "CHAT"
  val targetId: String,
)

object PropertyAlertMatcher {
  fun matches(property: PropertyListing, alert: SavedPropertyAlert): Boolean {
    if (!alert.enabled) return false
    val matchesType = alert.listingType == null || property.listingType == alert.listingType.name
    val matchesCategory = alert.category == null || property.propertyCategory == alert.category.name
    val matchesMin = alert.minPrice == null || property.price >= alert.minPrice
    val matchesMax = alert.maxPrice == null || property.price <= alert.maxPrice
    val matchesLocation =
      alert.locationQuery.isBlank() || PropertySearchMatcher.matches(property, alert.locationQuery)
    return matchesType && matchesCategory && matchesMin && matchesMax && matchesLocation
  }

  fun buildReadableSummary(alert: SavedPropertyAlert): String {
    val parts = mutableListOf<String>()
    alert.listingType?.let { parts.add(it.label) }
    alert.category?.let { parts.add(it.label) }
    if (alert.locationQuery.isNotBlank()) parts.add(alert.locationQuery.trim())
    if (alert.minPrice != null || alert.maxPrice != null) {
      parts.add(PropertyFormatters.formatPriceRangeLabel(alert.minPrice, alert.maxPrice))
    }
    return if (parts.isEmpty()) "All new properties" else parts.joinToString(" • ")
  }

  fun buildDefaultName(
    listingType: ListingType?,
    category: PropertyCategory?,
    locationQuery: String,
    minPrice: Long?,
    maxPrice: Long?,
  ): String {
    val parts = mutableListOf<String>()
    if (locationQuery.isNotBlank()) parts.add(locationQuery.trim())
    category?.let { parts.add(it.label) }
    listingType?.let { parts.add(it.label) }
    if (minPrice != null || maxPrice != null) {
      parts.add(PropertyFormatters.formatPriceRangeLabel(minPrice, maxPrice))
    }
    return if (parts.isEmpty()) "All New Listings Alert" else parts.take(3).joinToString(" • ")
  }
}

class PropertyLocalNotifier(private val context: Context) {
  private val appContext = context.applicationContext

  init {
    ensureChannels()
  }

  fun ensureChannels() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      ?: return

    val propertyChannel =
      NotificationChannel(
        CHANNEL_PROPERTY_ALERTS,
        appContext.getString(R.string.notification_channel_property_name),
        NotificationManager.IMPORTANCE_HIGH,
      ).apply {
        description = appContext.getString(R.string.notification_channel_property_desc)
      }

    val chatChannel =
      NotificationChannel(
        CHANNEL_CHAT_MESSAGES,
        appContext.getString(R.string.notification_channel_chat_name),
        NotificationManager.IMPORTANCE_HIGH,
      ).apply {
        description = appContext.getString(R.string.notification_channel_chat_desc)
      }

    manager.createNotificationChannel(propertyChannel)
    manager.createNotificationChannel(chatChannel)
  }

  fun hasPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
  }

  @SuppressLint("MissingPermission")
  fun notifyMatchingProperty(
    property: PropertyListing,
    matchedAlerts: List<SavedPropertyAlert>,
  ): Boolean {
    ensureChannels()
    if (!hasPermission()) return false

    val formattedPrice = PropertyFormatters.formatInrPrice(property.price, property.listingType)
    val matchedName = matchedAlerts.firstOrNull()?.name ?: "Saved Alert"
    val title = "New Match: ${property.title}"
    val body = "$formattedPrice • ${property.location} (Matches $matchedName)"

    val intent =
      Intent(appContext, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(EXTRA_DEEP_LINK_TYPE, DEEP_LINK_PROPERTY)
        putExtra(EXTRA_DEEP_LINK_ID, property.id)
      }
    val requestCode = ("prop_${property.id}").hashCode()
    val pendingIntent =
      PendingIntent.getActivity(
        appContext,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val notification =
      NotificationCompat.Builder(appContext, CHANNEL_PROPERTY_ALERTS)
        .setSmallIcon(R.drawable.ic_notification_property)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .build()

    NotificationManagerCompat.from(appContext).notify(requestCode, notification)
    return true
  }

  @SuppressLint("MissingPermission")
  fun notifyNewChatMessage(
    chat: PropertyChat,
    senderName: String,
    messageText: String,
  ): Boolean {
    ensureChannels()
    if (!hasPermission()) return false

    val title = "New message from $senderName"
    val body =
      if (chat.propertyTitle.isNotBlank()) "${chat.propertyTitle}: $messageText"
      else messageText

    val intent =
      Intent(appContext, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(EXTRA_DEEP_LINK_TYPE, DEEP_LINK_CHAT)
        putExtra(EXTRA_DEEP_LINK_ID, chat.id)
      }
    val requestCode = ("chat_${chat.id}").hashCode()
    val pendingIntent =
      PendingIntent.getActivity(
        appContext,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val notification =
      NotificationCompat.Builder(appContext, CHANNEL_CHAT_MESSAGES)
        .setSmallIcon(R.drawable.ic_notification_property)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .build()

    NotificationManagerCompat.from(appContext).notify(requestCode, notification)
    return true
  }

  companion object {
    const val CHANNEL_PROPERTY_ALERTS = "property_match_alerts"
    const val CHANNEL_CHAT_MESSAGES = "chat_message_alerts"
    const val EXTRA_DEEP_LINK_TYPE = "extra_deep_link_type"
    const val EXTRA_DEEP_LINK_ID = "extra_deep_link_id"
    const val DEEP_LINK_PROPERTY = "PROPERTY"
    const val DEEP_LINK_CHAT = "CHAT"

    fun extractDeepLink(intent: Intent?): NotificationDeepLink? {
      val type = intent?.getStringExtra(EXTRA_DEEP_LINK_TYPE)?.takeIf { it.isNotBlank() } ?: return null
      val id = intent.getStringExtra(EXTRA_DEEP_LINK_ID)?.takeIf { it.isNotBlank() } ?: return null
      return NotificationDeepLink(type = type, targetId = id)
    }
  }
}

class LocalNotificationStore(context: Context) {
  private val appContext = context.applicationContext
  private val prefs =
    appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  val notifier = PropertyLocalNotifier(appContext)

  private val _savedAlerts = MutableStateFlow(loadSavedAlerts())
  val savedAlerts: StateFlow<List<SavedPropertyAlert>> = _savedAlerts.asStateFlow()

  private val _settings = MutableStateFlow(loadSettings())
  val settings: StateFlow<NotificationSettings> = _settings.asStateFlow()

  private val _recentAlerts = MutableStateFlow(loadRecentAlerts())
  val recentAlerts: StateFlow<List<AppNotificationRecord>> = _recentAlerts.asStateFlow()

  // Session baselines so existing listings/chats never spam on first load
  private var propertiesInitialized = false
  private val seenPropertyIds = mutableSetOf<String>()

  private var chatsInitialized = false
  private val lastChatSnapshots = mutableMapOf<String, String>()
  private val locallySentSignatures = mutableMapOf<String, String>()

  fun resetWatchBaselines() {
    propertiesInitialized = false
    seenPropertyIds.clear()
    chatsInitialized = false
    lastChatSnapshots.clear()
    locallySentSignatures.clear()
  }

  fun saveAlert(alert: SavedPropertyAlert) {
    val updated = listOf(alert) + _savedAlerts.value.filterNot { it.id == alert.id }
    _savedAlerts.value = updated
    persistSavedAlerts(updated)
  }

  fun toggleAlert(alertId: String) {
    val updated =
      _savedAlerts.value.map { if (it.id == alertId) it.copy(enabled = !it.enabled) else it }
    _savedAlerts.value = updated
    persistSavedAlerts(updated)
  }

  fun deleteAlert(alertId: String) {
    val updated = _savedAlerts.value.filterNot { it.id == alertId }
    _savedAlerts.value = updated
    persistSavedAlerts(updated)
  }

  fun setPropertyAlertsEnabled(enabled: Boolean) {
    val updated = _settings.value.copy(propertyAlertsEnabled = enabled)
    _settings.value = updated
    persistSettings(updated)
  }

  fun setChatAlertsEnabled(enabled: Boolean) {
    val updated = _settings.value.copy(chatAlertsEnabled = enabled)
    _settings.value = updated
    persistSettings(updated)
  }

  fun markAllRead() {
    val updated = _recentAlerts.value.map { it.copy(isRead = true) }
    _recentAlerts.value = updated
    persistRecentAlerts(updated)
  }

  fun markRead(recordId: String) {
    val updated =
      _recentAlerts.value.map { if (it.id == recordId) it.copy(isRead = true) else it }
    _recentAlerts.value = updated
    persistRecentAlerts(updated)
  }

  fun clearHistory() {
    _recentAlerts.value = emptyList()
    persistRecentAlerts(emptyList())
  }

  fun markLocallySentMessage(chatId: String, text: String) {
    locallySentSignatures[chatId] = text.trim()
  }

  /**
   * Observes the property list stream. On first call, baselines existing IDs.
   * On subsequent calls, fires local notifications for newly added properties matching
   * any enabled [SavedPropertyAlert].
   * Returns newly created [AppNotificationRecord] entries (for optional in-app snackbars).
   */
  fun processPropertiesSnapshot(properties: List<PropertyListing>): List<AppNotificationRecord> {
    if (!propertiesInitialized) {
      seenPropertyIds.addAll(properties.map { it.id })
      propertiesInitialized = true
      return emptyList()
    }

    val newlyAdded = properties.filter { it.id.isNotBlank() && it.id !in seenPropertyIds }
    seenPropertyIds.addAll(properties.map { it.id })
    if (newlyAdded.isEmpty() || !_settings.value.propertyAlertsEnabled) return emptyList()

    val activeAlerts = _savedAlerts.value.filter { it.enabled }
    if (activeAlerts.isEmpty()) return emptyList()

    val triggered = mutableListOf<AppNotificationRecord>()
    newlyAdded.forEach { prop ->
      val matched = activeAlerts.filter { PropertyAlertMatcher.matches(prop, it) }
      if (matched.isNotEmpty()) {
        notifier.notifyMatchingProperty(prop, matched)
        val formattedPrice = PropertyFormatters.formatInrPrice(prop.price, prop.listingType)
        val record =
          AppNotificationRecord(
            type = TYPE_PROPERTY_MATCH,
            title = "New Match: ${prop.title}",
            message = "$formattedPrice • ${prop.location} (Matches ${matched.first().name})",
            targetId = prop.id,
          )
        triggered.add(record)
      }
    }

    if (triggered.isNotEmpty()) {
      appendRecords(triggered)
    }
    return triggered
  }

  /**
   * Observes the user's chats stream. On first call, baselines current chat lastMessages.
   * On subsequent calls, fires local notifications when a chat receives a new message
   * that was not sent locally by this device and is not currently open.
   */
  fun processChatsSnapshot(
    chats: List<PropertyChat>,
    currentUserId: String,
    activeChatId: String? = null,
  ): List<AppNotificationRecord> {
    if (!chatsInitialized) {
      chats.forEach { chat ->
        lastChatSnapshots[chat.id] = chatSignature(chat)
      }
      chatsInitialized = true
      return emptyList()
    }

    val triggered = mutableListOf<AppNotificationRecord>()
    chats.forEach { chat ->
      val signature = chatSignature(chat)
      val previous = lastChatSnapshots[chat.id]
      lastChatSnapshots[chat.id] = signature

      val isChanged = previous != signature
      val trimmedMessage = chat.lastMessage.trim()
      val wasLocalSend = locallySentSignatures[chat.id] == trimmedMessage
      if (wasLocalSend) {
        locallySentSignatures.remove(chat.id)
      }

      val isSelfStartedInquiry =
        previous == null && chat.buyerId == currentUserId && chat.ownerId != currentUserId

      if (
        isChanged &&
          !wasLocalSend &&
          !isSelfStartedInquiry &&
          trimmedMessage.isNotEmpty() &&
          chat.id != activeChatId &&
          _settings.value.chatAlertsEnabled
      ) {
        val otherParty =
          if (chat.buyerId == currentUserId) chat.ownerName.ifBlank { "Property Owner" }
          else chat.buyerName.ifBlank { "Property Buyer" }
        notifier.notifyNewChatMessage(chat, otherParty, trimmedMessage)
        val body =
          if (chat.propertyTitle.isNotBlank()) "${chat.propertyTitle}: $trimmedMessage"
          else trimmedMessage
        triggered.add(
          AppNotificationRecord(
            type = TYPE_CHAT_MESSAGE,
            title = "New message from $otherParty",
            message = body,
            targetId = chat.id,
          )
        )
      }
    }

    if (triggered.isNotEmpty()) {
      appendRecords(triggered)
    }
    return triggered
  }

  /**
   * Direct helper to trigger a chat notification when a remote message arrives on a thread.
   */
  fun notifyIncomingChatMessage(
    chat: PropertyChat,
    senderName: String,
    messageText: String,
  ): AppNotificationRecord? {
    val clean = messageText.trim()
    if (clean.isEmpty() || !_settings.value.chatAlertsEnabled) return null
    notifier.notifyNewChatMessage(chat, senderName, clean)
    val body =
      if (chat.propertyTitle.isNotBlank()) "${chat.propertyTitle}: $clean" else clean
    val record =
      AppNotificationRecord(
        type = TYPE_CHAT_MESSAGE,
        title = "New message from $senderName",
        message = body,
        targetId = chat.id,
      )
    appendRecords(listOf(record))
    return record
  }

  private fun appendRecords(newRecords: List<AppNotificationRecord>) {
    val updated = (newRecords + _recentAlerts.value).take(MAX_HISTORY_ITEMS)
    _recentAlerts.value = updated
    persistRecentAlerts(updated)
  }

  private fun chatSignature(chat: PropertyChat): String {
    val ts = chat.updatedAt?.seconds ?: 0L
    return "${chat.lastMessage.trim()}|$ts"
  }

  private fun loadSettings(): NotificationSettings =
    NotificationSettings(
      propertyAlertsEnabled = prefs.getBoolean(KEY_PROP_ALERTS_ENABLED, true),
      chatAlertsEnabled = prefs.getBoolean(KEY_CHAT_ALERTS_ENABLED, true),
    )

  private fun persistSettings(settings: NotificationSettings) {
    prefs
      .edit()
      .putBoolean(KEY_PROP_ALERTS_ENABLED, settings.propertyAlertsEnabled)
      .putBoolean(KEY_CHAT_ALERTS_ENABLED, settings.chatAlertsEnabled)
      .apply()
  }

  private fun loadSavedAlerts(): List<SavedPropertyAlert> {
    val raw = prefs.getString(KEY_SAVED_ALERTS, null) ?: return emptyList()
    return runCatching {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.getJSONObject(i)
          val typeRaw = obj.optString("listingType", "")
          val catRaw = obj.optString("category", "")
          add(
            SavedPropertyAlert(
              id = obj.optString("id", "alert_$i"),
              name = obj.optString("name", "Saved Alert"),
              listingType = if (typeRaw.isNotBlank()) ListingType.fromRaw(typeRaw) else null,
              category = if (catRaw.isNotBlank()) PropertyCategory.fromRaw(catRaw) else null,
              locationQuery = obj.optString("locationQuery", ""),
              minPrice = if (obj.has("minPrice") && !obj.isNull("minPrice")) obj.getLong("minPrice") else null,
              maxPrice = if (obj.has("maxPrice") && !obj.isNull("maxPrice")) obj.getLong("maxPrice") else null,
              enabled = obj.optBoolean("enabled", true),
              createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis()),
            )
          )
        }
      }
    }.getOrDefault(emptyList())
  }

  private fun persistSavedAlerts(alerts: List<SavedPropertyAlert>) {
    val arr = JSONArray()
    alerts.forEach { alert ->
      arr.put(
        JSONObject().apply {
          put("id", alert.id)
          put("name", alert.name)
          put("listingType", alert.listingType?.name ?: "")
          put("category", alert.category?.name ?: "")
          put("locationQuery", alert.locationQuery)
          if (alert.minPrice != null) put("minPrice", alert.minPrice) else put("minPrice", JSONObject.NULL)
          if (alert.maxPrice != null) put("maxPrice", alert.maxPrice) else put("maxPrice", JSONObject.NULL)
          put("enabled", alert.enabled)
          put("createdAtMillis", alert.createdAtMillis)
        }
      )
    }
    prefs.edit().putString(KEY_SAVED_ALERTS, arr.toString()).apply()
  }

  private fun loadRecentAlerts(): List<AppNotificationRecord> {
    val raw = prefs.getString(KEY_RECENT_ALERTS, null) ?: return emptyList()
    return runCatching {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.getJSONObject(i)
          add(
            AppNotificationRecord(
              id = obj.optString("id", "notif_$i"),
              type = obj.optString("type", TYPE_PROPERTY_MATCH),
              title = obj.optString("title", ""),
              message = obj.optString("message", ""),
              targetId = obj.optString("targetId", ""),
              timestampMillis = obj.optLong("timestampMillis", System.currentTimeMillis()),
              isRead = obj.optBoolean("isRead", false),
            )
          )
        }
      }
    }.getOrDefault(emptyList())
  }

  private fun persistRecentAlerts(records: List<AppNotificationRecord>) {
    val arr = JSONArray()
    records.forEach { r ->
      arr.put(
        JSONObject().apply {
          put("id", r.id)
          put("type", r.type)
          put("title", r.title)
          put("message", r.message)
          put("targetId", r.targetId)
          put("timestampMillis", r.timestampMillis)
          put("isRead", r.isRead)
        }
      )
    }
    prefs.edit().putString(KEY_RECENT_ALERTS, arr.toString()).apply()
  }

  companion object {
    const val TYPE_PROPERTY_MATCH = "PROPERTY_MATCH"
    const val TYPE_CHAT_MESSAGE = "CHAT_MESSAGE"
    private const val PREFS_NAME = "property_broker_local_notifications"
    private const val KEY_SAVED_ALERTS = "saved_property_alerts_json"
    private const val KEY_RECENT_ALERTS = "recent_alert_records_json"
    private const val KEY_PROP_ALERTS_ENABLED = "property_alerts_enabled"
    private const val KEY_CHAT_ALERTS_ENABLED = "chat_alerts_enabled"
    private const val MAX_HISTORY_ITEMS = 40
  }
}
