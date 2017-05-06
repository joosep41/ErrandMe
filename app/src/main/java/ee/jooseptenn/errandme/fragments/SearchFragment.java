package ee.jooseptenn.errandme.fragments;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import ee.jooseptenn.errandme.R;
import ee.jooseptenn.errandme.activities.LoginActivity;
import ee.jooseptenn.errandme.activities.MainActivity;
import ee.jooseptenn.errandme.baseclasses.User;
import ee.jooseptenn.errandme.services.ErrandStateService;

import static android.content.Context.LOCATION_SERVICE;

/**
 * A fragment that displays the searching form to the user.
 */

public class SearchFragment extends Fragment {

    private CheckBox enableSearchCheckBox;
    private CheckBox matchAllCheckBox;
    private Activity mActivity;

    public static Dialog inaccurateLocationDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mActivity = getActivity();
        View view = inflater.inflate(R.layout.search_layout, container, false);

        User user = ErrandStateService.user;
        if (user != null && user.getSharedInformation().equals("None")) { // Tell user to share contact information
            displayNoSharedInformationDialog(getContext());
        }

        // Get layout elements
        final TextInputEditText searchText = (TextInputEditText) view.findViewById(R.id.searchTextBox);
        enableSearchCheckBox = (CheckBox) view.findViewById(R.id.keywordsCheckBox);
        matchAllCheckBox = (CheckBox) view.findViewById(R.id.keywordsCheckBoxAll);
        final EditText payEditText = (EditText) view.findViewById(R.id.minimumPayTextBox);
        final CheckBox payCheckBox = (CheckBox) view.findViewById(R.id.payCheckBox);
        Button searchButton = (Button) view.findViewById(R.id.search_button);
        final EditText distanceEditText = (EditText) view.findViewById(R.id.search_radius);
        final EditText locationEditText = (EditText) view.findViewById(R.id.custom_location);
        final CheckBox locationCheckBox = (CheckBox) view.findViewById(R.id.locationCheckBox);
        final EditText timeEditText = (EditText) view.findViewById(R.id.timeTextBox);
        final CheckBox timeCheckBox = (CheckBox) view.findViewById(R.id.timeCheckBox);

        final SharedPreferences sharedPreferences = mActivity.getSharedPreferences(
                "ee.jooseptenn.errandme", Context.MODE_PRIVATE);

        enableSearchCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    searchText.setEnabled(true);
                    matchAllCheckBox.setEnabled(true);
                }
                else {
                    searchText.setEnabled(false);
                    matchAllCheckBox.setEnabled(false);
                }
            }
        });

        payCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    payEditText.setHintTextColor(searchText.getHintTextColors());
                    payEditText.setEnabled(true);
                }
                else {
                    payEditText.setEnabled(false);
                    payEditText.setHintTextColor(getResources().getColor(R.color.lightGray));
                }
            }
        });

        locationCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    locationEditText.setEnabled(false);
                    locationEditText.setHintTextColor(getResources().getColor(R.color.lightGray));
                    boolean inaccuracyDialogShown = sharedPreferences.getBoolean(getString(R.string.search_inaccuracy), false);
                    if (!inaccuracyDialogShown) {
                        showGPSInaccuracyDialog(mActivity); // notify new users about location determination inaccuracy
                    }
                }
                else {
                    locationEditText.setEnabled(true);
                    locationEditText.setHintTextColor(searchText.getHintTextColors());
                }
            }
        });

        timeCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    timeEditText.setEnabled(true);
                    timeEditText.setHintTextColor(searchText.getHintTextColors());
                }
                else {
                    timeEditText.setEnabled(false);
                    timeEditText.setHintTextColor(getResources().getColor(R.color.lightGray));
                }
            }
        });

        // Handle search button click
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                LocationManager locationManager = (LocationManager) getContext().getSystemService(LOCATION_SERVICE);
                boolean providersEnabled = MainActivity.checkLocationProviders(locationManager);

                if (locationCheckBox.isChecked() && providersEnabled || !locationCheckBox.isChecked()) { // Decide if location permission is required
                    if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) { // Location permission not granted
                        ((MainActivity)getActivity()).askPermission(); // No location permission. Ask permission
                    }
                    else {
                        if (LoginActivity.isNetworkAvailable(getContext())) { // Check if network is available

                            String pay = payEditText.getText().toString();
                            // Check input validity
                            if (payCheckBox.isChecked() && !AddFragment.isNumber(pay)) {
                                payEditText.setError(getString(R.string.enter_number));
                            } else if (payCheckBox.isChecked() && AddFragment.isNumber(pay) && !AddFragment.isPositive(pay)) {
                                payEditText.setError(getString(R.string.enter_positive_number));
                            } else {

                                String distanceText = distanceEditText.getText().toString();

                                if (distanceText.equals("")) {
                                    distanceEditText.setError(getString(R.string.enter_distance));
                                } else if (!AddFragment.isNumber(distanceText)) {
                                    distanceEditText.setError(getString(R.string.distance_not_numeric));
                                } else if (AddFragment.isNumber(distanceText) && !AddFragment.isPositive(distanceText)) {
                                    distanceEditText.setError(getString(R.string.enter_positive_distance));
                                } else {

                                    String location = locationEditText.getText().toString();
                                    boolean validLocation = AddFragment.validLocationCheck(getActivity(), location);

                                    if (!locationCheckBox.isChecked() && location.equals("")) {
                                        locationEditText.setError(getString(R.string.enter_location));
                                    } else if (!validLocation && !location.equals("")) {
                                        locationEditText.setError(getString(R.string.valid_location));
                                    } else {

                                        String time = timeEditText.getText().toString();

                                        if (timeCheckBox.isChecked() && !AddFragment.isNumber(time)) {
                                            timeEditText.setError(getString(R.string.enter_number));
                                        } else if (timeCheckBox.isChecked() && AddFragment.isNumber(time) && !AddFragment.isPositive(time)) {
                                            timeEditText.setError(getString(R.string.enter_positive_number));
                                        } else { // Input is valid, continue search process in SearchResultsFragment

                                            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                                            Fragment fragment = new SearchResultsFragment();
                                            Bundle args = new Bundle();
                                            args.putBoolean("enableSearchCheckBox", enableSearchCheckBox.isChecked());
                                            args.putBoolean("matchAllCheckBox", matchAllCheckBox.isChecked());
                                            args.putBoolean("payCheckBox", payCheckBox.isChecked());
                                            args.putBoolean("timeCheckBox", timeCheckBox.isChecked());
                                            args.putString("searchText", searchText.getText().toString());
                                            args.putString("payText", payEditText.getText().toString());
                                            args.putDouble("distance", Double.valueOf(distanceText));
                                            args.putString("time", timeEditText.getText().toString());
                                            if (!locationCheckBox.isChecked()) {
                                                args.putString("location", locationEditText.getText().toString());
                                            } else {
                                                args.putString("location", "current");
                                            }
                                            fragment.setArguments(args);
                                            fragmentManager.beginTransaction()
                                                    .replace(R.id.content_frame, fragment)
                                                    .addToBackStack(null)
                                                    .commit();
                                        }
                                    }
                                }
                            }
                        } else {
                            Toast.makeText(getActivity(), getString(R.string.no_internet), Toast.LENGTH_LONG).show();
                        }
                    }
                }
                else {
                    MainActivity.showAlert(getActivity(), R.string.search_errands_no_location);
                }
            }
        });

        return view;
    }

    // Encourage users to share some contact information (Currently not sharing)
    protected void displayNoSharedInformationDialog(Context context) {
        android.app.AlertDialog.Builder dialog = new android.app.AlertDialog.Builder(context);
        dialog.setMessage(R.string.no_shared_information);
        dialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dialog.create().show();
    }

    // A method that displays a dialog that notifies the user about the inaccuracy of location determination, and gives recommendations on how to get the most accurate location.
    private static void showGPSInaccuracyDialog(final Activity activity) {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
        dialogBuilder.setMessage(R.string.location_inaccuracy_search);
        dialogBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                inaccurateLocationDialog = null;
                final SharedPreferences sharedPreferences = activity.getSharedPreferences(
                        "ee.jooseptenn.errandme", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean(activity.getString(R.string.search_inaccuracy), true);
                editor.apply();
            }
        });

        inaccurateLocationDialog = dialogBuilder.show();
    }

    @Override
    public void onPause() {
        if (inaccurateLocationDialog != null) {
            inaccurateLocationDialog.dismiss();
            inaccurateLocationDialog = null;
        }
        super.onPause();
    }
}
