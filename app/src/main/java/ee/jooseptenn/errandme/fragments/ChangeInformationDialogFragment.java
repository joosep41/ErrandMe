package ee.jooseptenn.errandme.fragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import ee.jooseptenn.errandme.R;
import ee.jooseptenn.errandme.activities.RegisterActivity;
import ee.jooseptenn.errandme.baseclasses.User;
import ee.jooseptenn.errandme.services.ErrandStateService;

/**
 * A dialog for changing the user's contact e-mail address and/or phone number.
 */

public class ChangeInformationDialogFragment extends DialogFragment {

    LayoutInflater inflater;
    View view;

    public static FirebaseDatabase database;
    public static DatabaseReference myRef;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        inflater = getActivity().getLayoutInflater();
        view = inflater.inflate(R.layout.change_contact_info_layout, null);

        // Get layout elements
        final EditText emailEditText = (EditText) view.findViewById(R.id.email);
        final EditText phoneEditText = (EditText) view.findViewById(R.id.phone);

        // Get current email and phone number
        final String beginningEmail = emailEditText.getText().toString();
        final String beginningPhone = phoneEditText.getText().toString();

        // Get database references
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference();
        final User user = ErrandStateService.user;

        myRef.child("userData").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                User u = dataSnapshot.getValue(User.class); // Get current contact information values from database
                emailEditText.setText(u.getEmail());
                phoneEditText.setText(u.getPhoneNumber());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        // Dialog creation and setOnShowListener - http://stackoverflow.com/questions/2620444/how-to-prevent-a-dialog-from-closing-when-a-button-is-clicked/9523257 (modified compared to original)
        final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setView(view)
                .setPositiveButton(R.string.confirm, null)
                .setNegativeButton(R.string.cancel, null)
                .create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface dialog) {

                Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String newEmail = emailEditText.getText().toString();
                        String newPhone = phoneEditText.getText().toString();

                        if (newEmail.equals(beginningEmail) && newPhone.equals(beginningPhone)) {
                            dialog.dismiss();
                        }
                        else {
                            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

                            boolean correctInput = true;
                            if (newEmail.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                                correctInput = false;
                                emailEditText.setError(getString(R.string.invalid_email_format));
                            }
                            if (newPhone.isEmpty() || !RegisterActivity.hasOnlyDigits(newPhone)) {
                                correctInput = false;
                                phoneEditText.setError(getString(R.string.valid_phone));
                            }
                            if (correctInput) {
                                user.setEmail(newEmail);
                                user.setPhoneNumber(newPhone);
                                myRef.child("userData").child(uid).setValue(user);
                                dialog.dismiss();
                            }
                        }
                    }
                });
            }
        });

        return dialog;
    }


}
