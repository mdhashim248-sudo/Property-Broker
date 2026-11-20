package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Elevator
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.LocationResolver
import com.example.data.UiState
import com.example.data.model.ListingType
import com.example.data.model.PaymentMethod
import com.example.data.model.PropertyCategory
import com.example.data.model.PropertyContact
import com.example.data.model.PropertyFormatters
import com.example.ui.components.PropertyPhotoImage
import com.example.ui.theme.BrokerAmber
import com.example.ui.theme.BrokerBlue
import com.example.ui.theme.BrokerDeepPurple
import com.example.ui.theme.BrokerGreen
import com.example.ui.theme.BrokerIndigo
import com.example.ui.theme.BrokerPurple
import com.example.ui.theme.BrokerRed
import com.example.ui.theme.BrokerTeal
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.viewmodel.PropertyDetailViewModel

@Composable
fun PropertyDetailScreen(
  viewModel: PropertyDetailViewModel,
  currentUserId: String,
  currentUserName: String,
  isFavorite: Boolean,
  onToggleFavorite: () -> Unit,
  onDeleteProperty: (String) -> Unit,
  onOpenChat: (String) -> Unit,
  onBack: () -> Unit,
) {
  val propertyState by viewModel.propertyState.collectAsStateWithLifecycle()
  val hasContactAccess by viewModel.hasContactAccess.collectAsStateWithLifecycle()
  val protectedContact by viewModel.protectedContact.collectAsStateWithLifecycle()
  val contactLoading by viewModel.contactLoading.collectAsStateWithLifecycle()
  val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

  PropertyDetailContent(
    propertyState = propertyState,
    hasContactAccess = hasContactAccess,
    protectedContact = protectedContact,
    contactLoading = contactLoading,
    statusMessage = statusMessage,
    currentUserId = currentUserId,
    isFavorite = isFavorite,
    onToggleFavorite = onToggleFavorite,
    onClearStatusMessage = viewModel::clearStatusMessage,
    onUnlockSingle = { viewModel.unlockSingleContact(PaymentMethod.UPI_VERIFIED) },
    onActivatePass = { viewModel.activatePremiumPass(PaymentMethod.UPI_VERIFIED) },
    onStartChat = { property ->
      viewModel.openChatWithOwner(
        property = property,
        buyerName = currentUserName,
        onChatReady = onOpenChat,
      )
    },
    onDeleteProperty = onDeleteProperty,
    onBack = onBack,
  )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PropertyDetailContent(
  propertyState: UiState<com.example.data.model.PropertyListing?>,
  hasContactAccess: Boolean,
  protectedContact: PropertyContact?,
  contactLoading: Boolean,
  statusMessage: String?,
  currentUserId: String,
  isFavorite: Boolean,
  onToggleFavorite: () -> Unit,
  onClearStatusMessage: () -> Unit,
  onUnlockSingle: () -> Unit,
  onActivatePass: () -> Unit,
  onStartChat: (com.example.data.model.PropertyListing) -> Unit,
  onDeleteProperty: (String) -> Unit,
  onBack: () -> Unit,
) {
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }
  var pendingPaymentMode by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(statusMessage) {
    statusMessage?.let {
      snackbarHostState.showSnackbar(it)
      onClearStatusMessage()
    }
  }

  if (pendingPaymentMode != null) {
    val isPass = pendingPaymentMode == "PASS"
    AlertDialog(
      onDismissRequest = { pendingPaymentMode = null },
      title = {
        Text(
          text = if (isPass) "Get 7-Day Premium (₹999)" else "Pay ₹1000 to Unlock Contact",
          fontWeight = FontWeight.Bold,
          color = BrokerIndigo,
        )
      },
      text = {
        Text(
          text =
            if (isPass)
              "Unlock direct owner phone numbers across all listings for 7 days (₹999)."
            else
              "Pay ₹1,000 to unlock this owner's verified contact number from the private Firestore Vault."
        )
      },
      confirmButton = {
        Button(
          onClick = {
            val mode = pendingPaymentMode
            pendingPaymentMode = null
            if (mode == "PASS") onActivatePass()
            else onUnlockSingle()
          },
          colors = ButtonDefaults.buttonColors(containerColor = BrokerBlue),
        ) {
          Text(if (isPass) "Confirm ₹999" else "Confirm ₹1000")
        }
      },
      dismissButton = {
        TextButton(onClick = { pendingPaymentMode = null }) { Text("Cancel") }
      },
    )
  }

  val loadedProperty = (propertyState as? UiState.Success)?.data
  val isOwner = loadedProperty?.ownerId == currentUserId

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = { Text("Property Details", fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.testTag("detail_favorite_heart"),
          ) {
            Icon(
              imageVector =
                if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
              contentDescription =
                if (isFavorite) "Remove from Favorites" else "Save to Favorites",
              tint = if (isFavorite) Color(0xFFFF8A80) else Color.White,
            )
          }
          if (isOwner) {
            val ownerPropId = loadedProperty?.id
            if (ownerPropId != null) {
              IconButton(
                onClick = {
                  onDeleteProperty(ownerPropId)
                  onBack()
                }
              ) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete property")
              }
            }
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
  ) { innerPadding ->
    Box(
      contentAlignment = Alignment.TopCenter,
      modifier = Modifier.fillMaxSize().padding(innerPadding).background(MaterialTheme.colorScheme.background),
    ) {
      when (val state = propertyState) {
        is UiState.Loading -> {
          CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        is UiState.Error -> {
          Text(
            text = state.message,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.align(Alignment.Center).padding(24.dp),
          )
        }
        is UiState.Success -> {
          val property = state.data
          if (property == null) {
            Text(
              text = "Property not found.",
              modifier = Modifier.align(Alignment.Center).padding(24.dp),
            )
          } else {
            val photos = property.photoKeys
            Column(
              verticalArrangement = Arrangement.spacedBy(14.dp),
              modifier =
                Modifier.widthIn(max = 640.dp)
                  .fillMaxSize()
                  .verticalScroll(rememberScrollState())
                  .padding(vertical = 16.dp),
            ) {
              if (photos.isNotEmpty()) {
                LazyRow(
                  contentPadding = PaddingValues(horizontal = 16.dp),
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  itemsIndexed(
                    items = photos,
                    key = { index, key -> "${index}_$key" },
                    contentType = { _, _ -> "detail_photo" },
                  ) { _, key ->
                    PropertyPhotoImage(
                      photoKey = key,
                      contentDescription = property.title,
                      modifier =
                        Modifier.width(280.dp)
                          .height(200.dp)
                          .clip(RoundedCornerShape(10.dp)),
                    )
                  }
                }
              }

              Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(horizontal = 16.dp),
              ) {
                Card(
                  shape = RoundedCornerShape(10.dp),
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                  elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                  ) {
                    val category = PropertyCategory.fromRaw(property.propertyCategory)
                    Row(
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Surface(
                        color = BrokerDeepPurple.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp),
                      ) {
                        Text(
                          text = ListingType.fromRaw(property.listingType).label.uppercase(),
                          fontSize = 11.sp,
                          fontWeight = FontWeight.Bold,
                          color = BrokerDeepPurple,
                          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                      }
                      Surface(
                        color = BrokerBlue.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp),
                      ) {
                        Text(
                          text = category.label.uppercase(),
                          fontSize = 11.sp,
                          fontWeight = FontWeight.Bold,
                          color = BrokerBlue,
                          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                      }
                    }
                    Text(
                      text = property.title,
                      fontSize = 21.sp,
                      fontWeight = FontWeight.Bold,
                      color = BrokerIndigo,
                    )
                    Text(
                      text = PropertyFormatters.formatInrPrice(property.price, property.listingType),
                      fontSize = 20.sp,
                      fontWeight = FontWeight.Bold,
                      color = BrokerGreen,
                    )

                    if (property.listingType == ListingType.RENT.name && property.depositAmount > 0L) {
                      Text(
                        text = "Deposit: ${PropertyFormatters.formatFullInr(property.depositAmount)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrokerDeepPurple,
                        modifier = Modifier.testTag("detail_deposit_text"),
                      )
                    } else if (property.listingType == ListingType.HEAVY_DEPOSIT.name && property.monthlyRent > 0L) {
                      Text(
                        text = "Monthly Rent: ${PropertyFormatters.formatFullInr(property.monthlyRent)}/mo",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrokerDeepPurple,
                        modifier = Modifier.testTag("detail_monthly_rent_text"),
                      )
                    }

                    // Structured Property Type, Area, BHK, Floor, Deposit/Rent, Building Age & Lift details
                    Surface(
                      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                      shape = RoundedCornerShape(8.dp),
                      modifier = Modifier.fillMaxWidth().testTag("detail_specs_box"),
                    ) {
                      FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                      ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                          Icon(
                            imageVector = Icons.Filled.Apartment,
                            contentDescription = null,
                            tint = BrokerIndigo,
                            modifier = Modifier.size(16.dp),
                          )
                          Spacer(Modifier.width(5.dp))
                          Text(
                            text = category.label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                          )
                        }
                        if (property.areaSqFt > 0L) {
                          Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                              imageVector = Icons.Filled.SquareFoot,
                              contentDescription = null,
                              tint = BrokerIndigo,
                              modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                              text = PropertyFormatters.formatAreaSqFt(property.areaSqFt),
                              fontSize = 13.sp,
                              fontWeight = FontWeight.SemiBold,
                              color = MaterialTheme.colorScheme.onSurface,
                            )
                          }
                        }
                        if (property.bedrooms > 0L) {
                          Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                              imageVector = Icons.Filled.Bed,
                              contentDescription = null,
                              tint = BrokerIndigo,
                              modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                              text = "${property.bedrooms} BHK",
                              fontSize = 13.sp,
                              fontWeight = FontWeight.SemiBold,
                              color = MaterialTheme.colorScheme.onSurface,
                            )
                          }
                        }
                        if (property.floor.isNotBlank()) {
                          Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.testTag("detail_spec_floor"),
                          ) {
                            Icon(
                              imageVector = Icons.Filled.Layers,
                              contentDescription = null,
                              tint = BrokerIndigo,
                              modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                              text = "Floor: ${property.floor}",
                              fontSize = 13.sp,
                              fontWeight = FontWeight.SemiBold,
                              color = MaterialTheme.colorScheme.onSurface,
                            )
                          }
                        }
                        if (property.listingType == ListingType.RENT.name && property.depositAmount > 0L) {
                          Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.testTag("detail_spec_deposit"),
                          ) {
                            Icon(
                              imageVector = Icons.Filled.Payments,
                              contentDescription = null,
                              tint = BrokerIndigo,
                              modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                              text = "Deposit: ${PropertyFormatters.formatFullInr(property.depositAmount)}",
                              fontSize = 13.sp,
                              fontWeight = FontWeight.SemiBold,
                              color = MaterialTheme.colorScheme.onSurface,
                            )
                          }
                        }
                        if (property.listingType == ListingType.HEAVY_DEPOSIT.name && property.monthlyRent > 0L) {
                          Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.testTag("detail_spec_monthly_rent"),
                          ) {
                            Icon(
                              imageVector = Icons.Filled.Payments,
                              contentDescription = null,
                              tint = BrokerIndigo,
                              modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                              text = "Rent: ${PropertyFormatters.formatFullInr(property.monthlyRent)}/mo",
                              fontSize = 13.sp,
                              fontWeight = FontWeight.SemiBold,
                              color = MaterialTheme.colorScheme.onSurface,
                            )
                          }
                        }
                        if (property.buildingAge.isNotBlank()) {
                          Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.testTag("detail_spec_building_age"),
                          ) {
                            Icon(
                              imageVector = Icons.Filled.Schedule,
                              contentDescription = null,
                              tint = BrokerIndigo,
                              modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                              text = "Age: ${property.buildingAge}",
                              fontSize = 13.sp,
                              fontWeight = FontWeight.SemiBold,
                              color = MaterialTheme.colorScheme.onSurface,
                            )
                          }
                        }
                        property.hasLift?.let { liftAvailable ->
                          Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.testTag("detail_spec_lift"),
                          ) {
                            Icon(
                              imageVector = Icons.Filled.Elevator,
                              contentDescription = null,
                              tint = if (liftAvailable) BrokerGreen else BrokerIndigo,
                              modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                              text =
                                stringResource(
                                  if (liftAvailable) R.string.detail_lift_yes
                                  else R.string.detail_lift_no
                                ),
                              fontSize = 13.sp,
                              fontWeight = FontWeight.SemiBold,
                              color = MaterialTheme.colorScheme.onSurface,
                            )
                          }
                        }
                      }
                    }

                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween,
                      modifier = Modifier.fillMaxWidth(),
                    ) {
                      Text(
                        text = "Location: ${property.location}",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                      )
                      TextButton(
                        onClick = {
                          val geo = LocationResolver.resolveCoordinates(property)
                          val label = Uri.encode("${property.title} (${property.location})")
                          val uri =
                            Uri.parse("geo:${geo.latitude},${geo.longitude}?q=${geo.latitude},${geo.longitude}($label)")
                          runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                          }
                        },
                        modifier = Modifier.testTag("detail_open_map_button"),
                      ) {
                        Icon(
                          imageVector = Icons.Filled.LocationOn,
                          contentDescription = null,
                          tint = BrokerBlue,
                          modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                          text = "View Map",
                          color = BrokerBlue,
                          fontWeight = FontWeight.SemiBold,
                          fontSize = 13.sp,
                        )
                      }
                    }
                    Text(
                      text = "Owner: ${property.ownerName}",
                      fontSize = 14.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (property.hasVideo) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                          imageVector = Icons.Filled.Videocam,
                          contentDescription = null,
                          tint = BrokerDeepPurple,
                          modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                          text = "Property walkthrough video attached",
                          fontSize = 13.sp,
                          color = BrokerDeepPurple,
                        )
                      }
                    }
                    if (property.description.isNotBlank()) {
                      Spacer(Modifier.height(4.dp))
                      Text(
                        text = property.description,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }
                }

                // Protected Contact Card (Split-Collection PII Vault)
                ContactSectionCard(
                  unlocked = hasContactAccess,
                  contact = protectedContact,
                  isLoading = contactLoading,
                  onPaySingle = { pendingPaymentMode = "SINGLE" },
                  onGetPremium = { pendingPaymentMode = "PASS" },
                  onCall = { phone ->
                    runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))) }
                  },
                  onWhatsApp = { phone ->
                    val digits = phone.filter { it.isDigit() }
                    runCatching {
                      context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits")))
                    }
                  },
                )

                Button(
                  onClick = { onStartChat(property) },
                  shape = MaterialTheme.shapes.small,
                  colors =
                    ButtonDefaults.buttonColors(
                      containerColor = BrokerTeal,
                      contentColor = Color.White,
                    ),
                  modifier =
                    Modifier.fillMaxWidth().height(54.dp).testTag("chat_with_owner_button"),
                ) {
                  Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
                  Spacer(Modifier.width(8.dp))
                  Text("Chat with Owner", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                // RERA & Platform Disclaimer Card
                Surface(
                  color = BrokerAmber.copy(alpha = 0.10f),
                  shape = RoundedCornerShape(12.dp),
                  border = BorderStroke(1.dp, BrokerAmber.copy(alpha = 0.4f)),
                  modifier = Modifier.fillMaxWidth().testTag("detail_rera_disclaimer_card"),
                ) {
                  Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = BrokerAmber,
                        modifier = Modifier.size(18.dp),
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
                      lineHeight = 18.sp,
                    )
                    TextButton(
                      onClick = {
                        runCatching {
                          context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://maharera.maharashtra.gov.in"))
                          )
                        }
                      },
                      modifier = Modifier.testTag("detail_maharera_link"),
                    ) {
                      Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = BrokerIndigo,
                        modifier = Modifier.size(15.dp),
                      )
                      Spacer(Modifier.width(6.dp))
                      Text(
                        text = stringResource(R.string.rera_portal_link_label),
                        color = BrokerIndigo,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ContactSectionCard(
  unlocked: Boolean,
  contact: PropertyContact?,
  isLoading: Boolean,
  onPaySingle: () -> Unit,
  onGetPremium: () -> Unit,
  onCall: (String) -> Unit,
  onWhatsApp: (String) -> Unit,
) {
  Card(
    shape = RoundedCornerShape(10.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      verticalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier.fillMaxWidth().padding(16.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = if (unlocked) Icons.Filled.LockOpen else Icons.Filled.Lock,
          contentDescription = null,
          tint = if (unlocked) BrokerGreen else BrokerIndigo,
          modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
          text = if (unlocked) "Owner Contact Unlocked" else "Owner Contact Locked",
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold,
          color = if (unlocked) BrokerGreen else BrokerIndigo,
        )
      }

      if (isLoading) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
      } else if (unlocked && contact != null) {
        Text(
          text = contact.contactPhone,
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold,
          color = BrokerGreen,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
          Button(
            onClick = { onCall(contact.contactPhone) },
            shape = MaterialTheme.shapes.small,
            colors = ButtonDefaults.buttonColors(containerColor = BrokerGreen),
            modifier = Modifier.weight(1f).height(48.dp),
          ) {
            Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Call")
          }
          Button(
            onClick = { onWhatsApp(contact.contactPhone) },
            shape = MaterialTheme.shapes.small,
            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
            modifier = Modifier.weight(1f).height(48.dp),
          ) {
            Icon(
              painter = painterResource(id = R.drawable.ic_whatsapp_logo),
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text("WhatsApp", color = Color.White)
          }
        }
      } else {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = MaterialTheme.shapes.small,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = "+91 •••••• •••  (Secured in Firestore Vault)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(12.dp),
          )
        }
        Button(
          onClick = onPaySingle,
          shape = MaterialTheme.shapes.small,
          colors = ButtonDefaults.buttonColors(containerColor = BrokerBlue),
          modifier = Modifier.fillMaxWidth().height(50.dp).testTag("unlock_contact_button"),
        ) {
          Text("Pay ₹1000 to Unlock Contact", fontWeight = FontWeight.Bold)
        }
        Button(
          onClick = onGetPremium,
          shape = MaterialTheme.shapes.small,
          colors = ButtonDefaults.buttonColors(containerColor = BrokerPurple),
          modifier = Modifier.fillMaxWidth().height(50.dp).testTag("activate_pass_button"),
        ) {
          Text("Get 7-Day Premium (₹999)", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
