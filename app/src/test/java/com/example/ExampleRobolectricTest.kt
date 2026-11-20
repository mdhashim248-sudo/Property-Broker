package com.example

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import com.example.data.GeoPoint
import com.example.data.LocalNotificationStore
import com.example.data.LocationResolver
import com.example.data.PremiumSchedule
import com.example.data.PropertyAlertMatcher
import com.example.data.SavedPropertyAlert
import com.example.data.model.ListingType
import com.example.data.model.PropertyCategory
import com.example.data.model.PropertyChat
import com.example.data.model.PropertyFormatters
import com.example.data.model.PropertyListing
import com.example.data.model.PropertySearchMatcher
import com.google.firebase.Timestamp
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun `read string and verify home hero bitmap fits GPU texture limits`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Property Broker", appName)

    val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.img_home_hero_keys)
    assertNotNull("Home hero bitmap must decode cleanly", bitmap)
    assertTrue("Bitmap width must stay <= 2048px (was ${bitmap.width})", bitmap.width <= 2048)
    assertTrue("Bitmap height must stay <= 2048px (was ${bitmap.height})", bitmap.height <= 2048)
  }

  @Test
  fun `location resolver maps Indian localities and projects Web Mercator accurately`() {
    val storedProp =
      PropertyListing(id = "p1", location = "Unknown Area", latitude = 19.0596, longitude = 72.8295)
    val storedResolved = LocationResolver.resolveCoordinates(storedProp)
    assertEquals(19.0596, storedResolved.latitude, 0.0001)
    assertEquals(72.8295, storedResolved.longitude, 0.0001)

    val worliProp = PropertyListing(id = "p2", location = "Worli Sea Face, Mumbai")
    val worliGeo = LocationResolver.resolveCoordinates(worliProp)
    assertTrue("Expected Worli latitude near 19.02", abs(worliGeo.latitude - 19.0176) < 0.01)
    assertTrue("Expected Worli longitude near 72.82", abs(worliGeo.longitude - 72.8156) < 0.01)

    val origin = GeoPoint(19.0760, 72.8777)
    val world = LocationResolver.latLngToWorldPx(origin, 12.0)
    val roundTrip = LocationResolver.worldPxToLatLng(world, 12.0)
    assertEquals(origin.latitude, roundTrip.latitude, 0.0001)
    assertEquals(origin.longitude, roundTrip.longitude, 0.0001)

    assertEquals("₹3.45Cr", PropertyFormatters.formatCompactPinPrice(34_500_000L, ListingType.SALE.name))
    assertEquals("₹52K/m", PropertyFormatters.formatCompactPinPrice(52_000L, ListingType.RENT.name))
    assertEquals("₹15L", PropertyFormatters.formatCompactPinPrice(1_500_000L, ListingType.HEAVY_DEPOSIT.name))
    assertEquals("Any Price", PropertyFormatters.formatPriceRangeLabel(null, null))
    assertEquals("Under ₹50L", PropertyFormatters.formatPriceRangeLabel(null, 5_000_000L))
    assertEquals("₹50L – ₹1Cr", PropertyFormatters.formatPriceRangeLabel(5_000_000L, 10_000_000L))
    assertEquals("Above ₹1Cr", PropertyFormatters.formatPriceRangeLabel(10_000_000L, null))

    // Verify PropertySearchMatcher matches by name, location, and price expressions
    val sampleFlat =
      PropertyListing(
        id = "s1",
        title = "Sunshine 2BHK Flat",
        location = "Andheri West, Mumbai",
        price = 7_500_000L,
        listingType = ListingType.SALE.name,
      )
    assertTrue(PropertySearchMatcher.matches(sampleFlat, "Sunshine"))
    assertTrue(PropertySearchMatcher.matches(sampleFlat, "Andheri"))
    assertTrue(PropertySearchMatcher.matches(sampleFlat, "50L - 1Cr"))
    assertTrue(PropertySearchMatcher.matches(sampleFlat, "75L"))
    assertFalse(PropertySearchMatcher.matches(sampleFlat, "< 50L"))
    assertFalse(PropertySearchMatcher.matches(sampleFlat, "> 1Cr"))
  }

  @Test
  fun `premium schedule hides options through 31 Dec 2026 and activates on 1 Jan 2027`() {
    val utc = TimeZone.getTimeZone("UTC")
    val dec31End =
      Calendar.getInstance(utc).apply {
        set(2026, Calendar.DECEMBER, 31, 23, 59, 59)
        set(Calendar.MILLISECOND, 999)
      }.timeInMillis
    val jan1Start =
      Calendar.getInstance(utc).apply {
        set(2027, Calendar.JANUARY, 1, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
      }.timeInMillis

    // Through 31 Dec 2026: Premium hidden, Add Property and Contact Viewing free for all
    assertFalse(PremiumSchedule.isPremiumFeatureEnabled(dec31End, utc))
    assertTrue(PremiumSchedule.isFreeAccessPeriod(dec31End, utc))
    assertTrue(PremiumSchedule.canAddProperty(hasActivePremium = false, nowMillis = dec31End, timeZone = utc))
    assertTrue(
      PremiumSchedule.canViewContact(
        isOwner = false,
        isUnlocked = false,
        hasActivePremium = false,
        nowMillis = dec31End,
        timeZone = utc,
      )
    )

    // Starting 1 Jan 2027: Premium active for Add Property and Buyer/Renter contact viewing
    assertTrue(PremiumSchedule.isPremiumFeatureEnabled(jan1Start, utc))
    assertFalse(PremiumSchedule.isFreeAccessPeriod(jan1Start, utc))
    assertFalse(PremiumSchedule.canAddProperty(hasActivePremium = false, nowMillis = jan1Start, timeZone = utc))
    assertTrue(PremiumSchedule.canAddProperty(hasActivePremium = true, nowMillis = jan1Start, timeZone = utc))
    assertFalse(
      PremiumSchedule.canViewContact(
        isOwner = false,
        isUnlocked = false,
        hasActivePremium = false,
        nowMillis = jan1Start,
        timeZone = utc,
      )
    )
    assertTrue(
      PremiumSchedule.canViewContact(
        isOwner = false,
        isUnlocked = false,
        hasActivePremium = true,
        nowMillis = jan1Start,
        timeZone = utc,
      )
    )
  }

  @Test
  fun `local notification store matches saved property criteria and triggers chat alerts`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val store = LocalNotificationStore(context)
    store.clearHistory()
    store.savedAlerts.value.forEach { store.deleteAlert(it.id) }

    val bandraAlert =
      SavedPropertyAlert(
        id = "alert_bandra",
        name = "Bandra Sale Under 1Cr",
        listingType = ListingType.SALE,
        category = PropertyCategory.APARTMENT,
        locationQuery = "Bandra",
        maxPrice = 10_000_000L,
      )
    store.saveAlert(bandraAlert)

    val existingProp =
      PropertyListing(
        id = "p_old",
        title = "Old Bandra Flat",
        location = "Bandra West, Mumbai",
        price = 8_500_000L,
        listingType = ListingType.SALE.name,
        propertyCategory = PropertyCategory.APARTMENT.name,
      )
    // First snapshot baselines without spamming
    assertTrue(store.processPropertiesSnapshot(listOf(existingProp)).isEmpty())

    // New non-matching property (above maxPrice)
    val expensiveProp =
      PropertyListing(
        id = "p_expensive",
        title = "Luxury Penthouse",
        location = "Bandra West, Mumbai",
        price = 25_000_000L,
        listingType = ListingType.SALE.name,
        propertyCategory = PropertyCategory.APARTMENT.name,
      )
    assertFalse(PropertyAlertMatcher.matches(expensiveProp, bandraAlert))
    assertTrue(store.processPropertiesSnapshot(listOf(existingProp, expensiveProp)).isEmpty())

    // New matching property triggers local alert record
    val matchingProp =
      PropertyListing(
        id = "p_match",
        title = "Sea Breeze 2BHK",
        location = "Bandra East, Mumbai",
        price = 9_200_000L,
        listingType = ListingType.SALE.name,
        propertyCategory = PropertyCategory.APARTMENT.name,
      )
    assertTrue(PropertyAlertMatcher.matches(matchingProp, bandraAlert))
    val propAlerts =
      store.processPropertiesSnapshot(listOf(existingProp, expensiveProp, matchingProp))
    assertEquals(1, propAlerts.size)
    assertEquals(LocalNotificationStore.TYPE_PROPERTY_MATCH, propAlerts.first().type)
    assertEquals("p_match", propAlerts.first().targetId)

    // Chat notifications: baseline first, suppress local send, alert on incoming message
    val baseChat =
      PropertyChat(
        id = "chat_1",
        propertyId = "p_match",
        propertyTitle = "Sea Breeze 2BHK",
        buyerId = "user_me",
        buyerName = "Me",
        ownerId = "owner_1",
        ownerName = "Rajesh Sharma",
        participantUids = listOf("user_me", "owner_1"),
        lastMessage = "Hello",
        updatedAt = Timestamp(1000L, 0),
      )
    assertTrue(store.processChatsSnapshot(listOf(baseChat), "user_me", null).isEmpty())

    // Outgoing message sent by current user does NOT self-notify
    store.markLocallySentMessage("chat_1", "Is it available?")
    val selfUpdatedChat = baseChat.copy(lastMessage = "Is it available?", updatedAt = Timestamp(1005L, 0))
    assertTrue(store.processChatsSnapshot(listOf(selfUpdatedChat), "user_me", null).isEmpty())

    // Incoming reply from owner DOES trigger chat notification
    val ownerReplyChat =
      baseChat.copy(lastMessage = "Yes, visit tomorrow at 5 PM", updatedAt = Timestamp(1010L, 0))
    val chatAlerts = store.processChatsSnapshot(listOf(ownerReplyChat), "user_me", null)
    assertEquals(1, chatAlerts.size)
    assertEquals(LocalNotificationStore.TYPE_CHAT_MESSAGE, chatAlerts.first().type)
    assertEquals("chat_1", chatAlerts.first().targetId)
    assertTrue(chatAlerts.first().title.contains("Rajesh Sharma"))
  }

  @Test
  fun `launches main activity directly into production login and sign up without test mode`() {
    composeTestRule.waitForIdle()

    // Test Mode is completely removed: verify no testing banner or skip-sign-in bypass exists
    composeTestRule.onNodeWithTag("testing_hold_banner").assertDoesNotExist()
    composeTestRule.onNodeWithTag("continue_testing_mode_button").assertDoesNotExist()

    // Verify production Login & Sign Up switcher and Google Sign-In button are displayed
    composeTestRule.onNodeWithTag("auth_mode_switcher").assertIsDisplayed()
    composeTestRule.onNodeWithTag("auth_tab_login").assertIsDisplayed()
    composeTestRule.onNodeWithTag("auth_tab_signup").assertIsDisplayed()
    composeTestRule.onNodeWithTag("google_sign_in_button").performScrollTo().assertIsDisplayed()

    // Switch to Sign Up tab -> verify form inputs & validation
    composeTestRule.onNodeWithTag("auth_tab_signup").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("signup_name_input").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("signup_phone_input").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("signup_submit_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("auth_error_banner").performScrollTo().assertIsDisplayed()

    // Fill valid Sign Up details
    composeTestRule.onNodeWithTag("signup_name_input").performScrollTo().performTextInput("Aarav Sharma")
    composeTestRule.onNodeWithTag("signup_phone_input").performScrollTo().performTextInput("9820112233")
    composeTestRule.waitForIdle()

    // Switch back to Login mode cleanly
    composeTestRule.onNodeWithTag("go_to_login_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("google_sign_in_button").performScrollTo().assertIsDisplayed()
  }
}
