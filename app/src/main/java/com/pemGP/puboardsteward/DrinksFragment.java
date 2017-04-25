package com.pemGP.puboardsteward;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.TreeMap;

/**
 * A fragment representing a list of Items.
 * <p />
 * <p />
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class DrinksFragment extends Fragment implements DrinkStore.OnDrinkStoreUpdatedListener {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_SECTION_NUMBER = "DrinksFragment.Position";
    private static final String ARG_SECTION_TITLE = "DrinksFragment.Title";

    public static final String TAG = "PuboardSteward.DrinksFragment";

    // Model variables
    private TreeMap<Integer, Drink> mDrinks;
    private int categoryNumber;

    ArrayAdapter<Drink> drinkArrayAdapter;

    private OnDrinksFragmentInteractionListener mListener;

    // TODO: Rename and change types of parameters
    public static DrinksFragment newInstance(int sectionNumber, String sectionTitle) {
        DrinksFragment fragment = new DrinksFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        args.putString(ARG_SECTION_TITLE, sectionTitle);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public DrinksFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            // position +1
            categoryNumber = getArguments().getInt(ARG_SECTION_NUMBER);

        }

        // Prevent destroy on rotation:
        //setRetainInstance(true);

    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_drinks_order,container,false);

        // Get Drinks from DrinkStore in activity:
        // DrinkStore is Singleton;
        mDrinks = DrinkStore.get(getActivity()).getDrinks();

        // Create GridView of Drinks
        drinkArrayAdapter = new ArrayAdapter<Drink>(getActivity(),
                R.layout.simple_list_item_1);
                //new ArrayList<Drink>(mDrinks.subMap(categoryNumber*1000,(categoryNumber+1)*1000).values()));
        drinkArrayAdapter.addAll(mDrinks.subMap(categoryNumber*1000,(categoryNumber+1)*1000).values());

        GridView mGridViewDrinks = (GridView) v.findViewById(R.id.gridViewDrinks);
        mGridViewDrinks.setAdapter(drinkArrayAdapter);
        mGridViewDrinks.setOnItemClickListener(new GridView.OnItemClickListener(){

            @Override
            public void onItemClick(AdapterView<?> a, View v, int position, long id)
            {
                Drink d = (Drink)(a.getAdapter()).getItem(position);

                //tell parent activity which drink was clicked.
                mListener.onDrinksFragmentInteraction(d);

            }
        });

        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnDrinksFragmentInteractionListener) getParentFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException(getParentFragment().toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        DrinkStore.get(getActivity()).addOnDrinkStoreUpdatedListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        DrinkStore.get(getActivity()).removeOnDrinkStoreUpdatedListener(this);
    }

    protected void makeToast(String s){
        Toast.makeText(getActivity(),s, Toast.LENGTH_LONG).show();
    }

    public ArrayAdapter getArrayAdapter(){
        return drinkArrayAdapter;
    }


    /**
    * This interface must be implemented by activities that contain this
    * fragment to allow an interaction in this fragment to be communicated
    * to the activity and potentially other fragments contained in that
    * activity.
    * <p>
    * See the Android Training lesson <a href=
    * "http://developer.android.com/training/basics/fragments/communicating.html"
    * >Communicating with Other Fragments</a> for more information.
    */

    public interface OnDrinksFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onDrinksFragmentInteraction(Drink d);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onDrinkStoreUpdated() {
        drinkArrayAdapter.clear();
        drinkArrayAdapter.addAll(DrinkStore.get(getActivity()).getDrinks().subMap(categoryNumber * 1000, (categoryNumber + 1) * 1000).values());
        drinkArrayAdapter.notifyDataSetChanged();
    }
}
