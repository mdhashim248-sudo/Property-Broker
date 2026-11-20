package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.PremiumSchedule
import com.example.data.model.PaymentMethod
import com.example.data.model.PremiumMembership
import com.example.data.model.UserRole
import com.example.ui.theme.BrokerAmber
import com.example.ui.theme.BrokerBlue
import com.example.ui.theme.BrokerDeepPurple
import com.example.ui.theme.BrokerGreen
import com.example.ui.theme.BrokerIndigo
import com.example.ui.theme.BrokerPurple
import com.example.ui.theme.BrokerRed
import com.example.ui.theme.BrokerTeal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AppOverflowMenu(
  isPremiumActive: Boolean = false,
  showPremiumOption: Boolean = PremiumSchedule.isPremiumFeatureEnabled(),
  onOpenHome: () -> Unit,
  onOpenProfile: () -> Unit,
  onOpenPremium: () -> Unit,
  onOpenTerms: () -> Unit,
  onSignOut: () -> Unit,
  menuButtonTestTag: String = "home_overflow_menu",
) {
  var showMenu by remember { mutableStateOf(false) }

  Box {
    IconButton(
      onClick = { showMenu = true },
      modifier = Modifier.testTag(menuButtonTestTag),
    ) {
      Icon(
        imageVector = Icons.Filled.MoreVert,
        contentDescription = "More options",
      )
    }
    DropdownMenu(
      expanded = showMenu,
      onDismissRequest = { showMenu = false },
    ) {
      DropdownMenuItem(
        text = { Text(stringResource(R.string.menu_home), fontWeight = FontWeight.Medium) },
        leadingIcon = {
          Icon(
            imageVector = Icons.Filled.Home,
            contentDescription = null,
            tint = BrokerIndigo,
          )
        },
        onClick = {
          showMenu = false
          onOpenHome()
        },
        modifier = Modifier.testTag("menu_home_item"),
      )
      DropdownMenuItem(
        text = { Text(stringResource(R.string.menu_profile), fontWeight = FontWeight.Medium) },
        leadingIcon = {
          Icon(
            imageVector = Icons.Filled.Person,
            contentDescription = null,
            tint = BrokerBlue,
          )
        },
        onClick = {
          showMenu = false
          onOpenProfile()
        },
        modifier = Modifier.testTag("menu_profile_item"),
      )
      if (showPremiumOption) {
        DropdownMenuItem(
          text = {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Text(stringResource(R.string.menu_premium), fontWeight = FontWeight.Medium)
              if (isPremiumActive) {
                Surface(
                  color = BrokerGreen.copy(alpha = 0.16f),
                  shape = RoundedCornerShape(6.dp),
                ) {
                  Text(
                    text = "ACTIVE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrokerGreen,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  )
                }
              }
            }
          },
          leadingIcon = {
            Icon(
              imageVector = Icons.Filled.WorkspacePremium,
              contentDescription = null,
              tint = BrokerPurple,
            )
          },
          onClick = {
            showMenu = false
            onOpenPremium()
          },
          modifier = Modifier.testTag("menu_premium_item"),
        )
      }
      DropdownMenuItem(
        text = {
          Text(
            stringResource(R.string.menu_terms_conditions),
            fontWeight = FontWeight.Medium,
          )
        },
        leadingIcon = {
          Icon(
            imageVector = Icons.Filled.Description,
            contentDescription = null,
            tint = BrokerTeal,
          )
        },
        onClick = {
          showMenu = false
          onOpenTerms()
        },
        modifier = Modifier.testTag("menu_terms_item"),
      )

      HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

      DropdownMenuItem(
        text = { Text("Logout", color = BrokerRed) },
        leadingIcon = {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Logout,
            contentDescription = null,
            tint = BrokerRed,
          )
        },
        onClick = {
          showMenu = false
          onSignOut()
        },
        modifier = Modifier.testTag("menu_logout_item"),
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
  displayName: String,
  email: String,
  currentRole: UserRole,
  isPremiumActive: Boolean,
  showPremiumOption: Boolean = PremiumSchedule.isPremiumFeatureEnabled(),
  myListingsCount: Int,
  favoriteCount: Int,
  unlockedCount: Int,
  activeAlertsCount: Int = 0,
  isDeletingAccount: Boolean = false,
  onSelectRole: (UserRole) -> Unit,
  onBack: () -> Unit,
  onOpenHome: () -> Unit,
  onOpenPremium: () -> Unit,
  onOpenAlerts: (() -> Unit)? = null,
  onOpenTerms: () -> Unit,
  onOpenFavorites: () -> Unit,
  onSignOut: () -> Unit,
  onDeleteAccount: () -> Unit = {},
) {
  var showDeleteDialog by remember { mutableStateOf(false) }

  if (showDeleteDialog) {
    AlertDialog(
      onDismissRequest = { if (!isDeletingAccount) showDeleteDialog = false },
      title = { Text("Delete Account?", fontWeight = FontWeight.Bold, color = BrokerRed) },
      text = {
        Text(
          "This permanently deletes your Property Broker profile, your active listings, saved favorites, and local alerts. This action cannot be undone."
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showDeleteDialog = false
            onDeleteAccount()
          },
          enabled = !isDeletingAccount,
          colors = ButtonDefaults.buttonColors(containerColor = BrokerRed),
          modifier = Modifier.testTag("confirm_delete_account_button"),
        ) {
          Text("Delete Permanently", color = Color.White, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(
          onClick = { showDeleteDialog = false },
          enabled = !isDeletingAccount,
        ) {
          Text("Cancel")
        }
      },
    )
  }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.menu_profile), fontWeight = FontWeight.Bold) },
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
            onOpenProfile = {},
            onOpenPremium = onOpenPremium,
            onOpenTerms = onOpenTerms,
            onSignOut = onSignOut,
            menuButtonTestTag = "profile_overflow_menu",
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
          Modifier.widthIn(max = 560.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
      ) {
        // Hero Profile Header Card
        Card(
          shape = RoundedCornerShape(16.dp),
          elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
          modifier = Modifier.fillMaxWidth().testTag("profile_header_card"),
        ) {
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(BrokerIndigo, BrokerBlue)))
                .padding(20.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
              Surface(
                color = Color.White.copy(alpha = 0.2f),
                shape = CircleShape,
                modifier = Modifier.size(64.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(
                    text = displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "P",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                  )
                }
              }
              Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f),
              ) {
                Text(
                  text = displayName,
                  fontSize = 20.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.White,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = email.ifBlank { "Verified Google Account" },
                  fontSize = 13.sp,
                  color = Color.White.copy(alpha = 0.88f),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Surface(
                  color =
                    if (showPremiumOption && isPremiumActive) BrokerGreen
                    else Color.White.copy(alpha = 0.18f),
                  shape = RoundedCornerShape(8.dp),
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                  ) {
                    Icon(
                      imageVector =
                        if (showPremiumOption && isPremiumActive) Icons.Filled.WorkspacePremium
                        else Icons.Filled.VerifiedUser,
                      contentDescription = null,
                      tint = Color.White,
                      modifier = Modifier.size(14.dp),
                    )
                    Text(
                      text =
                        when {
                          showPremiumOption && isPremiumActive -> "Premium Active"
                          showPremiumOption -> "Standard Member"
                          else -> "Verified Member"
                        },
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color.White,
                    )
                  }
                }
              }
            }
          }
        }

        // Quick Activity Stats
        Row(
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          ProfileStatCard(
            label = "Listings",
            value = myListingsCount.toString(),
            icon = Icons.Filled.HomeWork,
            accent = BrokerBlue,
            onClick = null,
            modifier = Modifier.weight(1f),
          )
          ProfileStatCard(
            label = "Favorites",
            value = favoriteCount.toString(),
            icon = Icons.Filled.Favorite,
            accent = BrokerRed,
            onClick = onOpenFavorites,
            modifier = Modifier.weight(1f).testTag("profile_favorites_stat"),
          )
          if (showPremiumOption) {
            ProfileStatCard(
              label = "Unlocked",
              value = unlockedCount.toString(),
              icon = Icons.Filled.LockOpen,
              accent = BrokerGreen,
              onClick = null,
              modifier = Modifier.weight(1f),
            )
          } else {
            ProfileStatCard(
              label = "Alerts",
              value = activeAlertsCount.toString(),
              icon = Icons.Filled.NotificationsActive,
              accent = BrokerTeal,
              onClick = onOpenAlerts,
              modifier = Modifier.weight(1f).testTag("profile_alerts_stat"),
            )
          }
        }

        // Workspace Role Section
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
            Text(
              text = "Account Role",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "Choose how you primarily use Property Broker.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              UserRole.entries.forEach { role ->
                val selected = role == currentRole
                FilterChip(
                  selected = selected,
                  onClick = { onSelectRole(role) },
                  label = { Text(role.label) },
                  colors =
                    FilterChipDefaults.filterChipColors(
                      selectedContainerColor = BrokerIndigo,
                      selectedLabelColor = Color.White,
                    ),
                )
              }
            }
          }
        }

        // Quick Links Card
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            if (showPremiumOption) {
              ProfileMenuRow(
                icon = Icons.Filled.WorkspacePremium,
                iconTint = BrokerPurple,
                title = "Premium Membership",
                subtitle =
                  if (isPremiumActive) "7-Day Premium Pass is active"
                  else "Unlock unlimited verified owner contacts",
                onClick = onOpenPremium,
                modifier = Modifier.testTag("profile_open_premium_row"),
              )
              HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
            if (onOpenAlerts != null) {
              ProfileMenuRow(
                icon = Icons.Filled.NotificationsActive,
                iconTint = BrokerTeal,
                title = "Alerts & Notifications",
                subtitle = "$activeAlertsCount active saved property alerts",
                onClick = onOpenAlerts,
                modifier = Modifier.testTag("profile_open_alerts_row"),
              )
              HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
            ProfileMenuRow(
              icon = Icons.Filled.Favorite,
              iconTint = BrokerRed,
              title = "Saved Favorites",
              subtitle = "$favoriteCount saved properties",
              onClick = onOpenFavorites,
              modifier = Modifier.testTag("profile_open_favorites_row"),
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            ProfileMenuRow(
              icon = Icons.Filled.Description,
              iconTint = BrokerTeal,
              title = "Terms & Conditions",
              subtitle = "Platform guidelines, PII vault & disclaimers",
              onClick = onOpenTerms,
              modifier = Modifier.testTag("profile_open_terms_row"),
            )
          }
        }

        // Sign Out & Account Deletion Actions (Play Store Data Safety Compliance)
        OutlinedButton(
          onClick = onSignOut,
          enabled = !isDeletingAccount,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().height(52.dp).testTag("profile_sign_out_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Logout,
            contentDescription = null,
            tint = BrokerRed,
          )
          Spacer(Modifier.width(8.dp))
          Text("Sign Out", color = BrokerRed, fontWeight = FontWeight.Bold)
        }

        TextButton(
          onClick = { showDeleteDialog = true },
          enabled = !isDeletingAccount,
          modifier = Modifier.fillMaxWidth().testTag("profile_delete_account_button"),
        ) {
          if (isDeletingAccount) {
            CircularProgressIndicator(
              color = BrokerRed,
              strokeWidth = 2.dp,
              modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text("Deleting Account...", color = BrokerRed, fontWeight = FontWeight.SemiBold)
          } else {
            Icon(
              imageVector = Icons.Filled.DeleteForever,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
              text = "Delete Account & Data",
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontWeight = FontWeight.Medium,
            )
          }
        }

        Spacer(Modifier.height(16.dp))
      }
    }
  }
}

@Composable
private fun ProfileStatCard(
  label: String,
  value: String,
  icon: ImageVector,
  accent: Color,
  onClick: (() -> Unit)?,
  modifier: Modifier = Modifier,
) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(4.dp),
      modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 8.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = accent,
        modifier = Modifier.size(22.dp),
      )
      Text(
        text = value,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = label,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun ProfileMenuRow(
  icon: ImageVector,
  iconTint: Color,
  title: String,
  subtitle: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(14.dp),
    modifier = modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
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
    Icon(
      imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PremiumScreen(
  membership: PremiumMembership?,
  isSubmitting: Boolean,
  onActivatePass: (PaymentMethod) -> Unit,
  onBack: () -> Unit,
  onOpenHome: () -> Unit,
  onOpenProfile: () -> Unit,
  onOpenTerms: () -> Unit,
  onSignOut: () -> Unit,
) {
  val isPremiumActive =
    membership?.premiumActive == true && membership.expiresAtMillis > System.currentTimeMillis()
  var selectedMethod by remember { mutableStateOf(PaymentMethod.UPI_VERIFIED) }
  val expiryFormatted =
    remember(membership?.expiresAtMillis) {
      val exp = membership?.expiresAtMillis ?: 0L
      if (exp > 0L) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(exp))
      } else {
        ""
      }
    }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.menu_premium), fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          AppOverflowMenu(
            isPremiumActive = isPremiumActive,
            onOpenHome = onOpenHome,
            onOpenProfile = onOpenProfile,
            onOpenPremium = {},
            onOpenTerms = onOpenTerms,
            onSignOut = onSignOut,
            menuButtonTestTag = "premium_overflow_menu",
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
          Modifier.widthIn(max = 560.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
      ) {
        // Hero Premium Banner
        Card(
          shape = RoundedCornerShape(18.dp),
          elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
          modifier = Modifier.fillMaxWidth().testTag("premium_hero_card"),
        ) {
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(BrokerIndigo, BrokerDeepPurple, BrokerPurple)))
                .padding(22.dp),
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
              ) {
                Surface(
                  color = Color.White.copy(alpha = 0.2f),
                  shape = CircleShape,
                  modifier = Modifier.size(48.dp),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.Filled.WorkspacePremium,
                      contentDescription = null,
                      tint = Color.White,
                      modifier = Modifier.size(28.dp),
                    )
                  }
                }
                Surface(
                  color = if (isPremiumActive) BrokerGreen else Color.White.copy(alpha = 0.2f),
                  shape = RoundedCornerShape(20.dp),
                ) {
                  Text(
                    text = if (isPremiumActive) "PASS ACTIVE" else "BEST VALUE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                  )
                }
              }

              Text(
                text = "7-Day Premium Pass",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
              )

              Row(verticalAlignment = Alignment.Bottom) {
                Text(
                  text = "₹999",
                  fontSize = 32.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = Color.White,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                  text = "/ 7 days unlimited access",
                  fontSize = 14.sp,
                  color = Color.White.copy(alpha = 0.88f),
                  modifier = Modifier.padding(bottom = 5.dp),
                )
              }

              val activeMembership = membership?.takeIf { isPremiumActive }
              if (activeMembership != null) {
                Surface(
                  color = Color.White.copy(alpha = 0.16f),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.fillMaxWidth().testTag("premium_active_details"),
                ) {
                  Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(12.dp),
                  ) {
                    Text(
                      text = "Active Plan: ${activeMembership.planName}",
                      fontSize = 13.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = Color.White,
                    )
                    if (activeMembership.referenceCode.isNotBlank()) {
                      Text(
                        text = "Reference: ${activeMembership.referenceCode}",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.9f),
                      )
                    }
                    if (expiryFormatted.isNotBlank()) {
                      Text(
                        text = "Valid until: $expiryFormatted",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.9f),
                      )
                    }
                  }
                }
              }
            }
          }
        }

        // Included Benefits
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().padding(18.dp),
          ) {
            Text(
              text = "Premium Benefits",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            PremiumBenefitRow(
              icon = Icons.Filled.LockOpen,
              title = "Unlimited Contact Unlocks",
              description = "Reveal direct owner phone numbers & WhatsApp across Sale, Rent, and Heavy Deposit.",
            )
            PremiumBenefitRow(
              icon = Icons.Filled.VerifiedUser,
              title = "Zero-Trust PII Vault Access",
              description = "Verified owner details protected against spam and unlocked instantly.",
            )
            PremiumBenefitRow(
              icon = Icons.Filled.Forum,
              title = "Direct In-App Chat",
              description = "Message property owners and brokers directly with instant thread sync.",
            )
            PremiumBenefitRow(
              icon = Icons.Filled.AutoAwesome,
              title = "AI Property Assistant Included",
              description = "Use smart price benchmarks, EMI breakdowns, and neighborhood insights.",
            )
          }
        }

        // Payment Method & Activation Card
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().padding(18.dp),
          ) {
            Text(
              text = "Select Verification Method",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              PaymentMethod.entries.forEach { method ->
                FilterChip(
                  selected = selectedMethod == method,
                  onClick = { selectedMethod = method },
                  label = { Text(method.label) },
                  colors =
                    FilterChipDefaults.filterChipColors(
                      selectedContainerColor = BrokerPurple,
                      selectedLabelColor = Color.White,
                    ),
                )
              }
            }

            Button(
              onClick = { onActivatePass(selectedMethod) },
              enabled = !isSubmitting,
              colors = ButtonDefaults.buttonColors(containerColor = BrokerPurple),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth().height(54.dp).testTag("premium_activate_button"),
            ) {
              if (isSubmitting) {
                CircularProgressIndicator(
                  color = Color.White,
                  strokeWidth = 2.dp,
                  modifier = Modifier.size(20.dp),
                )
              } else {
                Icon(Icons.Filled.WorkspacePremium, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                  text =
                    if (isPremiumActive) "Renew 7-Day Premium Pass (₹999)"
                    else "Activate 7-Day Premium Pass (₹999)",
                  fontWeight = FontWeight.Bold,
                  fontSize = 15.sp,
                )
              }
            }

            Text(
              text = "Compare: Single contact unlock is ₹1,000 per listing, while the 7-Day Premium Pass unlocks unlimited contacts for ₹999.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        // Link to Terms
        OutlinedButton(
          onClick = onOpenTerms,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("premium_view_terms_button"),
        ) {
          Icon(Icons.Filled.Description, contentDescription = null, tint = BrokerTeal)
          Spacer(Modifier.width(8.dp))
          Text("Read Premium Terms & Conditions", color = BrokerIndigo, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(16.dp))
      }
    }
  }
}

@Composable
private fun PremiumBenefitRow(
  icon: ImageVector,
  title: String,
  description: String,
) {
  Row(
    verticalAlignment = Alignment.Top,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Surface(
      color = BrokerPurple.copy(alpha = 0.12f),
      shape = CircleShape,
      modifier = Modifier.size(36.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = BrokerPurple,
          modifier = Modifier.size(18.dp),
        )
      }
    }
    Column(
      verticalArrangement = Arrangement.spacedBy(2.dp),
      modifier = Modifier.weight(1f),
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
      )
      Text(
        text = description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

private data class TermsClause(
  val number: String,
  val heading: String,
  val body: String,
)

private val propertyBrokerTerms =
  listOf(
    TermsClause(
      number = "1",
      heading = "Acceptance of Terms",
      body =
        "By accessing or using Property Broker to list, search, map, favorite, or inquire about Sale, Buy, Rent, or Heavy Deposit properties, you agree to comply with these Terms & Conditions and all applicable local real-estate regulations.",
    ),
    TermsClause(
      number = "2",
      heading = "Listing Accuracy & Media Standards",
      body =
        "Property owners and brokers must provide accurate pricing, area (sq.ft), property category (Apartment, Villa, Shop, Gala, Office, Commercial, or Plot), location details, and genuine property photos (1–10 images, total media up to 8 MB). Misleading, duplicate, or fraudulent listings may be closed or removed without notice.",
    ),
    TermsClause(
      number = "3",
      heading = "Zero-Trust PII Vault & Contact Privacy",
      body =
        "Owner phone numbers, emails, and WhatsApp contacts are protected inside our contact vault and revealed only when unlocked via a single-contact unlock or an active 7-Day Premium Pass. Users may use unlocked details strictly for legitimate property inquiries and must not resell or spam contact information.",
    ),
    TermsClause(
      number = "4",
      heading = "RERA & Platform Disclaimer",
      body =
        "Property Broker operates solely as an online intermediary and technology listing platform connecting property owners, real estate agents, and prospective buyers/tenants. We do not act as a real estate broker, developer, or agent, nor do we verify, endorse, or guarantee the accuracy, legality, or RERA registration status of any property listed on this platform. Prospective buyers and tenants are strongly advised to independently verify all property titles, approvals, and RERA registration details directly on the official state RERA portal (such as maharera.maharashtra.gov.in) before making any financial commitment or transaction.",
    ),
    TermsClause(
      number = "5",
      heading = "Premium Pass & Unlock Policy",
      body =
        "The 7-Day Premium Pass provides unlimited verified owner contact unlocks for 7 continuous days (168 hours) from activation. Active plan status, reference code, and expiry timestamp remain visible inside your Profile and Premium screens.",
    ),
    TermsClause(
      number = "6",
      heading = "AI Property Assistant Disclaimer",
      body =
        "Price estimates, EMI projections, rental yield comparisons, and listing descriptions produced by the AI Property Assistant are automated informational aids only and do not constitute binding financial, tax, or legal advice.",
    ),
    TermsClause(
      number = "7",
      heading = "Account Security & Data Control",
      body =
        "Cloud-synced accounts use Google Sign-In for authentication. You are responsible for safeguarding access to your device and account, and you may manage your listings or sign out at any time from the Profile or top-right menu.",
    ),
  )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsAndConditionsScreen(
  isPremiumActive: Boolean,
  showPremiumOption: Boolean = PremiumSchedule.isPremiumFeatureEnabled(),
  requireAcceptance: Boolean = false,
  onAcceptTerms: () -> Unit = {},
  onBack: () -> Unit,
  onOpenHome: () -> Unit,
  onOpenProfile: () -> Unit,
  onOpenPremium: () -> Unit,
  onSignOut: () -> Unit,
) {
  var acceptedChecked by remember { mutableStateOf(false) }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = {
          Text(
            stringResource(
              if (requireAcceptance) R.string.terms_gate_title else R.string.menu_terms_conditions
            ),
            fontWeight = FontWeight.Bold,
          )
        },
        navigationIcon = {
          if (!requireAcceptance) {
            IconButton(onClick = onBack) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
          }
        },
        actions = {
          if (requireAcceptance) {
            TextButton(
              onClick = onSignOut,
              modifier = Modifier.testTag("terms_topbar_signout"),
            ) {
              Text(
                text = stringResource(R.string.logout),
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
              )
            }
          } else {
            AppOverflowMenu(
              isPremiumActive = isPremiumActive,
              showPremiumOption = showPremiumOption,
              onOpenHome = onOpenHome,
              onOpenProfile = onOpenProfile,
              onOpenPremium = onOpenPremium,
              onOpenTerms = {},
              onSignOut = onSignOut,
              menuButtonTestTag = "terms_overflow_menu",
            )
          }
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
    bottomBar = {
      if (requireAcceptance) {
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shadowElevation = 10.dp,
          tonalElevation = 3.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier =
              Modifier.fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier =
                Modifier.fillMaxWidth()
                  .clickable { acceptedChecked = !acceptedChecked }
                  .padding(vertical = 4.dp)
                  .testTag("terms_accept_checkbox_row"),
            ) {
              Checkbox(
                checked = acceptedChecked,
                onCheckedChange = { acceptedChecked = it },
                colors =
                  CheckboxDefaults.colors(
                    checkedColor = BrokerGreen,
                    uncheckedColor = BrokerIndigo,
                  ),
                modifier = Modifier.testTag("terms_accept_checkbox"),
              )
              Text(
                text = stringResource(R.string.terms_accept_checkbox),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 18.sp,
                modifier = Modifier.weight(1f),
              )
            }

            Row(
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              OutlinedButton(
                onClick = onSignOut,
                shape = RoundedCornerShape(12.dp),
                modifier =
                  Modifier.weight(1f)
                    .height(50.dp)
                    .testTag("terms_decline_signout_button"),
              ) {
                Text(
                  text = stringResource(R.string.terms_decline_signout),
                  fontWeight = FontWeight.SemiBold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              Button(
                onClick = onAcceptTerms,
                enabled = acceptedChecked,
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = BrokerGreen,
                    contentColor = Color.White,
                  ),
                shape = RoundedCornerShape(12.dp),
                modifier =
                  Modifier.weight(1.25f)
                    .height(50.dp)
                    .testTag("terms_accept_continue_button"),
              ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                  text = stringResource(R.string.terms_accept_continue),
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }
      }
    },
  ) { innerPadding ->
    Box(
      contentAlignment = Alignment.TopCenter,
      modifier = Modifier.fillMaxSize().padding(innerPadding),
    ) {
      Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier =
          Modifier.widthIn(max = 560.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
      ) {
        // Legal Header Banner
        Card(
          shape = RoundedCornerShape(16.dp),
          elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
          modifier = Modifier.fillMaxWidth().testTag("terms_header_card"),
        ) {
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(BrokerIndigo, BrokerTeal)))
                .padding(18.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
              Surface(
                color = Color.White.copy(alpha = 0.2f),
                shape = CircleShape,
                modifier = Modifier.size(48.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Filled.Gavel,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                  )
                }
              }
              Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                  text = "Property Broker Terms",
                  fontSize = 19.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.White,
                )
                Text(
                  text = "Marketplace Policies, PII Vault & User Guidelines",
                  fontSize = 12.sp,
                  color = Color.White.copy(alpha = 0.88f),
                )
              }
            }
          }
        }

        // Prominent RERA & Platform Disclaimer Card
        val termsContext = LocalContext.current
        Surface(
          color = BrokerAmber.copy(alpha = 0.12f),
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, BrokerAmber.copy(alpha = 0.45f)),
          modifier = Modifier.fillMaxWidth().testTag("terms_rera_disclaimer_card"),
        ) {
          Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = BrokerAmber,
                modifier = Modifier.size(20.dp),
              )
              Text(
                text = stringResource(R.string.rera_disclaimer_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
            Text(
              text = stringResource(R.string.rera_disclaimer_body),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              lineHeight = 19.sp,
            )
            TextButton(
              onClick = {
                runCatching {
                  termsContext.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://maharera.maharashtra.gov.in"))
                  )
                }
              },
              modifier = Modifier.testTag("terms_maharera_link"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = BrokerIndigo,
                modifier = Modifier.size(16.dp),
              )
              Spacer(Modifier.width(6.dp))
              Text(
                text = stringResource(R.string.rera_portal_link_label),
                color = BrokerIndigo,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
              )
            }
          }
        }

        val visibleTerms =
          remember(showPremiumOption) {
            val filtered =
              if (showPremiumOption) propertyBrokerTerms
              else propertyBrokerTerms.filterNot { it.heading.contains("Premium", ignoreCase = true) }
            filtered.mapIndexed { idx, clause -> clause.copy(number = (idx + 1).toString()) }
          }

        visibleTerms.forEach { clause ->
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              verticalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Surface(
                  color = BrokerIndigo.copy(alpha = 0.12f),
                  shape = CircleShape,
                  modifier = Modifier.size(26.dp),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Text(
                      text = clause.number,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold,
                      color = BrokerIndigo,
                    )
                  }
                }
                Text(
                  text = clause.heading,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }
              Text(
                text = clause.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp,
              )
            }
          }
        }

        Surface(
          color = BrokerGreen.copy(alpha = 0.1f),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(14.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.CheckCircle,
              contentDescription = null,
              tint = BrokerGreen,
              modifier = Modifier.size(20.dp),
            )
            Text(
              text = "Continued use of Property Broker confirms acceptance of these terms.",
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }

        if (!requireAcceptance) {
          Button(
            onClick = onOpenHome,
            colors = ButtonDefaults.buttonColors(containerColor = BrokerIndigo),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("terms_back_home_button"),
          ) {
            Icon(Icons.Filled.Home, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Back to Home", fontWeight = FontWeight.Bold)
          }
        }

        Spacer(Modifier.height(16.dp))
      }
    }
  }
}
