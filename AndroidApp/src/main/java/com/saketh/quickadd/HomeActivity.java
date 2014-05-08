package com.saketh.quickadd;

import android.accounts.AccountManager;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.accounts.GoogleAccountManager;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.TimePeriod;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class HomeActivity extends Activity {
  static final String TAG = "QuickAdd";
  static final String PREFS_NAME = TAG;

  static final int REQUEST_GOOGLE_PLAY_SERVICES = 0;
  static final int REQUEST_AUTHORIZATION = 1;
  static final int REQUEST_ACCOUNT_PICKER = 2;

  private static final String KEY_MODEL = "key_model";
  private static final String KEY_PREF_ACCOUNT_NAME = "key_pref_account_name";
  private static final String KEY_PREF_CALENDAR_ID = "key_pref_calendar_id";

  final HttpTransport transport = AndroidHttp.newCompatibleTransport();
  final JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
  com.google.api.services.calendar.Calendar client;
  GoogleAccountCredential credential;
  CalendarModel model = new CalendarModel();

  private ConnectivityReceiver mConnectivityReceiver;
  private ProgressBar mProgressBar;
  private ImageView mAddEvent;
  private ProgressBar mAddEventSpinner;
  private ImageView mCalendar;
  private ProgressBar mFreeBusySpinner;
  private EditText mEventText;
  private TextView mCalendarText;
  private LinearLayout mAddEventLayout;
  private LinearLayout mEventExamplesLayout;
  private LinearLayout mCalendarLayout;
  private LinearLayout mFreeBusyLayout;
  private LinearLayout mNoConnectionLayout;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_home);
    initActivity();
    credential = GoogleAccountCredential.usingOAuth2(this,
        Collections.singleton(CalendarScopes.CALENDAR));
    String prefAccount = getPreferredAccountName();
    if (prefAccount != null) credential.setSelectedAccountName(prefAccount);
    // Calendar client
    client = new com.google.api.services.calendar.Calendar.Builder(
        transport, jsonFactory, credential).setApplicationName("QuickAdd/1.0").build();
    CalendarModel storedModel = getModel();
    if (storedModel != null) model = storedModel;
  }

  private void initActivity() {
    mConnectivityReceiver = new ConnectivityReceiver();
    registerReceiver(mConnectivityReceiver,
        new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    mEventText = (EditText) findViewById(R.id.editTextEvent);
    mAddEvent = (ImageView) findViewById(R.id.buttonAddEvent);
    mAddEvent.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (isConnected()) {
          AsyncAddEvent.run(HomeActivity.this, mEventText.getText().toString());
        } else {
          mAddEventLayout.startAnimation(
              AnimationUtils.loadAnimation(HomeActivity.this, R.anim.shake));
          showToast(getString(R.string.toast_no_connectivity));
        }
      }
    });
    mAddEventSpinner = (ProgressBar) findViewById(R.id.progressBarAddEvent);
    mAddEventLayout = (LinearLayout) findViewById(R.id.linearLayoutEvent);
    mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
    mCalendarText = (TextView) findViewById(R.id.textViewCalendar);
    mCalendarLayout = (LinearLayout) findViewById(R.id.linearLayoutCalendar);
    mCalendarLayout.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        selectCalendar(getPreferredCalendarId());
      }
    });
    mEventExamplesLayout = (LinearLayout) findViewById(R.id.linearLayoutExamples);
    String[] examples = getResources().getStringArray(R.array.event_examples);
    LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    for (int i = 0; i < examples.length; i++) {
      View exampleView = inflater.inflate(R.layout.listview_example, mEventExamplesLayout, false);
      TextView textViewExample = (TextView) exampleView.findViewById(R.id.textViewExample);
      textViewExample.setText(examples[i]);
      textViewExample.setOnClickListener(mExampleOnClickListener);
      mEventExamplesLayout.addView(exampleView);
      if (i < examples.length - 1) {
        View divider = inflater.inflate(R.layout.listview_example_divider, mEventExamplesLayout, false);
        mEventExamplesLayout.addView(divider);
      }
    }
    mNoConnectionLayout = (LinearLayout) findViewById(R.id.linearLayoutNoConnection);
    mFreeBusyLayout = (LinearLayout) findViewById(R.id.linearLayoutFreeBusy);
    mCalendar = (ImageView) findViewById(R.id.imageViewCalendar);
    mFreeBusySpinner = (ProgressBar) findViewById(R.id.progressBarFreeBusy);
  }

  private View.OnClickListener mExampleOnClickListener = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      mEventText.setText(((TextView) v).getText());
    }
  };

  private void refreshViews(boolean change) {
    refreshCards();
    final CalendarInfo cal = model.get(getPreferredCalendarId());
    if (cal == null) {
      mCalendarLayout.startAnimation(AnimationUtils.loadAnimation(this, R.anim.push_right_out));
      mCalendarLayout.setVisibility(View.GONE);
    } else {
      if (mCalendarLayout.getVisibility() == View.GONE) {
        mCalendarText.setText(cal.summary);
        mCalendarLayout.setVisibility(View.VISIBLE);
        mCalendarLayout.startAnimation(
            AnimationUtils.loadAnimation(HomeActivity.this, R.anim.push_right_in));
      } else {
        if (change) {
          Animation out = AnimationUtils.loadAnimation(this, R.anim.push_right_out);
          out.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
              mCalendarText.setText(cal.summary);
              mCalendarLayout.startAnimation(
                  AnimationUtils.loadAnimation(HomeActivity.this, R.anim.push_right_in));
            }

            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

          });
          mCalendarLayout.startAnimation(out);
        }
      }
      mCalendarText.setText(cal.summary);
    }
  }

  private void refreshCards() {
    mNoConnectionLayout.setVisibility(isConnected() ? View.GONE : View.VISIBLE);
    mAddEventLayout.setVisibility(model.size() == 0 ? View.GONE : View.VISIBLE);
    mEventExamplesLayout.setVisibility(model.size() == 0 ? View.GONE : View.VISIBLE);
  }

  void showGooglePlayServicesAvailabilityErrorDialog(final int connectionStatusCode) {
    runOnUiThread(new Runnable() {
      public void run() {
        Dialog dialog = GooglePlayServicesUtil.getErrorDialog(
            connectionStatusCode, HomeActivity.this, REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
      }
    });
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (checkGooglePlayServicesAvailable()) {
      haveGooglePlayServices();
    }
    refreshViews(false);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    switch (requestCode) {
      case REQUEST_GOOGLE_PLAY_SERVICES:
        if (resultCode == Activity.RESULT_OK) {
          haveGooglePlayServices();
        } else {
          checkGooglePlayServicesAvailable();
        }
        break;
      case REQUEST_ACCOUNT_PICKER:
        if (resultCode == Activity.RESULT_OK && data != null && data.getExtras() != null) {
          String accountName = data.getExtras().getString(AccountManager.KEY_ACCOUNT_NAME);
          if (accountName != null) {
            credential.setSelectedAccountName(accountName);
            setPreferredAccountName(accountName);
            if(isConnected() && model.size() == 0) AsyncLoadCalendars.run(this);
          }
        }
        break;
      case REQUEST_AUTHORIZATION:
        if (resultCode == Activity.RESULT_OK) {
          if (isConnected() && model.size() == 0) AsyncLoadCalendars.run(this);
        } else {
          chooseAccount();
        }
        break;
    }
  }

  /**
   * Check that Google Play services APK is installed and up to date.
   */
  private boolean checkGooglePlayServicesAvailable() {
    final int connectionStatusCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
    if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
      showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
      return false;
    }
    return true;
  }

  private void haveGooglePlayServices() {
    Log.d(TAG, "haveGooglePlayServices");
    // check if there is already an account selected
    if (credential.getSelectedAccountName() == null) {
      // ask user to choose account
      chooseAccount();
    } else {
      // load calendars
      if (isConnected()) {
        AsyncLoadCalendars.run(this);
        AsyncGetBusyTimes.run(this, getPreferredCalendarId());
      }
    }
  }

  private void chooseAccount() {
    startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.home, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();
    switch (id) {
      case R.id.action_open_cal:
        openCalendar();
        return true;
      case R.id.action_select_cal:
        selectCalendar(getPreferredCalendarId());
        return true;
      case R.id.action_about:
        openAbout();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  private void openCalendar() {
    // A date-time specified in milliseconds since the epoch.
    long startMillis = System.currentTimeMillis();
    Uri.Builder builder = CalendarContract.CONTENT_URI.buildUpon();
    builder.appendPath("time");
    ContentUris.appendId(builder, startMillis);
    Intent intent = new Intent(Intent.ACTION_VIEW)
        .setData(builder.build());
    startActivity(intent);
  }

  private void selectCalendar(String prefCalId) {
    CalendarInfo[] calendars = model.toSortedArray();
    final ListAdapter adapter = new ArrayAdapter<CalendarInfo>(this,
        android.R.layout.simple_list_item_single_choice, calendars);
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(R.string.title_dialog_select_cal);
    builder.setCancelable(false);
    int selectedItem = 0;
    for (int i = 0; i < calendars.length; i++) {
      if (calendars[i].id.equals(prefCalId)) selectedItem = i;
    }

    builder.setSingleChoiceItems(adapter, selectedItem,
        new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            changePreferredCalId(((CalendarInfo) adapter.getItem(which)).id);
            dialog.dismiss();
            refreshViews(true);
          }
        }
    );
    builder.show();
  }

  private void openAbout() {
    startActivity(new Intent(HomeActivity.this, AboutActivity.class));
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    unregisterReceiver(mConnectivityReceiver);
    if (model.size() != 0) setModel(model);
  }

  void showLoadingCalendarsActivity(final boolean visible) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (visible) {
          showToast(getString(R.string.toast_fetching_calendars));
          mProgressBar.setVisibility(View.VISIBLE);
        } else {
          mProgressBar.setVisibility(View.INVISIBLE);
          synchronized (model) {
            if (model.size() > 0 && !model.containsKey(getPreferredCalendarId())) {
              selectCalendar(getPreferredCalendarId());
            }
          }
        }
        refreshViews(false);
      }
    });

  }

  void showAddEventActivity(final boolean active, final boolean success) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (active) {
          mAddEvent.setVisibility(View.GONE);
          mAddEventSpinner.setVisibility(View.VISIBLE);
        } else {
          mAddEventSpinner.setVisibility(View.GONE);
          mAddEvent.setVisibility(View.VISIBLE);
          if (success) showToast(getString(R.string.toast_add_event_success));
          else showToast(getString(R.string.toast_add_event_fail));
          mEventText.setText(null);
          AsyncGetBusyTimes.run(HomeActivity.this, getPreferredCalendarId());
        }
      }
    });
  }

  void showFreeBusyActivity(final boolean active, final String[] events) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (active) {
          mFreeBusyLayout.setVisibility(View.GONE);
          mFreeBusySpinner.setVisibility(View.VISIBLE);
        } else {
          mFreeBusyLayout.setVisibility(View.VISIBLE);
          mFreeBusySpinner.setVisibility(View.GONE);
          int children = mFreeBusyLayout.getChildCount();
          logd("No of children " + children);
          if (children > 2) mFreeBusyLayout.removeViews(2, children-2);
          TextView textStatus = (TextView) mFreeBusyLayout.getChildAt(1);
          if (events == null || events.length == 0) {
            textStatus.setText(R.string.status_free_schedule);
            getActionBar().setSplitBackgroundDrawable(new ColorDrawable(Color.GREEN));
          } else {
            textStatus.setText(R.string.status_busy_schedule);
            getActionBar().setStackedBackgroundDrawable(new ColorDrawable(Color.RED));
          }
          for (String event : events) {
            final View busy = inflater.inflate(R.layout.busy, mFreeBusyLayout, false);
            TextView textPeriod = (TextView) busy.findViewById(R.id.timePeriod);
            textPeriod.setText(event);
            mFreeBusyLayout.addView(busy);
          }

        }
      }
    });
  }

  void changePreferredCalId(String calId) {
    setPreferredCalId(calId);
    Log.d(TAG, getPreferredCalendarId() + " set as preferred calendar");
    if (getPreferredCalendarId() != null) {
      AsyncGetBusyTimes.run(HomeActivity.this, getPreferredCalendarId());
    }
  }

  String getPreferredAccountName() {
    SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    return settings.getString(KEY_PREF_ACCOUNT_NAME, null);
  }

  void setPreferredAccountName(String name) {
    SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
    editor.putString(KEY_PREF_ACCOUNT_NAME, name).commit();
  }

  String getPreferredCalendarId() {
    SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    return settings.getString(KEY_PREF_CALENDAR_ID, null);
  }

  void setPreferredCalId(String calId) {
    SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
    editor.putString(KEY_PREF_CALENDAR_ID, calId).commit();
  }

  CalendarModel getModel() {
    SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    Gson gson = new Gson();
    String json = settings.getString(KEY_MODEL, null);
    return gson.fromJson(json, CalendarModel.class);
  }

  void setModel(CalendarModel model) {
    SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
    Gson gson = new Gson();
    String json = gson.toJson(model);
    editor.putString(KEY_MODEL, json).commit();
  }

  boolean isConnected() {
    ConnectivityManager mgr = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
    NetworkInfo info = mgr.getActiveNetworkInfo();
    return (info != null && info.isConnected());
  }

  private class ConnectivityReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      if (!ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) return;
      if (isConnected() && credential.getSelectedAccountName() != null && model.size() == 0) {
        AsyncLoadCalendars.run(HomeActivity.this);
      }
      refreshCards();
    }
  }

  void showToast(String msg) {
    Toast.makeText(HomeActivity.this, msg, Toast.LENGTH_SHORT).show();
  }

  private void logd(String str) {
    Log.d("QuickAdd", str);
  }
}
