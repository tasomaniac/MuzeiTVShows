package com.tasomaniac.muzei.tvshows;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource;
import com.tasomaniac.muzei.tvshows.data.SeriesGuideContract.Episodes;
import com.tasomaniac.muzei.tvshows.data.SeriesGuideContract.Shows;

import java.util.Random;

import javax.inject.Inject;

public class SeriesGuideArtSource extends RemoteMuzeiArtSource {
    private static final String SOURCE_NAME = "SeriesGuideArtSource";

    private static final int ROTATE_TIME_MILLIS = 24 * 60 * 60 * 1000; // rotate every 24 hours
    private static final int NEXT_ON_ERROR_TIME_MILLIS = 60 * 60 * 1000; // rotate every 24 hours


    public static final String PATH_EPISODES = "episodes";
    public static final String PATH_WITHSHOW = "withshow";



    public interface ActivityQuery {

        String[] PROJECTION = new String[] {
                "episodes" + "." + Episodes._ID,
                Episodes.TITLE,
                Episodes.NUMBER,
                Episodes.SEASON,
                Episodes.FIRSTAIREDMS,
                Episodes.WATCHED,
                Episodes.COLLECTED,
                Shows.REF_SHOW_ID,
                Shows.TITLE,
                Shows.NETWORK,
                Shows.POSTER
        };

        String QUERY_UPCOMING = Episodes.FIRSTAIREDMS + ">=? AND " + Episodes.FIRSTAIREDMS
                + "<? AND " + Shows.SELECTION_NO_HIDDEN;

        String QUERY_RECENT = Episodes.FIRSTAIREDMS + "<? AND " + Episodes.FIRSTAIREDMS + ">? AND "
                + Shows.SELECTION_NO_HIDDEN;

        String SORTING_UPCOMING = Episodes.FIRSTAIREDMS + " ASC," + Shows.TITLE + " ASC,"
                + Episodes.NUMBER + " ASC";

        String SORTING_RECENT = Episodes.FIRSTAIREDMS + " DESC," + Shows.TITLE + " ASC,"
                + Episodes.NUMBER + " DESC";

        int _ID = 0;
        int TITLE = 1;
        int NUMBER = 2;
        int SEASON = 3;
        int RELEASE_TIME_MS = 4;
        int WATCHED = 5;
        int COLLECTED = 6;
        int SHOW_ID = 7;
        int SHOW_TITLE = 8;
        int SHOW_NETWORK = 9;
        int SHOW_POSTER = 10;
    }

    @Inject Random random;
    @Inject Analytics analytics;
    @Inject ContentResolver contentResolver;

    public SeriesGuideArtSource() {
        super(SOURCE_NAME);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        App.get(this).component().inject(this);
        setUserCommands(BUILTIN_COMMAND_ID_NEXT_ARTWORK);

    }

    @Override
    protected void onTryUpdate(int reason) throws RetryException {

        // go an hour back in time, so episodes move to recent one hour late
        long recentThreshold = System.currentTimeMillis() - DateUtils.HOUR_IN_MILLIS;

        String[] selectionArgs;
        String sortOrder;
        long timeThreshold = Long.MAX_VALUE;

        StringBuilder query = new StringBuilder(ActivityQuery.QUERY_UPCOMING);
        sortOrder = ActivityQuery.SORTING_UPCOMING;

        selectionArgs = new String[] {
                String.valueOf(recentThreshold), String.valueOf(timeThreshold)
        };

        // append unwatched selection if necessary
//        if (isOnlyUnwatched) {
//            query.append(" AND ").append(Episodes.SELECTION_UNWATCHED);
//        }

        final Cursor upcomingEpisodes =
                contentResolver.query(Episodes.CONTENT_URI_WITHSHOW,
                        ActivityQuery.PROJECTION,
                        query.toString(), selectionArgs, sortOrder);

        final long customCurrentTime = System.currentTimeMillis();
        int hourThreshold = 12;
        long latestTimeToInclude = customCurrentTime + hourThreshold * DateUtils.HOUR_IN_MILLIS;

        // Ensure there are episodes to show
        if (upcomingEpisodes != null) {
            if (upcomingEpisodes.moveToFirst()) {

                // Ensure those episodes are within the user set time frame
                long releaseTime = upcomingEpisodes
                        .getLong(ActivityQuery.RELEASE_TIME_MS);
//                if (releaseTime <= latestTimeToInclude) {
                    // build our DashClock panel

                    // title of first show
                    String expandedTitle = upcomingEpisodes.getString(ActivityQuery.SHOW_TITLE);

                    // get the actual release time
//                    Date actualRelease = TimeTools.applyUserOffset(this, releaseTime);
//                    String absoluteTime = TimeTools.formatToLocalTime(this, actualRelease);
//                    String releaseDay = TimeTools.formatToLocalDay(actualRelease);

                    // time and network, e.g. 'Mon 10:00, Network'
                    StringBuilder expandedBody = new StringBuilder();
//                    if (!DateUtils.isToday(actualRelease.getTime())) {
//                        expandedBody.append(releaseDay).append(" ");
//                    }
                    expandedBody.append(releaseTime);
                    String network = upcomingEpisodes
                            .getString(ActivityQuery.SHOW_NETWORK);
                    if (!TextUtils.isEmpty(network)) {
                        expandedBody.append(" — ").append(network);
                    }

                    // more than one episode at this time? Append e.g. '3 more'
                    int additionalEpisodes = 0;
                    while (upcomingEpisodes.moveToNext()
                            && releaseTime == upcomingEpisodes
                            .getLong(ActivityQuery.RELEASE_TIME_MS)) {
                        additionalEpisodes++;
                    }
//                    if (additionalEpisodes > 0) {
//                        expandedBody.append("\n");
//                        expandedBody.append(getString(R.string.more, additionalEpisodes));
//                    }

                    publishArtwork(new Artwork.Builder()
                            .title(expandedTitle)
                            .byline(expandedBody.toString())
                            .imageUri(Uri.parse("http://thetvdb.com/banners/_cache/" + upcomingEpisodes.getString(ActivityQuery.SHOW_POSTER)))
                            .token(upcomingEpisodes.getString(ActivityQuery._ID))
                            .viewIntent(new Intent("com.battlelancer.seriesguide.api.action.VIEW_SHOW"))
                            .build());

//                }
            }
            upcomingEpisodes.close();
        }

        scheduleUpdate(System.currentTimeMillis() + ROTATE_TIME_MILLIS);
    }
}

