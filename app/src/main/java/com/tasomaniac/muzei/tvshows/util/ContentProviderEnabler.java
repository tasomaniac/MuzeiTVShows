/*
 * Copyright (C) 2010 The Android Open Source Project
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
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.tasomaniac.muzei.tvshows.ui.IntegrationPreference;

public final class ContentProviderEnabler {

    private final Context mContext;
    @Nullable private IntegrationPreference mPref;

    private final ContentObserver mObserver = new ContentObserver(new Handler()) {

        @Override
        public void onChange(boolean selfChange) {
            handleStateChanged();
        }
    };

    public ContentProviderEnabler(@NonNull Context context,
                                  @Nullable IntegrationPreference pref) {
        mContext = context;
        mPref = pref;
    }

    public void resume() {

        handleStateChanged();

        if (mPref != null) {
            mContext.getContentResolver().registerContentObserver(Uri.parse(mPref.getExpectedContentUri()),
                    false, mObserver);
        }
    }

    public void pause() {
        if (mPref != null) {
            mContext.getContentResolver().unregisterContentObserver(mObserver);
        }
    }

    void handleStateChanged() {
        if (mPref != null) {
            mPref.checkState();
        }
    }

}
