package com.ride.me.mBox;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import com.ride.me.mMart.PlaceAutocompleteAdapter;
import com.ride.me.utils.Log;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.ride.me.config.General.BOUNDS;

public class PickLocation extends AppCompatActivity implements
        OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    @BindView(com.ride.me.R.id.mbox_picklocation)
    Button pickLocation;

    @BindView(com.ride.me.R.id.mbox_location)
    TextView mboxLocation;

    @BindView(com.ride.me.R.id.btn_home)
    ImageView backButton;

    @BindView(com.ride.me.R.id.locationPicker_autoCompleteText)
    AutoCompleteTextView autoCompleteTextView;

    private GoogleMap gMap;
    private GoogleApiClient googleApiClient;
    private Location lastKnownLocation;
    private LatLng pickLocationLatLang;
    public int position = 0 ;
    private PlaceAutocompleteAdapter mAdapter;

    private static final int REQUEST_PERMISSION_LOCATION = 991;
//    private static final LatLngBounds BOUNDS = new LatLngBounds(
//            new LatLng(-34.041458, 150.790100), new LatLng(-33.682247, 151.383362));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.ride.me.R.layout.activity_picklocation);
        ButterKnife.bind(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(com.ride.me.R.id.map_picklocation);
        mapFragment.getMapAsync(this);

//        if (googleApiClient == null) {
//            googleApiClient = new GoogleApiClient.Builder(this)
//                    .addConnectionCallbacks(this)
//                    .addOnConnectionFailedListener(this)
//                    .addApi(LocationServices.API)
//                    .build();
//        }

        if(getIntent().hasExtra(BoxOrder.POSITION)){
            position = getIntent().getIntExtra(BoxOrder.POSITION, 0);
        }

        pickLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onPickLocation();
            }
        });
        setupGoogleApiClient();
        setupAutocompleteTextView();

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void setupAutocompleteTextView() {
        mAdapter = new PlaceAutocompleteAdapter(this, googleApiClient, BOUNDS, null);
        autoCompleteTextView.setAdapter(mAdapter);
        autoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                InputMethodManager inputManager =
                        (InputMethodManager) PickLocation.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(autoCompleteTextView.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                AutocompletePrediction item = mAdapter.getItem(position);
                LatLng latLng = getLocationFromAddress(item.getFullText(null).toString());
                gMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            }
        });
    }

    private void setupGoogleApiClient() {
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .addApi(Places.GEO_DATA_API)
                    .build();
        }
    }


    @Override
    protected void onStart() {
        googleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        googleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSION_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateLastLocation(true);
            } else {
                // TODO: 10/15/2016 Tell user to use GPS
            }
        }
    }

    private LatLng getLocationFromAddress(String strAddress) {

        Geocoder coder = new Geocoder(this);
        List<Address> address;
        LatLng p1;

        try {
            address = coder.getFromLocationName(strAddress, 5);
            if (address == null) {
                return null;
            }
            Address location = address.get(0);
            location.getLatitude();
            location.getLongitude();

            p1 = new LatLng(location.getLatitude(), location.getLongitude());
            return p1;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        updateLastLocation(true);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        gMap = googleMap;
        gMap.getUiSettings().setMyLocationButtonEnabled(true);

        updateLastLocation(true);

        gMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                final LatLng centerPos = gMap.getCameraPosition().target;
                pickLocationLatLang = centerPos;
                fillAddress(mboxLocation, pickLocationLatLang);
            }
        });

    }

    private void updateLastLocation(boolean move) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION_LOCATION);
            return;
        }
        lastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(
                googleApiClient);
        gMap.setMyLocationEnabled(true);

        if (lastKnownLocation != null) {
            if(move) {
                gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()), 15f)
                );

                gMap.animateCamera(CameraUpdateFactory.zoomTo(15f));
            }
        }
    }

    private void onPickLocation() {
        Intent dataLocation = new Intent();
        Log.e("ADDRESS",  mboxLocation.getText().toString());
        dataLocation.putExtra(BoxOrder.ADDRESS_KEY, mboxLocation.getText().toString());
        dataLocation.putExtra(BoxOrder.LAT_KEY, pickLocationLatLang.latitude);
        dataLocation.putExtra(BoxOrder.LONG_KEY, pickLocationLatLang.longitude);
        dataLocation.putExtra(BoxOrder.POSITION, position);
        setResult(RESULT_OK, dataLocation);
        finish();
    }

    private void fillAddress(final TextView addresstext, final LatLng latLng) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Geocoder geocoder = new Geocoder(PickLocation.this, Locale.getDefault());
                    final List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                    PickLocation.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!addresses.isEmpty()) {
                                if (addresses.size() > 0) {
                                    String address = addresses.get(0).getAddressLine(0);
                                    addresstext.setText(address);
                                }
                            } else {
                                addresstext.setText(com.ride.me.R.string.text_addressNotAvailable);
                            }
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

}
