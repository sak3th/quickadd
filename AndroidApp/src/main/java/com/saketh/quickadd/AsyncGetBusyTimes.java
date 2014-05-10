package com.saketh.quickadd;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.FreeBusyRequest;
import com.google.api.services.calendar.model.FreeBusyRequestItem;
import com.google.api.services.calendar.model.FreeBusyResponse;
import com.google.api.services.calendar.model.TimePeriod;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AsyncGetBusyTimes extends CalendarAsyncTask {

    private static long TIME_PERIOD_MILLIS = 24 * 60 * 60 * 1000;
    String mCalendarId;

    AsyncGetBusyTimes(HomeActivity activity, String calId) {
        super(activity);
        mCalendarId = calId;
    }

    static void run(HomeActivity activity, String calId) {
        if (calId != null) {
            AsyncGetBusyTimes asyncTask = new AsyncGetBusyTimes(activity, calId);
            asyncTask.execute();
            activity.showFreeBusyActivity(true, null);
        }
    }

    @Override
    protected void doInBackground() throws IOException {
        FreeBusyRequest request = new FreeBusyRequest();
        ArrayList<FreeBusyRequestItem> items = new ArrayList<FreeBusyRequestItem>();
        String calId = mCalendarId;
        items.add(new FreeBusyRequestItem().setId(mCalendarId));
        request.setItems(items);
        Date now = new Date();
        request.setTimeMin(new DateTime(now));
        request.setTimeMax(new DateTime(now.getTime() + (TIME_PERIOD_MILLIS)));
        com.google.api.services.calendar.Calendar.Freebusy.Query query = client.freebusy().query
                (request);
        FreeBusyResponse response = query.execute();
        List<TimePeriod> periods = response.getCalendars().get(calId).getBusy();
        List<Event> events = client.events().list(mCalendarId).setTimeMin(
                request.getTimeMin()).setTimeMax(request.getTimeMax()).execute().getItems();
        activity.logd(" Events " + events.size());
        String[] eventStrings = new String[events.size()];
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        for (int i = 0; i < events.size(); i++) {
            Event event = events.get(i);
            DateTime start = event.getStart().getDateTime();
            DateTime end = event.getEnd().getDateTime();
            if (start == null || end == null) {
                eventStrings[i] = event.getSummary();
            } else {
                eventStrings[i] = event.getSummary() + "  " +
                        sdf.format(start.getValue()) + " - " +
                        sdf.format(end.getValue());
            }
        }
        activity.showFreeBusyActivity(false, eventStrings);
    }

    @Override
    protected void doInBackgroundError() {
        activity.showFreeBusyActivity(false, null);
    }
}
