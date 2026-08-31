package com.jdroidx.jfindme.utils

import android.util.Patterns

/**
 * ValidationEngine — Centralized Field Validation for ALL screens.
 *
 * AUDIT FIX [AUD-009, AUD-013, AUD-019]:
 * - Missing email format validation in AuthActivity
 * - Missing range/empty validation in GeofenceActivity
 * - Missing required-field validation in ProfileActivity
 *
 * Architecture: utils layer — pure Kotlin, no Android UI dependencies.
 * All validators return a ValidationResult with a user-friendly error message
 * (empty string = valid, non-empty = error to display).
 *
 * Usage:
 *   val result = ValidationEngine.validateEmail(emailInput)
 *   if (!result.isValid) showError(result.errorMessage)
 */
object ValidationEngine {

    // ─── RESULT MODEL ─────────────────────────────────────────────────────────

    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String = ""
    ) {
        companion object {
            val OK = ValidationResult(true)
            fun error(msg: String) = ValidationResult(false, msg)
        }
    }

    // ─── AUTH VALIDATIONS ──────────────────────────────────────────────────────

    fun validateEmail(email: String?): ValidationResult {
        val trimmed = email?.trim() ?: ""
        return when {
            trimmed.isEmpty() -> ValidationResult.error("Email address is required.")
            !Patterns.EMAIL_ADDRESS.matcher(trimmed).matches() ->
                ValidationResult.error("Please enter a valid email address (e.g. user@example.com).")
            trimmed.length > 254 ->
                ValidationResult.error("Email address is too long.")
            else -> ValidationResult.OK
        }
    }

    fun validatePassword(password: String?): ValidationResult {
        val pass = password ?: ""
        return when {
            pass.isEmpty() -> ValidationResult.error("Password is required.")
            pass.length < 6 -> ValidationResult.error("Password must be at least 6 characters.")
            pass.length > 128 -> ValidationResult.error("Password is too long (max 128 characters).")
            else -> ValidationResult.OK
        }
    }

    // ─── PROFILE & IDENTITY VALIDATIONS ───────────────────────────────────────

    fun validateFullName(name: String?): ValidationResult {
        val trimmed = name?.trim() ?: ""
        return when {
            trimmed.isEmpty() -> ValidationResult.error("Full name is required.")
            trimmed.length < 2 -> ValidationResult.error("Name must be at least 2 characters.")
            trimmed.length > 100 -> ValidationResult.error("Name is too long (max 100 characters).")
            !trimmed.matches(Regex("^[a-zA-Z0-9\\s._'-]+\$")) ->
                ValidationResult.error("Name contains invalid characters. Use letters, numbers, spaces, hyphens, periods, or underscores.")
            else -> ValidationResult.OK
        }
    }

    fun validateAlias(alias: String?): ValidationResult {
        val trimmed = alias?.trim() ?: ""
        return when {
            trimmed.isEmpty() -> ValidationResult.error("Identity alias is required.")
            trimmed.length < 2 -> ValidationResult.error("Alias must be at least 2 characters.")
            trimmed.length > 50 -> ValidationResult.error("Alias is too long (max 50 characters).")
            !trimmed.matches(Regex("^[a-zA-Z0-9\\s._'-]+\$")) ->
                ValidationResult.error("Alias contains invalid characters. Use letters, numbers, spaces, underscores, or hyphens.")
            else -> ValidationResult.OK
        }
    }

    fun validatePhone(phone: String?): ValidationResult {
        val trimmed = phone?.trim() ?: ""
        if (trimmed.isEmpty()) return ValidationResult.OK // Phone is optional
        return when {
            trimmed.length < 7 -> ValidationResult.error("Phone number is too short (min 7 digits).")
            trimmed.length > 16 -> ValidationResult.error("Phone number is too long (max 16 digits).")
            !trimmed.matches(Regex("^[+]?[0-9\\-\\s()]+\$")) ->
                ValidationResult.error("Phone number contains invalid characters. Use digits, +, -, (, ) only.")
            else -> ValidationResult.OK
        }
    }

    fun validatePincode(pincode: String?, countryCode: String = ""): ValidationResult {
        val trimmed = pincode?.trim() ?: ""
        if (trimmed.isEmpty()) return ValidationResult.error("Pincode / ZIP code is required.")
        return when {
            trimmed.length < 4 -> ValidationResult.error("Pincode is too short (min 4 digits).")
            trimmed.length > 10 -> ValidationResult.error("Pincode is too long (max 10 characters).")
            !trimmed.matches(Regex("^[0-9A-Za-z\\-\\s]+\$")) ->
                ValidationResult.error("Pincode contains invalid characters.")
            else -> ValidationResult.OK
        }
    }

    fun validateDateOfBirth(dob: String?): ValidationResult {
        if (dob.isNullOrBlank()) return ValidationResult.OK // DOB is optional
        return try {
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US)
            sdf.isLenient = false
            val parsed = sdf.parse(dob) ?: return ValidationResult.error("Invalid date format. Use DD/MM/YYYY.")
            val today = java.util.Calendar.getInstance()
            val dobCal = java.util.Calendar.getInstance().also { it.time = parsed }
            val age = today.get(java.util.Calendar.YEAR) - dobCal.get(java.util.Calendar.YEAR)
            when {
                parsed.after(java.util.Date()) -> ValidationResult.error("Date of birth cannot be in the future.")
                age < 5 -> ValidationResult.error("Age must be at least 5 years.")
                age > 120 -> ValidationResult.error("Please enter a valid date of birth.")
                else -> ValidationResult.OK
            }
        } catch (e: Exception) {
            ValidationResult.error("Invalid date format. Please use DD/MM/YYYY.")
        }
    }

    fun validateNote(note: String?, maxChars: Int = 500): ValidationResult {
        val trimmed = note?.trim() ?: ""
        return if (trimmed.length > maxChars)
            ValidationResult.error("Note exceeds maximum $maxChars characters (current: ${trimmed.length}).")
        else ValidationResult.OK
    }

    // ─── GEOFENCE VALIDATIONS ─────────────────────────────────────────────────

    fun validateZoneName(name: String?): ValidationResult {
        val trimmed = name?.trim() ?: ""
        return when {
            trimmed.isEmpty() -> ValidationResult.error("Zone name is required.")
            trimmed.length < 2 -> ValidationResult.error("Zone name must be at least 2 characters.")
            trimmed.length > 50 -> ValidationResult.error("Zone name is too long (max 50 characters).")
            else -> ValidationResult.OK
        }
    }

    fun validateGeofenceRadius(radiusMeters: Int): ValidationResult {
        return when {
            radiusMeters < 50 -> ValidationResult.error("Radius must be at least 50 meters.")
            radiusMeters > 50000 -> ValidationResult.error("Radius cannot exceed 50 km (50,000 meters).")
            else -> ValidationResult.OK
        }
    }

    fun validateGeofenceRadius(radiusMeters: Double): ValidationResult =
        validateGeofenceRadius(radiusMeters.toInt())

    fun validateMapAnchor(lat: Double, lng: Double): ValidationResult {
        return when {
            lat == 0.0 && lng == 0.0 ->
                ValidationResult.error("Please tap the map to place a zone anchor point first.")
            lat < -90.0 || lat > 90.0 ->
                ValidationResult.error("Invalid latitude value. Please re-select a map location.")
            lng < -180.0 || lng > 180.0 ->
                ValidationResult.error("Invalid longitude value. Please re-select a map location.")
            else -> ValidationResult.OK
        }
    }

    // ─── PAIRING VALIDATIONS ──────────────────────────────────────────────────

    fun validatePairingCode(code: String?): ValidationResult {
        val trimmed = code?.trim()?.uppercase() ?: ""
        return when {
            trimmed.isEmpty() -> ValidationResult.error("Pairing code is required.")
            trimmed.length != 8 -> ValidationResult.error("Pairing code must be exactly 8 characters.")
            !trimmed.matches(Regex("^[A-Z0-9]+\$")) ->
                ValidationResult.error("Pairing code must contain only letters and numbers.")
            else -> ValidationResult.OK
        }
    }

    // ─── GENERIC VALIDATORS ───────────────────────────────────────────────────

    /**
     * Returns the first failing ValidationResult from a list,
     * or ValidationResult.OK if all pass.
     * Usage: val result = ValidationEngine.validateAll(nameResult, emailResult, phoneResult)
     */
    fun validateAll(vararg results: ValidationResult): ValidationResult {
        return results.firstOrNull { !it.isValid } ?: ValidationResult.OK
    }

    /**
     * Validates that a text field is not blank.
     */
    fun validateRequired(value: String?, fieldName: String): ValidationResult {
        return if (value.isNullOrBlank())
            ValidationResult.error("$fieldName is required.")
        else ValidationResult.OK
    }
}
