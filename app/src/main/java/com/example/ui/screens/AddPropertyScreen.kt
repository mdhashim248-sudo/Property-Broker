package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Elevator
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WorkspacePremium
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.GeoPoint
import com.example.data.LocationResolver
import com.example.data.model.ListingType
import com.example.data.model.PropertyCategory
import com.example.data.model.PropertyMediaLimits
import com.example.ui.components.MapLocationPickerDialog
import com.example.ui.components.PropertyPhotoImage
import com.example.ui.theme.BrokerBlue
import com.example.ui.theme.BrokerDeepPurple
import com.example.ui.theme.BrokerGreen
import com.example.ui.theme.BrokerIndigo
import kotlinx.coroutines.launch
import java.util.Locale

private data class MediaAttachment(
  val key: String,
  val sizeBytes: Long,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddPropertyScreen(
  listingType: ListingType,
  defaultOwnerName: String,
  defaultPhone: String,
  defaultEmail: String,
  isSubmitting: Boolean,
  requiresPremiumGate: Boolean = false,
  onOpenPremium: () -> Unit = {},
  onBack: () -> Unit,
  onSubmit: (
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
    latitude: Double?,
    longitude: Double?,
    floor: String,
    depositAmount: Long?,
    monthlyRent: Long?,
    buildingAge: String,
    hasLift: Boolean,
  ) -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var isResolvingLocation by remember { mutableStateOf(false) }

  var title by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var priceText by remember { mutableStateOf("") }
  var floorText by remember { mutableStateOf("") }
  var depositText by remember { mutableStateOf("") }
  var monthlyRentText by remember { mutableStateOf("") }
  var areaSqFtText by remember { mutableStateOf("") }
  var bedroomsText by remember { mutableStateOf("") }
  var buildingAgeText by remember { mutableStateOf("") }
  var hasLift by remember { mutableStateOf(false) }
  var selectedCategory by remember { mutableStateOf(PropertyCategory.APARTMENT) }
  var location by remember { mutableStateOf("") }
  var pickedGeoPoint by remember { mutableStateOf<GeoPoint?>(null) }
  var showMapPicker by remember { mutableStateOf(false) }
  var ownerName by remember { mutableStateOf(defaultOwnerName) }
  var contactPhone by remember { mutableStateOf(defaultPhone) }
  var validationError by remember { mutableStateOf<String?>(null) }
  var mediaNotice by remember { mutableStateOf<String?>(null) }

  val selectedPhotos = remember { mutableStateListOf<MediaAttachment>() }
  var selectedVideo by remember { mutableStateOf<MediaAttachment?>(null) }

  val currentTotalBytes =
    selectedPhotos.sumOf { it.sizeBytes } + (selectedVideo?.sizeBytes ?: 0L)
  val usedMbText = formatBytesAsMb(currentTotalBytes)
  val usageFraction =
    (currentTotalBytes.toFloat() / PropertyMediaLimits.MAX_TOTAL_BYTES.toFloat()).coerceIn(0f, 1f)

  // Single Unified Media Picker (Photos + Video together, min 1 photo, max 10 photos, 8 MB total)
  val unifiedMediaPicker =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.PickMultipleVisualMedia(PropertyMediaLimits.MAX_PHOTOS + 1),
      onResult = { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        var runningBytes =
          selectedPhotos.sumOf { it.sizeBytes } + (selectedVideo?.sizeBytes ?: 0L)
        var skippedSizeCount = 0
        var skippedCapCount = 0

        uris.forEach { uri ->
          val mimeType = context.contentResolver.getType(uri).orEmpty()
          val isVideo = mimeType.startsWith("video/", ignoreCase = true)
          val itemBytes = queryUriSizeBytes(context, uri, isVideo)

          if (isVideo) {
            val previousVideoBytes = selectedVideo?.sizeBytes ?: 0L
            val candidateBytes = (runningBytes - previousVideoBytes) + itemBytes
            if (candidateBytes <= PropertyMediaLimits.MAX_TOTAL_BYTES) {
              selectedVideo = MediaAttachment(uri.toString(), itemBytes)
              runningBytes = candidateBytes
            } else {
              skippedSizeCount++
            }
          } else {
            val uriStr = uri.toString()
            if (selectedPhotos.any { it.key == uriStr }) {
              return@forEach
            }
            if (selectedPhotos.size >= PropertyMediaLimits.MAX_PHOTOS) {
              skippedCapCount++
              return@forEach
            }
            if (runningBytes + itemBytes <= PropertyMediaLimits.MAX_TOTAL_BYTES) {
              selectedPhotos.add(MediaAttachment(uriStr, itemBytes))
              runningBytes += itemBytes
            } else {
              skippedSizeCount++
            }
          }
        }

        mediaNotice =
          when {
            skippedSizeCount > 0 && skippedCapCount > 0 ->
              "Skipped $skippedSizeCount file(s) exceeding 8.00 MB limit and $skippedCapCount over the 10-photo cap."
            skippedSizeCount > 0 ->
              "Skipped $skippedSizeCount file(s) to stay within the 8.00 MB per-property limit."
            skippedCapCount > 0 ->
              "Maximum 10 photos per property; skipped $skippedCapCount extra photo(s)."
            else -> null
          }
      },
    )

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = {
          Text(
            text =
              when (listingType) {
                ListingType.RENT -> "Add Rental Property"
                ListingType.HEAVY_DEPOSIT -> "Add Heavy Deposit Property"
                ListingType.SALE -> "Add New Property"
              },
            fontWeight = FontWeight.Bold,
          )
        },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = BrokerIndigo,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
          ),
      )
    },
  ) { innerPadding ->
    Box(
      contentAlignment = Alignment.TopCenter,
      modifier = Modifier.fillMaxSize().padding(innerPadding).background(MaterialTheme.colorScheme.background),
    ) {
      if (requiresPremiumGate) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
          modifier =
            Modifier.widthIn(max = 520.dp)
              .fillMaxSize()
              .padding(24.dp)
              .testTag("add_property_premium_gate"),
        ) {
          Surface(
            color = BrokerDeepPurple.copy(alpha = 0.14f),
            shape = CircleShape,
            modifier = Modifier.size(72.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Filled.WorkspacePremium,
                contentDescription = null,
                tint = BrokerDeepPurple,
                modifier = Modifier.size(38.dp),
              )
            }
          }
          Spacer(Modifier.height(16.dp))
          Text(
            text = "Premium Required to Post Property",
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            color = BrokerIndigo,
          )
          Spacer(Modifier.height(8.dp))
          Text(
            text =
              "Activate the 7-Day Premium Pass (₹999) to post Sale, Rent, and Heavy Deposit listings and unlock all verified owner contacts.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(Modifier.height(20.dp))
          Button(
            onClick = onOpenPremium,
            colors = ButtonDefaults.buttonColors(containerColor = BrokerDeepPurple),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(54.dp).testTag("add_property_activate_premium_button"),
          ) {
            Icon(Icons.Filled.WorkspacePremium, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Get 7-Day Premium Pass (₹999)", fontWeight = FontWeight.Bold)
          }
        }
      } else {
      Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier =
          Modifier.widthIn(max = 600.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
      ) {
        Text(
          text = "Add New Property (${listingType.label})",
          fontSize = 22.sp,
          fontWeight = FontWeight.Bold,
          color = BrokerIndigo,
        )

        // Property Type selector: Apartment, Villa, Shop, Gala, House, Office, Studio, Commercial, Plot
        Card(
          shape = MaterialTheme.shapes.medium,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          modifier = Modifier.fillMaxWidth().testTag("property_category_section"),
        ) {
          Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(14.dp),
          ) {
            Text(
              text = "Property Type *",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = BrokerIndigo,
            )
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              PropertyCategory.entries.forEach { category ->
                val isSelected = selectedCategory == category
                FilterChip(
                  selected = isSelected,
                  onClick = {
                    selectedCategory = category
                    if (!category.isResidential) {
                      bedroomsText = ""
                    }
                  },
                  label = { Text(category.label) },
                  colors =
                    FilterChipDefaults.filterChipColors(
                      selectedContainerColor = BrokerIndigo,
                      selectedLabelColor = Color.White,
                    ),
                  modifier = Modifier.testTag("category_chip_${category.name.lowercase()}"),
                )
              }
            }
          }
        }

        OutlinedTextField(
          value = title,
          onValueChange = { title = it.take(100) },
          label = { Text("Title *") },
          placeholder = {
            Text(
              when (selectedCategory) {
                PropertyCategory.SHOP -> "e.g. Main Road Corner Shop"
                PropertyCategory.GALA -> "e.g. Ground Floor Industrial Gala"
                PropertyCategory.VILLA -> "e.g. 4BHK Luxury Villa"
                PropertyCategory.OFFICE -> "e.g. Furnished Commercial Office"
                PropertyCategory.PLOT -> "e.g. NA Corner Residential Plot"
                else -> "e.g. 2BHK Flat in Andheri"
              }
            )
          },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("input_title"),
        )

        OutlinedTextField(
          value = description,
          onValueChange = { description = it.take(1200) },
          label = { Text("Description") },
          placeholder = { Text("Property details...") },
          minLines = 3,
          maxLines = 5,
          modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
          value = priceText,
          onValueChange = { priceText = it.filter(Char::isDigit).take(10) },
          label = {
            Text(
              when (listingType) {
                ListingType.RENT -> "Monthly Rent (₹) *"
                ListingType.HEAVY_DEPOSIT -> "Heavy Deposit Amount (₹) *"
                ListingType.SALE -> "Price (₹) *"
              }
            )
          },
          placeholder = {
            Text(
              when (listingType) {
                ListingType.RENT -> "e.g. 35000"
                ListingType.HEAVY_DEPOSIT -> "e.g. 1500000"
                ListingType.SALE -> "e.g. 8500000"
              }
            )
          },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("input_price"),
        )

        // Rental: Floor + Deposit | Heavy Deposit: Floor + Monthly Rent
        if (listingType == ListingType.RENT || listingType == ListingType.HEAVY_DEPOSIT) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            OutlinedTextField(
              value = floorText,
              onValueChange = { floorText = it.take(40) },
              label = { Text(stringResource(R.string.field_floor_label)) },
              placeholder = { Text(stringResource(R.string.field_floor_placeholder)) },
              singleLine = true,
              modifier = Modifier.weight(1f).testTag("input_floor"),
            )

            if (listingType == ListingType.RENT) {
              OutlinedTextField(
                value = depositText,
                onValueChange = { depositText = it.filter(Char::isDigit).take(10) },
                label = { Text(stringResource(R.string.field_deposit_label)) },
                placeholder = { Text(stringResource(R.string.field_deposit_placeholder)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("input_deposit"),
              )
            } else {
              OutlinedTextField(
                value = monthlyRentText,
                onValueChange = { monthlyRentText = it.filter(Char::isDigit).take(10) },
                label = { Text(stringResource(R.string.field_monthly_rent_label)) },
                placeholder = { Text(stringResource(R.string.field_monthly_rent_placeholder)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("input_monthly_rent"),
              )
            }
          }
        }

        Row(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          OutlinedTextField(
            value = areaSqFtText,
            onValueChange = { areaSqFtText = it.filter(Char::isDigit).take(7) },
            label = { Text("Area (sq.ft) *") },
            placeholder = { Text("e.g. 1200") },
            suffix = { Text("sq.ft") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("input_area_sqft"),
          )

          if (selectedCategory.isResidential) {
            OutlinedTextField(
              value = bedroomsText,
              onValueChange = { bedroomsText = it.filter(Char::isDigit).take(2) },
              label = { Text("BHK") },
              placeholder = { Text("e.g. 2") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.width(116.dp).testTag("input_bedrooms"),
            )
          }
        }

        // Building Details (Building Age + Lift Yes/No) — shown for every property
        Card(
          shape = MaterialTheme.shapes.medium,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          modifier = Modifier.fillMaxWidth().testTag("building_details_section"),
        ) {
          Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().padding(14.dp),
          ) {
            Text(
              text = stringResource(R.string.section_building_details),
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = BrokerIndigo,
            )

            OutlinedTextField(
              value = buildingAgeText,
              onValueChange = { buildingAgeText = it.take(40) },
              label = { Text(stringResource(R.string.field_building_age_label)) },
              placeholder = { Text(stringResource(R.string.field_building_age_placeholder)) },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("input_building_age"),
            )

            val agePresets = remember { listOf("New", "1-5 Years", "5-10 Years", "10+ Years") }
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              agePresets.forEach { preset ->
                val isPresetSelected = buildingAgeText.equals(preset, ignoreCase = true)
                FilterChip(
                  selected = isPresetSelected,
                  onClick = { buildingAgeText = preset },
                  label = { Text(preset) },
                  colors =
                    FilterChipDefaults.filterChipColors(
                      selectedContainerColor = BrokerIndigo,
                      selectedLabelColor = Color.White,
                    ),
                )
              }
            }

            Row(
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.Elevator,
                  contentDescription = null,
                  tint = BrokerIndigo,
                  modifier = Modifier.size(18.dp),
                )
                Text(
                  text = stringResource(R.string.field_lift_label),
                  fontSize = 14.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }

              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                  selected = hasLift,
                  onClick = { hasLift = true },
                  label = { Text(stringResource(R.string.option_yes)) },
                  leadingIcon =
                    if (hasLift) {
                      {
                        Icon(
                          imageVector = Icons.Filled.Check,
                          contentDescription = null,
                          modifier = Modifier.size(16.dp),
                        )
                      }
                    } else null,
                  colors =
                    FilterChipDefaults.filterChipColors(
                      selectedContainerColor = BrokerGreen,
                      selectedLabelColor = Color.White,
                      selectedLeadingIconColor = Color.White,
                    ),
                  modifier = Modifier.testTag("lift_yes_chip"),
                )
                FilterChip(
                  selected = !hasLift,
                  onClick = { hasLift = false },
                  label = { Text(stringResource(R.string.option_no)) },
                  leadingIcon =
                    if (!hasLift) {
                      {
                        Icon(
                          imageVector = Icons.Filled.Check,
                          contentDescription = null,
                          modifier = Modifier.size(16.dp),
                        )
                      }
                    } else null,
                  colors =
                    FilterChipDefaults.filterChipColors(
                      selectedContainerColor = BrokerIndigo,
                      selectedLabelColor = Color.White,
                      selectedLeadingIconColor = Color.White,
                    ),
                  modifier = Modifier.testTag("lift_no_chip"),
                )
              }
            }
          }
        }

        OutlinedTextField(
          value = location,
          onValueChange = { location = it.take(120) },
          label = { Text("Location *") },
          placeholder = { Text("e.g. Andheri West, Mumbai") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("input_location"),
        )

        // Optional Map Location Picker
        Column(
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          OutlinedButton(
            onClick = { showMapPicker = true },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("pick_location_on_map_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.Map,
              contentDescription = null,
              tint = BrokerIndigo,
              modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
              text =
                stringResource(
                  if (pickedGeoPoint == null) R.string.map_picker_button_add
                  else R.string.map_picker_button_change
                ),
              color = BrokerIndigo,
              fontWeight = FontWeight.SemiBold,
            )
          }

          pickedGeoPoint?.let { pinned ->
            Surface(
              color = BrokerGreen.copy(alpha = 0.12f),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth().testTag("picked_map_pin_badge"),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  modifier = Modifier.weight(1f),
                ) {
                  Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = BrokerGreen,
                    modifier = Modifier.size(18.dp),
                  )
                  Text(
                    text =
                      String.format(
                        Locale.US,
                        "Map pin set (%.5f, %.5f)",
                        pinned.latitude,
                        pinned.longitude,
                      ),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = BrokerGreen,
                  )
                }
                IconButton(
                  onClick = { pickedGeoPoint = null },
                  modifier = Modifier.size(48.dp).testTag("clear_map_pin_button"),
                ) {
                  Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.map_picker_clear_cd),
                    tint = BrokerGreen,
                    modifier = Modifier.size(18.dp),
                  )
                }
              }
            }
          }
        }

        OutlinedTextField(
          value = ownerName,
          onValueChange = { ownerName = it.take(80) },
          label = { Text("Owner Name *") },
          placeholder = { Text("Your full name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("input_owner_name"),
        )

        OutlinedTextField(
          value = contactPhone,
          onValueChange = { contactPhone = it.take(20) },
          label = { Text("Contact Number *") },
          placeholder = { Text("9876543210") },
          supportingText = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
              )
              Spacer(Modifier.width(4.dp))
              Text("Protected in private Firestore Vault until unlocked")
            }
          },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("input_contact_phone"),
        )

        // Single Unified Media Upload Section (Photos + Video together, min 1 / max 10 photos, 8 MB limit)
        Card(
          shape = MaterialTheme.shapes.medium,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier.fillMaxWidth().testTag("unified_media_section"),
        ) {
          Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(14.dp),
          ) {
            Row(
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = "Photos & Video *",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = BrokerIndigo,
              )
              Text(
                text = "$usedMbText / 8.00 MB",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color =
                  if (usageFraction > 0.9f) MaterialTheme.colorScheme.error
                  else BrokerDeepPurple,
              )
            }

            LinearProgressIndicator(
              progress = { usageFraction },
              color =
                if (usageFraction > 0.9f) MaterialTheme.colorScheme.error
                else BrokerDeepPurple,
              trackColor = MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp)),
            )

            Button(
              onClick = {
                unifiedMediaPicker.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                )
              },
              enabled = currentTotalBytes < PropertyMediaLimits.MAX_TOTAL_BYTES,
              shape = MaterialTheme.shapes.small,
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = BrokerDeepPurple,
                  contentColor = Color.White,
                ),
              modifier = Modifier.fillMaxWidth().height(48.dp).testTag("pick_media_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.PermMedia,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(Modifier.width(8.dp))
              Text("Select Photos & Video from Gallery")
            }

            Text(
              text =
                "Photos: ${selectedPhotos.size}/10 (min 1) • Video: ${if (selectedVideo != null) "1/1" else "0/1"} • Max 8 MB total",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (mediaNotice != null) {
              Text(
                text = mediaNotice ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
              )
            }

            if (selectedPhotos.isNotEmpty()) {
              LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
              ) {
                itemsIndexed(selectedPhotos) { idx, attachment ->
                  Box(modifier = Modifier.size(96.dp)) {
                    PropertyPhotoImage(
                      photoKey = attachment.key,
                      contentDescription = "Selected property photo ${idx + 1}",
                      modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
                    )
                    Surface(
                      color = Color.Black.copy(alpha = 0.65f),
                      shape = CircleShape,
                      modifier =
                        Modifier.align(Alignment.TopEnd)
                          .padding(4.dp)
                          .size(26.dp)
                          .clickable { selectedPhotos.removeAt(idx) },
                    ) {
                      Box(contentAlignment = Alignment.Center) {
                        Icon(
                          imageVector = Icons.Filled.Close,
                          contentDescription = "Remove photo",
                          tint = Color.White,
                          modifier = Modifier.size(14.dp),
                        )
                      }
                    }
                    Surface(
                      color = Color.Black.copy(alpha = 0.65f),
                      shape = RoundedCornerShape(6.dp),
                      modifier = Modifier.align(Alignment.BottomStart).padding(4.dp),
                    ) {
                      Text(
                        text = formatShortSize(attachment.sizeBytes),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                      )
                    }
                  }
                }
              }
            }

            selectedVideo?.let { video ->
              Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                  ) {
                    Icon(
                      imageVector = Icons.Filled.Videocam,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.onSecondaryContainer,
                      modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                      text = "Video selected (${formatShortSize(video.sizeBytes)})",
                      style = MaterialTheme.typography.labelMedium,
                      color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                  }
                  IconButton(
                    onClick = { selectedVideo = null },
                    modifier = Modifier.size(48.dp).testTag("remove_video_button"),
                  ) {
                    Icon(
                      imageVector = Icons.Filled.Close,
                      contentDescription = "Remove video",
                      tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                  }
                }
              }
            }
          }
        }

        if (validationError != null) {
          Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = validationError ?: "",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onErrorContainer,
              modifier = Modifier.padding(12.dp),
            )
          }
        }

        Spacer(Modifier.height(4.dp))

        Button(
          onClick = {
            val cleanTitle = title.trim()
            val cleanLoc = location.trim()
            val cleanOwner = ownerName.trim()
            val cleanPhone = contactPhone.trim()
            val price = priceText.toLongOrNull() ?: 0L
            val areaSqFt = areaSqFtText.toLongOrNull() ?: 0L
            val bedrooms =
              if (selectedCategory.isResidential) {
                (bedroomsText.toLongOrNull() ?: 0L).coerceIn(0L, 20L)
              } else {
                0L
              }
            val photoCount = selectedPhotos.size
            val totalBytes =
              selectedPhotos.sumOf { it.sizeBytes } + (selectedVideo?.sizeBytes ?: 0L)

            validationError =
              when {
                cleanTitle.length < 3 -> "Please enter a title (at least 3 characters)."
                price <= 0L -> "Please enter a valid amount in ₹."
                areaSqFt !in 1L..1_000_000L -> "Please enter a valid area in sq.ft (1 to 10,00,000)."
                cleanLoc.length < 2 -> "Please enter a valid location."
                cleanOwner.isEmpty() -> "Please enter owner name."
                cleanPhone.length < 7 -> "Please enter a valid contact number."
                photoCount < PropertyMediaLimits.MIN_PHOTOS ->
                  "Please upload at least 1 photo (1–10 photos allowed, 8 MB limit)."
                photoCount > PropertyMediaLimits.MAX_PHOTOS ->
                  "Maximum 10 photos allowed per property."
                totalBytes > PropertyMediaLimits.MAX_TOTAL_BYTES ->
                  "Combined photos & video exceed the 8.00 MB limit."
                else -> null
              }

            if (validationError == null && !isResolvingLocation) {
              val photoKeysSnapshot = selectedPhotos.map { it.key }
              val videoKeySnapshot = selectedVideo?.key ?: ""
              val descSnapshot = description.trim()
              val floorSnapshot =
                if (listingType == ListingType.RENT || listingType == ListingType.HEAVY_DEPOSIT) {
                  floorText.trim().take(40)
                } else {
                  ""
                }
              val depositSnapshot =
                if (listingType == ListingType.RENT) depositText.toLongOrNull() else null
              val monthlyRentSnapshot =
                if (listingType == ListingType.HEAVY_DEPOSIT) monthlyRentText.toLongOrNull() else null
              val buildingAgeSnapshot = buildingAgeText.trim().take(40)
              val hasLiftSnapshot = hasLift
              val pinnedSnapshot = pickedGeoPoint
              scope.launch {
                isResolvingLocation = true
                val geo = pinnedSnapshot ?: LocationResolver.geocodeLocation(context, cleanLoc)
                isResolvingLocation = false
                onSubmit(
                  cleanTitle,
                  descSnapshot,
                  price,
                  cleanLoc,
                  cleanOwner,
                  cleanPhone,
                  defaultEmail,
                  true,
                  listingType,
                  selectedCategory,
                  bedrooms,
                  areaSqFt,
                  photoKeysSnapshot,
                  videoKeySnapshot,
                  totalBytes,
                  geo.latitude,
                  geo.longitude,
                  floorSnapshot,
                  depositSnapshot,
                  monthlyRentSnapshot,
                  buildingAgeSnapshot,
                  hasLiftSnapshot,
                )
              }
            }
          },
          enabled = !isSubmitting && !isResolvingLocation,
          shape = MaterialTheme.shapes.small,
          colors =
            ButtonDefaults.buttonColors(
              containerColor = BrokerBlue,
              contentColor = Color.White,
            ),
          modifier = Modifier.fillMaxWidth().height(56.dp).testTag("save_property_button"),
        ) {
          if (isSubmitting || isResolvingLocation) {
            CircularProgressIndicator(
              color = Color.White,
              strokeWidth = 2.dp,
              modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text("Saving Property...", fontSize = 16.sp, fontWeight = FontWeight.Bold)
          } else {
            Text("Save Property", fontSize = 18.sp, fontWeight = FontWeight.Bold)
          }
        }

        Spacer(Modifier.height(16.dp))
      }
      }
    }
  }

  if (showMapPicker) {
    MapLocationPickerDialog(
      initialPoint = pickedGeoPoint,
      initialQuery = location,
      onDismiss = { showMapPicker = false },
      onConfirm = { point, resolvedLocality ->
        pickedGeoPoint = point
        if (location.isBlank() && !resolvedLocality.isNullOrBlank()) {
          location = resolvedLocality.take(120)
        }
        showMapPicker = false
      },
    )
  }
}

private fun queryUriSizeBytes(context: Context, uri: Uri, isVideo: Boolean): Long {
  val queriedSize =
    runCatching {
      context.contentResolver
        .query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
        ?.use { cursor ->
          val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
          if (sizeIdx >= 0 && cursor.moveToFirst() && !cursor.isNull(sizeIdx)) {
            cursor.getLong(sizeIdx)
          } else {
            -1L
          }
        }
        ?: -1L
    }.getOrDefault(-1L)

  if (queriedSize > 0L) return queriedSize

  val descriptorSize =
    runCatching {
      context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
    }.getOrDefault(-1L)

  if (descriptorSize > 0L) return descriptorSize

  return if (isVideo) 1_500_000L else 250_000L
}

private fun formatBytesAsMb(bytes: Long): String {
  val mb = bytes / (1024.0 * 1024.0)
  return String.format(Locale.US, "%.2f", mb)
}

private fun formatShortSize(bytes: Long): String {
  return if (bytes >= 1024L * 1024L) {
    String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
  } else {
    "${(bytes / 1024L).coerceAtLeast(1L)} KB"
  }
}
