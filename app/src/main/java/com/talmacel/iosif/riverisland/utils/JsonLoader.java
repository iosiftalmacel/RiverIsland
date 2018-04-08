package com.talmacel.iosif.riverisland.utils;


import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.SoftReference;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class JsonLoader {
    public static JsonLoader instance;
    private Context mContext;

    private JsonLoader(Context context) {
        mContext = context;
    }

    public static void CreateStaticInstance(Context context){
        if(instance == null)
            instance = new JsonLoader(context);
    }

    public <T> void downloadParsedJson(String link, Class<T> typeParameterClass, OnLoadListener<T> listener){
        String localJson = getJsonFromDiskCache(link);

        if(localJson != null && !localJson.isEmpty()){
            listener.OnLoad(jsonToClass(typeParameterClass, localJson));
        }else{
            new JsonJobAsyncTask<T>(link, listener, typeParameterClass, this).execute();
        }
    }

    String getJsonFromDiskCache(String link){
        String fileName = Uri.parse(link).getLastPathSegment();
        File file = new File( mContext.getCacheDir(), fileName);
        if(file.exists()){
            try {
                StringBuilder text = new StringBuilder();
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line;

                while ((line = br.readLine()) != null) {
                    text.append(line);
                    text.append('\n');
                }
                br.close();

                return text.toString();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        return  null;
    }

    void addJsonToDiskCache(String link, String json){
        String fileName = Uri.parse(link).getLastPathSegment();
        File file = new File(mContext.getCacheDir(), fileName);
        try {
            FileWriter writer = new FileWriter(file);
            writer.append(json);
            writer.flush();
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public <T> T jsonToClass(Class<T> tClass, String json){
        JsonParser parser = new JsonParser();
        JsonElement mJson =  parser.parse(json);
        return new Gson().fromJson(mJson, tClass);
    }

    static class JsonJobAsyncTask<T> extends AsyncTask<String, Void, String> {
        private SoftReference<OnLoadListener<T>> listenerRef;
        private SoftReference<JsonLoader> jsonLoader;
        private Class<T> tClass;
        private String link;

        JsonJobAsyncTask(String link, OnLoadListener<T> listener,  Class<T> tClass, JsonLoader loader) {
            this.listenerRef = new SoftReference<>(listener);
            this.jsonLoader = new SoftReference<>(loader);
            this.link = link;
            this.tClass = tClass;
        }

        @Override
        protected String doInBackground(String... params) {
            HttpURLConnection urlConnection = null;
            BufferedReader bufferedReader = null;
            try {
                URL uri = new URL(link);
                urlConnection = (HttpURLConnection) uri.openConnection();
                int statusCode = urlConnection.getResponseCode();
                if (statusCode != HttpsURLConnection.HTTP_OK) {
                    return null;
                }

                bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

                StringBuilder stringBuffer = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null)
                {
                    stringBuffer.append(line);
                }

                return stringBuffer.toString();
            } catch (Exception e) {
                if (urlConnection != null)
                    urlConnection.disconnect();
                Log.e("JsonUtils", "Error downloading json from " + link);
                e.printStackTrace();

            } finally {
                if (urlConnection != null)
                    urlConnection.disconnect();
                if(bufferedReader != null)
                {
                    try {
                        bufferedReader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }
        @Override
        protected void onPostExecute(String json) {
            if (isCancelled()) {
                json = null;
            }

            if (json != null && listenerRef.get() != null && jsonLoader.get() != null) {
                T data = jsonLoader.get().jsonToClass(tClass, json);
                jsonLoader.get().addJsonToDiskCache(link, json);
                if(data != null)
                    listenerRef.get().OnLoad(data);
            }
        }
    }

    public interface OnLoadListener<T>{
        void OnLoad(T parsedJson);
    }
}