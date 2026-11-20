package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.SquareFoot
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.ListingType
import com.example.data.model.PropertyCategory
import com.example.data.model.PropertyFormatters
import com.example.data.model.PropertyListing

@Composable
fun PropertyPhotoImage(
  photoKey: String?,
  contentDescription: String?,
  modifier: Modifier = Modifier,
  contentScale: ContentScale = ContentScale.Crop,
) {
  val presetRes =
    when (photoKey) {
      "preset:studio" -> R.drawable.img_studio_cozy
      "preset:apartment" -> R.drawable.img_apartment_modern
      "preset:villa" -> R.drawable.img_villa_luxury
      "preset:keys" -> R.drawable.img_home_hero_keys
      else -> null
    }

  when {
    presetRes != null -> {
      Image(
        painter = painterResource(id = presetRes),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
      )
    }
    !photoKey.isNullOrBlank() -> {
      val context = LocalContext.current
      val request =
        androidx.compose.runtime.remember(context, photoKey) {
          ImageRequest.Builder(context)
            .data(photoKey)
            .crossfade(160)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
        }
      AsyncImage(
        model = request,
        contentDescription = contentDescription,
        contentScale = contentScale,
        placeholder = painterResource(id = R.drawable.img_apartment_modern),
        error = painterResource(id = R.drawable.img_home_hero_keys),
        modifier = modifier,
      )
    }
    else -> {
      Image(
        painter = painterResource(id = R.drawable.img_home_hero_keys),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
      )
    }
  }
}

@Composable
fun SecurityBadgeChip(
  label: String,
  isUnlocked: Boolean = false,
  modifier: Modifier = Modifier,
) {
  val containerColor =
    if (isUnlocked) MaterialTheme.colorScheme.tertiaryContainer
    else MaterialTheme.colorScheme.secondaryContainer
  val contentColor =
    if (isUnlocked) MaterialTheme.colorScheme.onTertiaryContainer
    else MaterialTheme.colorScheme.onSecondaryContainer

  Surface(
    color = containerColor,
    shape = RoundedCornerShape(999.dp),
    modifier = modifier,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
      Icon(
        imageVector = if (isUnlocked) Icons.Filled.LockOpen else Icons.Filled.VerifiedUser,
        contentDescription = null,
        tint = contentColor,
        modifier = Modifier.size(14.dp),
      )
      Spacer(Modifier.width(5.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = contentColor,
        fontWeight = FontWeight.SemiBold,
      )
    }
  }
}

@Composable
fun PropertyListingCard(
  property: PropertyListing,
  isFavorite: Boolean,
  isContactUnlocked: Boolean,
  isOwner: Boolean,
  onClick: () -> Unit,
  onToggleFavorite: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val listingType = ListingType.fromRaw(property.listingType)
  val category = PropertyCategory.fromRaw(property.propertyCategory)

  Card(
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier =
      modifier
        .fillMaxWidth()
        .clip(MaterialTheme.shapes.large)
        .clickable(onClick = onClick)
        .testTag("property_card_${property.id}"),
  ) {
    Column {
      Box(modifier = Modifier.fillMaxWidth().height(190.dp)) {
        PropertyPhotoImage(
          photoKey = property.photoKeys.firstOrNull(),
          contentDescription = property.title,
          modifier = Modifier.fillMaxSize(),
        )
        Box(
          modifier =
            Modifier.fillMaxSize()
              .background(
                Brush.verticalGradient(
                  colors =
                    listOf(
                      Color.Black.copy(alpha = 0.12f),
                      Color.Transparent,
                      Color.Black.copy(alpha = 0.65f),
                    )
                )
              )
        )

        Row(
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth().padding(12.dp),
        ) {
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(
              color =
                if (listingType == ListingType.SALE) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.tertiary,
              shape = RoundedCornerShape(999.dp),
            ) {
              Text(
                text = listingType.label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              )
            }
            if (property.hasVideo) {
              Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(999.dp),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Videocam,
                    contentDescription = "Has walkthrough video",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                  )
                  Spacer(Modifier.width(4.dp))
                  Text("Video", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
              }
            }
          }

          Surface(
            color = Color.Black.copy(alpha = 0.45f),
            shape = CircleShape,
          ) {
            IconButton(
              onClick = onToggleFavorite,
              modifier = Modifier.size(48.dp).testTag("favorite_button_${property.id}"),
            ) {
              Icon(
                imageVector =
                  if (isFavorite) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = if (isFavorite) "Remove bookmark" else "Save property",
                tint = if (isFavorite) MaterialTheme.colorScheme.secondary else Color.White,
              )
            }
          }
        }

        Column(modifier = Modifier.align(Alignment.BottomStart).padding(14.dp)) {
          Text(
            text = PropertyFormatters.formatInrPrice(property.price, property.listingType),
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = category.label + if (isOwner) " • Your Listing" else "",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.85f),
          )
        }
      }

      Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(16.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = property.title,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )
          if (property.verifiedListing) {
            Spacer(Modifier.width(8.dp))
            Icon(
              imageVector = Icons.Filled.VerifiedUser,
              contentDescription = "Verified Owner",
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(20.dp),
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Outlined.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
          )
          Spacer(Modifier.width(4.dp))
          Text(
            text = property.location,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        Row(
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (property.bedrooms > 0) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Outlined.Bed,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                  "${property.bedrooms} BHK",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }
            }
            if (property.areaSqFt > 0) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Outlined.SquareFoot,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                  "${property.areaSqFt} sq.ft",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }
            }
          }

          Surface(
            color =
              if (isContactUnlocked || isOwner) MaterialTheme.colorScheme.tertiaryContainer
              else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(999.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            ) {
              Icon(
                imageVector =
                  if (isContactUnlocked || isOwner) Icons.Filled.LockOpen else Icons.Filled.Lock,
                contentDescription = null,
                tint =
                  if (isContactUnlocked || isOwner) MaterialTheme.colorScheme.onTertiaryContainer
                  else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(13.dp),
              )
              Spacer(Modifier.width(4.dp))
              Text(
                text =
                  when {
                    isOwner -> "Owner"
                    isContactUnlocked -> "Unlocked"
                    else -> "Contact Locked"
                  },
                style = MaterialTheme.typography.labelSmall,
                color =
                  if (isContactUnlocked || isOwner) MaterialTheme.colorScheme.onTertiaryContainer
                  else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
  }
}
