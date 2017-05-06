package ee.jooseptenn.errandme.adapters;

import android.content.Context;
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
import java.util.ArrayList;
import ee.jooseptenn.errandme.fragments.DetailsDialogFragment;
import ee.jooseptenn.errandme.baseclasses.Errand;
import ee.jooseptenn.errandme.fragments.LocationDialogFragment;
import ee.jooseptenn.errandme.activities.LoginActivity;
import ee.jooseptenn.errandme.R;
import ee.jooseptenn.errandme.fragments.SearchResultsFragment;
import ee.jooseptenn.errandme.baseclasses.User;
import ee.jooseptenn.errandme.services.ErrandStateService;

/**
 * Adapter for displaying search results to the user.
 */

public class SearchResultsListAdapter extends BaseAdapter {

    private static Context mContext;
    private static ArrayList<Errand> errands;
    public static SearchResultsListAdapter adapter;

    private FirebaseDatabase database;
    private DatabaseReference myRef;

    /**
     * A constructor for the SearchResultsListAdapter.
     *
     * @param context context context of the caller, necessary for sending intents, accessing different methods
     * @param errands the errands that will be in the adapter
     */
    public SearchResultsListAdapter(Context context, ArrayList<Errand> errands) {
        this.mContext = context;
        this.errands = errands;
        adapter = this;
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference();
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
            view = inflater.inflate(R.layout.search_result_item_layout, null);
        }

        // getting layout elements
        TextView titleView = (TextView) view.findViewById(R.id.errand_title);
        TextView distanceView = (TextView) view.findViewById(R.id.errand_distance);
        TextView payView = (TextView) view.findViewById(R.id.errand_pay);
        TextView timeView = (TextView) view.findViewById(R.id.errand_time);

        titleView.setText(errands.get(position).getTitle());
        distanceView.setText(String.format( "%.2f",errands.get(position).getDistanceFromUser()) + " km");
        payView.setText(errands.get(position).getPay() + " " + errands.get(position).getCurrency());

        String time = errands.get(position).getEstimatedTime();
        if (time.equals("none")) {
            timeView.setVisibility(View.GONE);
        }
        else {
            timeView.setVisibility(View.VISIBLE);
            timeView.setText(time + " min");
        }

        Button runButton = (Button) view.findViewById(R.id.run_button);
        Button detailsButton = (Button) view.findViewById(R.id.details_button);
        Button locationButton = (Button) view.findViewById(R.id.location_button);

        // Handling run button click
        runButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (LoginActivity.isNetworkAvailable(mContext)) {
                    final String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    updateAccepter(errands.get(position), uid);
                }
                else
                    Toast.makeText(mContext, R.string.no_internet, Toast.LENGTH_SHORT).show();
            }
        });

        // Handling details button click
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

        // Handling location button click
        locationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, LocationDialogFragment.class);
                intent.putExtra("location", errands.get(position).getLocation());
                mContext.startActivity(intent);
            }
        });
        return view;
    }

    /**
     * A method for updating an errand's and the errand accepter's information in the database when an errand has been accepted.
     *
     * @param errand the errand that the user accepted
     * @param uid    the accepter's uid
     */
    private void updateAccepter(final Errand errand, final String uid) {
        myRef.child("errands").child(errand.getId()).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                Errand e = mutableData.getValue(Errand.class);
                if (e == null) {
                    return Transaction.abort();
                }

                if (e.getAccepterId().equals("")) {

                    e.setAccepterId(uid);
                    mutableData.setValue(e);
                    return Transaction.success(mutableData);
                }
                else
                    return Transaction.abort();

            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                    if (b) {
                        final Errand e = dataSnapshot.getValue(Errand.class);
                        User user = ErrandStateService.user;

                        user.addAcceptedErrands(e.getId());
                        myRef.child("userData").child(uid).setValue(user);

                        errands.remove(errand);
                        notifyDataSetChanged();
                    }
            }
        });
    }

    /**
     * A method for removing an errand from the adapter.
     *
     * @param errand an errand to remove from the adapter
     */
    public static void removeErrand(Errand errand) {
        try {
            ArrayList<Errand> errandsNew = new ArrayList<>(errands);
            for (Errand e : errandsNew) {
                if (e.getId().equals(errand.getId())) {
                    errandsNew.remove(e);
                    break;
                }
            }
            errands = errandsNew;
            adapter.notifyDataSetChanged();
        } catch (NullPointerException e) {}
    }

    /**
     * A method for editing an errand in the adapter.
     *
     * @param errand an edited errand that is used to edit the errand with the same id in the adapter
     */
    public static void editErrand(Errand errand) {
        try {
            ArrayList<Errand> errandsNew = new ArrayList<>(errands);
            boolean remove = false;
            for (Errand e : errandsNew) {
                if (e.getId().equals(errand.getId())) {
                    String title = errand.getTitle();
                    String pay = errand.getPay();
                    String time = errand.getEstimatedTime();
                    double distanceFromUser = SearchResultsFragment.filterByRadius(mContext, true, errand);
                    boolean validTitle = SearchResultsFragment.checkTitle(title);
                    boolean goodPay = SearchResultsFragment.goodPay(pay);
                    boolean goodTime = SearchResultsFragment.goodTime(time);
                    if (validTitle && goodPay && goodTime && distanceFromUser > -1) {
                        e.setTitle(title);
                        e.setDescription(errand.getDescription());
                        e.setPay(pay);
                        e.setCurrency(errand.getCurrency());
                        e.setLocation(errand.getLocation());
                        e.setAddress(errand.getAddress());
                        e.setDistanceFromUser(distanceFromUser);
                        e.setEstimatedTime(errand.getEstimatedTime());
                        break;
                    }
                    else {
                        remove = true;
                        break;
                    }
                }
            }
            if (remove) {
                ArrayList<Errand> errandsNewClone = new ArrayList<>(errandsNew);
                for (Errand e: errandsNewClone) {
                    if (e.getId().equals(errand.getId())) {
                        errandsNew.remove(e);
                        break;
                    }
                }
            }
            errands = errandsNew;
            adapter.notifyDataSetChanged();
        } catch (NullPointerException e) {}
    }
}
