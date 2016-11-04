package com.samp.ling.sampleapp.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

public class MyNetTool {
    private static final String tag = MyNetTool.class.getSimpleName();

    public static boolean isNetworkAvailable(@NonNull Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static class HostAvailabilityTask extends AsyncTask<String, Void, Boolean[]> {

        public HostAvailabilityTask() {
        }

        protected Boolean[] doInBackground(String... params) {
            int numOfArgs = params.length;
            Boolean[] results = new Boolean[numOfArgs];

            for (int i = 0; i < numOfArgs; i++) {
                try {
                    URL url = new URL(params[i]);
                    HttpURLConnection httpURLConn = (HttpURLConnection) url.openConnection();
                    httpURLConn.setConnectTimeout(1000);
                    httpURLConn.connect();
                    Boolean isReachable = (httpURLConn.getResponseCode() == HttpURLConnection.HTTP_OK);
                    Log.d(tag, isReachable + " " + params[i]);
                    results[i] = isReachable;
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    results[i] = false;
                } catch (IOException e) {
                    e.printStackTrace();
                    results[i] = false;
                }
            }

            return results;
        }

        protected void onPostExecute(Boolean[] result) {
            if (result != null) {
                Log.d(tag, "results: " + Arrays.toString(result));
            } else {
                Log.w(tag, "results null");
            }
        }
    }

    public static class MyNetReceiver extends BroadcastReceiver {

        public MyNetReceiver() {
            Log.d(tag, "MyNetReceiver set");
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(tag, "!!! MyNetReceiver !!! " + action);
        }

    }

}
