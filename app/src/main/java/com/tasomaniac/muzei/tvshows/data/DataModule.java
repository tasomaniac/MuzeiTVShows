package com.tasomaniac.muzei.tvshows.data;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.tasomaniac.muzei.tvshows.data.prefs.BooleanPreference;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public final class DataModule {
    public static final boolean DEFAULT_ONLY_UNWATCHED = false;

    @Provides
    @Singleton
    SharedPreferences provideSharedPreferences(Application app) {
        return PreferenceManager.getDefaultSharedPreferences(app);
    }

    @Provides @Singleton @OnlyUnwatched
    BooleanPreference provideOnlyUnwatchedPreference(
            SharedPreferences prefs) {
        return new BooleanPreference(prefs, "show-countdown", DEFAULT_ONLY_UNWATCHED);
    }

    @Provides @OnlyUnwatched Boolean provideOnlyUnwatched(@OnlyUnwatched BooleanPreference pref) {
        return pref.get();
    }
}