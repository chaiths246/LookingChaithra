package com.lookingbus.chaithra.demo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.location.LocationListener;

import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.android.PolyUtil;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.TravelMode;

import org.joda.time.DateTime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private static final String TAG = "MapsActivity";
    private Location mylocation;
    static GoogleMap map;
    Double latitude;
    Double longitude;
    Marker originMarker;
    Marker destinationMarker;
    LatLng locationOrigin;
    LatLng locationdistination;
    AutoCompleteTextView origin;
    String destinationaddress;
    String originaddress;
    AutoCompleteTextView destination;
    private GoogleApiClient googleApiClient;
    private final static int REQUEST_CHECK_SETTINGS_GPS = 0x1;
    private final static int REQUEST_ID_MULTIPLE_PERMISSIONS = 0x2;
    long REQUESTLOCATIONUPDATESTIME = 5 * 60000;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference userRef;
    List<AuthUI.IdpConfig> providers = Arrays.asList(
            new AuthUI.IdpConfig.GoogleBuilder().build()
    );
    private FirebaseAuth firebaseAuth;

    //    user
    private FirebaseUser user;
    String address;


    //    auth state listener
    private FirebaseAuth.AuthStateListener authStateListener;

    private static final int RC_SIGN_IN = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        connectGoogleClient();
        origin = (AutoCompleteTextView) findViewById(R.id.edtxt_origin);
        destination = (AutoCompleteTextView) findViewById(R.id.edtxt_destination);

        origin.setAdapter(new GooglePlacesAutocompleteAdapter(this, R.layout.list_item));
        destination.setAdapter(new GooglePlacesAutocompleteAdapter(this, R.layout.list_item));

        origin.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                originaddress = adapterView.getAdapter().getItem(i).toString();
                getLatLonFromAddress(originaddress, "ORIGIN");
            }
        });

        destination.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                destinationaddress = adapterView.getAdapter().getItem(i).toString();
                getLatLonFromAddress(destinationaddress, "DESTINATION");

            }
        });
        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        userRef = firebaseDatabase.getReference("users");
        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                user = firebaseAuth.getCurrentUser();
                if (user != null) {


                    // get user detail
                    String id = user.getUid();
                    String name = user.getDisplayName();
                    String email = user.getEmail();

                    String imgURL = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null;

                    //create model
                    UserModel userModel = new UserModel(id, name, email, imgURL);

                    //add to firebase database
                    userRef.child(userModel.getId()).setValue(userModel);
                    userRef.push().setValue(userModel);

                } else {

                    // Create and launch sign-in intent

                    StartSignIn();
                }
            }
        };

    }

    private void StartSignIn() {

        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build(),
                RC_SIGN_IN);
    }

    private void StartSignOut() {

        AuthUI.getInstance()
                .signOut(MapsActivity.this)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    public void onComplete(@NonNull Task<Void> task) {

                        Toast.makeText(MapsActivity.this, "Succesfully Signed Out", Toast.LENGTH_SHORT).show();


                    }
                });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.signout:
                StartSignOut();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();

        firebaseAuth.removeAuthStateListener(authStateListener);

    }

    @Override
    protected void onResume() {
        super.onResume();

        firebaseAuth.addAuthStateListener(authStateListener);

    }

    private void setoriginMarker(Double latitude, Double longitude) {
        locationOrigin = new LatLng(latitude, longitude);
        if (originMarker != null) {
            originMarker.remove();
        }
        originMarker = map.addMarker(new MarkerOptions()
                .position(locationOrigin)
                .title("Origin"));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(locationOrigin, 15));

    }

    private void setDestinationMarker(Double latitude, Double longitude) {
        locationdistination = new LatLng(latitude, longitude);
        if (destinationMarker != null) {
            destinationMarker.remove();
        }
        destinationMarker = map.addMarker(new MarkerOptions()
                .position(locationdistination)
                .title("Destination"));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(locationdistination, 15));
//        map.animateCamera(CameraUpdateFactory.zoomIn());
        //  getAddressFromLocation(latitude,longitude,"DESTINATION");
    }

    private synchronized void connectGoogleClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, 0, this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        mylocation = location;
        if (mylocation != null) {
            latitude = mylocation.getLatitude();
            longitude = mylocation.getLongitude();
            Geocoder geocoder;
            List<Address> addresses = null;
            geocoder = new Geocoder(this, Locale.getDefault());

            try {
                addresses = geocoder.getFromLocation(latitude, longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
            } catch (IOException e) {
                e.printStackTrace();
            }

            address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()

            Log.e(TAG, "Latitude : " + latitude + " Longitude : " + longitude);
            if (map != null) {
                setoriginMarker(latitude, longitude);
                origin.setText(address);
            }
        }
    }


    @Override
    public void onConnected(Bundle bundle) {
        checkPermissions();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(this, "Connection Suspended please check internet", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(this, "Connection Failed restart again", Toast.LENGTH_SHORT).show();
    }

    private void getMyLocation() {
        if (googleApiClient != null) {
            if (googleApiClient.isConnected()) {
                int permissionLocation = ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION);
                if (permissionLocation == PackageManager.PERMISSION_GRANTED) {
                    mylocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
                    LocationRequest locationRequest = new LocationRequest();
                    locationRequest.setInterval(REQUESTLOCATIONUPDATESTIME);
                    locationRequest.setFastestInterval(REQUESTLOCATIONUPDATESTIME);
                    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                    LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
                    builder.setAlwaysShow(false);
                    LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
                    PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
                    result.setResultCallback(new ResultCallback<LocationSettingsResult>() {

                        @Override
                        public void onResult(LocationSettingsResult result) {
                            final Status status = result.getStatus();
                            switch (status.getStatusCode()) {
                                case LocationSettingsStatusCodes.SUCCESS:
                                    int permissionLocation = ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION);
                                    if (permissionLocation == PackageManager.PERMISSION_GRANTED) {
                                        mylocation = LocationServices.FusedLocationApi
                                                .getLastLocation(googleApiClient);
                                    }
                                    break;
                                case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                    try {
                                        status.startResolutionForResult(MapsActivity.this, REQUEST_CHECK_SETTINGS_GPS);
                                    } catch (IntentSender.SendIntentException e) {
                                    }
                                    break;
                                case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                    Toast.makeText(MapsActivity.this, "GPS UNAVAILABLE", Toast.LENGTH_SHORT).show();
                                    break;
                            }
                        }
                    });
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS_GPS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        getMyLocation();
                        break;
                    case Activity.RESULT_CANCELED:
                        finish();
                        break;
                }
                break;
        }
    }

    private void checkPermissions() {
        List<String> listPermissionsNeeded = new ArrayList<>();
        if (permissionLocationGranted() != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
            if (!listPermissionsNeeded.isEmpty()) {
                ActivityCompat.requestPermissions(this,
                        listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), REQUEST_ID_MULTIPLE_PERMISSIONS);
            }
        } else {
            getMyLocation();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (permissionLocationGranted() == PackageManager.PERMISSION_GRANTED) {
            getMyLocation();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.e(TAG, "onMapReady called " + googleMap);
        map = googleMap;
    }

    private int permissionLocationGranted() {
        return ContextCompat.checkSelfPermission(MapsActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private void getLatLonFromAddress(String strAddress, String from) {
        Geocoder coder = new Geocoder(this);
        List<android.location.Address> address;

        try {
            address = coder.getFromLocationName(strAddress, 2);
            if (address == null) {
                return;
            }
            if (address.size() > 0) {
                android.location.Address location = address.get(0);
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                Log.e(TAG, "Address: " + address + " latitude " + latitude + " longitude " + longitude);
                if (from.equals("DESTINATION")) {
                    setDestinationMarker(latitude, longitude);
                } else {
                    setoriginMarker(latitude, longitude);
//                    setoriginMarker(37.3479173,-121.9982749);//3721 pecockroad
                }

                if (originaddress != null && destinationaddress != null) {
                    DirectionsResult results = getDirectionsDetails();
                    if (results != null) {
                        addPolyline(results, map);
                        if (results.routes.length > 0) {
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(results.routes[0].legs[0].startLocation.lat, results.routes[0].legs[0].startLocation.lng), 15));
                            if (originMarker != null) {
                                originMarker.remove();
                            }
                            if (destinationMarker != null) {
                                destinationMarker.remove();
                            }
                            originMarker = map.addMarker(new MarkerOptions().position(new LatLng(results.routes[0].legs[0].startLocation.lat, results.routes[0].legs[0].startLocation.lng)).title(results.routes[0].legs[0].startAddress));
                            destinationMarker = map.addMarker(new MarkerOptions().position(new LatLng(results.routes[0].legs[0].endLocation.lat, results.routes[0].legs[0].endLocation.lng)).title(results.routes[0].legs[0].startAddress).snippet(getEndLocationTitle(results)));
                        }
                        Double[] locarr = {latitude, longitude};
                        new VehicalInfo511().execute(locarr);
                    }
                } else {
                    Toast.makeText(this, "There is no bus route", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getEndLocationTitle(DirectionsResult results) {
        return "Time :" + results.routes[0].legs[0].duration.humanReadable + " Distance :" + results.routes[0].legs[0].distance.humanReadable;
    }

    private void addPolyline(DirectionsResult results, GoogleMap map) {
        if (results.routes.length > 0) {
            List<LatLng> decodedPath = PolyUtil.decode(results.routes[0].overviewPolyline.getEncodedPath());
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                return;
            }
            map.setMyLocationEnabled(true);
            map.addPolyline(new PolylineOptions().addAll(decodedPath));

        }else{
            Toast.makeText(this,"routes "+results.routes.length,Toast.LENGTH_SHORT).show();
        }
    }

    private GeoApiContext getGeoContext() {
        GeoApiContext geoApiContext = new GeoApiContext();
        return geoApiContext.setQueryRateLimit(3)
                .setApiKey(getString(R.string.google_maps_key))
                .setConnectTimeout(1, TimeUnit.SECONDS)
                .setReadTimeout(1, TimeUnit.SECONDS)
                .setWriteTimeout(1, TimeUnit.SECONDS);
    }

    private DirectionsResult getDirectionsDetails() {
        DateTime now = new DateTime();
        try {
            return DirectionsApi.newRequest(getGeoContext())
                    .mode(TravelMode.TRANSIT)
                    .origin(originaddress)
                    .destination(destinationaddress)
                    .departureTime(now)
                    .await();
        } catch (ApiException e) {
            e.printStackTrace();
            return null;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}