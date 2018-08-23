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
    private boolean isActive = false;       // used when user's current location distance from last location is farther than minimumdistance.
    private boolean isAvailable = false;      // true when there's atleast one response in user's location radius
    Map<String, String> titlemap;       // used to store title for the question as key
    String q;

    double myradius = 10;            // variable that defines the radius of results to be marked with center as user location
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


        titlemap = MyPreferences.getTitle(getContext());                    //get title map stored locally
        q = MyPreferences.getPollQues(getContext());                        // get the question that was selected when user pressed a tile

        myDialog = new Dialog(Objects.requireNonNull(getContext()));
        progressDialog = new ProgressDialog(getContext());

        cardView = view.findViewById(R.id.mapPollCard);
        mapQuesText = view.findViewById(R.id.mapPollQues);
        mapView = view.findViewById(R.id.mapView);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);

        mc = (MapController) mapView.getController();                                    //initialise map controller
        mc.setZoom(18);                                                                  // set map zoom levels

        Log.i("mac","mapfragment");


        // if user's current location is not stored then get the location first and then proceed
        // else first retreive values according to user's last known location and start getlocation in background.
        // if new location is farther than minimum location -> update the values according to the new location

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



    // Set the question in top bar, and when clicked showpopup with options

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


    // gets location based on network provider and store it locally and call getValues() to plot the results

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



    // gets location if user is moving -> update the change in location -> if new location is greater than minimumdistance -> retreive the values and plot it again

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
                    enableMyLocationOverlay();  //shows user current location on map

                    boolean b = checkMinimumDistance(location.getLatitude(), location.getLongitude());
                    if (b) {
                        isActive = true;            //set true to plot the new values
                    }

                    if (isActive) {
                        mapView.getOverlayManager().clear();    // when true clear the old plots from map
                        getValues();                            // and plot the new values
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


    //check minimum distance from last location to update the map plots

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


    //gets the lat-lon and responses from sharedPref stored when getDatabaseValues was called and plot it on map

    private void getValues() {

        //get user's last known lat and lon

        double currlat = Double.parseDouble(MyPreferences.getLat(getContext()));
        double currlon = Double.parseDouble(MyPreferences.getLon(getContext()));
        mc.animateTo(new GeoPoint(currlat, currlon));  //move the map to that locatiokn

        //retreive all the lat-lon and response values stored in sharedPref
        List<LatLng> loclist = MyPreferences.getAllLatLng(getContext());
        List<String> reslist = MyPreferences.getResponseList(getContext());

        List<String> Optionslist = MyPreferences.getOptionsList(getContext());
        map = new HashMap<>();                                  // key - option value and value - int which will be used to plot the coloured dot

        map.put(Optionslist.get(0), 0);             // for first option select red dot hence 0th position in array.

        for (int j = 1; j < Optionslist.size(); j++) {
            map.put(Optionslist.get(j), (j % resId.length));        // assign the coloured dots for each option
        }



        //calc distance from the lat-lon retreived from database to user's current location
        // if greater than myradius ignore else plot it on map


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


        // display messages accordingly

        if (!isAvailable) {
            Toast.makeText(mContext, "No information available in your area", Toast.LENGTH_SHORT).show();
        } else {
            if (titlemap.get(q).equals("Traffic Police Checking")) {
                Toast.makeText(mContext, "Carry your documents", Toast.LENGTH_SHORT).show();
            }
        }
    }


    // used to put marker or plot the point

    public void addMarker(GeoPoint center, int drawId, String title) {
        // mc.animateTo(center);
        int mydrawid;
        if (MyPreferences.getPollQues(getContext()).equals("Is it raining today?")) {
            mydrawid = R.drawable.raindrops;            // manually assigning rain symbol instead of dot
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


    // used to show user's current location by OSM

    private void enableMyLocationOverlay() {
        myLocationNewOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(Objects.requireNonNull(getContext())), mapView);
        myLocationNewOverlay.enableMyLocation();
        mapView.getOverlays().add(myLocationNewOverlay);
    }




    //pop up window showed when clicked on top question bar

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


        // retrieve question and its all options from shared Pref

        map = MyPreferences.getAllPolls(getContext());
        Options = map.get(MyPreferences.getPollQues(getContext()));
        MyPreferences.setOptionssList(getContext(), Options);


        pollQuestion.setText(MyPreferences.getPollQues(getContext()));


        //dynamically add poll options and change its color when clicked

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
                    String chosenOption = b.getText().toString();               // used to store user's selected option
                    MyPreferences.setChosenOption(getContext(), chosenOption);    // store it in sharedPref

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



        // when submit button is clicked check whether the user has already polled within 15 min period, if not store on firebase else show toast

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


                        // put all the values to be stored on firebase for each response
                        Map<String, Object> map = new HashMap<>();
                        map.put("timestamp", ServerValue.TIMESTAMP);
                        map.put("lat", MyPreferences.getLat(getContext()));
                        map.put("lon", MyPreferences.getLon(getContext()));
                        map.put("uid", currUser);
                        map.put("response", chosenOption);

                        MyPreferences.setChosenOption(getContext(), chosenOption);


                        //get a unique key for each response

                        String key = mDatabase.child("PollResults")
                                .child(MyPreferences.getPollQues(getContext()))
                                .child(date).push().getKey();


                        //if key is not null, push the values on firebase under that unique key
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
