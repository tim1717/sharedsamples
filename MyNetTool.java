import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MyNetTool {
    private static final String tag = MyNetTool.class.getSimpleName();

    /**
     * simply checks whether a network is connected
     * <br>note: even though network may have no internet connection
     */
    public static boolean isNetworkAvailable(@NonNull Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    /**
     * check whether URL(s) can be reached
     * <br>(internet connection can be confirmed, if URL is correct and TRUE)
     * <br>TRUE if reachable
     * <br>FALSE if not reachable
     * <br>NULL if URL is malformed
     * <br>use example:
        String[] checkUrls = new String[] {"http://www.qweasddfg.com", "https://www.google.com/", "pokpok", "", null};
        MyNetTool.HostAvailabilityTask checkHost = new MyNetTool.HostAvailabilityTask() {
            @Override
            protected void onPostExecute(List<UrlResult> urlResults) {
                if (urlResults != null) {
                    for (UrlResult urlResult : urlResults) {
                        // use urlResult.getUrl() and urlResult.getResult());
                    }
                }
            }
        };
        checkHost.execute(checkUrls);
     *
     */
    public static class HostAvailabilityTask extends AsyncTask<String, Void, List<HostAvailabilityTask.UrlResult>> {

        public HostAvailabilityTask() {
        }

        protected List<UrlResult> doInBackground(String... params) {
            List<UrlResult> urlResults = null;

            if (params != null) {
                int numOfArgs = params.length;
                urlResults = new ArrayList<>(numOfArgs);

                for (int i = 0; i < numOfArgs; i++) {
                    UrlResult urlResult = new UrlResult(params[i], null);
                    try {
                        URL url = new URL(params[i]);
                        HttpURLConnection httpURLConn = (HttpURLConnection) url.openConnection();
                        httpURLConn.setConnectTimeout(1000);
                        httpURLConn.connect();
                        boolean isReachable = (httpURLConn.getResponseCode() == HttpURLConnection.HTTP_OK);
                        urlResult.setResult(isReachable);
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        urlResult.setResult(false);
                        e.printStackTrace();
                    }

                    urlResults.add(urlResult);
                }
            }

            return urlResults;
        }

        protected void onPostExecute(List<UrlResult> urlResults) {
            if (urlResults != null) {
                Log.i(tag, "results: " + urlResults.size());
            } else {
                Log.w(tag, "results null");
            }
        }

        protected class UrlResult {
            private String url;
            private Boolean result;

            private UrlResult(String url, Boolean result) {
                this.url = url;
                this.result = result;
            }

            public Boolean getResult() {
                return result;
            }

            private void setResult(Boolean result) {
                this.result = result;
                Log.d(tag, this.url + " " + this.result);
            }

            public String getUrl() {
                return url;
            }
        }
    }

    /**
     * simple network broadcast receiver
     * <br><uses-permission android:name="android.permission.INTERNET" />
     * <br><uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
     * <br>intent-filter:
     * <br><action android:name="android.net.conn.CONNECTIVITY_CHANGE" />
     */
    public static class MyNetReceiver extends BroadcastReceiver {

        public MyNetReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(tag, "!!! MyNetReceiver !!! " + action);
            if (intent.getExtras() != null) {
                ConnectivityManager connectivityManager =
                        (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

                if (networkInfo != null) {
                    Log.i(tag, networkInfo.getTypeName() + " "
                            + String.valueOf(networkInfo.getState()) + " "
                            + String.valueOf(networkInfo.getDetailedState()));
                } else if (intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, Boolean.FALSE)) {
                    Log.d(tag, "No connection");
                } else {
                    Log.w(tag, "networkInfo null");
                }
            }
        }
    }

}
