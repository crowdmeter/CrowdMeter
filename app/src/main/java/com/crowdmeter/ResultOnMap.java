package com.crowdmeter;

import android.app.Activity;
import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Path;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.crowdmeter.Utility.Haversine;
import com.crowdmeter.Utility.MyPreferences;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;




/*
   This file is not in use as all the things were converted and transferred to mapfragment, recheck its usage and delete if safe
 */


public class ResultOnMap extends AppCompatActivity {

    private MapView mapView;
    private CardView cardView;
    private TextView mapQuesText;
    private MapController mc;
    private String currUser;
    private ProgressDialog progressDialog;
    private MyLocationNewOverlay myLocationNewOverlay;
    private LocationManager locationManager;
    private LocationListener locationListener;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference mDatabase;

    private Map<String, Integer> map;
    private int resId[] = {R.drawable.greendot,
            R.drawable.bluedot,
            R.drawable.yellowdot,
            R.drawable.blackdot,
            R.drawable.greydot};

    private int drawableId;
    private boolean isActive = true;
    private boolean isAvailable = false;
    Map<String, String> titlemap;
    String q;

    double myradius = 10;
    private String TAG = "mac";
    protected static final int REQUEST_CHECK_SETTINGS = 0x1;

    Dialog myDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_on_map);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        firebaseAuth = FirebaseAuth.getInstance();

        if (firebaseAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, PhoneVerification.class));
            finish();
        } else {
            currUser = firebaseAuth.getCurrentUser().getUid();
        }


        titlemap = MyPreferences.getTitle(getApplicationContext());
        q = MyPreferences.getPollQues(getApplicationContext());

        myDialog = new Dialog(this);
        progressDialog = new ProgressDialog(this);
        cardView = findViewById(R.id.mapPollCard);
        mapQuesText = findViewById(R.id.mapPollQues);
        mapView = findViewById(R.id.mapView);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);

        mc = (MapController) mapView.getController();
        mc.setZoom(18);

        enableMyLocationOverlay();

        if (MyPreferences.getLat(this) == null) {
            progressDialog.setMessage("Getting location");
            progressDialog.setCancelable(false);
            progressDialog.show();

            displayLocationSettingsRequest(this);


        } else {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            displayLocationSettingsRequest(this);
            getValues();

        }

        setPollQues();
    }


    private void setPollQues() {
        mapQuesText.setText(MyPreferences.getPollQues(this));
        cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPopUp();
            }
        });
    }


    private void getValues() {
        double currlat = Double.parseDouble(MyPreferences.getLat(this));
        double currlon = Double.parseDouble(MyPreferences.getLon(this));
        mc.animateTo(new GeoPoint(currlat, currlon));

        List<LatLng> loclist = MyPreferences.getAllLatLng(this);
        List<String> reslist = MyPreferences.getResponseList(this);

        List<String> Optionslist = MyPreferences.getOptionsList(this);
        map = new HashMap<>();

        map.put(Optionslist.get(0), 0);

        for (int j = 1; j < Optionslist.size(); j++) {
            map.put(Optionslist.get(j), (j % resId.length));
        }


        if (loclist != null) {
            for (int i = 0; i < loclist.size(); i++) {
                double distance = Haversine.haversineDistance(currlat, currlon, loclist.get(i).latitude, loclist.get(i).longitude);
                if (distance < myradius) {
                    isAvailable = true;
                    GeoPoint gp = new GeoPoint(loclist.get(i).latitude, loclist.get(i).longitude);
                    drawableId = map.get(reslist.get(i));
                    addMarker(gp, drawableId, reslist.get(i));
                }
            }
        }

        if (!isAvailable) {
            Toast.makeText(this, "No information available in your area", Toast.LENGTH_SHORT).show();
        } else {
            if (titlemap.get(q).equals("Traffic Police Checking")) {
                Toast.makeText(this, "Carry your documents", Toast.LENGTH_SHORT).show();
            }
        }
    }


    public void addMarker(GeoPoint center, int drawId, String title) {
        // mc.animateTo(center);
        int mydrawid;
        if (MyPreferences.getPollQues(this).equals("Is it raining today?")) {
            mydrawid = R.drawable.raindrops;
        } else if (drawId == 0) {
            mydrawid = R.drawable.reddot;
        } else {
            mydrawid = resId[drawId];
        }

        Marker marker = new Marker(mapView);
        marker.setPosition(center);
        marker.setTitle(title);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setIcon(getResources().getDrawable(mydrawid));
        mapView.getOverlays().add(marker);

    }


    private void getLocation() {

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (progressDialog != null)
                    progressDialog.dismiss();

                enableMyLocationOverlay();

                boolean b = checkMinimumDistance(location.getLatitude(),location.getLongitude());
                if(b){
                    isActive = true;
                }


                Log.i("mac", "loc: " + location.toString());
                GeoPoint gp = new GeoPoint(location.getLatitude(), location.getLongitude());
                mc.animateTo(gp);

                MyPreferences.setLat(getApplicationContext(), String.valueOf(location.getLatitude()));
                MyPreferences.setLon(getApplicationContext(), String.valueOf(location.getLongitude()));

                if (isActive) {
                    getValues();
                    isActive = false;
                }

            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //ask permission
        } else {
            //we have permission
            Log.i("mac","we have perm");
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 10, locationListener);
        }

    }

    private boolean checkMinimumDistance(double lat,double lon) {
        if(MyPreferences.getLat(this)!=null){
            double currlat = Double.parseDouble(MyPreferences.getLat(this));
            double currlon = Double.parseDouble(MyPreferences.getLon(this));
            double distance = Haversine.haversineDistance(currlat, currlon, lat, lon);
            Log.i("mac","distance from last loc: "+distance);
            return distance > 10;
        }
        return false;
    }


    private void enableMyLocationOverlay() {
        myLocationNewOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
        myLocationNewOverlay.enableMyLocation();
        mapView.getOverlays().add(myLocationNewOverlay);
    }


    public void displayLocationSettingsRequest(Context context) {

        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API).build();
        googleApiClient.connect();

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(10000 / 2);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        Log.i(TAG, "All location settings are satisfied.");
                        getLocation();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        Log.i(TAG, "Location settings are not satisfied. Show the user a dialog to upgrade location settings ");

                        try {
                            // Show the dialog by calling startResolutionForResult(), and check the result
                            // in onActivityResult().
                            status.startResolutionForResult(ResultOnMap.this, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            Log.i(TAG, "PendingIntent unable to execute request.");
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        Log.i(TAG, "Location settings are inadequate, and cannot be fixed here. Dialog not created.");
                        break;
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
// Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.i("mac", "here");
                        getLocation();
                        break;
                    case Activity.RESULT_CANCELED:
                        //displayLocationSettingsRequest(getApplicationContext());//keep asking if imp or do whatever
                        finish();
                        break;
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
        }
    }


    private void showPopUp() {
        TextView textclose;
        final LinearLayout pollLayout;
        TextView pollQuestion;
        Button submitPoll;
        String address;


        final Map<String, List<String>> map;
        List<String> Options;

        myDialog.setContentView(R.layout.activity_poll_page);
        textclose = myDialog.findViewById(R.id.closePopUp);

        pollLayout = myDialog.findViewById(R.id.pollLinearLayout);
        pollQuestion = myDialog.findViewById(R.id.pollQuestionTv);
        submitPoll = myDialog.findViewById(R.id.submitPoll);
        progressDialog = new ProgressDialog(this);


        textclose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myDialog.dismiss();
            }
        });


        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        map = MyPreferences.getAllPolls(this);
        Options = map.get(MyPreferences.getPollQues(this));
        MyPreferences.setOptionssList(this, Options);

        pollQuestion.setText(MyPreferences.getPollQues(this));

        for (int i = 0; i < Options.size(); i++) {
            final View rowView = Objects.requireNonNull(inflater).inflate(R.layout.rowoptions, null);
            final Button b = rowView.findViewById(R.id.pollOption);
            b.setText(Options.get(i));
            b.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            b.setPadding(24, 0, 0, 0);
            b.setTextSize(16);

            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    b.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.border_colored));
                    String chosenOption = b.getText().toString();
                    MyPreferences.setChosenOption(getApplicationContext(), chosenOption);

                    for (int j = 1; j < pollLayout.getChildCount(); j++) {
                        try {
                            View v = pollLayout.getChildAt(j);
                            Button button = v.findViewById(R.id.pollOption);
                            String btext = button.getText().toString();
                            if (!btext.equals(chosenOption)) {
                                button.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.border));
                            }

                        } catch (Exception e) {
                            Log.i("mac", "error:" + e.getMessage());
                        }
                    }
                }
            });

            pollLayout.addView(rowView, pollLayout.getChildCount());
        }


        submitPoll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (MyPreferences.getHasPolled(getApplicationContext())) {

                    String title = titlemap.get(q);
                    String period;

                    if (title.equals("Rain")) {
                        period = "60 min";
                    } else if (title.equals("Traffic")) {
                        period = "30 min";
                    } else if (title.equals("Traffic Police Checking")) {
                        period = "12 hours";
                    } else {
                        period = "30 min";
                    }


                    Toast.makeText(getApplicationContext(), "You can only vote once in " + period,
                            Toast.LENGTH_SHORT).show();
                    myDialog.dismiss();
                    return;
                }

                SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
                String date = df.format(Calendar.getInstance().getTime());

                String chosenOption = MyPreferences.getChosenOption(getApplicationContext());
                String address = MyPreferences.getLat(getApplicationContext());

                if (address != null && !address.isEmpty()) {
                    if (chosenOption != null && !chosenOption.isEmpty()) {

                        progressDialog.setTitle("Submitting your choice");
                        progressDialog.setMessage("Please wait...");
                        progressDialog.setCancelable(false);
                        progressDialog.show();

                        Map<String, Object> map = new HashMap<>();
                        map.put("timestamp", ServerValue.TIMESTAMP);
                        map.put("lat", MyPreferences.getLat(getApplicationContext()));
                        map.put("lon", MyPreferences.getLon(getApplicationContext()));
                        map.put("uid", currUser);
                        map.put("response", chosenOption);

                        MyPreferences.setChosenOption(getApplicationContext(), chosenOption);

                        String key = mDatabase.child("PollResults")
                                .child(MyPreferences.getPollQues(getApplicationContext()))
                                .child(date).push().getKey();

                        if (key != null) {

                            mDatabase.child("PollResults")
                                    .child(MyPreferences.getPollQues(getApplicationContext()))
                                    .child(key).setValue(map)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Toast.makeText(getApplicationContext(), "Success", Toast.LENGTH_SHORT).show();
                                            MyPreferences.setHasPolled(getApplicationContext(), true);
                                            putCurrentResultMarker();
                                            progressDialog.dismiss();
                                            myDialog.dismiss();
                                            //startActivity(new Intent(getApplicationContext(), ResultOnMap.class));
                                            //finish();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(getApplicationContext(), "failed " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            progressDialog.dismiss();
                                            myDialog.dismiss();
                                        }
                                    });
                        }else{
                            Toast.makeText(getApplicationContext(),"Please Try again!",Toast.LENGTH_SHORT).show();
                        }


                    } else {
                        Toast.makeText(getApplicationContext(), "Please choose one option!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Please set your location", Toast.LENGTH_SHORT).show();
                }
            }
        });


        myDialog.show();

    }


    private void putCurrentResultMarker() {
        GeoPoint g = new GeoPoint(Double.parseDouble(MyPreferences.getLat(this)), Double.parseDouble(MyPreferences.getLon(this)));
        String title = MyPreferences.getChosenOption(getApplicationContext());
        drawableId = map.get(title);
        addMarker(g, drawableId, title);
    }



}
