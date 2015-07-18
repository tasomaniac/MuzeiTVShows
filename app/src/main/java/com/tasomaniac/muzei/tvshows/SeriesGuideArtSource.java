package com.tasomaniac.muzei.tvshows;

import com.google.android.apps.muzei.api.RemoteMuzeiArtSource;

import java.util.Random;

public class SeriesGuideArtSource extends RemoteMuzeiArtSource {
    private static final String SOURCE_NAME = "SeriesGuideArtSource";

    private static final int ROTATE_TIME_MILLIS = 24 * 60 * 60 * 1000; // rotate every 24 hours
    private static final int NEXT_ON_ERROR_TIME_MILLIS = 60 * 60 * 1000; // rotate every 24 hours

    Random random;

    public SeriesGuideArtSource() {
        super(SOURCE_NAME);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setUserCommands(BUILTIN_COMMAND_ID_NEXT_ARTWORK);

        random = new Random();
    }

    @Override
    protected void onTryUpdate(int reason) throws RetryException {

        scheduleUpdate(System.currentTimeMillis() + ROTATE_TIME_MILLIS);
    }
}

