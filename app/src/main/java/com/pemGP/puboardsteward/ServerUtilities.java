package com.pemGP.puboardsteward;

/**
 * Created by Florian on 09.08.2014.
 */
import static com.pemGP.puboardsteward.CommonUtilities.SENDER_ID;
import static com.pemGP.puboardsteward.CommonUtilities.MY_SECRET;
import static com.pemGP.puboardsteward.CommonUtilities.JSON_FEED_URL;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableNotifiedException;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import javax.net.ssl.SSLPeerUnverifiedException;

/**
 * Helper class used to communicate with the demo server.
 */
class ServerUtilities {

    private static final int MAX_ATTEMPTS = 2;
    private static final int BACKOFF_MILLI_SECONDS = 2000;
    private static final String KEY_SERVER_REGISTRATION="is_registered_on_server";
    private static final String KEY_REGID = "puboardsteward_regid";
    private static final Random random = new Random();
    private static final String TAG = "PuboardSteward.ServerUtilities";

    private static Boolean isRegistered = null;
    private static String regid = null;

    Context context;
    private static List<Map<String, String>> pendingTransactions = null;
    /**
     * Register this account/device pair within the server.
     *
     * @return whether the registration succeeded or not.
     */

    ServerUtilities(Context ctx){
        context = ctx;
    }

    boolean register() {
        // used in onCreate to register for GCM and at gcm script server
        //checkNotNull(context, SERVER_URL, "SERVER_URL");
        checkNotNull(context, SENDER_ID, "SENDER_ID");
        checkNotNull(context, JSON_FEED_URL, "SERVER_URL");

        regid = getStoredRegId(context);
        if (regid == null) {
            // Starts new task!
            // This registers on server, sets registered = true and stores regId to SharedPreferences.
            new RegisterInBackground().execute(true);
            return true;    // means is going to register
        }
        // else: if has regid && registered on server
        if (isRegisteredOnServer(context)) {
            Log.i(TAG+".register", "already registered on server");
            return true;
        } else {
            Log.i(TAG+".register", "start RegisterInBackground");
            new RegisterInBackground().execute(true);
        }
        return true;    // means is going to register
    }

    /**
     * Unregister this account/device pair within the gcm script server.
     */
    void unregister(final Context context) {
        // get ID from storage
        if (regid == null)
            getStoredRegId(context);
        if (!isRegisteredOnServer(context)) {
            return;
        }
        new RegisterInBackground().execute(false);

    }

    private static DefaultHttpClient getDefaultHttpClient(){
        if (hc == null)
            return new DefaultHttpClient();
        else
            return hc;
    }

    static JSONArray getFromSpreadsheet(Context activity, String sheetType) throws IOException, SSLPeerUnverifiedException {
        // sheetType = {"drinks", "accounts"}
        DefaultHttpClient httpClient = getDefaultHttpClient();
        // Turn account name into a token, which must
        // be done in a background task, as it contacts
        // the network.
        String token = getAccessToken(activity);
        Log.i(TAG+".getFromSpreadsheet","Token acquired: "+token);
        // appending access token solves authentication problem!
        // Use of token taken from:
        // http://developer.android.com/google/play-services/auth.html#obtain
        // need fields: type=fetch and sheet= {drinks, accounts}
        HttpGet httpGet = new HttpGet(JSON_FEED_URL+"?access_token="+token+"&type=fetch&sheet="+sheetType);
        HttpResponse response = httpClient.execute(httpGet);
        if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            String result = EntityUtils.toString(response.getEntity());
            //Log.i(TAG,result);
            JSONArray objects;
            try {
                objects = new JSONArray(result);
            } catch (JSONException e) {
                // if redirected to authentication page. Start Account choose dialog fragment
                // after choice is made, this task will be restarted.
                Log.e(TAG+".getFromSpreadsheet", "Could not authenticate");
                // stop updating procedure
                return null;
            }
            return objects;
        }
        return null;
    }

    private static String getAccessToken(Context activity){
        String token = null;
        try {
            token =
                    GoogleAuthUtil.getTokenWithNotification(
                            activity,
                            AccountUtils.getAccountName(activity),
                            "oauth2:https://spreadsheets.google.com/feeds https://docs.google.com/feeds https://www.googleapis.com/auth/drive",
                            new Bundle());
        } catch (UserRecoverableNotifiedException userNotifiedException) {
            // Notification has already been pushed.
            // Continue without token or stop background task.
        } catch (GoogleAuthException authEx) {
            // This is likely unrecoverable.
            Log.e(TAG+".getAccessToken", "Unrecoverable authentication exception: " + authEx.getMessage(), authEx);
        } catch (IOException ioEx) {
            Log.i(TAG+".getAccessToken", "transient error encountered: " + ioEx.getMessage());
            //doExponentialBackoff();
            return null;
        }
        return token;
    }

    /**
     * Issue a POST request to the server.
     *
     * @param endpoint POST address.
     * @param params request parameters.
     *
     * @throws IOException propagated from POST.
     * DEPRECATED, use the other post method.
     */
    private static void post(String endpoint, Map<String, String> params)
            throws IOException {
        URL url;
        try {
            url = new URL(endpoint);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("invalid url: " + endpoint);
        }
        StringBuilder bodyBuilder = new StringBuilder();
        Iterator<Entry<String, String>> iterator = params.entrySet().iterator();
        // constructs the POST body using the parameters
        while (iterator.hasNext()) {
            Entry<String, String> param = iterator.next();
            bodyBuilder.append(param.getKey()).append('=')
                    .append(param.getValue());
            if (iterator.hasNext()) {
                bodyBuilder.append('&');
            }
        }
        String body = bodyBuilder.toString();
        Log.v(TAG, "Posting '" + body + "' to " + url);
        byte[] bytes = body.getBytes();
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setFixedLengthStreamingMode(bytes.length);
            conn.setRequestMethod("POST");
            //conn.setRequestProperty("Authorization","Bearer "+getAccessToken(context));
            conn.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded;charset=UTF-8");
            // post the request
            OutputStream out = conn.getOutputStream();
            out.write(bytes);
            out.close();
            // handle the response
            int status = conn.getResponseCode();
            if (status != 200) {
                throw new IOException("Post failed with error code " + status);
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static boolean post(Context context, String endpoint, List<NameValuePair> nameValuePairs)
            throws IOException{
        // shall handle authorization
        // Switching to HttpURLConnection is encouraged and saves battery?
        // use for OAuth2: HttpURLConnection.setRequestProperty("Authorization","Bearer "+getAccessToken(context));
        // TODO: need closable response and client
        DefaultHttpClient httpClient = getDefaultHttpClient();
        HttpPost httpPost = new HttpPost(endpoint);
        //httpPost.setHeader("Content-Type","application/x-www-form-urlencoded;charset=UTF-8");     DEPRECATED
        httpPost.setHeader("Authorization", "Bearer "+getAccessToken(context));             // using OAuth2.0
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));
            Log.i(TAG+".post","sending HttpPost: "+EntityUtils.toString(httpPost.getEntity()));
            // Execute HTTP Post Request
            HttpResponse response = httpClient.execute(httpPost);
            String responseBody = EntityUtils.toString(response.getEntity());
            if (response.getStatusLine().getStatusCode() != 200 || responseBody.equals("ERROR")) {
                Log.i(TAG + ".post", "received error in HttpResponse: " + responseBody + "\n with code: " + response.getStatusLine().getStatusCode());
                return false;
            } else {
                Log.i(TAG + ".post", "posted successfully with: "+ responseBody+ "\n with code: " + response.getStatusLine().getStatusCode() );
                return true;
            }
        } catch (ClientProtocolException e) {
            Log.e(TAG+".post",e+"");
            return false;
        } catch (UnsupportedEncodingException e){
            Log.e(TAG+".post",e+"");
            return false;
        }
    }

    static String getStoredRegId(Context context){
        // Try to load from shared preferences
        if (regid != null)
            return regid;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        regid = prefs.getString(KEY_REGID,null);
        return regid;
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private static void storeRegistrationId(Context context, String regid){
        // sets current state to sharedPreferences
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString(KEY_REGID, regid);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            editor.apply();
        } else {
            editor.commit();
        }
    }

    private static boolean isRegisteredOnServer(Context ctx) {
        // loads info from sharedPreferences
        if (isRegistered!= null) {
            return isRegistered;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getBoolean(KEY_SERVER_REGISTRATION, false);
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private static void setRegisteredOnServer(Context ctx, boolean state) {
        // sets current state to sharedPreferences
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(ctx).edit();
        editor.putBoolean(KEY_SERVER_REGISTRATION, state);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            editor.apply();
        } else {
            editor.commit();
        }
        isRegistered = state;
    }

    private static void checkNotNull(Context context, Object reference, String name) {
        if (reference == null) {
            throw new NullPointerException(context.getString(R.string.error_config, name));
        }
    }

    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration ID and app versionCode in the application's
     * shared preferences.
     */
    class RegisterInBackground extends AsyncTask<Boolean,Void,String> {
        //private static final String TAG = "PuboardSteward.ServerUtilities.RegisterInBackground";
        @Override
        protected String doInBackground(Boolean... mode) {
            /**
             * if mode == true, then register.
             *    mode == false, then unregister
             */
            String msg = "";
            try {
                if (regid == null) {
                    GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
                    regid = gcm.register(SENDER_ID);
                    msg = "Device registered for SenderID=" +SENDER_ID+", registration ID=" + regid;

                    // Persist the regID - no need to register again.
                    Log.i(TAG+".RegisterInBackground", "Device registered with GCM, storing registration ID");
                    storeRegistrationId(context, regid);
                }
                // You should send the registration ID to your server over HTTP,
                // so it can use GCM/HTTP or CCS to send messages to your app.
                // The request to your server should be authenticated if your app
                // is using accounts.

                Log.i(TAG+".RegisterInBackground","start sending to Backend");
                sendRegistrationIdToBackend(context, mode[0]);

                // For this demo: we don't need to send it because the device
                // will send upstream messages to a server that echo back the
                // message using the 'from' address in the message.

                if (!mode[0]) {
                    // erase regid to allow new registration!
                    regid = null;
                    storeRegistrationId(context, regid);
                }

                msg += "no error";
            } catch (IOException ex) {
                Log.e(TAG+".RegisterInBackground",ex.getMessage());
                msg = "Error :" + ex.getMessage();
                // If there is an error, don't just keep trying to register.
                // Require the user to click a button again, or perform
                // exponential back-off.
            }
            return msg;
        }

        @Override
        protected void onPostExecute(String msg) {
            Log.i(TAG+".RegisterInBackground","Finished with: "+msg);
        }

    }

    private static boolean sendRegistrationIdToBackend(Context context, boolean register) {
        // register on server if register == true
        // else: unregister
        if (register)
            Log.i(TAG+".sendRegistrationIdToBackend", "registering device on server (regId = " + regid + ")");
        else
            Log.i(TAG+".sendRegistrationIdToBackend", "unregistering device on server (regId = " + regid + ")");

        String serverUrl = JSON_FEED_URL;// + "/register";

        /**
         * for old post() using HTTPconnection
         * Map<String, String> params = new HashMap<String, String>();
         * params.put("regId", regid);
         * params.put("type", "register");
         * params.put("mysecret",MY_SECRET");
         */

        // Add your data for HttpClient
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
        nameValuePairs.add(new BasicNameValuePair("mysecret",MY_SECRET));
        nameValuePairs.add(new BasicNameValuePair(context.getString(R.string.transaction_regid), regid));
        if (register)
            nameValuePairs.add(new BasicNameValuePair("type", "register"));
        else
            nameValuePairs.add(new BasicNameValuePair("type", "unregister"));


        long backoff = BACKOFF_MILLI_SECONDS + random.nextInt(1000);
        // Once GCM returns a registration id, we need to register it in the
        // demo server. As the server might be down, we will retry it a couple
        // times.
        // only needed if authenticating: String token = getAccessToken(context);
        for (int i = 1; i <= MAX_ATTEMPTS; i++) {
            Log.d(TAG, "Attempt #" + i + " to (un)register");
            try {
                // post(serverUrl, params);
                if (!post(context, serverUrl,nameValuePairs))
                    throw new IOException("POST method returned with false");

                String message = "";
                if (register) {
                    setRegisteredOnServer(context, true);
                    message = context.getString(R.string.server_registered);
                } else {
                    setRegisteredOnServer(context, false);
                    message = context.getString(R.string.server_unregistered);
                }
                CommonUtilities.displayMessage(context, message);
                return true;
            } catch (IOException e) {
                // Here we are simplifying and retrying on any error; in a real
                // application, it should retry only on unrecoverable errors
                // (like HTTP error code 503).
                Log.e(TAG, "Failed to (un)register on attempt " + i, e);
                if (i == MAX_ATTEMPTS) {
                    break;
                }
                try {
                    Log.d(TAG, "Sleeping for " + backoff + " ms before retry");
                    Thread.sleep(backoff);
                } catch (InterruptedException e1) {
                    // Activity finished before we complete - exit.
                    Log.d(TAG, "Thread interrupted: abort remaining retries!");
                    Thread.currentThread().interrupt();
                    return false;
                }
                // increase backoff exponentially
                backoff *= 2;
            }
        }
        String message = "";
        if (register)
            message = context.getString(R.string.server_register_error, MAX_ATTEMPTS);
        else
            message = context.getString(R.string.server_unregister_error,null);
        CommonUtilities.displayMessage(context, message);
        return false;
    }

    /**
     *
     * @param info: contains information to be pushed into the accounts table.
     * @return
     */
    static void sendTransactionToBackend(final Context context, Map<String,String> info){
        new AsyncTask<Map<String,String>, Void, Map<String,String>>() {
            @Override
            protected Map<String,String> doInBackground(Map<String, String>... maps) {
                // construct Data stream
                Iterator<Entry<String, String>> iterator = maps[0].entrySet().iterator();

                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(maps[0].size()+3);
                nameValuePairs.add(new BasicNameValuePair("mysecret",MY_SECRET));
                nameValuePairs.add(new BasicNameValuePair(context.getString(R.string.transaction_regid), regid));
                nameValuePairs.add(new BasicNameValuePair(context.getString(R.string.transaction_type), context.getString(R.string.transaction_type_addtransaction)));
                while (iterator.hasNext()) {
                    Entry<String, String> param = iterator.next();
                    nameValuePairs.add(new BasicNameValuePair(param.getKey(), param.getValue()));
                }
                try {
                    if (!post(context, JSON_FEED_URL, nameValuePairs)){
                        // remember the transaction and post it later!
                        return maps[0];
                    }
                    Log.i(TAG,"Transaction sent successfully");
                    return null;
                } catch (IOException e){
                    Log.e(TAG+".sendTransactionToBackend", "Failed pushing Transaction to server");
                    return maps[0];
                }
            }

            @Override
            protected void onPostExecute(Map<String, String> stringStringMap) {
                super.onPostExecute(stringStringMap);
                if (stringStringMap == null)
                    return;
                // else store in array. and start another worker thread.
                // TODO: start a worker thread. And make fail safe
                //pendingTransactions.add(stringStringMap);
                Toast.makeText(context, "Transaction failed. Please repeat!", Toast.LENGTH_LONG).show();
            }
        }.execute(info);

    }
    static void undoLastTransaction(final Context context, final DrinksPagerFragment listener) {
        new AsyncTask<Void, Void, Boolean>(){
            @Override
            protected Boolean doInBackground(Void... voids) {
                // construct Data stream
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("mysecret", MY_SECRET));
                nameValuePairs.add(new BasicNameValuePair(context.getString(R.string.transaction_regid), regid));
                nameValuePairs.add(new BasicNameValuePair(context.getString(R.string.transaction_type), context.getString(R.string.transaction_type_undolasttransaction)));
                try {
                    if (!post(context, JSON_FEED_URL, nameValuePairs)){
                        // Undo failed
                        return false;
                    }
                    Log.i(TAG,"Last Transaction undone successfully");
                    return true;
                } catch (IOException e){
                    Log.e(TAG+".undoLastTransaction", "Failed pushing undoing last Transaction");
                    return false;
                }

            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                super.onPostExecute(aBoolean);
                if (!aBoolean) {
                    listener.displayConfirmTransaction(DrinksPagerFragment.TransactionState.UNDO_ERROR,0,null);
                }
//                else {
//                    // unnecessary since slower than response!
//                    listener.displayConfirmTransaction(DrinksPagerFragment.TransactionState.UNDO_COMPLETE,0,null);
//                }
            }
        }.execute();
    }
}