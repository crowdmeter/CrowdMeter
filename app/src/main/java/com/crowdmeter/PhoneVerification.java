package com.crowdmeter;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.hbb20.CountryCodePicker;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class PhoneVerification extends AppCompatActivity {

    private Button SendSmsButton;
    private EditText phoneNumberEt;
    private EditText verificationEt;
    private CountryCodePicker ccp;
    private TextView resendText;
    private ProgressBar progressBar;
    private LinearLayout verificationLayout;
    private LinearLayout resendLayout;
    private ScrollView backgroundLayout;
    private Boolean isVerificationActive = false;
    private String phone;
    private String mverificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private FirebaseAuth mAuth;
    private String TAG = "mac";
    private long timeOutDuration = 60;

    private LinearLayout timerLayout;
    private TextView timerText;

    private String[] perm;


    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_verification);

        if(checkPerm()){
            onPermissionTrue();
        }

    }


    private void onPermissionTrue(){
        SendSmsButton = findViewById(R.id.sendSmsButton);
        phoneNumberEt = findViewById(R.id.phoneEt);
        verificationEt = findViewById(R.id.verificationEt);
        ccp = findViewById(R.id.ccp);
        resendText = findViewById(R.id.resendText);
        progressBar = findViewById(R.id.PhoneprogressBar);
        verificationLayout = findViewById(R.id.verificationLayout);
        resendLayout = findViewById(R.id.resendLayout);
        backgroundLayout = findViewById(R.id.phonebackgroundLayout);
        timerLayout = findViewById(R.id.timerLayout);
        timerText = findViewById(R.id.timerText);

        ccp.registerCarrierNumberEditText(phoneNumberEt);

        mAuth = FirebaseAuth.getInstance();

        if(mAuth.getCurrentUser()!=null){
            startActivity(new Intent(this,HomeActivity.class));
            finish();
        }

        setUpCallbacks();

        SendSmsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isVerificationActive){
                    verifyCode();
                    //timerLayout.setVisibility(View.GONE);
                }else {
                    sendSms();
                }
            }
        });

        resendText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressBar.setVisibility(View.VISIBLE);
                resendVerificationCode(phone,mResendToken);
            }
        });
        
        backgroundLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeKeyBoard();
            }
        });
    }

    private void closeKeyBoard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        Objects.requireNonNull(inputMethodManager).hideSoftInputFromWindow(Objects.requireNonNull(getCurrentFocus()).getWindowToken(),0);
    }


    private void setUpCallbacks() {
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                signInWithPhoneAuthCredential(phoneAuthCredential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                Toast.makeText(getApplicationContext(),"Error: "+e.getMessage(),Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.INVISIBLE);
                phoneNumberEt.setEnabled(true);
                SendSmsButton.setEnabled(true);
            }

            @Override
            public void onCodeSent(String verificationId,
                                   PhoneAuthProvider.ForceResendingToken token) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.
                Log.d(TAG, "onCodeSent:" + verificationId);
                Toast.makeText(getApplicationContext(),"A verification code has benn sent to your mobile",Toast.LENGTH_SHORT).show();

                // Save verification ID and resending token so we can use them later
                mverificationId = verificationId;
                mResendToken = token;

                resendLayout.setVisibility(View.GONE);
                timerLayout.setVisibility(View.VISIBLE);
                MyTimer();

                verificationLayout.setVisibility(View.VISIBLE);
                //resendLayout.setVisibility(View.VISIBLE);
                resendText.setEnabled(true);
                SendSmsButton.setText("Verify Code");
                progressBar.setVisibility(View.INVISIBLE);
                isVerificationActive = true;
                SendSmsButton.setEnabled(true);

            }
        };
    }


    private void verifyCode() {
        String verificationcode = verificationEt.getText().toString().trim();
        if(verificationcode.isEmpty()){
            Toast.makeText(this,"Code can't be empty",Toast.LENGTH_SHORT).show();
            return;
        }

        SendSmsButton.setEnabled(false);
        verificationEt.setEnabled(false);
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mverificationId,verificationcode);
        signInWithPhoneAuthCredential(credential);
    }

    private boolean validatePhoneNumber() {

      //  Pattern p = Pattern.compile("((^[6789])(\\d{9}))");
        String phoneNumber = phoneNumberEt.getText().toString().trim();
        //Log.i("mac","ph: "+phoneNumber.length());

        if (TextUtils.isEmpty(phoneNumber)) {
            Toast.makeText(this,"Phone number can't be empty",Toast.LENGTH_SHORT).show();
            return false;
        }
        /*else if(!p.matcher(phoneNumber).matches()){
            Toast.makeText(this,"Phone number should be 10 digits long and starts with 6,7,8 or 9",Toast.LENGTH_SHORT).show();
            return false;
        }
        */
        return true;
    }

    private void sendSms() {

        phone = ccp.getFullNumberWithPlus();
        Log.i("mac","ccp: "+phone);

        //phone = phoneNumberEt.getText().toString().trim();
        //Log.i("mac","num: "+phone);

        if(!validatePhoneNumber()){
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        phoneNumberEt.setEnabled(false);
        SendSmsButton.setEnabled(false);


        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phone,
                timeOutDuration,
                TimeUnit.SECONDS,
                this,
                mCallbacks
        );

    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        progressBar.setVisibility(View.VISIBLE);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.i(TAG, "signInWithCredential:success");
                            progressBar.setVisibility(View.INVISIBLE);

                            startActivity(new Intent(getApplicationContext(),HomeActivity.class));
                            finish();

                        } else {
                            progressBar.setVisibility(View.INVISIBLE);
                            // Sign in failed, display a message and update the UI
                            Toast.makeText(getApplicationContext(), Objects.requireNonNull(task.getException()).getLocalizedMessage(),Toast.LENGTH_LONG).show();
                            verificationEt.setEnabled(true);
                            SendSmsButton.setEnabled(true);
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                            }
                        }
                    }
                });
    }

    private void resendVerificationCode(String phoneNumber,
                                        PhoneAuthProvider.ForceResendingToken token) {

        resendText.setEnabled(false);
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,        // Phone number to verify
                timeOutDuration,                 // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                this,               // Activity (for callback binding)
                mCallbacks,         // OnVerificationStateChangedCallbacks
                token);             // ForceResendingToken from callbacks
    }


    private void MyTimer(){
        new CountDownTimer(timeOutDuration*1000, 1000) {

            public void onTick(long millisUntilFinished) {
                timerText.setText(millisUntilFinished / 1000 + " sec )");
            }

            public void onFinish() {
                timerLayout.setVisibility(View.GONE);
                resendLayout.setVisibility(View.VISIBLE);
            }
        }.start();
    }


    private boolean checkPerm() {
        perm = new String[]{
                android.Manifest.permission.INTERNET,
                android.Manifest.permission.ACCESS_NETWORK_STATE,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_WIFI_STATE,

        };
        for (int i = 0; i < perm.length; i++) {
            if (ContextCompat.checkSelfPermission(this, perm[i]) == PackageManager.PERMISSION_GRANTED)
                continue;
            else {
                ActivityCompat.requestPermissions(this, perm, 1908);
                return false;
            }
        }
        return true;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.i("Check", "Got in the permission check");
        if (requestCode == 1908) {
            Log.i("Check", "Got in the permission check 1908");
            boolean b = true;
            for (int i = 0; i < perm.length; i++) {
                if (ContextCompat.checkSelfPermission(this, perm[i]) != PackageManager.PERMISSION_GRANTED) {
                    b = false;
                    break;
                }
            }
            if (b) {
                //Do something when all the permissions are granted.
                onPermissionTrue();
            } else {
                AlertDialog.Builder adb = new AlertDialog.Builder(this);
                adb.setTitle("Permission Denied!")
                        .setMessage("We cannot proceed without the permissions, Please allow again.")
                        .setPositiveButton("Allow", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                checkPerm();
                            }
                        })
                        .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                android.os.Process.killProcess(android.os.Process.myPid());
                                System.exit(1);
                            }
                        }).show();
            }

        }
    }

}
