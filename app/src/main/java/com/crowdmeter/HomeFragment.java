package com.crowdmeter;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.crowdmeter.Utility.MyPreferences;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HomeFragment extends Fragment {

    private DatabaseReference mDatabaseRef;
    private RecyclerView mPollRecyclerView;
    private PollAdapter mPollAdapter;
    private ProgressDialog progressDialog;
    private ProgressDialog progressDialog1;
    public static TextView noPollsTv;
    private FirebaseAuth firebaseAuth;
    public final static String adminemail = "social.crowdmeter@gmail.com";
    public final static String adminNumber = "+917073204933";
    private List<String> mPolls;
    private List<String> mTitles;
    private Map<String, List<String>> map;
    private Map<String, String> titlemap;
    private boolean refresh = false;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_main, container, false);

        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        mPollRecyclerView = view.findViewById(R.id.pollRecyclerView);
        mPollRecyclerView.setHasFixedSize(true);

        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        mPollRecyclerView.setLayoutManager(layoutManager);

        // mPollRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        progressDialog = new ProgressDialog(getContext());
        progressDialog1 = new ProgressDialog(getContext());
        noPollsTv = view.findViewById(R.id.noPollsTv);


      /*  if(MyPreferences.getAddress(getContext())==null){
            getPolls();
        }else {
            getPolls();
        }
*/

        getPolls();

        return view;
    }

    private void getPolls() {
        mPolls = new ArrayList<>();
        mTitles = new ArrayList<>();
        map = new HashMap<>();
        titlemap = new HashMap<>();
        final Map<String,String> titleQuesmap = new HashMap<>();

        boolean isNew = MyPreferences.getisNewPoll(getContext());

        if (!(MyPreferences.getAllPolls(getContext()).isEmpty()) && !(MyPreferences.getAllPolls(getContext()) == null) && !refresh && !isNew) {

            map = MyPreferences.getAllPolls(getContext());
            mPolls = new ArrayList<String>(map.keySet());
            titlemap = MyPreferences.getTitle(getContext());
            mTitles = new ArrayList<>(titlemap.values());
            mPollAdapter = new PollAdapter(getContext(), mPolls, mTitles);
            mPollRecyclerView.setAdapter(mPollAdapter);

        } else {

            progressDialog1.setTitle("Loading data");
            progressDialog1.setMessage("Please wait ...");
            progressDialog1.setCancelable(false);
            progressDialog1.show();


            mDatabaseRef.child("Polls").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    progressDialog1.dismiss();

                    if (!dataSnapshot.exists()) {
                        noPollsTv.setVisibility(View.VISIBLE);
                    } else {
                        noPollsTv.setVisibility(View.GONE);
                        for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {

                            try {
                                String s = postSnapshot.getKey(); //pollquestion

                                Map<String, Object> val = (Map<String, Object>) postSnapshot.getValue();

                                List<String> op = (List<String>) val.get("options");
                                String title = String.valueOf(val.get("title"));
                                mTitles.add(title);
                                Log.i("mac", "oplist: " + Objects.requireNonNull(op).toString());

                                titlemap.put(s, title);
                                titleQuesmap.put(title,s);
                                map.put(s, op);

                                mPolls.add(s);

                            } catch (Exception e) {
                                Log.i("mac", "exception get poll: " + e.getMessage());
                            }
                        }

                        MyPreferences.setAllPolls(getContext(), map);
                        MyPreferences.setTitle(getContext(), titlemap);
                       // MyPreferences.setTitleList(getContext(),mTitles);
                       // MyPreferences.setTitleQuestion(getContext(),titleQuesmap);


                        mPollAdapter = new PollAdapter(getContext(), mPolls, mTitles);
                        mPollRecyclerView.setAdapter(mPollAdapter);
                        refresh = false;
                        MyPreferences.setisNewPoll(getContext(), false);

                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    progressDialog1.dismiss();
                    Toast.makeText(getContext(), databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

}
