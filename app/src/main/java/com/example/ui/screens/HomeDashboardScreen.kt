package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.PremiumSchedule
import com.example.data.model.ListingType
import com.example.data.model.PropertyCategory
import com.example.ui.components.QuickPropertyFilterRow
import com.example.ui.theme.BrokerBlue
import com.example.ui.theme.BrokerIndigo
import com.example.ui.theme.BrokerPurple
import com.example.ui.theme.BrokerRed
import com.example.ui.theme.BrokerTeal
import com.example.ui.theme.TileBuyEnd
import com.example.ui.theme.TileBuyStart
import com.example.ui.theme.TileDepositEnd
import com.example.ui.theme.TileDepositStart
import com.example.ui.theme.TileRentEnd
import com.example.ui.theme.TileRentStart
import com.example.ui.theme.TileSaleEnd
import com.example.ui.theme.TileSaleStart
import com.example.ui.theme.WhatsAppGreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDashboardScreen(
  displayName: String,
  favoriteCount: Int = 0,
  activeAlertCount: Int = 0,
  unreadAlertCount: Int = 0,
  isPremiumActive: Boolean = false,
  showPremiumOption: Boolean = PremiumSchedule.isPremiumFeatureEnabled(),
  searchQuery: String = "",
  searchMatchCount: Int? = null,
  onSearchQueryChange: (String) -> Unit = {},
  onSearchSubmit: (String) -> Unit = {},
  onQuickFilterListingType: (ListingType) -> Unit = {},
  onQuickFilterCategory: (PropertyCategory) -> Unit = {},
  onOpenAiTool: () -> Unit = {},
  onOpenAlerts: () -> Unit = {},
  onOpenSaleList: () -> Unit,
  onOpenBuyList: () -> Unit,
  onOpenRentOptions: () -> Unit,
  onOpenHeavyDeposit: () -> Unit,
  onOpenMap: () -> Unit,
  onOpenFavorites: () -> Unit,
  onOpenChats: () -> Unit,
  onOpenHome: () -> Unit = {},
  onOpenProfile: () -> Unit = {},
  onOpenPremium: () -> Unit = {},
  onOpenTerms: () -> Unit = {},
  onSignOut: () -> Unit,
) {
  val scrollState = rememberScrollState()
  val scope = rememberCoroutineScope()

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = stringResource(R.string.app_name),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = BrokerIndigo,
            titleContentColor = Color.White,
            actionIconContentColor = Color.White,
          ),
        actions = {
          IconButton(
            onClick = onOpenAlerts,
            modifier = Modifier.testTag("home_notifications_button"),
          ) {
            BadgedBox(
              badge = {
                if (unreadAlertCount > 0) {
                  Badge(containerColor = BrokerRed, contentColor = Color.White) {
                    Text(unreadAlertCount.coerceAtMost(99).toString())
                  }
                } else if (activeAlertCount > 0) {
                  Badge(containerColor = TileBuyEnd)
                }
              }
            ) {
              Icon(
                imageVector =
                  if (unreadAlertCount > 0) Icons.Filled.NotificationsActive
                  else Icons.Filled.Notifications,
                contentDescription = stringResource(R.string.home_notifications_cd),
              )
            }
          }
          IconButton(
            onClick = onOpenAiTool,
            modifier = Modifier.testTag("home_ai_tool_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.AutoAwesome,
              contentDescription = "AI Property Assistant",
            )
          }
          IconButton(
            onClick = onOpenMap,
            modifier = Modifier.testTag("home_map_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.Map,
              contentDescription = "Property Map",
            )
          }
          IconButton(
            onClick = onOpenFavorites,
            modifier = Modifier.testTag("home_favorites_button"),
          ) {
            BadgedBox(
              badge = {
                if (favoriteCount > 0) {
                  Badge(containerColor = BrokerRed, contentColor = Color.White) {
                    Text(favoriteCount.coerceAtMost(99).toString())
                  }
                }
              }
            ) {
              Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = "Favorites",
              )
            }
          }
          AppOverflowMenu(
            isPremiumActive = isPremiumActive,
            showPremiumOption = showPremiumOption,
            onOpenHome = {
              scope.launch { scrollState.animateScrollTo(0) }
              onOpenHome()
            },
            onOpenProfile = onOpenProfile,
            onOpenPremium = onOpenPremium,
            onOpenTerms = onOpenTerms,
            onSignOut = onSignOut,
            menuButtonTestTag = "home_overflow_menu",
          )
        },
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = onOpenChats,
        shape = CircleShape,
        containerColor = WhatsAppGreen,
        contentColor = Color.White,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
        modifier = Modifier.size(60.dp).testTag("home_chats_button"),
      ) {
        Icon(
          painter = painterResource(id = R.drawable.ic_whatsapp_logo),
          contentDescription = "My Chats",
          tint = Color.White,
          modifier = Modifier.size(30.dp),
        )
      }
    },
  ) { innerPadding ->
    Box(
      contentAlignment = Alignment.TopCenter,
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .background(MaterialTheme.colorScheme.background),
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
          Modifier.widthIn(max = 540.dp)
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
      ) {
        // Slim Search Bar at top of Home Screen (Search by name, location, or price)
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = RoundedCornerShape(22.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
          shadowElevation = 2.dp,
          modifier = Modifier.fillMaxWidth().testTag("home_search_bar_card"),
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier =
                Modifier.fillMaxWidth()
                  .height(42.dp)
                  .padding(start = 12.dp, end = 4.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = BrokerIndigo,
                modifier = Modifier.size(19.dp),
              )
              Spacer(Modifier.width(8.dp))
              Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier.weight(1f),
              ) {
                if (searchQuery.isEmpty()) {
                  Text(
                    text = stringResource(R.string.home_search_placeholder),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                BasicTextField(
                  value = searchQuery,
                  onValueChange = onSearchQueryChange,
                  singleLine = true,
                  textStyle =
                    TextStyle(
                      fontSize = 13.5.sp,
                      color = MaterialTheme.colorScheme.onSurface,
                    ),
                  cursorBrush = SolidColor(BrokerIndigo),
                  keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                  keyboardActions = KeyboardActions(onSearch = { onSearchSubmit(searchQuery) }),
                  modifier = Modifier.fillMaxWidth().testTag("home_search_input"),
                )
              }
              if (searchQuery.isNotEmpty()) {
                Box(
                  contentAlignment = Alignment.Center,
                  modifier =
                    Modifier.size(34.dp)
                      .clip(CircleShape)
                      .clickable(
                        role = Role.Button,
                        onClickLabel = "Clear search",
                        onClick = { onSearchQueryChange("") },
                      ),
                ) {
                  Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Clear search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                  )
                }
              }
              Spacer(Modifier.width(4.dp))
              Surface(
                onClick = { onSearchSubmit(searchQuery) },
                color = BrokerIndigo,
                shape = CircleShape,
                modifier = Modifier.size(34.dp).testTag("home_search_submit_button"),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Search properties",
                    tint = Color.White,
                    modifier = Modifier.size(17.dp),
                  )
                }
              }
            }

            if (searchQuery.isNotBlank()) {
              Surface(
                onClick = { onSearchSubmit(searchQuery) },
                color = BrokerIndigo.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth().testTag("home_search_results_bar"),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                  modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                  val countSuffix =
                    if (searchMatchCount != null) " ($searchMatchCount found)" else ""
                  Text(
                    text = "Search \"${searchQuery.trim()}\"$countSuffix",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrokerIndigo,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                  )
                  Spacer(Modifier.width(8.dp))
                  Text(
                    text = "View →",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrokerBlue,
                  )
                }
              }
            }
          }
        }

        Spacer(Modifier.height(8.dp))

        // Horizontal quick filter chips below search bar (Apartment, House, Rent, Sale, etc.)
        QuickPropertyFilterRow(
          onToggleListingType = onQuickFilterListingType,
          onToggleCategory = onQuickFilterCategory,
          testTagPrefix = "home_quick_filter",
        )

        Spacer(Modifier.height(10.dp))

        // Home Screen Hero Photo Card (using uploaded img_home_hero_keys.jpg)
        Card(
          shape = RoundedCornerShape(16.dp),
          elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
          modifier = Modifier.fillMaxWidth().height(185.dp),
        ) {
          Box(modifier = Modifier.fillMaxSize()) {
            Image(
              painter = painterResource(id = R.drawable.img_home_hero_keys),
              contentDescription = "Property Broker Home Keys",
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
            )
            Box(
              modifier =
                Modifier.fillMaxSize()
                  .background(
                    Brush.verticalGradient(
                      colors =
                        listOf(
                          Color.Transparent,
                          BrokerIndigo.copy(alpha = 0.78f),
                        )
                    )
                  )
            )
            Column(
              modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Filled.HomeWork,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                  text = stringResource(R.string.app_name),
                  fontSize = 24.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.White,
                )
              }
              Text(
                text = "Welcome, $displayName",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.9f),
              )
            }
          }
        }

        Spacer(Modifier.height(18.dp))

        // 2x2 Vibrant Distinct-Color Square Action Tiles Grid
        Row(
          horizontalArrangement = Arrangement.spacedBy(14.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          HomeSquareActionButton(
            title = "1. SALE",
            subtitle = stringResource(R.string.tile_sale_subtitle),
            icon = Icons.Filled.Sell,
            gradientStart = TileSaleStart,
            gradientEnd = TileSaleEnd,
            onClick = onOpenSaleList,
            modifier = Modifier.weight(1f).testTag("home_sale_button"),
          )
          HomeSquareActionButton(
            title = "2. BUY",
            subtitle = stringResource(R.string.tile_buy_subtitle),
            icon = Icons.Filled.Home,
            gradientStart = TileBuyStart,
            gradientEnd = TileBuyEnd,
            onClick = onOpenBuyList,
            modifier = Modifier.weight(1f).testTag("home_buy_button"),
          )
        }

        Spacer(Modifier.height(14.dp))

        Row(
          horizontalArrangement = Arrangement.spacedBy(14.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          HomeSquareActionButton(
            title = "3. RENT",
            subtitle = stringResource(R.string.tile_rent_subtitle),
            icon = Icons.Filled.VpnKey,
            gradientStart = TileRentStart,
            gradientEnd = TileRentEnd,
            onClick = onOpenRentOptions,
            modifier = Modifier.weight(1f).testTag("home_rent_button"),
          )
          HomeSquareActionButton(
            title = "4. HEAVY DEPOSIT",
            subtitle = stringResource(R.string.tile_deposit_subtitle),
            icon = Icons.Filled.AccountBalanceWallet,
            gradientStart = TileDepositStart,
            gradientEnd = TileDepositEnd,
            onClick = onOpenHeavyDeposit,
            modifier = Modifier.weight(1f).testTag("home_heavy_deposit_button"),
          )
        }

        Spacer(Modifier.height(84.dp))
      }
    }
  }
}

@Composable
private fun HomeSquareActionButton(
  title: String,
  subtitle: String,
  icon: ImageVector,
  gradientStart: Color,
  gradientEnd: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    onClick = onClick,
    shape = RoundedCornerShape(18.dp),
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
    colors =
      CardDefaults.cardColors(
        containerColor = gradientStart,
        contentColor = Color.White,
      ),
    elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
    modifier = modifier.aspectRatio(1f),
  ) {
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(
            Brush.linearGradient(
              colors = listOf(gradientStart, gradientEnd),
            )
          )
          .drawBehind {
            drawCircle(
              color = Color.White.copy(alpha = 0.14f),
              radius = size.minDimension * 0.42f,
              center = Offset(size.width * 0.88f, size.height * 0.14f),
            )
            drawCircle(
              color = Color.White.copy(alpha = 0.08f),
              radius = size.minDimension * 0.34f,
              center = Offset(size.width * 0.10f, size.height * 0.92f),
            )
          },
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(14.dp),
      ) {
        Surface(
          color = Color.White.copy(alpha = 0.22f),
          shape = CircleShape,
          modifier = Modifier.size(54.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(28.dp),
            )
          }
        }
        Spacer(Modifier.height(12.dp))
        Text(
          text = title,
          fontSize = 16.sp,
          fontWeight = FontWeight.ExtraBold,
          color = Color.White,
          textAlign = TextAlign.Center,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(3.dp))
        Text(
          text = subtitle,
          fontSize = 11.5.sp,
          fontWeight = FontWeight.Medium,
          color = Color.White.copy(alpha = 0.90f),
          textAlign = TextAlign.Center,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentOptionsScreen(
  onBack: () -> Unit,
  onSelectOwner: () -> Unit,
  onSelectRenter: () -> Unit,
) {
  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = { Text("Rent Options", fontWeight = FontWeight.Bold) },
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
      contentAlignment = Alignment.Center,
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .background(MaterialTheme.colorScheme.background),
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth().padding(24.dp),
      ) {
        Text(
          text = "Rent Options",
          fontSize = 26.sp,
          fontWeight = FontWeight.Bold,
          color = BrokerIndigo,
        )

        Spacer(Modifier.height(40.dp))

        Button(
          onClick = onSelectOwner,
          shape = MaterialTheme.shapes.small,
          colors =
            ButtonDefaults.buttonColors(
              containerColor = BrokerPurple,
              contentColor = Color.White,
            ),
          modifier = Modifier.fillMaxWidth().height(60.dp).testTag("rent_owner_option"),
        ) {
          Text(
            text = "1. Property Owner",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
          )
        }

        Spacer(Modifier.height(16.dp))

        Button(
          onClick = onSelectRenter,
          shape = MaterialTheme.shapes.small,
          colors =
            ButtonDefaults.buttonColors(
              containerColor = BrokerTeal,
              contentColor = Color.White,
            ),
          modifier = Modifier.fillMaxWidth().height(60.dp).testTag("rent_renter_option"),
        ) {
          Text(
            text = "2. Renter",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeavyDepositOptionsScreen(
  onBack: () -> Unit,
  onSelectOwner: () -> Unit,
  onSelectSeeker: () -> Unit,
) {
  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = { Text("Heavy Deposit Options", fontWeight = FontWeight.Bold) },
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
      contentAlignment = Alignment.Center,
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .background(MaterialTheme.colorScheme.background),
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth().padding(24.dp),
      ) {
        Text(
          text = "Heavy Deposit Options",
          fontSize = 26.sp,
          fontWeight = FontWeight.Bold,
          color = BrokerIndigo,
          textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(40.dp))

        Button(
          onClick = onSelectOwner,
          shape = MaterialTheme.shapes.small,
          colors =
            ButtonDefaults.buttonColors(
              containerColor = BrokerPurple,
              contentColor = Color.White,
            ),
          modifier =
            Modifier.fillMaxWidth().height(68.dp).testTag("heavy_deposit_owner_option"),
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "1. Property Owner",
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "List property on Heavy Deposit",
              fontSize = 12.sp,
              color = Color.White.copy(alpha = 0.88f),
            )
          }
        }

        Spacer(Modifier.height(16.dp))

        Button(
          onClick = onSelectSeeker,
          shape = MaterialTheme.shapes.small,
          colors =
            ButtonDefaults.buttonColors(
              containerColor = BrokerTeal,
              contentColor = Color.White,
            ),
          modifier =
            Modifier.fillMaxWidth().height(68.dp).testTag("heavy_deposit_seeker_option"),
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "2. Seeker",
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "Find property on Heavy Deposit",
              fontSize = 12.sp,
              color = Color.White.copy(alpha = 0.88f),
            )
          }
        }
      }
    }
  }
}
