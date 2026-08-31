package com.jdroidx.jfindme.utils

/**
 * FirebaseErrorMapper — User-Friendly Firebase Authentication Error Messages.
 *
 * AUDIT FIX [AUD-010]: Raw Firebase exception text was exposed directly to users.
 * This mapper converts Firebase error codes into clear, actionable, user-friendly messages.
 *
 * Architecture: utils layer — no Android context, no Firebase SDK dependency.
 * All Firebase error codes documented at:
 * https://firebase.google.com/docs/auth/admin/errors
 */
object FirebaseErrorMapper {

    private const val TAG = "FirebaseErrorMapper"

    /**
     * Maps a Firebase Auth error code to a user-friendly display string.
     * @param errorCode  The Firebase Auth error code (e.g. "ERROR_WRONG_PASSWORD")
     * @param rawMessage Fallback raw message if the code is unrecognized
     * @return           A clean, actionable error string suitable for UI display
     */
    fun mapAuthError(errorCode: String?, rawMessage: String? = null): String {
        return when (errorCode) {
            "ERROR_INVALID_CREDENTIAL",
            "ERROR_INVALID_EMAIL_AND_PASSWORD" ->
                "Invalid email or password. Please check your details and try again."

            "ERROR_USER_NOT_FOUND" ->
                "No account found for this email. Please register first."

            "ERROR_WRONG_PASSWORD" ->
                "Incorrect password. Tap 'Forgot Password' below to reset it."

            "ERROR_USER_DISABLED" ->
                "This account has been suspended. Please contact support."

            "ERROR_TOO_MANY_REQUESTS" ->
                "Too many failed attempts. Please wait a few minutes before trying again."

            "ERROR_NETWORK_REQUEST_FAILED" ->
                "No internet connection detected. Please check your network and retry."

            "ERROR_EMAIL_ALREADY_IN_USE" ->
                "This email is already registered. Please log in or use a different email."

            "ERROR_WEAK_PASSWORD" ->
                "Password must be at least 6 characters. Please choose a stronger password."

            "ERROR_INVALID_EMAIL" ->
                "Please enter a valid email address (e.g. user@example.com)."

            "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" ->
                "An account already exists with a different sign-in method for this email."

            "ERROR_CREDENTIAL_ALREADY_IN_USE" ->
                "This credential is already linked to another account."

            "ERROR_REQUIRES_RECENT_LOGIN" ->
                "This action requires recent authentication. Please log out and log back in."

            "ERROR_OPERATION_NOT_ALLOWED" ->
                "This sign-in method is not enabled. Please contact your administrator."

            "ERROR_USER_TOKEN_EXPIRED" ->
                "Your session has expired. Please log in again."

            "ERROR_MISSING_EMAIL" ->
                "Please enter your email address to continue."

            "ERROR_MISSING_PASSWORD" ->
                "Please enter your password to continue."

            else -> {
                // Log the unrecognized code for forensic tracing without exposing it to user
                SystemLogger.w(TAG, "Unmapped Firebase error code: $errorCode | msg: $rawMessage")
                "Something went wrong. Please try again or contact support if the issue persists."
            }
        }
    }

    /**
     * Maps a Firebase Realtime Database / Firestore error to a user-friendly string.
     */
    fun mapDatabaseError(code: String?, rawMessage: String? = null): String {
        return when (code) {
            "PERMISSION_DENIED" ->
                "Access denied. You don't have permission to perform this action."

            "NETWORK_ERROR" ->
                "Database sync failed. Please check your internet connection."

            "DISCONNECTED" ->
                "Connection lost. Your data is saved locally and will sync when reconnected."

            "EXPIRED_TOKEN" ->
                "Your session has expired. Please log in again."

            "WRITE_CANCELLED" ->
                "The save operation was cancelled. Please try again."

            "MAX_RETRIES" ->
                "Sync failed after multiple attempts. Data is queued and will retry automatically."

            else -> {
                SystemLogger.w(TAG, "Unmapped DB error: $code | msg: $rawMessage")
                "Data operation failed. Please try again."
            }
        }
    }

    /**
     * Maps network/connectivity states to user-visible messages.
     */
    fun mapNetworkError(isOffline: Boolean): String {
        return if (isOffline)
            "You are offline. Changes are saved locally and will sync when connected."
        else
            "Network error. Please check your connection and try again."
    }
}
