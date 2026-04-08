package com.jdroidx.jfindme.ui.settings;

import android.graphics.*;
import android.view.*;
import android.widget.*;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;

public class AvatarCropActivity extends BaseActivityJava {
    private Bitmap rawPhoto;
    private Matrix matrix = new Matrix();
    private Matrix savedMatrix = new Matrix();
    
    // Multi-touch Gestures Hardware Hook
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;
    
    private PointF start = new PointF();
    private PointF mid = new PointF();
    private float oldDist = 1f;

    @Override
    protected View createLayout() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        
        java.io.File cf = new java.io.File(getFilesDir() + "/avatars", "custom_photo.png");
        if (!cf.exists()) {
            Toast.makeText(this, "No valid custom photo found to crop.", Toast.LENGTH_SHORT).show();
            finish();
            return root;
        }
        
        try {
            rawPhoto = BitmapFactory.decodeFile(cf.getAbsolutePath());
        } catch (Exception e) {
            finish();
            return root;
        }
        
        // Dynamic OpenGL-style Renderer
        CropView cv = new CropView(this);
        root.addView(cv, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        
        Button saveBtn = new Button(this);
        saveBtn.setText("EXECUTE FACE CROP");
        saveBtn.setBackgroundColor(getGradientStart());
        saveBtn.setTextColor(Color.WHITE);
        saveBtn.setTypeface(null, Typeface.BOLD);
        
        FrameLayout.LayoutParams btnLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 150);
        btnLp.gravity = Gravity.BOTTOM;
        btnLp.setMargins(50, 0, 50, 50);
        saveBtn.setLayoutParams(btnLp);
        
        saveBtn.setOnClickListener(v -> cv.performCropAndSave());
        Button rotateBtn = new Button(this);
        rotateBtn.setText("ROTATE IMAGE 90° ⟳");
        rotateBtn.setBackgroundColor(Color.parseColor("#424242")); // iOS Dark Mode Hue
        rotateBtn.setTextColor(Color.WHITE);
        rotateBtn.setTypeface(null, Typeface.BOLD);
        
        FrameLayout.LayoutParams rotLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 150);
        rotLp.gravity = Gravity.BOTTOM;
        rotLp.setMargins(50, 0, 50, 220); // Exactly stacked 20px above the primary 'Crop' executor
        rotateBtn.setLayoutParams(rotLp);
        
        rotateBtn.setOnClickListener(v -> cv.performRotate());
        root.addView(rotateBtn);

        root.addView(saveBtn);
        return root;
    }
    
    @Override
    protected void setupLogic() {}

    private class CropView extends View {
        private Paint maskPaint, borderPaint, bmpPaint;
        private float circleRadius, circleCX, circleCY;
        private boolean isInit = false;
        
        public CropView(android.content.Context context) {
            super(context);
            maskPaint = new Paint();
            maskPaint.setColor(Color.parseColor("#99000000")); // 60% Black Tint
            
            borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(8f);
            borderPaint.setColor(getGradientStart()); // Native Theme Circle
            
            bmpPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            bmpPaint.setFilterBitmap(true);
        }
        
        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            
            if (!isInit) {
                circleCX = getWidth() / 2f;
                circleCY = getHeight() / 2f;
                circleRadius = getWidth() * 0.4f; // Perfect 80% screen width circle
                // Force Native Zoom constraint to fill the hole safely
                float scale = (circleRadius * 2) / Math.min(rawPhoto.getWidth(), rawPhoto.getHeight());
                matrix.postScale(scale, scale);
                matrix.postTranslate(circleCX - (rawPhoto.getWidth()*scale)/2f, circleCY - (rawPhoto.getHeight()*scale)/2f);
                isInit = true;
            }
            
            canvas.drawColor(Color.DKGRAY);
            if (rawPhoto != null) canvas.drawBitmap(rawPhoto, matrix, bmpPaint);
            
            // Mask Overdraw Protocol
            Bitmap maskBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            Canvas maskCanvas = new Canvas(maskBitmap);
            maskCanvas.drawColor(Color.parseColor("#99000000"));
            
            Paint eraser = new Paint(Paint.ANTI_ALIAS_FLAG);
            eraser.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            maskCanvas.drawCircle(circleCX, circleCY, circleRadius, eraser);
            
            canvas.drawBitmap(maskBitmap, 0, 0, null);
            canvas.drawCircle(circleCX, circleCY, circleRadius, borderPaint);
        }
        
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    savedMatrix.set(matrix);
                    start.set(event.getX(), event.getY());
                    mode = DRAG;
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    oldDist = spacing(event);
                    if (oldDist > 10f) {
                        savedMatrix.set(matrix);
                        midPoint(mid, event);
                        mode = ZOOM;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    mode = NONE;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mode == DRAG) {
                        matrix.set(savedMatrix);
                        matrix.postTranslate(event.getX() - start.x, event.getY() - start.y);
                    } else if (mode == ZOOM) {
                        float newDist = spacing(event);
                        if (newDist > 10f) {
                            matrix.set(savedMatrix);
                            float scale = newDist / oldDist;
                            matrix.postScale(scale, scale, mid.x, mid.y);
                        }
                    }
                    invalidate();
                    break;
            }
            return true;
        }
        
        private float spacing(MotionEvent event) {
            float x = event.getX(0) - event.getX(1);
            float y = event.getY(0) - event.getY(1);
            return (float) Math.sqrt(x * x + y * y);
        }
        
        private void midPoint(PointF point, MotionEvent event) {
            float x = event.getX(0) + event.getX(1);
            float y = event.getY(0) + event.getY(1);
            point.set(x / 2, y / 2);
        }
        
        // 🚀 QA Fix: Global EXIF Header Rotator Bypass
        public void performRotate() {
            if (rawPhoto != null) {
                // Rotates flawlessly around the exact Cartesian center of the 80% screen Mask!
                matrix.postRotate(90f, circleCX, circleCY);
                invalidate();
            }
        }
        
        public void performCropAndSave() {
            try {
                int targetSize = (int) (circleRadius * 2);
                Bitmap output = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888);
                Canvas cropCanvas = new Canvas(output);
                
                Path p = new Path();
                p.addCircle(circleRadius, circleRadius, circleRadius, Path.Direction.CCW);
                cropCanvas.clipPath(p);
                
                Matrix exportMatrix = new Matrix(matrix);
                exportMatrix.postTranslate(-(circleCX - circleRadius), -(circleCY - circleRadius));
                cropCanvas.drawBitmap(rawPhoto, exportMatrix, bmpPaint);
                
                // 🚀 QA Fix: Master resolution (600px) ensures a 'best fit' within the 420px profile circle
                Bitmap scaledFinal = Bitmap.createScaledBitmap(output, 600, 600, true);
                
                java.io.File cf = new java.io.File(getFilesDir() + "/avatars", "custom_photo.png");
                java.io.FileOutputStream fos = new java.io.FileOutputStream(cf);
                scaledFinal.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush(); fos.close();
                
                // 🚀 QA Fix: Atomic Preference Sync (Set to Custom Identity -1)
                android.content.SharedPreferences prefs = getContext().getSharedPreferences("system_registry", android.content.Context.MODE_PRIVATE);
                prefs.edit().putInt("equipped_avatar_id", -1).apply();
                
                setResult(RESULT_OK);
                finish();
            } catch (Exception e) {
                Toast.makeText(getContext(), "Hardware GPU Crop execution failed.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
