/*
 * Copyright (c) 2012 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.saketh.quickadd;


import com.google.api.services.calendar.model.CalendarList;

import java.io.IOException;

/**
 * Asynchronously load the calendars.
 *
 * @author Yaniv Inbar
 */
class AsyncLoadCalendars extends CalendarAsyncTask {
    private static Boolean sAlive = false;

    AsyncLoadCalendars(HomeActivity calendarSample) {
        super(calendarSample);
    }

    static void run(HomeActivity activity) {
        activity.logd("Async Run");
        synchronized (sAlive) {
            if (!sAlive) {
                activity.logd("New Async Run");
                new AsyncLoadCalendars(activity).execute();
                sAlive = true;
                activity.showLoadingCalendarsActivity(true);
            } else {
                activity.logd("Async run Cancelled");
            }
        }
    }

    @Override
    protected void doInBackground() throws IOException {
        activity.logd("Asyncload  doInBackground");
        CalendarList feed = client.calendarList().list().setFields(CalendarInfo.FEED_FIELDS)
                .execute();
        model.reset(feed.getItems());
        activity.logd("Size - " + model.toSortedArray().length);
        for (CalendarInfo cal : model.toSortedArray()) {
            activity.logd(cal.id);
        }
        sAlive = false;
        activity.showLoadingCalendarsActivity(false);
    }

    @Override
    protected void doInBackgroundError() {
        sAlive = false;
        activity.showLoadingCalendarsActivity(false);
    }
}
