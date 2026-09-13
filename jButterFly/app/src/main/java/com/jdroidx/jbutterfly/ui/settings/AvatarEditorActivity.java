package com.jdroidx.jbutterfly.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.jdroidx.jbutterfly.R;
import com.jdroidx.jbutterfly.ui.base.ThemeManager;
import com.jdroidx.jbutterfly.ui.base.UiArchitect;
import com.jdroidx.jbutterfly.utils.AvatarLoader;
import com.jdroidx.jbutterfly.utils.RegistryManager;
import java.util.List;

public class AvatarEditorActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        buildLayout();
    }

    private void buildLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ThemeManager.getBackgroundColor(this));

        // Header
        LinearLayout header = UiArchitect.createHeaderBar(this, "Select Profile Avatar (50 Bundled)", v -> finish());
        root.addView(header);

        // Subtitle
        TextView tvSub = new TextView(this);
        tvSub.setText("Choose an avatar to represent your device on the Live Radar");
        tvSub.setTextColor(ThemeManager.getSecondaryTextColor(this));
        tvSub.setTextSize(UiArchitect.spToPx(this, 12f));
        tvSub.setPadding(UiArchitect.dpToPx(this, 16), UiArchitect.dpToPx(this, 8), UiArchitect.dpToPx(this, 16), UiArchitect.dpToPx(this, 12));
        root.addView(tvSub);

        // Grid View of 50 Avatars
        GridView grid = new GridView(this);
        grid.setNumColumns(4);
        grid.setVerticalSpacing(UiArchitect.dpToPx(this, 12));
        grid.setHorizontalSpacing(UiArchitect.dpToPx(this, 12));
        grid.setPadding(UiArchitect.dpToPx(this, 16), 0, UiArchitect.dpToPx(this, 16), UiArchitect.dpToPx(this, 24));
        grid.setGravity(Gravity.CENTER);

        List<String> avatars = AvatarLoader.getAllAvatars();
        grid.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return avatars.size();
            }

            @Override
            public Object getItem(int position) {
                return avatars.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                String avatarName = avatars.get(position);
                FrameLayout itemFrame = new FrameLayout(AvatarEditorActivity.this);
                int size = UiArchitect.dpToPx(AvatarEditorActivity.this, 70);
                itemFrame.setLayoutParams(new AbsListView.LayoutParams(size, size));

                GradientDrawable bg = new GradientDrawable();
                bg.setColor(ThemeManager.getSurfaceColor(AvatarEditorActivity.this));
                bg.setStroke(UiArchitect.dpToPx(AvatarEditorActivity.this, 1), ThemeManager.getBorderColor(AvatarEditorActivity.this));
                bg.setCornerRadius(UiArchitect.dpToPx(AvatarEditorActivity.this, 10));
                itemFrame.setBackground(bg);

                ImageView iv = new ImageView(AvatarEditorActivity.this);
                int pad = UiArchitect.dpToPx(AvatarEditorActivity.this, 8);
                iv.setPadding(pad, pad, pad, pad);
                AvatarLoader.loadAvatar(AvatarEditorActivity.this, iv, avatarName);
                itemFrame.addView(iv);

                itemFrame.setOnClickListener(v -> {
                    RegistryManager.putString(AvatarEditorActivity.this, RegistryManager.KEY_EQUIPPED_AVATAR, avatarName);
                    Toast.makeText(AvatarEditorActivity.this, "Equipped " + avatarName, Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });

                return itemFrame;
            }
        });

        root.addView(grid, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        ));

        setContentView(root);
    }
}
