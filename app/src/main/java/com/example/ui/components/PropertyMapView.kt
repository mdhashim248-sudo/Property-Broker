package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.GeoPoint
import com.example.data.LocationResolver
import com.example.data.WorldPoint
import com.example.data.model.ListingType
import com.example.data.model.PropertyCategory
import com.example.data.model.PropertyFormatters
import com.example.data.model.PropertyListing
import com.example.ui.theme.BrokerBlue
import com.example.ui.theme.BrokerGreen
import com.example.ui.theme.BrokerIndigo
import com.example.ui.theme.BrokerPurple
import com.example.ui.theme.BrokerRed
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

private data class PinnedProperty(
  val property: PropertyListing,
  val coordinate: GeoPoint,
)

@Composable
fun PropertyMapView(
  properties: List<PropertyListing>,
  favoriteIds: Set<String>,
  onToggleFavorite: (String) -> Unit,
  onOpenPropertyDetail: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val density = LocalDensity.current

  val pinnedProperties =
    remember(properties) {
      properties.map { prop ->
        PinnedProperty(property = prop, coordinate = LocationResolver.resolveCoordinates(prop))
      }
    }

  var centerLat by remember { mutableDoubleStateOf(LocationResolver.DEFAULT_CENTER.latitude) }
  var centerLng by remember { mutableDoubleStateOf(LocationResolver.DEFAULT_CENTER.longitude) }
  var zoom by remember { mutableDoubleStateOf(LocationResolver.DEFAULT_ZOOM) }
  var selectedPropertyId by remember { mutableStateOf<String?>(null) }

  val selectedProperty =
    remember(pinnedProperties, selectedPropertyId) {
      pinnedProperties.firstOrNull { it.property.id == selectedPropertyId }?.property
    }

  // Clear selection if filtered out
  LaunchedEffect(properties) {
    if (selectedPropertyId != null && properties.none { it.id == selectedPropertyId }) {
      selectedPropertyId = null
    }
  }

  BoxWithConstraints(
    modifier =
      modifier
        .fillMaxSize()
        .clipToBounds()
        .background(Color(0xFFE7EEF4))
        .testTag("property_map_container"),
  ) {
    val widthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
    val heightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)

    // Auto-fit map viewport whenever the set of visible properties changes
    val propertyIdsKey = remember(pinnedProperties) { pinnedProperties.map { it.property.id } }
    LaunchedEffect(propertyIdsKey, widthPx, heightPx) {
      val viewport =
        LocationResolver.computeFitViewport(
          points = pinnedProperties.map { it.coordinate },
          viewportWidthPx = widthPx,
          viewportHeightPx = heightPx,
        )
      centerLat = viewport.center.latitude
      centerLng = viewport.center.longitude
      zoom = viewport.zoom
    }

    val centerWorld =
      remember(centerLat, centerLng, zoom) {
        LocationResolver.latLngToWorldPx(GeoPoint(centerLat, centerLng), zoom)
      }

    // Gesture surface for smooth pan and pinch-to-zoom
    Box(
      modifier =
        Modifier.fillMaxSize().pointerInput(Unit) {
          detectTransformGestures { _, pan, zoomChange, _ ->
            val currentZoom = zoom
            val nextZoom =
              if (zoomChange != 1f && zoomChange.isFinite() && zoomChange > 0f) {
                (currentZoom + ln(zoomChange.toDouble()) / ln(2.0)).coerceIn(
                  LocationResolver.MIN_ZOOM,
                  LocationResolver.MAX_ZOOM,
                )
              } else {
                currentZoom
              }
            val currentWorld =
              LocationResolver.latLngToWorldPx(GeoPoint(centerLat, centerLng), nextZoom)
            val shiftedWorld = WorldPoint(currentWorld.x - pan.x, currentWorld.y - pan.y)
            val nextGeo = LocationResolver.worldPxToLatLng(shiftedWorld, nextZoom)
            centerLat = nextGeo.latitude
            centerLng = nextGeo.longitude
            zoom = nextZoom
          }
        },
    ) {
      // Layer 1: Crisp Stylized Cartographic Backdrop (renders immediately even offline)
      CartographicBackdropCanvas(
        centerWorld = centerWorld,
        zoom = zoom,
        modifier = Modifier.fillMaxSize(),
      )

      // Layer 2: Live HTTPS Map Tiles (Carto Voyager)
      val tileZ = floor(zoom).toInt().coerceIn(LocationResolver.MIN_ZOOM.toInt(), LocationResolver.MAX_ZOOM.toInt())
      val tileScaleFactor = 2.0.pow(zoom - tileZ)
      val tileDisplayPx = (LocationResolver.TILE_SIZE * tileScaleFactor).toFloat()
      val centerWorldAtTileZ =
        LocationResolver.latLngToWorldPx(GeoPoint(centerLat, centerLng), tileZ.toDouble())

      val halfWInTileZ = (widthPx / 2.0) / tileScaleFactor
      val halfHInTileZ = (heightPx / 2.0) / tileScaleFactor
      val minTileX = floor((centerWorldAtTileZ.x - halfWInTileZ) / LocationResolver.TILE_SIZE).toInt()
      val maxTileX = floor((centerWorldAtTileZ.x + halfWInTileZ) / LocationResolver.TILE_SIZE).toInt()
      val maxTileIndex = (1 shl tileZ) - 1
      val minTileY =
        floor((centerWorldAtTileZ.y - halfHInTileZ) / LocationResolver.TILE_SIZE)
          .toInt()
          .coerceIn(0, maxTileIndex)
      val maxTileY =
        floor((centerWorldAtTileZ.y + halfHInTileZ) / LocationResolver.TILE_SIZE)
          .toInt()
          .coerceIn(0, maxTileIndex)
      val tileDisplayDp = with(density) { (tileDisplayPx + 1f).toDp() }

      val tileCount = (maxTileX - minTileX + 1) * (maxTileY - minTileY + 1)
      if (tileCount in 1..48) {
        for (tx in minTileX..maxTileX) {
          val wrappedX = ((tx % (1 shl tileZ)) + (1 shl tileZ)) % (1 shl tileZ)
          val screenX =
            (widthPx / 2.0 + (tx * LocationResolver.TILE_SIZE - centerWorldAtTileZ.x) * tileScaleFactor)
              .roundToInt()
          for (ty in minTileY..maxTileY) {
            val screenY =
              (heightPx / 2.0 + (ty * LocationResolver.TILE_SIZE - centerWorldAtTileZ.y) * tileScaleFactor)
                .roundToInt()
            val subdomain = when ((wrappedX + ty) % 3) {
              0 -> "a"
              1 -> "b"
              else -> "c"
            }
            val tileUrl =
              "https://$subdomain.basemaps.cartocdn.com/rastertiles/voyager/$tileZ/$wrappedX/$ty.png"

            key("$tileZ/$wrappedX/$ty") {
              val request =
                remember(tileUrl) {
                  ImageRequest.Builder(context)
                    .data(tileUrl)
                    .addHeader("User-Agent", "PropertyBrokerAndroid/1.0")
                    .crossfade(true)
                    .build()
                }
              AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier =
                  Modifier.offset { IntOffset(screenX, screenY) }
                    .size(tileDisplayDp),
              )
            }
          }
        }
      }

      // Layer 3: Property Price-Pill Pins
      val pinHalfWidthPx = with(density) { 44.dp.toPx() }
      val pinHeightPx = with(density) { 44.dp.toPx() }
      pinnedProperties.forEach { pinned ->
        val prop = pinned.property
        val worldPt = LocationResolver.latLngToWorldPx(pinned.coordinate, zoom)
        val screenX = (widthPx / 2.0 + (worldPt.x - centerWorld.x)).toFloat()
        val screenY = (heightPx / 2.0 + (worldPt.y - centerWorld.y)).toFloat()

        // Only render pins near the viewport for smooth performance
        if (screenX in -120f..(widthPx + 120f) && screenY in -120f..(heightPx + 120f)) {
          val isSelected = prop.id == selectedPropertyId
          val isFavorite = prop.id in favoriteIds
          val pinColor =
            when {
              isSelected -> BrokerIndigo
              prop.listingType == ListingType.RENT.name -> BrokerRed
              prop.listingType == ListingType.HEAVY_DEPOSIT.name -> BrokerPurple
              else -> BrokerBlue
            }

          key(prop.id) {
            PropertyMapPin(
              priceLabel = PropertyFormatters.formatCompactPinPrice(prop.price, prop.listingType),
              pinColor = pinColor,
              isSelected = isSelected,
              isFavorite = isFavorite,
              onClick = {
                selectedPropertyId = prop.id
                centerLat = pinned.coordinate.latitude
                centerLng = pinned.coordinate.longitude
              },
              modifier =
                Modifier.zIndex(if (isSelected) 3f else 1f)
                  .offset {
                    IntOffset(
                      x = (screenX - pinHalfWidthPx).roundToInt(),
                      y = (screenY - pinHeightPx).roundToInt(),
                    )
                  }
                  .testTag("map_pin_${prop.id}"),
            )
          }
        }
      }
    }

    // Floating Top-Left Summary + Color Legend
    Surface(
      color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
      shape = RoundedCornerShape(12.dp),
      shadowElevation = 4.dp,
      modifier = Modifier.align(Alignment.TopStart).padding(12.dp).testTag("map_legend_card"),
    ) {
      Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      ) {
        Text(
          text = "${properties.size} ${if (properties.size == 1) "Property" else "Properties"}",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
          color = BrokerIndigo,
        )
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          LegendDot(color = BrokerBlue, label = "Sale")
          LegendDot(color = BrokerRed, label = "Rent")
          LegendDot(color = BrokerPurple, label = "Deposit")
        }
      }
    }

    // Floating Top-Right Zoom & Fit-All Controls (48dp touch targets)
    Surface(
      color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
      shape = RoundedCornerShape(12.dp),
      shadowElevation = 4.dp,
      modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
          onClick = { zoom = (zoom + 1.0).coerceAtMost(LocationResolver.MAX_ZOOM) },
          modifier = Modifier.size(48.dp).testTag("map_zoom_in"),
        ) {
          Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "Zoom in",
            tint = BrokerIndigo,
          )
        }
        HorizontalDivider(modifier = Modifier.width(32.dp))
        IconButton(
          onClick = { zoom = (zoom - 1.0).coerceAtLeast(LocationResolver.MIN_ZOOM) },
          modifier = Modifier.size(48.dp).testTag("map_zoom_out"),
        ) {
          Icon(
            imageVector = Icons.Filled.Remove,
            contentDescription = "Zoom out",
            tint = BrokerIndigo,
          )
        }
        HorizontalDivider(modifier = Modifier.width(32.dp))
        IconButton(
          onClick = {
            val viewport =
              LocationResolver.computeFitViewport(
                points = pinnedProperties.map { it.coordinate },
                viewportWidthPx = widthPx,
                viewportHeightPx = heightPx,
              )
            centerLat = viewport.center.latitude
            centerLng = viewport.center.longitude
            zoom = viewport.zoom
          },
          modifier = Modifier.size(48.dp).testTag("map_fit_all"),
        ) {
          Icon(
            imageVector = Icons.Filled.FilterCenterFocus,
            contentDescription = "Fit all properties",
            tint = BrokerIndigo,
          )
        }
      }
    }

    // Empty state overlay when no listings match current filter
    if (properties.isEmpty()) {
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier =
          Modifier.align(Alignment.Center)
            .padding(24.dp)
            .testTag("map_empty_state"),
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.padding(20.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.Map,
            contentDescription = null,
            tint = BrokerIndigo,
            modifier = Modifier.size(32.dp),
          )
          Text(
            text = "No properties to pin",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = BrokerIndigo,
          )
          Text(
            text = "Adjust your search or filters, or add a listing to see pins on the map.",
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    // Bottom Property Preview Card when a pin is tapped
    AnimatedVisibility(
      visible = selectedProperty != null,
      enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
      exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
      modifier =
        Modifier.align(Alignment.BottomCenter)
          .fillMaxWidth()
          .navigationBarsPadding()
          .padding(12.dp),
    ) {
      selectedProperty?.let { prop ->
        MapPropertyPreviewCard(
          property = prop,
          isFavorite = prop.id in favoriteIds,
          onToggleFavorite = { onToggleFavorite(prop.id) },
          onClose = { selectedPropertyId = null },
          onViewDetails = { onOpenPropertyDetail(prop.id) },
        )
      }
    }
  }
}

@Composable
private fun CartographicBackdropCanvas(
  centerWorld: WorldPoint,
  zoom: Double,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier) {
    // Soft map base tone
    drawRect(color = Color(0xFFEBF1F5))

    val gridStep = (72f * (1f + ((zoom - floor(zoom)).toFloat() * 0.5f))).coerceIn(48f, 128f)
    val offsetX = (-(centerWorld.x % gridStep)).toFloat()
    val offsetY = (-(centerWorld.y % gridStep)).toFloat()

    // Subtle secondary street grid
    var x = offsetX
    while (x < size.width) {
      drawLine(
        color = Color(0xFFD8E3EC),
        start = Offset(x, 0f),
        end = Offset(x, size.height),
        strokeWidth = 1.2f,
      )
      x += gridStep
    }
    var y = offsetY
    while (y < size.height) {
      drawLine(
        color = Color(0xFFD8E3EC),
        start = Offset(0f, y),
        end = Offset(size.width, y),
        strokeWidth = 1.2f,
      )
      y += gridStep
    }

    // Major arterial road grid lines
    val majorStep = gridStep * 3f
    val majorOffsetX = (-(centerWorld.x % majorStep)).toFloat()
    val majorOffsetY = (-(centerWorld.y % majorStep)).toFloat()
    var mx = majorOffsetX
    while (mx < size.width) {
      drawLine(
        color = Color(0xFFFDFDFD),
        start = Offset(mx, 0f),
        end = Offset(mx, size.height),
        strokeWidth = 4f,
      )
      mx += majorStep
    }
    var my = majorOffsetY
    while (my < size.height) {
      drawLine(
        color = Color(0xFFFDFDFD),
        start = Offset(0f, my),
        end = Offset(size.width, my),
        strokeWidth = 4f,
      )
      my += majorStep
    }
  }
}

@Composable
private fun PropertyMapPin(
  priceLabel: String,
  pinColor: Color,
  isSelected: Boolean,
  isFavorite: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier =
      modifier
        .clickable(onClick = onClick)
        .padding(horizontal = 2.dp),
  ) {
    Surface(
      color = pinColor,
      shape = RoundedCornerShape(999.dp),
      border = BorderStroke(if (isSelected) 2.5.dp else 1.5.dp, Color.White),
      shadowElevation = if (isSelected) 8.dp else 4.dp,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.padding(horizontal = if (isSelected) 11.dp else 9.dp, vertical = 5.dp),
      ) {
        if (isFavorite) {
          Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = null,
            tint = Color(0xFFFFCDD2),
            modifier = Modifier.size(12.dp),
          )
        }
        Text(
          text = priceLabel,
          color = Color.White,
          fontSize = if (isSelected) 13.sp else 12.sp,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
        )
      }
    }
    // Downward pointer notch
    Canvas(modifier = Modifier.size(width = 12.dp, height = 7.dp)) {
      val path =
        Path().apply {
          moveTo(0f, 0f)
          lineTo(size.width, 0f)
          lineTo(size.width / 2f, size.height)
          close()
        }
      drawPath(path = path, color = pinColor)
    }
  }
}

@Composable
private fun LegendDot(color: Color, label: String) {
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun MapPropertyPreviewCard(
  property: PropertyListing,
  isFavorite: Boolean,
  onToggleFavorite: () -> Unit,
  onClose: () -> Unit,
  onViewDetails: () -> Unit,
) {
  val categoryLabel = PropertyCategory.fromRaw(property.propertyCategory).label
  val listingLabel = ListingType.fromRaw(property.listingType).label

  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    modifier = Modifier.fillMaxWidth().testTag("map_preview_card"),
  ) {
    Column(
      verticalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier.fillMaxWidth().padding(12.dp),
    ) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
      ) {
        PropertyPhotoImage(
          photoKey = property.photoKeys.firstOrNull(),
          contentDescription = property.title,
          modifier = Modifier.size(76.dp).clip(RoundedCornerShape(10.dp)),
        )

        Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = property.title,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = BrokerIndigo,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f),
            )
            IconButton(
              onClick = onClose,
              modifier = Modifier.size(36.dp).testTag("map_preview_close"),
            ) {
              Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Close preview",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
              )
            }
          }

          Text(
            text = PropertyFormatters.formatInrPrice(property.price, property.listingType),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = BrokerGreen,
          )

          val specs =
            buildString {
              append(listingLabel)
              append(" • ")
              append(categoryLabel)
              if (property.areaSqFt > 0L) {
                append(" • ")
                append(PropertyFormatters.formatAreaSqFt(property.areaSqFt))
              }
            }
          Text(
            text = specs,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = BrokerBlue,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )

          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Filled.LocationOn,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(3.dp))
            Text(
              text = property.location,
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }

      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(10.dp),
        ) {
          IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.size(48.dp).testTag("map_preview_favorite"),
          ) {
            Icon(
              imageVector =
                if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
              contentDescription =
                if (isFavorite) "Remove from Favorites" else "Save to Favorites",
              tint = if (isFavorite) BrokerRed else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Button(
          onClick = onViewDetails,
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(containerColor = BrokerIndigo, contentColor = Color.White),
          modifier =
            Modifier.weight(1f)
              .height(48.dp)
              .testTag("map_preview_view_details"),
        ) {
          Text("View Property Details", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
