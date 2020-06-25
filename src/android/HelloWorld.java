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
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import io.electrosoft.helloworld.BatterySaver;

import static com.microsoft.appcenter.utils.HandlerUtils.runOnUiThread;

public class HelloWorld extends CordovaPlugin {

    /* MUST match the product code of the license otherwise it won't work */
    private final String PRODUCT_CODE = "62682"; // <--- replace value with the product code received from Avira
    private final String TAG = "MavapiBasicScan";

    final int numThreads = Runtime.getRuntime().availableProcessors();
    private io.electrosoft.helloworld.MavapiExecutor executor = new io.electrosoft.helloworld.MavapiExecutor(numThreads);

    private boolean run = false;

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

        Log.w(TAG, "Action: " + action);
        if(action.equals("getProcessing"))
        {
            this.executor.setWebviewContext(webView.getContext());
            callbackContext.success(executor.processData.process);
            return true;
        }

        if(action.equals("batterySaver")) {
            BatterySaver batterySaver = new BatterySaver(webView.getContext());
            callbackContext.success(batterySaver.getBatteryStatus());
        }

        if(action.equals("batterySaverMode")) {
            BatterySaver batterySaver = new BatterySaver(webView.getContext());
           // batterySaver.mode();
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


}
