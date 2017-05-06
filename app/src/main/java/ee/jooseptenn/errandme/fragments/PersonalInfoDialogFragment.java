package ee.jooseptenn.errandme.fragments;


import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import ee.jooseptenn.errandme.R;
import ee.jooseptenn.errandme.activities.RegisterActivity;
import ee.jooseptenn.errandme.baseclasses.User;
import ee.jooseptenn.errandme.services.ErrandStateService;

/**
 * A dialog that is used to choose the personal information that the user wishes to share with the other application users.
 */

public class PersonalInfoDialogFragment extends DialogFragment {

    LayoutInflater inflater;
    View view;
    RadioGroup mRadioGroup;
    TextInputEditText phoneEditText;
    TextInputLayout phoneContainer;
    TextView phoneTextView;
    TextView radioTextView;
    boolean validInput = true;

    public static FirebaseDatabase database;
    public static DatabaseReference myRef;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        inflater = getActivity().getLayoutInflater();
        view = inflater.inflate(R.layout.private_info_layout, null);

        // Set up database reference
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference();

        Bundle b = getArguments();
        final boolean isPhone = b.getBoolean("phone", false);
        final boolean isChoice = b.getBoolean("choice", false);

        // Get layout elements
        phoneTextView = (TextView) view.findViewById(R.id.phoneNumberTextView);
        radioTextView = (TextView) view.findViewById(R.id.chooseInfoTextView);
        phoneEditText = (TextInputEditText) view.findViewById(R.id.phoneNumberEditText);
        phoneContainer = (TextInputLayout) view.findViewById(R.id.phoneContainer);
        mRadioGroup = (RadioGroup) view.findViewById(R.id.radioGroup);

        if (isPhone && !isChoice) {
            radioTextView.setVisibility(View.GONE);
            mRadioGroup.setVisibility(View.GONE);
        }
        else if (isChoice && !isPhone) {
            phoneTextView.setVisibility(View.GONE);
            phoneContainer.setVisibility(View.GONE);
            phoneEditText.setVisibility(View.GONE);
        }

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
                        User user = ErrandStateService.user;
                        if (isChoice) {
                            int radioButtonID = mRadioGroup.getCheckedRadioButtonId();
                            View radioButton = mRadioGroup.findViewById(radioButtonID);
                            int buttonId = mRadioGroup.indexOfChild(radioButton);

                            String sharedInfo = "";
                            switch (buttonId) {
                                case 0:
                                    sharedInfo = "phone";
                                    break;
                                case 1:
                                    sharedInfo = "email";
                                    break;
                                default:
                                    sharedInfo = "both";
                            }

                            user.setSharedInformation(sharedInfo);
                        }
                        if (isPhone) {
                            String phone = phoneEditText.getText().toString();

                            if (!phone.isEmpty() && RegisterActivity.hasOnlyDigits(phone)) { // Check if input is correct
                                user.setPhoneNumber(phone);
                                validInput = true;
                            }
                            else {
                                phoneEditText.setError(getString(R.string.valid_phone));
                                validInput = false;
                            }
                        }
                        if (validInput) {
                            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                            myRef.child("userData").child(uid).setValue(user);
                            dialog.dismiss();
                        }
                    }
                });
            }
        });

        return dialog;

    }
}
