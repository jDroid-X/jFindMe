package com.jdroidx.jbutterfly.utils

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.jdroidx.jbutterfly.R

object AvatarLoader {

    private val AVATAR_LIST: List<String> by lazy {
        val list = mutableListOf<String>()
        for (i in 1..50) {
            list.add("avatar_custom_$i")
        }
        list.add("avatar_epic")
        list.add("avatar_legendary")
        list.add("avatar_rare")
        list
    }

    @JvmStatic
    fun getAllAvatars(): List<String> = AVATAR_LIST

    @JvmStatic
    fun getAvatarResId(context: Context, avatarName: String?): Int {
        if (avatarName.isNullOrBlank()) return R.drawable.avatar_custom_1
        val resId = context.resources.getIdentifier(avatarName, "drawable", context.packageName)
        return if (resId != 0) resId else R.drawable.avatar_custom_1
    }

    @JvmStatic
    fun loadAvatar(context: Context, imageView: ImageView, avatarName: String?) {
        val resId = getAvatarResId(context, avatarName)
        imageView.setImageResource(resId)
    }

    @JvmStatic
    fun getCircularBitmap(bitmap: Bitmap): Bitmap {
        val size = Math.min(bitmap.width, bitmap.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            isDither = true
        }

        val rect = Rect(0, 0, size, size)
        val rectF = RectF(rect)

        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawOval(rectF, paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)

        return output
    }
}
