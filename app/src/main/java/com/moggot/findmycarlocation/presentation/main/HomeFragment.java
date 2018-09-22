package com.moggot.findmycarlocation.presentation.main;

import android.app.AlertDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.moggot.findmycarlocation.CarViewModel;
import com.moggot.findmycarlocation.ErrorStatus;
import com.moggot.findmycarlocation.R;
import com.moggot.findmycarlocation.Utils;
import com.moggot.findmycarlocation.presentation.common.BaseFragment;

import butterknife.BindView;

public class HomeFragment extends BaseFragment<CarViewModel> implements View.OnTouchListener {

    private static final String TAG = "HomeFragment";

    @BindView(R.id.iv_gear)
    View ivGear;
    @BindView(R.id.ad_main)
    AdView adView;

    private boolean isAnimated;
    private float startY;
    private AdRequest adRequest;
    private CarViewModel carViewModel;

    public HomeFragment() {
        // Required empty public constructor
    }

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState, CarViewModel viewModel) {
        this.carViewModel = viewModel;
        adRequest = new AdRequest.Builder().build();
    }

    @Override
    protected Class<CarViewModel> getViewModel() {
        return CarViewModel.class;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adView.loadAd(adRequest);
        isAnimated = false;
        ivGear.setOnTouchListener(this);
        carViewModel.getErrorStatus().observe(this, errorStatus -> {
            switch (errorStatus.getStatus()) {
                case ErrorStatus.LOCATION_ERROR:
                    Toast.makeText(getContext(), getString(R.string.no_location), Toast.LENGTH_SHORT).show();
                    break;
                default:
                    throw new IllegalArgumentException("Unknown parking state = " + errorStatus.getStatus());
            }
        });
        carViewModel.parkDataIfNeed().observe(this, needPark -> {
            if (needPark) {
                animateParking();
                Toast.makeText(getContext(), getString(R.string.save_car_location_success), Toast.LENGTH_SHORT).show();
            } else {
                createDialog();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        adView.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        adView.pause();
    }

    @Override
    public String getFragmentTag() {
        return TAG;
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.fragment_home;
    }

    private void animateParking() {
        Animation animationUp = AnimationUtils.loadAnimation(getContext(), R.anim.middle_up_middle);
        animationUp.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                isAnimated = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                isAnimated = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                //do nothing
            }
        });
        ivGear.startAnimation(animationUp);
    }

    private void createDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.dialog_you_not_find_car)
                .setMessage(R.string.dialog_title_save_car)
                .setPositiveButton(R.string.dialog_yes,
                        (dialog, id) -> carViewModel.reParkCar())
                .setNegativeButton(R.string.dialog_no,
                        (dialog, id) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        v.performClick();
        int action = event.getAction();
        if (isAnimated) {
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                startY = event.getY();
                return true;

            case MotionEvent.ACTION_UP:
                float endY = event.getY();
                if (startY > endY) {
                    carViewModel.parkCar();
                } else {
                    boolean isShowMap = carViewModel.tryToShowMap();
                    if (isShowMap) {
                        openMap();
                    } else {
                        Toast.makeText(getContext(), getString(R.string.you_should_save_car_location), Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            default:
                break;
        }
        return false;
    }

    private void openMap() {
        if (!Utils.isOnline(getContext())) {
            noInternet();
            return;
        }
        Animation animationDown = AnimationUtils.loadAnimation(getContext(), R.anim.middle_down_middle);
        animationDown.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                isAnimated = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                isAnimated = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                //do nothing
            }
        });
        ivGear.startAnimation(animationDown);
    }

    private void noInternet() {
        Toast.makeText(getContext(), getString(R.string.no_internet), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        adView.destroy();
        super.onDestroyView();
    }
}
