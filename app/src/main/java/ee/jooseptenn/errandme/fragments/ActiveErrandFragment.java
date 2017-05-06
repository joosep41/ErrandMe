package ee.jooseptenn.errandme.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import ee.jooseptenn.errandme.R;
import ee.jooseptenn.errandme.adapters.AcceptedErrandsAdapter;
import ee.jooseptenn.errandme.adapters.AddedErrandsAdapter;
import ee.jooseptenn.errandme.baseclasses.Errand;
import ee.jooseptenn.errandme.services.ErrandStateService;

/**
 * A fragment that initializes the AddedErrandsAdapter and the AcceptedErrandsAdapter and puts them into a ListView.
 */

public class ActiveErrandFragment extends Fragment {

    private TextView noErrands;
    private ListView listView;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.errands_fragment, container, false);
        Bundle args = getArguments();
        String type = args.getString("adapterType", "");

        noErrands = (TextView) view.findViewById(R.id.no_studies); // get reference to the TextView that is displayed when there are no errands to display
        listView = (ListView) view.findViewById(R.id.listView); // get reference to the ListView
        listView.setEmptyView(noErrands);

        ArrayList<Errand> userAcceptedErrands = ErrandStateService.getUserAcceptedErrands();
        ArrayList<Errand> userAddedErrands = ErrandStateService.getUserAddedErrands();

        // Setting up the ListView
        if (userAcceptedErrands.size() == 0 && userAddedErrands.size() == 0) {
            if (type.equals("addedErrandsAdapter")) {
                ErrandStateService.addedAdapter = new AddedErrandsAdapter(getActivity(), new ArrayList<Errand>());
                listView.setAdapter(ErrandStateService.addedAdapter);
            } else {
                ErrandStateService.acceptedAdapter = new AcceptedErrandsAdapter(getActivity(), new ArrayList<Errand>());
                listView.setAdapter(ErrandStateService.acceptedAdapter);
            }
        }
        else {
            if (type.equals("addedErrandsAdapter")) {
                if (userAddedErrands.size() > 0) {
                    ErrandStateService.addedAdapter = new AddedErrandsAdapter(getActivity(), userAddedErrands);
                    listView.setAdapter(ErrandStateService.addedAdapter);
                }
            } else {
                if (userAcceptedErrands.size() > 0) {
                    ErrandStateService.acceptedAdapter = new AcceptedErrandsAdapter(getActivity(), userAcceptedErrands);
                    listView.setAdapter(ErrandStateService.acceptedAdapter);
                }
            }
        }

        return view;
    }
}