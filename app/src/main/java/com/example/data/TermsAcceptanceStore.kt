package com.example.data

import android.content.Context

/**
 * Persists per-account acceptance of Property Broker's Terms & Conditions and RERA Disclaimer
 * so users must read and accept them after signing in before using the app.
 */
class TermsAcceptanceStore(context: Context) {
  private val prefs =
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  fun hasAccepted(userId: String): Boolean {
    if (userId.isBlank()) return false
    return prefs.getBoolean(acceptedKey(userId), false)
  }

  fun markAccepted(userId: String) {
    if (userId.isBlank()) return
    prefs
      .edit()
      .putBoolean(acceptedKey(userId), true)
      .putLong(timestampKey(userId), System.currentTimeMillis())
      .apply()
  }

  fun clearAcceptance(userId: String) {
    if (userId.isBlank()) return
    prefs
      .edit()
      .remove(acceptedKey(userId))
      .remove(timestampKey(userId))
      .apply()
  }

  private fun acceptedKey(userId: String) = "terms_accepted_v${TERMS_VERSION}_$userId"

  private fun timestampKey(userId: String) = "terms_accepted_at_v${TERMS_VERSION}_$userId"

  companion object {
    private const val PREFS_NAME = "property_broker_terms_prefs"
    private const val TERMS_VERSION = 1
  }
}
