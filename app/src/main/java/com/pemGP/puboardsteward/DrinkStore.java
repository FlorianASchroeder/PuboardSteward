package com.pemGP.puboardsteward;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.TreeMap;



/**
 * Created by Florian on 01.08.2014.
 */
public class DrinkStore {
    private TreeMap<Integer, Drink> mDrinks;

    // not final, will be read out from spreadsheet:
    private static String[] CATEGORY_TITLES = {"Beer", "Shot"};//, "Ale", "Cider", "Shot", "Misc"};

    private static DrinkStore sDrinkStore;
    private Context mAppContext;
    private OnDrinkStoreUpdatedListener mListener;
    private ArrayList<OnDrinkStoreUpdatedListener>  mListeners;


    public static final String TAG = "PuboardSteward.DrinksStore";

    protected static final String FILE_DRINKS="drinks.dat";
    protected static final String FILE_CATEGORIES="categories.dat";

    private static final int CATEGORY_BEER = 1000;
    private static final int CATEGORY_ALE = 4000;
    private static final int CATEGORY_CIDER = 3000;
    private static final int CATEGORY_SHOT = 2000;
    private static final int CATEGORY_MISC = 5000;

    private boolean updating;

    private DrinkStore(Context appContext) {
        mAppContext = appContext.getApplicationContext();
        mListener = (OnDrinkStoreUpdatedListener) appContext;       // the main activity, but can be cancelled
        mListeners = new ArrayList<OnDrinkStoreUpdatedListener>();

        // first load old entries:
        read(mAppContext, FILE_CATEGORIES);

        read(mAppContext, FILE_DRINKS);

        // In the meantime: retrieve updated entries from spreadsheet:
        //new DrinksSpreadsheetImportTask().execute();
    }

    public static DrinkStore get(Context c){
        // c is Activity
        if (sDrinkStore == null){
            sDrinkStore = new DrinkStore(c);
        }
        return sDrinkStore;
    }

    public TreeMap<Integer, Drink> getDrinks() {
        if (mDrinks != null)
            return mDrinks;
        else
            return new TreeMap<Integer, Drink>();
    }

    public Drink getDrink(int id){
        return mDrinks.get(id);
    }

    public String[] getCategoryTitles() {
        return CATEGORY_TITLES;
    }

    protected void makeToast(String s){
        Toast.makeText(mAppContext, s, Toast.LENGTH_LONG).show();
    }

    public static void save(Context c, String fileName,Object mObj) throws IOException {
        FileOutputStream fout = c.openFileOutput(fileName, Context.MODE_PRIVATE);
        ObjectOutputStream oos = new ObjectOutputStream(fout);
        oos.writeObject(mObj);
        fout.close();
    }

    public void read(final Context c, final String fileName) {

        new AsyncTask<Void, Void, Object>(){

            @Override
            protected Object doInBackground(Void... voids) {
                try {
                    FileInputStream fin = c.openFileInput(fileName);
                    ObjectInputStream ois = new ObjectInputStream(fin);
                    Object tempObj = ois.readObject();
                    fin.close();
                    if (fileName.equals(FILE_DRINKS))
                        Log.i(TAG, "Drinks loaded from file");
                    //makeToast("Accounts loaded from file");
                    return tempObj;

                } catch (IOException e) {
                    Log.e(TAG+".read", "Could not read Drink file: "+e);
                    return null;
                } catch (ClassNotFoundException e) {
                    Log.e(TAG+".read", "Class not found: "+e);
                    return null;
                } catch (Exception e) {
                    Log.e(TAG+".read", "Something else happened: "+e);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Object o) {
                super.onPostExecute(o);
                if (fileName.equals(FILE_DRINKS)) {
                    if (o != null) {
                        mDrinks = (TreeMap<Integer, Drink>) o;
                        //makeToast("Drinks loaded from file");     //you can see that
                    }
                    else {
                        Log.e(TAG+".read", "drinks will be added from sample list.");
                        mDrinks = new TreeMap<Integer, Drink>();
                        // code the categories: Beer(lager) = 01, Ale = 02, Cider = 03, Shots = 04, else = 05
                        mDrinks.put(CATEGORY_BEER+1, new Drink("Asahi",1.5,CATEGORY_BEER+1));
                        mDrinks.put(CATEGORY_BEER + 3, new Drink("Corona", 1.5, CATEGORY_BEER + 3));
                        mDrinks.put(CATEGORY_BEER + 5, new Drink("Tsing Tao", 1.0, CATEGORY_BEER + 5));
                        mDrinks.put(CATEGORY_BEER + 4, new Drink("Peroni", 1, CATEGORY_BEER + 4));
                        mDrinks.put(CATEGORY_BEER + 2, new Drink("Becks", 1.5, CATEGORY_BEER + 2));
                        mDrinks.put(CATEGORY_SHOT + 3, new Drink("Smirnoff Vodka", 1.0, CATEGORY_SHOT + 3));
                        mDrinks.put(CATEGORY_SHOT + 2, new Drink("Captain Morgan Rum", 1, CATEGORY_SHOT + 2));
                        mDrinks.put(CATEGORY_SHOT + 1, new Drink("Bacardi Rum", 1, CATEGORY_SHOT + 1));
                        mDrinks.put(CATEGORY_ALE + 2, new Drink("Newcastle Brown Ale", 2, CATEGORY_ALE + 2));
                        mDrinks.put(CATEGORY_ALE+1, new Drink("Hobgoblin", 2,CATEGORY_ALE+1));
                        mDrinks.put(CATEGORY_CIDER + 2, new Drink("Bulmers", 1.0, CATEGORY_CIDER + 2));
                        mDrinks.put(CATEGORY_CIDER + 1, new Drink("Aspall", 1, CATEGORY_CIDER + 1));
                        for (int i=0; i < 5; i++) {
                            mDrinks.put(CATEGORY_MISC + i + 1, new Drink("Drink " + (i + 1), (i + 1), CATEGORY_MISC + i + 1));
                        }
                        try{
                            save(mAppContext,FILE_DRINKS, mDrinks);
                            makeToast("Sample Drinks saved to file");
                        } catch (Exception f) {
                            Log.e(TAG, f.toString());
                        }
                    }
                    notifyOnDrinkStoreUpdatedListener();     // put this wherever is the last call categories or drinks
                    // Update from web after loading from file!
                    new DrinksSpreadsheetImportTask().execute();

                } else if (fileName.equals(FILE_CATEGORIES)) {
                    if (o != null) {
                        CATEGORY_TITLES = (String[]) o;
                    }
                }
            }
        }.execute();
    }

    private class DrinksSpreadsheetImportTask extends AsyncTask<Void,Void,TreeMap<Integer, Drink>> {
        LinkedHashMap<String,Integer> categoryLastID= new LinkedHashMap<String, Integer>();

        @Override
        protected TreeMap<Integer, Drink> doInBackground(Void... voids) {
            updating = true;
            TreeMap<Integer, Drink> tempDrinks = new TreeMap<Integer, Drink>();
            try {
                JSONArray objects = ServerUtilities.getFromSpreadsheet((NavDrawActivity) mListener,"drinks");

                if (objects == null) {
                    Log.e(TAG,"Could not obtain JSONArray");
                    // stop updating procedure
                    updating = false;
                    ((OnAuthenticationErrorListener) mListener).onAuthenticationError();
                    return null;
                }

                int ID = 0;
                //mDrinks.clear();        // empty all drinks first
                for (int i = 1; i < objects.length(); i++) {
                    JSONObject session = objects.getJSONObject(i);

                    String tempCategory = session.getString("category");

                    if (!categoryLastID.containsKey(tempCategory)){
                        // add category and initialise ID
                        categoryLastID.put(tempCategory,(categoryLastID.size()+1)*1000);
                    }
                    // get last ID of category, increment and push back to map
                    categoryLastID.put(tempCategory, ID = categoryLastID.get(tempCategory)+1);

                    Drink item = new Drink(session.getString("name"),session.getDouble("price"),ID);
                    tempDrinks.put(item.getId(),item);
                }
            } catch (Exception e) {
                Log.e(TAG+".DrinksSpreadsheetImportTask", "Error loading JSON", e);
                // Sheet is being edited
                return null;
            }
            return tempDrinks;
        }

        @Override
        protected void onPostExecute(TreeMap<Integer, Drink> tempDrinks) {
            super.onPostExecute(tempDrinks);
            if (tempDrinks != null) {
                CATEGORY_TITLES = categoryLastID.keySet().toArray(new String[0]);
                // following 2 lines necessary to use mAdapter.notifyDataSetHasChanged();
                mDrinks.clear();
                mDrinks.putAll( tempDrinks );

                String msg = "Drinks updated";
                try {
                    save(mAppContext, FILE_DRINKS, mDrinks);
                    save(mAppContext, FILE_CATEGORIES, CATEGORY_TITLES);
                    msg += " and saved";
                    updating = false;
                } catch (IOException e) {
                    Log.e(TAG, e + "");
                }
                makeToast(msg);
                Log.i(TAG, msg);

                notifyOnDrinkStoreUpdatedListener();
                return;
            }
            Log.e(TAG+".DrinksSpreadsheetImportTask", "Import failed");
            makeToast("Updating failed! Check Settings and Connection");
            updating = false;
        }
    }

    public void refreshDrinks(Context context){
        mListener = (OnDrinkStoreUpdatedListener) context;
        // Only start new instance if not already updating
        if (!updating)
            new DrinksSpreadsheetImportTask().execute();
    }
    public interface OnDrinkStoreUpdatedListener {
        // TODO: Update argument type and name
        public void onDrinkStoreUpdated();
    }

    public void addOnDrinkStoreUpdatedListener (OnDrinkStoreUpdatedListener mListener) {
        mListeners.add(mListener);
    }

    public void removeOnDrinkStoreUpdatedListener (OnDrinkStoreUpdatedListener mListener) {
        mListeners.remove(mListener);
    }

    public void notifyOnDrinkStoreUpdatedListener (){
        for (int i = 0; i < mListeners.size(); i++) {
            try {
                mListeners.get(i).onDrinkStoreUpdated();
            } catch (Exception e) {
                Log.e(TAG+".notifyOnDrinkStoreUpdatedListener", "Could not call Listener. \n"+e);
            }
        }
    }

    public interface OnAuthenticationErrorListener {
        public void onAuthenticationError();
    }

}
