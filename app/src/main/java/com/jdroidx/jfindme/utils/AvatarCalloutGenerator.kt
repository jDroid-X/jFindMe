package com.jdroidx.jfindme.utils

import android.content.Context
import android.graphics.*
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

object AvatarCalloutGenerator {
    
    // 🚀 ULTRA-OPTIMIZATION: Persistent GPU Memory Paint Banks (Averts Garbage Collection Micro-Stutters)
    private var isInitialized = false
    private lateinit var textPaint: Paint
    private lateinit var boxPaint: Paint
    private lateinit var neonPaintOrange: Paint
    private lateinit var neonPaintBlue: Paint
    private lateinit var neonPaintGold: Paint
    private lateinit var neonPaintRed: Paint
    private lateinit var neonPaintGrey: Paint
    private lateinit var avatarPaint: Paint
    
    private fun initPaints(density: Float) {
        if (isInitialized) return
        textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 14f * density
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(180, 255, 255, 255) // Frost Glassmorphism (70% Opacity)
            setShadowLayer(5f, 0f, 3f, Color.argb(80, 0, 0, 0))
        }
        neonPaintOrange = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF6D00") // Deep Neon Orange (Child)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * density
            setShadowLayer(8f, 0f, 0f, Color.parseColor("#FF9100")) 
        }
        neonPaintBlue = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00E5FF") // Cyberpunk Blue (Self)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * density
            setShadowLayer(8f, 0f, 0f, Color.parseColor("#00B0FF")) 
        }
        neonPaintRed = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF1744") // Neon Red (Spouse)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * density
            setShadowLayer(8f, 0f, 0f, Color.parseColor("#FF5252")) 
        }
        neonPaintGrey = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#B0BEC5") // Grey Neon (Friends)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * density
            setShadowLayer(8f, 0f, 0f, Color.parseColor("#CFD8DC")) 
        }
        neonPaintGold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFD700") // Core Guardian Gold (Parent fallback)
            style = Paint.Style.STROKE
            strokeWidth = 2.0f * density
            setShadowLayer(10f, 0f, 0f, Color.parseColor("#FFC400")) 
        }
        
        avatarPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        isInitialized = true
    }

    /**
     * Engine Override: Google Maps natively isolates InfoWindows (1 visible at a time).
     * To fulfill the 'Permanently Visible' array structure, we dynamically draw
     * the Custom Name Callout Box directly into the physical Bitmap payload in local RAM.
     */
    @JvmStatic
    @JvmOverloads
    fun generate(context: Context, name: String, avatarBmp: Bitmap? = null, showName: Boolean = true, isSelf: Boolean = false, stackCount: Int = 0, roleStr: String = "DEFAULT"): BitmapDescriptor {
        return BitmapDescriptorFactory.fromBitmap(generateRawBitmap(context, name, avatarBmp, showName, isSelf, stackCount, roleStr))
    }

    @JvmStatic
    @JvmOverloads
    fun generateRawBitmap(context: Context, name: String, avatarBmp: Bitmap? = null, showName: Boolean = true, isSelf: Boolean = false, stackCount: Int = 0, roleStr: String = "DEFAULT"): Bitmap {
        val density = context.resources.displayMetrics.density
        
        // 🚀 QA Structural Override: The Orbital Tether Graphic Matrix
        val frameSize = (140 * density).toInt() // Massive transparent border dynamically holding the offset
        val anchorPx = frameSize / 2f    // The exact physical GPS coordinate on the Google Map
        
        val avatarSize = (46 * density).toInt()  // The actual Squircle sizing
        val halfAv = avatarSize / 2f
        
        initPaints(density)
        
        val resultBmp = Bitmap.createBitmap(frameSize, frameSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBmp)
        
        // 🚀 QA Fix: Role-Based Identity Matrix per new Tracking Specifications
        val safeUpperRole = roleStr.uppercase()
        val activeNeonPaint = when {
            isSelf -> neonPaintBlue
            safeUpperRole.contains("SPOUSE") -> neonPaintRed
            safeUpperRole.contains("CHILD") -> neonPaintOrange
            safeUpperRole.contains("FRIEND") -> neonPaintGrey
            safeUpperRole.contains("GUARDIAN") -> neonPaintGold // Parent fallback
            else -> neonPaintOrange
        }

        // Orchestrate Orbital Displacement Mathematics
        // If stackCount is 0, it pushes straight Up (-90 degrees). Each new player pushes out 72 degrees.
        val tetherLength = 40f * density
        val angleDeg = (stackCount * 72) - 90 
        val angleRad = angleDeg * Math.PI / 180.0
        
        val cx = anchorPx + (tetherLength * Math.cos(angleRad)).toFloat()
        val cy = anchorPx + (tetherLength * Math.sin(angleRad)).toFloat()

        // 1. Construct the Speech Balloon (Circle + Pointer Tail) using Native Vector Geometry
        val shapePath = Path()
        shapePath.addCircle(cx, cy, halfAv, Path.Direction.CW)
        
        val tailPath = Path()
        val pointingAngleToDot = Math.atan2((anchorPx - cy).toDouble(), (anchorPx - cx).toDouble())
        val spreadAngle = 20.0 * Math.PI / 180.0 // The Base width of the tail triangle
        
        val p1x = cx + halfAv * Math.cos(pointingAngleToDot - spreadAngle).toFloat()
        val p1y = cy + halfAv * Math.sin(pointingAngleToDot - spreadAngle).toFloat()
        val p2x = cx + halfAv * Math.cos(pointingAngleToDot + spreadAngle).toFloat()
        val p2y = cy + halfAv * Math.sin(pointingAngleToDot + spreadAngle).toFloat()
        
        tailPath.moveTo(anchorPx, anchorPx) // Exact tip of the pointer touching the GPS Coordinate
        tailPath.lineTo(p1x, p1y)
        tailPath.lineTo(p2x, p2y)
        tailPath.close()
        
        shapePath.op(tailPath, Path.Op.UNION) // Fuses the Balloon to the Tail natively!

        // 2. White Occlusion Background Scrubbed per UX requirement; Bubble interior is natively fully transparent.
        
        // 3. Render the Custom Graphic physically into the Balloon Head!
        if (avatarBmp != null) {
            val scaledAvatar = Bitmap.createScaledBitmap(avatarBmp, avatarSize, avatarSize, true)
            val avatarFillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            avatarFillPaint.shader = BitmapShader(scaledAvatar, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            canvas.save()
            canvas.translate(cx - halfAv, cy - halfAv) // Translate Shader Matrix securely!
            canvas.drawCircle(halfAv, halfAv, halfAv - 1f, avatarFillPaint) 
            canvas.restore()
        }
        
        // 4. Trace the continuous Neon Polygon directly over the bounds of the entire Balloon Shape
        canvas.drawPath(shapePath, activeNeonPaint)
        
        // 5. Hard Anchor Dot at the very tip of the Balloon Tail (The exact point of tracking!)
        val staticAnchorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { 
            color = activeNeonPaint.color
            style = Paint.Style.FILL 
        }
        canvas.drawCircle(anchorPx, anchorPx, 3f * density, staticAnchorPaint)

        // 🚀 6. PERMANENTLY BURN <Avatar name> TOOLTIP DIRECTLY INTO THE MAP LAYER
        // Text dynamically floats securely beneath the absolute GPS tracker dot!
        if (showName) {
            val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 14f * density
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                setShadowLayer(5f, 0f, 3f, Color.BLACK)
            }
            canvas.drawText(name, anchorPx, anchorPx + 20f * density, namePaint)
        }

        return resultBmp
    }
}
