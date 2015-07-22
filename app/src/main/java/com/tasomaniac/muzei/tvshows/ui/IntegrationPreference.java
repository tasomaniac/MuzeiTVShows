package com.tasomaniac.muzei.tvshows.ui;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.preference.Preference;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;

import com.tasomaniac.muzei.tvshows.App;
import com.tasomaniac.muzei.tvshows.R;

import javax.inject.Inject;

public class IntegrationPreference extends Preference {

    @Inject PackageManager packageManager;
    @Inject ContentResolver contentResolver;

    public IntegrationPreference(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.preferenceStyle);
    }

    public IntegrationPreference(Context context) {
        this(context, null);
    }

    public IntegrationPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        App.get(context).component().inject(this);
    }

    public boolean hasTroublesomeProvider(Uri uri) {
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(uri,
                    new String[]{BaseColumns._ID}, null, null, null);
            return cursor == null || cursor.getCount() == 0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public boolean hasTroublesomeIntent() {
        Intent intent = getIntent();
        return intent == null
                || packageManager.resolveActivity(intent, 0) == null;
    }

    public void adjustPreference(@Nullable Preference dependantPref,
                                 @NonNull Intent alternativeIntent,
                                 @StringRes int alternativeSummary) {

        setIntent(alternativeIntent);

        int errorColor = getContext().getResources().getColor(R.color.error_color);

        SpannableString summarySpan =
                new SpannableString(getContext().getString(alternativeSummary));
        summarySpan.setSpan(new ForegroundColorSpan(errorColor), 0, summarySpan.length(), 0);
        setSummary(summarySpan);

        setWidgetLayoutResource(R.layout.error_layout);

        if (hasTroublesomeIntent()) {
            setIntent(null);
        }
        if (dependantPref != null) {
            dependantPref.setEnabled(false);
        }
    }
}
