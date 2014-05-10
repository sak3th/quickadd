package com.saketh.quickadd;

import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;

import java.io.IOException;

class AsyncAddEvent extends CalendarAsyncTask {

    private String mEvent;

    AsyncAddEvent(HomeActivity activity) {
        super(activity);
    }

    static void run(HomeActivity activity, String event) {
        AsyncAddEvent addEvent = new AsyncAddEvent(activity);
        addEvent.setEvent(event);
        addEvent.execute();
        activity.showAddEventActivity(true, false);
    }

    @Override
    protected void doInBackground() throws IOException {
        String calId = activity.getPreferredCalendarId();
        //mEvent = "Appointment at Somewhere on June 3rd 10am-10:25am";
        Calendar.Events.QuickAdd quickAdd = null;
        Event createdEvent = null;

        quickAdd = client.events().quickAdd(calId, mEvent);
        createdEvent = quickAdd.execute();
        activity.logd("adding event " + createdEvent.getId());
        activity.showAddEventActivity(false, true);
    }

    @Override
    protected void doInBackgroundError() {
        activity.showAddEventActivity(false, true);
    }

    public void setEvent(String mEvent) {
        this.mEvent = mEvent;
    }
}