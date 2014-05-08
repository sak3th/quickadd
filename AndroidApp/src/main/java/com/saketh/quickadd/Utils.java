package com.saketh.quickadd;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;

public class Utils {
  /**
   * Inserts a drawable with today's day into the today's icon in the option menu
   * @param icon - today's icon from the options menu
   */
  public static void setTodayIcon(LayerDrawable icon, Context c, String timezone) {
    DayOfMonthDrawable today;
    // Reuse current drawable if possible
    Drawable currentDrawable = icon.findDrawableByLayerId(R.id.today_icon_day);
    if (currentDrawable != null && currentDrawable instanceof DayOfMonthDrawable) {
      today = (DayOfMonthDrawable)currentDrawable;
    } else {
      today = new DayOfMonthDrawable(c);
    }
    // Set the day and update the icon
    Time now =  new Time(timezone);
    now.setToNow();
    now.normalize(false);
    today.setDayOfMonth(now.monthDay);
    icon.mutate();
    icon.setDrawableByLayerId(R.id.today_icon_day, today);
  }

  /**
   * Logs the given throwable and shows an error alert dialog with its message.
   *
   * @param activity activity
   * @param tag log tag to use
   * @param t throwable to log and show
   */
  public static void logAndShow(Activity activity, String tag, Throwable t) {
    Log.e(tag, "Error", t);
    String message = t.getMessage();
    if (t instanceof GoogleJsonResponseException) {
      GoogleJsonError details = ((GoogleJsonResponseException) t).getDetails();
      if (details != null) {
        message = details.getMessage();
      }
    } else if (t.getCause() instanceof GoogleAuthException) {
      message = ((GoogleAuthException) t.getCause()).getMessage();
    }
    showError(activity, message);
  }

  /**
   * Logs the given message and shows an error alert dialog with it.
   *
   * @param activity activity
   * @param tag log tag to use
   * @param message message to log and show or {@code null} for none
   */
  public static void logAndShowError(Activity activity, String tag, String message) {
    String errorMessage = getErrorMessage(activity, message);
    Log.e(tag, errorMessage);
    showErrorInternal(activity, errorMessage);
  }

  /**
   * Shows an error alert dialog with the given message.
   *
   * @param activity activity
   * @param message message to show or {@code null} for none
   */
  public static void showError(Activity activity, String message) {
    String errorMessage = getErrorMessage(activity, message);
    //showErrorInternal(activity, errorMessage);
  }

  private static void showErrorInternal(final Activity activity, final String errorMessage) {
    activity.runOnUiThread(new Runnable() {
      public void run() {
        Toast.makeText(activity, errorMessage, Toast.LENGTH_LONG).show();
      }
    });
  }

  private static String getErrorMessage(Activity activity, String message) {
//    Resources resources = activity.getResources();
//    if (message == null) {
//      return resources.getString(R.string.error);
//    }
//    return resources.getString(R.string.error_format, message);
    return null;
  }
}
