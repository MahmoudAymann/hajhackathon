package com.creatokids.hajwithibraheem.Services.Maps;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.creatokids.hajwithibraheem.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBufferResponse;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import static com.creatokids.hajwithibraheem.Global.GlobalVars.mLastKnownLocation;
import static com.creatokids.hajwithibraheem.Global.MethodFactory.logMessage;

public class MapController {

    private static final float DEFAULT_ZOOM = 50.2f;
    private Context mContext;
    private FragmentActivity mActivity;

    private String TAG = getClass().getSimpleName();

    private boolean mLocationPermissionGranted;
    public final static int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 201;

    private GeoDataClient mGeoDataClient;
    private PlaceDetectionClient mPlaceDetectionClient;
    private FusedLocationProviderClient mFusedLocationProviderClient;

    private GoogleMap mGoogleMap;

    private LatLng mDefaultLocation = new LatLng(21.4225, 39.8262); // Location of Kaaba

    // Used for selecting the current place.
    private static final int M_MAX_ENTRIES = 5;
    private String[] mLikelyPlaceNames;
    private String[] mLikelyPlaceAddresses;
    private String[] mLikelyPlaceAttributions;
    private LatLng[] mLikelyPlaceLatLngs;
    private int PROXIMITY_RADIUS = 25;


    public MapController(Context pContext, FragmentActivity pActivity) {
        mContext = pContext;
        mActivity = pActivity;
        init();
    }

    private void init() {
        // Construct a GeoDataClient.
        mGeoDataClient = Places.getGeoDataClient(mContext, null);

        // Construct a PlaceDetectionClient.
        mPlaceDetectionClient = Places.getPlaceDetectionClient(mContext, null);

        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(mContext);

        // Ask for location permission
        getLocationPermission();
    }

    public boolean isLocationPermissionGranted() {
        return mLocationPermissionGranted;
    }

    public void setLocationPermissionGranted(boolean mLocationPermissionGranted) {
        this.mLocationPermissionGranted = mLocationPermissionGranted;
    }

    public void updateLocationUI(GoogleMap pMap) {
        // If the user has granted location permission, enable the My Location layer and
        // the related control on the map, otherwise disable the layer and the control,
        // and set the current location to null.
        mGoogleMap = pMap;
        if (mGoogleMap == null) {
            return;
        }
        try {
            if (isLocationPermissionGranted()) {
                mGoogleMap.setMyLocationEnabled(true);
                mGoogleMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mGoogleMap.setMyLocationEnabled(false);
                mGoogleMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(mContext.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            // set the location permission flag
            setLocationPermissionGranted(true);
        } else {
            ActivityCompat.requestPermissions(mActivity,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    public void updateCurrentLocation() {
        mLastKnownLocation = getCurrentLocation();
    }

    public Location getDeviceLocation(GoogleMap pMap) {
        mGoogleMap = pMap;
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (mLocationPermissionGranted) {
                Task locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(mActivity, new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {

                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = (Location) task.getResult();
                            if (mLastKnownLocation == null) {
                                logMessage(TAG, "locationResult.addOnCompleteListener -> mLastKnownLocation equals null");
                                return;
                            }
                            Toast.makeText(mContext, "hehe: "+mLastKnownLocation.getLatitude()+" "+"hehe: "+mLastKnownLocation.getLongitude(), Toast.LENGTH_SHORT).show();
                            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(mLastKnownLocation.getLatitude(),
                                            mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));

                            Toast.makeText(mContext, "kjl", Toast.LENGTH_SHORT).show();
//                            loadNearByPlaces(mLastKnownLocation.getLatitude(),
//                                    mLastKnownLocation.getLongitude());
                        } else {
                            logMessage(TAG, "Current location is null. Using defaults.");
                            logMessage(TAG, "Exception: " + task.getException());
                            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                            mGoogleMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            } else {
                getLocationPermission();
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
        return mLastKnownLocation;
    }

    public Location getCurrentLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (mLocationPermissionGranted) {
                Task locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(mActivity, new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = (Location) task.getResult();
                        } else {
                            logMessage(TAG, "Current location is null. Using defaults.");
                            logMessage(TAG, "Exception: " + task.getException());
                        }
                    }
                });
            } else {
                getLocationPermission();
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
        return mLastKnownLocation;
    }

    /**
     * Prompts the user to select the current place from a list of likely places, and shows the
     * current place on the map - provided the user has granted location permission.
     */
    public void showCurrentPlace() {
        if (mGoogleMap == null) {
            logMessage(TAG, "showCurrentPlace(): mMap is Null");
            return;
        }

        if (isLocationPermissionGranted()) {
            // Get the likely places - that is, the businesses and other points of interest that
            // are the best match for the device's current location.

            @SuppressWarnings("MissingPermission") final Task<PlaceLikelihoodBufferResponse> placeResult =
                    mPlaceDetectionClient.getCurrentPlace(null);
            placeResult.addOnCompleteListener
                    (new OnCompleteListener<PlaceLikelihoodBufferResponse>() {
                        @Override
                        public void onComplete(@NonNull Task<PlaceLikelihoodBufferResponse> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                PlaceLikelihoodBufferResponse likelyPlaces = task.getResult();

                                // Set the count, handling cases where less than 5 entries are returned.
                                int count;
                                if (likelyPlaces.getCount() < M_MAX_ENTRIES) {
                                    count = likelyPlaces.getCount();
                                } else {
                                    count = M_MAX_ENTRIES;
                                }

                                int i = 0;
                                mLikelyPlaceNames = new String[count];
                                mLikelyPlaceAddresses = new String[count];
                                mLikelyPlaceAttributions = new String[count];
                                mLikelyPlaceLatLngs = new LatLng[count];
//
                                for (PlaceLikelihood placeLikelihood : likelyPlaces) {

//                                     Build a list of likely places to show the user.
                                    mLikelyPlaceNames[i] = (String) placeLikelihood.getPlace().getName();
                                    mLikelyPlaceAddresses[i] = (String) placeLikelihood.getPlace().getAddress();
                                    mLikelyPlaceAttributions[i] = (String) placeLikelihood.getPlace().getAttributions();
                                    mLikelyPlaceLatLngs[i] = placeLikelihood.getPlace().getLatLng();

                                    logMessage(TAG, mLikelyPlaceNames[i]);
                                    logMessage(TAG, mLikelyPlaceAddresses[i]);
                                    logMessage(TAG, mLikelyPlaceAttributions[i]);
                                    logMessage(TAG, mLikelyPlaceLatLngs[i].toString());

                                    i++;
                                    if (i > (count - 1)) {
                                        break;
                                    }
                                }

//                                 Release the place likelihood buffer, to avoid memory leaks.
                                likelyPlaces.release();

                                // Show a dialog offering the user the list of likely places, and add a
                                // marker at the selected place.
                                openPlacesDialog();

                            } else {
                                logMessage(TAG, "Exception: " + task.getException());
                            }
                        }
                    });
        } else {
            // The user has not granted permission.
            logMessage(TAG, "The user did not grant location permission.");

            // Add a default marker, because the user hasn't selected a place.
            mGoogleMap.addMarker(new MarkerOptions()
                    .title(mActivity.getString(R.string.default_info_title))
                    .position(mDefaultLocation)
                    .snippet(mActivity.getString(R.string.default_info_snippet)));

            // Prompt the user for permission.
            getLocationPermission();
        }
    }

    /**
     * Displays a form allowing the user to select a place from a list of likely places.
     */
    private void openPlacesDialog() {
        Toast.makeText(mContext, "hehe", Toast.LENGTH_SHORT).show();
        // Ask the user to choose the place where they are now.
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // The "which" argument contains the position of the selected item.
                LatLng markerLatLng = mLikelyPlaceLatLngs[which];
                String markerSnippet = mLikelyPlaceAddresses[which];
                if (mLikelyPlaceAttributions[which] != null) {
                    markerSnippet = markerSnippet + "\n" + mLikelyPlaceAttributions[which];
                }

                // Add a marker for the selected place, with an info window
                // showing information about that place.
                mGoogleMap.addMarker(new MarkerOptions()
                        .title(mLikelyPlaceNames[which])
                        .position(markerLatLng)
                        .snippet(markerSnippet));

                // Position the map's camera at the location of the marker.
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(markerLatLng,
                        DEFAULT_ZOOM));
            }
        };

        // Display the dialog.
        AlertDialog dialog = new AlertDialog.Builder(mActivity)
                .setTitle(R.string.pick_place)
                .setItems(mLikelyPlaceNames, listener)
                .show();
    }

    public void loadNearByPlaces(double latitude, double longitude) {
        //YOU Can change this type at your own will, e.g hospital, cafe, restaurant.... and see how it all works
        Toast.makeText(mContext, "lat: " + latitude + "longX: " + longitude, Toast.LENGTH_LONG).show();


        mGoogleMap.clear();
//        Intent i = getIntent();
        String type = "hospital";

        StringBuilder googlePlacesUrl =
                new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=");
        googlePlacesUrl.append(latitude)
                .append(",")
                .append(longitude);
        googlePlacesUrl.append("&radius=").append(PROXIMITY_RADIUS);
        googlePlacesUrl.append("&types=").append(type);
        googlePlacesUrl.append("&sensor=true");
        googlePlacesUrl.append("&key=").append(mContext.getResources().getString(R.string.google_maps_key));

        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(mContext);
        String url = googlePlacesUrl.toString();
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        logMessage(TAG, "onResponse: Result= " + response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                logMessage(TAG, "onErrorResponse: Error= " + error.getMessage());
            }
        });

        queue.add(stringRequest);

    }

    public void loadNearByPlaces(Location location, String type) {
        //YOU Can change this type at your own will, e.g hospital, cafe, restaurant.... and see how it all works

        mGoogleMap.clear();

        StringBuilder googlePlacesUrl = new StringBuilder("http://rashidapi.gear.host/api/location?");

        googlePlacesUrl.append("querytxt=").append(type);
        googlePlacesUrl.append("&lat=").append(location.getLatitude());
        googlePlacesUrl.append("&lng=").append(location.getLongitude());

        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(mContext);
        String url = googlePlacesUrl.toString();
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        logMessage(TAG, "^_^onResponse: Result= " + response);

                        // Here send the response where ever you want, I recommend to broadcast the
                        // result and register a receiver in the desired place
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                logMessage(TAG, "onErrorResponse: Error= " + error.getMessage());
            }
        });
        queue.add(stringRequest);
    }
}
