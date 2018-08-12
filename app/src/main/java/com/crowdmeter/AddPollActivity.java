package com.crowdmeter;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.crowdmeter.Utility.MyPreferences;
import com.crowdmeter.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AddPollActivity extends AppCompatActivity {

    private LinearLayout parentLayout;
    private Button submitButton;
    private EditText option1;
    private EditText pollTitle;
    private EditText pollQuestion;
    private DatabaseReference mDatabase;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_poll);

        parentLayout = findViewById(R.id.parent_layout);
        submitButton = findViewById(R.id.submitButton);
        option1 = findViewById(R.id.option_et);
        pollTitle = findViewById(R.id.pollTitle);
        pollQuestion = findViewById(R.id.pollQuesEt);
        progressDialog = new ProgressDialog(this);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String errmsg = "";
                List<String> options = new ArrayList<>();
                String op1 = option1.getText().toString().trim();
                String title = pollTitle.getText().toString().trim();
                String ques = pollQuestion.getText().toString().trim();

                if(!validateField(ques)){
                    errmsg = "Question can't be left empty";
                    Toast.makeText(getApplicationContext(),errmsg,Toast.LENGTH_SHORT).show();
                    return;
                }
                else if(!validateField(op1)){
                    errmsg = "Option 1 can't be left empty";
                    Toast.makeText(getApplicationContext(),errmsg,Toast.LENGTH_SHORT).show();
                    return;
                }else if(!validateField(title)){
                    errmsg = "Title can't be left empty";
                    Toast.makeText(getApplicationContext(),errmsg,Toast.LENGTH_SHORT).show();
                    return;
                }

                options.add(op1);

                for(int i=6;i<parentLayout.getChildCount()-1;i++){
                    View v = parentLayout.getChildAt(i);
                    EditText e = v.findViewById(R.id.option_new);

                    try{
                        String s = e.getText().toString();
                        Log.i("mac","op: "+s);
                        options.add(s);
                        if(!validateField(s)){
                            Toast.makeText(getApplicationContext(),"Options can't be left empty",Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }catch (Exception ex){
                        Log.i("mac","error: "+ ex.getMessage());
                    }
                }


                Map<String,Object> map = new HashMap<>();
                map.put("title",title);
                map.put("options",options);

                progressDialog.setTitle("Adding your entry");
                progressDialog.setMessage("Please wait ...");
                progressDialog.setCancelable(false);
                progressDialog.show();

                mDatabase.child("Polls").child(ques).setValue(map)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                progressDialog.dismiss();
                                Toast.makeText(getApplicationContext(),"Added ",Toast.LENGTH_SHORT).show();
                                MyPreferences.setisNewPoll(getApplicationContext(),true);
                                startActivity(new Intent(getApplicationContext(),HomeActivity.class));
                                finish();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(getApplicationContext(),"Error while uploading: "+e.getMessage(),Toast.LENGTH_SHORT).show();
                            }
                        });

            }
        });
    }

    private boolean validateField(String s){
        if(s.isEmpty()||s.equals("")){
            return false;
        }else
            return true;
    }

    public void onAddField(View v){
        int childCount = parentLayout.getChildCount();
        Log.i("mac", String.valueOf(childCount));
        if(childCount<11) {
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View rowView = Objects.requireNonNull(inflater).inflate(R.layout.rowfield,null);
            EditText e = rowView.findViewById(R.id.option_new);
            e.setHint("Option "+(childCount-5));
            //e.setId(childCount);
            parentLayout.addView(rowView, parentLayout.getChildCount() - 1);
        }else{
            Toast.makeText(this,"Options limit reached",Toast.LENGTH_SHORT).show();
        }
    }

    public void onRemoveField(View v){
        parentLayout.removeView((View)v.getParent());
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(this,HomeActivity.class));
        finish();
    }
}
