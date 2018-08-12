package com.crowdmeter;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.crowdmeter.Utility.MyPreferences;
import com.crowdmeter.R;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PollAdapter extends RecyclerView.Adapter<PollAdapter.PollHolder> {

    private Context mContext;
    private List<String> mPolls;
    private List<String> mTitles;

    private DatabaseReference mDatabaseRef;
    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;
    private Map<String, Integer> map;
    private String currUser;
    private boolean flag = false;

    private long timeperiod = 30 * 60 * 1000;
    private long raintimeperiod = 3 * 60 * 60 * 1000;
    private long traffictimeperiod = 30 * 60 * 1000;
    private long polictimeperiod = 12 * 60 * 60 * 1000;

    private long displaytimeperiod = 15 * 60 * 1000;


    public PollAdapter(Context mContext, List<String> mPolls, List<String> mTitles) {
        this.mContext = mContext;
        this.mPolls = mPolls;
        this.mTitles = mTitles;
    }

    @NonNull
    @Override
    public PollHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(mContext).inflate(R.layout.poll_card, parent, false);
        return new PollHolder(v);
    }

    @Override
    public void onBindViewHolder(PollHolder holder, final int position) {

        holder.pollQuesCardTv.setText(mTitles.get(position));

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MyPreferences.setPollQues(mContext, mPolls.get(position));

                Map<String, List<String>> map = MyPreferences.getAllPolls(mContext);
                List<String> Options = map.get(MyPreferences.getPollQues(mContext));
                MyPreferences.setOptionssList(mContext, Options);

                ((HomeActivity) mContext).getDatabaseValues();

                setNavigationItemChecked(position);
                //getDatabaseValues();
            }
        });

        holder.delPollButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder;

                builder = new AlertDialog.Builder(mContext);

                builder.setTitle("Delete entry")
                        .setMessage("Are you sure you want to delete this entry?")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                delEntry(position);
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // do nothing
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        });

    }

    private void setNavigationItemChecked(int position) {
        NavigationView navigationView = ((HomeActivity) mContext).findViewById(R.id.nav_view);
        if (navigationView != null) {
            String title = mTitles.get(position);
            switch (title) {
                case "Rain":
                    navigationView.setCheckedItem(R.id.nav_rain);
                    break;

                case "Traffic":
                    navigationView.setCheckedItem(R.id.nav_traffic);
                    break;

                case "Traffic Police Checking":
                    navigationView.setCheckedItem(R.id.nav_police);
                    break;

                case "Accident":
                    navigationView.setCheckedItem(R.id.nav_accident);
                    break;

                case "Power Cut":
                    navigationView.setCheckedItem(R.id.nav_powercut);
                    break;

                case "Road Quality":
                    navigationView.setCheckedItem(R.id.nav_roadQuality);
                    break;

            }

        }
    }


    @Override
    public int getItemCount() {

        return mPolls.size();
    }


    public class PollHolder extends RecyclerView.ViewHolder {

        public TextView pollQuesCardTv;
        public ImageView delPollButton;


        public PollHolder(View itemView) {
            super(itemView);

            pollQuesCardTv = itemView.findViewById(R.id.pollquesCardTv);
            delPollButton = itemView.findViewById(R.id.delPollButton);
            mDatabaseRef = FirebaseDatabase.getInstance().getReference();
            firebaseAuth = FirebaseAuth.getInstance();
            progressDialog = new ProgressDialog(mContext);
            map = new HashMap<>();

            MyPreferences.setHasPolled(mContext, false);

            if (firebaseAuth.getCurrentUser() == null) {
                mContext.startActivity(new Intent(mContext, PhoneVerification.class));
            } else {
                currUser = firebaseAuth.getCurrentUser().getUid();
                try {
                    if (Objects.requireNonNull(firebaseAuth.getCurrentUser().getPhoneNumber()).equals(HomeActivity.adminNumber)) {
                        delPollButton.setVisibility(View.GONE);
                    } else {
                        delPollButton.setVisibility(View.GONE);
                    }
                } catch (Exception e) {
                    Log.i("email error", e.getMessage());
                }
            }
        }
    }


    private void getDatabaseValues() {
        SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
        final String date = df.format(Calendar.getInstance().getTime());
        //Log.i("mac","date:"+date);

        Map<String, String> titlemap = MyPreferences.getTitle(mContext);
        String q = MyPreferences.getPollQues(mContext);
        String title = titlemap.get(q);
        long period;

        if (title.equals("Rain")) {
            period = raintimeperiod;
        } else if (title.equals("Traffic")) {
            period = traffictimeperiod;
        } else if (title.equals("Traffic Police Checking")) {
            period = polictimeperiod;
        } else {
            period = timeperiod;
        }

        final long currtime = System.currentTimeMillis();
        long starttime = currtime - period;

        Log.i("mac", "currtime: " + currtime);
        Log.i("mac", "starttime: " + starttime);

        final List<LatLng> mylist = new ArrayList<>();
        final List<String> responselist = new ArrayList<>();

        progressDialog.setTitle("Loading");
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        mDatabaseRef.child("PollResults")
                .child(MyPreferences.getPollQues(mContext))
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

                                if (map.get("uid").equals(currUser) && timediff <= displaytimeperiod) {
                                    MyPreferences.setHasPolled(mContext, true);
                                }

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
                        MyPreferences.setAllLatLng(mContext, mylist); //store all latlng for timeperiod
                        MyPreferences.setResponseList(mContext, responselist);

                        ((HomeActivity) mContext).displayLocationSettingsRequest(mContext); //call gps status which starts map frag

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        progressDialog.dismiss();
                        Log.i("mac", "error: " + databaseError.getMessage());

                    }
                });
    }


    private void delEntry(final int pos) {
        final String ques = mPolls.get(pos);

        mDatabaseRef.child("Polls").child(ques).removeValue()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(mContext, "Entry deleted from database", Toast.LENGTH_SHORT).show();
                        mPolls.remove(pos);
                        notifyDataSetChanged();
                        mDatabaseRef.child("PollResults").child(ques).removeValue();

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(mContext, "Error while deleting", Toast.LENGTH_SHORT).show();
                    }
                });
    }

}


