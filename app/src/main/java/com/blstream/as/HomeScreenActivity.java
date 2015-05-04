package com.blstream.as;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.blstream.as.ar.ArFragment;
import com.blstream.as.data.fragments.PoiFragment;
import com.blstream.as.data.rest.service.Server;
import com.blstream.as.map.MapsFragment;
import com.blstream.as.maps2d.MockDialog;

import com.google.android.gms.maps.model.MarkerOptions;

public class HomeScreenActivity extends ActionBarActivity implements
        OnPoiAdd,
        ArFragment.Callbacks,
        MapsFragment.Callbacks,
        PoiFragment.OnPoiSelectedListener,
        NetworkStateReceiver.NetworkStateReceiverListener {

    public final static String TAG = HomeScreenActivity.class.getSimpleName();

    private final static int NUM_IMAGES = 5;
    private PoiImageSlider viewPagerAdapter;
    private ViewPager viewPager;
    private TextView nearbyPoiButton;
    private TextView ownPlacesButton;
    private TextView addPoiButton;
    private TextView settingsButton;

    private boolean isFragmentProcessing;
    private NetworkStateReceiver networkStateReceiver;

    private int[] images;

    public HomeScreenActivity() {
    }

    public static HomeScreenActivity newInstance() {
        return new HomeScreenActivity();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isFragmentProcessing = false;
        Server.getPoiList();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_home_screen);
        setImages();
        viewPagerAdapter = new PoiImageSlider(this);
        viewPager = (ViewPager) findViewById(R.id.imageViewPager);
        viewPager.setAdapter(viewPagerAdapter);

        networkStateReceiver = new NetworkStateReceiver();
        networkStateReceiver.addListener(this);
        this.registerReceiver(networkStateReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));
        setButtons();
        setButtonsListeners();
        switchToMaps2D(true);
    }

    //Only for testing
    private void setImages() {
        images = new int[NUM_IMAGES];
        images[0] = R.drawable.splash;
        images[1] = R.drawable.ic_drawer;
        images[2] = R.drawable.bubble_mask;
        images[3] = R.drawable.abc_btn_check_material;
        images[4] = R.drawable.common_signin_btn_icon_focus_light;
    }

    void setButtons() {
        nearbyPoiButton = (TextView) findViewById(R.id.nearby);
        addPoiButton = (TextView) findViewById(R.id.add_poi);
        settingsButton = (TextView) findViewById(R.id.settings);
        ownPlacesButton = (TextView) findViewById(R.id.own_places);
    }

    void setButtonsListeners() {
        setNearbyPoiListener();
        setAddPoiListener();
        setSettingsListener();
        setOwnPlacesListener();
    }

    void setNearbyPoiListener() {
        nearbyPoiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchToMaps2D(true);
            }
        });
    }

    void setAddPoiListener() {
        addPoiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (findViewById(R.id.mock_dialog) == null) {
                    FragmentManager dialogFragmentManager = getSupportFragmentManager();
                    MockDialog mockDialog = new MockDialog();
                    mockDialog.show(dialogFragmentManager, "mock");
                }
            }
        });
    }

    void setSettingsListener() {
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO settings tab
            }
        });
    }

    void setOwnPlacesListener() {
        ownPlacesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                if (fragmentManager.findFragmentByTag(PoiFragment.TAG) == null) {
                    fragmentTransaction.addToBackStack(PoiFragment.TAG);
                }

                fragmentTransaction.replace(R.id.container, PoiFragment.newInstance());
                fragmentTransaction.commit();
            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // TODO Auto-generated method stub
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void switchToMaps2D(boolean centerOnPosition) {
        if (centerOnPosition) {
            MapsFragment.markerTarget = null;
        }
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        if (fragmentManager.findFragmentByTag(MapsFragment.TAG) == null) {
            fragmentTransaction.addToBackStack(MapsFragment.TAG);
        }

        fragmentTransaction.replace(R.id.container, MapsFragment.newInstance()); //FIXME: za kazdym razem tworzysz newInstance, powinienes znalezc po tagu i wywolac juz otwarty, a newInstance tylko jak nie znajdzie (zobacz w apce jak sie mapa za kazdym razem teraz rysuje powoli)
        fragmentTransaction.commit();
    }

    @Override
    public void switchToHome() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        int count = fragmentManager.getBackStackEntryCount();
        for(int i = 0; i < count; ++i) {
            fragmentManager.popBackStackImmediate();
        }
    }

    @Override
    public void sendPOIfromDialog(MarkerOptions markerOptions) {
    }

    @Override
    public void goToMarker(String poiId) {
        MapsFragment.markerTarget = MapsFragment.getMarkerFromPoiId(poiId);
        switchToMaps2D(false);
    }

    @Override
    public void onBackPressed() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        int backStackEntryCount = fragmentManager.getBackStackEntryCount();

        if (!LoginUtils.isUserLogged(this)) {
            fragmentManager.popBackStack();
            super.onBackPressed();
        }

        else if (moreThanOneOnStack(backStackEntryCount)) {
            fragmentManager.popBackStack();
            backStackEntryCount--;
            FragmentManager.BackStackEntry backStackEntry = fragmentManager.getBackStackEntryAt(getLastOnStack(backStackEntryCount));

            String lastFragmentOnStack = backStackEntry.getName();
            if (lastFragmentOnStack.equals(ArFragment.TAG)) {
                fragmentManager.popBackStack();
                switchToAr();
            }
            else if (lastFragmentOnStack.equals(MapsFragment.TAG)) {
                fragmentManager.popBackStack();
                switchToMaps2D(true);
            }
            else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                super.onBackPressed();
            }
        }
        else if (!isStackEmpty(backStackEntryCount)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            fragmentManager.popBackStack();
        }
    }

    private boolean moreThanOneOnStack(int count) {
        return (count > 1);
    }

    private int getLastOnStack(int count) {
        return (count - 1);
    }

    private boolean isStackEmpty(int count) {
        return (count == 0);
    }

    @Override
    public void switchToAr() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        if (fragmentManager.findFragmentByTag(ArFragment.TAG) == null) {
            fragmentTransaction.addToBackStack(ArFragment.TAG);
        }
        fragmentTransaction.replace(R.id.container, ArFragment.newInstance());
        fragmentTransaction.commit();
    }

    @Override
    public void gpsLost() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.gps_lost_title)
                .setMessage(R.string.gps_lost_description)
                .setPositiveButton(R.string.wifi_lost_close, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                        System.exit(0);
                    }
                })
                .setNegativeButton(R.string.wifi_lost_settings, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS), 0);
                    }
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public boolean isUserLogged() {
        return LoginUtils.isUserLogged(this);
    }

    @Override
    public void networkAvailable() {
        Log.v(TAG, "Internet dostepny!");
    }

    @Override
    public void networkUnavailable() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.network_lost_title)
                .setMessage(R.string.network_lost_description)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public void wifiOr3gConnected() {
        Log.v(TAG, "Wifi lub 3G podlaczane!");
    }

    @Override
    public void wifiOr3gDisconnected() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.wifi_lost_title)
                .setMessage(R.string.wifi_lost_description)
                .setPositiveButton(R.string.wifi_lost_close, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                        System.exit(0);
                    }
                })
                .setNegativeButton(R.string.wifi_lost_settings, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS), 0);

                    }
                })
                .setCancelable(false)
                .show();
    }

    private class PoiImageSlider extends PagerAdapter {

        Context context;
        LayoutInflater layoutInflater;

        public PoiImageSlider(Context context) {
            this.context = context;
            this.layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return images.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View itemView = layoutInflater.inflate(R.layout.view_pager_image, container, false);

            ImageView imageView = (ImageView) itemView.findViewById(R.id.image);
            imageView.setImageResource(images[position]);

            container.addView(itemView);

            return itemView;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((LinearLayout) object);
        }
    }
}
