package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.ChatRepository
import com.example.data.LocalNotificationStore
import com.example.data.NotificationDeepLink
import com.example.data.PremiumSchedule
import com.example.data.PropertyAlertMatcher
import com.example.data.PropertyLocalNotifier
import com.example.data.PropertyRepository
import com.example.data.SavedPropertyAlert
import com.example.data.TermsAcceptanceStore
import com.example.data.UiState
import com.example.data.authStateFlow
import com.example.data.configureAppCheck
import com.example.data.deleteAuthenticatedFirebaseAccount
import com.example.data.model.ListingType
import com.example.data.model.PropertySearchMatcher
import com.example.data.model.UserRole
import com.example.data.signOutUser
import com.example.ui.screens.AddPropertyScreen
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatThreadScreen
import com.example.ui.screens.ChatsListScreen
import com.example.ui.screens.ExplorePropertiesScreen
import com.example.ui.screens.HeavyDepositOptionsScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.PremiumScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.PropertyDetailScreen
import com.example.ui.screens.RentOptionsScreen
import com.example.ui.screens.TermsAndConditionsScreen
import com.example.ui.theme.PropertyBrokerTheme
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.MarketplaceViewModel
import com.example.ui.viewmodel.PropertyAiViewModel
import com.example.ui.viewmodel.PropertyDetailViewModel
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

object Routes {
  const val HOME = "home"
  const val RENT_OPTIONS = "rent_options"
  const val HEAVY_DEPOSIT_OPTIONS = "heavy_deposit_options"
  const val PROPERTY_LIST = "property_list/{mode}"
  const val ADD_PROPERTY = "add_property/{listingType}"
  const val PROPERTY_DETAIL = "property_detail/{propertyId}"
  const val CHATS = "chats"
  const val CHAT_THREAD = "chat_thread/{chatId}"
  const val AI_ASSISTANT = "ai_assistant"
  const val NOTIFICATIONS = "notifications"
  const val PROFILE = "profile"
  const val PREMIUM = "premium"
  const val TERMS = "terms"

  fun propertyList(mode: String) = "property_list/$mode"
  fun addProperty(type: ListingType) = "add_property/${type.name}"
  fun propertyDetail(propertyId: String) = "property_detail/$propertyId"
  fun chatThread(chatId: String) = "chat_thread/$chatId"
}

private const val NAV_ANIM_DURATION_MS = 200

class MainActivity : ComponentActivity() {
  private val deepLinkFlow = MutableStateFlow<NotificationDeepLink?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // STEP 1: Configure Firebase App Check with launch intent before any UI, ViewModels, or Firebase operations
    configureAppCheck(this, intent)
    deepLinkFlow.value = PropertyLocalNotifier.extractDeepLink(intent)
    enableEdgeToEdge()
    setContent {
      PropertyBrokerTheme {
        val pendingDeepLink by deepLinkFlow.collectAsStateWithLifecycle()
        AppNavigation(
          pendingDeepLink = pendingDeepLink,
          onConsumeDeepLink = { deepLinkFlow.value = null },
        )
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    PropertyLocalNotifier.extractDeepLink(intent)?.let { link ->
      deepLinkFlow.value = link
    }
  }
}

@Composable
fun AppNavigation(
  pendingDeepLink: NotificationDeepLink? = null,
  onConsumeDeepLink: () -> Unit = {},
) {
  val context = LocalContext.current
  val auth =
    remember(context) {
      runCatching {
        if (FirebaseApp.getApps(context.applicationContext).isEmpty()) {
          FirebaseApp.initializeApp(context.applicationContext)
        }
        Firebase.auth
      }.getOrNull()
    }
  val authState = remember(auth) { auth?.authStateFlow() ?: flowOf(null) }
  val currentUser by authState.collectAsStateWithLifecycle(initialValue = auth?.currentUser)
  val user = currentUser

  var pendingSignUpName by remember { mutableStateOf<String?>(null) }
  var pendingSignUpPhone by remember { mutableStateOf<String?>(null) }
  val notificationStore = remember(context) { LocalNotificationStore(context) }
  val termsAcceptanceStore = remember(context) { TermsAcceptanceStore(context) }
  val gateScope = rememberCoroutineScope()
  val gateCredentialManager = remember(context) { CredentialManager.create(context) }

  if (user == null) {
    AuthScreen(
      onAuthSuccess = { name, phone ->
        if (!name.isNullOrBlank()) pendingSignUpName = name
        if (!phone.isNullOrBlank()) pendingSignUpPhone = phone
      },
    )
  } else {
    key(user.uid) {
      var hasAcceptedTerms by remember(user.uid) {
        mutableStateOf(termsAcceptanceStore.hasAccepted(user.uid))
      }

      if (!hasAcceptedTerms) {
        TermsAndConditionsScreen(
          isPremiumActive = false,
          requireAcceptance = true,
          onAcceptTerms = {
            termsAcceptanceStore.markAccepted(user.uid)
            hasAcceptedTerms = true
          },
          onBack = {},
          onOpenHome = {},
          onOpenProfile = {},
          onOpenPremium = {},
          onSignOut = {
            notificationStore.resetWatchBaselines()
            pendingSignUpName = null
            pendingSignUpPhone = null
            signOutUser(gateCredentialManager, gateScope)
          },
        )
      } else {
        AuthenticatedShell(
          user = user,
          preferredName = pendingSignUpName,
          preferredPhone = pendingSignUpPhone.orEmpty(),
          notificationStore = notificationStore,
          termsAcceptanceStore = termsAcceptanceStore,
          pendingDeepLink = pendingDeepLink,
          onConsumeDeepLink = onConsumeDeepLink,
          onSignOutCleanup = {
            notificationStore.resetWatchBaselines()
            pendingSignUpName = null
            pendingSignUpPhone = null
          },
        )
      }
    }
  }
}

@Composable
private fun AuthenticatedShell(
  user: FirebaseUser,
  preferredName: String?,
  preferredPhone: String,
  notificationStore: LocalNotificationStore,
  termsAcceptanceStore: TermsAcceptanceStore,
  pendingDeepLink: NotificationDeepLink?,
  onConsumeDeepLink: () -> Unit,
  onSignOutCleanup: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val marketplaceViewModel: MarketplaceViewModel =
    viewModel(
      key = user.uid,
      factory =
        viewModelFactory {
          initializer {
            val app = checkNotNull(this[APPLICATION_KEY])
            val databaseId = app.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(databaseId)
            MarketplaceViewModel(
              propertyRepository = PropertyRepository(db),
              chatRepository = ChatRepository(db),
              currentUserId = user.uid,
            )
          }
        },
    )

  LaunchedEffect(user.uid, preferredName) {
    marketplaceViewModel.ensureProfile(user, preferredDisplayName = preferredName)
  }

  val userProfile by marketplaceViewModel.userProfile.collectAsStateWithLifecycle()
  val activePropertiesState by
    marketplaceViewModel.activePropertiesState.collectAsStateWithLifecycle()
  val myPropertiesState by
    marketplaceViewModel.myPropertiesState.collectAsStateWithLifecycle()
  val filteredPropertiesState by
    marketplaceViewModel.filteredPropertiesState.collectAsStateWithLifecycle()
  val favoriteIds by marketplaceViewModel.favoriteIds.collectAsStateWithLifecycle()
  val unlockedIds by marketplaceViewModel.unlockedPropertyIds.collectAsStateWithLifecycle()
  val membership by marketplaceViewModel.membership.collectAsStateWithLifecycle()
  val myChatsState by marketplaceViewModel.myChatsState.collectAsStateWithLifecycle()
  val filter by marketplaceViewModel.filter.collectAsStateWithLifecycle()
  val actionBanner by marketplaceViewModel.actionBanner.collectAsStateWithLifecycle()
  val isSubmitting by marketplaceViewModel.isSubmitting.collectAsStateWithLifecycle()

  val savedAlerts by notificationStore.savedAlerts.collectAsStateWithLifecycle()
  val notificationSettings by notificationStore.settings.collectAsStateWithLifecycle()
  val recentAlerts by notificationStore.recentAlerts.collectAsStateWithLifecycle()

  var isDeletingAccount by remember { mutableStateOf(false) }

  val isPremiumActive =
    remember(membership) {
      membership?.premiumActive == true && membership!!.expiresAtMillis > System.currentTimeMillis()
    }
  val showPremiumOption = remember { PremiumSchedule.isPremiumFeatureEnabled() }
  val requiresPremiumToAdd =
    remember(isPremiumActive) { !PremiumSchedule.canAddProperty(isPremiumActive) }
  val activeAlertCount by remember { derivedStateOf { savedAlerts.count { it.enabled } } }
  val unreadAlertCount by remember { derivedStateOf { recentAlerts.count { !it.isRead } } }
  val myListingsCount by remember {
    derivedStateOf { (myPropertiesState as? UiState.Success)?.data?.size ?: 0 }
  }

  val aiViewModel: PropertyAiViewModel = viewModel(key = "${user.uid}_ai_vm")
  var homeSearchQuery by remember { mutableStateOf("") }

  val activePropertiesList by remember {
    derivedStateOf { (activePropertiesState as? UiState.Success)?.data.orEmpty() }
  }

  val homeSearchMatchCount by remember {
    derivedStateOf {
      val data = (activePropertiesState as? UiState.Success)?.data ?: return@derivedStateOf null
      if (homeSearchQuery.isBlank()) null
      else data.count { item -> PropertySearchMatcher.matches(item, homeSearchQuery) }
    }
  }

  val navController = rememberNavController()
  val backStackEntry by navController.currentBackStackEntryAsState()
  val activeChatId =
    if (backStackEntry?.destination?.route == Routes.CHAT_THREAD) {
      backStackEntry?.arguments?.getString("chatId")
    } else null
  val snackbarHostState = remember { SnackbarHostState() }

  fun navigateSingleTop(route: String) {
    navController.navigate(route) {
      launchSingleTop = true
    }
  }

  LaunchedEffect(pendingDeepLink) {
    pendingDeepLink?.let { link ->
      when (link.type) {
        PropertyLocalNotifier.DEEP_LINK_PROPERTY ->
          navigateSingleTop(Routes.propertyDetail(link.targetId))
        PropertyLocalNotifier.DEEP_LINK_CHAT ->
          navigateSingleTop(Routes.chatThread(link.targetId))
      }
      onConsumeDeepLink()
    }
  }

  LaunchedEffect(activePropertiesState) {
    val loaded = (activePropertiesState as? UiState.Success)?.data ?: return@LaunchedEffect
    val triggered = notificationStore.processPropertiesSnapshot(loaded)
    triggered.firstOrNull()?.let { alert ->
      snackbarHostState.showSnackbar("Alert: ${alert.title}")
    }
  }

  LaunchedEffect(myChatsState) {
    val loadedChats = (myChatsState as? UiState.Success)?.data ?: return@LaunchedEffect
    val triggered = notificationStore.processChatsSnapshot(loadedChats, user.uid, activeChatId)
    triggered.firstOrNull()?.let { alert ->
      snackbarHostState.showSnackbar(alert.title)
    }
  }

  LaunchedEffect(actionBanner) {
    actionBanner?.let {
      snackbarHostState.showSnackbar(it)
      marketplaceViewModel.clearBanner()
    }
  }

  val displayName =
    preferredName?.ifBlank { null }
      ?: userProfile?.displayName?.ifBlank { null }
      ?: user.displayName?.ifBlank { null }
      ?: user.email?.substringBefore("@")
      ?: "Member"
  val userEmail = user.email ?: ""

  val performSignOut: () -> Unit = {
    onSignOutCleanup()
    signOutUser(credentialManager, scope)
  }

  val performDeleteAccount: () -> Unit = {
    if (!isDeletingAccount) {
      scope.launch {
        isDeletingAccount = true
        val dataResult = marketplaceViewModel.deleteUserAccountData()
        if (dataResult.isFailure) {
          isDeletingAccount = false
          marketplaceViewModel.showMessage(
            dataResult.exceptionOrNull()?.localizedMessage
              ?: "Unable to delete account data. Please try again."
          )
          return@launch
        }
        notificationStore.clearHistory()
        notificationStore.resetWatchBaselines()
        termsAcceptanceStore.clearAcceptance(user.uid)
        val authResult = deleteAuthenticatedFirebaseAccount(credentialManager)
        isDeletingAccount = false
        if (authResult.isSuccess) {
          onSignOutCleanup()
        } else {
          marketplaceViewModel.showMessage(
            authResult.exceptionOrNull()?.localizedMessage
              ?: "Please sign out and sign in again to complete account deletion."
          )
        }
      }
    }
  }

  Scaffold(
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    snackbarHost = { SnackbarHost(snackbarHostState) },
  ) { innerPadding ->
    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
      NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        enterTransition = {
          fadeIn(tween(NAV_ANIM_DURATION_MS, easing = FastOutSlowInEasing)) +
            slideInHorizontally(tween(NAV_ANIM_DURATION_MS, easing = FastOutSlowInEasing)) {
              it / 10
            }
        },
        exitTransition = {
          fadeOut(tween(NAV_ANIM_DURATION_MS, easing = FastOutSlowInEasing))
        },
        popEnterTransition = {
          fadeIn(tween(NAV_ANIM_DURATION_MS, easing = FastOutSlowInEasing))
        },
        popExitTransition = {
          fadeOut(tween(NAV_ANIM_DURATION_MS, easing = FastOutSlowInEasing)) +
            slideOutHorizontally(tween(NAV_ANIM_DURATION_MS, easing = FastOutSlowInEasing)) {
              it / 10
            }
        },
      ) {
        composable(Routes.HOME) {
          HomeDashboardScreen(
            displayName = displayName,
            favoriteCount = favoriteIds.size,
            activeAlertCount = activeAlertCount,
            unreadAlertCount = unreadAlertCount,
            isPremiumActive = isPremiumActive,
            showPremiumOption = showPremiumOption,
            searchQuery = homeSearchQuery,
            searchMatchCount = homeSearchMatchCount,
            onSearchQueryChange = { q -> homeSearchQuery = q },
            onSearchSubmit = { q ->
              marketplaceViewModel.resetFilters()
              marketplaceViewModel.updateSearchQuery(q.trim())
              navController.navigate(Routes.propertyList("SEARCH"))
            },
            onQuickFilterListingType = { type ->
              marketplaceViewModel.resetFilters()
              if (homeSearchQuery.isNotBlank()) {
                marketplaceViewModel.updateSearchQuery(homeSearchQuery.trim())
              }
              marketplaceViewModel.setListingTypeFilter(type)
              val routeMode =
                when (type) {
                  ListingType.SALE -> "SALE"
                  ListingType.RENT -> "RENT"
                  ListingType.HEAVY_DEPOSIT -> "HEAVY_DEPOSIT"
                }
              navController.navigate(Routes.propertyList(routeMode))
            },
            onQuickFilterCategory = { category ->
              marketplaceViewModel.resetFilters()
              if (homeSearchQuery.isNotBlank()) {
                marketplaceViewModel.updateSearchQuery(homeSearchQuery.trim())
              }
              marketplaceViewModel.setCategoryFilter(category)
              navController.navigate(Routes.propertyList("SEARCH"))
            },
            onOpenAiTool = { navigateSingleTop(Routes.AI_ASSISTANT) },
            onOpenAlerts = { navigateSingleTop(Routes.NOTIFICATIONS) },
            onOpenSaleList = {
              marketplaceViewModel.resetFilters()
              marketplaceViewModel.setListingTypeFilter(ListingType.SALE)
              navController.navigate(Routes.propertyList("SALE"))
            },
            onOpenBuyList = {
              marketplaceViewModel.resetFilters()
              marketplaceViewModel.setListingTypeFilter(ListingType.SALE)
              navController.navigate(Routes.propertyList("BUY"))
            },
            onOpenRentOptions = { navigateSingleTop(Routes.RENT_OPTIONS) },
            onOpenHeavyDeposit = { navigateSingleTop(Routes.HEAVY_DEPOSIT_OPTIONS) },
            onOpenMap = {
              marketplaceViewModel.resetFilters()
              navController.navigate(Routes.propertyList("MAP"))
            },
            onOpenFavorites = {
              marketplaceViewModel.resetFilters()
              marketplaceViewModel.setFavoritesOnly(true)
              navController.navigate(Routes.propertyList("FAVORITES"))
            },
            onOpenChats = { navigateSingleTop(Routes.CHATS) },
            onOpenHome = { navController.popBackStack(Routes.HOME, inclusive = false) },
            onOpenProfile = { navigateSingleTop(Routes.PROFILE) },
            onOpenPremium = { navigateSingleTop(Routes.PREMIUM) },
            onOpenTerms = { navigateSingleTop(Routes.TERMS) },
            onSignOut = performSignOut,
          )
        }

        composable(Routes.RENT_OPTIONS) {
          RentOptionsScreen(
            onBack = { navController.popBackStack() },
            onSelectOwner = { navController.navigate(Routes.addProperty(ListingType.RENT)) },
            onSelectRenter = {
              marketplaceViewModel.resetFilters()
              marketplaceViewModel.setListingTypeFilter(ListingType.RENT)
              navController.navigate(Routes.propertyList("RENT"))
            },
          )
        }

        composable(Routes.HEAVY_DEPOSIT_OPTIONS) {
          HeavyDepositOptionsScreen(
            onBack = { navController.popBackStack() },
            onSelectOwner = {
              navController.navigate(Routes.addProperty(ListingType.HEAVY_DEPOSIT))
            },
            onSelectSeeker = {
              marketplaceViewModel.resetFilters()
              marketplaceViewModel.setListingTypeFilter(ListingType.HEAVY_DEPOSIT)
              navController.navigate(Routes.propertyList("HEAVY_DEPOSIT"))
            },
          )
        }

        composable(
          route = Routes.PROPERTY_LIST,
          arguments = listOf(navArgument("mode") { type = NavType.StringType }),
        ) { entry ->
          val mode = entry.arguments?.getString("mode") ?: "BUY"
          val canAdd = mode == "SALE"
          val isFavMode = mode == "FAVORITES"
          val isMapMode = mode == "MAP"
          val screenTitle =
            when (mode) {
              "SALE" -> "Sale Properties"
              "RENT" -> "Rent Properties"
              "HEAVY_DEPOSIT" -> "Heavy Deposit Properties"
              "FAVORITES" -> "Favorites"
              "MAP" -> "Property Map"
              "PRICE_FILTER" -> "Properties by Price"
              "SEARCH" -> "Search Results"
              else -> "Buy Properties"
            }

          ExplorePropertiesScreen(
            screenTitle = screenTitle,
            canAddProperty = canAdd,
            isFavoritesScreen = isFavMode,
            initialMapMode = isMapMode,
            propertiesState = filteredPropertiesState,
            filter = filter,
            favoriteIds = favoriteIds,
            unlockedIds = unlockedIds,
            membership = membership,
            currentUserId = user.uid,
            onBack = { navController.popBackStack() },
            onSearchChange = marketplaceViewModel::updateSearchQuery,
            onListingTypeChange = marketplaceViewModel::setListingTypeFilter,
            onPriceRangeChange = marketplaceViewModel::setPriceRange,
            onCategoryChange = marketplaceViewModel::setCategoryFilter,
            onToggleFavorite = marketplaceViewModel::toggleFavorite,
            onOpenFavorites = {
              marketplaceViewModel.resetFilters()
              marketplaceViewModel.setFavoritesOnly(true)
              navController.navigate(Routes.propertyList("FAVORITES"))
            },
            onSaveSearchAlert = {
              val alertName =
                PropertyAlertMatcher.buildDefaultName(
                  listingType = filter.listingType,
                  category = filter.category,
                  locationQuery = filter.searchQuery,
                  minPrice = filter.minPrice,
                  maxPrice = filter.maxPrice,
                )
              notificationStore.saveAlert(
                SavedPropertyAlert(
                  name = alertName,
                  listingType = filter.listingType,
                  category = filter.category,
                  locationQuery = filter.searchQuery.trim(),
                  minPrice = filter.minPrice,
                  maxPrice = filter.maxPrice,
                )
              )
              scope.launch {
                snackbarHostState.showSnackbar("Saved alert: $alertName")
              }
            },
            onOpenPropertyDetail = { propertyId ->
              navController.navigate(Routes.propertyDetail(propertyId))
            },
            onAddProperty = {
              val addType =
                if (mode == "HEAVY_DEPOSIT") ListingType.HEAVY_DEPOSIT else ListingType.SALE
              navController.navigate(Routes.addProperty(addType))
            },
          )
        }

        composable(
          route = Routes.ADD_PROPERTY,
          arguments = listOf(navArgument("listingType") { type = NavType.StringType }),
        ) { entry ->
          val listingType =
            ListingType.fromRaw(entry.arguments?.getString("listingType"))
          AddPropertyScreen(
            listingType = listingType,
            defaultOwnerName = displayName,
            defaultPhone = preferredPhone,
            defaultEmail = userEmail,
            isSubmitting = isSubmitting,
            requiresPremiumGate = requiresPremiumToAdd,
            onOpenPremium = { navigateSingleTop(Routes.PREMIUM) },
            onBack = { navController.popBackStack() },
            onSubmit = {
              title,
              description,
              price,
              location,
              ownerName,
              contactPhone,
              contactEmail,
              whatsappEnabled,
              type,
              category,
              bedrooms,
              areaSqFt,
              photoKeys,
              videoLabel,
              totalMediaBytes,
              latitude,
              longitude,
              floor,
              depositAmount,
              monthlyRent,
              buildingAge,
              hasLift ->
              marketplaceViewModel.createProperty(
                title = title,
                description = description,
                price = price,
                location = location,
                ownerName = ownerName,
                contactPhone = contactPhone,
                contactEmail = contactEmail,
                whatsappEnabled = whatsappEnabled,
                listingType = type,
                category = category,
                bedrooms = bedrooms,
                areaSqFt = areaSqFt,
                photoKeys = photoKeys,
                videoLabel = videoLabel,
                totalMediaBytes = totalMediaBytes,
                latitude = latitude,
                longitude = longitude,
                floor = floor,
                depositAmount = depositAmount,
                monthlyRent = monthlyRent,
                buildingAge = buildingAge,
                hasLift = hasLift,
                onCreated = { createdId ->
                  navController.popBackStack()
                  navController.navigate(Routes.propertyDetail(createdId))
                },
              )
            },
          )
        }

        composable(
          route = Routes.PROPERTY_DETAIL,
          arguments = listOf(navArgument("propertyId") { type = NavType.StringType }),
        ) { entry ->
          val propertyId = entry.arguments?.getString("propertyId") ?: return@composable
          val detailViewModel: PropertyDetailViewModel =
            viewModel(
              key = "${user.uid}_detail_$propertyId",
              factory =
                viewModelFactory {
                  initializer {
                    val app = checkNotNull(this[APPLICATION_KEY])
                    val databaseId = app.getString(R.string.firestore_database_id)
                    val db = FirebaseFirestore.getInstance(databaseId)
                    PropertyDetailViewModel(
                      propertyRepository = PropertyRepository(db),
                      chatRepository = ChatRepository(db),
                      currentUserId = user.uid,
                      propertyId = propertyId,
                    )
                  }
                },
            )
          PropertyDetailScreen(
            viewModel = detailViewModel,
            currentUserId = user.uid,
            currentUserName = displayName,
            isFavorite = propertyId in favoriteIds,
            onToggleFavorite = { marketplaceViewModel.toggleFavorite(propertyId) },
            onDeleteProperty = { id -> marketplaceViewModel.deleteProperty(id) },
            onOpenChat = { chatId -> navController.navigate(Routes.chatThread(chatId)) },
            onBack = { navController.popBackStack() },
          )
        }

        composable(Routes.CHATS) {
          ChatsListScreen(
            chatsState = myChatsState,
            currentUserId = user.uid,
            onBack = { navController.popBackStack() },
            onOpenChat = { chatId -> navController.navigate(Routes.chatThread(chatId)) },
          )
        }

        composable(
          route = Routes.CHAT_THREAD,
          arguments = listOf(navArgument("chatId") { type = NavType.StringType }),
        ) { entry ->
          val chatId = entry.arguments?.getString("chatId") ?: return@composable
          val chatViewModel: ChatViewModel =
            viewModel(
              key = "${user.uid}_chat_$chatId",
              factory =
                viewModelFactory {
                  initializer {
                    val app = checkNotNull(this[APPLICATION_KEY])
                    val databaseId = app.getString(R.string.firestore_database_id)
                    val db = FirebaseFirestore.getInstance(databaseId)
                    ChatViewModel(
                      chatRepository = ChatRepository(db),
                      currentUserId = user.uid,
                      chatId = chatId,
                    )
                  }
                },
            )
          ChatThreadScreen(
            viewModel = chatViewModel,
            currentUserName = displayName,
            onMessageSent = { text -> notificationStore.markLocallySentMessage(chatId, text) },
            onBack = { navController.popBackStack() },
          )
        }

        composable(Routes.AI_ASSISTANT) {
          AiAssistantScreen(
            viewModel = aiViewModel,
            availableProperties = activePropertiesList,
            onBack = { navController.popBackStack() },
          )
        }

        composable(Routes.NOTIFICATIONS) {
          NotificationsScreen(
            savedAlerts = savedAlerts,
            settings = notificationSettings,
            recentAlerts = recentAlerts,
            isPremiumActive = isPremiumActive,
            showPremiumOption = showPremiumOption,
            onTogglePropertyAlerts = { notificationStore.setPropertyAlertsEnabled(it) },
            onToggleChatAlerts = { notificationStore.setChatAlertsEnabled(it) },
            onSaveAlert = { alert ->
              notificationStore.saveAlert(alert)
              scope.launch { snackbarHostState.showSnackbar("Saved alert: ${alert.name}") }
            },
            onToggleAlertItem = { notificationStore.toggleAlert(it) },
            onDeleteAlertItem = { notificationStore.deleteAlert(it) },
            onMarkAllRead = { notificationStore.markAllRead() },
            onClearHistory = { notificationStore.clearHistory() },
            onOpenRecord = { record ->
              notificationStore.markRead(record.id)
              if (record.type == LocalNotificationStore.TYPE_CHAT_MESSAGE) {
                navController.navigate(Routes.chatThread(record.targetId))
              } else {
                navController.navigate(Routes.propertyDetail(record.targetId))
              }
            },
            onBack = { navController.popBackStack() },
            onOpenHome = { navController.popBackStack(Routes.HOME, inclusive = false) },
            onOpenProfile = { navigateSingleTop(Routes.PROFILE) },
            onOpenPremium = { navigateSingleTop(Routes.PREMIUM) },
            onOpenTerms = { navigateSingleTop(Routes.TERMS) },
            onSignOut = performSignOut,
          )
        }

        composable(Routes.PROFILE) {
          ProfileScreen(
            displayName = displayName,
            email = userEmail,
            currentRole = UserRole.fromRaw(userProfile?.role),
            isPremiumActive = isPremiumActive,
            showPremiumOption = showPremiumOption,
            myListingsCount = myListingsCount,
            favoriteCount = favoriteIds.size,
            unlockedCount = unlockedIds.size,
            activeAlertsCount = activeAlertCount,
            isDeletingAccount = isDeletingAccount,
            onSelectRole = { role -> marketplaceViewModel.updateUserRole(role) },
            onBack = { navController.popBackStack() },
            onOpenHome = { navController.popBackStack(Routes.HOME, inclusive = false) },
            onOpenPremium = { navigateSingleTop(Routes.PREMIUM) },
            onOpenAlerts = { navigateSingleTop(Routes.NOTIFICATIONS) },
            onOpenTerms = { navigateSingleTop(Routes.TERMS) },
            onOpenFavorites = {
              marketplaceViewModel.resetFilters()
              marketplaceViewModel.setFavoritesOnly(true)
              navController.navigate(Routes.propertyList("FAVORITES"))
            },
            onSignOut = performSignOut,
            onDeleteAccount = performDeleteAccount,
          )
        }

        composable(Routes.PREMIUM) {
          PremiumScreen(
            membership = membership,
            isSubmitting = isSubmitting,
            onActivatePass = { method -> marketplaceViewModel.activatePremiumPass(method) },
            onBack = { navController.popBackStack() },
            onOpenHome = { navController.popBackStack(Routes.HOME, inclusive = false) },
            onOpenProfile = { navigateSingleTop(Routes.PROFILE) },
            onOpenTerms = { navigateSingleTop(Routes.TERMS) },
            onSignOut = performSignOut,
          )
        }

        composable(Routes.TERMS) {
          TermsAndConditionsScreen(
            isPremiumActive = isPremiumActive,
            showPremiumOption = showPremiumOption,
            onBack = { navController.popBackStack() },
            onOpenHome = { navController.popBackStack(Routes.HOME, inclusive = false) },
            onOpenProfile = { navigateSingleTop(Routes.PROFILE) },
            onOpenPremium = { navigateSingleTop(Routes.PREMIUM) },
            onSignOut = performSignOut,
          )
        }
      }
    }
  }
}
