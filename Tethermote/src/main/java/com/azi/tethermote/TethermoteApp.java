package com.azi.tethermote;

/*
 * Copyright Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.Application;

//import com.google.android.gms.analytics.ExceptionReporter;
//import com.google.android.gms.analytics.GoogleAnalytics;
//import com.google.android.gms.analytics.HitBuilders;
//import com.google.android.gms.analytics.StandardExceptionParser;
//import com.google.android.gms.analytics.Tracker;

class TethermoteApp extends Application {
//    private Tracker mTracker;

    @Override
    public void onCreate() {
        super.onCreate();

//        Thread.UncaughtExceptionHandler myHandler = new ExceptionReporter(
//                getDefaultTracker(),
//                Thread.getDefaultUncaughtExceptionHandler(),
//                this);
//
//// Make myHandler the new default uncaught exception handler.
//        Thread.setDefaultUncaughtExceptionHandler(myHandler);
    }

    //    synchronized public Tracker getDefaultTracker() {
//        if (mTracker == null) {
//            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
//            // To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
//            mTracker = analytics.newTracker("UA-76982704-1");
//        }
//        return mTracker;
//    }
    public void sendException(Exception e) {
//        getDefaultTracker().send(new HitBuilders.ExceptionBuilder()
//                .setDescription(new StandardExceptionParser(this, null)
//                        .getDescription(Thread.currentThread().getName(), e))
//                .setFatal(false)
//                .build()
//        );
    }
}
