package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.GeoPoint
import com.example.data.LocationResolver
import com.example.data.WorldPoint
import com.example.ui.theme.BrokerBlue
import com.example.ui.theme.BrokerIndigo
import com.example.ui.theme.BrokerRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

@Composable
fun MapLocationPickerDialog(
  initialPoint: GeoPoint?,
  initialQuery: String,
  onDismiss: () -> Unit,
  onConfirm: (point: GeoPoint, resolvedLocality: String?) -> Unit,
) {
  val context = LocalContext.current
  val density = LocalDensity.current
  val scope = rememberCoroutineScope()

  val startingPoint =
    remember(initialPoint, initialQuery) {
      initialPoint
        ?: if (initialQuery.isNotBlank()) {
          LocationResolver.resolveCoordinatesForText(initialQuery)
        } else {
          LocationResolver.DEFAULT_CENTER
        }
    }

  var centerLat by remember { mutableDoubleStateOf(startingPoint.latitude) }
  var centerLng by remember { mutableDoubleStateOf(startingPoint.longitude) }
  var zoom by remember { mutableDoubleStateOf(if (initialPoint != null) 14.5 else 12.2) }
  var searchQuery by remember { mutableStateOf(initialQuery) }
  var isSearching by remember { mutableStateOf(false) }
  var resolvedAddress by remember { mutableStateOf<String?>(null) }
  var isResolvingAddress by remember { mutableStateOf(false) }

  // Debounced reverse geocoding whenever map settles on a coordinate
  LaunchedEffect(centerLat, centerLng) {
    isResolvingAddress = true
    delay(450L)
    val address = LocationResolver.reverseGeocodeLocation(context, GeoPoint(centerLat, centerLng))
    resolvedAddress = address
    isResolvingAddress = false
  }

  fun searchLocalityOnMap() {
    val q = searchQuery.trim()
    if (q.isEmpty() || isSearching) return
    scope.launch {
      isSearching = true
      val geo = LocationResolver.geocodeLocation(context, q)
      centerLat = geo.latitude
      centerLng = geo.longitude
      zoom = 14.2
      isSearching = false
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      color = MaterialTheme.colorScheme.background,
      modifier = Modifier.fillMaxSize().testTag("map_location_picker_dialog"),
    ) {
      Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        // Header Bar
        Surface(
          color = BrokerIndigo,
          shadowElevation = 4.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f).padding(start = 8.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp),
              )
              Spacer(Modifier.width(8.dp))
              Column {
                Text(
                  text = stringResource(R.string.map_picker_title),
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                  fontSize = 17.sp,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = stringResource(R.string.map_picker_subtitle),
                  color = Color.White.copy(alpha = 0.82f),
                  fontSize = 12.sp,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
            IconButton(
              onClick = onDismiss,
              modifier = Modifier.testTag("map_picker_close_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.map_picker_close_cd),
                tint = Color.White,
              )
            }
          }
        }

        // Locality Search Bar inside Map Picker
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier =
            Modifier.fillMaxWidth()
              .background(MaterialTheme.colorScheme.surface)
              .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it.take(100) },
            placeholder = { Text(stringResource(R.string.map_picker_search_placeholder)) },
            singleLine = true,
            leadingIcon = {
              Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = BrokerIndigo,
              )
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { searchLocalityOnMap() }),
            modifier = Modifier.weight(1f).testTag("map_picker_search_input"),
          )
          Button(
            onClick = { searchLocalityOnMap() },
            enabled = !isSearching && searchQuery.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = BrokerIndigo),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.height(54.dp).testTag("map_picker_search_button"),
          ) {
            if (isSearching) {
              CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp),
              )
            } else {
              Text(stringResource(R.string.map_picker_go), fontWeight = FontWeight.Bold)
            }
          }
        }

        // Interactive Map Canvas
        BoxWithConstraints(
          modifier =
            Modifier.weight(1f)
              .fillMaxWidth()
              .clipToBounds()
              .background(Color(0xFFE7EEF4))
              .testTag("map_picker_canvas"),
        ) {
          val widthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
          val heightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)

          val centerWorld =
            remember(centerLat, centerLng, zoom) {
              LocationResolver.latLngToWorldPx(GeoPoint(centerLat, centerLng), zoom)
            }

          Box(
            modifier =
              Modifier.fillMaxSize()
                .pointerInput(Unit) {
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
                }
                .pointerInput(widthPx, heightPx) {
                  detectTapGestures { tapOffset ->
                    val currentWorld =
                      LocationResolver.latLngToWorldPx(GeoPoint(centerLat, centerLng), zoom)
                    val dx = tapOffset.x - widthPx / 2.0
                    val dy = tapOffset.y - heightPx / 2.0
                    val tappedGeo =
                      LocationResolver.worldPxToLatLng(
                        WorldPoint(currentWorld.x + dx, currentWorld.y + dy),
                        zoom,
                      )
                    centerLat = tappedGeo.latitude
                    centerLng = tappedGeo.longitude
                  }
                },
          ) {
            // Subtle cartographic grid backdrop
            PickerMapBackdrop(centerWorld = centerWorld, modifier = Modifier.fillMaxSize())

            // Live Carto Voyager Tiles
            val tileZ =
              floor(zoom)
                .toInt()
                .coerceIn(LocationResolver.MIN_ZOOM.toInt(), LocationResolver.MAX_ZOOM.toInt())
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
          }

          // Center Target Drop-Pin Overlay (pin tip aligns precisely at map center)
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.align(Alignment.Center),
          ) {
            Surface(
              color = Color.Black.copy(alpha = 0.25f),
              shape = CircleShape,
              modifier = Modifier.size(12.dp),
            ) {}
            Icon(
              imageVector = Icons.Filled.LocationOn,
              contentDescription = stringResource(R.string.map_picker_pin_cd),
              tint = BrokerRed,
              modifier = Modifier.size(44.dp).offset(y = (-20).dp),
            )
          }

          // Floating Zoom Controls
          Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shape = RoundedCornerShape(14.dp),
            shadowElevation = 4.dp,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              IconButton(
                onClick = { zoom = (zoom + 1.0).coerceAtMost(LocationResolver.MAX_ZOOM) },
                modifier = Modifier.size(48.dp).testTag("map_picker_zoom_in"),
              ) {
                Icon(Icons.Filled.Add, contentDescription = "Zoom in", tint = BrokerIndigo)
              }
              HorizontalDivider(modifier = Modifier.width(32.dp))
              IconButton(
                onClick = { zoom = (zoom - 1.0).coerceAtLeast(LocationResolver.MIN_ZOOM) },
                modifier = Modifier.size(48.dp).testTag("map_picker_zoom_out"),
              ) {
                Icon(Icons.Filled.Remove, contentDescription = "Zoom out", tint = BrokerIndigo)
              }
              HorizontalDivider(modifier = Modifier.width(32.dp))
              IconButton(
                onClick = {
                  centerLat = startingPoint.latitude
                  centerLng = startingPoint.longitude
                  zoom = 13.5
                },
                modifier = Modifier.size(48.dp).testTag("map_picker_reset"),
              ) {
                Icon(Icons.Filled.MyLocation, contentDescription = "Reset center", tint = BrokerBlue)
              }
            }
          }
        }

        // Bottom Selection Summary & Confirm Actions
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shadowElevation = 8.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Surface(
                color = BrokerRed.copy(alpha = 0.12f),
                shape = CircleShape,
                modifier = Modifier.size(40.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = BrokerRed,
                    modifier = Modifier.size(22.dp),
                  )
                }
              }
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text =
                    when {
                      isResolvingAddress -> stringResource(R.string.map_picker_resolving)
                      !resolvedAddress.isNullOrBlank() -> resolvedAddress!!
                      else -> stringResource(R.string.map_picker_pinned_location)
                    },
                  fontWeight = FontWeight.Bold,
                  fontSize = 15.sp,
                  color = BrokerIndigo,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text =
                    String.format(
                      Locale.US,
                      "Lat %.5f, Lng %.5f",
                      centerLat,
                      centerLng,
                    ),
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }

            Row(
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f).height(48.dp),
              ) {
                Text(stringResource(R.string.map_picker_cancel))
              }
              Button(
                onClick = {
                  onConfirm(GeoPoint(centerLat, centerLng), resolvedAddress)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrokerBlue),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1.4f).height(48.dp).testTag("map_picker_confirm_button"),
              ) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.map_picker_confirm), fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PickerMapBackdrop(
  centerWorld: WorldPoint,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier) {
    drawRect(color = Color(0xFFE9F0F5))
    val gridStep = 128f
    val offsetX = (-(centerWorld.x % gridStep)).toFloat()
    val offsetY = (-(centerWorld.y % gridStep)).toFloat()
    val lineColor = Color(0xFFD5E1EA)

    var x = offsetX
    while (x < size.width) {
      drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
      x += gridStep
    }
    var y = offsetY
    while (y < size.height) {
      drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
      y += gridStep
    }
  }
}
