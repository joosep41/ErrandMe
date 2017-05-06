package ee.jooseptenn.errandme.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import ee.jooseptenn.errandme.R;
import ee.jooseptenn.errandme.baseclasses.User;

/**
 * An activity for logging in with an existing account.
 */

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText email;
    private TextInputEditText password;
    private Button logInButton;
    private Button googleLogInButton;
    private TextView tv;
    private DatabaseReference myRef;
    private FirebaseAuth mAuth;
    private ProgressDialog logInProgressDialog;
    private ProgressDialog waitDialog;
    boolean logInDialogVisible;
    boolean waitDialogVisible;
    private static Activity mActivity;

    // https://firebase.google.com/docs/auth/android/google-signin
    public static GoogleApiClient mGoogleApiClient;
    private static final int RC_SIGN_IN = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mActivity = this;
        mAuth = FirebaseAuth.getInstance();
        myRef = FirebaseDatabase.getInstance().getReference();

        // https://firebase.google.com/docs/auth/android/google-signin
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_id))
                .requestEmail()
                .build();

        // https://firebase.google.com/docs/auth/android/google-signin
        // Build a GoogleApiClient with access to the Google Sign-In API and the
        // options specified by gso.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                    }
                } /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        // ErrandMe label
        TextView textView = (TextView) findViewById(R.id.textLabel);
        Typeface custom_font = Typeface.createFromAsset(getAssets(), "fonts/LobsterTwo-BoldItalic.otf");
        textView.setTypeface(custom_font);

        // email field and password field
        email = (TextInputEditText) findViewById(R.id.email);
        password = (TextInputEditText) findViewById(R.id.password);

        logInProgressDialog = new ProgressDialog(LoginActivity.this);
        logInProgressDialog.setIndeterminate(true);
        logInProgressDialog.setMessage(getString(R.string.log_in_progress));
        logInProgressDialog.setCanceledOnTouchOutside(false);

        waitDialog = new ProgressDialog(LoginActivity.this);
        waitDialog.setIndeterminate(true);
        waitDialog.setMessage(getString(R.string.please_wait));
        waitDialog.setCanceledOnTouchOutside(false);

        logInDialogVisible = false;
        waitDialogVisible = false;


        logInButton = (Button) findViewById(R.id.login);

        // email and password login
        logInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logIn();
            }
        });

        googleLogInButton = (Button) findViewById(R.id.loginGoogle);

        // Google login
        googleLogInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signIn();
            }
        });

        // Sign up! TextView
        tv = (TextView)findViewById(R.id.register_right);

        tv.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                Intent registerIntent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(registerIntent);
                finish();
            }
        });
    }

    /**
     * A method that tries to log the user in.
     *
     * @return true if the user is logged in false otherwise
     */
    public boolean logIn() {
        tv.setEnabled(false);
        googleLogInButton.setEnabled(false);
        logInProgressDialog.show();
        logInDialogVisible = true;

        // user entered email and password
        String emailString = email.getText().toString();
        String passwordString = password.getText().toString();

        if (!isValidInput(emailString,passwordString)) {
            dismissDialog();
            tv.setEnabled(true);
            googleLogInButton.setEnabled(true);
            Toast.makeText(this, R.string.wrong_email_or_password, Toast.LENGTH_LONG).show();
            return false;
        }
        if (!isNetworkAvailable(mActivity)) {
            dismissDialog();
            tv.setEnabled(true);
            googleLogInButton.setEnabled(true);
            Toast.makeText(this, R.string.no_internet, Toast.LENGTH_LONG).show();
            return false;
        }

        // email and password login initialization
        mAuth.signInWithEmailAndPassword(emailString, passwordString) // https://firebase.google.com/docs/auth/android/password-auth
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            dismissDialog();
                            tv.setEnabled(true);
                            googleLogInButton.setEnabled(true);
                            Toast.makeText(LoginActivity.this, R.string.invalid_credentials,
                                    Toast.LENGTH_SHORT).show();
                        }
                        else {
                            dismissDialog();
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        }

                        // ...
                    }
                });
        return true;
    }

    /**
     * https://firebase.google.com/docs/auth/android/google-signin
     * A method that initializes the Google sign-in process
     */
    private void signIn() {
        tv.setEnabled(false);
        logInButton.setEnabled(false);

        if (!isNetworkAvailable(mActivity)) {
            tv.setEnabled(true);
            logInButton.setEnabled(true);
            dismissDialog();
            Toast.makeText(this, R.string.no_internet, Toast.LENGTH_LONG).show();
            return;
        }

        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    /**
     * https://firebase.google.com/docs/auth/android/google-signin
     * A method that gets the result of the intent launched with Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
     *
     * @param requestCode request code
     * @param resultCode  result code
     * @param data        calling Intent
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            dismissDialog();
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = result.getSignInAccount();
                firebaseAuthWithGoogle(account);
            } else {
                tv.setEnabled(true);
                logInButton.setEnabled(true);
                // Google Sign In failed, update UI appropriately
                // ...
            }
        }
    }

    /**
     * https://firebase.google.com/docs/auth/android/google-signin
     * A method that that tries to sign in with Google credentials
     *
     * @param acct account to sign in with
     */
    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        waitDialog.show();
        waitDialogVisible = true;
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                final String name = firebaseUser.getDisplayName();
                                final String email = firebaseUser.getEmail();
                                final String uid = firebaseUser.getUid();

                                final SharedPreferences sharedPreferences = LoginActivity.this.getSharedPreferences(
                                        "ee.jooseptenn.errandme", Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putBoolean(getString(R.string.google_log_in), true);
                                editor.apply();

                                myRef.child("userData").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        if (!dataSnapshot.exists()) {
                                            User user = new User(name, "", email, "None");
                                            myRef.child("userData").child(uid).setValue(user);
                                        }

                                        dismissDialog();
                                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                        startActivity(intent);
                                        finish();
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }
                                });
                            } else {
                                tv.setEnabled(true);
                                logInButton.setEnabled(true);
                                // If sign in fails, display a message to the user.
                                Toast.makeText(LoginActivity.this, "Authentication failed.",
                                        Toast.LENGTH_SHORT).show();
                                dismissDialog();
                            }

                            // ...
                        }
                    }
                });
    }

    /**
     * A method that is used to check the validity of the email and password.
     *
     * @param emailString    an e-mail address that the user entered
     * @param passwordString a password that the user entered
     * @return               true if e-mail and password are valid false otherwise
     */
    public boolean isValidInput(String emailString, String passwordString) {
        boolean isValid = true;

        if(emailString.isEmpty()) {
            email.setError(getString(R.string.enter_email));
            isValid = false;
        }

        if(passwordString.isEmpty()) {
            password.setError(getString(R.string.enter_password));
            isValid = false;
        }
        else if (passwordString.length() > 25 || passwordString.length() < 8) {
            password.setError(getString(R.string.invalid_password_format));
            isValid = false;
        }
        return isValid;
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    /**
     * A method for hiding the ProgressDialog.
     */
    public void dismissDialog() {
        try {
            if (logInDialogVisible) {
                logInProgressDialog.dismiss();
                logInDialogVisible = false;
            }
            if (waitDialogVisible) {
                waitDialog.dismiss();
                waitDialogVisible = false;
            }
        } catch (Exception e) {
        }
    }

    /**
     * A method that checks for network availability
     *
     * @return true if there is an internet connection false otherwise
     */
    public static boolean isNetworkAvailable(Context context) { //https://developer.android.com/training/monitoring-device-state/connectivity-monitoring.html
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}