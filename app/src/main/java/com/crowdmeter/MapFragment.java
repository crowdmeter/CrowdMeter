package com.crowdmeter;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.crowdmeter.Utility.Haversine;
import com.crowdmeter.Utility.MyPreferences;
import com.crowdmeter.Utility.SingleShotLocationProvider;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.DeflaterOutputStream;

public class MapFragment extends Fragment {

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
    private Context mContext;
    private DatabaseReference mDatabase;

    private Map<String, Integer> map;
    private int resId[] = {R.drawable.greendot,
            R.drawable.bluedot,
            R.drawable.yellowdot,
            R.drawable.blackdot,
            R.drawable.greydot};

    private int drawableId;
    private boolean isActive = false;
    private boolean isAvailable = false;
    Map<String, String> titlemap;
    String q;

    double myradius = 10;
    private String TAG = "mac";
    protected static final int REQUEST_CHECK_SETTINGS = 0x1;

    Dialog myDialog;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_result_on_map, container, false);

        mContext = Objects.requireNonNull(container).getContext();

        firebaseAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        progressDialog = new ProgressDialog(getContext());

        if (firebaseAuth.getCurrentUser() == null) {
            startActivity(new Intent(getContext(), PhoneVerification.class));
            Objects.requireNonNull(getActivity()).finish();

        } else {
            currUser = firebaseAuth.getCurrentUser().getUid();
        }

        titlemap = MyPreferences.getTitle(getContext());
        q = MyPreferences.getPollQues(getContext());

        myDialog = new Dialog(Objects.requireNonNull(getContext()));
        progressDialog = new ProgressDialog(getContext());

        cardView = view.findViewById(R.id.mapPollCard);
        mapQuesText = view.findViewById(R.id.mapPollQues);
        mapView = view.findViewById(R.id.mapView);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);

        mc = (MapController) mapView.getController();
        mc.setZoom(18);

        Log.i("mac","mapfragment");


        if (MyPreferences.getLat(getContext()) == null) {

            singleShotLocation(); //calls getvalues() after
            getLocation();

        } else {

            getValues();
            myLastLocationMarker();
            getLocation();

        }

        setPollQues();

        //enableMyLocationOverlay();

        return view;
    }


    private void setPollQues() {
        mapQuesText.setText(MyPreferences.getPollQues(getContext()));
        cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MyPreferences.setChosenOption(getContext(),null);
              //  Log.i("mac",MyPreferences.getChosenOption(getContext()));
                showPopUp();
            }
        });
    }

    private void singleShotLocation(){
        progressDialog.setMessage("Getting location");
        progressDialog.setCancelable(false);
        progressDialog.show();

        SingleShotLocationProvider.requestSingleUpdate(Objects.requireNonNull(getContext()),
                new SingleShotLocationProvider.LocationCallback() {
                    @Override
                    public void onNewLocationAvailable(SingleShotLocationProvider.GPSCoordinates location) {
                        Log.i("mac", "single location is " + location.latitude+" "+location.longitude);

                        progressDialog.dismiss();
                        MyPreferences.setLat(getContext(),String.valueOf(location.latitude));
                        MyPreferences.setLon(getContext(),String.valueOf(location.longitude));

                        GeoPoint g = new GeoPoint(location.latitude,location.longitude);
                        mc.animateTo(g);
                        myLastLocationMarker();
                        getValues();
                    }
                });
    }


    private void getLocation() {

        locationManager = (LocationManager) Objects.requireNonNull(getActivity()).getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (progressDialog != null)
                    progressDialog.dismiss();

                Log.i("mac", "loc change: " + location.toString());

                MyPreferences.setLat(getContext(), String.valueOf(location.getLatitude()));
                MyPreferences.setLon(getContext(), String.valueOf(location.getLongitude()));

                try {
                    enableMyLocationOverlay();

                    boolean b = checkMinimumDistance(location.getLatitude(), location.getLongitude());
                    if (b) {
                        isActive = true;
                    }

                    if (isActive) {
                        mapView.getOverlayManager().clear();
                        getValues();
                        isActive = false;

                    }
                }catch (Exception e){
                    Log.i("mac","exception : "+e.getMessage());
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

        if (ContextCompat.checkSelfPermission(Objects.requireNonNull(getContext()), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //ask permission
        } else {
            //we have permission
            Log.i("mac","we have perm");
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 10, locationListener);
        }

    }


    private boolean checkMinimumDistance(double lat,double lon) {
        if(MyPreferences.getLat(getContext())!=null){
            double currlat = Double.parseDouble(MyPreferences.getLat(getContext()));
            double currlon = Double.parseDouble(MyPreferences.getLon(getContext()));
            double distance = Haversine.haversineDistance(currlat, currlon, lat, lon);
            Log.i("mac","distance from last loc: "+distance);
            return distance > 5;
        }
        return false;
    }


    private void getValues() {
        double currlat = Double.parseDouble(MyPreferences.getLat(getContext()));
        double currlon = Double.parseDouble(MyPreferences.getLon(getContext()));
        mc.animateTo(new GeoPoint(currlat, currlon));

        List<LatLng> loclist = MyPreferences.getAllLatLng(getContext());
        List<String> reslist = MyPreferences.getResponseList(getContext());

        List<String> Optionslist = MyPreferences.getOptionsList(getContext());
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
            Toast.makeText(mContext, "No information available in your area", Toast.LENGTH_SHORT).show();
        } else {
            if (titlemap.get(q).equals("Traffic Police Checking")) {
                Toast.makeText(mContext, "Carry your documents", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void addMarker(GeoPoint center, int drawId, String title) {
        // mc.animateTo(center);
        int mydrawid;
        if (MyPreferences.getPollQues(getContext()).equals("Is it raining today?")) {
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


    private void enableMyLocationOverlay() {
        myLocationNewOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(Objects.requireNonNull(getContext())), mapView);
        myLocationNewOverlay.enableMyLocation();
        mapView.getOverlays().add(myLocationNewOverlay);
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
        progressDialog = new ProgressDialog(getContext());


        textclose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myDialog.dismiss();
            }
        });


        LayoutInflater inflater = (LayoutInflater) Objects.requireNonNull(getActivity()).getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        map = MyPreferences.getAllPolls(getContext());
        Options = map.get(MyPreferences.getPollQues(getContext()));
        MyPreferences.setOptionssList(getContext(), Options);


        pollQuestion.setText(MyPreferences.getPollQues(getContext()));

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
                    b.setBackground(ContextCompat.getDrawable(Objects.requireNonNull(getContext()), R.drawable.border_colored));
                    String chosenOption = b.getText().toString();
                    MyPreferences.setChosenOption(getContext(), chosenOption);

                    for (int j = 1; j < pollLayout.getChildCount(); j++) {
                        try {
                            View v = pollLayout.getChildAt(j);
                            Button button = v.findViewById(R.id.pollOption);
                            String btext = button.getText().toString();
                            if (!btext.equals(chosenOption)) {
                                button.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.border));
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

                if (MyPreferences.getHasPolled(getContext())) {

                    String period = "15 min";

                    Toast.makeText(getContext(), "You can only vote once in " + period,
                            Toast.LENGTH_SHORT).show();
                    myDialog.dismiss();
                    return;
                }

                SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
                String date = df.format(Calendar.getInstance().getTime());

                String chosenOption = MyPreferences.getChosenOption(getContext());
                Log.i("mac","chose: "+chosenOption);
                String address = MyPreferences.getLat(getContext());

                if (address != null && !address.isEmpty()) {
                    if (chosenOption != null && !chosenOption.isEmpty()) {

                        progressDialog.setTitle("Submitting your choice");
                        progressDialog.setMessage("Please wait...");
                        progressDialog.setCancelable(false);
                        progressDialog.show();

                        Map<String, Object> map = new HashMap<>();
                        map.put("timestamp", ServerValue.TIMESTAMP);
                        map.put("lat", MyPreferences.getLat(getContext()));
                        map.put("lon", MyPreferences.getLon(getContext()));
                        map.put("uid", currUser);
                        map.put("response", chosenOption);

                        MyPreferences.setChosenOption(getContext(), chosenOption);

                        String key = mDatabase.child("PollResults")
                                .child(MyPreferences.getPollQues(getContext()))
                                .child(date).push().getKey();

                        if (key != null) {

                            mDatabase.child("PollResults")
                                    .child(MyPreferences.getPollQues(getContext()))
                                    .child(key).setValue(map)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Toast.makeText(getContext(), "Success", Toast.LENGTH_SHORT).show();
                                            MyPreferences.setHasPolled(getContext(), true);
                                            putCurrentResultMarker();
                                            progressDialog.dismiss();
                                            myDialog.dismiss();
                                            //startActivity(new Intent(getContext(), ResultOnMap.class));
                                            //finish();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(getContext(), "failed " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            progressDialog.dismiss();
                                            myDialog.dismiss();
                                        }
                                    });
                        } else {
                            Toast.makeText(getContext(), "Please Try again!", Toast.LENGTH_SHORT).show();
                        }


                    } else {
                        Toast.makeText(getContext(), "Please choose one option!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Please set your location", Toast.LENGTH_SHORT).show();
                }
            }
        });


        myDialog.show();

    }

    private void putCurrentResultMarker() {
        GeoPoint g = new GeoPoint(Double.parseDouble(MyPreferences.getLat(getContext())), Double.parseDouble(MyPreferences.getLon(getContext())));
        String title = MyPreferences.getChosenOption(getContext());
        drawableId = map.get(title);
        addMarker(g, drawableId, title);
    }

    private void myLastLocationMarker(){
        double lat = Double.parseDouble(MyPreferences.getLat(getContext()));
        double lon = Double.parseDouble(MyPreferences.getLon(getContext()));
        GeoPoint g = new GeoPoint(lat,lon);
        Marker marker = new Marker(mapView);
        marker.setPosition(g);
        marker.setTitle("last Location");
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setIcon(getResources().getDrawable(R.drawable.ic_location_on_black_24dp));
        mapView.getOverlays().add(marker);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(locationManager!=null){
            locationManager.removeUpdates(locationListener);
        }
    }
}
