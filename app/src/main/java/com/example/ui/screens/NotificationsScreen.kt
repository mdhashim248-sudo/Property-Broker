package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.AppNotificationRecord
import com.example.data.LocalNotificationStore
import com.example.data.NotificationSettings
import com.example.data.PropertyAlertMatcher
import com.example.data.SavedPropertyAlert
import com.example.data.model.ListingType
import com.example.data.model.PriceBracketPreset
import com.example.data.model.PropertyCategory
import com.example.ui.theme.BrokerBlue
import com.example.ui.theme.BrokerGreen
import com.example.ui.theme.BrokerIndigo
import com.example.ui.theme.BrokerPurple
import com.example.ui.theme.BrokerRed
import com.example.ui.theme.BrokerTeal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NotificationsScreen(
  savedAlerts: List<SavedPropertyAlert>,
  settings: NotificationSettings,
  recentAlerts: List<AppNotificationRecord>,
  isPremiumActive: Boolean,
  showPremiumOption: Boolean = com.example.data.PremiumSchedule.isPremiumFeatureEnabled(),
  onTogglePropertyAlerts: (Boolean) -> Unit,
  onToggleChatAlerts: (Boolean) -> Unit,
  onSaveAlert: (SavedPropertyAlert) -> Unit,
  onToggleAlertItem: (String) -> Unit,
  onDeleteAlertItem: (String) -> Unit,
  onMarkAllRead: () -> Unit,
  onClearHistory: () -> Unit,
  onOpenRecord: (AppNotificationRecord) -> Unit,
  onBack: () -> Unit,
  onOpenHome: () -> Unit,
  onOpenProfile: () -> Unit,
  onOpenPremium: () -> Unit,
  onOpenTerms: () -> Unit,
  onSignOut: () -> Unit,
) {
  val context = LocalContext.current
  val requiresRuntimePermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

  var hasNotificationPermission by remember {
    mutableStateOf(
      !requiresRuntimePermission ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
          PackageManager.PERMISSION_GRANTED
    )
  }
  var permissionDeniedOnce by remember { mutableStateOf(false) }

  val permissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      hasNotificationPermission = granted
      if (!granted) permissionDeniedOnce = true
    }

  var showCreateDialog by remember { mutableStateOf(false) }

  if (showCreateDialog) {
    CreateAlertDialog(
      onDismiss = { showCreateDialog = false },
      onSave = { newAlert ->
        onSaveAlert(newAlert)
        showCreateDialog = false
      },
    )
  }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = stringResource(R.string.alerts_screen_title),
            fontWeight = FontWeight.Bold,
          )
        },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          AppOverflowMenu(
            isPremiumActive = isPremiumActive,
            showPremiumOption = showPremiumOption,
            onOpenHome = onOpenHome,
            onOpenProfile = onOpenProfile,
            onOpenPremium = onOpenPremium,
            onOpenTerms = onOpenTerms,
            onSignOut = onSignOut,
            menuButtonTestTag = "alerts_overflow_menu",
          )
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = BrokerIndigo,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White,
          ),
      )
    },
  ) { innerPadding ->
    Box(
      contentAlignment = Alignment.TopCenter,
      modifier = Modifier.fillMaxSize().padding(innerPadding),
    ) {
      Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier =
          Modifier.widthIn(max = 580.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
      ) {
        // Android 13+ Runtime Notification Permission Card
        if (requiresRuntimePermission && !hasNotificationPermission) {
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = BrokerPurple.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth().testTag("notification_permission_card"),
          ) {
            Column(
              verticalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.NotificationsOff,
                  contentDescription = null,
                  tint = BrokerPurple,
                )
                Text(
                  text = "Allow System Notifications",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = BrokerIndigo,
                )
              }
              Text(
                text =
                  if (permissionDeniedOnce)
                    "Notifications are currently blocked. Open Android Settings to receive instant alerts for new matching properties and incoming chats."
                  else
                    "Enable notifications so Property Broker can alert you in your status bar when a matching property is posted or a new chat message arrives.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                  onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                      permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = BrokerPurple),
                  modifier = Modifier.testTag("request_notification_permission_button"),
                ) {
                  Text("Allow Notifications", fontWeight = FontWeight.SemiBold)
                }
                if (permissionDeniedOnce) {
                  OutlinedButton(
                    onClick = {
                      val intent =
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                          putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                      runCatching { context.startActivity(intent) }
                    }
                  ) {
                    Text("Settings")
                  }
                }
              }
            }
          }
        }

        // Notification Toggles Card
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
              text = "Notification Preferences",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = BrokerIndigo,
            )
            Spacer(Modifier.height(10.dp))
            NotificationToggleRow(
              icon = Icons.Filled.HomeWork,
              iconTint = BrokerBlue,
              title = "Matching Property Alerts",
              subtitle = "Notify when a newly posted property matches your saved criteria",
              checked = settings.propertyAlertsEnabled,
              onCheckedChange = onTogglePropertyAlerts,
              switchTag = "toggle_property_alerts_switch",
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
            NotificationToggleRow(
              icon = Icons.Filled.Forum,
              iconTint = BrokerGreen,
              title = "New Chat Messages",
              subtitle = "Notify when you receive a new buyer or owner message",
              checked = settings.chatAlertsEnabled,
              onCheckedChange = onToggleChatAlerts,
              switchTag = "toggle_chat_alerts_switch",
            )
          }
        }

        // Saved Property Alert Criteria Section
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Saved Property Criteria",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = BrokerIndigo,
                )
                Text(
                  text = "${savedAlerts.count { it.enabled }} active alert rules",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = BrokerIndigo),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("add_alert_criteria_button"),
              ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add Alert", fontWeight = FontWeight.SemiBold)
              }
            }

            if (savedAlerts.isEmpty()) {
              Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(6.dp),
                  modifier = Modifier.fillMaxWidth().padding(16.dp),
                ) {
                  Icon(
                    imageVector = Icons.Filled.NotificationsActive,
                    contentDescription = null,
                    tint = BrokerIndigo,
                    modifier = Modifier.size(28.dp),
                  )
                  Text(
                    text = "No saved criteria yet",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                  )
                  Text(
                    text = "Tap Add Alert above or Save Alert while browsing properties to get notified when a match is posted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            } else {
              savedAlerts.forEachIndexed { idx, alert ->
                if (idx > 0) HorizontalDivider()
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                  modifier = Modifier.fillMaxWidth().testTag("saved_alert_item_${alert.id}"),
                ) {
                  Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.weight(1f),
                  ) {
                    Text(
                      text = alert.name,
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                      text = PropertyAlertMatcher.buildReadableSummary(alert),
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  Switch(
                    checked = alert.enabled,
                    onCheckedChange = { onToggleAlertItem(alert.id) },
                    colors = SwitchDefaults.colors(checkedTrackColor = BrokerGreen),
                  )
                  IconButton(
                    onClick = { onDeleteAlertItem(alert.id) },
                    modifier = Modifier.testTag("delete_alert_${alert.id}"),
                  ) {
                    Icon(
                      imageVector = Icons.Filled.Delete,
                      contentDescription = "Delete alert",
                      tint = BrokerRed,
                    )
                  }
                }
              }
            }
          }
        }

        // Recent Notifications Inbox
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = "Recent Alerts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = BrokerIndigo,
              )
              if (recentAlerts.isNotEmpty()) {
                Row {
                  if (recentAlerts.any { !it.isRead }) {
                    TextButton(onClick = onMarkAllRead) {
                      Text("Mark Read", fontSize = 12.sp)
                    }
                  }
                  TextButton(onClick = onClearHistory) {
                    Text("Clear", fontSize = 12.sp, color = BrokerRed)
                  }
                }
              }
            }

            if (recentAlerts.isEmpty()) {
              Text(
                text = "No alerts received yet. New matching properties and incoming chat messages will appear here and in your Android notification bar.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            } else {
              val timeFmt = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
              recentAlerts.forEachIndexed { idx, record ->
                if (idx > 0) HorizontalDivider()
                val isChat = record.type == LocalNotificationStore.TYPE_CHAT_MESSAGE
                Row(
                  verticalAlignment = Alignment.Top,
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                  modifier =
                    Modifier.fillMaxWidth()
                      .clickable { onOpenRecord(record) }
                      .padding(vertical = 6.dp)
                      .testTag("alert_record_${record.id}"),
                ) {
                  Surface(
                    color = (if (isChat) BrokerGreen else BrokerTeal).copy(alpha = 0.14f),
                    shape = CircleShape,
                    modifier = Modifier.size(38.dp),
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        imageVector = if (isChat) Icons.Filled.Forum else Icons.Filled.Notifications,
                        contentDescription = null,
                        tint = if (isChat) BrokerGreen else BrokerTeal,
                        modifier = Modifier.size(20.dp),
                      )
                    }
                  }
                  Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.weight(1f),
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                      Text(
                        text = record.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (record.isRead) FontWeight.Medium else FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                      )
                      if (!record.isRead) {
                        Box(
                          modifier =
                            Modifier.size(8.dp).background(BrokerRed, CircleShape)
                        )
                      }
                    }
                    Text(
                      text = record.message,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                      text = timeFmt.format(Date(record.timestampMillis)),
                      fontSize = 11.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    )
                  }
                }
              }
            }
          }
        }

        Spacer(Modifier.height(16.dp))
      }
    }
  }
}

@Composable
private fun NotificationToggleRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  iconTint: Color,
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  switchTag: String,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Surface(
      color = iconTint.copy(alpha = 0.12f),
      shape = CircleShape,
      modifier = Modifier.size(40.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = iconTint,
          modifier = Modifier.size(20.dp),
        )
      }
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = SwitchDefaults.colors(checkedTrackColor = BrokerIndigo),
      modifier = Modifier.testTag(switchTag),
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateAlertDialog(
  onDismiss: () -> Unit,
  onSave: (SavedPropertyAlert) -> Unit,
) {
  var nameInput by remember { mutableStateOf("") }
  var selectedListingType by remember { mutableStateOf<ListingType?>(null) }
  var selectedCategory by remember { mutableStateOf<PropertyCategory?>(null) }
  var locationInput by remember { mutableStateOf("") }
  var minPriceInput by remember { mutableStateOf("") }
  var maxPriceInput by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "Create Property Alert",
        fontWeight = FontWeight.Bold,
        color = BrokerIndigo,
      )
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.verticalScroll(rememberScrollState()).testTag("create_alert_dialog"),
      ) {
        OutlinedTextField(
          value = nameInput,
          onValueChange = { nameInput = it },
          label = { Text("Alert Name (optional)") },
          placeholder = { Text("e.g. Bandra 2BHK under 1Cr") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("alert_name_input"),
        )

        OutlinedTextField(
          value = locationInput,
          onValueChange = { locationInput = it },
          label = { Text("Location or Keyword") },
          placeholder = { Text("e.g. Bandra, Andheri, Villa") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("alert_location_input"),
        )

        Text("Listing Type", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = BrokerIndigo)
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          FilterChip(
            selected = selectedListingType == null,
            onClick = { selectedListingType = null },
            label = { Text("Any") },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = BrokerIndigo,
              selectedLabelColor = Color.White,
            ),
          )
          ListingType.entries.forEach { type ->
            FilterChip(
              selected = selectedListingType == type,
              onClick = { selectedListingType = type },
              label = { Text(type.label) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = BrokerIndigo,
                selectedLabelColor = Color.White,
              ),
            )
          }
        }

        Text("Property Type", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = BrokerIndigo)
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          FilterChip(
            selected = selectedCategory == null,
            onClick = { selectedCategory = null },
            label = { Text("Any") },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = BrokerIndigo,
              selectedLabelColor = Color.White,
            ),
          )
          PropertyCategory.entries.forEach { cat ->
            FilterChip(
              selected = selectedCategory == cat,
              onClick = { selectedCategory = cat },
              label = { Text(cat.label) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = BrokerIndigo,
                selectedLabelColor = Color.White,
              ),
            )
          }
        }

        Text("Price Range", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = BrokerIndigo)
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          PriceBracketPreset.entries.forEach { preset ->
            val selected =
              minPriceInput.toLongOrNull() == preset.minPrice &&
                maxPriceInput.toLongOrNull() == preset.maxPrice
            FilterChip(
              selected = selected,
              onClick = {
                minPriceInput = preset.minPrice?.toString() ?: ""
                maxPriceInput = preset.maxPrice?.toString() ?: ""
              },
              label = { Text(preset.label) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = BrokerIndigo,
                selectedLabelColor = Color.White,
              ),
            )
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = minPriceInput,
            onValueChange = { minPriceInput = it.filter(Char::isDigit) },
            label = { Text("Min (₹)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = maxPriceInput,
            onValueChange = { maxPriceInput = it.filter(Char::isDigit) },
            label = { Text("Max (₹)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val minVal = minPriceInput.toLongOrNull()
          val maxVal = maxPriceInput.toLongOrNull()
          val cleanName =
            nameInput.trim().ifBlank {
              PropertyAlertMatcher.buildDefaultName(
                listingType = selectedListingType,
                category = selectedCategory,
                locationQuery = locationInput,
                minPrice = minVal,
                maxPrice = maxVal,
              )
            }
          onSave(
            SavedPropertyAlert(
              name = cleanName,
              listingType = selectedListingType,
              category = selectedCategory,
              locationQuery = locationInput.trim(),
              minPrice = minVal,
              maxPrice = maxVal,
              enabled = true,
            )
          )
        },
        colors = ButtonDefaults.buttonColors(containerColor = BrokerIndigo),
        modifier = Modifier.testTag("save_alert_confirm_button"),
      ) {
        Text("Save Alert", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    },
  )
}
