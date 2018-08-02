package com.creatokids.hajwithibraheem.Fragments;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;


import com.creatokids.hajwithibraheem.R;
import com.creatokids.hajwithibraheem.Services.Maps.MapController;
import com.creatokids.hajwithibraheem.R;
import com.creatokids.hajwithibraheem.Services.Maps.MapController;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;

import static android.app.Activity.RESULT_OK;
import static com.creatokids.hajwithibraheem.Global.GlobalVars.mLastKnownLocation;
import static com.creatokids.hajwithibraheem.Global.GlobalVars.mix;
import static com.creatokids.hajwithibraheem.Global.MethodFactory.logMessage;
import static com.creatokids.hajwithibraheem.Services.Maps.MapController.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MapsFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MapsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MapsFragment extends Fragment implements OnMapReadyCallback {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    View myFragmentView;

    private String TAG = getClass().getSimpleName();

    Context mContext;
    FragmentActivity mActivity;
    MapController mMapsController;


    private GoogleMap mMap;
    // Keys for storing activity state.
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";

    private CameraPosition mCameraPosition;

    int PLACE_PICKER_REQUEST = 1;
    PlacePicker.IntentBuilder builder;


    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    public MapsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MapsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MapsFragment newInstance(String param1, String param2) {
        MapsFragment fragment = new MapsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Retrieve location and camera position from saved instance state.
        if (savedInstanceState != null) {
            mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            mCameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }
        // Inflate the layout for this fragment
        myFragmentView = inflater.inflate(R.layout.fragment_maps, container, false);

        mContext = getContext();
        mActivity = getActivity();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) mActivity.getSupportFragmentManager()
                .findFragmentById(R.id.map);

        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        mMapsController = new MapController(mContext, mActivity);

        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        return myFragmentView;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }


    @Override
    public void onStart() {
        super.onStart();

        builder = new PlacePicker.IntentBuilder();

    }

    /**
     * Saves the state of the map when the activity is paused.
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mMap != null && mLastKnownLocation != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, mLastKnownLocation);
            super.onSaveInstanceState(outState);
        }
    }


    @Override
    public void onResume() {
        super.onResume();

        updateCurrentLocation();

        try {
            startActivityForResult(builder.build(mActivity), PLACE_PICKER_REQUEST);

        } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }

        Toast.makeText(mContext, ""+mLastKnownLocation.getLongitude(), Toast.LENGTH_SHORT).show();

    }

    private void loadNearbyPlaces(){
        if (mMapsController == null || mix == null){
            logMessage(TAG, "LoadNearbyPlaces -> mMapController is null or mix is null");
            return;
        }
        // Content here is the Category of wanted place,
        // See the method doSomeMess() in the class MainChatController.
        String placeCategory = mix.getContent();
        mMapsController.loadNearByPlaces(mLastKnownLocation, placeCategory);
    }

    private void updateCurrentLocation(){
        if(mMapsController == null){
            logMessage(TAG, "getCurrentLocation() -> mMapController is null");
            return;
        }
        mMapsController.updateCurrentLocation();
    }



    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                Place place = PlacePicker.getPlace(mContext, data);
                String toastMsg = String.format("Place: %s", place.getName());
                mMapsController.loadNearByPlaces(place.getLatLng().latitude, place.getLatLng().longitude);
            }
        }
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

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

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
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
