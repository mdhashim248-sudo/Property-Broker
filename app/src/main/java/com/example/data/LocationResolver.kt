package com.example.data

import android.content.Context
import android.location.Geocoder
import android.os.Build
import com.example.data.model.PropertyListing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.sinh
import kotlin.math.tan

data class GeoPoint(
  val latitude: Double,
  val longitude: Double,
)

data class WorldPoint(
  val x: Double,
  val y: Double,
)

data class MapViewport(
  val center: GeoPoint,
  val zoom: Double,
)

object LocationResolver {
  const val TILE_SIZE = 256.0
  const val MIN_ZOOM = 4.0
  const val MAX_ZOOM = 17.0
  val DEFAULT_CENTER = GeoPoint(19.0760, 72.8777) // Mumbai Metro
  const val DEFAULT_ZOOM = 11.2

  // Ordered from more specific localities to broader cities so substring matching prefers localities.
  private val knownLocations: List<Pair<String, GeoPoint>> =
    listOf(
      "worli" to GeoPoint(19.0176, 72.8156),
      "bandra west" to GeoPoint(19.0596, 72.8295),
      "bandra east" to GeoPoint(19.0607, 72.8465),
      "bandra" to GeoPoint(19.0596, 72.8295),
      "andheri west" to GeoPoint(19.1364, 72.8296),
      "andheri east" to GeoPoint(19.1136, 72.8697),
      "andheri" to GeoPoint(19.1197, 72.8464),
      "powai" to GeoPoint(19.1176, 72.9060),
      "malad" to GeoPoint(19.1874, 72.8484),
      "borivali" to GeoPoint(19.2307, 72.8567),
      "dadar" to GeoPoint(19.0178, 72.8478),
      "juhu" to GeoPoint(19.1075, 72.8263),
      "colaba" to GeoPoint(18.9067, 72.8147),
      "goregaon" to GeoPoint(19.1663, 72.8526),
      "kandivali" to GeoPoint(19.2047, 72.8526),
      "chembur" to GeoPoint(19.0522, 72.9005),
      "kurla" to GeoPoint(19.0726, 72.8845),
      "ghatkopar" to GeoPoint(19.0860, 72.9090),
      "mulund" to GeoPoint(19.1726, 72.9425),
      "lower parel" to GeoPoint(18.9977, 72.8300),
      "parel" to GeoPoint(19.0005, 72.8415),
      "sion" to GeoPoint(19.0434, 72.8633),
      "santacruz" to GeoPoint(19.0843, 72.8360),
      "vile parle" to GeoPoint(19.0990, 72.8440),
      "mira road" to GeoPoint(19.2813, 72.8744),
      "bhayandar" to GeoPoint(19.3012, 72.8511),
      "vasai" to GeoPoint(19.3919, 72.8397),
      "virar" to GeoPoint(19.4564, 72.7925),
      "thane" to GeoPoint(19.2183, 72.9781),
      "bhiwandi" to GeoPoint(19.2813, 73.0483),
      "kalyan" to GeoPoint(19.2403, 73.1305),
      "dombivli" to GeoPoint(19.2183, 73.0867),
      "navi mumbai" to GeoPoint(19.0330, 73.0297),
      "vashi" to GeoPoint(19.0771, 72.9986),
      "nerul" to GeoPoint(19.0338, 73.0196),
      "kharghar" to GeoPoint(19.0473, 73.0699),
      "panvel" to GeoPoint(18.9894, 73.1175),
      "airoli" to GeoPoint(19.1590, 72.9986),
      "mumbai" to GeoPoint(19.0760, 72.8777),
      "hinjewadi" to GeoPoint(18.5913, 73.7389),
      "baner" to GeoPoint(18.5590, 73.7868),
      "kharadi" to GeoPoint(18.5515, 73.9348),
      "wakad" to GeoPoint(18.5993, 73.7625),
      "koregaon park" to GeoPoint(18.5362, 73.8940),
      "pune" to GeoPoint(18.5204, 73.8567),
      "nashik" to GeoPoint(19.9975, 73.7898),
      "nagpur" to GeoPoint(21.1458, 79.0882),
      "surat" to GeoPoint(21.1702, 72.8311),
      "ahmedabad" to GeoPoint(23.0225, 72.5714),
      "vadodara" to GeoPoint(22.3072, 73.1812),
      "assagao" to GeoPoint(15.5937, 73.7640),
      "panaji" to GeoPoint(15.4909, 73.8278),
      "goa" to GeoPoint(15.2993, 74.1240),
      "whitefield" to GeoPoint(12.9698, 77.7500),
      "indiranagar" to GeoPoint(12.9784, 77.6408),
      "koramangala" to GeoPoint(12.9352, 77.6245),
      "bengaluru" to GeoPoint(12.9716, 77.5946),
      "bangalore" to GeoPoint(12.9716, 77.5946),
      "hyderabad" to GeoPoint(17.3850, 78.4867),
      "chennai" to GeoPoint(13.0827, 80.2707),
      "kolkata" to GeoPoint(22.5726, 88.3639),
      "noida" to GeoPoint(28.5355, 77.3910),
      "gurgaon" to GeoPoint(28.4595, 77.0266),
      "gurugram" to GeoPoint(28.4595, 77.0266),
      "new delhi" to GeoPoint(28.6139, 77.2090),
      "delhi" to GeoPoint(28.6139, 77.2090),
      "jaipur" to GeoPoint(26.9124, 75.7873),
      "indore" to GeoPoint(22.7196, 75.8577),
      "lucknow" to GeoPoint(26.8467, 80.9462),
      "chandigarh" to GeoPoint(30.7333, 76.7794),
      "kochi" to GeoPoint(9.9312, 76.2673),
    )

  fun resolveCoordinates(property: PropertyListing): GeoPoint {
    val lat = property.latitude
    val lng = property.longitude
    if (lat != null && lng != null && lat.isFinite() && lng.isFinite() &&
      lat in -85.0..85.0 && lng in -180.0..180.0
    ) {
      return GeoPoint(lat, lng)
    }
    return resolveCoordinatesForText(property.location, seedKey = property.id.ifBlank { property.title })
  }

  fun resolveCoordinatesForText(locationText: String, seedKey: String = ""): GeoPoint {
    val normalized = locationText.trim().lowercase(Locale.ROOT)
    val base =
      knownLocations.firstOrNull { (keyword, _) -> normalized.contains(keyword) }?.second
        ?: DEFAULT_CENTER

    if (seedKey.isBlank()) return base

    // Add a small deterministic micro-offset (~150m to 550m) so pins in the same locality stay distinguishable.
    val hash = seedKey.hashCode()
    val latSteps = ((hash and 0xFF) - 128) / 128.0
    val lngSteps = (((hash ushr 8) and 0xFF) - 128) / 128.0
    val latOffset = latSteps * 0.0045
    val lngOffset = lngSteps * 0.0045
    return GeoPoint(
      latitude = (base.latitude + latOffset).coerceIn(-85.0, 85.0),
      longitude = (base.longitude + lngOffset).coerceIn(-180.0, 180.0),
    )
  }

  suspend fun geocodeLocation(context: Context, locationText: String): GeoPoint {
    val clean = locationText.trim()
    if (clean.isEmpty()) return DEFAULT_CENTER

    val geocoded =
      withTimeoutOrNull(2200L) {
        withContext(Dispatchers.IO) {
          runCatching {
            if (!Geocoder.isPresent()) return@runCatching null
            val geocoder = Geocoder(context.applicationContext, Locale("en", "IN"))
            val query = if (clean.lowercase(Locale.ROOT).contains("india")) clean else "$clean, India"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              suspendCancellableCoroutine<GeoPoint?> { cont ->
                geocoder.getFromLocationName(query, 1) { addresses ->
                  val first = addresses.firstOrNull()
                  if (cont.isActive) {
                    cont.resume(first?.let { GeoPoint(it.latitude, it.longitude) })
                  }
                }
              }
            } else {
              @Suppress("DEPRECATION")
              val first = geocoder.getFromLocationName(query, 1)?.firstOrNull()
              first?.let { GeoPoint(it.latitude, it.longitude) }
            }
          }.getOrNull()
        }
      }

    return geocoded ?: resolveCoordinatesForText(clean, seedKey = clean)
  }

  suspend fun reverseGeocodeLocation(context: Context, point: GeoPoint): String? {
    val reverseGeocoded =
      withTimeoutOrNull(2500L) {
        withContext(Dispatchers.IO) {
          runCatching {
            if (!Geocoder.isPresent()) return@runCatching null
            val geocoder = Geocoder(context.applicationContext, Locale.Builder().setLanguage("en").setRegion("IN").build())
            val address =
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine<android.location.Address?> { cont ->
                  geocoder.getFromLocation(point.latitude, point.longitude, 1) { addresses ->
                    if (cont.isActive) {
                      cont.resume(addresses.firstOrNull())
                    }
                  }
                }
              } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(point.latitude, point.longitude, 1)?.firstOrNull()
              }
            if (address == null) return@runCatching null
            val parts =
              listOfNotNull(
                address.subLocality?.trim()?.takeIf { it.isNotEmpty() },
                address.locality?.trim()?.takeIf { it.isNotEmpty() },
                address.subAdminArea?.trim()?.takeIf { it.isNotEmpty() },
                address.adminArea?.trim()?.takeIf { it.isNotEmpty() },
              ).distinct()
            when {
              parts.size >= 2 -> "${parts[0]}, ${parts[1]}"
              parts.size == 1 -> parts[0]
              else -> address.getAddressLine(0)?.trim()?.take(80)
            }
          }.getOrNull()
        }
      }

    if (!reverseGeocoded.isNullOrBlank()) return reverseGeocoded

    // Fallback to nearest known locality within ~25 km (~0.25 degrees)
    val nearest =
      knownLocations.minByOrNull { (_, knownPt) ->
        val dLat = point.latitude - knownPt.latitude
        val dLng = point.longitude - knownPt.longitude
        dLat * dLat + dLng * dLng
      }
    if (nearest != null) {
      val dLat = point.latitude - nearest.second.latitude
      val dLng = point.longitude - nearest.second.longitude
      if (dLat * dLat + dLng * dLng <= 0.0625) {
        return nearest.first.split(" ").joinToString(" ") { word ->
          word.replaceFirstChar { ch -> ch.titlecase(Locale.ROOT) }
        }
      }
    }
    return null
  }

  fun latLngToWorldPx(point: GeoPoint, zoom: Double): WorldPoint {
    val clampedLat = point.latitude.coerceIn(-85.05112878, 85.05112878)
    val scale = TILE_SIZE * 2.0.pow(zoom)
    val x = (point.longitude + 180.0) / 360.0 * scale
    val latRad = Math.toRadians(clampedLat)
    val y = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * scale
    return WorldPoint(x, y)
  }

  fun worldPxToLatLng(worldPoint: WorldPoint, zoom: Double): GeoPoint {
    val scale = TILE_SIZE * 2.0.pow(zoom)
    val lng = ((worldPoint.x / scale) * 360.0 - 180.0).coerceIn(-180.0, 180.0)
    val n = PI - 2.0 * PI * (worldPoint.y / scale)
    val lat = Math.toDegrees(atan(sinh(n))).coerceIn(-85.05112878, 85.05112878)
    return GeoPoint(lat, lng)
  }

  fun computeFitViewport(
    points: List<GeoPoint>,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
  ): MapViewport {
    if (points.isEmpty()) return MapViewport(DEFAULT_CENTER, DEFAULT_ZOOM)
    if (points.size == 1) return MapViewport(points.first(), 13.5)

    val minLat = points.minOf { it.latitude }
    val maxLat = points.maxOf { it.latitude }
    val minLng = points.minOf { it.longitude }
    val maxLng = points.maxOf { it.longitude }

    val center = GeoPoint((minLat + maxLat) / 2.0, (minLng + maxLng) / 2.0)

    val usableW = (viewportWidthPx * 0.72f).coerceAtLeast(180f).toDouble()
    val usableH = (viewportHeightPx * 0.56f).coerceAtLeast(180f).toDouble()

    val topLeft0 = latLngToWorldPx(GeoPoint(maxLat, minLng), 0.0)
    val bottomRight0 = latLngToWorldPx(GeoPoint(minLat, maxLng), 0.0)
    val spanX = abs(bottomRight0.x - topLeft0.x)
    val spanY = abs(bottomRight0.y - topLeft0.y)

    if (spanX < 1e-6 && spanY < 1e-6) {
      return MapViewport(center, 13.5)
    }

    val zoomX = if (spanX > 1e-6) log2(usableW / spanX) else MAX_ZOOM
    val zoomY = if (spanY > 1e-6) log2(usableH / spanY) else MAX_ZOOM
    val fitZoom = minOf(zoomX, zoomY).coerceIn(MIN_ZOOM, 14.5)
    return MapViewport(center, fitZoom)
  }
}
