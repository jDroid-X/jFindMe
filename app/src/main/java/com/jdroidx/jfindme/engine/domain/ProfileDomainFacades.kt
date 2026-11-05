package com.jdroidx.jfindme.engine.domain

import android.content.Context
import com.jdroidx.jfindme.utils.DeviceIdentityHelper

/**
 * 🏛️ ProfileDomainFacades: OOPS Domain Handlers for Screen 03 (ProfileActivity).
 * [Variables]: context, name, phone, gender, state, pincode
 * [Constants]: REGEX_PINCODE, REGEX_PHONE
 * [Dependencies]: Context, DeviceIdentityHelper
 * [Integration Points]: ProfileActivity.java, EnterpriseUnitTestSuite.kt
 */
object NameFormatValidator {
    fun isValid(name: String?): Boolean = !name.isNullOrBlank() && name.trim().length >= 2
}

object PhoneNumberUtil {
    fun isValidNumber(phone: String?): Boolean {
        if (phone.isNullOrBlank()) return false
        val digits = phone.filter { it.isDigit() }
        return digits.length in 10..15
    }
}

object GenderEnumMapper {
    fun toDisplayString(genderCode: String?): String = when (genderCode?.uppercase()) {
        "M", "MALE" -> "Male"
        "F", "FEMALE" -> "Female"
        "O", "OTHER" -> "Other"
        else -> "Prefer not to say"
    }
}

object GeographicLookupRepository {
    fun getCitiesForState(context: Context, state: String): List<String> = emptyList()
}

object PincodeValidator {
    fun isValid(pincode: String?): Boolean {
        if (pincode.isNullOrBlank()) return false
        val digits = pincode.trim()
        return digits.length == 6 && digits.all { it.isDigit() }
    }
}

