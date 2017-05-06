package ee.jooseptenn.errandme.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import ee.jooseptenn.errandme.R;
import ee.jooseptenn.errandme.adapters.ErrandsPagerAdapter;

/**
 * A fragment that sets the adapter for the ViewPager that is used to switch between the user's activeAddedErrands and activeAcceptedErrands.
 */

public class MyErrandsFragment extends Fragment { //https://developer.android.com/training/implementing-navigation/lateral.html

    private ErrandsPagerAdapter mErrandsPagerAdapter;
    private ViewPager mViewPager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.my_errands_layout, container, false);

        mErrandsPagerAdapter = new ErrandsPagerAdapter(getChildFragmentManager());

        mViewPager = (ViewPager) view.findViewById(R.id.pager);
        mViewPager.setAdapter(mErrandsPagerAdapter);

        return view;
    }

}
