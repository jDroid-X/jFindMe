package com.jdroidx.jfindme.engine.avatar

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore

/**
 * 🎨 AvatarSourceSelector & ImageIntentLauncher: OOPS MVC Intent Dispatches for Avatar Sourcing.
 * Manages Camera capture vs Photo Gallery picking with runtime permission awareness.
 *
 * [Variables]: activity, outputUri, intent
 * [Constants]: REQ_PICK_IMAGE, REQ_CAPTURE_IMAGE
 * [Dependencies]: android.app.Activity, android.content.Intent, android.provider.MediaStore
 * [Integration Points]: ProfileActivity.java, AvatarEditorActivity.java, AvatarCropActivity.java
 */
object AvatarSourceSelector {

    const val REQ_PICK_IMAGE = 2001
    const val REQ_CAPTURE_IMAGE = 2002

    fun launchGalleryPicker(activity: Activity) {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        activity.startActivityForResult(intent, REQ_PICK_IMAGE)
    }

    fun launchCameraCapture(activity: Activity, outputUri: Uri? = null) {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (outputUri != null) {
            intent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri)
        }
        activity.startActivityForResult(intent, REQ_CAPTURE_IMAGE)
    }
}

/**
 * ImageIntentLauncher helper for uniform call-by-reference.
 */
object ImageIntentLauncher {
    fun openGallery(activity: Activity) = AvatarSourceSelector.launchGalleryPicker(activity)
    fun openCamera(activity: Activity, uri: Uri? = null) = AvatarSourceSelector.launchCameraCapture(activity, uri)
}

