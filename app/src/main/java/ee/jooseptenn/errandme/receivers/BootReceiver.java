package ee.jooseptenn.errandme.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.google.firebase.auth.FirebaseAuth;

/**
 * A receiver that activates when the device is booted.
 */

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            FirebaseAuth.getInstance().signOut(); // Log out
        } catch (Exception e) {}
    }
}