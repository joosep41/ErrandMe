package ee.jooseptenn.errandme.services;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import ee.jooseptenn.errandme.R;
import ee.jooseptenn.errandme.activities.MainActivity;
import ee.jooseptenn.errandme.adapters.AcceptedErrandsAdapter;
import ee.jooseptenn.errandme.adapters.AddedErrandsAdapter;
import ee.jooseptenn.errandme.adapters.SearchResultsListAdapter;
import ee.jooseptenn.errandme.baseclasses.Errand;
import ee.jooseptenn.errandme.baseclasses.User;
import ee.jooseptenn.errandme.fragments.SearchResultsFragment;
import ee.jooseptenn.errandme.other.FirebaseMultiQuery;

/**
 * A service that updates the user information in the application with new data from the database.
 * It also sends notifications to a user when the user's errand states change (accepted, removed, declined)
 */

public class ErrandStateService extends IntentService {

    private static FirebaseDatabase mDb;
    private static DatabaseReference mRef;
    private static FirebaseAuth firebaseAuth;
    private static int notificationId = 0;

    public static ValueEventListener userListener;
    public static ChildEventListener errandListener;

    private static ArrayList<Errand> userAcceptedErrands = new ArrayList<>();;
    private static ArrayList<Errand> userAddedErrands = new ArrayList<>();

    public static AcceptedErrandsAdapter acceptedAdapter;
    public static AddedErrandsAdapter addedAdapter;

    public static ArrayList<String> updatedUserAddedErrandIds;
    public static ArrayList<String> updatedUserAcceptedErrandIds;

    public static User user;
    private static ArrayList<Errand> errands = new ArrayList<>();

    private ErrandStateService errandStateService;
    private static Context mContext;

    public static HashMap<String, ChildEventListener> errandToListenerMap = new HashMap<>();

    /**
     * A constructor for the ErrandStateService class.
     */
    public ErrandStateService() {
        super(ErrandStateService.class.getName());
    }


    @Override
    protected void onHandleIntent(Intent intent) {

        if (mContext == null)
            mContext = ErrandStateService.this;

        if (errandStateService == null)
            errandStateService = this;

        if (acceptedAdapter == null && addedAdapter == null) { // Initialize added errands adapter and accepted errands adapter
            acceptedAdapter = new AcceptedErrandsAdapter(getApplicationContext(), userAcceptedErrands);
            addedAdapter = new AddedErrandsAdapter(getApplicationContext(), userAddedErrands);
        }

        // Set up Firebase information (Authentication information and database reference)
        if (mDb == null && mRef == null && firebaseAuth == null) {
            firebaseAuth = FirebaseAuth.getInstance();
            mDb = FirebaseDatabase.getInstance();
            mRef = mDb.getReference();
        }

        final String uid = firebaseAuth.getCurrentUser().getUid(); // Get current user's uid

        if (userListener == null) {
            userListener = new ValueEventListener() { // Listener that receives user data (initial data and updates)
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    final User updatedUser = dataSnapshot.getValue(User.class);
                    user = updatedUser;

                    userAcceptedErrands = new ArrayList<>();
                    userAddedErrands = new ArrayList<>();

                    updatedUserAddedErrandIds = updatedUser.getActiveAddedErrands();
                    updatedUserAcceptedErrandIds = updatedUser.getActiveAcceptedErrands();


                    ArrayList<DatabaseReference> errandRefs = new ArrayList<>();

                    for (String s : updatedUserAddedErrandIds) {
                        errandRefs.add(mRef.child("errands").child(s));
                    }

                    for (String s : updatedUserAcceptedErrandIds) {
                        errandRefs.add(mRef.child("errands").child(s));
                    }

                    // http://stackoverflow.com/questions/38173569/only-load-layout-when-firebase-calls-are-complete (modified compared to original)
                    FirebaseMultiQuery firebaseMultiQuery = new FirebaseMultiQuery(errandRefs);
                    final Task<Map<DatabaseReference, DataSnapshot>> allLoad = firebaseMultiQuery.start();
                    allLoad.addOnCompleteListener(new AllOnCompleteListener(errandRefs, firebaseMultiQuery));

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            };
            mRef.child("userData").child(uid).addValueEventListener(userListener);
        }

        // Listening for changes in the errands node in the database (and receiving inital data)
        if (errandListener == null) {
            errandListener = new ChildEventListener() {


                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) { // new errand added
                    Errand errand = dataSnapshot.getValue(Errand.class);
                    errands.add(errand);
                    SearchResultsFragment.fillErrandsList(errandStateService);
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) { // errand changed
                    Errand errand = dataSnapshot.getValue(Errand.class);
                    for (Errand e : errands) {
                        if (e.getId().equals(errand.getId())) {
                            errands.remove(e);
                            errands.add(errand);
                            break;
                        }
                    }
                    ArrayList<Errand> addedErrands = addedAdapter.getErrands();
                    for (int i = 0; i < addedErrands.size(); i++) {
                        if (addedErrands.get(i).getId().equals(errand.getId())) {
                            addedErrands.remove(i);
                            addedErrands.add(i, errand);
                            addedAdapter.updateArrayList(addedErrands);
                            break;
                        }
                    }
                    SearchResultsFragment.fillErrandsList(errandStateService);
                    if (!errand.getAccepterId().equals("")) {
                        if (!errand.getAccepterId().equals(uid)) {
                            SearchResultsListAdapter.removeErrand(errand);
                        }
                    }
                    SearchResultsListAdapter.editErrand(errand);
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) { // errand removed
                    Errand errand = dataSnapshot.getValue(Errand.class);
                    for (Errand e : new ArrayList<>(errands)) {
                        if (e.getId().equals(errand.getId())) {
                            errands.remove(e);
                            break;
                        }
                    }
                    SearchResultsFragment.fillErrandsList(errandStateService);
                    SearchResultsListAdapter.removeErrand(errand);
                    addedAdapter.removeItem(errand);

                }

                @Override

                public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            };
            mRef.child("errands").addChildEventListener(errandListener);
        }
    }

    // http://stackoverflow.com/questions/38173569/only-load-layout-when-firebase-calls-are-complete (modified compared to original)
    // A class that gets all of the data from the listeners after they have all returned the requested information
    private class AllOnCompleteListener implements OnCompleteListener<Map<DatabaseReference, DataSnapshot>> {

        ArrayList<DatabaseReference> refs;
        FirebaseMultiQuery fmq;

        public AllOnCompleteListener(ArrayList<DatabaseReference> refs, FirebaseMultiQuery fmq) {
            this.refs = refs;
            this.fmq = fmq;
        }

        // The data is ready to be retrieved
        @Override
        public void onComplete(@NonNull Task<Map<DatabaseReference, DataSnapshot>> task) {
            if (task.isSuccessful()) {
                final Map<DatabaseReference, DataSnapshot> result = task.getResult();

                // Look up DataSnapshot objects using the same DatabaseReferences you passed into FirebaseMultiQuery
                for (DatabaseReference ref : refs) {
                    final Errand errand = result.get(ref).getValue(Errand.class);

                    if (isUserAddedErrand(errand)) { // Check errand type (user added errand or accepted errand)
                        userAddedErrands.add(errand);
                        ChildEventListener listener = new ChildEventListener() { // A listener that is added to the errand retrieved. Can be used to send notification to the user if errand is accepted or declined.
                            @Override
                            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                            }

                            @Override
                            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                                if (dataSnapshot.getKey().equals("accepterId")) { // The accepterId of the errand changed
                                    final String accepterId = dataSnapshot.getValue(String.class);
                                    ValueEventListener errandNameListener = new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            final Errand e = dataSnapshot.getValue(Errand.class);
                                            if (accepterId.equals("")) {
                                                sendNotification(ErrandStateService.this, e.getTitle(), null, "declined"); // Errand has been declined, notify user
                                            }
                                            else {
                                                ValueEventListener accepterListener = new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(DataSnapshot snap) {
                                                        User accepter = snap.getValue(User.class);
                                                        sendNotification(ErrandStateService.this, e.getTitle(), accepter.getName(), "accepted"); // Errand has been accepted, notify user
                                                        mRef.child("userData").child(accepterId).removeEventListener(this);
                                                    }
                                                    @Override
                                                    public void onCancelled(DatabaseError databaseError) {
                                                    }
                                                };
                                                mRef.child("userData").child(accepterId).addListenerForSingleValueEvent(accepterListener);
                                            }
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {

                                        }
                                    };
                                    mRef.child("errands").child(errand.getId()).addListenerForSingleValueEvent(errandNameListener);
                                }
                            }
                            @Override
                            public void onChildRemoved(DataSnapshot dataSnapshot) { // User removed errand, remove listener
                                mRef.child("errands").child(errand.getId()).removeEventListener(this);
                                errandToListenerMap.remove(errand.getId());
                            }
                            @Override
                            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        };
                        if (errandToListenerMap.get(errand.getId()) == null) {
                            mRef.child("errands").child(errand.getId()).addChildEventListener(listener);
                            errandToListenerMap.put(errand.getId(), listener);
                        }
                    }
                    else {
                        userAcceptedErrands.add(errand); //
                        try {
                            Location location = MainActivity.getLocation();
                            double lat = location.getLatitude();
                            double longit = location.getLongitude();
                            if (lat != 0 && longit != 0)
                                errand.setDistanceFromUser(SearchResultsFragment.calculateDistanceFromUser(errand,location.getLatitude(), location.getLongitude())); // Update user's distance from the errand
                        } catch (Exception e) {
                        }
                        ChildEventListener listener = new ChildEventListener() {
                            @Override
                            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                            }

                            @Override
                            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                            }

                            @Override
                            public void onChildRemoved(DataSnapshot dataSnapshot) {
                                sendNotification(ErrandStateService.this, errand.getTitle(), null, "removed"); // Errand has been removed, notify user and remove listener
                                mRef.child("errands").child(errand.getId()).removeEventListener(this);
                                errandToListenerMap.remove(errand.getId());
                            }

                            @Override
                            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        };
                        if (errandToListenerMap.get(errand.getId()) == null) {
                            mRef.child("errands").child(errand.getId()).addChildEventListener(listener);
                            errandToListenerMap.put(errand.getId(), listener);
                        }
                    }
                }

                addedAdapter.updateArrayList(userAddedErrands); // update the content of the adapters based on the new information
                acceptedAdapter.updateArrayList(userAcceptedErrands);

                Intent broadcastIntent = new Intent(); //http://stackoverflow.com/questions/4823133/send-data-from-service-back-to-my-activity (modified compared to original)
                broadcastIntent.setAction(MainActivity.UserStateResponseReceiver.ACTION_RESP); // Send information to MainActivity
                broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
                broadcastIntent.putExtra("user", user);
                broadcastIntent.putParcelableArrayListExtra("addedErrands", userAddedErrands);
                broadcastIntent.putParcelableArrayListExtra("acceptedErrands", userAcceptedErrands);
                sendBroadcast(broadcastIntent);

                fmq.stop();

            }

        }
    }

    /**
     * A method to check if an errand is a user added errand.
     * @param errand the errand to check
     * @return       true if the errand is the current user's added errand false otherwise
     */
    private boolean isUserAddedErrand(Errand errand) {
        boolean addedErrand = false;
        for (String s: updatedUserAddedErrandIds) {
            if (errand.getId().equals(s)) {
                addedErrand = true;
                break;
            }
        }
        return addedErrand;
    }

    /**
     * A method that sends a notification to the user when the user's errand is accepted or declined and when the user's accepted errand is removed .
     *
     * @param context     a Context, that is used for sending an intent, creating a TaskStackBuilder and accessing the notification service
     * @param errandTitle the errand that's state changed
     * @param name        the name of the person who accepted an errand if an errand was accepted null otherwise
     * @param state       the type of notification to send
     */
    private static void sendNotification(Context context, String errandTitle, String name, String state) { //https://developer.android.com/guide/topics/ui/notifiers/notifications.html (modified compared to original)

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context);
        mBuilder.setSmallIcon(R.drawable.ic_notification);
        mBuilder.setAutoCancel(true);

        if (state.equals("accepted")) {
            mBuilder.setContentTitle(mContext.getString(R.string.errand_accepted))
                    .setContentText(mContext.getString(R.string.errand_space) + " \"" + errandTitle + "\" " + mContext.getString(R.string.accepted_by) + " " + name);
        }
        else if (state.equals("declined")){
            mBuilder.setContentTitle(mContext.getString(R.string.errand_declined))
                    .setContentText(mContext.getString(R.string.errand_space) + " \"" + errandTitle + "\" " + mContext.getString(R.string.been_declined));
        }
        else {
            mBuilder.setContentTitle(mContext.getString(R.string.errand_removed))
                    .setContentText(mContext.getString(R.string.errand_space) + " \"" + errandTitle + "\" " + mContext.getString(R.string.been_removed));
        }

        Intent resultIntent = new Intent(context, MainActivity.class);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);

        stackBuilder.addParentStack(MainActivity.class);

        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(Integer.valueOf(notificationId++), mBuilder.build());
    }

    /**
     * A method to clear the errands list.
     */
    public static void clearErrands() {
        errands.clear();
    }

    /**
     * A method to get all of the user's active added errands.
     * @return
     */
    public static ArrayList<Errand> getUserAddedErrands() {
        return userAddedErrands;
    }

    /**
     * A method to get all of the user's active accepted errands.
     * @return
     */
    public static ArrayList<Errand> getUserAcceptedErrands() {
        return userAcceptedErrands;
    }

    /**
     * A method to clear the list of user's active accepted errands.
     */
    public static void clearAcceptedErrands() {
        userAcceptedErrands.clear();
    }

    /**
     * A method to clear the list of user's active added errands.
     */
    public static void clearAddedErrands() {
        userAddedErrands.clear();
    }

    /**
     * A method to get all of the errands.
     * @return all of the errands in the database
     */
    public ArrayList<Errand> getErrands() {
        return errands;
    }

    /**
     * A method to clear variables
     */
    public static void clearVariables(){
        mRef = null;
        mDb = null;
        firebaseAuth = null;
        mContext = null;
    }
}