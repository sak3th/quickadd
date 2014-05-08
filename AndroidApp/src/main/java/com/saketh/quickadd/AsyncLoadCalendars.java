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

import android.util.Log;

import com.google.api.services.calendar.model.CalendarList;

import java.io.IOException;

/**
 * Asynchronously load the calendars.
 * 
 * @author Yaniv Inbar
 */
class AsyncLoadCalendars extends CalendarAsyncTask {
  private static boolean sAlive = false;
  AsyncLoadCalendars(HomeActivity calendarSample) {
    super(calendarSample);
  }

  @Override
  protected void doInBackground() throws IOException {
    CalendarList feed = client.calendarList().list().setFields(CalendarInfo.FEED_FIELDS).execute();
    model.reset(feed.getItems());
    Log.d("QuickAdd", "Size - " + model.toSortedArray().length);
    for (CalendarInfo cal : model.toSortedArray()) {
      Log.d("QuickAdd", cal.id);
    }
    sAlive = false;
    activity.showLoadingCalendarsActivity(false);
  }

    @Override
    protected void doInBackgroundError() {
        sAlive = false;
        activity.showLoadingCalendarsActivity(false);
    }

    static void run(HomeActivity activity) {
    if (!sAlive) {
      new AsyncLoadCalendars(activity).execute();
      sAlive = true;
      activity.showLoadingCalendarsActivity(true);
    }
  }
}
