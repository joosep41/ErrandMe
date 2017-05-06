package ee.jooseptenn.errandme.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import ee.jooseptenn.errandme.R;
import ee.jooseptenn.errandme.activities.LoginActivity;
import ee.jooseptenn.errandme.baseclasses.User;
import ee.jooseptenn.errandme.services.ErrandStateService;

/**
 * A fragment that contains the settings which enable the user to change shared information and contact information.
 */

public class SettingsFragment extends PreferenceFragmentCompat {

    private static FirebaseDatabase database;
    private static DatabaseReference myRef;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set up database reference
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference();

        final ListPreference changeSharedDataPreference = (ListPreference) findPreference(getString(R.string.privacy));
        Preference changeDetailsPreference = (Preference) findPreference(getString(R.string.change_personal_information));

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        final String sharedDataPref = sharedPref.getString(getString(R.string.privacy), "");

        final SharedPreferences sharedPreferences = getActivity().getSharedPreferences(
                "ee.jooseptenn.errandme", Context.MODE_PRIVATE);

        final String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        ValueEventListener listener = new ValueEventListener() { // Enable correct radio button
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                User u = dataSnapshot.getValue(User.class);

                int index = 0;
                String sharedInfo = u.getSharedInformation();
                if (sharedInfo.equals("phone"))
                    index = 0;
                else if (sharedInfo.equals("email"))
                    index = 1;
                else if (sharedInfo.equals("both"))
                    index = 2;
                else if (sharedInfo.equals("None")) {
                    index = -1;
                }
                if (index != -1) {
                    changeSharedDataPreference.setValueIndex(index);
                }
                else {
                    changeSharedDataPreference.setValue(null);
                }

                myRef.child("userData").child(uid).removeEventListener(this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
             }
        };
        myRef.child("userData").child(uid).addListenerForSingleValueEvent(listener);



        // User chooses what information to share
        changeSharedDataPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (LoginActivity.isNetworkAvailable(getContext())) {
                    User user = ErrandStateService.user;
                    String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    String newValueString = String.valueOf(newValue);

                    if (newValueString.equals("0"))
                        user.setSharedInformation("phone");
                    else if (newValueString.equals("1"))
                        user.setSharedInformation("email");
                    else
                        user.setSharedInformation("both");

                    myRef.child("userData").child(uid).setValue(user);

                    return true;
                }
                else
                    Toast.makeText(getActivity(), R.string.no_internet, Toast.LENGTH_SHORT).show();
                return false;

            }
        });

        // User changes email or phone number
        changeDetailsPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (LoginActivity.isNetworkAvailable(getContext())) {
                    FragmentActivity activity = (FragmentActivity) (getActivity());
                    final android.support.v4.app.FragmentManager fm = activity.getSupportFragmentManager();
                    ChangeInformationDialogFragment changeInformationDialogFragment = new ChangeInformationDialogFragment();
                    changeInformationDialogFragment.show(fm, "changeDetailsDialog");

                    return true;
                } else
                    Toast.makeText(getActivity(), R.string.no_internet, Toast.LENGTH_SHORT).show();
                return false;
            }
        });

    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        setPreferencesFromResource(R.xml.preferences, s);
    }


}
