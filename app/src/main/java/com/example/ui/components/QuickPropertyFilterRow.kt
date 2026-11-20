package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Villa
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ListingType
import com.example.data.model.PropertyCategory
import com.example.ui.theme.BrokerIndigo
import com.example.ui.theme.TileDepositStart
import com.example.ui.theme.TileRentStart
import com.example.ui.theme.TileSaleStart

private sealed interface QuickFilterChipSpec {
  val key: String
  val label: String
  val icon: ImageVector
  val accent: Color

  data class CategoryChip(
    val category: PropertyCategory,
    override val label: String,
    override val icon: ImageVector,
    override val accent: Color = BrokerIndigo,
  ) : QuickFilterChipSpec {
    override val key: String = "cat_${category.name.lowercase()}"
  }

  data class ListingTypeChip(
    val listingType: ListingType,
    override val label: String,
    override val icon: ImageVector,
    override val accent: Color,
  ) : QuickFilterChipSpec {
    override val key: String = "type_${listingType.name.lowercase()}"
  }
}

private val QuickFilterChips: List<QuickFilterChipSpec> =
  listOf(
    QuickFilterChipSpec.CategoryChip(
      category = PropertyCategory.APARTMENT,
      label = "Apartment",
      icon = Icons.Filled.Apartment,
    ),
    QuickFilterChipSpec.CategoryChip(
      category = PropertyCategory.HOUSE,
      label = "House",
      icon = Icons.Filled.Home,
    ),
    QuickFilterChipSpec.ListingTypeChip(
      listingType = ListingType.RENT,
      label = "Rent",
      icon = Icons.Filled.VpnKey,
      accent = TileRentStart,
    ),
    QuickFilterChipSpec.ListingTypeChip(
      listingType = ListingType.SALE,
      label = "Sale",
      icon = Icons.Filled.Sell,
      accent = TileSaleStart,
    ),
    QuickFilterChipSpec.ListingTypeChip(
      listingType = ListingType.HEAVY_DEPOSIT,
      label = "Heavy Deposit",
      icon = Icons.Filled.AccountBalanceWallet,
      accent = TileDepositStart,
    ),
    QuickFilterChipSpec.CategoryChip(
      category = PropertyCategory.VILLA,
      label = "Villa",
      icon = Icons.Filled.Villa,
    ),
    QuickFilterChipSpec.CategoryChip(
      category = PropertyCategory.SHOP,
      label = "Shop",
      icon = Icons.Filled.Storefront,
    ),
    QuickFilterChipSpec.CategoryChip(
      category = PropertyCategory.GALA,
      label = "Gala",
      icon = Icons.Filled.Warehouse,
    ),
    QuickFilterChipSpec.CategoryChip(
      category = PropertyCategory.OFFICE,
      label = "Office",
      icon = Icons.Filled.Business,
    ),
    QuickFilterChipSpec.CategoryChip(
      category = PropertyCategory.PLOT,
      label = "Plot",
      icon = Icons.Filled.Landscape,
    ),
  )

@Composable
fun QuickPropertyFilterRow(
  selectedListingType: ListingType? = null,
  selectedCategory: PropertyCategory? = null,
  showAllChip: Boolean = false,
  onClearAll: () -> Unit = {},
  onToggleListingType: (ListingType) -> Unit,
  onToggleCategory: (PropertyCategory) -> Unit,
  contentPadding: PaddingValues = PaddingValues(horizontal = 0.dp),
  testTagPrefix: String = "quick_filter",
  modifier: Modifier = Modifier,
) {
  LazyRow(
    contentPadding = contentPadding,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    modifier = modifier.fillMaxWidth().testTag("${testTagPrefix}_row"),
  ) {
    if (showAllChip) {
      item(key = "all") {
        val allSelected = selectedListingType == null && selectedCategory == null
        FilterChip(
          selected = allSelected,
          onClick = onClearAll,
          label = {
            Text(
              text = "All",
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold,
            )
          },
          leadingIcon = {
            Icon(
              imageVector = if (allSelected) Icons.Filled.Check else Icons.Filled.GridView,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
          },
          colors =
            FilterChipDefaults.filterChipColors(
              selectedContainerColor = BrokerIndigo,
              selectedLabelColor = Color.White,
              selectedLeadingIconColor = Color.White,
              containerColor = MaterialTheme.colorScheme.surface,
              labelColor = MaterialTheme.colorScheme.onSurface,
              iconColor = BrokerIndigo,
            ),
          modifier = Modifier.testTag("${testTagPrefix}_all"),
        )
      }
    }

    items(QuickFilterChips, key = { it.key }) { spec ->
      val isSelected =
        when (spec) {
          is QuickFilterChipSpec.CategoryChip -> selectedCategory == spec.category
          is QuickFilterChipSpec.ListingTypeChip -> selectedListingType == spec.listingType
        }

      FilterChip(
        selected = isSelected,
        onClick = {
          when (spec) {
            is QuickFilterChipSpec.CategoryChip -> onToggleCategory(spec.category)
            is QuickFilterChipSpec.ListingTypeChip -> onToggleListingType(spec.listingType)
          }
        },
        label = {
          Text(
            text = spec.label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
          )
        },
        leadingIcon = {
          Icon(
            imageVector = if (isSelected) Icons.Filled.Check else spec.icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
        },
        colors =
          FilterChipDefaults.filterChipColors(
            selectedContainerColor = spec.accent,
            selectedLabelColor = Color.White,
            selectedLeadingIconColor = Color.White,
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurface,
            iconColor = spec.accent,
          ),
        modifier = Modifier.testTag("${testTagPrefix}_${spec.key}"),
      )
    }
  }
}
