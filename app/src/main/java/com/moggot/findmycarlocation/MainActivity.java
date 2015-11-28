package com.moggot.findmycarlocation;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import java.util.Calendar;

public class MainActivity extends Activity {

    ImageView img_animation;
    int height;

    int trigger = 0;
    float y1, y2;

    final static String LOG_TAG = "myLogs";
    boolean isLocationSaved;

    private static boolean isAnimation = false;

    int widgetID = AppWidgetManager.INVALID_APPWIDGET_ID;
    Intent resultValue;
    boolean isWidgetInstalled = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        installWidget();
        setContentView(R.layout.activity_main);
        img_animation = (ImageView) findViewById(R.id.ivTrigger);

        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        Tracker t = ((AnalyticsApplication) getApplication())
                .getTracker(AnalyticsApplication.TrackerName.APP_TRACKER);
        t.enableAdvertisingIdCollection(true);

        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        height = displaymetrics.heightPixels;
        if (AppWidgetManager.getInstance(this).getAppWidgetIds(new ComponentName(this, MyWidget.class)).length == 0)
            SharedPreference.SaveInstallWidgetState(this, false);

        int rate_count = SharedPreference.LoadRatingCount(this);
        if (rate_count >= 11) {
            SharedPreference.SaveRatingCount(this, 0);
            showRatingDialog();
        }
    }

    public boolean onTouchEvent(MotionEvent touchevent) {
        if (isAnimation)
            return false;
        switch (touchevent.getAction()) {
            // when user first touches the screen we get x and y coordinate
            case MotionEvent.ACTION_DOWN: {
                y1 = touchevent.getY();
                break;
            }
            case MotionEvent.ACTION_UP: {
                y2 = touchevent.getY();

                if (y1 < y2) {
                    if (trigger == 0) {
                        if (!isLocationSaved) {
                            save_car_location();
                            break;
                        }
                        animationDown(0, height / 9);
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                showMap();
                            }
                        }, 500);
                        break;
                    }
                }

                // if Down to UP sweep event on screen
                if (y1 > y2) {

                    if (trigger == -1) {
                        animationDown(height / 9, 0);
                        trigger = 0;
                        break;
                    }
                    if (trigger == 0) {
                        if (isLocationSaved) {
                            showSaveDialog();
                            break;
                        }
                        saveLocation();
                        animationUP();
                        break;
                    }
                }
            }
            break;

        }
        return false;
    }

    void showSaveDialog() {
        AlertDialog.Builder ad;
        ad = new AlertDialog.Builder(MainActivity.this);

        ad.setTitle(getResources().getString(R.string.dialog_title_save_car));

        ad.setMessage(getResources().getString(R.string.dialog_you_not_find_car));
        ad.setPositiveButton(getResources().getString(R.string.dialog_yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
                saveLocation();
                animationUP();
            }
        });
        ad.setNegativeButton(getResources().getString(R.string.dialog_no), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
            }
        });
        ad.setCancelable(true);
        ad.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
            }
        });
        ad.create();
        ad.show();
    }

    public void showRatingDialog() {

        final AlertDialog.Builder ratingdialog = new AlertDialog.Builder(this);

        ratingdialog.setIcon(android.R.drawable.btn_star_big_on);
        ratingdialog.setTitle(getResources().getString(R.string.rating_title));
        ratingdialog.setMessage(getResources().getString(R.string.rating_text));

        View linearlayout = getLayoutInflater().inflate(R.layout.rating_dialog, null);
        ratingdialog.setView(linearlayout);

        final RatingBar rating = (RatingBar) linearlayout.findViewById(R.id.ratingbar);

        ratingdialog.create();
        final AlertDialog ad = ratingdialog.show();
        rating.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {

            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                Uri uri = Uri.parse("https://play.google.com/store/apps/details?id=com.moggot.findmycarlocation");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                if (!MyStartActivity(intent))
                    return;
                ad.dismiss();
            }
        });
    }

    private boolean MyStartActivity(Intent aIntent) {
        try {
            startActivity(aIntent);
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        }
    }

    private void updateWidget(boolean isLocationSavedValue) {
        if (widgetID == AppWidgetManager.INVALID_APPWIDGET_ID)
            return;
        SharedPreferences sp = getSharedPreferences(MyWidget.WIDGET_PREF, MODE_PRIVATE);
        sp.edit().putBoolean(SharedPreference.s_state_location_save, isLocationSavedValue).apply();
        MyWidget.updateMyWidget(this, AppWidgetManager.getInstance(this), widgetID);
        setResult(RESULT_OK, resultValue);
    }

    private void installWidget() {
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        isLocationSaved = SharedPreference
                .LoadIsLocationSavedState(this);
        if (extras != null) {
            widgetID = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        // и проверяем его корректность
        if (widgetID != AppWidgetManager.INVALID_APPWIDGET_ID) {
            SharedPreference.SaveWidgetID(this, widgetID);
            resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);

            setResult(RESULT_CANCELED, resultValue);
            isWidgetInstalled = SharedPreference.LoadInstallWidgetState(this);
            if (isWidgetInstalled == false) {
                updateWidget(isLocationSaved);
                isWidgetInstalled = true;
                SharedPreference.SaveInstallWidgetState(this, isWidgetInstalled);
                finish();
                return;
            }

            if (isLocationSaved) {
                showMap();
            } else {
                int res = saveLocation();
                if (res == 0)
                    finish();
            }
        }
    }

    void animationUP() {
        TranslateAnimation animation = new TranslateAnimation(0.0f, 0.0f,
                0, -height / 9);

        final int[] count_anim = {0};

        animation.setAnimationListener(new AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
                isAnimation = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                ++count_anim[0];
                if (count_anim[0] == 2)
                    return;
                TranslateAnimation animation_repeate = new TranslateAnimation(0.0f, 0.0f,
                        -height / 9, 0);
                animation_repeate.setDuration(500);
                animation_repeate.setFillAfter(true);
                img_animation.startAnimation(animation_repeate);
                isAnimation = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        animation.setDuration(500);
        animation.setFillAfter(true);
        img_animation.startAnimation(animation);
    }

    void animationDown(float start, float end) {
        TranslateAnimation animation = new TranslateAnimation(0.0f, 0.0f,
                start, end);
        animation.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                Log.i(LOG_TAG, "onAnimationStart");
                isAnimation = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                isAnimation = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        animation.setDuration(500);
        animation.setFillAfter(true);
        img_animation.startAnimation(animation);
    }

    private int saveLocation() {

        Calendar time = Calendar.getInstance();
        int cur_day = time.get(Calendar.DAY_OF_MONTH);
        int cur_hour = time.get(Calendar.HOUR_OF_DAY);
        int cur_minute = time.get(Calendar.MINUTE);
        SharedPreference.SaveTime(this, cur_day, cur_hour,
                cur_minute);

        NetworkManager nwM = new NetworkManager(this);
        nwM.checkLocationSettings();
        String provider = nwM.locationManager.NETWORK_PROVIDER;
        Location location = nwM.getLocation();
        if (location == null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                location = nwM.locationManager.getLastKnownLocation(provider);
            }
        }
        int res = 0;
        if (location != null) {

            isLocationSaved = true;
            SharedPreference.SaveIsLocationSavedState(this, isLocationSaved);
            updateWidget(isLocationSaved);
            SharedPreference.SaveLocation(this,
                    location.getLatitude(),
                    location.getLongitude());
//            SharedPreference.SaveLocation(this, 55.928,
//                    36.520);
            int rate_count = SharedPreference.LoadRatingCount(this);
            ++rate_count;
            SharedPreference.SaveRatingCount(this, rate_count);
            car_loc_save_success();
            res = 0;
        } else {
            res = NetworkManager.LOCATION_NOT_BE_RETRIEVED;
        }
        trigger = 0;
        return res;
    }

    public void showMap() {
        if (isLocationSaved) {
            isAnimation = false;
            trigger = -1;
            Intent intent = new Intent(MainActivity.this,
                    ScreenMap.class);
            startActivityForResult(intent, SharedPreference.ACTIVITY_RESULT_CODE.MAP_SCREEN);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.info) {
            Intent intent = new Intent(MainActivity.this, ScreenInfo.class);
            startActivityForResult(intent, SharedPreference.ACTIVITY_RESULT_CODE.INFO_SCREEN);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        isLocationSaved = SharedPreference.LoadIsLocationSavedState(this);
        final int REQUEST_CHECK_SETTINGS = 199;
        final Handler handler = new Handler();
        switch (requestCode) {
            case SharedPreference.ACTIVITY_RESULT_CODE.MAP_SCREEN:
                if (widgetID == AppWidgetManager.INVALID_APPWIDGET_ID) {
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            animationDown(height / 9, 0);
                        }
                    }, 500);
                }
                trigger = 0;
                updateWidget(isLocationSaved);
                break;
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.i(LOG_TAG, "User agreed to make required location settings changes.");
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                saveLocation();
                                animationUP();
                            }
                        }, 2000);
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i(LOG_TAG, "User chose not to make required location settings changes.");
                        break;
                }
                break;

        }

    }

    private void save_car_location() {
        Toast.makeText(getBaseContext(), R.string.you_should_save_car_location,
                Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        GoogleAnalytics.getInstance(this).reportActivityStart(this);
    }

    @Override
    protected void onStop() {
        // Stop the analytics tracking
        GoogleAnalytics.getInstance(this).reportActivityStop(this);
        super.onStop();
        setResult(RESULT_OK, resultValue);
    }

    private void car_loc_save_success() {
        Toast.makeText(this, R.string.save_car_location_success,
                Toast.LENGTH_SHORT).show();
    }

}
