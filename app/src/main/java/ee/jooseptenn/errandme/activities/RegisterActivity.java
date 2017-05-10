package ee.jooseptenn.errandme.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import ee.jooseptenn.errandme.R;
import ee.jooseptenn.errandme.baseclasses.User;

/**
 * An activity for registering a new account.
 */

public class RegisterActivity extends Activity {

    private TextInputEditText nameText;
    private TextInputEditText phoneNumber;
    private TextInputEditText userEmail;
    private TextInputEditText userPassword;
    private TextInputEditText userPasswordConfirm;
    private Button registerButton;
    private FirebaseAuth mAuth;
    private DatabaseReference myRef;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private ProgressDialog registerProgressDialog;
    private ScrollView scrollView;

    protected static boolean scrolledToError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // ErrandMe label
        TextView textView = (TextView) findViewById(R.id.textLabel);
        Typeface custom_font = Typeface.createFromAsset(getAssets(), "fonts/LobsterTwo-BoldItalic.otf");
        textView.setTypeface(custom_font);

        // Getting references to layout elements
        nameText = (TextInputEditText) findViewById(R.id.name);
        phoneNumber = (TextInputEditText) findViewById(R.id.phoneNumber);
        userEmail = (TextInputEditText) findViewById(R.id.email_register);
        userPassword = (TextInputEditText) findViewById(R.id.password_input_register);
        userPasswordConfirm = (TextInputEditText) findViewById(R.id.password_input_register_confirm);
        registerButton = (Button) findViewById(R.id.register_confirm);
        scrollView = (ScrollView) findViewById(R.id.scrollView);

        registerProgressDialog = new ProgressDialog(RegisterActivity.this);
        registerProgressDialog.setIndeterminate(true);
        registerProgressDialog.setMessage(getString(R.string.register_progress));
        registerProgressDialog.setCanceledOnTouchOutside(false);

        // getting Firebase related information (database reference, authentication instance)
        mAuth = FirebaseAuth.getInstance();
        myRef = FirebaseDatabase.getInstance().getReference();

        // Listening for changes in the authentication state - https://firebase.google.com/docs/auth/android/password-auth (modified compared to original)
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    dismissProgressDialog();
                    // User is signed in
                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    // User is signed out

                }
                // ...
            }
        };

        // initialize registration process
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                register();
            }
        });
    }

    /**
     * A method for validating the user input.
     *
     * @param name            entered name
     * @param phoneN          entered phone number
     * @param email           entered e-mail address
     * @param password        entered password
     * @param passwordConfirm entered password confirmation
     * @return                true if input is valid false otherwise
     */
    private boolean validateInput(String name, String phoneN, String email, String password, String passwordConfirm, ScrollView scrollView) {
        boolean isValid = true;
        scrolledToError = false;

        if (name.isEmpty() || !hasAllowedCharacters(name)) {
            nameText.setError(getString(R.string.valid_name));
            isValid = false;
            scrollToErrorMessage(scrollView, nameText);
        }

        if (phoneN.isEmpty() || !hasOnlyDigits(phoneN)) {
            phoneNumber.setError(getString(R.string.valid_phone));
            isValid = false;
            scrollToErrorMessage(scrollView, phoneNumber);
        }

        if(email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            userEmail.setError(getString(R.string.invalid_email_format));
            isValid = false;
            scrollToErrorMessage(scrollView, userEmail);
        }

        if(password.isEmpty() || password.length() > 25 || password.length() < 8) {
            userPassword.setError(getString(R.string.password_length));
            isValid = false;
            scrollToErrorMessage(scrollView, userPassword);
        }

        if(!password.equals(passwordConfirm)) {
            userPasswordConfirm.setError(getString(R.string.password_match));
            isValid = false;
            scrollToErrorMessage(scrollView, userPasswordConfirm);
        }

        return isValid;
    }

    /**
     * A method for checking if the input only contains digits.
     *
     * @param phoneN the string that is checked
     * @return       true if all the characters are digits false otherwise
     */
    public static boolean hasOnlyDigits(String phoneN) {
        boolean pass = true;
        for (int i = 0; i < phoneN.length(); i++) {
            if (!Character.isDigit(phoneN.charAt(i))) {
                pass = false;
            }
        }
        return pass;
    }

    /**
     * A method for checking if the input only contains letters.
     *
     * @param name  the string that is checked
     * @return      true if all the characters are letters false otherwise
     */
    private boolean hasAllowedCharacters(String name) {
        boolean pass = true;
        for (int i = 0; i < name.length(); i++) {
            if (!Character.isLetter(name.charAt(i)) && name.charAt(i) != '-' && name.charAt(i) != ' ' && name.charAt(i) != '\'') {
                pass = false;
            }
        }
        return pass;
    }

    /**
     * A method that tries to register a new user account.
     *
     * @return true if registering is successful false otherwise
     */
    public boolean register() {

        registerProgressDialog.show();

        // getting user entered values
        final String name = nameText.getText().toString();
        final String phoneN = phoneNumber.getText().toString();
        final String email = userEmail.getText().toString();
        String password = userPassword.getText().toString();
        String passwordConfirm = userPasswordConfirm.getText().toString();

        if (!validateInput(name, phoneN, email, password, passwordConfirm, scrollView)) {
            dismissProgressDialog();
            return false;
        }

        if(!LoginActivity.isNetworkAvailable(getApplicationContext())) {
            dismissProgressDialog();
            Toast.makeText(getBaseContext(), R.string.no_internet, Toast.LENGTH_LONG).show();
            return false;
        }


        mAuth = FirebaseAuth.getInstance();

        // https://firebase.google.com/docs/auth/android/password-auth (modified compared to original)
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            dismissProgressDialog();
                            Toast.makeText(RegisterActivity.this, R.string.account_exists,
                                    Toast.LENGTH_SHORT).show();
                        }
                        else { // Let's create an User object which will store all the necessary user information including all the errands related to the user
                            String uid = task.getResult().getUser().getUid();
                            User user = new User(name, phoneN, email, "None");
                            myRef.child("userData").child(uid).setValue(user);
                        }

                        // ...
                    }
                });

        return true;
    }

    // Scrolling to error messages http://stackoverflow.com/questions/6831671/is-there-a-way-to-programmatically-scroll-a-scroll-view-to-a-specific-edit-text (modified compared to original)
    protected void scrollToErrorMessage(final ScrollView scrollView, final TextInputEditText textInputEditText) {
        if (!scrolledToError) {
            scrollView.post(new Runnable() {
                @Override
                public void run() {
                    scrollView.smoothScrollTo(0, textInputEditText.getBottom());
                }
            });
            scrolledToError = true;
        }
    }

    /**
     * A method for hiding the ProgressDialog.
     */
    protected void dismissProgressDialog() {
        try {
            registerProgressDialog.dismiss();
        } catch (Exception e) {
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }
}
