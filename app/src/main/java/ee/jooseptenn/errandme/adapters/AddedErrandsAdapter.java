package ee.jooseptenn.errandme.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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
import ee.jooseptenn.errandme.fragments.EditDialogFragment;
import ee.jooseptenn.errandme.baseclasses.Errand;
import ee.jooseptenn.errandme.activities.LoginActivity;
import ee.jooseptenn.errandme.R;
import ee.jooseptenn.errandme.baseclasses.User;

/**
 * This adapter is responsible for displaying the added errands of a logged in user
 */

public class AddedErrandsAdapter extends BaseAdapter {

    private Context mContext;
    private ArrayList<Errand> errands;
    FirebaseDatabase database;
    DatabaseReference myRef;
    private String uid;

    /**
     * A constructor for the AddedErrandsAdapter.
     *
     * @param context context of the caller, necessary for sending intents, accessing different methods
     * @param errands the errands that will be in the adapter
     */
    public AddedErrandsAdapter(Context context, ArrayList<Errand> errands) {
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
            view = inflater.inflate(R.layout.added_errand_layout, null);
        }

        // getting layout elements
        TextView titleView = (TextView) view.findViewById(R.id.errand_title);
        final TextView stateView = (TextView) view.findViewById(R.id.errand_state);
        final TextView accepterView = (TextView) view.findViewById(R.id.accepterNameText);
        final Button detailsButton = (Button) view.findViewById(R.id.details_button);
        Button editButton = (Button) view.findViewById(R.id.edit_button);
        Button removeButton = (Button) view.findViewById(R.id.remove_button);
        final Button accepterProfileButton = (Button) view.findViewById(R.id.accepter_button);

        // Hide and show certain layout elements when errand has been accepted
        if (!errands.get(position).getAccepterId().equals("")) {
            editButton.setVisibility(View.INVISIBLE);
            accepterProfileButton.setVisibility(View.VISIBLE);
            accepterView.setVisibility(View.VISIBLE);
        }

        // Hide and show certain layout elements when errand is not accepted
        else if (errands.get(position).getAccepterId().equals("")) {
            editButton.setVisibility(View.VISIBLE);
            accepterProfileButton.setVisibility(View.GONE);
            accepterView.setVisibility(View.GONE);
        }

        titleView.setText(errands.get(position).getTitle());

        // Getting errand accepter information and changing layout appropriately
        if (!errands.get(position).getAccepterId().equals("")) {
            ValueEventListener listener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    User user = dataSnapshot.getValue(User.class);
                    stateView.setText(mContext.getString(R.string.accepted));
                    accepterView.setText(user.getName());

                    Drawable dr = mContext.getResources().getDrawable(R.drawable.ic_accepted);
                    Bitmap bitmap = ((BitmapDrawable) dr).getBitmap();

                    Drawable d = new BitmapDrawable(mContext.getResources(), Bitmap.createScaledBitmap(bitmap, 55, 55, true)); // http://stackoverflow.com/questions/7021578/resize-drawable-in-android (modified compared to original)
                    stateView.setCompoundDrawablesWithIntrinsicBounds(d, null, null, null);
                    stateView.setCompoundDrawablePadding(5);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            };
            myRef.child("userData").child(errands.get(position).getAccepterId()).addListenerForSingleValueEvent(listener);
        }

        else { // Style layout according to an errand that has not been accepted
            stateView.setText(R.string.not_accepted);
            Drawable dr = mContext.getResources().getDrawable(R.drawable.ic_not_accepted);
            Bitmap bitmap = ((BitmapDrawable) dr).getBitmap();
            Drawable d = new BitmapDrawable(mContext.getResources(), Bitmap.createScaledBitmap(bitmap, 55, 55, true)); // http://stackoverflow.com/questions/7021578/resize-drawable-in-android (modified compared to original)
            stateView.setCompoundDrawablesWithIntrinsicBounds(d, null, null, null);
            stateView.setCompoundDrawablePadding(5);
        }

        // Handle details button click
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

        // Handle edit button click
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentActivity activity = (FragmentActivity) (mContext);
                final android.support.v4.app.FragmentManager fm = activity.getSupportFragmentManager();
                EditDialogFragment editDialogFragment = new EditDialogFragment();
                Bundle b = new Bundle();
                b.putString("title", errands.get(position).getTitle());
                b.putString("description", errands.get(position).getDescription());
                b.putString("pay", errands.get(position).getPay());
                b.putString("currency", errands.get(position).getCurrency());
                b.putString("location", errands.get(position).getAddress());
                b.putString("errandId", errands.get(position).getId());
                b.putString("time", errands.get(position).getEstimatedTime());
                editDialogFragment.setArguments(b);
                editDialogFragment.show(fm, "errandEditingDialog");
            }
        });

        // Handle remove button click
        removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
                dialog.setMessage(R.string.remove_errand);
                dialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (LoginActivity.isNetworkAvailable(mContext)) {
                            final Errand removableErrand = errands.get(position);
                            removeErrand(removableErrand);
                        }
                        else {
                            EditDialogFragment.displayNoInternetDialog(mContext);
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

        // Handle contact button click
        accepterProfileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (LoginActivity.isNetworkAvailable(mContext)) {
                    FragmentActivity activity = (FragmentActivity) (mContext);
                    final android.support.v4.app.FragmentManager fm = activity.getSupportFragmentManager();
                    final ContactInfoFragment accepterInfoFragment = new ContactInfoFragment();
                    ValueEventListener listener = new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            User accepter = dataSnapshot.getValue(User.class);
                            Bundle b = new Bundle();
                            b.putString("name", accepter.getName());

                            String sharedInfo = accepter.getSharedInformation();
                            if (sharedInfo.equals("phone") || sharedInfo.equals("both"))
                                b.putString("phonenumber", accepter.getPhoneNumber());
                            else
                                b.putString("phonenumber", "-");
                            if (sharedInfo.equals("email") || sharedInfo.equals("both"))
                                b.putString("email", accepter.getEmail());
                            else
                                b.putString("email", "-");
                            accepterInfoFragment.setArguments(b);
                            accepterInfoFragment.show(fm, "detailsViewingDialog");
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    };
                    myRef.child("userData").child(errands.get(position).getAccepterId()).addListenerForSingleValueEvent(listener);
                }
                else
                    Toast.makeText(mContext, R.string.no_internet, Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    /**
     * A method for removing an errand from the database and updating the accepter's information in the database.
     *
     * @param removableErrand the errand to be removed from the database
     */
    public void removeErrand(final Errand removableErrand) {
        myRef.child("errands").child(removableErrand.getId()).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                Errand errand = mutableData.getValue(Errand.class);
                final String accepterId = errand.getAccepterId();
                if (!accepterId.equals("")) {
                    final ValueEventListener listener = new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {

                            User u = dataSnapshot.getValue(User.class);

                            ArrayList<String> acceptedErrands = u.getActiveAcceptedErrands();

                            for (String e : acceptedErrands) {
                                if (removableErrand.getId().equals(e)) {
                                    u.getActiveAcceptedErrands().remove(e);
                                    break;
                                }
                            }

                            myRef.child("userData").child(accepterId).setValue(u);
                            myRef.child("userData").child(accepterId).removeEventListener(this);
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    };
                    myRef.child("userData").child(accepterId).addListenerForSingleValueEvent(listener);

                }
                mutableData.setValue(null);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

            }
        });
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

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        ArrayList<String> errandIds = new ArrayList<>();
        for (Errand e: errands) {
            errandIds.add(e.getId());
        }
        myRef.child("userData").child(uid).child("activeAddedErrands").setValue(errandIds);
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
     * A method for getting all the errands that are in the adapter.
     *
     * @return the errands in the adapter
     */
    public ArrayList<Errand> getErrands() {
        return errands;
    }

}
