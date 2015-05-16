package com.blstream.as.map;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.activeandroid.content.ContentProvider;
import com.blstream.as.data.rest.model.Poi;
import com.blstream.as.data.rest.service.Server;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.HashMap;

public class MapsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, LocationListener, GoogleMap.OnMarkerClickListener, GoogleMap.OnMarkerDragListener, GoogleMap.OnMapClickListener, GoogleMap.OnCameraChangeListener {

    public static final String TAG = MapsFragment.class.getSimpleName();

    private static final float ZOOM = 14;
    private static final float FULL_RESIZE = 1.0f;
    private static final int MAX_UPDATE_TIME = 1000;
    private static final int MAX_UPDATE_DISTANCE = 1;
    private static final int DEFAULT_POI_PANEL_HEIGHT = 200;
    private static final int HIDDEN = 0;
    private static final LatLng defaultPosition = new LatLng(0.0, 0.0);

    private static HashMap<String, Marker> markerHashMap = new HashMap<>();

    private GoogleMap googleMap;
    private boolean poiAddingMode = false;
    private boolean gpsChecked;
    private boolean cameraSet = false;
    private boolean poiSelected = false;

    private Marker markerTarget;
    private Marker userPositionMarker;
    private ScrollView scrollView;
    private SlidingUpPanelLayout poiPreviewLayout;
    private LinearLayout poiToolbar;
    private Callbacks activityConnector;

    private View rootView;
    private LocationManager locationManager;

    public static MapsFragment newInstance() {
        return new MapsFragment();
    }

    public void moveToMarker(Marker marker) {
        if (googleMap != null && marker != null) {
            cameraSet = true;
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), ZOOM));
            marker.showInfoWindow();
        }
    }

    public interface Callbacks {
        void switchToAr();

        void switchToHome();

        void gpsLost();

        boolean isUserLogged();

        void showConfirmPoiWindow(Marker marker);

        void showEditPoiWindow(Marker marker);

        void dismissConfirmAddPoiWindow();

        void deletePoi(Marker marker);

        void confirmDeletePoi(Marker marker);
    }

    public void setPoiAddingMode(boolean poiAddingMode) {
        this.poiAddingMode = poiAddingMode;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (rootView == null) {
            rootView = inflater.inflate(R.layout.fragment_map, container, false);
        }
        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MAX_UPDATE_TIME, MAX_UPDATE_DISTANCE, this);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MAX_UPDATE_TIME, MAX_UPDATE_DISTANCE, this);

        gpsChecked = false;
        setUpMapIfNeeded();
        setPoiPreview();

        googleMap.setOnMapClickListener(this);
        googleMap.setOnMarkerDragListener(this);

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof Callbacks) {
            activityConnector = (Callbacks) activity;
        } else {
            throw new ClassCastException(activity.toString()
                    + " must implement MapFragment.Callbacks");
        }
    }


    private void setUpMapIfNeeded() {
        if (googleMap == null) {
            SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
            googleMap = mapFragment.getMap();
            if (googleMap != null) {
                Log.v(TAG, "Map loaded");
                setUpMap();
                googleMap.setOnMapClickListener(this);
            }
        }
    }

    private void setUpMap() {
        googleMap.setMyLocationEnabled(false);
        googleMap.setOnMarkerClickListener(this);

        if (userPositionMarker == null) {
            BitmapDescriptor userPositionIcon = BitmapDescriptorFactory.fromResource(R.drawable.user_icon);
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(defaultPosition);
            markerOptions.icon(userPositionIcon);
            userPositionMarker = googleMap.addMarker(markerOptions);
        }
    }


    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.v(TAG, "Starting loading");
        return new CursorLoader(getActivity(),
                ContentProvider.createUri(Poi.class, null),
                null, null, null, null
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

        removeAllMarkers();

        int poiIdIndex = cursor.getColumnIndex(Poi.POI_ID);
        int nameIndex = cursor.getColumnIndex(Poi.NAME);
        int longitudeIndex = cursor.getColumnIndex(Poi.LONGITUDE);
        int latitudeIndex = cursor.getColumnIndex(Poi.LATITUDE);

        if (cursor.moveToFirst()) {
            do {
                if (googleMap != null) {
                    Marker marker = googleMap.addMarker(new MarkerOptions()
                                    .title(cursor.getString(nameIndex))
                                    .position(new LatLng(Double.parseDouble(cursor.getString(latitudeIndex))
                                            , Double.parseDouble(cursor.getString(longitudeIndex))))
                    );
                    markerHashMap.put(cursor.getString(poiIdIndex), marker);
                    Log.v(TAG, "Loaded: " + marker.getTitle() + ", id: " + marker.getId());
                }
            } while (cursor.moveToNext());
        }
    }

    private void removeAllMarkers() {
        for (Marker marker : markerHashMap.values()) {
            marker.remove();
        }

    }

    private void setPoiPreview() {

        poiPreviewLayout = (SlidingUpPanelLayout) rootView.findViewById(R.id.slidingUpPanel);
        poiPreviewLayout.setTouchEnabled(false);
        poiPreviewLayout.setPanelHeight(HIDDEN);

        View poiPreviewView = rootView.findViewById(R.id.poiPreviewLayout);
        scrollView = (ScrollView) poiPreviewView.findViewById(R.id.poiScrollView);
        poiToolbar = (LinearLayout) poiPreviewView.findViewById(R.id.poiToolbar);

        setToolbarOnTouchListener();
        setSliderListener();
    }

    private void setToolbarOnTouchListener() {
        poiToolbar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                DisplayMetrics displaymetrics = new DisplayMetrics();
                getActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
                int layoutHeight = displaymetrics.heightPixels;
                int eventRawY = (int) event.getRawY();
                if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_MOVE) {
                    int toolbarHeight = poiToolbar.getHeight();
                    int panelHeight = layoutHeight - eventRawY + toolbarHeight / 2;
                    if (panelHeight > layoutHeight - toolbarHeight) {
                        panelHeight = layoutHeight - toolbarHeight;
                    }
                    if (panelHeight < HIDDEN) {
                        poiSelected = false;
                        panelHeight = HIDDEN;
                    }
                    poiPreviewLayout.setPanelHeight(panelHeight);
                }
                return true;
            }
        });
    }

    private void setSliderListener() {
        poiPreviewLayout.setPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View view, float v) {
                resizeScrollView(view, v);
            }

            @Override
            public void onPanelCollapsed(View view) {

            }

            @Override
            public void onPanelExpanded(View view) {
                resizeScrollView(view, HIDDEN);
            }

            @Override
            public void onPanelAnchored(View view) {
                resizeScrollView(view, HIDDEN);
            }

            @Override
            public void onPanelHidden(View view) {

            }
        });
    }

    private void resizeScrollView(View panel, float slideOffset) {
        float reversedOffset = FULL_RESIZE - slideOffset;
        int scrollViewHeight = panel.getHeight() - poiPreviewLayout.getPanelHeight();
        scrollViewHeight *= reversedOffset;
        scrollView.setLayoutParams(
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                        scrollViewHeight));
    }

    /**
     * @param poiId Poi id on server,
     * @return Marker created from Poi with given ID, or null if there is not such marker
     */
    public static Marker getMarkerFromPoiId(String poiId) {
        if (markerHashMap != null) {
            return markerHashMap.get(poiId);
        } else {
            return null;
        }
    }

    public void deletePoi(Marker marker) {
        if (markerHashMap != null) {
            for (String poId : markerHashMap.keySet()) {
                if (marker.equals(getMarkerFromPoiId(poId))) {
                    marker.remove();
                    poiPreviewLayout.setPanelHeight(HIDDEN);
                    Server.deletePoi(poId);
                }
            }
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if (marker.equals(userPositionMarker)) {
            return true;
        } else if (markerIsNew(marker)) {
            activityConnector.showConfirmPoiWindow(marker);
        } else {
            setPoiPreviewInfo(marker);
            poiSelected = true;
            poiPreviewLayout.setPanelHeight(DEFAULT_POI_PANEL_HEIGHT);
        }
        return false;
    }

    private boolean markerIsNew(Marker marker) {
        return (marker.getTitle() == null || marker.getTitle().equals(""));
    }

    //Most data here is only for testing purposes
    private void setPoiPreviewInfo(Marker marker) {
        View poiPreviewView = rootView.findViewById(R.id.poiPreviewLayout);

        TextView category = (TextView) poiPreviewView.findViewById(R.id.categoryTextView);

        category.setText("Kategoria"); //Hardcoded - uzywane tylko do testow
        TextView name = (TextView) poiPreviewView.findViewById(R.id.nameTextView);
        name.setText(marker.getTitle());
        TextView description = (TextView) poiPreviewView.findViewById(R.id.descriptionTextView);

        Button editPoiButton = (Button) poiPreviewView.findViewById(R.id.editPoiButton);
        Button deletePoiButton = (Button) poiPreviewView.findViewById(R.id.deletePoiButton);

        EditPoiOnClickListener editPoiOnClickListener = new EditPoiOnClickListener(marker, false, activityConnector);
        editPoiButton.setOnClickListener(editPoiOnClickListener);
        deletePoiButton.setOnClickListener(editPoiOnClickListener);

        String position = "";
        position += "Longitude: " + marker.getPosition().longitude; //Hardcoded - uzywane tylko do testow
        position += "\nLatitude: " + marker.getPosition().latitude; //Hardcoded - uzywane tylko do testow
        description.setText(position);

        ImageView image = (ImageView) poiPreviewView.findViewById(R.id.imageView);
        image.setImageResource(R.drawable.splash);
    }

    public void setMarkerTarget(Marker markerTarget) {
        this.markerTarget = markerTarget;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.v(TAG, "Location updated");
        Log.v(TAG, location.getLatitude() + ", " + location.getLongitude());
        LatLng googleLocation = new LatLng(location.getLatitude(), location.getLongitude());
        if (userPositionMarker != null) {
            userPositionMarker.setPosition(googleLocation);
            if (!cameraSet && googleMap != null) {
                moveToMarker(userPositionMarker);
                cameraSet = true;
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        cameraSet = false;
        locationManager.removeUpdates(this);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
        LocationManager lm = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER) && !lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) && !gpsChecked) {
            gpsChecked = true;
            activityConnector.gpsLost();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (markerTarget != null) {
            markerTarget.remove();
        }
        setPoiAddingMode(false);
        activityConnector.dismissConfirmAddPoiWindow();
        googleMap.setOnMarkerClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(0, null, this);
        poiPreviewLayout.setPanelHeight(HIDDEN);
    }

    public void moveToActiveMarker() {
        if (markerTarget == null) {
            moveToMarker(userPositionMarker);
        } else {
            moveToMarker(markerTarget);
        }
    }

    @Override
    public void onMarkerDragStart(Marker marker) {
        activityConnector.dismissConfirmAddPoiWindow();

    }

    @Override
    public void onMarkerDrag(Marker marker) {

    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), ZOOM), new AnimateCameraCallbacks());

    }

    @Override
    public void onMapClick(LatLng latLng) {
        poiSelected = false;
        poiPreviewLayout.setPanelHeight(HIDDEN);
        activityConnector.dismissConfirmAddPoiWindow();
        if (poiAddingMode) {
            Marker marker = googleMap.addMarker(new MarkerOptions().position(latLng));
            markerTarget = marker;
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), ZOOM), new AnimateCameraCallbacks());
            marker.setDraggable(true);
            setPoiAddingMode(false);
        }
    }

    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);

        if (!poiSelected) {
            poiPreviewLayout.setPanelHeight(HIDDEN);
        } else if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            poiPreviewLayout.setPanelHeight(DEFAULT_POI_PANEL_HEIGHT);
        } else if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            poiPreviewLayout.setPanelHeight(DEFAULT_POI_PANEL_HEIGHT / 2);
        }
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        activityConnector.dismissConfirmAddPoiWindow();
    }

    public class AnimateCameraCallbacks implements GoogleMap.CancelableCallback {

        @Override
        public void onFinish() {
            activityConnector.showConfirmPoiWindow(markerTarget);
        }

        @Override
        public void onCancel() {

        }
    }

}
