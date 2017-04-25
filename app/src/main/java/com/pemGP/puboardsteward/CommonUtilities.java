package com.pemGP.puboardsteward;

/**
 * Created by Florian on 09.08.2014.
 * From: https://github.com/entaq/GoogleAppsScript/blob/master/Android/ClientCode/src/com/google/android/gcm/demo/app/CommonUtilities.java
 */
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.logging.Logger;

/**
 * Helper class providing methods and constants common to other classes in the
 * app.
 */
public final class CommonUtilities {

    /**
     * Feed URL for the Drinks list and account list
     * also for GCM registration in Apps script
     * Main script in Spreadsheet
     * For schroederflorian:    https://script.google.com/macros/s/AKfycbyLOc6hXGXwk3LOknQ8buV27X2jyg2WBiMaJuZ1anImycmFO0Zc/exec
     *
     * TODO: enter your Script link and ID here!
     */
    static String JSON_FEED_URL = "https://script.google.com/macros/s/AKfycbyLOc6hXGXwk3LOknQ8buV27X2jyg2WBiMaJuZ1anImycmFO0Zc/exec";
    static String JSON_FEED_ID  = "AKfycbyLOc6hXGXwk3LOknQ8buV27X2jyg2WBiMaJuZ1anImycmFO0Zc";

    /**
     * Google API project number registered to use GCM.
     * This is the main PuboardSteward script
     * Needed to tell which Project this app is connected to
     * For schroederflorian:    390546722706
     *
     * TODO: enter your Google API project number here! Get it after registering project at console.developers.google.com
     *       spreadsheet script needs to be associated with that project.
     *       Necessary APIs: Google Cloud Messaging, Google Cloud Storage, Google Cloud Storage JSON API, Google Drive API
     */
    static String SENDER_ID = "390546722706";

    /**
     * ID of the Google Drive Spreadsheet holding all Drinks and Account information
     * For schroederflorian:    1sW2OQ2NMj1ILU0EWbd9YCxVT49vg_pTEuqC-eXSYVkU
     *
     * TODO: enter your spreadsheet id here
     *
     */
    static String SPREADSHEET_ID = "1sW2OQ2NMj1ILU0EWbd9YCxVT49vg_pTEuqC-eXSYVkU";
    static String SPREADSHEET_URL;

    /**
     * Secret for registering GCM device ID at apps script
     * TODO: create a long random string and put here!
     *       the same string has to be put into the Code.gs script in your Google Spreadsheet in the doPost method
     */
    static final String MY_SECRET = "ThisNeedsToBeChosenByYou!";

    /**
     * Intent used to display a message in the screen.
     */
    static final String DISPLAY_MESSAGE_ACTION =
            "com.pemGP.puboardsteward.DISPLAY_MESSAGE";

    /**
     * Intent's extra that contains the message to be displayed.
     */
    static final String EXTRA_MESSAGE = "message";

    /**
     * Shared Preferences Keys to store SENDER_ID and JSON_FEED_URL
     */
    static final String KEY_JSON_FEED_ID = "puboardsteward_json_feed_id";
    static final String KEY_SENDER_ID = "puboardsteward_sender_id";
    static final String KEY_SPREADSHEET_ID = "puboardsteward_spreadsheet_id";


    private static final String TAG = "PuboardSteward.CommonUtilities";
    /**
     * Notifies UI to display a message.
     * <p>
     * This method is defined in the common helper because it's used both by
     * the UI and the background service.
     *
     * @param context application's context.
     * @param message message to be displayed.
     */
    static void displayMessage(Context context, String message) {
        Intent intent = new Intent(DISPLAY_MESSAGE_ACTION);
        intent.putExtra(EXTRA_MESSAGE, message);
        context.sendBroadcast(intent);
    }

    static void loadScriptSettings(final Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        // keep old, if no stored key!
        JSON_FEED_ID = prefs.getString(KEY_JSON_FEED_ID,JSON_FEED_ID);
        JSON_FEED_URL = "https://script.google.com/macros/s/"+JSON_FEED_ID+"/exec";

        SENDER_ID = prefs.getString(KEY_SENDER_ID, SENDER_ID);

        SPREADSHEET_ID = prefs.getString(KEY_SPREADSHEET_ID,SPREADSHEET_ID);
        SPREADSHEET_URL = "https://docs.google.com/spreadsheets/d/" + SPREADSHEET_ID;

        Log.i(TAG,"Loaded Script Settings");
    }
}
