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

import android.os.AsyncTask;

import com.google.api.client.googleapis.extensions.android.gms.auth
        .GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

import java.io.IOException;


abstract class CalendarAsyncTask extends AsyncTask<Void, Void, Boolean> {

    final HomeActivity activity;
    final CalendarModel model;
    final com.google.api.services.calendar.Calendar client;

    CalendarAsyncTask(HomeActivity activity) {
        this.activity = activity;
        model = activity.model;
        client = activity.client;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected final Boolean doInBackground(Void... ignored) {
        try {
            doInBackground();
            return true;
        } catch (final GooglePlayServicesAvailabilityIOException availabilityException) {
            doInBackgroundError();
            activity.showGooglePlayServicesAvailabilityErrorDialog(
                    availabilityException.getConnectionStatusCode());
        } catch (UserRecoverableAuthIOException userRecoverableException) {
            doInBackgroundError();
            activity.startActivityForResult(
                    userRecoverableException.getIntent(), HomeActivity.REQUEST_AUTHORIZATION);
        } catch (IOException e) {
            doInBackgroundError();
            Utils.logAndShow(activity, HomeActivity.TAG, e);
        }
        return false;
    }

    @Override
    protected final void onPostExecute(Boolean success) {
        super.onPostExecute(success);
    }

    abstract protected void doInBackground() throws IOException;

    abstract protected void doInBackgroundError();
}
