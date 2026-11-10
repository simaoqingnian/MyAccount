package com.simaoqingnian.myaccount;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

/** Recreate themed windows through Android's saved-state path, including entry drafts. */
public abstract class ThemedActivity extends Activity {
    private String createdTheme;
    protected boolean themeRefreshPending;

    public static ThemePalette preference(Context context) {
        return ThemePalette.forId(context.getSharedPreferences("appearance", MODE_PRIVATE).getString("theme", "titanium"));
    }

    @Override protected void onCreate(Bundle state) {
        ThemePalette selected=preference(this);
        createdTheme=selected.id;
        setTheme(selected==ThemePalette.PEACH?R.style.Theme_Heji_Peach:selected==ThemePalette.SAGE?R.style.Theme_Heji_Sage:R.style.Theme_Heji);
        Ui.applyTheme(selected);
        super.onCreate(state);
    }

    @Override protected void onResume() {
        super.onResume();
        if(!createdTheme.equals(preference(this).id)) {
            themeRefreshPending=true;
            recreate();
        }
    }
}
