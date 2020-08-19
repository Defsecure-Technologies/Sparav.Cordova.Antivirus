package io.electrosoft.helloworld;

import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.net.Uri;
import android.nfc.Tag;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

import com.avira.mavapi.MavapiCallbackData;
import com.avira.mavapi.MavapiException;
import com.avira.mavapi.MavapiScanner;

import org.apache.cordova.LOG;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import java.io.File;
import java.io.FileOutputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import android.os.Environment;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;
import android.content.Context;
import android.graphics.PixelFormat;
import java.util.List;


public class MavapiExecutor extends ThreadPoolExecutor implements ThreadFactory {
    private AtomicInteger INFECTED = new AtomicInteger(0);
    private final String TAG = "MavapiMultithreadedScan";
    private AtomicInteger CLEAN = new AtomicInteger(0);
    private AtomicInteger INCONCLUSIVE = new AtomicInteger(0);
    private Map<Long, io.electrosoft.helloworld.MyThread> threadMap = new HashMap<>();


    public io.electrosoft.helloworld.ProcessData processData = new io.electrosoft.helloworld.ProcessData();

    public int scannedApks = 0;

    // can be more simple, i think..
    HashMap<String, ApplicationInfo> packages = new HashMap<String, ApplicationInfo>();

    private JSONArray infectedFilesList = new JSONArray();

    private JSONArray scannedFilesList = new JSONArray();

    public Context webviewContext;

    public File appDirectory = null;

    private String path;

    public MavapiExecutor(int threads) {

        /* Avoid unwanted behaviour by NOT changing the corePoolSize and maxCorePoolSize  */
        super(threads, threads, 0L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        setThreadFactory(this);

    }

    public void setWebviewContext(Context context) {
        webviewContext = context;
        ContextWrapper cw = new ContextWrapper(webviewContext);
        appDirectory = cw.getDir("imageDir", Context.MODE_PRIVATE);

    }

    /*private void uninstallAPK(String apkPackageName) {
        String appPackage = "com.your.app.package";
        Intent intent = new Intent(getActivity(), getActivity().getClass());
        PendingIntent sender = PendingIntent.getActivity(getActivity(), 0, intent, 0);
        PackageInstaller mPackageInstaller = webviewContext.getActivity().getPackageManager().getPackageInstaller();
        mPackageInstaller.uninstall(appPackage, sender.getIntentSender());
    }*/

    // source: https://github.com/googleads/googleads-consent-sdk-android/issues/2
    private String getAppIconURIString(Drawable iconDrawable) {
        Bitmap bitmap = Bitmap.createBitmap(iconDrawable.getIntrinsicWidth(),
                iconDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        iconDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        iconDrawable.draw(canvas);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    @Override
    public Thread newThread(@NonNull Runnable r) {
        MavapiScanner scanner;
        try {
            scanner = new MavapiScanner();
            scanner.setScannerListener(new MavapiScanner.ScannerListener() {
                /* Method called to notify the end of object scan, regardless of the result of the scan */
                @Override
                public void onScanComplete(MavapiCallbackData mavapiCallbackData) {

                    ApplicationInfo application = packages.get(mavapiCallbackData.getFilePath());

                    // store scanned files.
                    // file object...
                    JSONObject f = new JSONObject();

                    try {
                        f.put("fileName", application.loadLabel(webviewContext.getPackageManager()).toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    try {
                        f.put("packageName", application.packageName);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    // app : defsecuretech.sparav.app
                    Drawable drawable = application.loadIcon(webviewContext.getPackageManager());

                    String imageEncoded = getAppIconURIString(drawable);

                    try {
                        f.put("icon", "data:image/png;base64," + imageEncoded);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    try {
                        f.put("shown", 0);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    scannedFilesList.put(f);


                    try {
                        processData.process.put("scannedFileList", scannedFilesList);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    // if malware..
                    if (mavapiCallbackData.getMalwareInfos() != null) {
                        INFECTED.incrementAndGet();

                        boolean deleted = mavapiCallbackData.getFile().delete();
                        LOG.w(TAG, "FILE: " + mavapiCallbackData.getFile() + ": " + deleted);

                        // file object...
                        JSONObject file = new JSONObject();

                      //  Log.w(TAG, "INFECTED FILE");

                        try {
                            file.put("fileName", application.loadLabel(webviewContext.getPackageManager()).toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                     //   Log.w(TAG, "Base64:");
                       // Log.w(TAG, imageEncoded);

                        try {
                            file.put("icon", "data:image/png;base64," + imageEncoded);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        try {
                            file.put("path", mavapiCallbackData.getFilePath());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        infectedFilesList.put(file);

                        try {
                            processData.process.put("infectedFiles", getInfectedFilesList());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    } else if (mavapiCallbackData.getErrorCode() != 0) {
                        INCONCLUSIVE.incrementAndGet();
                    } else {
                        CLEAN.incrementAndGet();
                    }


                    //Log.w(TAG, "gg " + scannedApks);
                    scannedApks++;

                    try {
                        double percentage = Math.round((scannedApks / (double) processData.totalApks) * 100);
                        processData.process.put("percentage", percentage);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    try {
                        processData.process.put("scannedFiles", scannedApks);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    try {
                        processData.process.put("ready", "1");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if(scannedApks == processData.totalApks) {
                        try {
                            processData.process.put("done", "1");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                   // Log.w(TAG, "for fuck sake.. " + processData.totalApks + " " + scannedApks);

                }

                /**
                 * Method called to report file scan errors such as: encrypted, max recursion, etc.
                 * Can be called multiple times depending on the content of the object scanned.
                 */
                @Override
                public void onScanError(MavapiCallbackData mavapiCallbackData) {

                    scannedApks++;
                    Log.e(TAG, "Scan error: " + mavapiCallbackData.getFilePath());
                    /* ... */
                }
            });
        } catch (MavapiException e) {
            scannedApks++;
            e.printStackTrace();
            return null;
        }

        io.electrosoft.helloworld.MyThread thread = new io.electrosoft.helloworld.MyThread(r, scanner);
        threadMap.put(thread.getId(), thread);
        return thread;
    }

    /* Called upon executor termination */
    @Override
    protected void terminated() {
        super.terminated();
        clearScanners();
    }

    /**
     * It is recommended to destroy the scanner instance
     * when finished scanning and before updating.
     */
    private void clearScanners() {
        for (io.electrosoft.helloworld.MyThread t : threadMap.values()) {
            t.getScanner().destroy();
        }
        threadMap.clear();
    }

    /* Get the current thread's scanner in order to start scanning */
    void doScanning(final String file) throws Exception {
        if (threadMap.containsKey(Thread.currentThread().getId())) {
            io.electrosoft.helloworld.MyThread t = threadMap.get(Thread.currentThread().getId());
            t.getScanner().scan(file);
        } else throw new Exception("Cannot scan file!");
    }

    /* Getters */
    int getINFECTED() {
        return INFECTED.get();
    }

    int getCLEAN() {
        return CLEAN.get();
    }

    int getINCONCLUSIVE() {
        return INCONCLUSIVE.get();
    }

    JSONArray getInfectedFilesList() {
        return infectedFilesList;
    }


}
