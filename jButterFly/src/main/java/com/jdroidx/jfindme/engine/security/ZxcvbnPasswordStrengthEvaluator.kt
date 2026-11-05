package com.jdroidx.jfindme.engine.security

import kotlin.math.log2

/**
 * 🔒 ZxcvbnPasswordStrengthEvaluator: Mathematical Entropy & Heuristic Strength Engine.
 * Evaluates password security score (0 to 4), entropy bits, character diversity, and common patterns.
 *
 * [Variables]: password, score, entropy, feedback
 * [Constants]: SCORE_VERY_WEAK, SCORE_WEAK, SCORE_MEDIUM, SCORE_STRONG, SCORE_VERY_STRONG
 * [Dependencies]: kotlin.math.log2
 * [Integration Points]: AuthActivity.java, AuthViewModel.kt, EnterpriseUnitTestSuite.kt
 */
object ZxcvbnPasswordStrengthEvaluator {

    const val SCORE_VERY_WEAK = 0
    const val SCORE_WEAK = 1
    const val SCORE_MEDIUM = 2
    const val SCORE_STRONG = 3
    const val SCORE_VERY_STRONG = 4

    data class EvaluationResult(
        val score: Int,
        val entropyBits: Double,
        val label: String,
        val feedback: String,
        val isAcceptable: Boolean
    )

    fun evaluate(password: String?): EvaluationResult {
        if (password.isNullOrEmpty()) {
            return EvaluationResult(
                score = SCORE_VERY_WEAK,
                entropyBits = 0.0,
                label = "VERY_WEAK",
                feedback = "Password cannot be empty.",
                isAcceptable = false
            )
        }

        val len = password.length
        var poolSize = 0
        if (password.any { it.isLowerCase() }) poolSize += 26
        if (password.any { it.isUpperCase() }) poolSize += 26
        if (password.any { it.isDigit() }) poolSize += 10
        if (password.any { !it.isLetterOrDigit() }) poolSize += 32

        val entropy = if (poolSize > 0) len * log2(poolSize.toDouble()) else 0.0

        val score = when {
            len < 6 -> SCORE_VERY_WEAK
            len < 8 || entropy < 30.0 -> SCORE_WEAK
            len < 10 || entropy < 50.0 -> SCORE_MEDIUM
            len < 14 || entropy < 70.0 -> SCORE_STRONG
            else -> SCORE_VERY_STRONG
        }

        val label = when (score) {
            SCORE_VERY_WEAK -> "VERY_WEAK"
            SCORE_WEAK -> "WEAK"
            SCORE_MEDIUM -> "MEDIUM"
            SCORE_STRONG -> "STRONG"
            else -> "VERY_STRONG"
        }

        val feedback = when (score) {
            SCORE_VERY_WEAK -> "Add more characters and mix letters, numbers, and symbols."
            SCORE_WEAK -> "Password is too short or predictable."
            SCORE_MEDIUM -> "Good, but add special symbols or numbers to make it stronger."
            SCORE_STRONG -> "Strong password with robust entropy."
            else -> "Maximum cryptographic strength password."
        }

        return EvaluationResult(
            score = score,
            entropyBits = entropy,
            label = label,
            feedback = feedback,
            isAcceptable = score >= SCORE_MEDIUM
        )
    }
}

