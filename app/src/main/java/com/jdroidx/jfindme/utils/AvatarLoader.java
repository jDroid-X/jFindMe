package com.jdroidx.jfindme.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

public class AvatarLoader {

    // 🚀 QA Fix: Centralized Source of Truth for Local Custom Graphic Avatars
    public static void bindCurrentUserAvatar(Context context, ImageView targetView) {
        int currentId = context.getSharedPreferences("jFindMe_Prefs", Context.MODE_PRIVATE)
                               .getInt("equipped_avatar_id", 1);
        
        java.io.File customPhotoFile = new java.io.File(context.getFilesDir() + "/avatars", "custom_photo.png");
        if (currentId == -1 && customPhotoFile.exists()) {
            Bitmap b = BitmapFactory.decodeFile(customPhotoFile.getAbsolutePath());
            if (b != null) {
                targetView.setImageBitmap(b);
                return;
            }
        }
        
        int dynIdx = (currentId % 22 == 0) ? 22 : (Math.abs(currentId) % 22);
        int dynRes = context.getResources().getIdentifier("avatar_custom_" + dynIdx, "drawable", context.getPackageName());
        targetView.setImageResource(dynRes != 0 ? dynRes : android.R.drawable.ic_menu_myplaces);
    }
    
    // Core Overload: Returns a physical Bitmap Object correctly bound (used by Markers & Callouts)
    public static Bitmap getCurrentUserAvatarBitmap(Context context, int targetSize) {
        int currentId = context.getSharedPreferences("jFindMe_Prefs", Context.MODE_PRIVATE)
                               .getInt("equipped_avatar_id", 1);
        
        java.io.File customPhotoFile = new java.io.File(context.getFilesDir() + "/avatars", "custom_photo.png");
        if (currentId == -1 && customPhotoFile.exists()) {
            Bitmap raw = BitmapFactory.decodeFile(customPhotoFile.getAbsolutePath());
            if (raw != null) return Bitmap.createScaledBitmap(raw, targetSize, targetSize, true);
        }
        
        int dynIdx = (currentId % 22 == 0) ? 22 : (Math.abs(currentId) % 22);
        int resId = context.getResources().getIdentifier("avatar_custom_" + dynIdx, "drawable", context.getPackageName());
        if (resId == 0) resId = context.getResources().getIdentifier("avatar_custom_1", "drawable", context.getPackageName());
        
        Bitmap raw = BitmapFactory.decodeResource(context.getResources(), resId);
        if (raw == null) {
            Bitmap empty = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888);
            new android.graphics.Canvas(empty).drawColor(android.graphics.Color.LTGRAY);
            return empty;
        }
        return Bitmap.createScaledBitmap(raw, targetSize, targetSize, true);
    }
    
    // Binds avatars for REMOTE Network Targets dynamically 
    public static Bitmap getRemoteAvatarBitmap(Context context, String avatarIdStr, int targetSize) {
        int resId = 0;
        
        if (avatarIdStr != null && !avatarIdStr.isEmpty()) {
            boolean isVrm = avatarIdStr.contains(".vrm");
            if (isVrm) {
                int dynamicIdx = Math.abs(avatarIdStr.hashCode()) % 22 + 1;
                resId = context.getResources().getIdentifier("avatar_custom_" + dynamicIdx, "drawable", context.getPackageName());
            } else {
                resId = context.getResources().getIdentifier(avatarIdStr, "drawable", context.getPackageName());
            }
        }
        
        if (resId == 0) resId = context.getResources().getIdentifier("avatar_custom_1", "drawable", context.getPackageName());
        
        Bitmap raw = BitmapFactory.decodeResource(context.getResources(), resId);
        if (raw == null) {
            Bitmap empty = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888);
            new android.graphics.Canvas(empty).drawColor(android.graphics.Color.LTGRAY);
            return empty;
        }
        return Bitmap.createScaledBitmap(raw, targetSize, targetSize, true);
    }
}
