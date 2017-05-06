package ee.jooseptenn.errandme.fragments;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import ee.jooseptenn.errandme.R;

/**
 * A dialog that displays the location of an errand on a map.
 */

public class LocationDialogFragment extends FragmentActivity implements OnMapReadyCallback { // https://developers.google.com/maps/documentation/android-api/

    LayoutInflater inflater;
    static View view;
    private GoogleMap mMap;
    private String location;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.map_layout);
        getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);

        location = getIntent().getStringExtra("location");

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Button closeButton = (Button) findViewById(R.id.close_button);

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    /**
     * A method that is called when the GoogleMap is ready. A marker is added on the map to the location where the errand has to be done.
     *
     * @param map the GoogleMap that is shown to the user
     */
    @Override
    public void onMapReady(GoogleMap map) {

        mMap = map;

        String latitude = location.split(",")[0];
        String longitude = location.split(",")[1];

        LatLng loc = new LatLng(Double.valueOf(latitude), Double.valueOf(longitude));


        mMap.addMarker(new MarkerOptions()
                    .position(loc)
                    .title(getBaseContext().getString(R.string.location)));

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc,15));

    }

}
