package com.pemGP.puboardsteward;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Florian on 01.08.2014.
 */
public class AccountStore {
    private HashMap<String, mAccount> mAccounts;

    private static AccountStore sAccountStore;
    private Context mAppContext;
    private static Context mActivity;
    private ArrayList<OnAccountStoreUpdatedListener> mListeners;
    private HashMap<Integer, String> pendingTransactions;    // contains <ID, amount>
    private OnTransactionCompletedListener mTransactionListener;
    private int mTransactionIDcounter;      // will be incremented for each transaction.

    public static final String TAG = "PuboardSteward.AccountStore";

    protected static final String FILE_ACCOUNTS="accounts.dat";

    private boolean updating;

    private AccountStore(Context appContext) {
        mAppContext = appContext;
        mListeners = new ArrayList<OnAccountStoreUpdatedListener>();
        mAccounts = new HashMap<String, mAccount>();
        pendingTransactions = new HashMap<Integer, String>();
        mTransactionIDcounter = 1;          // some starting value

        read(mAppContext, FILE_ACCOUNTS);
        // reads in an extra thread

        // refreshAccounts();
        // get information from internet
    }

    public static AccountStore get(Context c){
        if (sAccountStore == null){
            // c is Activity
            mActivity = c;
            sAccountStore = new AccountStore(c.getApplicationContext());
        }
        return sAccountStore;
    }

    public HashMap<String, mAccount> getAccounts() {
        return mAccounts;
    }

    public mAccount getAccount(String id){
        if (mAccounts.containsKey(id))
            return mAccounts.get(id);
        else
            return null;
    }

    /**
     *
     * @param a
     */
    public void putAccount(mAccount a){
        //  this overwrites existing entry
        mAccounts.put(a.getAccID(), a);
        try {
            save(mAppContext,FILE_ACCOUNTS,mAccounts);
        } catch (Exception f) {
            Log.e(TAG, f.toString());
        }
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
        new AsyncTask<Void, Void, HashMap<String, mAccount>>(){
            @Override
            protected HashMap<String, mAccount> doInBackground(Void... voids) {
                try {
                    FileInputStream fin = c.openFileInput(fileName);
                    ObjectInputStream ois = new ObjectInputStream(fin);
                    Object tempObj = ois.readObject();
                    fin.close();
                    Log.i(TAG, "Accounts loaded from file");
                    //makeToast("Accounts loaded from file");
                    return (HashMap<String, mAccount>) tempObj;
                } catch (IOException e) {
                    Log.e(TAG+".read", "Could not read Account file: "+e);
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
            protected void onPostExecute(HashMap<String, mAccount> AccountHashMap) {
                super.onPostExecute(AccountHashMap);
                if (AccountHashMap != null) {
                    // synchronize and save:
                    mAccounts.clear();
                    mAccounts.putAll(AccountHashMap);

                    makeToast("Accounts loaded from file");
                }
                else {
                    mAccounts.clear();
                    try{
                        save(mAppContext,FILE_ACCOUNTS,mAccounts);
                        makeToast("Empty accounts saved to file");
                    } catch (Exception f) {
                        Log.e(TAG, f.toString());
                    }
                }
                notifyOnAccountStoreUpdatedListener();

                // fetch from web:
                refreshAccounts();
            }
        }.execute();
    }

    private class AccountsSpreadsheetImportTask extends AsyncTask<Void, Void, HashMap<String, mAccount>> {
        @Override
        protected HashMap<String, mAccount> doInBackground(Void... params) {
            updating = true;
            HashMap<String, mAccount> tempAccounts = new HashMap<String, mAccount>();
            try {
                JSONArray objects = ServerUtilities.getFromSpreadsheet(mActivity,"accounts");
                // TODO: write above such that it sums over all entries in the accounts table and returns all values.
                if (objects == null) {
                    Log.e(TAG,"Could not obtain JSONArray");
                    // stop updating procedure
                    ((OnAuthenticationErrorListener) mActivity).onAuthenticationError();
                    return null;
                }

                for (int i = 0; i < objects.length(); i++) {
                    JSONObject session = objects.getJSONObject(i);
                    mAccount acc = new mAccount(session.getString("name"),session.getString("id"),session.getDouble("amount"));
                    tempAccounts.put(acc.getAccID(), acc);
                }
            } catch (Exception e) {
                Log.e("ItemFeed", "Error loading JSON", e);
                return null;
            }
            return tempAccounts;
        }

        @Override
        protected void onPostExecute(HashMap<String, mAccount> tempAccounts) {
            super.onPostExecute(tempAccounts);
            if (tempAccounts != null) {
                // synchronize and save:
                mAccounts.clear();
                mAccounts.putAll(tempAccounts);
                notifyOnAccountStoreUpdatedListener();

                try {
                    save(mAppContext, FILE_ACCOUNTS, mAccounts);
                    makeToast("Accounts updated and saved to file");
                } catch (IOException e) {
                    Log.e(TAG, e + "");
                }
                updating = false;
                return;
            }
            updating = false;
        }
    }

    public void processJSON(Context context, JSONArray data) {
        /**
         * used for incoming transaction updates.
         * parse JSON here and update account.
         */
        String id=null, name=null, regid=null, amountStr = null, totalStr = null, type = null;
        double amount=0, total=0;
        int tid = 0;
        JSONObject session = null;
        for (int i = 0; i < data.length(); i++) {
            try {
                // read JSON objects
                session = data.getJSONObject(i);
                id = session.getString(context.getString(R.string.transaction_id));
                name = session.getString(context.getString(R.string.transaction_name));
                amountStr = session.getString(context.getString(R.string.transaction_amount));
                totalStr = session.getString(context.getString(R.string.transaction_total));
                regid = session.getString(context.getString(R.string.transaction_regid));
                tid = session.getInt(context.getString(R.string.transaction_tid));
                type = session.getString(context.getString(R.string.transaction_type));
                Log.i(TAG + ".processJSON", "Read from JSON GCM: "+id+", "+name+", "+amountStr+", "+totalStr);
                amount = Double.parseDouble(amountStr);
                total = Double.parseDouble(totalStr);
            } catch (JSONException e) {
                Log.e(TAG+".processJSON",e+"");
                continue;
                // exit for loop for this entry
            } catch (NumberFormatException e) {
                Log.e(TAG+".processJSON",e+""+"\nCould not parse double from: Total = "+totalStr+", Amount = "+amountStr);
                continue;
            }
            mAccount tempAcc = mAccounts.get(id);
            if (tempAcc == null){
                // if new account created
                tempAcc = new mAccount(name,id);
            }
            tempAcc.addAccMoney(amount);
            if (tempAcc.getAccMoney() != total)
                Toast.makeText(context, "Please check Accounts. Transactions do not sum up well!", Toast.LENGTH_LONG).show();
            // this method ensure saving of accounts to file.
            putAccount(tempAcc);
            if (regid.equals(ServerUtilities.getStoredRegId(context))) {
                // if Transaction came from THIS device AND is waiting to be confirmed:
                String accId = pendingTransactions.remove(tid);
                if (accId != null) {
                    if (accId.equals(id)) {
                        // just a double check. If this is the same account ID.
                        Toast.makeText(context, "Transaction completed", Toast.LENGTH_LONG).show();
                    } else {
                        // Account IDs do not agree! There must be a session error!
                        Toast.makeText(context, "Transaction completed, but this transaction was from another account!", Toast.LENGTH_LONG).show();
                    }
                    // callback to DrinksPagerFragment
                    if (mTransactionListener != null) {
                        try {
                            mTransactionListener.onTransactionCompleted(amount, tempAcc);
                        } catch (Exception e) {
                            // likely to happen if switched to another fragment.
                            Log.e(TAG+".processJSON", "Could not call onTransactionCompleted+\n"+e);
                        }
                    }
                } else if (type.equals(context.getString(R.string.transaction_type_undolasttransaction))){
                    // produced by undoLastTransaction?
                    ((DrinksPagerFragment)mTransactionListener).displayConfirmTransaction(DrinksPagerFragment.TransactionState.UNDO_COMPLETE,amount,tempAcc);
                } else {
                    // Debug
                    Log.i(TAG+".processJSON", "type: "+type +", regid: "+regid);
                }
            }
        }

    }

    public void pushTransaction(Context context, mAccount account, double amount, String action,
                                ArrayList<Drink> items, OnTransactionCompletedListener listener){
        /**
         * two ways: either directly store info on device && push to server.
         *  then: server only pushes refresh JSON update to other device
         * OR:
         *  Only push to server here && get update in other thread.
         *  --> Web connection necessary!
         *  NOW: choose second!
         *
         *  Only provide items and listener if doing purchase. Topup only needs listener!
         */

        // Register the fragment waiting for confirmation
        if (listener != null)
            mTransactionListener = listener;

        // create Hashmap containing push information
        HashMap<String,String> transaction = new HashMap<String, String>();
        transaction.put(context.getString(R.string.transaction_id),
                account.getAccID());
        transaction.put(context.getString(R.string.transaction_name),
                account.getAccName());
        // put negative sign before purchase, top-ups are positive
        if (action.equals(context.getString(R.string.transaction_purchase)))
            amount = -amount;
        transaction.put(context.getString(R.string.transaction_amount), amount+"");
        transaction.put(context.getString(R.string.transaction_action),
                action);
        // Put the inventory record:
        if (items != null) {
            String itemsPut = "";
            for (int i = 0; i < items.size(); i++) {
                itemsPut += items.get(i).getName() + ",";   // no space after comma! Necessary for javascript code
            }
            transaction.put(context.getString(R.string.transaction_items), itemsPut);
        }
        mTransactionIDcounter++;
        transaction.put(context.getString(R.string.transaction_tid), mTransactionIDcounter+"");
        // Push transaction to server
        ServerUtilities.sendTransactionToBackend(context, transaction);
        // Store that this transaction is waiting to be confirmed.
        // put for top-up AND purchase
        pendingTransactions.put(mTransactionIDcounter, account.getAccID());
    }

    public void refreshAccounts(){
        // Only start new instance if not already updating
        if (!updating)
            new AccountsSpreadsheetImportTask().execute();
    }

    public void addOnAccountStoreUpdatedListener (OnAccountStoreUpdatedListener mListener) {
        mListeners.add(mListener);
    }

    public void removeOnAccountStoreUpdatedListener (OnAccountStoreUpdatedListener mListener) {
        mListeners.remove(mListener);
    }

    public void notifyOnAccountStoreUpdatedListener (){
        for (int i = 0; i < mListeners.size(); i++) {
            try {
                mListeners.get(i).onAccountStoreUpdated();
            } catch (Exception e) {
                Log.e(TAG+".notifyOnAccountStoreUpdatedListener", "Could not call Listener. \n"+e);
            }
        }
    }

    public interface OnAuthenticationErrorListener {
        public void onAuthenticationError();
    }

    public interface OnAccountStoreUpdatedListener {
        public void onAccountStoreUpdated();
    }

    public interface OnTransactionCompletedListener {
        public void onTransactionCompleted(double amountConfirmed, mAccount acc);
    }
}
