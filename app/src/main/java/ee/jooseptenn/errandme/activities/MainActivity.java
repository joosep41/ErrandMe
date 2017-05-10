package ee.jooseptenn.errandme.activities;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.ArrayList;
import java.util.Map;
import ee.jooseptenn.errandme.baseclasses.Errand;
import ee.jooseptenn.errandme.fragments.AddFragment;
import ee.jooseptenn.errandme.fragments.SearchResultsFragment;
import ee.jooseptenn.errandme.services.ErrandStateService;
import ee.jooseptenn.errandme.fragments.MyErrandsFragment;
import ee.jooseptenn.errandme.fragments.PersonalInfoDialogFragment;
import ee.jooseptenn.errandme.R;
import ee.jooseptenn.errandme.fragments.SearchFragment;
import ee.jooseptenn.errandme.fragments.SettingsFragment;
import ee.jooseptenn.errandme.baseclasses.User;

/**
 * This activity is responsible for managing all the fragment and navigation in the application. It also starts and controls all the services.
 */

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private FirebaseDatabase mDb;
    private DatabaseReference mRef;
    private FirebaseAuth firebaseAuth;

    private String[] mNavigationItems;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    private ActionBarDrawerToggle mDrawerToggle;
    private Bundle savedInstanceState;
    public static ArrayAdapter<CharSequence> adapter;
    private Activity mActivity;

    private UserStateResponseReceiver receiver;

    /**
     * location variables
     * https://github.com/googlesamples/android-play-location/blob/master/LocationUpdates/app/src/main/java/com/google/android/gms/location/sample/locationupdates/MainActivity.java
     */
    protected Boolean mRequestingLocationUpdates;
    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    protected LocationSettingsRequest mLocationSettingsRequest;
    private static Location location;

    /**
     * location static final values
     * https://github.com/googlesamples/android-play-location/blob/master/LocationUpdates/app/src/main/java/com/google/android/gms/location/sample/locationupdates/MainActivity.java (modified compared to original)
     */
    private static final int REQUEST_LOCATION = 1;
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    public static final long UPDATE_INTERVAL = 60 * 1000;
    public static final long FASTEST_UPDATE_INTERVAL = 20 * 1000;
    protected static final int REQUEST_CHECK_SETTINGS = 0x1;

    /**
     * Keys for storing activity state in the Bundle.
     * https://github.com/googlesamples/android-play-location/blob/master/LocationUpdates/app/src/main/java/com/google/android/gms/location/sample/locationupdates/MainActivity.java
     */
    protected final static String KEY_REQUESTING_LOCATION_UPDATES = "requesting-location-updates";
    protected final static String KEY_LOCATION = "location";

    private String uid;
    private static User user;
    public static Dialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mActivity = this;

        // set up main listeners; receive errand, user data and updates
        Intent intent = new Intent((Context) this, ErrandStateService.class);
        startService(intent); // starting ErrandStateService

        // Everything location related - https://github.com/googlesamples/android-play-location/blob/master/LocationUpdates/app/src/main/java/com/google/android/gms/location/sample/locationupdates/MainActivity.java (modified according to the needs of ErrandMe)
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();

        mRequestingLocationUpdates = true;

        // Update values using data stored in the Bundle.
        updateValuesFromBundle(savedInstanceState);

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid(); // Currently logged in user uid

        super.onCreate(savedInstanceState);
        this.savedInstanceState = savedInstanceState;
        setContentView(R.layout.activity_main);

        // Curency spinner initialization (used in the errand adding and editing view)
        adapter = ArrayAdapter.createFromResource(this,
                R.array.currency_array, android.R.layout.simple_spinner_item);

        AddFragment.adapter = this.adapter;

        // Navigation drawer (mDrawer) implementation - https://developer.android.com/training/implementing-navigation/nav-drawer.html
        mTitle = mDrawerTitle = getTitle();
        mNavigationItems = getResources().getStringArray(R.array.navigation_item_array);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        mDrawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, mNavigationItems));

        mDrawerList.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItem(position);
            }
        });

        mDrawerToggle = new ActionBarDrawerToggle(
                this,
                mDrawerLayout,
                R.string.open,
                R.string.close
        ) {
            public void onDrawerClosed(View view) {
                getSupportActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                getSupportActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        // getting Firebase related information (database reference, authentication instance)
        firebaseAuth = FirebaseAuth.getInstance();
        mDb = FirebaseDatabase.getInstance();
        mRef = mDb.getReference();

        displayErrands(); // show user the My Errands view

    }

    /**
     * https://github.com/googlesamples/android-play-location/blob/master/LocationUpdates/app/src/main/java/com/google/android/gms/location/sample/locationupdates/MainActivity.java
     * Update field (mRequestingLocationUpdates, location) with values from the bundle.
     *
     * @param savedInstanceState a savedInstanceState
     */
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Update the value of mRequestingLocationUpdates from the Bundle, and make sure that
            // the Start Updates and Stop Updates buttons are correctly enabled or disabled.
            if (savedInstanceState.keySet().contains(KEY_REQUESTING_LOCATION_UPDATES)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        KEY_REQUESTING_LOCATION_UPDATES);
            }

            // Update the value of mCurrentLocation from the Bundle and update the UI to show the
            // correct latitude and longitude.
            if (savedInstanceState.keySet().contains(KEY_LOCATION)) {
                // Since KEY_LOCATION was found in the Bundle, we can be sure that mCurrentLocation
                // is not null.
                location = savedInstanceState.getParcelable(KEY_LOCATION);
            }

        }
    }

    /**
     * https://github.com/googlesamples/android-play-location/blob/master/LocationUpdates/app/src/main/java/com/google/android/gms/location/sample/locationupdates/MainActivity.java (modified compared to original)
     * A method that starts the loation updates.
     *
     */
    protected void startLocationUpdates() {
        LocationServices.SettingsApi.checkLocationSettings(
                mGoogleApiClient,
                mLocationSettingsRequest).setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can
                        // initialize location requests here.
                        if (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
                            if (mRequestingLocationUpdates)
                                askPermission();
                        }
                        else {
                            LocationServices.FusedLocationApi.requestLocationUpdates(
                                    mGoogleApiClient, mLocationRequest, MainActivity.this);
                        }
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        if (mRequestingLocationUpdates) {
                            mRequestingLocationUpdates = false;
                            // Location settings are not satisfied, but this can be fixed
                            // by showing the user a dialog.
                            try {
                                // Show the dialog by calling startResolutionForResult(),
                                // and check the result in onActivityResult().
                                status.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                            } catch (IntentSender.SendIntentException e) {
                                // Ignore the error.
                            }
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        String errorMessage = "Location settings are inadequate, and cannot be " +
                                "fixed here. Fix in Settings.";
                        Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        mRequestingLocationUpdates = false;
                }
            }
        });
    }

    /**
     * https://github.com/googlesamples/android-play-location/blob/master/LocationUpdates/app/src/main/java/com/google/android/gms/location/sample/locationupdates/MainActivity.java
     * A method that is called after startResolutionForResult(). (modified compared to original)
     *
     * @param requestCode request code
     * @param resultCode  result code
     * @param data        calling Intent
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        mRequestingLocationUpdates = true;
                        // Nothing to do. startLocationupdates() gets called in onResume again.
                        break;
                    case Activity.RESULT_CANCELED:
                        break;
                }
                break;
        }
    }

    @Override
    protected void onResume() {
        mGoogleApiClient.connect();
        if (receiver == null) {
            IntentFilter filter = new IntentFilter(UserStateResponseReceiver.ACTION_RESP);
            filter.addCategory(Intent.CATEGORY_DEFAULT);
            receiver = new UserStateResponseReceiver();
            registerReceiver(receiver, filter);
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        try {
            unregisterReceiver(receiver);
            super.onStop();
        } catch (Exception e) {
            super.onStop();
        }
    }

    /**
     * Runs when a GoogleApiClient object successfully connects.
     *
     * https://github.com/googlesamples/android-play-location/blob/master/LocationUpdates/app/src/main/java/com/google/android/gms/location/sample/locationupdates/MainActivity.java (modified compared to original)
     *
     */
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (mRequestingLocationUpdates)
                askPermission();
        } else {
            Location loc = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (loc == null) {
                if (mRequestingLocationUpdates) {
                    startLocationUpdates();
                }
            } else {
                ArrayList<Errand> userAcceptedErrands = ErrandStateService.getUserAcceptedErrands();
                location = loc;
                for (Errand e : userAcceptedErrands) {
                    e.setDistanceFromUser(SearchResultsFragment.calculateDistanceFromUser(e, location.getLatitude(), location.getLongitude()));
                }

                ErrandStateService.acceptedAdapter.updateArrayList(userAcceptedErrands);
            }
        }
    }

    /**
     * Method that is called after the GoogleApiClient connection has been suspended.
     */
    @Override
    public void onConnectionSuspended(int i) {

    }

    /**
     * A method that is called when the GoogleApiCilent connection failed.
     *
     * https://github.com/googlesamples/android-play-location/blob/master/LocationUpdates/app/src/main/java/com/google/android/gms/location/sample/locationupdates/MainActivity.java (modified compared to original)
     *
     * @param connectionResult a connection result object
     */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
            }
        } else {

        }
    }

    /**
     * Callback that fires when the location changes.
     *
     * https://github.com/googlesamples/android-play-location/blob/master/LocationUpdates/app/src/main/java/com/google/android/gms/location/sample/locationupdates/MainActivity.java
     */
    @Override
    public void onLocationChanged(Location location) {
        this.location = location;
        try {
            ArrayList<Errand> userAcceptedErrands = ErrandStateService.getUserAcceptedErrands();
            for (Errand e : userAcceptedErrands) {
                e.setDistanceFromUser(SearchResultsFragment.calculateDistanceFromUser(e, location.getLatitude(), location.getLongitude()));
            }

            ErrandStateService.acceptedAdapter.updateArrayList(userAcceptedErrands);
        } catch (Exception e) {
        }
    }

    /**
     * A method to get the user's current location.
     */
    public static Location getLocation() {
        return location;
    }

    /**
     * A method to check if it is possible to get the user's location with network or GPS.
     *
     * @param locationManager a class that makes it possible to check the availability of location
     * @return                true if location is available false otherwise
     */
    public static boolean checkLocationProviders(LocationManager locationManager) {
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (isGPSEnabled || isNetworkEnabled)
            return true;
        return false;
    }

    /**
     * A method that shows an alert if the user doesn't have proper Location settings.
     */
    public static void showAlert(final Activity activity, int resource) {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
        dialogBuilder.setTitle(R.string.enable_location);
        if (resource == -1)
            dialogBuilder.setMessage(R.string.location_off);
        else
            dialogBuilder.setMessage(resource);
        dialogBuilder.setPositiveButton(R.string.location_settings, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                dialog = null;
                activity.startActivity(myIntent);
            }
        })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int paramInt) {
                        dialog = null;
                        dialogInterface.dismiss();
                    }
                });
        dialog = dialogBuilder.show();
    }


    /**
     * Stores activity data in the Bundle.
     *
     * https://github.com/googlesamples/android-play-location/blob/master/LocationUpdates/app/src/main/java/com/google/android/gms/location/sample/locationupdates/MainActivity.java (modified compared to original)
     *
     */
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(KEY_REQUESTING_LOCATION_UPDATES, mRequestingLocationUpdates);
        savedInstanceState.putParcelable(KEY_LOCATION, location);
        super.onSaveInstanceState(savedInstanceState);
    }


    /**
     * This class is used for receiving the initial data from ErrandStateService and renewing user information if necessary
     */
    public class UserStateResponseReceiver extends BroadcastReceiver {
        public static final String ACTION_RESP =
                "MESSAGE_PROCESSED";

        @Override
        public void onReceive(Context context, Intent intent) {
            final SharedPreferences sharedPreferences = MainActivity.this.getSharedPreferences(
                    "ee.jooseptenn.errandme", Context.MODE_PRIVATE);
            boolean loggedInWithGoogle = sharedPreferences.getBoolean(getString(R.string.google_log_in), false);
            user = intent.getParcelableExtra("user");
            if (user.getSharedInformation().equals("None")) { // user has not chosen shared information
                Bundle b = new Bundle();
                b.putBoolean("choice", true);
                if (loggedInWithGoogle && user.getPhoneNumber().equals("")) // user logged in with Google and has not entered a phone number
                    b.putBoolean("phone",true);
                else
                    b.putBoolean("phone",false);
                showPersonalDialog(b);
            }
            else if ((user.getSharedInformation().equals("phone") || user.getSharedInformation().equals("both")) && user.getPhoneNumber().equals("")) { // user is sharing his phone number, but has not entered a phone number
                Bundle b = new Bundle();
                b.putBoolean("choice", false);
                if (loggedInWithGoogle) {
                    b.putBoolean("phone",true);
                }
                showPersonalDialog(b);
            }
        }
    }

    // A dialog that encourages the user to enter a phone number and/or choose personal information to share
    private void showPersonalDialog(Bundle b) {
        FragmentActivity activity = (FragmentActivity) (mActivity);
        final android.support.v4.app.FragmentManager fm = activity.getSupportFragmentManager();
        PersonalInfoDialogFragment personalFragment = new PersonalInfoDialogFragment();
        personalFragment.setArguments(b);
        personalFragment.show(fm, "personalInfoDialog");
    }

    // https://developer.android.com/training/implementing-navigation/nav-drawer.html
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    /**
     * https://developer.android.com/training/permissions/requesting.html (modified compared to original)
     * A method for asking the user for permission to use Location in the application.
     */
    public void askPermission() {
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_LOCATION);
    }

    /**
     * https://developer.android.com/training/permissions/requesting.html (modified compared to original)
     * A method to handle the result of a permission request (Location permission)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_DENIED){
                    mRequestingLocationUpdates = false;
                    if (mActivity == null) {
                        return;
                    }
                    else if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity, Manifest.permission.ACCESS_FINE_LOCATION)) {
                        showRationale();
                    }
                    else {
                        Toast.makeText(mActivity, R.string.no_permission, Toast.LENGTH_LONG).show();
                    }
                }
                return;
            }
        }
    }

    /**
     * A method that displays a dialog to the user when the user has denied the location permission.
     */
    public void showRationale() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(mActivity);
        dialog.setMessage(R.string.deny_location);
        dialog.setPositiveButton(R.string.close_dialog, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dialog.create().show();
    }

    //https://developer.android.com/training/implementing-navigation/nav-drawer.html
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    /**
     * A method for loading a fragment, navigating in the application.
     *
     * @param position the position of the item that is selected from the navigation drawer
     */
    public void selectItem(int position) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        switch (position) {
            case 0:
                Fragment errandFragment = new MyErrandsFragment();
                fragmentManager.beginTransaction()
                        .replace(R.id.content_frame, errandFragment)
                        .addToBackStack(getString(R.string.my_errands))
                        .commit();
                break;
            case 1:
                Fragment addFragment = new AddFragment();
                fragmentManager.beginTransaction()
                        .replace(R.id.content_frame, addFragment)
                        .addToBackStack(getString(R.string.add_errands))
                        .commit();
                break;
            case 2:
                Fragment searchFragment = new SearchFragment();
                fragmentManager.beginTransaction()
                        .replace(R.id.content_frame, searchFragment)
                        .addToBackStack(getString(R.string.search_errands))
                        .commit();
                break;
            case 3:
                Fragment settingsFragment = new SettingsFragment();
                fragmentManager.beginTransaction()
                        .replace(R.id.content_frame, settingsFragment)
                        .addToBackStack(getString(R.string.settings))
                        .commit();
                break;
            default:

                final SharedPreferences sharedPreferences = this.getSharedPreferences(
                        "ee.jooseptenn.errandme", Context.MODE_PRIVATE);
                boolean loggedInWithGoogle = sharedPreferences.getBoolean(getString(R.string.google_log_in), false);
                if (loggedInWithGoogle) {
                    // http://stackoverflow.com/questions/38039320/googleapiclient-is-not-connected-yet-on-logout-when-using-firebase-auth-with-g (modified compared to original)
                    LoginActivity.mGoogleApiClient.connect();
                    LoginActivity.mGoogleApiClient.registerConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                        @Override
                        public void onConnected(@Nullable Bundle bundle) {

                            firebaseAuth.signOut(); //End user session
                            if (LoginActivity.mGoogleApiClient.isConnected()) {
                                Auth.GoogleSignInApi.signOut(LoginActivity.mGoogleApiClient).setResultCallback(new ResultCallback<Status>() {
                                    @Override
                                    public void onResult(@NonNull Status status) {
                                        if (status.isSuccess()) {
                                            clearErrandStateService();
                                            SharedPreferences.Editor editor = sharedPreferences.edit();
                                            editor.putBoolean(getString(R.string.google_log_in), false);
                                            editor.apply();
                                            startActivity(new Intent(MainActivity.this, LoginActivity.class));
                                            finish();
                                        }
                                    }
                                });
                            }
                        }

                        @Override
                        public void onConnectionSuspended(int i) {

                        }
                    });
                }
                else {
                    firebaseAuth.signOut(); //End user session
                    clearErrandStateService();
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    finish();
                }
        }

        mDrawerList.setItemChecked(position, true);
        setTitle(mNavigationItems[position]);
        mDrawerLayout.closeDrawer(mDrawerList);
    }

    /**
     * A method to prepare the ErrandStateService to be stopped.
     */
    private void clearErrandStateService() {
        mRef.child("userData").child(uid).removeEventListener(ErrandStateService.userListener);
        mRef.child("errands").removeEventListener(ErrandStateService.errandListener);
        for (Map.Entry<String, ChildEventListener> entry : ErrandStateService.errandToListenerMap.entrySet()) {
            String errandId = entry.getKey();
            ChildEventListener childListener = entry.getValue();
            mRef.child("errands").child(errandId).removeEventListener(childListener);
        }
        ErrandStateService.userListener = null;
        ErrandStateService.errandListener = null;
        ErrandStateService.acceptedAdapter = null;
        ErrandStateService.addedAdapter = null;
        ErrandStateService.updatedUserAddedErrandIds = null;
        ErrandStateService.updatedUserAcceptedErrandIds = null;
        ErrandStateService.errandToListenerMap.clear();
        ErrandStateService.user = null;
        ErrandStateService.clearErrands();
        ErrandStateService.clearAddedErrands();
        ErrandStateService.clearAcceptedErrands();
        ErrandStateService.clearVariables();
    }

    // https://developer.android.com/training/implementing-navigation/nav-drawer.html
    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getSupportActionBar().setTitle(mTitle);
    }

    // https://developer.android.com/training/implementing-navigation/nav-drawer.html (modified compared to original)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A method to display the user added errands on login.
     */
    public void displayErrands() {
        if (savedInstanceState == null) {
            selectItem(0);
        }
    }

    /**
     * A method that is used to determine how the application reacts when the device's back button is pressed.
     */
    @Override
    public void onBackPressed() { // http://stackoverflow.com/questions/5448653/how-to-implement-onbackpressed-in-fragments (modified compared to original)

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.content_frame);
        if (fragment instanceof MyErrandsFragment) {
            finish();
        }

        int count = getSupportFragmentManager().getBackStackEntryCount();

        if (count == 0) {
            super.onBackPressed();
            finish();
        } else {
            try {
                getSupportFragmentManager().popBackStack();
            }
            catch (Exception e) {
            }
        }

    }

}