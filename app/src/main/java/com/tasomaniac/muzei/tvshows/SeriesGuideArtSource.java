package com.tasomaniac.muzei.tvshows;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource;
import com.tasomaniac.muzei.tvshows.data.OnlyUnwatched;
import com.tasomaniac.muzei.tvshows.data.SeriesGuideContract.Episodes;
import com.tasomaniac.muzei.tvshows.data.SeriesGuideContract.Shows;
import com.tasomaniac.muzei.tvshows.util.TimeTools;

import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import javax.inject.Inject;

public class SeriesGuideArtSource extends RemoteMuzeiArtSource {
    private static final String SOURCE_NAME = "SeriesGuideArtSource";

    private static final int ROTATE_TIME_MILLIS = 24 * 60 * 60 * 1000; // rotate every 24 hours
    private static final int NEXT_ON_ERROR_TIME_MILLIS = 60 * 60 * 1000; // rotate every 24 hours

    public interface EpisodeQuery {

        String[] PROJECTION = new String[]{
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

    @Inject @OnlyUnwatched Boolean isOnlyUnwatched;

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

        //TODO Check if the user have Series Guide installed.
        //TODO Check if the user has Series Guide Content Provider setted up.
        onTryUpcomingEpisodesUpdate();


        scheduleUpdate(System.currentTimeMillis() + ROTATE_TIME_MILLIS);
    }

    private void onTryUpcomingEpisodesUpdate() {
        // go an hour back in time, so episodes move to recent one hour late
        long recentThreshold = System.currentTimeMillis() - DateUtils.HOUR_IN_MILLIS;
        long timeThreshold = recentThreshold + DateUtils.DAY_IN_MILLIS;

        StringBuilder query = new StringBuilder(EpisodeQuery.QUERY_UPCOMING);
        String sortOrder = EpisodeQuery.SORTING_UPCOMING;
        String[] selectionArgs = new String[]{
                String.valueOf(recentThreshold), String.valueOf(timeThreshold)
        };

        // append unwatched selection if necessary
        if (isOnlyUnwatched) {
            query.append(" AND ").append(Episodes.SELECTION_UNWATCHED);
        }

        final Cursor upcomingEpisodes =
                contentResolver.query(Episodes.CONTENT_URI_WITHSHOW,
                        EpisodeQuery.PROJECTION,
                        query.toString(), selectionArgs, sortOrder);

        // Ensure there are episodes to show
        // TODO make the user to install SeriesGuide, setup and have some shows in it.
        if (upcomingEpisodes != null) {
            while (upcomingEpisodes.moveToNext()) {

                String currentToken = (getCurrentArtwork() != null) ?
                        getCurrentArtwork().getToken() : null;
                String upcomingToken = upcomingEpisodes.getString(EpisodeQuery._ID);

                if (upcomingToken.equals(currentToken)) {
                    continue;
                }

                // Ensure those episodes are within the user set time frame
                long releaseTime = upcomingEpisodes
                        .getLong(EpisodeQuery.RELEASE_TIME_MS);

                // title of first show
                final String title = upcomingEpisodes.getString(EpisodeQuery.SHOW_TITLE);

                // get the actual release time
                Calendar dateTime = Calendar.getInstance();
                dateTime.setTimeInMillis(releaseTime);
                Date actualRelease = dateTime.getTime();
                String absoluteTime = TimeTools.formatToLocalTime(this, actualRelease);
                String releaseDay = TimeTools.formatToLocalDay(actualRelease);

                // time and network, e.g. 'Mon 10:00, Network'
                StringBuilder byline = new StringBuilder();
                if (!DateUtils.isToday(releaseTime)) {
                    byline.append(releaseDay).append(" ");
                }
                byline.append(absoluteTime);
                final String network = upcomingEpisodes
                        .getString(EpisodeQuery.SHOW_NETWORK);
                if (!TextUtils.isEmpty(network)) {
                    byline.append(" — ").append(network);
                }

                publishArtwork(new Artwork.Builder()
                        .title(title)
                        .byline(byline.toString())
                        .imageUri(getPosterImageUri(upcomingEpisodes))
                        .token(upcomingToken)
                        .viewIntent(getViewIntent())
                        .build());

                break;
            }
            upcomingEpisodes.close();
        }
    }

    @NonNull
    private Intent getViewIntent() {
        //TODO open the show/episode/movie directly.
        return new Intent("com.battlelancer.seriesguide.api.action.VIEW_SHOW");
    }

    @NonNull
    private Uri getPosterImageUri(Cursor upcomingEpisodes) {
        return Uri.parse("http://thetvdb.com/banners/_cache/" +
                upcomingEpisodes.getString(EpisodeQuery.SHOW_POSTER));
    }
}

