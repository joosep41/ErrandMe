package ee.jooseptenn.errandme.fragments;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ee.jooseptenn.errandme.R;
import ee.jooseptenn.errandme.activities.LoginActivity;
import ee.jooseptenn.errandme.activities.MainActivity;
import ee.jooseptenn.errandme.baseclasses.Errand;
import ee.jooseptenn.errandme.baseclasses.User;
import ee.jooseptenn.errandme.services.ErrandStateService;

import static android.content.Context.LOCATION_SERVICE;

/**
 * A fragment to add new errands.
 */

public class AddFragment extends Fragment {

    public static FirebaseDatabase database;
    public static DatabaseReference myRef;
    public static ArrayAdapter<CharSequence> adapter;
    private static Activity mActivity;

    private static TextInputEditText titleEditText;
    private static TextInputEditText descriptionEditText;
    private static TextInputEditText locationEditText;
    private static TextInputEditText payEditText;
    private static TextInputEditText timeEditText;


    private static ScrollView scrollView;
    private static boolean scrolledToError;

    public static Dialog inaccurateLocationDialog;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mActivity = getActivity();

        // notify new users about location determination inaccuracy
        final SharedPreferences sharedPreferences = mActivity.getSharedPreferences(
                "ee.jooseptenn.errandme", Context.MODE_PRIVATE);
        boolean inaccuracyDialogShown = sharedPreferences.getBoolean(getString(R.string.add_inaccuracy), false);
        if (!inaccuracyDialogShown) {
            showGPSInaccuracyDialog(mActivity);
        }

        // Initialize necessary Firebase variables (reference to database)
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference();

        View view = inflater.inflate(R.layout.add_layout, container, false);

        // Set up spinner
        Spinner spinner = (Spinner) view.findViewById(R.id.currencySpinner); // https://developer.android.com/guide/topics/ui/controls/spinner.html
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // Get layout elements
        final CheckBox locationCheckBox = (CheckBox) view.findViewById(R.id.checkbox_location);
        final CheckBox timeCheckBox = (CheckBox) view.findViewById(R.id.checkbox_time);
        descriptionEditText = (TextInputEditText) view.findViewById(R.id.description);
        locationEditText = (TextInputEditText) view.findViewById(R.id.locationEditText);
        titleEditText = (TextInputEditText) view.findViewById(R.id.title);
        payEditText = (TextInputEditText) view.findViewById(R.id.pay);
        final Spinner currencySpinner = (Spinner) view.findViewById(R.id.currencySpinner);
        timeEditText = (TextInputEditText) view.findViewById(R.id.estimatedTime);
        scrollView = (ScrollView) view.findViewById(R.id.addErrandScrollView);

        // Handle location CheckBox changes
        locationCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    locationEditText.setHintTextColor(getResources().getColor(R.color.lightGray));
                    locationEditText.setEnabled(false);
                }
                else {
                    locationEditText.setHintTextColor(descriptionEditText.getHintTextColors());
                    locationEditText.setEnabled(true);
                }
            }
        });

        // Handle time CheckBox changes
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

        Button confirmButton = (Button) view.findViewById(R.id.confirm);

        // Handle confirm button click
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addErrand(getActivity(), locationCheckBox, timeCheckBox, titleEditText, descriptionEditText, locationEditText, payEditText, currencySpinner, timeEditText, false, null);
            }
        });
        return view;
    }

    /**
     * A method that checks if everything that the user entered in the errand adding form is valid, gets the proper location and address,
     * checks if it is possible to add or edit an errand (checks internet connection, location availability) and calls a method writeNewErrand or editErrand depending from the value of the edit boolean.
     *
     * @param activity            an Activity, needed for sending toasts, calling other static methods and used by Geocoder
     * @param locationCheckBox    a CheckBox which specifies if current location is used (when checked) or the user specified location is used (when unchecked)
     * @param titleEditText       an EditText where the errand title is entered
     * @param descriptionEditText an EditText where the errand description is entered
     * @param locationEditText    an EditText where the errand location is entered (address)
     * @param payEditText         an EditText where the pay for the errand is entered
     * @param currencySpinner     a Spinner that allows to choose the currency for the errand
     * @param edit                a boolean that is true when the operation to be performed is editing an existing errand and false if a new errand should be added
     * @param errandId            a String which is null if a new errand has to be added otherwise the id of the errand that will be edited
     */
    public static void addErrand(Activity activity, CheckBox locationCheckBox, CheckBox timeCheckBox, TextInputEditText titleEditText, TextInputEditText descriptionEditText, TextInputEditText locationEditText, TextInputEditText payEditText, Spinner currencySpinner, TextInputEditText timeEditText, boolean edit, String errandId) {
        // Get user entered values
        String currency = currencySpinner.getSelectedItem().toString();
        String title = titleEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String pay = payEditText.getText().toString().trim();
        String loc = locationEditText.getText().toString().trim();
        String time = timeEditText.getText().toString().trim();

        double latitude = 0;
        double longitude = 0;
        Location locat = MainActivity.getLocation(); // Get user's current location
        if (locat != null) {
            latitude = locat.getLatitude();
            longitude = locat.getLongitude();
        }

        boolean properInput = true;
        boolean locationEnabled = !locationCheckBox.isChecked();
        boolean timeExists = timeCheckBox.isChecked();

        String timeValue = "";
        if (timeExists) {
            timeValue = time;
        }
        else {
            timeValue = "none";
        }

        scrolledToError = false;

        if (!edit) { // if not directed from EditDialogFragment, check user input validity
            if (title.equals("")) {
                titleEditText.setError(mActivity.getString(R.string.enter_title));
                properInput = false;
                scrollToErrorMessage(scrollView, titleEditText);
            }
            if (title.length() > 200) {
                titleEditText.setError(mActivity.getString(R.string.long_title));
                properInput = false;
                scrollToErrorMessage(scrollView, titleEditText);
            }
            if (description.equals("")) {
                descriptionEditText.setError(mActivity.getString(R.string.enter_description));
                properInput = false;
                scrollToErrorMessage(scrollView, descriptionEditText);
            }
            if (description.length() > 2000) {
                descriptionEditText.setError(mActivity.getString(R.string.long_description));
                properInput = false;
                scrollToErrorMessage(scrollView, descriptionEditText);
            }
            if (locationEnabled && loc.equals("")) {
                locationEditText.setError(mActivity.getString(R.string.enter_location));
                properInput = false;
                scrollToErrorMessage(scrollView, locationEditText);
            }
            if (pay.length() > 20) {
                payEditText.setError(mActivity.getString(R.string.long_pay));
                properInput = false;
                scrollToErrorMessage(scrollView, payEditText);
            }
            if (pay.equals("")) {
                payEditText.setError(mActivity.getString(R.string.enter_pay));
                properInput = false;
                scrollToErrorMessage(scrollView, payEditText);
            } else if (!isNumber(pay)) {
                payEditText.setError(mActivity.getString(R.string.enter_number));
                properInput = false;
                scrollToErrorMessage(scrollView, payEditText);
            } else if (isNumber(pay) && !isPositive(pay)) {
                payEditText.setError(mActivity.getString(R.string.enter_positive_number));
                properInput = false;
                scrollToErrorMessage(scrollView, payEditText);
            } else if (timeExists && !isNumber(time)) {
                timeEditText.setError(mActivity.getString(R.string.enter_time));
                properInput = false;
                scrollToErrorMessage(scrollView, timeEditText);
            } else if (isNumber(time) && !isPositive(time)) {
                payEditText.setError(mActivity.getString(R.string.enter_positive_number));
                properInput = false;
                scrollToErrorMessage(scrollView, timeEditText);
            }
        }
        if (!LoginActivity.isNetworkAvailable(activity.getApplicationContext())) { // Check network availability
            Toast.makeText(activity, R.string.no_internet, Toast.LENGTH_SHORT).show();
        }
        else if (properInput) { // If user input is OK
            if (locationEnabled) { // If using manually entered location
                String location = locationEditText.getText().toString();
                boolean isValidLocation = true;
                if (!edit)
                    isValidLocation = validLocationCheck(activity, location); // Check if entered location is valid
                String addressRaw = "";
                if (isValidLocation) { // If user entered location is valid

                    Geocoder geocoder = new Geocoder(activity);  // http://stackoverflow.com/questions/9409195/how-to-get-complete-address-from-latitude-and-longitude
                    List<Address> addresses = null;

                    try { // Try to get exact address and coordinates
                        addresses = geocoder.getFromLocationName(location, 1);

                        if (addresses.size() > 0) {

                            Address address = addresses.get(0);
                            latitude = address.getLatitude();
                            longitude = address.getLongitude();

                            String country = addresses.get(0).getCountryName();
                            String city = addresses.get(0).getLocality();
                            String streetAndHouse = addresses.get(0).getAddressLine(0);
                            String zipCode = addresses.get(0).getPostalCode();

                            addressRaw = getCorrectAddress(country, city, streetAndHouse, zipCode);
                        }
                    } catch (IOException e) {
                    }

                    if (!edit) { // If new errand
                        writeNewErrand(title, description, String.valueOf(latitude) + "," + String.valueOf(longitude), addressRaw, pay, currency, timeValue);
                    }
                    else // If editing existing errand
                        editErrand(title, description, String.valueOf(latitude) + "," + String.valueOf(longitude), addressRaw, pay, currency, timeValue, errandId);
                }
                else {
                    if (!LoginActivity.isNetworkAvailable(activity.getApplicationContext()))
                        Toast.makeText(activity, R.string.no_internet, Toast.LENGTH_LONG).show();
                    else {
                        locationEditText.setError(mActivity.getString(R.string.valid_location));
                        scrollToErrorMessage(scrollView, locationEditText);
                    }
                }
            } else { // Using user's current location to add/edit errand

                boolean providersEnabled = true;
                LocationManager locationManager = (LocationManager) activity.getSystemService(LOCATION_SERVICE);
                providersEnabled = MainActivity.checkLocationProviders(locationManager);
                if (!edit && !providersEnabled) { // Location providers not available
                    MainActivity.showAlert(activity, R.string.add_errand_enable_location);
                }
                else if (!edit && ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
                    ((MainActivity) activity).askPermission(); // No location permission. Ask for permission
                }
                else {
                    Geocoder geocoder; // http://stackoverflow.com/questions/9409195/how-to-get-complete-address-from-latitude-and-longitude
                    List<Address> addresses;
                    geocoder = new Geocoder(activity, Locale.getDefault());
                    try {
                        addresses = geocoder.getFromLocation(latitude, longitude, 1);
                        String country = addresses.get(0).getCountryName();
                        String city = addresses.get(0).getLocality();
                        String streetAndHouse = addresses.get(0).getAddressLine(0);
                        String zipCode = addresses.get(0).getPostalCode();
                        String address = getCorrectAddress(country, city, streetAndHouse, zipCode);

                        if (!edit)
                            writeNewErrand(title, description, String.valueOf(latitude) + "," + String.valueOf(longitude), address, pay, currency, timeValue);
                        else
                            editErrand(title, description, String.valueOf(latitude) + "," + String.valueOf(longitude), address, pay, currency, timeValue, errandId);
                    } catch (Exception e) {
                        Toast.makeText(activity, R.string.issue_getting_location, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    // A method that displays a dialog that notifies the user about the inaccuracy of location determination, and gives recommendations on how to get the most accurate location.
    private static void showGPSInaccuracyDialog(final Activity activity) {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
        dialogBuilder.setMessage(R.string.location_inaccuracy);
        dialogBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                inaccurateLocationDialog = null;
                final SharedPreferences sharedPreferences = activity.getSharedPreferences(
                        "ee.jooseptenn.errandme", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean(activity.getString(R.string.add_inaccuracy), true);
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

    // Scrolling to error messages http://stackoverflow.com/questions/6831671/is-there-a-way-to-programmatically-scroll-a-scroll-view-to-a-specific-edit-text
    protected static void scrollToErrorMessage(final ScrollView scrollView, final TextInputEditText textInputEditText) {
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
     * A method that removes duplicate names from the address of the errand. This method is needed when a vague address is entered (Paris for example).
     *
     * @param country        the country where the errand has to be done
     * @param city           the city where the errand has to be done
     * @param streetAndHouse specific location details of the errand location (like street and house number)
     * @param zipCode        the zip code of the location where the errand has to be done
     * @return               a new corrected string without duplicate names (e.g. UK, London, London -> UK, London)
     */
    private static String getCorrectAddress(String country, String city, String streetAndHouse, String zipCode) {
        String addressRaw = "";

        if (city == null)
            city = "";
        else
            city = city + ",";

        String streetAndHouseCorrect = "";
        String[] streetAndHouseParts = streetAndHouse.split(",");
        for (String part: streetAndHouseParts) {
            if (!part.equals("null")) {
                if (streetAndHouseCorrect.equals(""))
                    streetAndHouseCorrect += part;
                else
                    streetAndHouse += "," + part;
            }
        }

        if (!city.equals("") && city.substring(0, city.length()-1).equals(streetAndHouseCorrect)) {
            streetAndHouseCorrect = "";
        }

        if (zipCode == null)
            zipCode = "";
        else if (!streetAndHouseCorrect.equals(""))
            zipCode = "," + zipCode;

        addressRaw = country + "," + city + streetAndHouseCorrect + zipCode;
        if (addressRaw.endsWith(","))
            addressRaw = addressRaw.substring(0, addressRaw.length()-1);

        String parts[] = addressRaw.split(",");
        if (parts.length == 2 && parts[0].equals(parts[1]))
            return parts[0];

        return addressRaw;
    }

    /**
     * A method used to write a new errand to the database and renew the user's data.
     *
     * @param title       the title of the errand that will be added to the database
     * @param description the description of the errand that will be added to the database
     * @param location    the location (comma separated latitude and longitude) of the errand that will be added to the database
     * @param address     the address of the errand that will be added to the database
     * @param pay         the pay of the errand that will be added to the database
     * @param currency    the currency of the errand that will be added to the database
     */
    private static void writeNewErrand(String title, String description, String location, String address, String pay, String currency, String estimatedTime) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        ArrayList<Errand> addedErrands = ErrandStateService.getUserAddedErrands();

        final Errand errand = new Errand(uid, title, description, location, address, pay, currency, estimatedTime);
        boolean passes = true;

        for (Errand err : addedErrands) {
            if (err.getAssignerId().equals(uid) && err.getTitle().equals(errand.getTitle())) {
                Toast.makeText(mActivity, R.string.existing_errand, Toast.LENGTH_LONG).show();
                passes = false;
            }
        }

        if (passes) {
            User user = ErrandStateService.user;

            DatabaseReference pushRef = myRef.child("errands").push();
            String pushedKey = pushRef.getKey();
            errand.setId(pushedKey);
            pushRef.setValue(errand);

            user.addAddedErrands(errand.getId());
            //timeTest = System.currentTimeMillis();
            myRef.child("userData").child(uid).setValue(user);

            if (user.getSharedInformation().equals("None"))
                displayNoSharedInformationDialog();
            ((MainActivity)mActivity).selectItem(0);

        }
    }

    /**
     * A method that checks if the user has already added an errand with the specified title and tells the user to change the title if it is already present in the user's activeAddedErrands.
     * If the title is unique updateErrand is called to modify the errand in the database and to update the user's information in the database.
     *
     * @param title       the title of the edited errand
     * @param description the description of the edited errand
     * @param location    the location (comma separated latitude and longitude) of the edited errand
     * @param address     the address of the edited errand
     * @param pay         the pay of the edited errand
     * @param currency    the currency of the edited errand
     * @param errandId    the errandId of the edited errand
     */
    private static void editErrand(String title, String description, String location, String address, String pay, String currency, String estimatedTime, String errandId) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        User user = ErrandStateService.user;
        Errand errand = new Errand(errandId, uid, title, description, location, address, pay, currency, estimatedTime);

        boolean passes = true;
        ArrayList<Errand> addedErrands = ErrandStateService.getUserAddedErrands();

        for (Errand e: addedErrands) {
            if (e.getAssignerId().equals(uid) && e.getTitle().equals(errand.getTitle()) && !e.getId().equals(errandId)) {
                Toast.makeText(mActivity, R.string.existing_errand, Toast.LENGTH_LONG).show();
                passes = false;
            }
        }

        if (passes) {
            updateErrand(errand);
        }
    }

    /**
     * A method that is used to edit an existing errand in the database.
     *
     * @param errand the edited errand
     */
    private static void updateErrand(final Errand errand) {
        //errandUpdateTime = System.currentTimeMillis();
        myRef.child("errands").child(errand.getId()).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                Errand e = mutableData.getValue(Errand.class);
                if (e == null) {
                    return Transaction.success(mutableData);
                }
                if (e.getAccepterId().equals("")) {
                    e.setDistanceFromUser(errand.getDistanceFromUser());
                    e.setAddress(errand.getAddress());
                    e.setLocation(errand.getLocation());
                    e.setPay(errand.getPay());
                    e.setTitle(errand.getTitle());
                    e.setDescription(errand.getDescription());
                    e.setCurrency(errand.getCurrency());
                    e.setEstimatedTime(errand.getEstimatedTime());

                    mutableData.setValue(e);
                    return Transaction.success(mutableData);
                }
                else {
                    return Transaction.abort();
                }
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
            }
        });
    }

    // Encourage user to share some contact information with other users (currently sharing nothing)
    protected static void displayNoSharedInformationDialog() {
        android.app.AlertDialog.Builder dialog = new android.app.AlertDialog.Builder(mActivity);
        dialog.setMessage(R.string.no_shared_information);
        dialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dialog.create().show();
    }

    /**
     * A method that checks if the user entered a valid location in the errand adding form.
     *
     * @param activity an Activity, used by Geocoder
     * @param location the address that the user entered in the errand adding form
     * @return true if the location is a valid location false otherwise
     */
    public static boolean validLocationCheck(Activity activity, String location) {

        if (location.equals("")) {
            return false;
        }
        else {
            Geocoder geocoder = new Geocoder(activity);
            List<Address> addresses = null;
            try {
                addresses = geocoder.getFromLocationName(location, 1);
                Address address = addresses.get(0);
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    /**
     * A method that checks if a string is a number.
     *
     * @param pay the string which is checked
     * @return    true if the entered string is a number false otherwise
     */
    public static boolean isNumber(String pay) {
        try {
            double number = Double.valueOf(pay.replace(",","."));
            boolean pass = true;
            int dotCount = 0;
            for (int i = 0; i < pay.length(); i++) {
                if (!Character.isDigit(pay.charAt(i))) {
                    if (dotCount == 0 && pay.charAt(i) == '.') {
                        dotCount++;
                    }
                    else if (i == 0 && pay.charAt(i) == '-') {
                    }
                    else {
                        pass = false;
                    }
                }
            }
            if (pay.charAt(pay.length()-1) == '.')
                pass = false;
            return pass;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * A method that checks if the entered number (entered as a String) is a positive number.
     *
     * @param pay the string which is checked
     * @return    true if the entered string is a positive number, false if the entered string is not a positive number and throws an exception if the entered string is not a number
     */
    public static boolean isPositive(String pay) {
        return Double.valueOf(pay) > 0;
    }
}