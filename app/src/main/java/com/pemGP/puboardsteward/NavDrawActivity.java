package com.pemGP.puboardsteward;
// Authentication inspired by: http://www.androiddesignpatterns.com/2013/01/google-play-services-setup.html

import static com.pemGP.puboardsteward.CommonUtilities.DISPLAY_MESSAGE_ACTION;
import static com.pemGP.puboardsteward.CommonUtilities.EXTRA_MESSAGE;
import static com.pemGP.puboardsteward.CommonUtilities.KEY_SPREADSHEET_ID;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class NavDrawActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks,
        DrinksFragment.OnDrinksFragmentInteractionListener,
        AccountListFragment.OnAccountListFragmentInteractionListener,
        DrinkListFragment.OnDrinkListFragmentInteractionListener,
        DrinkStore.OnDrinkStoreUpdatedListener,
        DrinkStore.OnAuthenticationErrorListener {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    // Fragment currently displayed in Container
    Fragment mContainerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    // Model variables
    AccountStore accountStore;

    static final int REQUEST_CODE_RECOVER_PLAY_SERVICES = 1001;
    static final int REQUEST_CODE_PICK_ACCOUNT = 1002;
    static final int REQUEST_CODE_EDIT_SHEET = 1003;


    public static final String TAG = "PuboardSteward.NavDrawActivity";
    public static final String STATE_CONTAINER_FRAGMENT = "PuboardSteward.CurrentContainerFragment";

    public static final String MIME_TEXT_PLAIN="text/plain";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nav_draw);

        // load script server settings
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        CommonUtilities.loadScriptSettings(this);

        // find left drawer fragment from .xml
        FragmentManager fragmentManager = getFragmentManager();

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                fragmentManager.findFragmentById(R.id.navigation_drawer);
        if (savedInstanceState != null) {
            // Restore the fragment's instance
            // is this necessary? From: http://stackoverflow.com/questions/15313598/once-for-all-how-to-correctly-save-instance-state-of-fragments-in-back-stack

            /*mContainerFragment = fragmentManager.getFragment(savedInstanceState,STATE_CONTAINER_FRAGMENT);
            fragmentManager.beginTransaction()
                    .replace(R.id.container, mContainerFragment)
                    .commit();*/
        }

        mTitle = getTitle();

        // Set up the drawer inside the main activity drawer_layout.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        // Load Account Singleton
        accountStore = AccountStore.get(this);

        //NFC handle
        NfcAdapter mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter ==null) {
            //Stop here, need NFC!
            this.makeToast("Your device does not support NFC");
            finish();
            return;
        }

        // register with GCM script, always!
        ServerUtilities serverUtil = new ServerUtilities(this);
        if(!serverUtil.register())
            Log.e(TAG,"Could not register with GCM");

        handleIntent(getIntent());
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // Implemented NavigationDrawerFragment interface.
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.container);

        // restore old fragments:
        if (position == 0 && (fragment == null || fragment.getClass() != DrinksPagerFragment.class)) {
            fragment = DrinksPagerFragment.newInstance(position+1);
        } else if (position == 1 && (fragment == null || fragment.getClass() != AccountListFragment.class)){
            fragment = AccountListFragment.newInstance(position+1);
        } else if (position == 2 && (fragment == null || fragment.getClass() != DrinkListFragment.class)) {
            //fragment = DrinkListFragment.newInstance(position + 1);
            fragment = new SettingsFragment();
        }
        // attach Fragment
        mContainerFragment = fragment;
        fragmentManager.beginTransaction()
                .replace(R.id.container, mContainerFragment)
//                .addToBackStack(null) // do not put on backstack, as back + screen rotate does not lead to proper fragment initioation
                .commit();
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_section1);
                break;
            case 2:
                mTitle = getString(R.string.title_section2);
                break;
            case 3:
                mTitle = getString(R.string.title_section3);
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.nav_draw, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        ServerUtilities servUtil;
        switch (item.getItemId()) {
//            case R.id.action_settings:
//                return true;
            case R.id.options_unregister:
                Log.i(TAG,"unregister called");
                servUtil = new ServerUtilities(this);
                servUtil.unregister(this);
            case R.id.options_register:
                Log.i(TAG,"register called");
                servUtil = new ServerUtilities(this);
                servUtil.register();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //Save current fragment's instance
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.putFragment(outState,STATE_CONTAINER_FRAGMENT,fragmentManager.findFragmentById(R.id.container));
    }

    // NFC Handling

    @Override
    protected void onNewIntent(Intent intent) {
       //super.onNewIntent(intent);
        /**
         * This method gets called, when a new Intent gets associated with the current activity instance.
         * Instead of creating a new activity, onNewIntent will be called. For more information have a look
         * at the documentation.
         *
         * In our case this method gets called, when the user attaches a Tag to the device.
         */
        Log.i(TAG, "Obtained intent: " + intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {

        String action = intent.getAction();
        //mTextViewHistory.setText(action);
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Log.i(TAG, action);
            String type = intent.getType();
            if (MIME_TEXT_PLAIN.equals(type)) {
                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                new NdefReaderTask().execute(tag);
            } else {
                Log.i(TAG, "Wrong mime type: " + type);
            }

        } else if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {

            // In case we would still use the Tech Discovered Intent
            //Log.i(TAG,action);
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            Log.i(TAG+".handleIntent",action+"\n"+tag);
            String[] techList = tag.getTechList();
            String searchedTech = Ndef.class.getName();
            // Important for College Cards:
            String searchedTech2= MifareClassic.class.getName();

            for (String tech : techList) {
                if (searchedTech.equals(tech)) {
                    new NdefReaderTask().execute(tag);
                    break;
                } else if (searchedTech2.equals(tech)){
                    Log.i(TAG, "read Mifare Classic");
                    try {
                        new MifareClassicUidReaderTask().execute(tag);
                    } catch (Exception e) {
                        Log.e(TAG,e.toString()+"\n ReaderTask error");
                    }
                    break;
                }
            }
        } else {
            Log.i(TAG,"other action:"+action);
        }
    }

    private class MifareClassicUidReaderTask extends AsyncTask<Tag, Void, String>
    {
        // Only read Sector 0, Block 0
        @Override
        protected String doInBackground(Tag[] p1) {
            Tag tag = p1[0];
            MifareClassic mfc = MifareClassic.get(tag);
            byte[] data;
            String cardData = null;
            try {				//  5.1) Connect to card
                mfc.connect();
                boolean auth;
                // 5.2) and get the number of sectors this card has..and loop thru these sectors
                int secCount = mfc.getSectorCount();
                //Log.i(TAG,"Card has "+secCount+" sectors");
                int bCount;
                int bIndex;
                for(int j = 0; j < 1; j++){ // last sector gives crash, Only select first sector
                    // to read more sectors increase threshold
                    // 6.1) authenticate the sector
                    auth = mfc.authenticateSectorWithKeyA(j, MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY);   //should work for all uni-cards
                    if (!auth){
                        auth = mfc.authenticateSectorWithKeyA(j,MifareClassic.KEY_DEFAULT);
                    }
                    if (!auth){
                        auth = mfc.authenticateSectorWithKeyA(j,MifareClassic.KEY_NFC_FORUM);
                    }
                    if(auth){
                        // 6.2) In each sector - get the block count
                        bCount = mfc.getBlockCountInSector(j);
                        //Log.i(TAG,"Sector" +j+" has "+bCount+" blocks");
                        bIndex = mfc.sectorToBlock(j);
                        for(int i = 0; i < 1; i++){
                            //bIndex = mfc.sectorToBlock(j);
                            // 6.3) Read the block
                            data = mfc.readBlock(bIndex);
                            // 7) Convert the data into a string from Hex format. 
                            Log.i(TAG+".MifareClassicUidReaderTask", new java.math.BigInteger(1, data).toString(16));
                            //Log.i(TAG, data.toString());
                            //Log.i(TAG, new String(data));
                            if (i == 0 && j == 0)
                                cardData = new java.math.BigInteger(1, data).toString(16);
                            bIndex++;

                        }
                    }else{		// Authentication failed - Handle it
                        Log.i(TAG, "No auth for Sector "+j);
                    }
                }
                mfc.close();
            }catch (IOException e) {
                Log.e(TAG, e+"");
                //showAlert(3);
            }
            return cardData;    // contains card ID

        }

        @Override
        protected void onPostExecute(String result) {
            // decide which fragments to call
            // if account exists and DrinksFragment hasSaldo
            if (result == null || result.equals("")) {
                makeToast("No ID found on card");
                return;
            }

//            makeToast("Read ID: "+result);
            mAccount tempFoundAccount = accountStore.getAccount(result);
            if (tempFoundAccount == null){
                makeToast("Make new Account");
                // ask for Name and Top-Up amount.
                tempFoundAccount = new mAccount("",result,0);     // create here to pass ID

            } else {
                // Process transaction if Drinks selected
                if (mContainerFragment.getClass() == DrinksPagerFragment.class) {
                    if (((DrinksPagerFragment) mContainerFragment).hasSaldo()) {
                        // deduct money and stop
                        ((DrinksPagerFragment) mContainerFragment).processTransaction(tempFoundAccount);
                        return;
                    }
                }
                /* Then open account setup drawer.
                     * select Section and open fragment in container. This is nice quickAccess
                     */
                //return;
                /**
                 * else: top-up and present Name and Current Money
                 */
            }
            FragmentManager fm = getFragmentManager();
            AccountSetupDialogFragment dialog;
            try {
                 dialog = AccountSetupDialogFragment.newInstance(tempFoundAccount);
            } catch (IllegalArgumentException e) {
                // No proper ID found
                Log.e(TAG,e+"");
                return;
            }
            dialog.show(fm, "AccountCreateDialog");
        }

    }

    private class NdefReaderTask extends AsyncTask<Tag, Void, String> {

        @Override
        protected String doInBackground(Tag... params) {
            Tag tag = params[0];

            Ndef ndef = Ndef.get(tag);
            if (ndef == null) {
                // NDEF is not supported by this Tag.

                Log.d(TAG,"No ndef tag");
                return null;
            }

            NdefMessage ndefMessage = ndef.getCachedNdefMessage();

            NdefRecord[] records = ndefMessage.getRecords();
            for (NdefRecord ndefRecord : records) {
                if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
                    try {
                        return readText(ndefRecord);
                    } catch (UnsupportedEncodingException e) {
                        Log.e(TAG, "Unsupported Encoding", e);
                    }
                }
            }

            return null;
        }

        private String readText(NdefRecord record) throws UnsupportedEncodingException {
			/*
			 * See NFC forum specification for "Text Record Type Definition" at 3.2.1
			 *
			 * http://www.nfc-forum.org/specs/
			 *
			 * bit_7 defines encoding
			 * bit_6 reserved for future use, must be 0
			 * bit_5..0 length of IANA language code
			 */

            byte[] payload = record.getPayload();

            // Get the Text Encoding
            String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";

            // Get the Language Code
            int languageCodeLength = payload[0] & 0063;

            // String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
            // e.g. "en"

            // Get the Text
            return new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                makeToast("Read content: " + result);
            }
        }
    }


    // If want filtering, use following methods:
/**
 *   public static void setupForegroundDispatch(final Activity activity, NfcAdapter adapter) {
 *
 *		final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
 *		intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
 *		final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);
 *		IntentFilter[] filters = new IntentFilter[1];
 *		String[][] techList = new String[][]{};
 *		// Notice that this is the same filter as in our manifest.
 *		filters[0] = new IntentFilter();
 *		filters[0].addAction(NfcAdapter.ACTION_TECH_DISCOVERED); //or NDEF
 *		filters[0].addCategory(Intent.CATEGORY_DEFAULT);
 *		try {
 *			filters[0].addDataType(MIME_TEXT_PLAIN);
 *		} catch (MalformedMimeTypeException e) {
 *			throw new RuntimeException("Check your mime type.");
 *		}
 *		adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
 *	}
 *	/**
 *	 * @param activity The corresponding {@link com.pemGP.puboardsteward.NavDrawActivity} requesting to stop the foreground dispatch.
 *	 * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
 *	 /
 *
 *  public static void stopForegroundDispatch(final Activity activity, NfcAdapter adapter) {
 *      adapter.disableForegroundDispatch(activity);
 *  }
 *
 *
 *	@Override
 *	protected void onResume()
 *	{
 *		super.onResume();
 *		/**
 *		* It's important, that the activity is in the foreground (resumed). Otherwise
 *		* an IllegalStateException is thrown.
 *		/
 *  setupForegroundDispatch(this, mNfcAdapter);
 *  }
 *
 *  @Override
 *  protected void onPause()
 *  {
 *      /**
 *       * Call this before onPause, otherwise an IllegalArgumentException is thrown as well.
 *       /
 *      stopForegroundDispatch(this, mNfcAdapter);
 *      super.onPause();
 *  }
 *
 */


    // Fragment interactions

    @Override
    public void onDrinksFragmentInteraction(Drink d) {
        // TODO: need to implement reaction to selection of a drink??
    }

    @Override
    public void onAccountListFragmentInteraction(String id) {
        //TODO: need to implement reaction to selection of account?
    }

    @Override
    public void onDrinkListFragmentInteraction(int id){
        // TODO:
    }

    @Override
    public void onDrinkStoreUpdated() {
        // TODO: anything to implement?
    }

    // From here: Authentication stuff

    @Override
    protected void onResume() {
        super.onResume();
        if (checkPlayServices() && checkUserAccount()) {
            // Then we're good to go!
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mHandleMessageReceiver, new IntentFilter(DISPLAY_MESSAGE_ACTION));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mHandleMessageReceiver);
    }


    private boolean checkPlayServices() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (status != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(status)) {
                showErrorDialog(status);
            } else {
                makeToast("This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    private boolean checkUserAccount() {
        String accountName = AccountUtils.getAccountName(this);
        if (accountName == null) {
            // Then the user was not found in the SharedPreferences. Either the
            // application deliberately removed the account, or the application's
            // data has been forcefully erased.
            Log.i(TAG,"No account name stored");
            showAccountPicker();
            return false;
        }

        Log.i(TAG,"Stored account name: "+accountName);

        Account account = AccountUtils.getGoogleAccountByName(this, accountName);
        if (account == null) {
            Log.i(TAG,"Stored Account is not on device anymore. Choose a new one!");
            // Then the account has since been removed.
            AccountUtils.removeAccount(this);
            showAccountPicker();
            return false;
        }
        Log.i(TAG,"Account data: "+account);
        return true;
    }

    void showErrorDialog(int code) {
        GooglePlayServicesUtil.getErrorDialog(code, this,
                REQUEST_CODE_RECOVER_PLAY_SERVICES).show();
    }

    private void showAccountPicker() {
        // set to false such that an account is automatically picked if only one exists
        Intent pickAccountIntent = AccountPicker.newChooseAccountIntent(
                null, null, new String[] { GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE },
                false, null, null, null, null);
        startActivityForResult(pickAccountIntent, REQUEST_CODE_PICK_ACCOUNT);
    }

    @Override
    public void onAuthenticationError() {
        Log.i(TAG,"Started Authentication Procedure");
        showAccountPicker();
        // upon account selection, onActivityResult() will be called with requestCode = 1;

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_RECOVER_PLAY_SERVICES:
                if (resultCode == RESULT_CANCELED) {
                    makeToast("Google Play Services must be installed.");
                    finish();
                }
                return;
            case REQUEST_CODE_PICK_ACCOUNT:
                if (resultCode == RESULT_OK) {
                    String accountName = data.getStringExtra(
                            AccountManager.KEY_ACCOUNT_NAME);
                    AccountUtils.setAccountName(this, accountName);
                } else if (resultCode == RESULT_CANCELED) {
                    makeToast("This application requires a Google account.");
                    //finish();
                }
                return;
            case REQUEST_CODE_EDIT_SHEET:
                // update in any case
                DrinkStore.get(this).refreshDrinks(this);
                Log.i(TAG,"Google sheets request triggered refresh Drinks");
        }
        super.onActivityResult(requestCode, resultCode, data);

    }

    protected void makeToast(String s){
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    private final BroadcastReceiver mHandleMessageReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String newMessage = intent.getExtras().getString(EXTRA_MESSAGE);
                    // try to parse JSON
                    try {
                        JSONArray pushedData = new JSONArray(newMessage);
                        if (pushedData == null)
                            throw new JSONException("No data found in message");
                        AccountStore.get(context).processJSON(context, pushedData);
                        return;
                    } catch (JSONException e) {
                        // message was no JSON data object
                        // so update drinkslist
                        //TODO: remove this Log after debugging
                        Log.i(TAG+".mHandleMessageReceiver",newMessage);

                    }

                    // If notified from GCM: fetch new message
                    //AccountStore.get(context).refreshAccounts();
                    DrinkStore.get(context).refreshDrinks(context);
                }
            };


    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences().
                        registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences().
                        unregisterOnSharedPreferenceChangeListener(this);
        }


        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(CommonUtilities.KEY_SENDER_ID)||key.equals(CommonUtilities.KEY_JSON_FEED_ID) || key.equals(KEY_SPREADSHEET_ID)){
                CommonUtilities.loadScriptSettings(getActivity());
            }
        }
    }
}
