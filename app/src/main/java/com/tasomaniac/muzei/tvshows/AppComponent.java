package com.tasomaniac.muzei.tvshows;

import com.tasomaniac.muzei.tvshows.data.DataModule;
import com.tasomaniac.muzei.tvshows.ui.IntegrationPreference;
import com.tasomaniac.muzei.tvshows.ui.SettingsFragment;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {AppModule.class, DataModule.class})
public interface AppComponent {

    void inject(App app);

    void inject(SeriesGuideArtSource artSource);

    void inject(SettingsFragment fragment);

    void inject(IntegrationPreference pref);

    /**
     * An initializer that creates the graph from an application.
     */
    final class Initializer {
        static AppComponent init(App app) {
            return DaggerAppComponent.builder()
                    .appModule(new AppModule(app))
                    .build();
        }

        private Initializer() {
        } // No instances.
    }
}