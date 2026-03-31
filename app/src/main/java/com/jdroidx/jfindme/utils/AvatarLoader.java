package com.jdroidx.jfindme.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

public class AvatarLoader {

    private static final android.util.LruCache<String, Bitmap> bitmapCache = new android.util.LruCache<>(30);

    public static int getResolvedAvatarIndex(int id) {
        if (id == -1) return -1;
        return (id % 22 == 0) ? 22 : (Math.abs(id) % 22);
    }

    // 🚀 QA Fix: Centralized Source of Truth for Local Custom Graphic Avatars
    public static void bindCurrentUserAvatar(Context context, ImageView targetView) {
        int currentId = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
                               .getInt("equipped_avatar_id", 1);
        
        java.io.File customPhotoFile = new java.io.File(context.getFilesDir() + "/avatars", "custom_photo.png");
        if (currentId == -1 && customPhotoFile.exists()) {
            String cacheKey = "local_custom_photo";
            Bitmap cached = bitmapCache.get(cacheKey);
            if (cached != null) {
                targetView.setImageBitmap(cached);
                return;
            }
            Bitmap b = BitmapFactory.decodeFile(customPhotoFile.getAbsolutePath());
            if (b != null) {
                bitmapCache.put(cacheKey, b);
                targetView.setImageBitmap(b);
                return;
            }
        }
        
        int dynIdx = getResolvedAvatarIndex(currentId);
        int dynRes = context.getResources().getIdentifier("avatar_custom_" + dynIdx, "drawable", context.getPackageName());
        targetView.setImageResource(dynRes != 0 ? dynRes : android.R.drawable.ic_menu_myplaces);
    }
    
    // Core Overload: Returns a physical Bitmap Object correctly bound (used by Markers & Callouts)
    public static Bitmap getCurrentUserAvatarBitmap(Context context, int targetSize) {
        int currentId = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
                               .getInt("equipped_avatar_id", 1);
        String cacheKey = "ME_ID_" + currentId + "_S_" + targetSize;
        Bitmap cached = bitmapCache.get(cacheKey);
        if (cached != null) return cached;

        java.io.File customPhotoFile = new java.io.File(context.getFilesDir() + "/avatars", "custom_photo.png");
        Bitmap result;
        if (currentId == -1 && customPhotoFile.exists()) {
            Bitmap raw = BitmapFactory.decodeFile(customPhotoFile.getAbsolutePath());
            if (raw != null) {
                result = Bitmap.createScaledBitmap(raw, targetSize, targetSize, true);
                bitmapCache.put(cacheKey, result);
                return result;
            }
        }
        
        int dynIdx = getResolvedAvatarIndex(currentId);
        int resId = context.getResources().getIdentifier("avatar_custom_" + dynIdx, "drawable", context.getPackageName());
        if (resId == 0) resId = context.getResources().getIdentifier("avatar_custom_1", "drawable", context.getPackageName());
        
        Bitmap raw = BitmapFactory.decodeResource(context.getResources(), resId);
        if (raw == null) {
            result = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888);
            new android.graphics.Canvas(result).drawColor(android.graphics.Color.LTGRAY);
        } else {
            result = Bitmap.createScaledBitmap(raw, targetSize, targetSize, true);
        }
        bitmapCache.put(cacheKey, result);
        return result;
    }
    
    // Binds avatars for REMOTE Network Targets dynamically 
    public static Bitmap getRemoteAvatarBitmap(Context context, String avatarIdStr, int targetSize) {
        String cacheKey = "REMOTE_" + avatarIdStr + "_S_" + targetSize;
        Bitmap cached = bitmapCache.get(cacheKey);
        if (cached != null) return cached;

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
        
        Bitmap result;
        Bitmap raw = BitmapFactory.decodeResource(context.getResources(), resId);
        if (raw == null) {
            result = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888);
            new android.graphics.Canvas(result).drawColor(android.graphics.Color.LTGRAY);
        } else {
            result = Bitmap.createScaledBitmap(raw, targetSize, targetSize, true);
        }
        bitmapCache.put(cacheKey, result);
        return result;
    }

    public static void clearCache() {
        if (bitmapCache != null) bitmapCache.evictAll();
    }
}
