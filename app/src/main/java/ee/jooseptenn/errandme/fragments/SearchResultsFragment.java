package ee.jooseptenn.errandme.fragments;


import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ee.jooseptenn.errandme.R;
import ee.jooseptenn.errandme.activities.MainActivity;
import ee.jooseptenn.errandme.adapters.SearchResultsListAdapter;
import ee.jooseptenn.errandme.baseclasses.Errand;
import ee.jooseptenn.errandme.services.ErrandStateService;

/**
 * A fragment that filters the search results based on the values entered into the search form.
 */

public class SearchResultsFragment extends Fragment {

    private static ArrayList<Errand> filteredErrands;
    private static ArrayList<Errand> allErrands;

    private TextView emptyListText;
    private TextView progressText;
    private ProgressBar progressBar;

    private static boolean enableKeywords;
    private static boolean matchAllKeywords;
    private static boolean filterPay;
    private static boolean timeGiven;

    private static String searchText;
    private static String payText;
    private static double distance;
    private static String location;
    private static String timeText;

    private ListView listView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            filteredErrands = new ArrayList<Errand>(allErrands);
        } catch (NullPointerException e) {
            filteredErrands = new ArrayList<>();
        }

        Bundle bundle = getArguments();
        enableKeywords = bundle.getBoolean("enableSearchCheckBox");
        matchAllKeywords = bundle.getBoolean("matchAllCheckBox");
        filterPay = bundle.getBoolean("payCheckBox");
        timeGiven = bundle.getBoolean("timeCheckBox");
        searchText = bundle.getString("searchText");
        payText = bundle.getString("payText");
        distance = bundle.getDouble("distance");
        location = bundle.getString("location");
        timeText = bundle.getString("time");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_search_results, container, false);

        emptyListText = (TextView) view.findViewById(R.id.no_results);

        progressBar = (ProgressBar) view.findViewById(R.id.resultsProgressBar);
        progressText = (TextView) view.findViewById(R.id.resultsProgressBarText);
        progressBar.setVisibility(View.VISIBLE);
        progressText.setVisibility(View.VISIBLE);

        listView = (ListView) view.findViewById(R.id.listView);
        listView.setEmptyView(emptyListText);

        if (enableKeywords) { // Filter by errand keywords and phrases in the errand title
            filterByTitleKeywords();
        }
        if (filterPay) { // Filter by errand pay
            String text = payText;
            filterByPay(text);
        }
        if (timeGiven) { // Filter by estimated completion time
            String text = timeText;
            filterByTime(text);
        }
        filterByRadius(getActivity(), false, null); // Filter by radius
        if (ErrandStateService.user != null)
            filterUserAndAcceptedErrands(); // Remove user's errands from search results

        progressBar.setVisibility(View.GONE);
        progressText.setVisibility(View.GONE);

        SearchResultsListAdapter srla = new SearchResultsListAdapter(getActivity(), filteredErrands); // initialize adapter
        listView.setAdapter(srla);

        return view;
    }

    /**
     * A method that filters errands by the text entered into the title EditText of the search form. This method is only called if the user has enabled keyword search.
     * If the user has checked the match all CheckBox then the errands which have a title that do not contain all the keywords are filtered out.
     */
    public void filterByTitleKeywords() {

        try {
            for (Errand errand : allErrands) {
                String errandTitle = errand.getTitle().toLowerCase();

                boolean matches = checkTitle(errandTitle);
                if (!matches && filteredErrands.contains(errand))
                    filteredErrands.remove(errand);
            }
        } catch (Exception e) {
        }
    }

    /**
     * A method that filters errands by pay. Errands which have a pay lower than the entered pay will be filtered out.
     *
     * @param text the pay entered in the search form's pay EditText
     */
    public void filterByPay(String text) {
            ArrayList<Errand> filteredErrandsClone = new ArrayList<Errand>(filteredErrands);

            double pay = Double.valueOf(text.replace(",","."));

            for (Errand errand: filteredErrandsClone) {
                Double errandPay = Double.valueOf(errand.getPay());
                if (errandPay < pay) {
                    filteredErrands.remove(errand);
                }
            }
    }

    /**
     * A method that filters errands by estimated completion time. Errands which have an estimated time that is greater than the entered time will be filtered out.
     *
     * @param text the time entered in the search form's time EditText
     */
    public void filterByTime(String text) {
        ArrayList<Errand> filteredErrandsClone = new ArrayList<Errand>(filteredErrands);

        double time = Double.valueOf(text.replace(",", "."));

        for (Errand errand: filteredErrandsClone) {
            String stringTime = errand.getEstimatedTime();
            if (!stringTime.equals("none")) {
                Double errandTime = Double.valueOf(stringTime);
                if (errandTime > time) {
                    filteredErrands.remove(errand);
                }
            }
            else {
                filteredErrands.remove(errand);
            }
        }
    }

    /**
     * A method that filters errands based on the distance from the search location that the user entered/chose.
     * All the errands that are further from the entered location than the value entered in the search form's distance EditText will be filtered out.
     * The method can be used on all the errands in the filteredErrands ArrayList or on a specific errand.
     *
     * @param context Context that is used by Geocoder.
     * @param edit    true if the method is used on a specific errand false otherwise
     * @param errand  a specific errand, null if used on the filteredErrands ArrayList
     * @return        0 if used on filteredErrands, -1 if used on a specific errand which is not in the specified radius, the distance between the errand and the location entered by the user if the errand is in the radius
     */
    public static double filterByRadius(Context context, boolean edit, Errand errand) {

        Double latitude = 0.0;
        Double longitude = 0.0;

        if (location.equals("current")) {
            Location locat = MainActivity.getLocation();
            if (locat != null) {
                latitude = locat.getLatitude();
                longitude = locat.getLongitude();
            }
        }
        else {
            String addressRaw = "";
            Geocoder geocoder = new Geocoder(context); // http://stackoverflow.com/questions/9409195/how-to-get-complete-address-from-latitude-and-longitude
            List<Address> addresses = null;
            try {
                addresses = geocoder.getFromLocationName(location, 1);
                if (addresses.size() > 0) {
                    Address address = addresses.get(0);
                    latitude = address.getLatitude();
                    longitude = address.getLongitude();
                }
            } catch (IOException e) {
                filteredErrands.clear();
            }
        }

        if (!edit) {
            ArrayList<Errand> filteredErrandsClone = new ArrayList<Errand>(filteredErrands);

            for (Errand e : filteredErrandsClone) {
                double distanceFromUser = calculateDistanceFromUser(e, latitude, longitude);
                e.setDistanceFromUser(distanceFromUser);
                if (distanceFromUser > distance)
                    filteredErrands.remove(e);
            }
            return 0;
        }
        else {
            double distanceFromUser = calculateDistanceFromUser(errand, latitude, longitude);
            if (distanceFromUser > distance) {
                return -1;
            }
            return distanceFromUser;
        }
    }

    /**
     * A method for calculating the distance between an errand and the location entered/chosen by the user.
     *
     * @param e         an errand to check the distance from the location entered by the user
     * @param latitude  latitude of the location entered/chosen by the user
     * @param longitude longitude of the location entered/chosen by the user
     * @return          distance between the user entered location and the errand in kilometres
     */
    public static double calculateDistanceFromUser(Errand e, double latitude, double longitude) {
        // Distance calculation - http://stackoverflow.com/questions/27504947/check-if-a-gps-location-is-within-a-certain-radius-of-another-gps-location-in-an

        String[] locationParts = e.getLocation().split(",");
        Double latitudeErrand = Double.valueOf(locationParts[0]);
        Double longitudeErrand = Double.valueOf(locationParts[1]);
        float[] distanceBetween = new float[1];
        Location.distanceBetween(latitude, longitude, latitudeErrand, longitudeErrand, distanceBetween);

        return distanceBetween[0] / 1000.0;
    }

    /**
     * A method used for filtering out the user added and user accepted errands from the search result.
     */
    public void filterUserAndAcceptedErrands() {
        ArrayList<Errand> filteredErrandsClone = new ArrayList<Errand>(filteredErrands);

        ArrayList<Errand> acceptedErrands = ErrandStateService.acceptedAdapter.getErrands();
        ArrayList<Errand> addedErrands = ErrandStateService.addedAdapter.getErrands();

        for (Errand errand: filteredErrandsClone) {

            for (Errand userErrand: acceptedErrands) {

                if (userErrand.getAssignerId().equals(errand.getAssignerId()) && userErrand.getTitle().equals(errand.getTitle()))
                    filteredErrands.remove(errand);

            }
            for (Errand userErrand: addedErrands) {
                if (userErrand.getAssignerId().equals(errand.getAssignerId()) && userErrand.getTitle().equals(errand.getTitle()))
                    filteredErrands.remove(errand);
            }

            if (!errand.getAccepterId().equals(""))
                try {
                    filteredErrands.remove(errand);
                } catch (Exception e) {
                }
        }
    }

    /**
     * A method to check if the title matches the criteria set by the checkboxes related to title keywords.
     *
     * @param title title entered by the user in the search form's title EditText
     * @return      true if errand matches the criteria set by the checkboxes false otherwise
     */
    public static boolean checkTitle(String title) {
        String matchText = new String(searchText);
        String[] keywords = matchText.toLowerCase().split(",");

        String lowerCaseTitle = title.toLowerCase();

        if (matchAllKeywords) {

            boolean matches = true;

            for (String word: keywords) {
                if (!lowerCaseTitle.matches(".*" + word + ".*")) {
                    matches = false;
                }
            }
            return matches;
        }
        else {
            boolean matches = false;

            for (String word: keywords) {
                if (lowerCaseTitle.matches(".*" + word + ".*")) {
                    matches = true;
                }
            }
            return matches;
        }
    }

    /**
     * A method to check if the pay is not lower than the pay entered in the search form's pay EditText.
     *
     * @param pay the pay entered in the search form's pay EditText
     * @return    true if the entered pay is not lower false otherwise
     */
    public static boolean goodPay(String pay) {
        if (filterPay && Double.valueOf(payText.replace(",",".")) > Double.valueOf(pay)) {
            return false;
        }
        return true;
    }

    /**
     * A method to check if the estimated completion time is not greater than the completion time entered in the search form's time EditText.
     * @param time the time entered in the search form's time EditText
     * @return     true if entered time is smaller false otherwise
     */
    public static boolean goodTime(String time) {
        if (timeGiven && Double.valueOf(timeText.replace(",",".")) < Double.valueOf(time)) {
            return false;
        }
        return true;
    }

    /**
     * A method to fill allErrands and filteredErrands with fresh errands from the database.
     *
     //* @param errandStateService the caller
     */
    public static void fillErrandsList(ErrandStateService errandStateService) {
        allErrands = errandStateService.getErrands();
        filteredErrands = new ArrayList<Errand>(allErrands);
    }
}
