package com.creatokids.hajwithibraheem.Services.Maps;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.creatokids.hajwithibraheem.Global.GlobalVars;
import com.creatokids.hajwithibraheem.R;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;

import static com.creatokids.hajwithibraheem.Global.GlobalVars.mLastKnownLocation;
import static com.creatokids.hajwithibraheem.Global.MethodFactory.logMessage;
import static com.creatokids.hajwithibraheem.Services.Maps.MapController.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private String TAG = getClass().getSimpleName();

    private GoogleMap mMap;
    private MapController mMapsController;
    private Context mContext;
    private FragmentActivity mActivity;

    // Keys for storing activity state.
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";

    private CameraPosition mCameraPosition;

    int PLACE_PICKER_REQUEST = 1;
    PlacePicker.IntentBuilder builder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve location and camera position from saved instance state.
        if (savedInstanceState != null) {
            mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            mCameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }

        setContentView(R.layout.activity_maps);

        mContext = getApplicationContext();
        mActivity = MapsActivity.this;

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        assert mapFragment != null;
            mapFragment.getMapAsync(this);

        mMapsController = new MapController(mContext, mActivity);


    }

    @Override
    protected void onStart() {
        super.onStart();
        builder = new PlacePicker.IntentBuilder();
    }

    @Override
    protected void onResume() {
        super.onResume();

        try {
            startActivityForResult(builder.build(mActivity), PLACE_PICKER_REQUEST);
        } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                Place place = PlacePicker.getPlace(this, data);
                String toastMsg = String.format("Place: %s", place.getName());
                Toast.makeText(this, toastMsg, Toast.LENGTH_LONG).show();
                mMapsController.loadNearByPlaces(place.getLatLng().latitude, place.getLatLng().longitude);
            }
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;

        // Add a marker in Sydney and move the camera
//        LatLng sydney = new LatLng(-34, 151);
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI(mMap);
        // Get the current location of the device and set the position of the map.
        getDeviceLocation(mMap);
    }

    /**
     * Saves the state of the map when the activity is paused.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMap != null && mLastKnownLocation != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, mLastKnownLocation);
            super.onSaveInstanceState(outState);
        }
    }

    private void getDeviceLocation(GoogleMap pMap) {
        if (mMapsController == null){
            mMapsController = new MapController(mContext, mActivity);
        }
        mLastKnownLocation = mMapsController.getDeviceLocation(pMap);
    }

    private void updateLocationUI(GoogleMap pMap){
        if (mMapsController == null){
            mMapsController = new MapController(mContext, mActivity);
        }
        mMapsController.updateLocationUI(pMap);
    }

    /**
     * Handles a click on the menu option to get a place.
     * @param item The menu item to handle.
     * @return Boolean.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.option_get_place) {
            showCurrentPlace(null);
        }
        return true;
    }

    public void showCurrentPlace(View view) {
        if (mMapsController == null){
            logMessage(TAG, "showCurrentPlace() -> mMapsController is null");
            return;
        }
        mMapsController.showCurrentPlace();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {

        // update the flag
        setPermissionGranted(false);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // update the flag
                    setPermissionGranted(true);
                }
            }
        }
        updateLocationUI(mMap);
    }

    private void setPermissionGranted(boolean isGranted){
        if (mMapsController == null){
            logMessage(TAG, "mMapsController == null");
            return;
        }
        // update the flag
        mMapsController.setLocationPermissionGranted(isGranted);
    }

    private boolean isLocationPermissionGranted(){
        if (mMapsController == null){
            logMessage(TAG, "mMapsController == null");
            return false;
        }
        // update the flag
        return mMapsController.isLocationPermissionGranted();
    }
}