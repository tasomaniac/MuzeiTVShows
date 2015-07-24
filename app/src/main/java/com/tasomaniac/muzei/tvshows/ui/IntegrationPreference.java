package com.tasomaniac.muzei.tvshows.ui;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Build;
import android.preference.CheckBoxPreference;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;

import com.tasomaniac.muzei.tvshows.App;
import com.tasomaniac.muzei.tvshows.R;
import com.tasomaniac.muzei.tvshows.util.AppInstallEnabler;
import com.tasomaniac.muzei.tvshows.util.ContentProviderEnabler;

import javax.inject.Inject;

import timber.log.Timber;

public class IntegrationPreference extends CheckBoxPreference {

    @Inject PackageManager packageManager;
    @Inject ContentResolver contentResolver;

    AppInstallEnabler appInstallEnabler;
    ContentProviderEnabler contentProviderEnabler;

    Intent originalIntent;
    Intent alternativeIntent;
    Intent uriMissingIntent;
    private Uri expectedContentUri;
    private String summaryUriMissing;

    public IntegrationPreference(Context context) {
        super(context);
        initialize(context, null);
    }

    public IntegrationPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context, attrs);
    }

    public IntegrationPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public IntegrationPreference(
            Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(context, attrs);
    }

    private void initialize(Context context, AttributeSet attrs) {
        App.get(context).component().inject(this);

        setWidgetLayoutResource(R.layout.preference_widget_error);
        setDefaultValue(true);

        final TypedArray sa = context.obtainStyledAttributes(attrs,
                R.styleable.IntegrationPreference);

        extractAlternativeIntent(sa);
        extractUriMissingIntent(sa);

        String expectedContentUriString =
                sa.getString(R.styleable.IntegrationPreference_expectedContentUri);
        if (!TextUtils.isEmpty(expectedContentUriString)) {
            expectedContentUri = Uri.parse(expectedContentUriString);
        }
        summaryUriMissing = sa.getString(R.styleable.IntegrationPreference_summaryUriMissing);

        sa.recycle();

        setDisableDependentsState(true);

        appInstallEnabler = new AppInstallEnabler(context, this);
        contentProviderEnabler = new ContentProviderEnabler(context, this);
    }

    @Override
    protected void onAttachedToActivity() {
        super.onAttachedToActivity();
        originalIntent = getIntent();

        if (originalIntent != null && !hasIntent(alternativeIntent)) {
            ComponentName component = originalIntent.getComponent();
            if (component != null) {
                alternativeIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=" + component.getPackageName()));
            }
        }
        checkState();
    }

    public Uri getExpectedContentUri() {
        return expectedContentUri;
    }

    public void resume() {
        appInstallEnabler.resume();
        contentProviderEnabler.resume();
    }

    public void pause() {
        appInstallEnabler.pause();
        contentProviderEnabler.pause();
    }

    public void checkState() {
        if (hasIntent(originalIntent)) {
            if (expectedContentUri != null
                    && hasTroublesomeProvider(expectedContentUri)) {
                setSummaryOff(summaryUriMissing);
                adjustPreference(uriMissingIntent, false);
            } else {
                adjustPreference(originalIntent, true);
            }
        } else {
            adjustPreference(alternativeIntent, false);
        }
    }

    private void extractAlternativeIntent(TypedArray sa) {
        //Parse the alternative Intent
        alternativeIntent = new Intent();

        alternativeIntent.setAction(
                sa.getString(R.styleable.IntegrationPreference_alternativeIntentAction));

        String data = sa.getString(R.styleable.IntegrationPreference_alternativeIntentData);
        String mimeType = sa.getString(R.styleable.IntegrationPreference_alternativeIntentMimeType);
        alternativeIntent.setDataAndType(data != null ? Uri.parse(data) : null, mimeType);

        String packageName =
                sa.getString(R.styleable.IntegrationPreference_alternativeIntentTargetPackage);
        String className =
                sa.getString(R.styleable.IntegrationPreference_alternativeIntentTargetClass);
        if (packageName != null && className != null) {
            alternativeIntent.setComponent(new ComponentName(packageName, className));
        }
    }

    private void extractUriMissingIntent(TypedArray sa) {
        //Parse the URI Missing Intent
        uriMissingIntent = new Intent();

        uriMissingIntent.setAction(
                sa.getString(R.styleable.IntegrationPreference_uriMissingIntentAction));

        String data = sa.getString(R.styleable.IntegrationPreference_uriMissingIntentData);
        String mimeType = sa.getString(R.styleable.IntegrationPreference_uriMissingIntentMimeType);
        uriMissingIntent.setDataAndType(data != null ? Uri.parse(data) : null, mimeType);

        String packageName =
                sa.getString(R.styleable.IntegrationPreference_uriMissingIntentTargetPackage);
        String className =
                sa.getString(R.styleable.IntegrationPreference_uriMissingIntentTargetClass);
        if (packageName != null && className != null) {
            uriMissingIntent.setComponent(new ComponentName(packageName, className));
        }
    }

    @Override
    public void setSummaryOff(CharSequence summary) {
        if (summary != null) {
            SpannableString summarySpan = getErrorString(summary);
            super.setSummaryOff(summarySpan);
        } else {
            super.setSummaryOff(null);
        }
    }

    private boolean hasTroublesomeProvider(@NonNull Uri uri) {
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(uri,
                    new String[]{BaseColumns._ID}, null, null, null);
            return cursor == null || cursor.getCount() == 0;
        } catch (SQLiteException e) {
            Timber.e(e, "Error accessing Series Guide database");
            return true;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private boolean hasIntent(@Nullable Intent intent) {
        return intent != null
                && packageManager.resolveActivity(intent, 0) != null;
    }

    @Override
    protected void onClick() {
    }

    @Override
    public void setChecked(boolean checked) {
        super.setChecked(checked);

        if (checked) {
            setTitle(getTitle().toString());
        } else {
            SpannableString titleSpan = getErrorString(getTitle());
            setTitle(titleSpan);
        }
    }

    @NonNull
    private SpannableString getErrorString(CharSequence originalString) {
        int errorColor = getContext().getResources().getColor(R.color.error_color);
        SpannableString errorSpan = new SpannableString(originalString);
        errorSpan.setSpan(new ForegroundColorSpan(errorColor), 0, errorSpan.length(), 0);
        return errorSpan;
    }

    private void adjustPreference(Intent intentOff, boolean checked) {
        setIntent(intentOff);
        setChecked(checked);

        if (!hasIntent(intentOff)) {
            setIntent(null);
        }
        //TODO
//        if (getDependency() != null) {
//            Preference dependencyPref = getPreferenceManager().findPreference(getDependency());
//            if (dependencyPref != null) {
//                dependencyPref.setEnabled(false);
//            }
//        }
    }
}
