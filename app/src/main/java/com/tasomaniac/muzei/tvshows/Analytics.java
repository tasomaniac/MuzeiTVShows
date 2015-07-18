package com.tasomaniac.muzei.tvshows;

import com.google.android.gms.analytics.Tracker;

import java.util.Map;

interface Analytics {

    /**
     * @see {@link Tracker#send(Map)} for usage.
     */
    void send(Map<String, String> params);

    class GoogleAnalytics implements Analytics {
        private final Tracker tracker;

        public GoogleAnalytics(Tracker tracker) {
            this.tracker = tracker;
        }

        @Override
        public void send(Map<String, String> params) {
            tracker.send(params);
        }
    }
}