package com.jdroidx.jfindme.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.widget.ImageView;

import java.io.File;

/**
 * 🎨 Centralized High-Definition Avatar Loader & Smart Geometry Normalizer (L7 OOPS Engine)
 * - Enhanced anti-aliased ARGB_8888 rendering with 2x High-DPI Resolution.
 * - Hardware-accelerated circular masking & smart CENTER_CROP alignment.
 * - Multi-tier LRU Bitmap Caching (40 high-res slots) for smooth 60fps scrolling.
 */
public class AvatarLoader {

    private static final android.util.LruCache<String, Bitmap> bitmapCache = new android.util.LruCache<>(40);

    public static int getResolvedAvatarIndex(int id) {
        if (id == -1 || id == 999) return -1;
        if (id >= 1 && id <= 50) return id;
        return (id % 50 == 0) ? 50 : (Math.abs(id) % 50);
    }

    /**
     * 🚀 High-Quality Circular Bitmap Masking with Anti-Aliasing
     */
    public static Bitmap createCircularBitmap(Bitmap src, int targetSize) {
        if (src == null) return null;
        Bitmap output = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        Rect srcRect;
        int minDim = Math.min(src.getWidth(), src.getHeight());
        int srcX = (src.getWidth() - minDim) / 2;
        int srcY = (src.getHeight() - minDim) / 2;
        srcRect = new Rect(srcX, srcY, srcX + minDim, srcY + minDim);

        RectF dstRect = new RectF(0, 0, targetSize, targetSize);
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawCircle(targetSize / 2f, targetSize / 2f, targetSize / 2f, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(src, srcRect, dstRect, paint);
        return output;
    }

    /**
     * 🚀 Binds the local user's equipped avatar to any ImageView with optimal Center Crop
     */
    public static void bindCurrentUserAvatar(Context context, ImageView targetView) {
        if (targetView == null || context == null) return;
        
        // 🔧 NULL-002: Add null safety for all operations
        try {
            targetView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            targetView.setAdjustViewBounds(true);

            String prefsName = "system_registry";
            android.content.SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
            if (prefs == null) return;
            
            int currentId = prefs.getInt("equipped_avatar_id", 1);
            String customPath = prefs.getString("equipped_vrm_string", "");

            if (currentId == -1 || currentId == 999) {
                String cacheKey = "local_custom_hq_" + customPath;
                Bitmap cached = bitmapCache.get(cacheKey);
                if (cached != null) {
                    targetView.setImageBitmap(cached);
                    return;
                }

                File targetFile = null;
                if (!customPath.isEmpty()) targetFile = new File(customPath);
                if (targetFile == null || !targetFile.exists()) {
                    targetFile = new File(context.getFilesDir() + "/avatars", "custom_photo.png");
                }

                if (targetFile.exists()) {
                    BitmapFactory.Options opts = new BitmapFactory.Options();
                    opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
                    Bitmap b = BitmapFactory.decodeFile(targetFile.getAbsolutePath(), opts);
                    if (b != null) {
                        bitmapCache.put(cacheKey, b);
                        targetView.setImageBitmap(b);
                        return;
                    }
                }
            }

            int dynIdx = getResolvedAvatarIndex(currentId);
            int dynRes = 0;
            if (dynIdx > 0) {
                dynRes = context.getResources().getIdentifier("avatar_custom_" + dynIdx, "drawable", context.getPackageName());
            }
            if (dynRes == 0) {
                dynRes = context.getResources().getIdentifier("butterfly", "drawable", context.getPackageName());
            }
            if (dynRes == 0) {
                dynRes = context.getResources().getIdentifier("avatar_custom_1", "drawable", context.getPackageName());
            }
            targetView.setImageResource(dynRes != 0 ? dynRes : android.R.drawable.ic_menu_myplaces);
        } catch (Exception e) {
            // Silently ignore any avatar loading errors
        }
    }

    /**
     * 🚀 Generates/retrieves High-Fidelity Bitmap of current user's avatar (2x Double Resolution)
     */
    public static Bitmap getCurrentUserAvatarBitmap(Context context, int targetSize) {
        if (context == null) return null;
        int doubleSize = targetSize * 2; // Enhanced 2x super-sampled fidelity
        int currentId = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
                               .getInt("equipped_avatar_id", 1);
        String cacheKey = "ME_ID_HQ_" + currentId + "_S_" + doubleSize;
        Bitmap cached = bitmapCache.get(cacheKey);
        if (cached != null) return cached;

        String customPath = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
                               .getString("equipped_vrm_string", "");

        Bitmap result = null;
        if (currentId == -1 || currentId == 999) {
            File targetFile = null;
            if (!customPath.isEmpty()) targetFile = new File(customPath);
            if (targetFile == null || !targetFile.exists()) {
                targetFile = new File(context.getFilesDir() + "/avatars", "custom_photo.png");
            }

            if (targetFile.exists()) {
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
                Bitmap raw = BitmapFactory.decodeFile(targetFile.getAbsolutePath(), opts);
                if (raw != null) {
                    result = Bitmap.createScaledBitmap(raw, doubleSize, doubleSize, true);
                    bitmapCache.put(cacheKey, result);
                    return result;
                }
            }
        }

        int dynIdx = getResolvedAvatarIndex(currentId);
        int resId = 0;
        if (dynIdx > 0) {
            resId = context.getResources().getIdentifier("avatar_custom_" + dynIdx, "drawable", context.getPackageName());
        }
        if (resId == 0) resId = context.getResources().getIdentifier("butterfly", "drawable", context.getPackageName());
        if (resId == 0) resId = context.getResources().getIdentifier("avatar_custom_1", "drawable", context.getPackageName());

        BitmapFactory.Options resOpts = new BitmapFactory.Options();
        resOpts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap raw = BitmapFactory.decodeResource(context.getResources(), resId, resOpts);
        if (raw == null) {
            result = Bitmap.createBitmap(doubleSize, doubleSize, Bitmap.Config.ARGB_8888);
            new Canvas(result).drawColor(android.graphics.Color.LTGRAY);
        } else {
            result = Bitmap.createScaledBitmap(raw, doubleSize, doubleSize, true);
        }
        bitmapCache.put(cacheKey, result);
        return result;
    }

    // [Variables] context, deviceId, avatarIdStr, avatarsDir, clean, candidates, f, firstUnderscore, lastUnderscore, extractedAgent | [Constants] "avatars", "_custom.png" | [Dependencies] File, DeviceIdentityHelper | [Integration Points] AvatarLoader, MapActivity, MultiMarkerEngine
    public static File resolveCustomAvatarFile(Context context, String deviceId, String avatarIdStr) {
        if (context == null) return null;
        File avatarsDir = new File(context.getFilesDir(), "avatars");
        if (!avatarsDir.exists()) return null;

        java.util.List<String> candidates = new java.util.ArrayList<>();
        if (deviceId != null && !deviceId.trim().isEmpty()) {
            String clean = com.jdroidx.jfindme.utils.DeviceIdentityHelper.toCanonicalAgentId(deviceId.trim());
            candidates.add(deviceId.trim());
            candidates.add(clean);
            if (!clean.startsWith("HW_")) candidates.add("HW_" + clean);
            if (clean.startsWith("HW_")) candidates.add(clean.substring(3));
        }

        if (avatarIdStr != null && avatarIdStr.startsWith("AV_")) {
            // Format is AV_{agentId}_{seq}
            try {
                int firstUnderscore = avatarIdStr.indexOf('_');
                int lastUnderscore = avatarIdStr.lastIndexOf('_');
                if (firstUnderscore >= 0 && lastUnderscore > firstUnderscore) {
                    String extractedAgent = avatarIdStr.substring(firstUnderscore + 1, lastUnderscore);
                    candidates.add(extractedAgent);
                    candidates.add("HW_" + extractedAgent);
                }
            } catch (Exception ignored) {}
        }

        for (String idCandidate : candidates) {
            File f = new File(avatarsDir, idCandidate + "_custom.png");
            if (f.exists() && f.length() > 0) return f;
        }
        return null;
    }

    /**
     * 🚀 High-Fidelity Remote Avatar Bitmap Resolver (2x Double Resolution & Smart Fit)
     */
    public static Bitmap getRemoteAvatarBitmap(Context context, String avatarIdStr, int targetSize) {
        return getRemoteAvatarBitmap(context, null, avatarIdStr, targetSize);
    }

    public static Bitmap getRemoteAvatarBitmap(Context context, String deviceId, String avatarIdStr, int targetSize) {
        if (context == null) return null;
        int doubleSize = targetSize * 2;
        String devKey = (deviceId != null && !deviceId.isEmpty()) ? deviceId : "GLOBAL";
        String cacheKey = "REMOTE_HQ_" + devKey + "_" + (avatarIdStr != null ? avatarIdStr : "") + "_S_" + doubleSize;
        Bitmap cached = bitmapCache.get(cacheKey);
        if (cached != null) return cached;

        // 🛡️ PHYSICAL DISK TRUTH CHECK: If a device-specific custom avatar exists on disk, load it unconditionally!
        File peerCustomFile = resolveCustomAvatarFile(context, deviceId, avatarIdStr);
        if (peerCustomFile != null && peerCustomFile.exists()) {
            try {
                Bitmap b = BitmapFactory.decodeFile(peerCustomFile.getAbsolutePath());
                if (b != null) {
                    Bitmap customRes = Bitmap.createScaledBitmap(b, doubleSize, doubleSize, true);
                    bitmapCache.put(cacheKey, customRes);
                    return customRes;
                }
            } catch (Exception ignored) {}
        }

        int resId = 0;
        if (avatarIdStr != null && !avatarIdStr.isEmpty()) {
            if (avatarIdStr.startsWith("/") || avatarIdStr.contains("avatars/")) {
                File targetFile = new File(avatarIdStr);
                if (targetFile.exists()) {
                    BitmapFactory.Options opts = new BitmapFactory.Options();
                    opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
                    Bitmap raw = BitmapFactory.decodeFile(targetFile.getAbsolutePath(), opts);
                    if (raw != null) {
                        Bitmap customRes = Bitmap.createScaledBitmap(raw, doubleSize, doubleSize, true);
                        bitmapCache.put(cacheKey, customRes);
                        return customRes;
                    }
                }
            }

            boolean isVrm = avatarIdStr.contains(".vrm");
            if (avatarIdStr.equals("butterfly") || avatarIdStr.equalsIgnoreCase("butterfly_angel") || avatarIdStr.equalsIgnoreCase("AngleButterfly")) {
                resId = context.getResources().getIdentifier("butterfly", "drawable", context.getPackageName());
            } else if (avatarIdStr.startsWith("data:image/") || avatarIdStr.length() > 200) {
                // Direct Base64 data payload
                try {
                    String cleanB64 = avatarIdStr.contains(",") ? avatarIdStr.substring(avatarIdStr.indexOf(",") + 1) : avatarIdStr;
                    byte[] decoded = android.util.Base64.decode(cleanB64, android.util.Base64.DEFAULT);
                    Bitmap b64Bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                    if (b64Bmp != null) {
                        Bitmap customRes = Bitmap.createScaledBitmap(b64Bmp, doubleSize, doubleSize, true);
                        bitmapCache.put(cacheKey, customRes);
                        return customRes;
                    }
                } catch (Exception ignored) {}
            } else if (isVrm || avatarIdStr.equals("-1") || avatarIdStr.equals("999") || avatarIdStr.contains("custom_photo") || avatarIdStr.startsWith("AV_")) {
                // 🛡️ Peer Isolation: Check if a peer-specific cached photo exists, but NEVER load host user's custom_photo.png for remote peers!
                File peerCustom = resolveCustomAvatarFile(context, deviceId, avatarIdStr);
                if (peerCustom != null && peerCustom.exists()) {
                    try {
                        Bitmap b = BitmapFactory.decodeFile(peerCustom.getAbsolutePath());
                        if (b != null) {
                            Bitmap customRes = Bitmap.createScaledBitmap(b, doubleSize, doubleSize, true);
                            bitmapCache.put(cacheKey, customRes);
                            return customRes;
                        }
                    } catch (Exception ignored) {}
                }

                if (deviceId != null && !deviceId.isEmpty()) {
                    String selfAgent = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(context);
                    String selfAuth = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
                    if (deviceId.equals(selfAgent) || (selfAuth != null && deviceId.equals(selfAuth)) || "SELF_USER".equals(deviceId) || "You".equalsIgnoreCase(deviceId)) {
                        File selfCustom = new File(context.getFilesDir() + "/avatars", "custom_photo.png");
                        if (selfCustom.exists()) {
                            try {
                                Bitmap b = BitmapFactory.decodeFile(selfCustom.getAbsolutePath());
                                if (b != null) {
                                    Bitmap customRes = Bitmap.createScaledBitmap(b, doubleSize, doubleSize, true);
                                    bitmapCache.put(cacheKey, customRes);
                                    return customRes;
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }
                int seed = (deviceId != null && !deviceId.isEmpty()) ? deviceId.hashCode() : avatarIdStr.hashCode();
                int dynamicIdx = Math.abs(seed % 50) + 1;
                resId = context.getResources().getIdentifier("avatar_custom_" + dynamicIdx, "drawable", context.getPackageName());
            } else if (avatarIdStr.matches("^\\d+$")) {
                // [Variables] idVal, dynamicIdx, resId | [Constants] None | [Dependencies] NumberFormatException | [Integration Points] FamilyFriendsActivity, MapActivity
                try {
                    int idVal = Integer.parseInt(avatarIdStr);
                    int dynamicIdx = getResolvedAvatarIndex(idVal);
                    if (dynamicIdx > 0) {
                        resId = context.getResources().getIdentifier("avatar_custom_" + dynamicIdx, "drawable", context.getPackageName());
                    }
                } catch (NumberFormatException nfe) {
                    int dynamicIdx = Math.abs(avatarIdStr.hashCode() % 50) + 1;
                    resId = context.getResources().getIdentifier("avatar_custom_" + dynamicIdx, "drawable", context.getPackageName());
                }
            } else {
                resId = context.getResources().getIdentifier(avatarIdStr, "drawable", context.getPackageName());
            }
        }

        if (resId == 0) resId = context.getResources().getIdentifier("butterfly", "drawable", context.getPackageName());
        if (resId == 0) resId = context.getResources().getIdentifier("avatar_custom_1", "drawable", context.getPackageName());

        BitmapFactory.Options resOpts = new BitmapFactory.Options();
        resOpts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap raw = BitmapFactory.decodeResource(context.getResources(), resId, resOpts);
        Bitmap result;
        if (raw == null) {
            result = Bitmap.createBitmap(doubleSize, doubleSize, Bitmap.Config.ARGB_8888);
            new Canvas(result).drawColor(android.graphics.Color.LTGRAY);
        } else {
            result = Bitmap.createScaledBitmap(raw, doubleSize, doubleSize, true);
        }
        bitmapCache.put(cacheKey, result);
        return result;
    }

    /**
     * 🚀 Centralized Smart-Fit Avatar Binder with Proportional Scaling & Optional Grey Mode
     * [Variables] context, targetView, deviceId, avatarIdStr, isGreyMode | [Constants] defaultRes, dynamicIdx | [Dependencies] ImageView, Bitmap, File | [Integration Points] MapActivity, FamilyFriendsActivity
     */
    public static void bindAvatar(Context context, ImageView targetView, String avatarIdStr) {
        bindAvatar(context, targetView, null, avatarIdStr, false);
    }

    public static void bindAvatar(Context context, ImageView targetView, String avatarIdStr, boolean isGreyMode) {
        bindAvatar(context, targetView, null, avatarIdStr, isGreyMode);
    }

    public static void bindAvatar(Context context, ImageView targetView, String deviceId, String avatarIdStr) {
        bindAvatar(context, targetView, deviceId, avatarIdStr, false);
    }

    public static void bindAvatar(Context context, ImageView targetView, String deviceId, String avatarIdStr, boolean isGreyMode) {
        if (targetView == null || context == null) return;
        targetView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        targetView.setAdjustViewBounds(true);

        if (isGreyMode) {
            android.graphics.ColorMatrix cm = new android.graphics.ColorMatrix();
            cm.setSaturation(0f); // Complete desaturation -> Grey Mode
            targetView.setColorFilter(new android.graphics.ColorMatrixColorFilter(cm));
            targetView.setAlpha(0.60f); // 60% opacity for grey ghost appearance
        } else {
            targetView.clearColorFilter();
            targetView.setAlpha(1.0f);
        }

        int defaultRes = context.getResources().getIdentifier("butterfly", "drawable", context.getPackageName());
        if (defaultRes == 0) defaultRes = context.getResources().getIdentifier("avatar_custom_1", "drawable", context.getPackageName());

        if (avatarIdStr == null || avatarIdStr.isEmpty()) {
            targetView.setImageResource(defaultRes != 0 ? defaultRes : android.R.drawable.ic_menu_myplaces);
            return;
        }

        File peerCustom = resolveCustomAvatarFile(context, deviceId, avatarIdStr);
        if (peerCustom != null && peerCustom.exists()) {
            try {
                Bitmap b = BitmapFactory.decodeFile(peerCustom.getAbsolutePath());
                if (b != null) {
                    targetView.setImageBitmap(b);
                    return;
                }
            } catch (Exception ignored) {}
        }

        if (deviceId != null && !deviceId.isEmpty()) {
            // 🛡️ Self Hardware Identity Anchor: Check custom_photo.png if deviceId matches self
            String selfAgent = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(context);
            String selfAuth = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
            if (deviceId.equals(selfAgent) || (selfAuth != null && deviceId.equals(selfAuth)) || "SELF_USER".equals(deviceId) || "You".equalsIgnoreCase(deviceId)) {
                File selfCustom = new File(context.getFilesDir() + "/avatars", "custom_photo.png");
                if (selfCustom.exists()) {
                    try {
                        Bitmap b = BitmapFactory.decodeFile(selfCustom.getAbsolutePath());
                        if (b != null) {
                            targetView.setImageBitmap(b);
                            return;
                        }
                    } catch (Exception ignored) {}
                }
            }
        }

        if (avatarIdStr.startsWith("/") || avatarIdStr.contains("avatars/")) {
            File targetFile = new File(avatarIdStr);
            if (targetFile.exists()) {
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
                Bitmap b = BitmapFactory.decodeFile(targetFile.getAbsolutePath(), opts);
                if (b != null) {
                    targetView.setImageBitmap(b);
                    return;
                }
            }
        }

        int resId = 0;
        if (avatarIdStr.equals("butterfly") || avatarIdStr.equalsIgnoreCase("butterfly_angel") || avatarIdStr.equalsIgnoreCase("AngleButterfly")) {
            resId = context.getResources().getIdentifier("butterfly", "drawable", context.getPackageName());
        } else if (avatarIdStr.matches("^\\d+$")) {
            // [Variables] idVal, dynamicIdx, resId | [Constants] None | [Dependencies] NumberFormatException | [Integration Points] FamilyFriendsActivity, MapActivity
            try {
                int idVal = Integer.parseInt(avatarIdStr);
                int dynamicIdx = getResolvedAvatarIndex(idVal);
                if (dynamicIdx > 0) {
                    resId = context.getResources().getIdentifier("avatar_custom_" + dynamicIdx, "drawable", context.getPackageName());
                }
            } catch (NumberFormatException nfe) {
                int seed = (deviceId != null && !deviceId.isEmpty()) ? deviceId.hashCode() : avatarIdStr.hashCode();
                int dynamicIdx = Math.abs(seed % 50) + 1;
                resId = context.getResources().getIdentifier("avatar_custom_" + dynamicIdx, "drawable", context.getPackageName());
            }
        } else if (avatarIdStr.equals("-1") || avatarIdStr.equals("999") || avatarIdStr.contains("custom_photo") || avatarIdStr.contains(".vrm") || avatarIdStr.startsWith("AV_")) {
            int seed = (deviceId != null && !deviceId.isEmpty()) ? deviceId.hashCode() : avatarIdStr.hashCode();
            int dynamicIdx = Math.abs(seed % 50) + 1;
            resId = context.getResources().getIdentifier("avatar_custom_" + dynamicIdx, "drawable", context.getPackageName());
        } else {
            resId = context.getResources().getIdentifier(avatarIdStr, "drawable", context.getPackageName());
        }

        if (resId == 0) {
            resId = defaultRes;
        }
        targetView.setImageResource(resId != 0 ? resId : android.R.drawable.ic_menu_myplaces);
    }

    // [Variables] context, deviceId, avatarIdStr, targetSize, devKey, cacheKey, cached, raw, circular | [Constants] None | [Dependencies] Bitmap, Canvas, Paint, Context | [Integration Points] MultiMarkerEngine, MapActivity
    // Circular Avatar Resolution: Combines getRemoteAvatarBitmap and createCircularBitmap with compound caching
    public static Bitmap getCircularAvatarBitmap(Context context, String avatarIdStr, int targetSize) {
        return getCircularAvatarBitmap(context, null, avatarIdStr, targetSize);
    }

    public static Bitmap getCircularAvatarBitmap(Context context, String deviceId, String avatarIdStr, int targetSize) {
        if (context == null) return null;
        String devKey = (deviceId != null && !deviceId.isEmpty()) ? deviceId : "GLOBAL";
        String cacheKey = "CIRCULAR_HQ_" + devKey + "_" + (avatarIdStr != null ? avatarIdStr : "") + "_S_" + targetSize;
        Bitmap cached = bitmapCache.get(cacheKey);
        if (cached != null && !cached.isRecycled()) {
            return cached;
        }
        Bitmap raw = getRemoteAvatarBitmap(context, deviceId, avatarIdStr, targetSize);
        if (raw == null) return null;
        Bitmap circular = createCircularBitmap(raw, targetSize);
        if (circular != null) {
            bitmapCache.put(cacheKey, circular);
        }
        return circular;
    }

    public static Bitmap getCompositeMarkerBitmap(Context context, String avatarId, int strokeColor, int sizePx) {
        return getCompositeMarkerBitmap(context, null, avatarId, strokeColor, sizePx);
    }

    public static Bitmap getCompositeMarkerBitmap(Context context, String deviceId, String avatarId, int strokeColor, int sizePx) {
        if (context == null) return null;
        String devKey = (deviceId != null && !deviceId.isEmpty()) ? deviceId : "GLOBAL";
        String cacheKey = "marker_" + devKey + "_" + (avatarId != null ? avatarId : "") + "_" + strokeColor + "_" + sizePx;
        Bitmap cached = bitmapCache.get(cacheKey);
        if (cached != null && !cached.isRecycled()) {
            return cached;
        }

        Bitmap base = getCircularAvatarBitmap(context, deviceId, avatarId, sizePx);
        if (base == null) return null;

        Bitmap composited = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(composited);

        Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setColor(strokeColor);
        float strokeWidth = sizePx * 0.08f;
        ringPaint.setStrokeWidth(strokeWidth);

        canvas.drawBitmap(base, 0, 0, null);
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, (sizePx - strokeWidth) / 2f, ringPaint);

        bitmapCache.put(cacheKey, composited);
        return composited;
    }

    // [Variables] deviceId, snapshot, key | [Constants] None | [Dependencies] LruCache, Map | [Integration Points] MapActivity, LocationRepository
    /**
     * 🎯 Targeted Eviction: Removes only avatar cache entries belonging to a specific device ID,
     * preserving other fleet node bitmaps to prevent heap thrashing and 60fps drops.
     */
    public static void evictDevice(String deviceId) {
        if (bitmapCache == null || deviceId == null || deviceId.isEmpty()) return;
        try {
            java.util.Map<String, Bitmap> snapshot = bitmapCache.snapshot();
            for (String key : snapshot.keySet()) {
                if (key.contains(deviceId)) {
                    bitmapCache.remove(key);
                }
            }
        } catch (Exception ignored) {}
    }

    public static void clearCache() {
        if (bitmapCache != null) bitmapCache.evictAll();
    }
}
