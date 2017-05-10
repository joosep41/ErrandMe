package ee.jooseptenn.errandme.fragments;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ScrollView;
import android.widget.Spinner;

import com.google.firebase.database.FirebaseDatabase;

import ee.jooseptenn.errandme.R;
import ee.jooseptenn.errandme.activities.LoginActivity;
import ee.jooseptenn.errandme.activities.MainActivity;

import static android.content.Context.LOCATION_SERVICE;


/**
 * A dialog for editing an errand.
 */

public class EditDialogFragment extends DialogFragment {

    LayoutInflater inflater;
    View view;
    private static boolean scrolledToError;


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        inflater = getActivity().getLayoutInflater();
        view = inflater.inflate(R.layout.errand_edit_layout, null);
        final Bundle args = getArguments();

        // Get layout elements
        final CheckBox locationCheckBox = (CheckBox) view.findViewById(R.id.checkbox_location);
        final TextInputEditText titleEditText = (TextInputEditText) view.findViewById(R.id.title);
        final TextInputEditText descriptionEditText = (TextInputEditText) view.findViewById(R.id.description);
        final TextInputEditText payEditText = (TextInputEditText) view.findViewById(R.id.pay);
        final TextInputEditText addressEditText = (TextInputEditText) view.findViewById(R.id.location);
        final Spinner currencySpinner = (Spinner) view.findViewById(R.id.currencySpinner);
        final TextInputEditText timeEditText = (TextInputEditText) view.findViewById(R.id.estimatedTime);
        final CheckBox timeCheckBox = (CheckBox) view.findViewById(R.id.checkbox_time);
        final ScrollView scrollView = (ScrollView) view.findViewById(R.id.editErrandScrollView);

        // Set values
        titleEditText.setText(args.getString("title"));
        descriptionEditText.setText(args.getString("description"));
        payEditText.setText(args.getString("pay"));
        addressEditText.setText(args.getString("location"));
        final String errandId = args.getString("errandId");

        String timeText = args.getString("time");

        if (timeText.equals("none")) {
            timeEditText.setEnabled(false);
            timeEditText.setHintTextColor(getResources().getColor(R.color.lightGray));
        }
        else {
            timeEditText.setText(timeText);
            timeCheckBox.setChecked(true);
        }

        // Set up currency spinner
        ArrayAdapter<CharSequence> adapter = MainActivity.adapter; // https://developer.android.com/guide/topics/ui/controls/spinner.html (modified compared to original)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        currencySpinner.setAdapter(adapter);
        currencySpinner.setSelection(AddFragment.adapter.getPosition(args.getString("currency")));

        // Handle location CheckBox click
        locationCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    addressEditText.setHintTextColor(getResources().getColor(R.color.lightGray));
                    addressEditText.setEnabled(false);
                }
                else {
                    addressEditText.setHintTextColor(descriptionEditText.getHintTextColors());
                    addressEditText.setEnabled(true);
                }
            }
        });

        // Handle time CheckBox click
        timeCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    timeEditText.setHintTextColor(descriptionEditText.getHintTextColors());
                    timeEditText.setEnabled(true);
                }
                else {
                    timeEditText.setHintTextColor(getResources().getColor(R.color.lightGray));
                    timeEditText.setEnabled(false);
                }
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
                        boolean providersEnabled = true;
                        LocationManager locationManager = (LocationManager) getActivity().getSystemService(LOCATION_SERVICE);
                        providersEnabled = MainActivity.checkLocationProviders(locationManager);
                        if (providersEnabled) {

                            if (AddFragment.database == null && AddFragment.myRef == null) {
                                AddFragment.database = FirebaseDatabase.getInstance();
                                AddFragment.myRef = AddFragment.database.getReference();
                            }

                            // Get user entered values
                            String title = titleEditText.getText().toString().trim();
                            String description = descriptionEditText.getText().toString().trim();
                            String location = addressEditText.getText().toString().trim();
                            String pay = payEditText.getText().toString().trim();
                            String time = timeEditText.getText().toString().trim();

                            if (LoginActivity.isNetworkAvailable(getContext())) { // check network availability
                                boolean properInput = false;
                                if (locationCheckBox.isChecked()) { // Using current location
                                    if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) { // Location permission granted
                                        properInput = checkInput(title, titleEditText, description, descriptionEditText, location, addressEditText, pay, payEditText,
                                                time, timeEditText, timeCheckBox, locationCheckBox, scrollView);
                                    } else {
                                        ((MainActivity) getActivity()).askPermission(); // No location permission. Ask for permission
                                    }
                                } else {
                                    properInput = checkInput(title, titleEditText, description, descriptionEditText, location, addressEditText, pay, payEditText,
                                            time, timeEditText, timeCheckBox, locationCheckBox, scrollView);

                                    if (!AddFragment.validLocationCheck(getActivity(), location)) { // check location validity
                                        addressEditText.setError(getActivity().getString(R.string.valid_location));
                                        properInput = false;
                                        scrollToErrorMessage(scrollView, addressEditText);
                                    }
                                }
                                if (properInput) { // if user input is valid, proceed to edit existing errand
                                    AddFragment.addErrand(getActivity(), locationCheckBox, timeCheckBox, titleEditText, descriptionEditText, addressEditText, payEditText, currencySpinner, timeEditText, true, errandId);
                                    dialog.dismiss();
                                }
                            } else {
                                displayNoInternetDialog(getContext());
                            }
                        } else {
                            MainActivity.showAlert(getActivity(), R.string.edit_errand_no_location);
                        }
                    }
                });
            }
        });

        return dialog;
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
     * A method to check if user entered input is valid
     * @param title               title to check
     * @param titleEditText       the titleEditText to scroll to if title is invalid
     * @param description         description to check
     * @param descriptionEditText the descriptionEditText to scroll to if description is invalid
     * @param location            location to check
     * @param addressEditText     addressEditText to scroll to if location is invalid
     * @param pay                 pay to check
     * @param payEditText         payEditText to scroll to if pay is invalid
     * @param time                time to check
     * @param timeEditText        timeEditText to scroll to if time is invalid
     * @param timeCheckBox        time CheckBox
     * @param locationCheckBox    location CheckBox
     * @param scrollView          ScrollView that contains the layout elements
     * @return                    true if input is valid false otherwise
     */
    public boolean checkInput(String title, TextInputEditText titleEditText, String description, TextInputEditText descriptionEditText, String location,
                              TextInputEditText addressEditText, String pay, TextInputEditText payEditText,  String time, TextInputEditText timeEditText, CheckBox timeCheckBox,
                              CheckBox locationCheckBox, ScrollView scrollView) {
        boolean properInput = true;

        boolean timeExists = timeCheckBox.isChecked();
        scrolledToError = false;


        if (title.equals("")) {
            titleEditText.setError(getActivity().getString(R.string.enter_title));
            properInput = false;
            scrollToErrorMessage(scrollView, titleEditText);

        }
        if (title.length() > 200) {
            titleEditText.setError(getActivity().getString(R.string.long_title));
            properInput = false;
            scrollToErrorMessage(scrollView, titleEditText);
        }
        if (description.equals("")) {
            descriptionEditText.setError(getActivity().getString(R.string.enter_description));
            properInput = false;
            scrollToErrorMessage(scrollView, descriptionEditText);
        }
        if (description.length() > 2000) {
            descriptionEditText.setError(getContext().getString(R.string.long_description));
            properInput = false;
            scrollToErrorMessage(scrollView, descriptionEditText);
        }
        if (!locationCheckBox.isChecked() && location.equals("")) {
            addressEditText.setError(getActivity().getString(R.string.enter_location));
            properInput = false;
            scrollToErrorMessage(scrollView, addressEditText);
        }
        if (pay.length() > 20) {
            payEditText.setError(getActivity().getString(R.string.long_pay));
            properInput = false;
            scrollToErrorMessage(scrollView, payEditText);
        }
        if (pay.equals("")) {
            payEditText.setError(getActivity().getString(R.string.enter_pay));
            properInput = false;
            scrollToErrorMessage(scrollView, payEditText);
        } else if (!AddFragment.isNumber(pay)) {
            payEditText.setError(getActivity().getString(R.string.enter_number));
            properInput = false;
            scrollToErrorMessage(scrollView, payEditText);
        } else if (AddFragment.isNumber(pay) && !AddFragment.isPositive(pay)) {
            payEditText.setError(getActivity().getString(R.string.enter_positive_number));
            properInput = false;
            scrollToErrorMessage(scrollView, payEditText);
        } else if (timeExists && !AddFragment.isNumber(time)) {
            timeEditText.setError(getActivity().getString(R.string.enter_time));
            properInput = false;
            scrollToErrorMessage(scrollView, timeEditText);
        } else if (AddFragment.isNumber(time) && !AddFragment.isPositive(time)) {
            payEditText.setError(getActivity().getString(R.string.enter_positive_number));
            properInput = false;
            scrollToErrorMessage(scrollView, timeEditText);
        }

        if (AddFragment.database == null && AddFragment.myRef == null) {
            AddFragment.database = FirebaseDatabase.getInstance();
            AddFragment.myRef = AddFragment.database.getReference();
        }

        return properInput;
    }

    /**
     * A method to display a dialog when the user has no internet connection.
     * @param context context that is required to create a dialog
     */
    public static void displayNoInternetDialog(Context context){
        android.app.AlertDialog.Builder dialog = new android.app.AlertDialog.Builder(context);
        dialog.setMessage(R.string.no_internet);
        dialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dialog.create().show();
    }
}
