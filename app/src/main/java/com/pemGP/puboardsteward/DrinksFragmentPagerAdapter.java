package com.pemGP.puboardsteward;

import android.content.Context;
import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentPagerAdapter;

/**
 * Created by Florian on 04.08.2014.
 */
class DrinksFragmentPagerAdapter extends FragmentPagerAdapter {
    private static int NUM_ITEMS;
    private static String[] TITLE_CATEGORY;

    Context context;

    DrinksFragmentPagerAdapter(Context context, FragmentManager fragmentManager) {
        super(fragmentManager);
        this.context = context;
        TITLE_CATEGORY = DrinkStore.get(context).getCategoryTitles();
        NUM_ITEMS = TITLE_CATEGORY.length;
    }

    // Returns total number of pages
    @Override
    public int getCount() {
        return NUM_ITEMS;
    }

    // Returns the fragment to display for that page
    @Override
    public Fragment getItem(int position) {
        return DrinksFragment.newInstance(position+1, TITLE_CATEGORY[position]);

    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    // Returns the page title for the top indicator
    @Override
    public CharSequence getPageTitle(int position) {
        return TITLE_CATEGORY[position];
    }

    void refreshCategories(Context context) {
        TITLE_CATEGORY= DrinkStore.get(context).getCategoryTitles();
        NUM_ITEMS = TITLE_CATEGORY.length;
    }
}

