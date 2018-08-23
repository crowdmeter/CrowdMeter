package com.crowdmeter;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawer;
    private NavigationView navigationView;

    private LocationManager locationManager;
    private LocationListener locationListener;
    private String TAG = "mac";
    protected static final int REQUEST_CHECK_SETTINGS = 0x1;
    private int backPressedCount = 0;

    private boolean canStartMap = false;


    //time period for retreiving results from Firebase for each item

    private long timeperiod = 30 * 60 * 1000;
    private long raintimeperiod = 3 * 60 * 60 * 1000;
    private long traffictimeperiod = 30 * 60 * 1000;
    private long polictimeperiod = 12 * 60 * 60 * 1000;
    private long accidenttimeperiod = 2 * 60 * 60 * 1000;
    private long powercuttimeperiod = 1 * 60 * 60 * 1000;
    private long roadqualitytimeperiod = 24 * 60 * 60 * 1000;

    private long displaytimeperiod = 15 * 60 * 1000;

    private List<String> mPolls;
    private List<String> mTitles;
    private MapFragment mfrag;

    public final static String adminNumber = "+917073204933"; //change admin number to get addpoll option in menu

    private DatabaseReference mDatabaseRef;
    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;
    private Map<String, Integer> map;
    private String currUser;
    private String currNumber;
    private boolean flag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        progressDialog = new ProgressDialog(this);
        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        firebaseAuth = FirebaseAuth.getInstance();

        // check for current user, if logged in get user's number and user's unique uid(which is provided by firebase to unique identification)

        if (firebaseAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, PhoneVerification.class));
            finish();
        } else {
            currUser = firebaseAuth.getCurrentUser().getUid();
            currNumber = firebaseAuth.getCurrentUser().getPhoneNumber();
        }

        // set up toolbar and navigation drawer menu

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        //navigationView.setItemIconTintList(null);
        navigationView.setNavigationItemSelectedListener(this);

        Menu menu = navigationView.getMenu();
        try {
            if (!currNumber.equals(adminNumber)) {
                menu.removeItem(R.id.nav_addnew);
            }
        } catch (Exception e) {
            Log.i("mac", "exception during removing addnew menu: " + e.getMessage());
        }

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);

        drawer.addDrawerListener(toggle);
        toggle.syncState();

        if (savedInstanceState == null) {
            startHomeFragment();
        }

    }

    // home fragment displays all the card layout on the first screen

    private void startHomeFragment() {
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                new HomeFragment()).commit();
        navigationView.setCheckedItem(R.id.nav_home);
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        String title = (String) item.getTitle();
        Log.i("mac", "item name: " + title);

        switch (item.getItemId()) {
            case R.id.nav_home:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                        new HomeFragment()).commit();
                break;

            case R.id.nav_rain:
                setUpOptions(title);
                break;

            case R.id.nav_traffic:
                setUpOptions(title);  //pass title of the menu
                break;

            case R.id.nav_police:
                setUpOptions(title);
                break;

            case R.id.nav_accident:
                setUpOptions(title);
                break;

            case R.id.nav_powercut:
                setUpOptions(title);
                break;

            case R.id.nav_roadQuality:
                setUpOptions(title);
                break;

            case R.id.nav_addnew:
                startActivity(new Intent(this, AddPollActivity.class));
                finish();
                break;

            case R.id.nav_shareApp:
                shareApp();
                break;

            case R.id.nav_logout:
                if (firebaseAuth.getCurrentUser() != null) {
                    firebaseAuth.signOut();
                    MyPreferences.clearSP();
                    startActivity(new Intent(this, PhoneVerification.class));
                    finish();
                    Toast.makeText(this, "logged out", Toast.LENGTH_SHORT).show();
                }

                break;
        }

        drawer.closeDrawer(GravityCompat.START);

        return true;
    }


    // setUpOptions() takes the title (for eg: Rain) as arguement and uses this to set up current poll question and its options.
    // it calls getDatabaseValues() after the setup.

    private void setUpOptions(String title) {
        Map<String, String> tmap = MyPreferences.getTitle(this);
        String ques = getKeyFromValue(tmap, title);
        Log.i("mac", "q: " + ques);
        MyPreferences.setPollQues(this, ques);

        Map<String, List<String>> map = MyPreferences.getAllPolls(this);
        List<String> Options = map.get(MyPreferences.getPollQues(this));
        MyPreferences.setOptionssList(this, Options);

        getDatabaseValues(); //starts mapfragment after getting values

    }


    private String getKeyFromValue(Map<String, String> map, String value) {
        for (String o : map.keySet()) {
            if (map.get(o).equals(value)) {
                return o;
            }
        }
        return null;
    }


    // code for dynamically adding menu items in navigation drawer ---> not used here
    public void addMenuItem() {
        Menu menu = navigationView.getMenu();
        List<String> t = MyPreferences.getTitleList(getApplicationContext());
        try {
            if (t != null) {
                for (int i = 0; i < t.size(); i++) {
                    menu.add(R.id.group4, 0, 0, t.get(i)).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem menuItem) {
                            Toast.makeText(getApplicationContext(), "clicked", Toast.LENGTH_SHORT).show();
                            return true;
                        }
                    });
                }
            }
        } catch (Exception e) {
            Log.i("mac", "add menu ex: " + e.getMessage());
        }
    }



    /* onBackPressed() used to control how app behaves on backpress.
       1. if navigation drawer in open -> close it
       2. if mapview is open --> return back to home fragment which has all the tiles
       3. if on homefragment --> double click to exit app
    */

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);                                                // 1
        } else {
            FragmentManager fm = getSupportFragmentManager();
            Fragment f = fm.findFragmentById(R.id.fragment_container);

            if (f instanceof HomeFragment) {                                                        //3
                backPressedCount++;
                if (backPressedCount == 1) {
                    Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show();
                }
                if (backPressedCount == 2) {
                    backPressedCount = 0;
                    super.onBackPressed();
                }

            } else if (f instanceof MapFragment) {                                                  //2
                backPressedCount = 0;
                startHomeFragment();

            } else {
                backPressedCount = 0;
                super.onBackPressed();
            }
        }
    }


    //used to retreive all the votes casted by user for a particular question in a timeperiod defined above

    public void getDatabaseValues() {
        MyPreferences.setHasPolled(this, false);

        SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
        final String date = df.format(Calendar.getInstance().getTime());
        //Log.i("mac","date:"+date);


        //get title using poll question

        Map<String, String> titlemap = MyPreferences.getTitle(this);
        String q = MyPreferences.getPollQues(this);
        String title = titlemap.get(q);
        long period;


        // set up timeperiod according to selected quesion/title

        switch (title) {
            case "Rain":
                period = raintimeperiod;
                break;
            case "Traffic":
                period = traffictimeperiod;
                break;
            case "Traffic Police Checking":
                period = polictimeperiod;
                break;
            case "Power Cut":
                period = powercuttimeperiod;
                break;
            case "Accident":
                period = accidenttimeperiod;
                break;
            case "Road Quality":
                period = roadqualitytimeperiod;
                break;
            default:
                period = timeperiod;
                break;
        }


        // calc starttime after which the results are to be retreived

        final long currtime = System.currentTimeMillis();
        long starttime = currtime - period;
        Log.i("mac", "currtime: " + currtime);
        Log.i("mac", "starttime: " + starttime);


        // lists for storing all lat-lan and their respective responses

        final List<LatLng> mylist = new ArrayList<>();
        final List<String> responselist = new ArrayList<>();

        progressDialog.setTitle("Loading");
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
        progressDialog.show();


        /* Access PollResults->PollQuestion->timestamp
           order it by timestamp and use startAt to retreive the values only after start time
        */


        mDatabaseRef.child("PollResults")
                .child(MyPreferences.getPollQues(this))
                .orderByChild("timestamp").startAt(starttime)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Double lat;
                        Double lng;
                        boolean flag = false;
                        progressDialog.dismiss();
                        //  Map<String, Object> data = new HashMap<>();
                        Map<String, String> map = new HashMap<>();

                        for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                            //    data.put(childSnapshot.getKey(), childSnapshot.getValue());
                            //   Log.i("mac","results: "+ Objects.requireNonNull(childSnapshot.getValue()).toString());

                            try {
                                for (DataSnapshot mysnap : childSnapshot.getChildren()) {
                                    map.put(mysnap.getKey(), String.valueOf(mysnap.getValue()));
                                }

                                long timestamp = Long.parseLong(map.get("timestamp"));
                                long timediff = currtime - timestamp;

                                Log.i("mac", "timediff: " + timediff);
                                Log.i("mac", "disptimeperiod: " + displaytimeperiod);


                                // check if the user has already polled within the current display time -> set boolean value to true

                                if (map.get("uid").equals(currUser) && timediff <= displaytimeperiod) {
                                    MyPreferences.setHasPolled(getApplicationContext(), true);
                                }


                                //store (lat,lan) and response to respective list

                                lat = Double.parseDouble(map.get("lat"));
                                lng = Double.parseDouble(map.get("lon"));
                                LatLng currloc = new LatLng(lat, lng);
                                mylist.add(currloc);

                                responselist.add(map.get("response"));

                            } catch (Exception e) {
                                Log.i("mac", "Ex : " + e.getMessage());
                            }

                        }

                        Log.i("mac", "loclist: " + mylist);
                        Log.i("mac", "reslist: " + responselist);
                        MyPreferences.setAllLatLng(getApplicationContext(), mylist); //save all latlng for timeperiod in sharedPreference
                        MyPreferences.setResponseList(getApplicationContext(), responselist); ////save all response for timeperiod in sharedPreference

                        //check gps status -> if On -> start mapfragment
                        //                       else -> display popup to turn on gps
                        displayLocationSettingsRequest(getApplicationContext());

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        progressDialog.dismiss();
                        Log.i("mac", "error: " + databaseError.getMessage());

                    }
                });
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

                        //if satisfied start mapfragment

                        //canStartMap = true;
                        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                                new MapFragment()).commitAllowingStateLoss();


                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        Log.i(TAG, "Location settings are not satisfied. Show the user a dialog to upgrade location settings ");

                        try {
                            // Show the dialog by calling startResolutionForResult(), and check the result
                            // in onActivityResult().
                            status.startResolutionForResult(HomeActivity.this, REQUEST_CHECK_SETTINGS);
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
                        Log.i("mac", "result ok here");

                        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                                new MapFragment()).commitAllowingStateLoss();


                        break;
                    case Activity.RESULT_CANCELED:
                        navigationView.setCheckedItem(R.id.nav_home);
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


    private void shareApp(){
        try {
            String link = "http://play.google.com/store/apps/details?id=" + this.getPackageName();

            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_SUBJECT, "Crowd Meter");
            String sAux = "\nCheck Out the application : Crowd Meter\n\n";
            sAux = sAux + link;
            i.putExtra(Intent.EXTRA_TEXT, sAux);
            startActivity(Intent.createChooser(i, "Choose one"));
        } catch(Exception e) {
            Log.i("mac","share app exception: "+e.getMessage());
        }
    }

}
