package com.samp.ling.sampleapp.utils;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

/**
 * https://developer.android.com/training/permissions/requesting.html
 * https://developer.android.com/guide/topics/security/permissions.html
 */

public class MyPermissions {
    private static final String tag = MyPermissions.class.getSimpleName();

    public static final String PREFERENCE_KEY = MyGlobals.PREFERENCE_KEY;

    /**
     * TRUE if GRANTED
     */
    public static boolean checkSelfPermission(@NonNull Context context, @NonNull String permission) {
        if (MyStrTool.isReallyEmpty(permission)) return false;

        boolean granted = (ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED);
        Log.d(tag, permission + " granted: " + granted);

        return granted;
    }

    public static boolean shouldShowRequestPermissionRationale(@NonNull Activity activity,
                                                                @NonNull String permission) {
        if (MyStrTool.isReallyEmpty(permission)) return false;

        boolean show = ActivityCompat.shouldShowRequestPermissionRationale(activity, permission);
        Log.d(tag, permission + " rationale: " + show);

        return show;
    }

    public static void requestPermissions(@NonNull Activity activity,
                                           @NonNull String[] permissions, int requestCode) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode);
    }

    /**
     * only permission check (single)
     */
    public static void checkPermissions(@NonNull Activity activity,
                                        @NonNull String permission, int requestCode) {
        String[] permissions = new String[] {permission};

        MyPermissions.checkPermissions(activity, permissions, requestCode, null);
    }

    /**
     * only permission check (multi)
     */
    public static void checkPermissions(@NonNull Activity activity,
                                        @NonNull String[] permissions, int requestCode) {

        MyPermissions.checkPermissions(activity, permissions, requestCode, null);
    }

    /**
     * both permission check (single) and rationale
     */
    public static void checkPermissions(@NonNull Activity activity,
                                         @NonNull String permission, int requestCode, RationaleDialog rationaleDialog) {
        String[] permissions = new String[] {permission};

        MyPermissions.checkPermissions(activity, permissions, requestCode, rationaleDialog);
    }

    /**
     * permissions check and rationale
     * <br>onRequestPermissionsResult will always handle permitted actions
     * <br>Otherwise can change method to 'boolean checkPermissions()'
     * <br>(FALSE if pass through checkSelfPermission (needToCheck), TRUE if already permitted)
     * @param activity context
     * @param permissions permissions: Manifest.permission.PERMISSION_NAME
     * @param requestCode request code
     * @param rationaleDialog custom AlertDialog explaining permission
     */
    public static void checkPermissions(@NonNull Activity activity,
                                         @NonNull String[] permissions, int requestCode, RationaleDialog rationaleDialog) {
        Log.i(tag, "checkPermissions[] >>> " + permissions.length);
        if (permissions.length <= 0) return;

        Set<String> needToCheckPermissions = new HashSet<>();
        Set<String> needToShowRationales = new HashSet<>();

        for (String permission : permissions) {
            Log.d(tag, permission);
            /**
             * 1st time: false, false
             * 2nd, deny: false, true
             * 3rd+, dont ask: false, false
             * allow: true, dc
             * differ 1st vs dont ask: use sharedPreferences
             */

            if (!MyStrTool.isReallyEmpty(permission)) {
                boolean granted = MyPermissions.checkSelfPermission(activity, permission);
                if (!granted) {
                    needToCheckPermissions.add(permission);
                }

                SharedPreferences sharedPreferences = activity.getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE);
                boolean firstTime = sharedPreferences.getBoolean(permission, true);

                boolean shouldShow = MyPermissions.shouldShowRequestPermissionRationale(activity, permission);
                if (shouldShow || (!shouldShow && firstTime)) {
                    needToShowRationales.add(permission);
                    sharedPreferences.edit().putBoolean(permission, false).apply();
                }
            }
        }

        int needToCheckPermissionsSize = needToCheckPermissions.size();
        int needToShowRationalesSize = needToShowRationales.size();
        boolean needToCheck = needToCheckPermissionsSize > 0;
        Log.d(tag, "permissions need check >>> p" + needToCheckPermissionsSize + "/r" + needToShowRationalesSize);

        if (needToCheck) {
            if (needToShowRationalesSize > 0
                    && (rationaleDialog != null && !MyStrTool.isReallyEmpty(rationaleDialog.getMessage()))) {
                Log.d(tag, "prepare rationaleDialog");
                RunRequestPermissions runRequestPermissions = new RunRequestPermissions(activity, needToCheckPermissions, requestCode);

                // custom dialog for shouldShowRequestPermissionRationale
                rationaleDialog.setCallback(runRequestPermissions);
                rationaleDialog.show();
            } else {
                String[] doCheckPermissions = needToCheckPermissions.toArray(new String[needToCheckPermissionsSize]);

                MyPermissions.requestPermissions(activity, doCheckPermissions, requestCode);
            }
        } else {
            // onRequestPermissionsResult to handle permitted
            MyPermissions.requestPermissions(activity, permissions, requestCode);
        }
    }

    /**
     * callback to do requestPermissions after rationaleMessage dialog
     */
    private static class RunRequestPermissions implements Runnable {
        private Activity activity;
        private Set<String> permissionsSet;
        private String[] permissionsArray;
        private int requestCode;

        public RunRequestPermissions(@NonNull Activity activity,
                                     @NonNull Set<String> permissions, int requestCode) {
            this.activity = activity;
            this.permissionsSet = permissions;
            this.requestCode = requestCode;
        }

        public RunRequestPermissions(@NonNull Activity activity,
                                     @NonNull String[] permissions, int requestCode) {
            this.activity = activity;
            this.permissionsArray = permissions;
            this.requestCode = requestCode;
        }

        @Override
        public void run() {
            Log.d(tag, "RunRequestPermissions run");

            if (permissionsSet != null) {
                int permissionsSize = permissionsSet.size();

                String[] doCheckPermissions = permissionsSet.toArray(new String[permissionsSize]);

                MyPermissions.requestPermissions(activity, doCheckPermissions, requestCode);
            } else if (permissionsArray != null) {
                MyPermissions.requestPermissions(activity, permissionsArray, requestCode);
            } else {
                Log.w(tag, "RunRequestPermissions null");
            }
        }
    }

    public static class RationaleDialog extends AlertDialog {
        private Context mContext;
        private String message;
        private Runnable callback;
        private AlertDialog alertDialog;

        public RationaleDialog(@NonNull Context context, String message) {
            super(context);
            this.mContext = context;
            this.message = message;
        }

        protected void setCallback(@NonNull Runnable callback) {
            this.callback = callback;
        }

        protected String getMessage() {
            return this.message;
        }

        @Override
        public void show() {
            Builder alertDialogBuilder = new Builder(mContext);

            alertDialogBuilder.setTitle("Permissions");

            if (!MyStrTool.isReallyEmpty(message))
                alertDialogBuilder.setMessage(message);

            alertDialogBuilder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,int id) {
                    dialog.cancel();
                }
            });

            alertDialogBuilder.setCancelable(false);

            alertDialogBuilder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    if (callback != null)
                        callback.run();
                }
            });

            alertDialog = alertDialogBuilder.create();

            alertDialog.show();
        }

        @Override
        public void dismiss() {
            // otherwise will window leak
            alertDialog.dismiss();
            super.dismiss();
        }
    }

}