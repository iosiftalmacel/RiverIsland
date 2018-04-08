package com.talmacel.iosif.riverisland.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.net.ssl.HttpsURLConnection;

public class ImageLoader {
    public static ImageLoader instance;

    private List<ImageLoadRequest> imageRequests;
    private List<ImageLoadTask> imageTasks;
    private LruCache<String, Bitmap> memoryCache;
    private Context context;

    private Handler handler;
    private int maxJobs;
    private long timeout;

    public static void CreateStaticInstance(Context context, int maxRunningJobs, long connectionTimeout){
        if(instance == null)
            instance = new ImageLoader(context, maxRunningJobs, connectionTimeout);
    }

    public ImageLoader(Context context, int maxRunningJobs, long connectionTimeout) {
        imageRequests = new ArrayList<>();
        imageTasks = new ArrayList<>();
        handler = new Handler();
        maxJobs = maxRunningJobs;
        timeout = connectionTimeout;
        this.context = context;

        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;
        memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };

        Runnable runnable = new Runnable() {
            public void run() {
                if(imageTasks.size() < maxJobs && imageRequests.size() > 0) {
                    int newTaskAllowedCount = maxJobs - imageTasks.size();

                    for (int i = 0, j = 0; i < imageRequests.size() && j < newTaskAllowedCount; i++) {
                        ImageLoadRequest request = imageRequests.get(imageRequests.size() - i - 1);
                        if (startNewTask(request)){
                            request.startTime = System.currentTimeMillis();
                            j++;
                        }
                    }
                }
                //cleanup
                for (int i = imageTasks.size() - 1; i >= 0; i--){
                    if(System.currentTimeMillis() - imageTasks.get(i).startTime > timeout)
                        onTaskFinished(false, imageTasks.get(i).link, null);
                }

                for (int i = imageRequests.size() - 1; i >= 0; i--){
                    final OnLoadListener listener = imageRequests.get(i).listener.get();
                    if((imageRequests.get(i).startTime != 0 && System.currentTimeMillis() - imageRequests.get(i).startTime > timeout) || listener == null)
                    {
                        if(listener != null)
                            listener.OnFailed();
                        imageRequests.remove(i);
                    }
                }
                handler.postDelayed(this, imageRequests.size() > 1 ? 50 : 150);
            }
        };
        handler.postDelayed(runnable, 200);
    }


    public void removeAllListeners(Context context) {
        for (int i = imageRequests.size() - 1; i >= 0; i--){
            if(imageRequests.get(i).context.get() == context)
                imageRequests.remove(i);
        }
    }

    public void removeListener(OnLoadListener listener) {
        for (int i = imageRequests.size() - 1; i >= 0; i--){
            if(imageRequests.get(i).listener.get() == listener)
                imageRequests.remove(i);
        }
    }

    public void load(String link, OnLoadListener listener){
        load(null, link, false, listener);
    }

    public void load(String link, boolean saveOnDiskCache, OnLoadListener listener){
        load(null, link, saveOnDiskCache, listener);
    }

    public void load(Context context, String link, OnLoadListener listener){
        load(context, link, false, listener);
    }

    public void load(Context context, String link, boolean saveOnDiskCache, OnLoadListener listener){
        if (TextUtils.isEmpty(link) || listener == null) {
            Log.e("ImageLoader","Invalid Parameters");
        }
        Bitmap bitmap = getBitmapFromMemoryCache(link);

        if(bitmap != null){
            listener.OnLoad(bitmap);
        }else{
            ImageLoadRequest newRequest = getImageLoadRequest(context, link, saveOnDiskCache, listener);
            imageRequests.add(newRequest);
        }
    }

    private ImageLoadRequest getImageLoadRequest(Context context, String link, boolean saveOnDiskCache, OnLoadListener listener){
        for (ImageLoadRequest current : imageRequests) {
            if(current.listener.get() == listener){
                current.link = link;
                current.saveOnDiskCache = saveOnDiskCache;
                return  current;
            }
        }

        return new ImageLoadRequest(context, link, saveOnDiskCache, listener);
    }


    private boolean startNewTask(ImageLoadRequest request){
        for (ImageLoadTask current : imageTasks) {
            if(current.link.equals(request.link)){
                return  false;
            }
        }
        imageTasks.add(new ImageLoadTask(request.link));
        new ImageJobAsyncTask(request.link, request.saveOnDiskCache, ImageLoader.this).execute();
        return  true;
    }

    private void onTaskFinished(boolean wasSuccessful, String link, Bitmap bitmap){
        if(wasSuccessful)
            addBitmapToMemoryCache(link, bitmap);

        for (int i = imageRequests.size() - 1; i >= 0; i--) {
            ImageLoadRequest current = imageRequests.get(i);
            if (current.link.equals(link) && current.listener != null && current.listener.get() != null) {
                if (wasSuccessful)
                    current.listener.get().OnLoad(bitmap);
                else
                    current.listener.get().OnFailed();
                imageRequests.remove(current);
            }
        }

        for (int i = imageTasks.size() - 1; i >= 0; i--) {
            if(imageTasks.get(i).link.equals(link)){
                imageTasks.remove(i);
            }
        }
    }

    private void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemoryCache(key) == null) {
            memoryCache.put(key, bitmap);
        }
    }

    private Bitmap getBitmapFromMemoryCache(String key) {
        return memoryCache.get(key);
    }

    private void addBitmapToDiskCache(String link, Bitmap bitmap){
        String fileName = Uri.parse(link).getLastPathSegment();
        File path = new File(context.getCacheDir() + "/images/");

        if(!path.exists())
            path.mkdir();

        File file = new File(path, fileName);
        try {
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    Bitmap getBitmapFromDiskCache(String link){
        String fileName = Uri.parse(link).getLastPathSegment();
        File file = new File( context.getCacheDir() + "/images", fileName);
        if(file.exists()){
            return BitmapFactory.decodeFile(file.getAbsolutePath());
        }
        return  null;
    }


    static class ImageJobAsyncTask extends AsyncTask<String, Void, Bitmap> {
        private String link;
        private boolean saveOnDisk;
        private WeakReference<ImageLoader> loader;

        ImageJobAsyncTask(String link, boolean saveOnDisk, ImageLoader loader) {
            this.link = link;
            this.saveOnDisk = saveOnDisk;
            this.loader = new WeakReference<>(loader);
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            //see if it was saved on disk
            if(saveOnDisk && loader.get() != null){
                Bitmap bitmapFormDiskCache = loader.get().getBitmapFromDiskCache(link);
                if(bitmapFormDiskCache != null)
                    return bitmapFormDiskCache;
            }

            HttpURLConnection urlConnection = null;
            try {
                URL uri = new URL(link);
                urlConnection = (HttpURLConnection) uri.openConnection();
                int statusCode = urlConnection.getResponseCode();
                if (statusCode != HttpsURLConnection.HTTP_OK) {
                    return null;
                }

                InputStream inputStream = urlConnection.getInputStream();
                if (inputStream != null) {
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                    //save it on disk
                    if(saveOnDisk && loader.get() != null)
                        loader.get().addBitmapToDiskCache(link, bitmap);
                    return  bitmap;
                }
            } catch (Exception e) {
                if (urlConnection != null)
                    urlConnection.disconnect();
                Log.w("ImageLoader", "Error downloading image from " + link);
            } finally {
                if (urlConnection != null)
                    urlConnection.disconnect();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (isCancelled()) {
                bitmap = null;
            }

            if (loader.get() != null) {
                loader.get().onTaskFinished(bitmap != null, link, bitmap);
            }
        }
    }

    public interface OnLoadListener{
        void OnLoad(Bitmap photo);
        void OnFailed();
    }

    class ImageLoadTask {
        String link;
        long startTime;

        ImageLoadTask(String link) {
            this.link = link;
            this.startTime = System.currentTimeMillis();
        }
    }

    class ImageLoadRequest {
        WeakReference<Context> context;
        WeakReference<OnLoadListener> listener;
        String link;
        boolean saveOnDiskCache;
        long startTime;

        ImageLoadRequest(Context context, String link, boolean saveOnDiskCache, OnLoadListener listener) {
            this.link = link;
            this.listener = new WeakReference<>(listener);
            this.context = new WeakReference<>(context);
            this.saveOnDiskCache = saveOnDiskCache;
        }
    }
}
