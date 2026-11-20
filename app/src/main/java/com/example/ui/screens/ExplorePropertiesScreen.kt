package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NotificationAdd
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PremiumSchedule
import com.example.data.UiState
import com.example.data.model.ListingType
import com.example.data.model.PremiumMembership
import com.example.data.model.PriceBracketPreset
import com.example.data.model.PropertyCategory
import com.example.data.model.PropertyFormatters
import com.example.data.model.PropertyListing
import com.example.ui.components.PropertyMapView
import com.example.ui.components.PropertyPhotoImage
import com.example.ui.components.QuickPropertyFilterRow
import com.example.ui.theme.BrokerBlue
import com.example.ui.theme.BrokerDeepPurple
import com.example.ui.theme.BrokerGreen
import com.example.ui.theme.BrokerIndigo
import com.example.ui.theme.BrokerRed
import com.example.ui.viewmodel.MarketplaceFilter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExplorePropertiesScreen(
  screenTitle: String,
  canAddProperty: Boolean,
  isFavoritesScreen: Boolean = false,
  initialMapMode: Boolean = false,
  propertiesState: UiState<List<PropertyListing>>,
  filter: MarketplaceFilter,
  favoriteIds: Set<String>,
  unlockedIds: Set<String>,
  membership: PremiumMembership?,
  currentUserId: String,
  onBack: () -> Unit,
  onSearchChange: (String) -> Unit,
  onListingTypeChange: (ListingType?) -> Unit = {},
  onPriceRangeChange: (Long?, Long?) -> Unit,
  onCategoryChange: (PropertyCategory?) -> Unit,
  onToggleFavorite: (String) -> Unit,
  onOpenFavorites: () -> Unit,
  onSaveSearchAlert: (() -> Unit)? = null,
  onOpenPropertyDetail: (String) -> Unit,
  onAddProperty: () -> Unit,
) {
  var isMapView by remember(initialMapMode) { mutableStateOf(initialMapMode) }
  var showFilterDialog by remember { mutableStateOf(false) }
  var minPriceInput by remember(filter.minPrice) { mutableStateOf(filter.minPrice?.toString() ?: "") }
  var maxPriceInput by remember(filter.maxPrice) { mutableStateOf(filter.maxPrice?.toString() ?: "") }
  var selectedCategoryInput by remember(filter.category) { mutableStateOf(filter.category) }

  val isPremiumActive =
    membership?.premiumActive == true && membership.expiresAtMillis > System.currentTimeMillis()

  if (showFilterDialog) {
    AlertDialog(
      onDismissRequest = { showFilterDialog = false },
      title = { Text("Filter Properties", fontWeight = FontWeight.Bold, color = BrokerIndigo) },
      text = {
        Column(
          verticalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.verticalScroll(rememberScrollState()),
        ) {
          Text(
            text = "Property Type",
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = BrokerIndigo,
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            FilterChip(
              selected = selectedCategoryInput == null,
              onClick = { selectedCategoryInput = null },
              label = { Text("All") },
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = BrokerIndigo,
                  selectedLabelColor = Color.White,
                ),
            )
            PropertyCategory.entries.forEach { cat ->
              FilterChip(
                selected = selectedCategoryInput == cat,
                onClick = { selectedCategoryInput = cat },
                label = { Text(cat.label) },
                colors =
                  FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BrokerIndigo,
                    selectedLabelColor = Color.White,
                  ),
              )
            }
          }
          Text(
            text = "Quick Price Range",
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = BrokerIndigo,
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            PriceBracketPreset.entries.forEach { preset ->
              val isPresetSelected =
                minPriceInput.toLongOrNull() == preset.minPrice &&
                  maxPriceInput.toLongOrNull() == preset.maxPrice
              FilterChip(
                selected = isPresetSelected,
                onClick = {
                  minPriceInput = preset.minPrice?.toString() ?: ""
                  maxPriceInput = preset.maxPrice?.toString() ?: ""
                },
                label = { Text(preset.label) },
                colors =
                  FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BrokerIndigo,
                    selectedLabelColor = Color.White,
                  ),
              )
            }
          }
          OutlinedTextField(
            value = minPriceInput,
            onValueChange = { minPriceInput = it.filter(Char::isDigit) },
            label = { Text("Min Price (₹)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = maxPriceInput,
            onValueChange = { maxPriceInput = it.filter(Char::isDigit) },
            label = { Text("Max Price (₹)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onPriceRangeChange(minPriceInput.toLongOrNull(), maxPriceInput.toLongOrNull())
            onCategoryChange(selectedCategoryInput)
            showFilterDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = BrokerBlue),
        ) {
          Text("Apply")
        }
      },
      dismissButton = {
        TextButton(
          onClick = {
            minPriceInput = ""
            maxPriceInput = ""
            selectedCategoryInput = null
            onPriceRangeChange(null, null)
            onCategoryChange(null)
            showFilterDialog = false
          }
        ) {
          Text("Clear")
        }
      },
    )
  }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = { Text(screenTitle, fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(
            onClick = { isMapView = !isMapView },
            modifier = Modifier.testTag("toggle_map_list_button"),
          ) {
            Icon(
              imageVector = if (isMapView) Icons.AutoMirrored.Filled.ViewList else Icons.Filled.Map,
              contentDescription = if (isMapView) "Switch to List View" else "Switch to Map View",
            )
          }
          if (!isFavoritesScreen && onSaveSearchAlert != null) {
            IconButton(
              onClick = onSaveSearchAlert,
              modifier = Modifier.testTag("save_search_alert_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.NotificationAdd,
                contentDescription = "Save Search Alert",
              )
            }
          }
          if (!isFavoritesScreen) {
            IconButton(
              onClick = onOpenFavorites,
              modifier = Modifier.testTag("list_favorites_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = "Open Favorites",
              )
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
      Column(modifier = Modifier.widthIn(max = 640.dp).fillMaxSize()) {
        // White elevated search + purple Filter bar matching activity_property_list.xml
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shadowElevation = 2.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 6.dp),
            ) {
              OutlinedTextField(
                value = filter.searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search name, location, or price...") },
                trailingIcon = {
                  if (filter.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                      Icon(Icons.Filled.Close, contentDescription = "Clear search")
                    }
                  }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.weight(1f).testTag("search_properties_input"),
              )
              Button(
                onClick = { showFilterDialog = true },
                shape = MaterialTheme.shapes.small,
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = BrokerDeepPurple,
                    contentColor = Color.White,
                  ),
                modifier = Modifier.height(52.dp).testTag("filter_properties_button"),
              ) {
                Text("Filter", fontWeight = FontWeight.SemiBold)
              }
            }

            if (!isFavoritesScreen) {
              QuickPropertyFilterRow(
                selectedListingType = filter.listingType,
                selectedCategory = filter.category,
                showAllChip = true,
                onClearAll = {
                  onListingTypeChange(null)
                  onCategoryChange(null)
                },
                onToggleListingType = { clickedType ->
                  val next = if (filter.listingType == clickedType) null else clickedType
                  onListingTypeChange(next)
                },
                onToggleCategory = { clickedCat ->
                  val next = if (filter.category == clickedCat) null else clickedCat
                  onCategoryChange(next)
                },
                contentPadding = PaddingValues(horizontal = 12.dp),
                testTagPrefix = "explore_quick_filter",
                modifier = Modifier.padding(bottom = 8.dp),
              )
            }
          }
        }

        if (filter.minPrice != null || filter.maxPrice != null || filter.category != null) {
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
          ) {
            if (filter.minPrice != null || filter.maxPrice != null) {
              InputChip(
                selected = true,
                onClick = { onPriceRangeChange(null, null) },
                label = {
                  Text(
                    text =
                      "Price: ${PropertyFormatters.formatPriceRangeLabel(filter.minPrice, filter.maxPrice)}",
                    fontWeight = FontWeight.SemiBold,
                  )
                },
                trailingIcon = {
                  Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Clear price filter",
                    modifier = Modifier.size(16.dp),
                  )
                },
                colors =
                  InputChipDefaults.inputChipColors(
                    selectedContainerColor = BrokerIndigo.copy(alpha = 0.14f),
                    selectedLabelColor = BrokerIndigo,
                    selectedTrailingIconColor = BrokerIndigo,
                  ),
                modifier = Modifier.testTag("active_price_filter_chip"),
              )
            }
            filter.category?.let { activeCat ->
              InputChip(
                selected = true,
                onClick = { onCategoryChange(null) },
                label = { Text(activeCat.label, fontWeight = FontWeight.SemiBold) },
                trailingIcon = {
                  Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Clear property type filter",
                    modifier = Modifier.size(16.dp),
                  )
                },
                colors =
                  InputChipDefaults.inputChipColors(
                    selectedContainerColor = BrokerBlue.copy(alpha = 0.14f),
                    selectedLabelColor = BrokerBlue,
                    selectedTrailingIconColor = BrokerBlue,
                  ),
                modifier = Modifier.testTag("active_category_filter_chip"),
              )
            }
          }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
          when (propertiesState) {
            is UiState.Loading -> {
              CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is UiState.Error -> {
              Text(
                text = propertiesState.message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
              )
            }
            is UiState.Success -> {
              val items = propertiesState.data
              if (isMapView) {
                PropertyMapView(
                  properties = items,
                  favoriteIds = favoriteIds,
                  onToggleFavorite = onToggleFavorite,
                  onOpenPropertyDetail = onOpenPropertyDetail,
                  modifier = Modifier.fillMaxSize(),
                )
              } else if (items.isEmpty()) {
                Text(
                  text =
                    if (isFavoritesScreen)
                      "No favorite properties yet.\nTap the heart icon on any property to save it."
                    else "No properties found",
                  fontSize = 16.sp,
                  textAlign = TextAlign.Center,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.align(Alignment.Center).padding(24.dp),
                )
              } else {
                LazyColumn(
                  contentPadding = PaddingValues(12.dp),
                  verticalArrangement = Arrangement.spacedBy(12.dp),
                  modifier = Modifier.fillMaxSize(),
                ) {
                  items(items, key = { it.id }, contentType = { "property_card" }) { property ->
                    val isOwner = property.ownerId == currentUserId
                    val isUnlocked = isOwner || isPremiumActive || property.id in unlockedIds
                    CompactPropertyCard(
                      property = property,
                      isFavorite = property.id in favoriteIds,
                      isOwner = isOwner,
                      isUnlocked = isUnlocked,
                      onToggleFavorite = { onToggleFavorite(property.id) },
                      onClick = { onOpenPropertyDetail(property.id) },
                    )
                  }
                }
              }
            }
          }
        }

        if (canAddProperty) {
          Button(
            onClick = onAddProperty,
            shape = MaterialTheme.shapes.small,
            colors =
              ButtonDefaults.buttonColors(
                containerColor = BrokerBlue,
                contentColor = Color.White,
              ),
            modifier =
              Modifier.fillMaxWidth()
                .padding(16.dp)
                .height(56.dp)
                .testTag("add_property_button"),
          ) {
            Text(
              text = "+ Add New Property",
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun CompactPropertyCard(
  property: PropertyListing,
  isFavorite: Boolean,
  isOwner: Boolean,
  isUnlocked: Boolean,
  onToggleFavorite: () -> Unit,
  onClick: () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(10.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(10.dp))
        .clickable(onClick = onClick)
        .testTag("property_card_${property.id}"),
  ) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth().padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
    ) {
      if (property.photoKeys.isNotEmpty()) {
        PropertyPhotoImage(
          photoKey = property.photoKeys.firstOrNull(),
          contentDescription = property.title,
          modifier = Modifier.size(84.dp).clip(RoundedCornerShape(8.dp)),
        )
      }

      Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = property.title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = BrokerIndigo,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )
          IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.size(48.dp).testTag("favorite_heart_${property.id}"),
          ) {
            Icon(
              imageVector =
                if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
              contentDescription =
                if (isFavorite) "Remove from Favorites" else "Save to Favorites",
              tint =
                if (isFavorite) BrokerRed else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        Text(
          text = property.location,
          fontSize = 14.sp,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        val formattedPrice =
          remember(property.price, property.listingType) {
            PropertyFormatters.formatInrPrice(property.price, property.listingType)
          }
        val specsLine =
          remember(property.propertyCategory, property.areaSqFt, property.bedrooms) {
            val categoryLabel = PropertyCategory.fromRaw(property.propertyCategory).label
            buildString {
              append(categoryLabel)
              if (property.areaSqFt > 0L) {
                append(" • ")
                append(PropertyFormatters.formatAreaSqFt(property.areaSqFt))
              }
              if (property.bedrooms > 0L) {
                append(" • ")
                append("${property.bedrooms} BHK")
              }
            }
          }
        val listingTypeBadge =
          remember(property.listingType) {
            ListingType.fromRaw(property.listingType).label.uppercase()
          }
        Text(
          text = formattedPrice,
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold,
          color = BrokerGreen,
        )
        Text(
          text = specsLine,
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold,
          color = BrokerBlue,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "Owner: ${property.ownerName}",
          fontSize = 13.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Row(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = listingTypeBadge,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = BrokerDeepPurple,
          )
          if (property.hasVideo) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Filled.Videocam,
                contentDescription = "Has video",
                tint = BrokerDeepPurple,
                modifier = Modifier.size(14.dp),
              )
              Spacer(Modifier.width(3.dp))
              Text("Video", fontSize = 12.sp, color = BrokerDeepPurple)
            }
          }
          if (PremiumSchedule.isPremiumFeatureEnabled()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = if (isUnlocked) Icons.Filled.LockOpen else Icons.Filled.Lock,
                contentDescription = null,
                tint = if (isUnlocked) BrokerGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(13.dp),
              )
              Spacer(Modifier.width(3.dp))
              Text(
                text = if (isOwner) "Yours" else if (isUnlocked) "Unlocked" else "Locked",
                fontSize = 12.sp,
                color = if (isUnlocked) BrokerGreen else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          } else if (isOwner) {
            Text(
              text = "Your Listing",
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = BrokerGreen,
            )
          }
        }
      }
    }
  }
}
