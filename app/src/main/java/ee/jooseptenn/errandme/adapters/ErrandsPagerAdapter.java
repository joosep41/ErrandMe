package ee.jooseptenn.errandme.adapters;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import ee.jooseptenn.errandme.fragments.ActiveErrandFragment;

/**
 * Adapter for swiping between the AddedErrandsAdapter and the AcceptedErrandsAdapter.
 */

public class ErrandsPagerAdapter extends FragmentPagerAdapter { //https://developer.android.com/training/implementing-navigation/lateral.html (modified compared to original)

    public ErrandsPagerAdapter(FragmentManager fm) {
        super(fm);
    }


    /**
     * A method for getting the view for a specific item.
     *
     * @param position the position of the item
     * @return         a fragment that corresponds to the specified item
     */
    @Override
    public Fragment getItem(int position) {
        Fragment fragment = new ActiveErrandFragment();
        Bundle b = new Bundle();
        switch (position) {
            case 0:
                b.putString("adapterType", "addedErrandsAdapter");
                break;
            case 1:
                b.putString("adapterType", "acceptedErrandsAdapter");
        }
        fragment.setArguments(b);
        return fragment;
    }

    /**
     * A method for getting the item count.
     *
     * @return the amount of items in the adapter
     */
    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return "Added errands";
            default:
                return "Accepted errands";
        }
    }
}
