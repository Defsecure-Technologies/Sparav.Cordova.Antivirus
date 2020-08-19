package io.electrosoft.helloworld;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;


import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.avira.mavapi.Mavapi;
import com.avira.mavapi.MavapiConfig;
import com.avira.mavapi.Updater;
import com.avira.mavapi.UpdaterResult;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


import de.blinkt.openvpn.LaunchVPN2;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.Connection;
import de.blinkt.openvpn.core.ProfileManager;
import defsecuretech.sparav.app.MainActivity;
import io.electrosoft.helloworld.BatterySaver;


public class HelloWorld extends CordovaPlugin {

    /* MUST match the product code of the license otherwise it won't work */
    private final String PRODUCT_CODE = "62682"; // <--- replace value with the product code received from Avira
    private final String TAG = "MavapiBasicScan";

    final int numThreads = Runtime.getRuntime().availableProcessors();
    private io.electrosoft.helloworld.MavapiExecutor executor = new io.electrosoft.helloworld.MavapiExecutor(numThreads);

    private boolean run = false;

    private String  certificateUrl = "http://sparavvpnapiprod.azurewebsites.net/api/v1/certificate";
    private String profileUUID = "0f416e52-c10a-11ea-b3de-0242ac130004"; // DO NOT CHANGE THIS!


    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        if(action.equals("nativeToast")){
           /* for(int i = 0; args.getJSONArray(0).length() > i; i++) {
                Log.w(TAG, "Test " + args.getJSONArray(0).get(i));
            }*/
            this.executor.setWebviewContext(webView.getContext());
            int numThreads = Runtime.getRuntime().availableProcessors();
            executor = new io.electrosoft.helloworld.MavapiExecutor(numThreads);
            this.executor.scannedApks = 0;
            this.executor.processData = new io.electrosoft.helloworld.ProcessData();
            this.run = false;
            nativeToast();
        }

        if(action.equals("startVpn")) {
            String host = args.getJSONArray(0).get(0).toString();
            String port = args.getJSONArray(0).get(1).toString();
            String license_username = args.getJSONArray(0).get(2).toString();
            String license_password = args.getJSONArray(0).get(3).toString();

            Log.w("DILDO3", license_username);

            connectToVpn(host, port,license_username, license_password);
        }

        if(action.equals("disconnectVpn")) {
            MainActivity.management.stopVPN(false);
        }

        Log.w(TAG, "Action: " + action);
        if(action.equals("getProcessing"))
        {
            this.executor.setWebviewContext(webView.getContext());
            callbackContext.success(executor.processData.process);
            return true;
        }

        if(action.equals("batterySaver")) {
            BatterySaver batterySaver = new BatterySaver(webView.getContext());
            batterySaver.batteryStatus();
            callbackContext.success(batterySaver.getBatteryStatus());
        }

        if(action.equals("batterySaverMode")) {
            BatterySaver batterySaver = new BatterySaver(webView.getContext());
            //batterySaver.mode();
        }

        // start scan again..
        if(action.equals("startAgain")) {
            this.executor.setWebviewContext(webView.getContext());
            int numThreads = Runtime.getRuntime().availableProcessors();
            executor = new io.electrosoft.helloworld.MavapiExecutor(numThreads);
            nativeToast();
        }

        return false;
    }

    public boolean resolve() {
        return true;
    }

    public void nativeToast()
    {
        //mavapiRun(this);
        if(!run)
        {
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        basicScan(webView.getContext());
                    } catch(JSONException j) {

                    }
                }
            });
            run = true;
        }
    }


    private void basicScan(Context context) throws JSONException {
        /* Set configurations */
        MavapiConfig config = new MavapiConfig.Builder(context)
                .setDetectAdspy(true)
                .setDetectAdware(true)
                .setDetectAppl(true)
                .setDetectPfs(true)
                .setDetectPua(true)
                .setDetectSpr(true)
                .setProductCode(PRODUCT_CODE)
                .build();

        /**
         * Initialize Mavapi globally
         * During runtime, it is recommended to set the configuration and call Mavapi initialize only once.
         * Calling multiple times should not affect the process, but using a different configuration might cause unknown behavior.
         */
        if (!Mavapi.initialize(context, config)) {
            Log.w(TAG, "not inited..");
            return;
        }

        /* Update virus definitions files */
        UpdaterResult result = new Updater().download();

        executor.processData.process.put("ready", "0");
        executor.processData.process.put("done", "0");

        /* Check if it's at the latest version */
        if (result != UpdaterResult.UP_TO_DATE && result != UpdaterResult.DONE) {
            Log.w(TAG, "update thing?!?");
           // return;
        }

        Log.w(TAG, "UPDATE DONE!");

        /**
         * MavapiExecutor creates threads, assigning a scanner instance for each and every thread,
         * handles tasks and takes care of the scanning process overall.
         *
         * @note: One cannot reuse the same executor for further usage
         *        (e.g. using the same executor after doing an update)
         *        and it is mandatory to create another executor.
         */
        /* Get a list of installed apps */
        final List<ApplicationInfo> packages =
                context.getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);

        for (final ApplicationInfo packageInfo : packages) {
            boolean isSystemApp = ((packageInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
            if (isSystemApp) {
                continue;
            }
            executor.processData.totalApks++;
        }

        for (final ApplicationInfo packageInfo : packages) {
            boolean isSystemApp = ((packageInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
            if(isSystemApp) {
                continue;
            }

            Log.w(TAG, "scanning: " + packageInfo.packageName);

            /* Create tasks for each file to be scanned */
            io.electrosoft.helloworld.ScanTask task = new io.electrosoft.helloworld.ScanTask(packageInfo.sourceDir);
            executor.packages.put(packageInfo.sourceDir, packageInfo); // store info about package in a hashmap...

            /* Each task is linked to a scan executor which handles them */
            task.setScanExecutor(executor);

            /* Send task to executor and begin the actual scanning process */
            executor.execute(task);
        }

        executor.shutdown();

        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public void connectToVpn(String host, String port, String device_id, String password) {
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(MainActivity.getContext());
        // Request a string response from the provided URL.
        JsonObjectRequest stringRequest = new JsonObjectRequest(Request.Method.GET, certificateUrl, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // Display the first 500 characters of the response string.
                        try {
                            // update profile with latest data..
                            createProfile(response, host, device_id, password);
                            // .. and then connect to VPN!
                            LaunchVPN2 launchVPN2 = new LaunchVPN2();
                            launchVPN2.UUID = profileUUID;
                            launchVPN2.startVpnFromIntent();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.w("DILDO", error.getMessage());
            }
        });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    public void createProfile(JSONObject response, String host, String device_id, String password) throws JSONException {
        Connection connection = new Connection();
        connection.mServerName = host;
        connection.mServerPort =  "443";
        connection.mUseUdp = false;
        connection.mCustomConfiguration = "";
        connection.mEnabled = true;
        connection.mConnectTimeout = 0;
        connection.mUseProxyAuth = false;
        connection.mProxyAuthPassword = null;
        Connection[] connections = new Connection[] { connection };

        VpnProfile profile1 = new VpnProfile("Sparav");
        profile1.mConnections[0].mServerName = host;
        profile1.mUsername =  device_id;
        profile1.mPassword = password;
        profile1.mAlias  = "Sparav";
        profile1.mClientCertFilename = null;
        profile1.mTLSAuthDirection  = response.getString("mTLSAuthDirection");
        profile1.mTLSAuthFilename = response.getString("mTLSAuthFilename");
        profile1.mCaFilename = response.getString("mCaFilename");
        profile1.mCustomConfigOptions = response.getString("mCustomConfigOptions");
        profile1.mPrivateKey = null;

        profile1.mClientKeyFilename  = null;
        profile1.mPKCS12Filename = "tls-auth";
        profile1.mPKCS12Password = null;
        profile1.mUseTLSAuth = true;
        profile1.mDNS1 = "8.8.8.8";
        profile1.mDNS2 = "8.8.4.4";
        profile1.mIPv4Address  = null;
        profile1.mIPv6Address = null;
        profile1.mOverrideDNS = false;
        profile1.mSearchDomain = "blinkt.de";
        profile1.mUseDefaultRoute = false;
        profile1.mUsePull = true;
        profile1.mCustomRoutes = null;
        profile1.mCheckRemoteCN = false;
        profile1.mExpectTLSCert = false;
        profile1.mRemoteCN = "";
        profile1.mRoutenopull = false;
        profile1.mUseRandomHostname = false;
        profile1.mUseFloat = false;
        profile1.mUseCustomConfig = true;
        profile1.mVerb = "1";
        profile1.mCipher = response.getString("mCipher");
        profile1.mNobind = false;
        profile1.mUseDefaultRoutev6 = false;
        profile1.mCustomRoutesv6 = "";
        profile1.mKeyPassword = "";
        profile1.mConnectRetryMax = "-1";
        profile1.mConnectRetry = "2";
        profile1.mConnectRetryMaxTime = "300";
        profile1.mUserEditable = true;
        profile1.mAuth = response.getString("mAuth");
        profile1.mx509UsernameField = null;
        profile1.mAllowLocalLAN = true;
        profile1.mExcludedRoutes = null;
        profile1.mExcludedRoutesv6 =  null;
        profile1.mMssFix = 0;
        profile1.mRemoteRandom = false;
        profile1.mAllowedAppsVpnAreDisallowed = true;
        profile1.mAllowAppVpnBypass = false;
        profile1.mCrlFilename = null;
        profile1.mProfileCreator = "";
        profile1.mExternalAuthenticator = "";
        profile1.mTunMtu = 0;
        profile1.mPushPeerInfo = false;
        profile1.mServerName = connections[0].mServerName;
        profile1.mServerPort = connections[0].mServerPort;
        profile1.mUseUdp = false;
        profile1.mTemporaryProfile = false;
        profile1.mAuthenticationType = 3;
        profile1.mVersion = 13;
        profile1.mProfileCreator = null;
        profile1.importedProfileHash = null;
        profile1.mExternalAuthenticator = null;
        profile1.mCipher = response.getString("mCipher");
        profile1.mTLSAuthDirection =  "1";
        profile1.mPKCS12Filename = null;
        profile1.mUseLzo = false;

        profile1.mBlockUnusedAddressFamilies = true;
        profile1.mProfileVersion = 8;
        profile1.mAuthRetry = 0;
        profile1.mX509AuthType = 3;

        profile1.mPersistTun = false;
        profile1.mRemoteCN = "";
        profile1.mTLSAuthDirection = "1";
        profile1.mDNS1 = "8.8.8.8";
        profile1.mConnections = connections;
        profile1.setUUID(UUID.fromString(profileUUID)); // unique identifer for the profile.

        ProfileManager.getInstance(MainActivity.getContext()).removeProfile(MainActivity.getContext(), profile1);
        ProfileManager.getInstance(MainActivity.getContext()).addProfile(profile1);
        ProfileManager.getInstance(MainActivity.getContext()).saveProfile(MainActivity.getContext(), profile1);
    }

}
