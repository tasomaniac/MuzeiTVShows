package com.tasomaniac.muzei.tvshows;

import android.app.Application;
import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import java.util.Map;
import java.util.Random;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import timber.log.Timber;

/**
 * A module for Android-specific dependencies which require a Context to create.
 *
 * Created by Said Tahsin Dane on 17/03/15.
 */
@Module
final class AppModule {
    private final App app;

    AppModule(App app) {
        this.app = app;
    }

    @Provides @Singleton Application application() {
        return app;
    }

    @Provides @Singleton
    ContentResolver provideContentResolver() {
        return app.getContentResolver();
    }

    @Provides @Singleton Analytics provideAnalytics() {
        if (BuildConfig.DEBUG) {
            return new Analytics() {
                @Override public void send(Map<String, String> params) {
                    Timber.tag("Analytics").d(String.valueOf(params));
                }
            };
        }

        GoogleAnalytics googleAnalytics = GoogleAnalytics.getInstance(app);
        Tracker tracker = googleAnalytics.newTracker(BuildConfig.ANALYTICS_KEY);
        tracker.setSessionTimeout(300); // ms? s? better be s.
        return new Analytics.GoogleAnalytics(tracker);
    }

    @Provides @Singleton SharedPreferences provideSharedPreferences(Application app) {
        return PreferenceManager.getDefaultSharedPreferences(app);
    }

    @Provides @Singleton Random provideRandom() {
        return new Random();
    }
}