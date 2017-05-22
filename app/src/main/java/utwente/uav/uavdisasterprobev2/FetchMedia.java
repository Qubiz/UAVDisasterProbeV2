package utwente.uav.uavdisasterprobev2;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.util.CommonCallbacks;
import dji.sdk.camera.Camera;
import dji.sdk.camera.MediaFile;
import dji.sdk.camera.MediaManager;

/**
 * Created by Mathijs on 5/12/2017.
 */

public class FetchMedia {

    private static final String LOG_TAG = "FetchMedia";

    private static List<MediaFile> mediaFileList = null;

    private Context context;

    private DownloadProgressCallback callback;

    public FetchMedia(Context context, DownloadProgressCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    private static void writeBitmapToFile(Bitmap bitmap, File destinationFolder, String fileName) throws IOException {
        File file = new File(destinationFolder, fileName + ".jpg");
        FileOutputStream outputStream = new FileOutputStream(file);

        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);

        outputStream.flush();
        outputStream.close();

        Log.d(LOG_TAG, "writeBitmapToFile() [PREVIEW] | Success: " + file.getPath());
    }

    private void fetchMediaList(final MediaManager.DownloadListener downloadListener) {
        setCameraToDownloadMode(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                String error = (djiError == null) ? "Success" : djiError.getDescription();
                Log.d(LOG_TAG, "setCameraToDownloadMode() | " + error);

                if (djiError == null) {
                    MediaManager mediaManager = UAVDisasterProbeApplication.getCameraInstance().getMediaManager();
                    if (mediaManager != null) {
                        mediaManager.fetchMediaList(downloadListener);
                    }
                } else {
                    setCameraToDownloadMode(this);
                }
            }
        });
    }

    public void fetchLatestPhoto(final String destinationFolderPath, final String fileName, @Nullable final String attitudeDataString, final boolean previewImage) {
        final File destination = new File(destinationFolderPath);

        fetchMediaList(new MediaManager.DownloadListener<List<MediaFile>>() {
            @Override
            public void onStart() {
                Log.d(LOG_TAG, "fetchMediaList() | Started");
            }

            @Override
            public void onRateUpdate(long total, long current, long rate) {
                DecimalFormat decimalFormat = new DecimalFormat("#.##");
                decimalFormat.setRoundingMode(RoundingMode.CEILING);

                double percentage = ((double) current / (double) total) * 100;

                Log.d(LOG_TAG, "fetchLatestPhoto() | " + "[" + decimalFormat.format(percentage) + "%] " + current + " / " + total + "(" + rate + " B/s)");
            }

            @Override
            public void onProgress(long l, long l1) {

            }

            @Override
            public void onSuccess(List<MediaFile> mediaFiles) {
                Log.d(LOG_TAG, "fetchMediaList() | Completed");
                mediaFileList = mediaFiles;

                if (mediaFileList != null) {
                    MediaManager mediaManager = UAVDisasterProbeApplication.getCameraInstance().getMediaManager();
                    for (MediaFile mediaFile : mediaFileList) {
                        Log.d(LOG_TAG, "fetchMediaList() | Name: " + mediaFile.getFileName() + " / Date: " + mediaFile.getDateCreated());
                    }

                    if (mediaManager != null) {

                        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        Date dateCurrentFile = null;

                        MediaFile mediaFile = null;

                        for (int i = 0; i < mediaFileList.size(); i++) {
                            if (mediaFileList.get(i).getMediaType().equals(MediaFile.MediaType.JPEG)) {
                                try {
                                    Date dateCompare = formatter.parse(mediaFiles.get(i).getDateCreated());
                                    if (dateCurrentFile == null) {
                                        dateCurrentFile = dateCompare;
                                        mediaFile = mediaFileList.get(i);
                                    } else {
                                        if (dateCompare.after(dateCurrentFile)) {
                                            dateCurrentFile = dateCompare;
                                            mediaFile = mediaFileList.get(i);
                                        }
                                    }
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        if (mediaFile != null) {
                            final String mediaFileName = mediaFile.getFileName().substring(0, mediaFile.getFileName().length() - 5);
                            if (previewImage) {
                                mediaManager.fetchPreviewImage(mediaFile, new MediaManager.DownloadListener<Bitmap>() {
                                    @Override
                                    public void onStart() {
                                        Log.d(LOG_TAG, "fetchLatestPhoto() [PREVIEW] | Started");
                                        callback.updateDownloadProgressText("Fetching: "  + mediaFileName + " (PREVIEW)"  , "Progress: Started... [0 %]");
                                    }

                                    @Override
                                    public void onRateUpdate(long total, long current, long rate) {
                                        DecimalFormat decimalFormat = new DecimalFormat("#.##");
                                        decimalFormat.setRoundingMode(RoundingMode.CEILING);

                                        double percentage = ((double) current / (double) total) * 100;

                                        callback.updateDownloadProgressText("Fetching: "  + mediaFileName + " (FULL RES)", "Progress: [" + decimalFormat.format(percentage) + " %]" +  "(" + (rate/1000) + " KB/s)");

                                        Log.d(LOG_TAG, "fetchLatestPhoto() [PREVIEW] | " + "[" + decimalFormat.format(percentage) + "%] " + current + " / " + total + "(" + rate + " B/s)");
                                    }

                                    @Override
                                    public void onProgress(long l, long l1) {

                                    }

                                    @Override
                                    public void onSuccess(Bitmap bitmap) {
                                        callback.updateDownloadProgressText("Fetching: "  + mediaFileName + " (PREVIEW)", "Progress: Completed... [100 %]");
                                        try {
                                            writeBitmapToFile(bitmap, destination, (attitudeDataString == null) ? fileName : fileName + "_" + attitudeDataString);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }

                                    @Override
                                    public void onFailure(DJIError djiError) {
                                        Log.d(LOG_TAG, "fetchLatestPhoto() [PREVIEW] | " + djiError.getDescription());
                                        callback.updateDownloadProgressText("Fetching: "  + mediaFileName + " (PREVIEW)", "Progress: Failed... ");
                                    }
                                });
                            } else {
                                mediaManager.fetchMediaData(mediaFile, destination, (attitudeDataString == null) ? fileName : fileName + "_" + attitudeDataString, new MediaManager.DownloadListener<String>() {
                                    @Override
                                    public void onStart() {
                                        Log.d(LOG_TAG, "fetchLatestPhoto() | Started");
                                        callback.updateDownloadProgressText("Fetching: "  + mediaFileName + " (FULL RES)"  , "Progress: Started... [0 %]");
                                    }

                                    @Override
                                    public void onRateUpdate(long total, long current, long rate) {
                                        DecimalFormat decimalFormat = new DecimalFormat("#.##");
                                        decimalFormat.setRoundingMode(RoundingMode.CEILING);

                                        double percentage = ((double) current / (double) total) * 100;

                                        callback.updateDownloadProgressText("Fetching: "  + mediaFileName + " (FULL RES)", "Progress: [" + decimalFormat.format(percentage) + " %]" +  "(" + (rate/1000) + " KB/s)");

                                        Log.d(LOG_TAG, "fetchLatestPhoto() | " + "[" + decimalFormat.format(percentage) + " %] " + current + " / " + total + "(" + rate + " B/s)");
                                    }

                                    @Override
                                    public void onProgress(long l, long l1) {

                                    }

                                    @Override
                                    public void onSuccess(String path) {
                                        callback.updateDownloadProgressText("Fetching: "  + mediaFileName + " (FULL RES)", "Progress: Completed... [100 %]");
                                        Log.d(LOG_TAG, "fetchLatestPhoto() | Success: " + path);
                                    }

                                    @Override
                                    public void onFailure(DJIError djiError) {
                                        Log.d(LOG_TAG, "fetchLatestPhoto() | " + djiError.getDescription());
                                    }
                                });
                            }
                        }
                    }
                }
            }

            @Override
            public void onFailure(DJIError djiError) {
                Log.d(LOG_TAG, "fetchMediaList() | " + djiError.getDescription());
            }
        });
    }

    private void setCameraToDownloadMode(final CommonCallbacks.CompletionCallback completionCallback) {
        final Camera camera = UAVDisasterProbeApplication.getCameraInstance();
        if (camera != null) {
            camera.getMode(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.CameraMode>() {
                @Override
                public void onSuccess(SettingsDefinitions.CameraMode cm) {
                    camera.setMode(SettingsDefinitions.CameraMode.MEDIA_DOWNLOAD, completionCallback);
                }

                @Override
                public void onFailure(DJIError djiError) {

                }
            });
        }
    }

    @Nullable
    public List<MediaFile> getMediaFileList() {
        return mediaFileList;
    }

    public interface DownloadProgressCallback {
        void updateDownloadProgressText(String fileName, String progress);
    }

}
