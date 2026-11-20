package com.example.data

import java.util.Calendar
import java.util.TimeZone

/**
 * Controls the scheduled launch of Premium features:
 * - Through 31 December 2026: All Premium options are hidden and Add Property + viewing
 *   Owner Contact Information are open free of charge across every service.
 * - From 1 January 2027 (00:00:00) onward: Premium options automatically activate across
 *   all services (Sale, Buy, Rent, Heavy Deposit) for Add Property and viewing Owner
 *   Contact Information for Buyers, Renters, and Heavy Deposit Seekers.
 */
object PremiumSchedule {
  const val ACTIVATION_YEAR = 2027
  const val ACTIVATION_MONTH = Calendar.JANUARY // 0-indexed (January)
  const val ACTIVATION_DAY = 1
  const val ACTIVATION_DATE_LABEL = "1 January 2027"

  fun activationStartMillis(timeZone: TimeZone = TimeZone.getDefault()): Long {
    val cal =
      Calendar.getInstance(timeZone).apply {
        set(Calendar.YEAR, ACTIVATION_YEAR)
        set(Calendar.MONTH, ACTIVATION_MONTH)
        set(Calendar.DAY_OF_MONTH, ACTIVATION_DAY)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }
    return cal.timeInMillis
  }

  fun isPremiumFeatureEnabled(
    nowMillis: Long = System.currentTimeMillis(),
    timeZone: TimeZone = TimeZone.getDefault(),
  ): Boolean = nowMillis >= activationStartMillis(timeZone)

  fun isFreeAccessPeriod(
    nowMillis: Long = System.currentTimeMillis(),
    timeZone: TimeZone = TimeZone.getDefault(),
  ): Boolean = !isPremiumFeatureEnabled(nowMillis, timeZone)

  fun canAddProperty(
    hasActivePremium: Boolean,
    nowMillis: Long = System.currentTimeMillis(),
    timeZone: TimeZone = TimeZone.getDefault(),
  ): Boolean = isFreeAccessPeriod(nowMillis, timeZone) || hasActivePremium

  fun canViewContact(
    isOwner: Boolean,
    isUnlocked: Boolean,
    hasActivePremium: Boolean,
    nowMillis: Long = System.currentTimeMillis(),
    timeZone: TimeZone = TimeZone.getDefault(),
  ): Boolean = isFreeAccessPeriod(nowMillis, timeZone) || isOwner || isUnlocked || hasActivePremium
}
