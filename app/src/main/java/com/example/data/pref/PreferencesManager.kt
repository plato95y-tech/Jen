package com.example.data.pref

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("debt_tracker_prefs", Context.MODE_PRIVATE)

    private val _currency = MutableStateFlow(prefs.getString(KEY_CURRENCY, "ر.س") ?: "ر.س")
    val currency: StateFlow<String> = _currency.asStateFlow()

    private val _defaultCurrencyCode = MutableStateFlow(prefs.getString(KEY_DEFAULT_CURRENCY_CODE, "SAR") ?: "SAR")
    val defaultCurrencyCode: StateFlow<String> = _defaultCurrencyCode.asStateFlow()

    private val _useLatestRateForAll = MutableStateFlow(prefs.getBoolean(KEY_USE_LATEST_RATE_FOR_ALL, false))
    val useLatestRateForAll: StateFlow<Boolean> = _useLatestRateForAll.asStateFlow()

    private val _lastBackupTime = MutableStateFlow(prefs.getLong(KEY_LAST_BACKUP, 0L))
    val lastBackupTime: StateFlow<Long> = _lastBackupTime.asStateFlow()

    private val _themeMode = MutableStateFlow(prefs.getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _themePalette = MutableStateFlow(prefs.getString(KEY_THEME_PALETTE, "EMERALD") ?: "EMERALD")
    val themePalette: StateFlow<String> = _themePalette.asStateFlow()

    private val _isPrivacyMode = MutableStateFlow(prefs.getBoolean(KEY_PRIVACY_MODE, false))
    val isPrivacyMode: StateFlow<Boolean> = _isPrivacyMode.asStateFlow()

    private val _isAppLockEnabled = MutableStateFlow(prefs.getBoolean(KEY_APP_LOCK_ENABLED, false))
    val isAppLockEnabled: StateFlow<Boolean> = _isAppLockEnabled.asStateFlow()

    private val _pinCode = MutableStateFlow(prefs.getString(KEY_PIN_CODE, "") ?: "")
    val pinCode: StateFlow<String> = _pinCode.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false))
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    // Auto-backup preferences (Proposal #4)
    private val _isAutoBackupEnabled = MutableStateFlow(prefs.getBoolean(KEY_AUTO_BACKUP_ENABLED, true))
    val isAutoBackupEnabled: StateFlow<Boolean> = _isAutoBackupEnabled.asStateFlow()

    private val _autoBackupIntervalDays = MutableStateFlow(prefs.getInt(KEY_AUTO_BACKUP_INTERVAL_DAYS, 7))
    val autoBackupIntervalDays: StateFlow<Int> = _autoBackupIntervalDays.asStateFlow()

    // Smart input: remember last accessed/used store
    private val _lastUsedStoreId = MutableStateFlow(prefs.getLong(KEY_LAST_USED_STORE_ID, 0L))
    val lastUsedStoreId: StateFlow<Long> = _lastUsedStoreId.asStateFlow()

    // Smart notifications
    private val _isSmartSummaryNotificationEnabled = MutableStateFlow(prefs.getBoolean(KEY_SMART_SUMMARY_ENABLED, true))
    val isSmartSummaryNotificationEnabled: StateFlow<Boolean> = _isSmartSummaryNotificationEnabled.asStateFlow()

    private val _isDebtLimitAlertEnabled = MutableStateFlow(prefs.getBoolean(KEY_DEBT_LIMIT_ALERT_ENABLED, true))
    val isDebtLimitAlertEnabled: StateFlow<Boolean> = _isDebtLimitAlertEnabled.asStateFlow()

    // Daily Transaction Reminder
    private val _isDailyReminderEnabled = MutableStateFlow(prefs.getBoolean(KEY_DAILY_REMINDER_ENABLED, false))
    val isDailyReminderEnabled: StateFlow<Boolean> = _isDailyReminderEnabled.asStateFlow()

    private val _dailyReminderHour = MutableStateFlow(prefs.getInt(KEY_DAILY_REMINDER_HOUR, 21)) // 9:00 PM default
    val dailyReminderHour: StateFlow<Int> = _dailyReminderHour.asStateFlow()

    private val _dailyReminderMinute = MutableStateFlow(prefs.getInt(KEY_DAILY_REMINDER_MINUTE, 0))
    val dailyReminderMinute: StateFlow<Int> = _dailyReminderMinute.asStateFlow()

    private val _dailyReminderDays = MutableStateFlow(
        prefs.getStringSet(KEY_DAILY_REMINDER_DAYS, setOf("1", "2", "3", "4", "5", "6", "7"))
            ?.mapNotNull { it.toIntOrNull() }?.toSet() ?: setOf(1, 2, 3, 4, 5, 6, 7)
    )
    val dailyReminderDays: StateFlow<Set<Int>> = _dailyReminderDays.asStateFlow()

    private val _skipReminderIfRecordedToday = MutableStateFlow(prefs.getBoolean(KEY_SKIP_REMINDER_IF_RECORDED_TODAY, true))
    val skipReminderIfRecordedToday: StateFlow<Boolean> = _skipReminderIfRecordedToday.asStateFlow()

    // Auto-prune orphaned rate history preference
    private val _isAutoPruneRateHistoryEnabled = MutableStateFlow(prefs.getBoolean(KEY_AUTO_PRUNE_RATE_HISTORY, false))
    val isAutoPruneRateHistoryEnabled: StateFlow<Boolean> = _isAutoPruneRateHistoryEnabled.asStateFlow()

    // Runtime unlock state: if lock is enabled and pin is set, starts locked
    private val _isUnlocked = MutableStateFlow(
        !prefs.getBoolean(KEY_APP_LOCK_ENABLED, false) || prefs.getString(KEY_PIN_CODE, "").isNullOrEmpty()
    )
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    fun setPrivacyMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PRIVACY_MODE, enabled).apply()
        _isPrivacyMode.value = enabled
    }

    fun togglePrivacyMode() {
        setPrivacyMode(!_isPrivacyMode.value)
    }

    fun setAppLock(enabled: Boolean, pin: String) {
        prefs.edit()
            .putBoolean(KEY_APP_LOCK_ENABLED, enabled)
            .putString(KEY_PIN_CODE, pin)
            .apply()
        _isAppLockEnabled.value = enabled
        _pinCode.value = pin
        if (!enabled) {
            _isUnlocked.value = true
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
        _isBiometricEnabled.value = enabled
    }

    fun unlock() {
        _isUnlocked.value = true
    }

    fun lock() {
        if (_isAppLockEnabled.value && _pinCode.value.isNotEmpty()) {
            _isUnlocked.value = false
        }
    }

    fun verifyPin(enteredPin: String): Boolean {
        val matches = _pinCode.value == enteredPin
        if (matches) {
            _isUnlocked.value = true
        }
        return matches
    }

    fun setCurrency(newCurrency: String) {
        val trimmed = newCurrency.trim()
        if (trimmed.isNotEmpty()) {
            prefs.edit().putString(KEY_CURRENCY, trimmed).apply()
            _currency.value = trimmed
        }
    }

    fun setDefaultCurrencyCode(code: String) {
        val trimmed = code.trim()
        if (trimmed.isNotEmpty()) {
            prefs.edit().putString(KEY_DEFAULT_CURRENCY_CODE, trimmed).apply()
            _defaultCurrencyCode.value = trimmed
        }
    }

    fun setUseLatestRateForAll(useLatest: Boolean) {
        prefs.edit().putBoolean(KEY_USE_LATEST_RATE_FOR_ALL, useLatest).apply()
        _useLatestRateForAll.value = useLatest
    }

    fun updateLastBackupTime(timestamp: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(KEY_LAST_BACKUP, timestamp).apply()
        _lastBackupTime.value = timestamp
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
        _themeMode.value = mode
    }

    fun setThemePalette(palette: String) {
        prefs.edit().putString(KEY_THEME_PALETTE, palette).apply()
        _themePalette.value = palette
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_BACKUP_ENABLED, enabled).apply()
        _isAutoBackupEnabled.value = enabled
    }

    fun setAutoBackupIntervalDays(days: Int) {
        val validDays = days.coerceIn(1, 30)
        prefs.edit().putInt(KEY_AUTO_BACKUP_INTERVAL_DAYS, validDays).apply()
        _autoBackupIntervalDays.value = validDays
    }

    fun setLastUsedStoreId(storeId: Long) {
        prefs.edit().putLong(KEY_LAST_USED_STORE_ID, storeId).apply()
        _lastUsedStoreId.value = storeId
    }

    fun setSmartSummaryNotificationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SMART_SUMMARY_ENABLED, enabled).apply()
        _isSmartSummaryNotificationEnabled.value = enabled
    }

    fun setDebtLimitAlertEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DEBT_LIMIT_ALERT_ENABLED, enabled).apply()
        _isDebtLimitAlertEnabled.value = enabled
    }

    fun setDailyReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DAILY_REMINDER_ENABLED, enabled).apply()
        _isDailyReminderEnabled.value = enabled
    }

    fun setDailyReminderTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt(KEY_DAILY_REMINDER_HOUR, hour)
            .putInt(KEY_DAILY_REMINDER_MINUTE, minute)
            .apply()
        _dailyReminderHour.value = hour
        _dailyReminderMinute.value = minute
    }

    fun setDailyReminderDays(days: Set<Int>) {
        val stringSet = days.map { it.toString() }.toSet()
        prefs.edit().putStringSet(KEY_DAILY_REMINDER_DAYS, stringSet).apply()
        _dailyReminderDays.value = days
    }

    fun setSkipReminderIfRecordedToday(skip: Boolean) {
        prefs.edit().putBoolean(KEY_SKIP_REMINDER_IF_RECORDED_TODAY, skip).apply()
        _skipReminderIfRecordedToday.value = skip
    }

    fun setAutoPruneRateHistoryEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_PRUNE_RATE_HISTORY, enabled).apply()
        _isAutoPruneRateHistoryEnabled.value = enabled
    }

    companion object {
        private const val KEY_CURRENCY = "app_currency"
        private const val KEY_DEFAULT_CURRENCY_CODE = "default_currency_code"
        private const val KEY_USE_LATEST_RATE_FOR_ALL = "use_latest_rate_for_all"
        private const val KEY_LAST_BACKUP = "last_backup_timestamp"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_THEME_PALETTE = "theme_palette"
        private const val KEY_PRIVACY_MODE = "privacy_mode"
        private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
        private const val KEY_PIN_CODE = "app_pin_code"
        private const val KEY_BIOMETRIC_ENABLED = "app_biometric_enabled"
        private const val KEY_AUTO_BACKUP_ENABLED = "auto_backup_enabled"
        private const val KEY_AUTO_BACKUP_INTERVAL_DAYS = "auto_backup_interval_days"
        private const val KEY_LAST_USED_STORE_ID = "last_used_store_id"
        private const val KEY_SMART_SUMMARY_ENABLED = "smart_summary_enabled"
        private const val KEY_DEBT_LIMIT_ALERT_ENABLED = "debt_limit_alert_enabled"
        private const val KEY_AUTO_PRUNE_RATE_HISTORY = "auto_prune_rate_history"
        private const val KEY_DAILY_REMINDER_ENABLED = "daily_reminder_enabled"
        private const val KEY_DAILY_REMINDER_HOUR = "daily_reminder_hour"
        private const val KEY_DAILY_REMINDER_MINUTE = "daily_reminder_minute"
        private const val KEY_DAILY_REMINDER_DAYS = "daily_reminder_days"
        private const val KEY_SKIP_REMINDER_IF_RECORDED_TODAY = "skip_reminder_if_recorded_today"
    }
}
