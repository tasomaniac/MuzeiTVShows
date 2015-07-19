/*
 * Copyright 2014 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tasomaniac.muzei.tvshows.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.format.DateFormat;
import android.text.format.DateUtils;

import com.tasomaniac.muzei.tvshows.R;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Helper tools for converting and formatting date times for shows and episodes.
 */
public class TimeTools {

    public static final int RELEASE_WEEKDAY_DAILY = 0;

    private static final String TIMEZONE_ID_PREFIX_AMERICA = "America/";

    private static final String ISO3166_1_UNITED_STATES = "us";
    private static final String TIMEZONE_ID_US_EASTERN = "America/New_York";
    private static final Object TIMEZONE_ID_US_EASTERN_DETROIT = "America/Detroit";
    private static final String TIMEZONE_ID_US_CENTRAL = "America/Chicago";
    private static final String TIMEZONE_ID_US_MOUNTAIN = "America/Denver";
    private static final String TIMEZONE_ID_US_ARIZONA = "America/Phoenix";
    private static final String TIMEZONE_ID_US_PACIFIC = "America/Los_Angeles";

    private static final DateTimeFormatter DATE_TIME_FORMATTER_UTC
            = ISODateTimeFormat.dateTime().withZoneUTC();

    private static final DateTimeFormatter TVDB_DATE_FORMATTER = ISODateTimeFormat.date();

    /**
     * Returns the appropriate time zone for the given tzdata zone identifier.
     *
     * <p> Falls back to "America/New_York" if timezone string is empty or unknown.
     */
    public static DateTimeZone getDateTimeZone(@Nullable String timezone) {
        if (timezone != null && timezone.length() != 0) {
            try {
                return DateTimeZone.forID(timezone);
            } catch (IllegalArgumentException ignored) {
            }
        }

        return DateTimeZone.forID(TIMEZONE_ID_US_EASTERN);
    }

    /**
     * Parses a ISO 8601 time string (e.g. "20:30") and encodes it into an integer with format
     * "hhmm" (e.g. 2030).
     *
     * <p> If time is invalid returns -1. Performs no extensive formatting check, though.
     */
    public static int parseShowReleaseTime(@Nullable String localTime) {
        if (localTime == null || localTime.length() != 5) {
            return -1;
        }

        // extract hour and minute, example: "20:30" => hour = 20, minute = 30
        int hour = Integer.valueOf(localTime.substring(0, 2));
        int minute = Integer.valueOf(localTime.substring(3, 5));

        // return int encoded time, e.g. hhmm (2030)
        return hour * 100 + minute;
    }

    /**
     * Converts US week day string to {@link org.joda.time.DateTimeConstants} day.
     *
     * <p> Returns -1 if no conversion is possible and 0 if it is "Daily".
     */
    public static int parseShowReleaseWeekDay(String day) {
        if (day == null || day.length() == 0) {
            return -1;
        }

        // catch Monday through Sunday
        switch (day) {
            case "Monday":
                return DateTimeConstants.MONDAY;
            case "Tuesday":
                return DateTimeConstants.TUESDAY;
            case "Wednesday":
                return DateTimeConstants.WEDNESDAY;
            case "Thursday":
                return DateTimeConstants.THURSDAY;
            case "Friday":
                return DateTimeConstants.FRIDAY;
            case "Saturday":
                return DateTimeConstants.SATURDAY;
            case "Sunday":
                return DateTimeConstants.SUNDAY;
            case "Daily":
                return 0;
        }

        // no match
        return -1;
    }

    /**
     * Parses a {@link DateTime} to its ISO datetime string representation (in UTC).
     */
    public static String parseShowFirstRelease(@Nullable DateTime dateTime) {
        return dateTime == null ? "" : DATE_TIME_FORMATTER_UTC.print(dateTime);
    }


    /**
     * Creates the show release time from a
     * encoded value.
     *
     * <p> If the encoded time passed is not from 0 to 2359 or the encoded minute is larger than 59,
     * a sensible default is returned.
     */
    public static LocalTime getShowReleaseTime(int showReleaseTime) {
        if (showReleaseTime >= 0 && showReleaseTime <= 2359) {
            int hour = showReleaseTime / 100;
            int minute = showReleaseTime - (hour * 100);
            if (minute <= 59) {
                return new LocalTime(hour, minute);
            }
        }

        // if no time is available, use a sensible default
        return new LocalTime(7, 0);
    }

    /**
     * Calculates the current release date time. Adjusts for time zone effects on release time, e.g.
     * delays between time zones (e.g. in the United States) and DST. Adjusts for user-defined
     * offset.
     *
     * @param time See {@link #getShowReleaseTime(int)}.
     * @return The date is today or on the next day matching the given week day.
     */
    public static Date getShowReleaseDateTime(@NonNull Context context, @NonNull LocalTime time,
            int weekDay, @Nullable String timeZone, @Nullable String country) {
        // determine show time zone (falls back to America/New_York)
        DateTimeZone showTimeZone = getDateTimeZone(timeZone);

        // create current date in show time zone, set local show release time
        LocalDateTime localDateTime = new LocalDate(showTimeZone).toLocalDateTime(time);

        // adjust day of week so datetime is today or within the next week
        // for daily shows (weekDay == 0) just use the current day
        if (weekDay >= 1 && weekDay <= 7) {
            // joda tries to preserve week
            // so if we want a week day earlier in the week, advance by 7 days first
            if (weekDay < localDateTime.getDayOfWeek()) {
                localDateTime = localDateTime.plusWeeks(1);
            }
            localDateTime = localDateTime.withDayOfWeek(weekDay);
        }

        localDateTime = handleHourPastMidnight(country, localDateTime);
        localDateTime = handleDstGap(showTimeZone, localDateTime);

        DateTime dateTime = localDateTime.toDateTime(showTimeZone);

        // handle time zone effects on release time for US shows (only if device is set to US zone)
        String localTimeZone = TimeZone.getDefault().getID();
        if (localTimeZone.startsWith(TIMEZONE_ID_PREFIX_AMERICA)) {
            dateTime = applyUnitedStatesCorrections(country, localTimeZone, dateTime);
        }

        return dateTime.toDate();
    }

    /**
     * If the release time is within the hour past midnight (0:00 until 0:59) moves the date one day
     * into the future (currently US shows only).
     *
     * <p> This is based on late night shows being commonly listed as releasing the day before if
     * they air past midnight (e.g. "Monday night at 0:35" actually is Tuesday 0:35).
     *
     * <p>Example: https://thetvdb.com/?tab=series&id=292421
     *
     * <p>See also: https://forums.thetvdb.com/viewtopic.php?t=22791
     */
    private static LocalDateTime handleHourPastMidnight(@Nullable String country,
            LocalDateTime localDateTime) {
        // Example:
        if (ISO3166_1_UNITED_STATES.equals(country) && localDateTime.getHourOfDay() == 0) {
            return localDateTime.plusDays(1);
        }
        return localDateTime;
    }

    /**
     * Calculate the year string of a show's first release in the user's default locale from an ISO
     * date time string.
     *
     * @return Returns {@code null} if the given date time is empty.
     */
    public static String getShowReleaseYear(@Nullable String releaseDateTime) {
        if (releaseDateTime == null || releaseDateTime.length() == 0) {
            return null;
        }

        DateTime dateTime;

        try {
            dateTime = DATE_TIME_FORMATTER_UTC.parseDateTime(releaseDateTime);
        } catch (IllegalArgumentException ignored) {
            // legacy format, or otherwise invalid
            try {
                // try legacy date only parser
                dateTime = TVDB_DATE_FORMATTER.parseDateTime(releaseDateTime);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        return new SimpleDateFormat("yyyy", Locale.getDefault()).format(dateTime.toDate());
    }

    private static DateTime applyUnitedStatesCorrections(@Nullable String country,
            @NonNull String localTimeZone, @NonNull DateTime dateTime) {
        // assumed base time zone for US shows by trakt is America/New_York
        // EST UTC−5:00, EDT UTC−4:00

        // east feed (default): simultaneously in Eastern and Central
        // delayed 1 hour in Mountain
        // delayed three hours in Pacific
        // <==>
        // same local time in Eastern + Pacific (e.g. 20:00)
        // same local time in Central + Mountain (e.g. 19:00)

        // not a US show or no correction necessary (getting east feed)
        if (!ISO3166_1_UNITED_STATES.equals(country)
                || localTimeZone.equals(TIMEZONE_ID_US_EASTERN)
                || localTimeZone.equals(TIMEZONE_ID_US_EASTERN_DETROIT)
                || localTimeZone.equals(TIMEZONE_ID_US_CENTRAL)) {
            return dateTime;
        }

        int offset = 0;
        if (localTimeZone.equals(TIMEZONE_ID_US_MOUNTAIN)) {
            // MST UTC−7:00, MDT UTC−6:00
            offset += 1;
        } else if (localTimeZone.equals(TIMEZONE_ID_US_ARIZONA)) {
            // is always UTC-07:00, so like Mountain, but no DST
            boolean noDstInEastern = DateTimeZone.forID(TIMEZONE_ID_US_EASTERN)
                    .isStandardOffset(dateTime.getMillis());
            if (noDstInEastern) {
                offset += 1;
            } else {
                offset += 2;
            }
        } else if (localTimeZone.equals(TIMEZONE_ID_US_PACIFIC)) {
            // PST UTC−8:00 or PDT UTC−7:00
            offset += 3;
        }

        dateTime = dateTime.plusHours(offset);

        return dateTime;
    }

    /**
     * Handles DST gap (typically a missing clock hour when DST is getting enabled) by moving the
     * time forward in hour increments until the local date time is outside the gap.
     */
    private static LocalDateTime handleDstGap(DateTimeZone showTimeZone,
            LocalDateTime localDateTime) {
        while (showTimeZone.isLocalDateTimeGap(localDateTime)) {
            // move time forward in 1 hour increments, until outside of the gap
            localDateTime = localDateTime.plusHours(1);
        }
        return localDateTime;
    }

    /**
     * Formats to the week day abbreviation (e.g. "Mon") as defined by the devices locale.
     */
    public static String formatToLocalDay(Date dateTime) {
        SimpleDateFormat localDayFormat = new SimpleDateFormat("E", Locale.getDefault());
        return localDayFormat.format(dateTime);
    }

    /**
     * Formats to the week day abbreviation (e.g. "Mon") as defined by the devices locale. If the
     * given weekDay is 0, returns the local version of "Daily".
     */
    public static String formatToLocalDayOrDaily(Context context, Date dateTime, int weekDay) {
        if (weekDay == RELEASE_WEEKDAY_DAILY) {
            return context.getString(R.string.daily);
        }
        return formatToLocalDay(dateTime);
    }

    /**
     * Formats to absolute time format (e.g. "08:00 PM") as defined by the devices locale.
     */
    public static String formatToLocalTime(Context context, Date dateTime) {
        java.text.DateFormat localTimeFormat = DateFormat.getTimeFormat(context);
        return localTimeFormat.format(dateTime);
    }

    /**
     * Formats to relative time in relation to the current system time (e.g. "in 12 min") as defined
     * by the devices locale. If the time difference is lower than a minute, returns the localized
     * equivalent of "now".
     */
    public static String formatToLocalRelativeTime(Context context, Date dateTime) {
        long now = System.currentTimeMillis();
        long dateTimeInstant = dateTime.getTime();

        // if we are below the resolution of getRelativeTimeSpanString, return "now"
        if (Math.abs(now - dateTimeInstant) < DateUtils.MINUTE_IN_MILLIS) {
            return context.getString(R.string.now);
        }

        return DateUtils
                .getRelativeTimeSpanString(dateTimeInstant, now, DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_ALL).toString();
    }

    /**
     * Formats to day and relative time in relation to the current system time (e.g. "Mon in 3
     * days") as defined by the devices locale. If the time is today, returns the local equivalent
     * for "today".
     */
    public static String formatToLocalDayAndRelativeTime(Context context, Date dateTime) {
        StringBuilder dayAndTime = new StringBuilder();

        // day abbreviation, e.g. "Mon"
        dayAndTime.append(formatToLocalDay(dateTime));
        dayAndTime.append(" ");

        // relative time to dateTime, "today" or e.g. "3 days ago"
        if (DateUtils.isToday(dateTime.getTime())) {
            // show 'today' instead of '0 days ago'
            dayAndTime.append(context.getString(R.string.today));
        } else {
            dayAndTime.append(DateUtils
                    .getRelativeTimeSpanString(
                            dateTime.getTime(),
                            System.currentTimeMillis(),
                            DateUtils.DAY_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL));
        }

        return dayAndTime.toString();
    }

    /**
     * Formats to a date, time zone and week day (e.g. "2014/02/04 CET (Mon)") as defined by the
     * devices locale. If the date time is today, uses the local equivalent of "today" instead of a
     * week day.
     */
    public static String formatToLocalDateAndDay(Context context, Date dateTime) {
        StringBuilder date = new StringBuilder();

        // date, e.g. "2014/05/31"
        date.append(DateFormat.getDateFormat(context).format(dateTime));
        date.append(" ");

        // device time zone, e.g. "CEST"
        TimeZone timeZone = TimeZone.getDefault();
        date.append(timeZone.getDisplayName(timeZone.inDaylightTime(dateTime), TimeZone.SHORT,
                Locale.getDefault()));

        //
        // Show 'today' instead of e.g. 'Mon'
        date.append(" (");
        if (DateUtils.isToday(dateTime.getTime())) {
            date.append(context.getString(R.string.today));
        } else {
            date.append(formatToLocalDay(dateTime));
        }

        return date.append(")").toString();
    }
}