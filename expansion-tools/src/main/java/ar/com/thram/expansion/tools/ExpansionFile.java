package ar.com.thram.expansion.tools;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Messenger;
import android.util.DisplayMetrics;
import android.util.Log;

import com.android.vending.expansion.zipfile.APKExpansionSupport;
import com.android.vending.expansion.zipfile.ZipResourceFile;
import com.google.android.vending.expansion.downloader.DownloadProgressInfo;
import com.google.android.vending.expansion.downloader.DownloaderClientMarshaller;
import com.google.android.vending.expansion.downloader.DownloaderServiceMarshaller;
import com.google.android.vending.expansion.downloader.Helpers;
import com.google.android.vending.expansion.downloader.IDownloaderClient;
import com.google.android.vending.expansion.downloader.IDownloaderService;
import com.google.android.vending.expansion.downloader.IStub;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Thram on 29/12/15.
 */
public class ExpansionFile {
    //    private final static String EXP_PATH = "/Android/obb/";
    private final int patchVersion;
    private final int version;
    private final int size;
    private final boolean isMain;
    private Context context;
    private static List<XAPKFile> xAPKS = new ArrayList<>();
    private ZipResourceFile expansionFile = null;

    /**
     * This is a little helper class that demonstrates simple testing of an
     * Expansion APK file delivered by Market. You may not wish to hard-code
     * things such as file lengths into your executable... and you may wish to
     * turn this code off during application development.
     */
    private static class XAPKFile {
        public final boolean mIsMain;
        public final int mFileVersion;
        public final long mFileSize;

        XAPKFile(boolean isMain, int fileVersion, long fileSize) {
            mIsMain = isMain;
            mFileVersion = fileVersion;
            mFileSize = fileSize;
        }
    }

    public ExpansionFile(int version, int size) {
        this(version, size, true);
    }

    public ExpansionFile(int version, int size, boolean isMain) {
        this.version = version;
        this.size = size;
        this.isMain = isMain;
        patchVersion = 0;
    }

    public ExpansionFile(int version, int size, int patchVersion) {
        this.version = version;
        this.size = size;
        this.patchVersion = patchVersion;
        this.isMain = false;
    }


    public Downloader downloader(Activity activity) {
        this.context = activity.getApplicationContext();
        return new Downloader(activity, this.version, this.size, this.isMain);
    }


    public String getResourcePath(String fileName, String prefix) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        String path = "res/" + prefix;
        if (dm.densityDpi <= 0.75f) {
            path += "-ldpi";
        }
        if (dm.densityDpi > 0.75f && dm.densityDpi <= 1f) {
            path += "-mdpi";
        }
        if (dm.densityDpi > 1f && dm.densityDpi <= 1.5f) {
            path += "-hdpi";
        }
        if (dm.densityDpi > 1.5f && dm.densityDpi <= 2f) {
            path += "-xhdpi";
        }
        if (dm.densityDpi > 2f && dm.densityDpi <= 3f) {
            path += "-xxhdpi";
        }
        if (dm.densityDpi > 3f && dm.densityDpi <= 4f) {
            path += "-xxxhdpi";
        }

        return path + '/' + fileName;
    }

    public Bitmap getDrawableResource(String filePath) {
        return getBitmapResource("drawable", filePath);
    }

    public Bitmap getBitmapResource(String prefix, String filePath) {
        return getBitmap(getResourcePath(filePath, prefix));
    }

    public Bitmap getBitmap(String filePath) {
        InputStream is = getInputStream(filePath);
        return is != null ? BitmapFactory.decodeStream(is) : null;
    }

    public AssetFileDescriptor getAssetFileDescriptor(String assetPath) {
        return expansionFile.getAssetFileDescriptor(assetPath);
    }

    public InputStream getInputStream(String filePath) {
        try {
            return expansionFile.getInputStream(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public class Downloader implements IDownloaderClient {
        private IStub downloaderClientStub;
        private ProgressDialog progressDialog = null;
        private Activity activity;
        private static final String LOG_TAG = "Downloader";
        public String downloadingMessage = "Downloading...";
        public String completeMessage = "Download Complete!";
        public String errorTitle = "Error";
        public String errorMessage = "Download Failed! Please contact check if you have data available.";
        public String closeButton = "Close";
        // The shared path to all app expansion files

        private Runnable onProgress = null;
        private Runnable onError = null;
        private Runnable onComplete = null;
        public boolean showAlertError = true;
        public boolean downloaded = false;

        public Downloader(Activity a, int version, int size, boolean isMain) {
            this.activity = a;
            /**
             * Here is where you place the data that the validator will use to determine
             * if the file was delivered correctly. This is encoded in the source code
             * so the application can easily determine whether the file has been
             * properly delivered without having to talk to the server. If the
             * application is using LVL for licensing, it may make sense to eliminate
             * these checks and to just rely on the server.
             */
            xAPKS.add(new XAPKFile(
                    isMain, // true signifies a main file
                    version, // the version of the APK that the file was uploaded against
                    size // the length of the file in bytes
            ));
            // Check if expansion files are available before going any further
            if (!expansionFilesDelivered()) {

                try {
                    Intent launchIntent = activity.getIntent();

                    // Build an Intent to start this activity from the Notification
                    Intent notifierIntent = new Intent(activity, activity.getClass());
                    notifierIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    notifierIntent.setAction(launchIntent.getAction());

                    if (launchIntent.getCategories() != null) {
                        for (String category : launchIntent.getCategories()) {
                            notifierIntent.addCategory(category);
                        }
                    }

                    PendingIntent pendingIntent = PendingIntent.getActivity(activity, 0, notifierIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                    // Start the download service (if required)
                    Log.v(LOG_TAG, "Start the download service");
                    int startResult = DownloaderClientMarshaller.startDownloadServiceIfRequired(activity, pendingIntent, DownloaderService.class);

                    // If download has started, initialize activity to show progress
                    if (startResult != DownloaderClientMarshaller.NO_DOWNLOAD_REQUIRED) {
                        Log.v(LOG_TAG, "initialize activity to show progress");
                        // Instantiate a member instance of IStub
                        downloaderClientStub = DownloaderClientMarshaller.CreateStub(this, DownloaderService.class);
                        // Shows download progress
                        if (progressDialog == null) {
                            progressDialog = new ProgressDialog(activity);
                            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                            progressDialog.setMessage(downloadingMessage);
                            progressDialog.setCancelable(false);
                        }
                        progressDialog.show();
                    }
                    // If the download wasn't necessary, fall through to start the app
                    else {
                        Log.v(LOG_TAG, "No download required");
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(LOG_TAG, "Cannot find own package! MAYDAY!");
                    e.printStackTrace();
                } catch (Exception e) {
                    Log.e(LOG_TAG, e.getMessage());
                    e.printStackTrace();
                }

            } else {
                downloaded = true;
            }
        }

        public void setProgressDialog(ProgressDialog pg) {
            progressDialog = pg;
        }

        public void setOnProgressListener(Runnable onProgressListener) {
            onProgress = onProgressListener;

        }

        public void setOnCompleteListener(Runnable onCompleteListener) {
            onComplete = onCompleteListener;
        }

        public void setOnErrorListener(Runnable onErrorListener) {
            onError = onErrorListener;
        }

        private void onProgress() {
            if (onProgress != null) onProgress.run();
        }

        private void onComplete() {
            if (expansionFile == null) {
                try {
                    expansionFile = APKExpansionSupport.getAPKExpansionZipFile(context, version, patchVersion);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (onComplete != null) onComplete.run();
        }

        private void onError() {
            if (onError != null) onError.run();
        }


        /**
         * Go through each of the APK Expansion files defined in the structure above
         * and determine if the files are present and match the required size. Free
         * applications should definitely consider doing this, as this allows the
         * application to be launched for the first time without having a network
         * connection present. Paid applications that use LVL should probably do at
         * least one LVL check that requires the network to be present, so this is
         * not as necessary.
         *
         * @return true if they are present.
         */
        boolean expansionFilesDelivered() {
            for (int i = 0; i < xAPKS.size(); i++) {
                XAPKFile xf = xAPKS.get(i);
                String fileName = Helpers.getExpansionAPKFileName(activity, xf.mIsMain, xf.mFileVersion);
                if (!Helpers.doesFileExist(activity, fileName, xf.mFileSize, false)) {
                    Log.e(LOG_TAG, "ExpansionAPKFile doesn't exist or has a wrong size (" + fileName + ").");
                    return false;
                }
            }
            return true;
        }


        /**
         * Connect the stub to our service on start.
         */
        public void connect() {
            if (null != downloaderClientStub) {
                downloaderClientStub.connect(activity);
            } else {
                if (downloaded && onComplete != null) onComplete.run();
            }
        }

        /**
         * Disconnect the stub from our service on stop
         */
        public void disconnect() {
            if (null != downloaderClientStub) {
                downloaderClientStub.disconnect(activity);
            }
        }

        @Override
        public void onServiceConnected(Messenger m) {
            IDownloaderService remoteService = DownloaderServiceMarshaller.CreateProxy(m);
            remoteService.onClientUpdated(downloaderClientStub.getMessenger());
        }

        @Override
        public void onDownloadProgress(DownloadProgressInfo progress) {
            long percents = progress.mOverallProgress * 100 / progress.mOverallTotal;
            Log.v(LOG_TAG, "DownloadProgress:" + Long.toString(percents) + "%");
            progressDialog.setProgress((int) percents);
            onProgress();
        }

        @Override
        public void onDownloadStateChanged(int newState) {
            Log.v(LOG_TAG, "DownloadStateChanged : " + activity.getString(Helpers.getDownloaderStringResourceIDFromState(newState)));

            switch (newState) {
                case IDownloaderClient.STATE_DOWNLOADING:
                    Log.v(LOG_TAG, "Downloading...");
                    break;
                case IDownloaderClient.STATE_COMPLETED: // The download was finished
                    progressDialog.setMessage(completeMessage);
                    // dismiss progress dialog
                    progressDialog.dismiss();
                    downloaded = true;
                    if (onComplete != null) onComplete.run();
                    break;
                case IDownloaderClient.STATE_FAILED_UNLICENSED:
                case IDownloaderClient.STATE_FAILED_FETCHING_URL:
                case IDownloaderClient.STATE_FAILED_SDCARD_FULL:
                case IDownloaderClient.STATE_FAILED_CANCELED:
                case IDownloaderClient.STATE_FAILED:
                    if (showAlertError) {
                        AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                        alert.setTitle(errorTitle);
                        alert.setMessage(errorMessage);
                        alert.setNeutralButton(closeButton, null);
                        alert.show();
                    }

                    if (onError != null) onError.run();
                    break;
            }
        }

    }

}
