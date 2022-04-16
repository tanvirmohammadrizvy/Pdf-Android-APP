package com.fulcrumy.pdfeditor.utils;

import android.content.Context;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatDelegate;

import com.fulcrumy.pdfeditor.fragments.SettingsFragment;

public class ThemeUtils {
    public static void setTheme(Context context) {
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(SettingsFragment.NIGHT_MODE_ENABLED, false)) {
            AppCompatDelegate.setDefaultNightMode(2);
        } else {
            AppCompatDelegate.setDefaultNightMode(3);
        }
    }
}
