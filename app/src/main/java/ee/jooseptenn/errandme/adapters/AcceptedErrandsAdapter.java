package ee.jooseptenn.errandme.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import ee.jooseptenn.errandme.fragments.ContactInfoFragment;
import ee.jooseptenn.errandme.fragments.DetailsDialogFragment;
import ee.jooseptenn.errandme.baseclasses.Errand;
import ee.jooseptenn.errandme.fragments.EditDialogFragment;
import ee.jooseptenn.errandme.fragments.LocationDialogFragment;
import ee.jooseptenn.errandme.activities.LoginActivity;
import ee.jooseptenn.errandme.R;
import ee.jooseptenn.errandme.baseclasses.User;
import ee.jooseptenn.errandme.services.ErrandStateService;

/**
 * This adapter is responsible for displaying the accepted errands of a logged in user.
 */

public class AcceptedErrandsAdapter extends BaseAdapter {

    private Context mContext;
    private ArrayList<Errand> errands;
    FirebaseDatabase database;
    DatabaseReference myRef;
    private String uid;

    /**
     * A constructor for the AcceptedErrandsAdapter.
     *
     * @param context context of the caller, necessary for sending intents, accessing different methods
     * @param errands the errands that will be in the adapter
     */
    public AcceptedErrandsAdapter(Context context, ArrayList<Errand> errands) {
        this.mContext = context;
        this.errands = errands;
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference();
        if (uid == null)
            uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    /**
     * A method for getting the errand count.
     *
     * @return amount of errands in the adapter
     */
    @Override
    public int getCount() {
        return errands.size();
    }

    /**
     * A method for getting a specific errand from the adapter.
     *
     * @param position the position of the errand in the adapter
     * @return         Object value of the specified errand
     */
    @Override
    public Object getItem(int position) {
        return errands.get(position);
    }

    /**
     * Unused method, necessary when extending BaseAdapter
     */
    @Override
    public long getItemId(int position) {
        return 0;
    }

    /**
     * A method for getting the view for a specific item/position in the data set.
     * In here the xml elements in the specified layout are given values and listeners are attached to buttons.
     *
     * @param position    the position of the item in the adapter
     * @param convertView old view to reuse
     * @param parent      the parent that the view will be attached to
     * @return            the view that will be used
     */
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view = convertView;

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.accepted_errand_layout, null);
        }

        // Getting references to layout elements
        TextView titleView = (TextView) view.findViewById(R.id.errand_title);
        TextView distanceView = (TextView) view.findViewById(R.id.errand_distance);
        TextView payView = (TextView) view.findViewById(R.id.errand_pay);
        TextView timeView = (TextView) view.findViewById(R.id.errand_time);
        Button detailsButton = (Button) view.findViewById(R.id.details_button);
        final Button declineButton = (Button) view.findViewById(R.id.decline_button);
        Button contactButton = (Button) view.findViewById(R.id.contact_button);
        Button locationButton = (Button) view.findViewById(R.id.location_button);

        titleView.setText(errands.get(position).getTitle());
        distanceView.setText(String.format( "%.2f", errands.get(position).getDistanceFromUser()) + " km");
        payView.setText(errands.get(position).getPay() + " " + errands.get(position).getCurrency());

        String estimatedTime = errands.get(position).getEstimatedTime();
        if (estimatedTime.equals("none")) {
            timeView.setVisibility(View.INVISIBLE);
        }
        else {
            timeView.setVisibility(View.VISIBLE);
            timeView.setText(estimatedTime + " min");
        }

        // handle details button press
        detailsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentActivity activity = (FragmentActivity) (mContext);
                final android.support.v4.app.FragmentManager fm = activity.getSupportFragmentManager();
                DetailsDialogFragment detailsDialogFragment = new DetailsDialogFragment();
                Bundle b = new Bundle();
                b.putString("title", errands.get(position).getTitle());
                b.putString("description", errands.get(position).getDescription());
                b.putString("pay", errands.get(position).getPay());
                b.putString("currency", errands.get(position).getCurrency());
                b.putString("location", errands.get(position).getAddress());
                b.putString("time", errands.get(position).getEstimatedTime());
                detailsDialogFragment.setArguments(b);
                detailsDialogFragment.show(fm, "detailsViewingDialog");
            }
        });

        // handle decline button press
        declineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
                dialog.setMessage(R.string.decline_errand);
                dialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            if (LoginActivity.isNetworkAvailable(mContext)) {
                                final Errand declineErrand = errands.get(position);
                                removeItem(declineErrand);
                                removeErrand(declineErrand);
                                removeListener(declineErrand);
                            }
                            else {
                                EditDialogFragment.displayNoInternetDialog(mContext);
                            }
                        } catch (Exception e) {
                            Toast.makeText(mContext, R.string.errand_already_removed, Toast.LENGTH_SHORT);
                        }
                    }
                })
                        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                        });
                dialog.create().show();

            }
        });

        // handle contact button press
        contactButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (LoginActivity.isNetworkAvailable(mContext)) {
                    FragmentActivity activity = (FragmentActivity) (mContext);
                    final android.support.v4.app.FragmentManager fm = activity.getSupportFragmentManager();
                    final ContactInfoFragment accepterInfoFragment = new ContactInfoFragment();
                    ValueEventListener listener = new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            User assigner = dataSnapshot.getValue(User.class);
                            Bundle b = new Bundle();
                            b.putString("name", assigner.getName());

                            String sharedInfo = assigner.getSharedInformation();
                            if (sharedInfo.equals("phone") || sharedInfo.equals("both"))
                                b.putString("phonenumber", assigner.getPhoneNumber());
                            else
                                b.putString("phonenumber", "-");
                            if (sharedInfo.equals("email") || sharedInfo.equals("both"))
                                b.putString("email", assigner.getEmail());
                            else
                                b.putString("email", "-");
                            accepterInfoFragment.setArguments(b);
                            accepterInfoFragment.show(fm, "accepterInfoDialog");
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    };
                    myRef.child("userData").child(errands.get(position).getAssignerId()).addListenerForSingleValueEvent(listener);
                }
                else
                    Toast.makeText(mContext, R.string.no_internet, Toast.LENGTH_SHORT).show();
            }
        });

        // handle location button press
        locationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(mContext, LocationDialogFragment.class);
                    intent.putExtra("location", errands.get(position).getLocation());
                    mContext.startActivity(intent);
                } catch (Exception e) {}

            }
        });


        return view;
    }

    /**
     * A method for updating the data in the adapter.
     *
     * @param updatedErrands ArrayList that will be used to replace the old adapter content
     */
    public void updateArrayList(ArrayList<Errand> updatedErrands) {
        this.errands = updatedErrands;
        this.notifyDataSetChanged();
    }

    /**
     * A method for removing an item from the adapter and updating the user's data in the database.
     *
     * @param errand the Errand that will be removed from the adapter
     */
    public void removeItem(Errand errand) {
        for (Errand e : errands) {
            if (e.getId().equals(errand.getId())) {
                errands.remove(e);
                break;
            }
        }
        notifyDataSetChanged();
        ArrayList<String> errandIds = new ArrayList<>();
        for (Errand e: errands) {
            errandIds.add(e.getId());
        }
        myRef.child("userData").child(uid).child("activeAcceptedErrands").setValue(errandIds);
    }

    /**
     * A method for updating a declined errand's information in the database when an errand has been declined.
     *
     * @param declineErrand the declined errand, this errand's information will be updated
     */
    public void removeErrand(final Errand declineErrand) {
        myRef.child("errands").child(declineErrand.getId()).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {

                Errand e = mutableData.getValue(Errand.class);
                if (e == null) {
                    Transaction.success(mutableData);
                }

                e.setAccepterId("");
                mutableData.setValue(e);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

            }
        });

    }

    /**
     * A method to stop listening for errand updates.
     *
     * @param errand the errand that the listener will be removed from
     */
    public void removeListener(Errand errand) {
        myRef.child("errands").child(errand.getId()).removeEventListener(ErrandStateService.errandToListenerMap.get(errand.getId()));
        ErrandStateService.errandToListenerMap.remove(errand.getId());
    }

    /**
     * A method for getting all the errands that are in the adapter.
     *
     * @return the errands in the adapter
     */
    public ArrayList<Errand> getErrands() {
        return errands;
    }
}
