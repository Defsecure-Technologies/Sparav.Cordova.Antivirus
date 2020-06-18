package io.electrosoft.helloworld;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.avira.mavapi.BuildConfig;
import com.avira.mavapi.Mavapi;
import com.avira.mavapi.MavapiCallbackData;
import com.avira.mavapi.MavapiConfig;
import com.avira.mavapi.MavapiException;
import com.avira.mavapi.MavapiMalwareInfo;
import com.avira.mavapi.MavapiScanner;
import com.avira.mavapi.Updater;
import com.avira.mavapi.UpdaterResult;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.microsoft.appcenter.utils.HandlerUtils.runOnUiThread;

public class HelloWorld extends CordovaPlugin {

    /* MUST match the product code of the license otherwise it won't work */
    private final String PRODUCT_CODE = "62682"; // <--- replace value with the product code received from Avira
    private final String TAG = "MavapiBasicScan";

    private int CLEAN = 0;
    private int INFECTED = 0;
    private int INCONCLUSIVE = 0;

    private String text = "lolgs";

    // chosen deleted files..
    private String[] pathFiles;

    private boolean run = false;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if(action.equals("nativeToast")){


            for(int i = 0; args.getJSONArray(0).length() > i; i++) {
                Log.w(TAG, "Test " + args.getJSONArray(0).get(i));
            }

            // nativeToast(args.get(1).toString());
        }

        Log.w(TAG, "Action: " + action);



        if(action.equals("getProcessing")) {
            Log.w(TAG, "Be the one!");
            //getProcessing();
            //callbackContext.success("great job dude!");
            return true;

        }

        return false;
    }

    public boolean resolve() {
        return true;
    }

    public void getProcessing() {

    }

    public void nativeToast(String test)
    {
        //mavapiRun(this);
        Toast.makeText(webView.getContext(),  text, Toast.LENGTH_SHORT).show();
        if(!run)
        {
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    basicScan(webView.getContext());
                }
            });
            run = true;
        }
    }


    private void basicScan(Context context) {
        final int[] numInconclusive = { 0 };
        final int[] numClean = { 0 };
        final int[] numInfected = { 0 };

        setInfoMessage("Initializing . . .", false);
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
            setInfoMessage("Failed initializing MAVAPI", true);
            return;
        }

        /* Update virus definitions files */
        UpdaterResult result = new Updater().download();

        /* Check if it's at the latest version */
        if (result != UpdaterResult.UP_TO_DATE && result != UpdaterResult.DONE) {
            setInfoMessage("Failed to update the virus database", true);
            return;
        }

        MavapiScanner scanner;
        try {
            /* Initialize scanner */
            scanner = new MavapiScanner();
            scanner.setScannerListener(new MavapiScanner.ScannerListener() {
                /* Method called to notify the end of object scan, regardless of the result of the scan */
                @Override
                public void onScanComplete(MavapiCallbackData mavapiCallbackData) {
                    if (mavapiCallbackData.getMalwareInfos() != null) {
                        /* Malware found */
                        numInfected[0]++;
                        /* Log results */
                        Log.w(TAG, String.format("Local " + result + " .. scan result for '%s': Infected:",  mavapiCallbackData.getFilePath()));

                        boolean deleted = mavapiCallbackData.getFile().delete();

                        for (MavapiMalwareInfo info : mavapiCallbackData.getMalwareInfos())
                            Log.w(TAG, String.format("\t- %s (%s): %s\n", info.getName(), info.getType(),  info.getMessage()));
                    } else if (mavapiCallbackData.getErrorCode() != 0) {
                        /* Error while scanning */
                        numInconclusive[0]++;
                        /* Log results */
                        Log.i(TAG, String.format("Local scan result for '%s': Could not scan file completely",  mavapiCallbackData.getFilePath()));
                    } else {
                        /* Clean */
                        numClean[0]++;
                        /* Log results */
                        Log.i(TAG, String.format("Local scan result for '%s': Clean",  mavapiCallbackData.getFilePath()));
                    }
                }

                /**
                 * Method called to report file scan errors such as: encrypted, max recursion, etc.
                 * Can be called multiple times depending on the content of the object scanned.
                 */
                @Override
                public void onScanError(MavapiCallbackData mavapiCallbackData) {
                    Log.e(TAG, "Scan error: " + mavapiCallbackData.getFilePath());
                    /* ... */
                }
            });
        } catch (MavapiException e) {
            setInfoMessage("Initialization failed", true);
            return;
        }

        setInfoMessage("Scanning in progress . . .", false);

        int scannedAPKs = 0;
        long start = (new Date()).getTime();

        /* Get a list of installed apps */
        List<ApplicationInfo> packages =  context.getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo packageInfo : packages) {

            if(!packageInfo.packageName.equals("com.androidantivirus.testvirus"))
            {
                continue;
            }

            Log.w(packageInfo.packageName, packageInfo.packageName);

            /* Do scanning */
            scanner.scan(packageInfo.sourceDir);
            scannedAPKs++;
        }
        /**
         * It is recommended to destroy the scanner instance
         * when finished scanning and before updating.
         */
        scanner.destroy();

        final double timeLapsed = (1.0 * ((new Date()).getTime() - start) / 1000);

        String msg = String.format(
                Locale.US,
                "Scan complete: %d files in %.2f s\n\nClean = %d\nInfected = %d\nInconclusive = %d\n",
                scannedAPKs,
                timeLapsed,
                numClean[0],
                numInfected[0],
                numInconclusive[0]);
        setInfoMessage(msg, false);
    }


    /* Display informations about scanning process on UI */
    private void setInfoMessage(final String message, final boolean isError) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                text = message;
            }
        });

    }

}
