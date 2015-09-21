package com.moggot.findmycarlocation;

import android.Manifest;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.analytics.GoogleAnalytics;

import java.util.Calendar;

public class MainActivity extends Activity {

    float y1, y2;
    ImageView img_animation;
    int height;

    int trigger = 0;
    final static String LOG_TAG = "myLogs";
    boolean isLocationSaved;

    private static boolean isAnimation = false;
    private static boolean repeat = true;
    private static boolean show_map = false;

    int widgetID = AppWidgetManager.INVALID_APPWIDGET_ID;
    Intent resultValue;
    SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        isLocationSaved = SharedPreference.LoadIsLocationSavedState(this);
        installWidget();

        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        ((AnalyticsApplication) getApplication())
                .getTracker(AnalyticsApplication.TrackerName.APP_TRACKER);

        img_animation = (ImageView) findViewById(R.id.ivTrigger);
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        height = displaymetrics.heightPixels;
    }


    public boolean onTouchEvent(MotionEvent touchevent) {
        isLocationSaved = SharedPreference.LoadIsLocationSavedState(this);
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
                    Log.d(LOG_TAG, "isLocationSaved = " + isLocationSaved);
                    Log.d(LOG_TAG, "trigger = " + trigger);
                    isLocationSaved = SharedPreference.LoadIsLocationSavedState(this);
                    updateWidget(isLocationSaved);
                    // if UP to DOWN sweep event on screen
                    if (trigger == 0) {
                        if (!isLocationSaved) {
                            save_car_location();
                            break;
                        }
                        show_map = true;
                        animationDown(0, height / 9);
                        trigger = -1;
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
                            find_your_car();
                            break;
                        }
                        animationUP();
                        Calendar time = Calendar.getInstance();
                        int cur_day = time.get(Calendar.DAY_OF_MONTH);
                        int cur_hour = time.get(Calendar.HOUR_OF_DAY);
                        int cur_minute = time.get(Calendar.MINUTE);
                        SharedPreference.SaveTime(this, cur_day, cur_hour,
                                cur_minute);
                        SharedPreference.SaveIsLocationSavedState(this, true);
                        updateWidget(true);
                        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                        Criteria criteria = new Criteria();
                        String provider = locationManager.getBestProvider(criteria,
                                false);
                        if (provider != null && !provider.equals("")) {
                            Location location = null;
                            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                location = locationManager
                                        .getLastKnownLocation(provider);
                            }

                            if (location != null) {
                                Log.d(LOG_TAG,
                                        "location_lng = " + location.getLongitude());
                                Log.d(LOG_TAG,
                                        "location_lat = " + location.getLatitude());

                                // SharedPreference.SaveLocation(this, 55.928,
                                // 37.520);
                                SharedPreference.SaveLocation(this,
                                        location.getLatitude(),
                                        location.getLongitude());
                            } else {
                                no_location();
                                startActivity(new Intent(
                                        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                            }
                        } else {
                            no_provider();
                            startActivity(new Intent(
                                    android.provider.Settings.ACTION_WIFI_SETTINGS));
                        }

                        trigger = 0;
                        break;
                    }
                }
            }
            break;

        }
        return false;
    }


    private void updateWidget(boolean isLocationSavedValue) {
        if (widgetID == -1)
            return;
        sp = getSharedPreferences(MyWidget.WIDGET_PREF, MODE_PRIVATE);
        sp.edit().putBoolean(SharedPreference.s_state_location_save, isLocationSavedValue).apply();
        Log.d(LOG_TAG,
                "isLocationSavedValue = " + isLocationSavedValue);
        Log.d(LOG_TAG,
                "widgetIDValue = " + widgetID);
        MyWidget.updateMyWidget(this, AppWidgetManager.getInstance(this), widgetID);
        setResult(RESULT_OK, resultValue);
    }

    private void installWidget() {
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        widgetID = SharedPreference.LoadWidgetID(this);

        Log.d(LOG_TAG, "widgetID = " + widgetID);
        if (extras != null) {
            widgetID = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            Log.d(LOG_TAG, "widgetID_extras = " + widgetID);
            SharedPreference.SaveWidgetID(this, widgetID);

            if (isLocationSaved) {
                Intent intent_screen = new Intent(MainActivity.this,
                        ScreenMap.class);
                startActivityForResult(intent_screen, SharedPreference.ACTIVITY_RESULT_CODE.MAP_SCREEN);
            } else
                saveLocation();

        }
        // и проверяем его корректность
        if (widgetID != AppWidgetManager.INVALID_APPWIDGET_ID) {
            // формируем intent ответа
            resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);
            // отрицательный ответ
            setResult(RESULT_CANCELED, resultValue);
            sp = getSharedPreferences(MyWidget.WIDGET_PREF, MODE_PRIVATE);
            setResult(RESULT_OK, resultValue);
            isLocationSaved = SharedPreference
                    .LoadIsLocationSavedState(this);
            updateWidget(isLocationSaved);
        }
    }

    void animationUP() {
        TranslateAnimation animation = new TranslateAnimation(0.0f, 0.0f,
                0, -height / 9);

        animation.setAnimationListener(new AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
                isAnimation = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (repeat) {
                    TranslateAnimation animation_repeate = new TranslateAnimation(0.0f, 0.0f,
                            -height / 9, 0);
                    animation_repeate.setDuration(1000);
                    animation_repeate.setFillAfter(true);
                    img_animation.startAnimation(animation_repeate);
                    car_loc_save_success();
                    repeat = false;
                    isAnimation = false;
                }

            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        animation.setDuration(1000);
        animation.setFillAfter(true);
        img_animation.startAnimation(animation);
    }

    void animationDown(float start, float end) {
        TranslateAnimation animation = new TranslateAnimation(0.0f, 0.0f,
                start, end);

        isLocationSaved = SharedPreference.LoadIsLocationSavedState(this);
        updateWidget(isLocationSaved);
        animation.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                isAnimation = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (isLocationSaved && show_map) {
                    Intent intent = new Intent(MainActivity.this,
                            ScreenMap.class);
                    startActivityForResult(intent, 1);
                    isAnimation = false;
                }
                isAnimation = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        animation.setDuration(1000);
        animation.setFillAfter(true);
        img_animation.startAnimation(animation);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(
                    android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            return true;
        }
        if (id == R.id.info) {
            Intent intent = new Intent(MainActivity.this, ScreenInfo.class);
            startActivityForResult(intent, 2);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void addShortcut() {
        //Adding shortcut for MainActivity
        //on Home screen
        Intent shortcutIntent = new Intent(getApplicationContext(),
                MainActivity.class);

        shortcutIntent.setAction(Intent.ACTION_MAIN);

        Intent addIntent = new Intent();
        addIntent
                .putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "HelloWorldShortcut");
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(getApplicationContext(),
                        R.mipmap.ic_launcher));

        addIntent
                .setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        getApplicationContext().sendBroadcast(addIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            animationDown(height / 9, 0);
            trigger = 0;
            repeat = true;
            show_map = false;
            updateWidget(isLocationSaved);
        }
    }

    }

    private void no_internet() {
        Toast.makeText(this, R.string.no_internet,
                Toast.LENGTH_SHORT).show();
    }

    private void no_location() {
        Toast.makeText(getBaseContext(), R.string.no_location,
                Toast.LENGTH_SHORT).show();
    }

    private void no_provider() {
        Toast.makeText(getBaseContext(), R.string.no_provider,
                Toast.LENGTH_SHORT).show();
    }

    private void find_your_car() {
        Toast.makeText(getBaseContext(), R.string.find_your_car,
                Toast.LENGTH_SHORT).show();
    }

    private void save_car_location() {
        Toast.makeText(getBaseContext(), R.string.save_car_location,
                Toast.LENGTH_SHORT).show();
    }

    private void car_loc_save_success() {
        Toast.makeText(getBaseContext(), R.string.save_car_loc,
                Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Get an Analytics tracker to report app starts & uncaught exceptions
        // etc.
        GoogleAnalytics.getInstance(this).reportActivityStart(this);

    }

    @Override
    protected void onStop() {

        // Stop the analytics tracking
        GoogleAnalytics.getInstance(this).reportActivityStop(this);
        super.onStop();
    }

}
