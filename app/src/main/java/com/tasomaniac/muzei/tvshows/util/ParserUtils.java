package com.tasomaniac.muzei.tvshows.util;

import android.content.ContentProvider;
import android.net.Uri;

import java.util.regex.Pattern;

public class ParserUtils {

    /** Used to sanitize a string to be {@link Uri} safe. */
    private static final Pattern sSanitizePattern = Pattern.compile("[^a-z0-9-_]");

    /**
     * Sanitize the given string to be {@link Uri} safe for building
     * {@link ContentProvider} paths.
     */
    public static String sanitizeId(String input) {
        if (input == null) {
            return null;
        }
        return sSanitizePattern.matcher(input.toLowerCase()).replaceAll("");
    }

}