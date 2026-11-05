package com.jdroidx.jfindme.utils

import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText

/**
 * FormUtils — Centralized Form Helpers.
 *
 * AUDIT FIX [AUD-019]: Ensures programmatic tab sequencing for dynamically
 * generated form fields. In Android, dynamically created EditTexts lack IDs by default,
 * breaking keyboard "Next" navigation.
 */
object FormUtils {

    /**
     * Links an array of EditTexts into a sequential tab chain.
     * 1. Assigns View.generateViewId() if the field lacks an ID.
     * 2. Sets IME_ACTION_NEXT and nextFocusDownId for all but the last field.
     * 3. Sets IME_ACTION_DONE for the final field.
     *
     * @param fields Vararg of EditText fields in logical sequential order.
     */
    @JvmStatic
    fun linkTabSequence(vararg fields: EditText) {
        if (fields.isEmpty()) return

        // First pass: Ensure every field has a unique ID
        for (field in fields) {
            if (field.id == View.NO_ID) {
                field.id = View.generateViewId()
            }
        }

        // Second pass: Wire the sequence
        for (i in fields.indices) {
            val field = fields[i]
            
            // Retain original input type, just alter the IME options
            val currentIme = field.imeOptions
            // Strip out action mask
            val cleanIme = currentIme and EditorInfo.IME_MASK_ACTION.inv()

            if (i < fields.size - 1) {
                val nextField = fields[i + 1]
                field.imeOptions = cleanIme or EditorInfo.IME_ACTION_NEXT
                field.nextFocusDownId = nextField.id
                field.nextFocusForwardId = nextField.id
                field.nextFocusRightId = nextField.id
            } else {
                field.imeOptions = cleanIme or EditorInfo.IME_ACTION_DONE
            }
        }
    }
}
