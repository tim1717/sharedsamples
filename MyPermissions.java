import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.net.Uri;
import android.provider.Settings;
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
 * use example:
    String[] permissions = new String[] {android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
    MyPermissions.RationaleDialog rationaleDialog =
        MyPermissions.checkPermissions(MainActivity.this, permissions, PERMISSION_CODE, "please!");
 * note: remember to rationaleDialog.dismiss() somewhere like onDestroy
 */

public class MyPermissions {
    private static final String tag = MyPermissions.class.getSimpleName();

    public static final String PREFERENCE_KEY = MyGlobals.PREFERENCE_KEY;
    public static final int FIRST = 0;
    public static final int SEEN = 1;
    public static final int DONTASK = 2;

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
     * only permission check (single), no rationale
     */
    public static void checkPermissions(@NonNull Activity activity,
                                        @NonNull String permission, int requestCode) {
        String[] permissions = new String[] {permission};

        MyPermissions.checkPermissions(activity, permissions, requestCode, null);
    }

    /**
     * only permission check (multi), no rationale
     */
    public static void checkPermissions(@NonNull Activity activity,
                                        @NonNull String[] permissions, int requestCode) {

        MyPermissions.checkPermissions(activity, permissions, requestCode, null);
    }

    /**
     * both permission check (single) and rationale
     */
    public static RationaleDialog checkPermissions(@NonNull Activity activity,
                                         @NonNull String permission, int requestCode, String rationaleMessage) {
        String[] permissions = new String[] {permission};

        return MyPermissions.checkPermissions(activity, permissions, requestCode, rationaleMessage);
    }

    /**
     * permissions check and rationale
     * <br>onRequestPermissionsResult should always handle permitted actions
     * <br>Otherwise can change method to 'boolean checkPermissions()'
     * <br>(FALSE if pass through checkSelfPermission (needToCheck), TRUE if already permitted)
     * @param activity context
     * @param permissions permissions: Manifest.permission.PERMISSION_NAME
     * @param requestCode request code
     * @param rationaleMessage message explaining permission
     */
    public static RationaleDialog checkPermissions(@NonNull Activity activity,
                                        @NonNull String[] permissions, int requestCode,
                                        String rationaleMessage) {
        Log.i(tag, "checkingPermissions[] >>> " + permissions.length);
        if (permissions.length <= 0) return null;

        Set<String> toCheckPermissions = new HashSet<>();
        Set<String> toShowRationales = new HashSet<>();

        for (String permission : permissions) {
            Log.d(tag, permission);
            /**
             * granted / shouldShow / firstTime
             * 1st time: false, false, true
             * 2nd, deny: false, true, false
             * 3rd+, dont ask: false, false, false
             * allow: true, dc
             * differ 1st vs dont ask: use sharedPreferences
             */

            if (!MyStrTool.isReallyEmpty(permission)) {
                boolean granted = MyPermissions.checkSelfPermission(activity, permission);
                if (!granted) {
                    toCheckPermissions.add(permission);
                }

                boolean shouldShow = MyPermissions.shouldShowRequestPermissionRationale(activity, permission);
                SharedPreferences sharedPreferences = activity.getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE);
				int firstTime = sharedPreferences.getInt(permission, FIRST);
                boolean firstTimeState = sharedPreferences.getInt(permission, FIRST) < SEEN;
				Log.d(tag, "permissions shouldShow=" + shouldShow + " firstTime=" + firstTime + firstTimeState);

                if (shouldShow || firstTimeState) {
                    toShowRationales.add(permission);
                    sharedPreferences.edit().putInt(permission, SEEN).apply();
                } else if (!shouldShow && !firstTimeState) {
                    sharedPreferences.edit().putInt(permission, DONTASK).apply();
                }
            }
        }

        int toCheckSize = toCheckPermissions.size();
        int toShowSize = toShowRationales.size();
        Log.d(tag, "permissions to check >>> p" + toCheckSize + "/r" + toShowSize);

        if (toCheckSize > 0) {
            if (toShowSize > 0 && !MyStrTool.isReallyEmpty(rationaleMessage)) {
                Log.d(tag, "prepare rationaleDialog");
                RationaleDialog rationaleDialog = new RationaleDialog(activity, rationaleMessage);
                RunRequestPermissions runRequestPermissions = new RunRequestPermissions(activity, toCheckPermissions, requestCode);

                // custom dialog for shouldShowRequestPermissionRationale
                rationaleDialog.setCallback(runRequestPermissions);
                rationaleDialog.show();

                return rationaleDialog;
            } else {
                String[] doCheckPermissions = toCheckPermissions.toArray(new String[toCheckSize]);

                MyPermissions.requestPermissions(activity, doCheckPermissions, requestCode);
            }
        } else {
            // onRequestPermissionsResult to handle permitted
            MyPermissions.requestPermissions(activity, permissions, requestCode);
        }

        return null;
    }

    public static String getPermissionName(String permission) throws IndexOutOfBoundsException {
        return permission.substring(permission.lastIndexOf(".") + 1);
    }

    public static String getPermissionGroup(Context context, String permission) {
        PackageManager packageManager = context.getPackageManager();
        String permissionGroup = null;
        try {
            PermissionInfo permissionGroupInfo = packageManager.getPermissionInfo(permission, 0);
            permissionGroup = permissionGroupInfo.group;
            if (!MyStrTool.isReallyEmpty(permissionGroup)) {
                permissionGroup = permissionGroup.substring(permissionGroup.lastIndexOf(".") + 1);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return permissionGroup;
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

    /**
     * basic AlertDialog for rationaleMessage
     */
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

        @Override
        public void show() {
            Builder alertDialogBuilder = new Builder(mContext);

            alertDialogBuilder.setTitle("Permissions");

            if (!MyStrTool.isReallyEmpty(message))
                alertDialogBuilder.setMessage(message);

            alertDialogBuilder.setNegativeButton("Proceed", new DialogInterface.OnClickListener() {
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
            // otherwise might window leak
            if (alertDialog != null) {
                alertDialog.dismiss();
                alertDialog = null;
            }
            super.dismiss();
        }
    }

    /**
     * basic AlertDialog for warning disabled Permissions
     */
    public static AlertDialog warnPermissions(@NonNull final Context context, String message) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

        if (!MyStrTool.isReallyEmpty(message))
            alertDialogBuilder.setMessage(message);

        alertDialogBuilder.setPositiveButton("Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        PackageManager packageManager = context.getPackageManager();
        final Intent intent = PermissionsHelper.applicationSettings(context);
        if (intent.resolveActivity(packageManager) != null) {
            alertDialogBuilder.setNeutralButton("Settings", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    context.startActivity(intent);
                }
            });
        }

        AlertDialog alertDialog = alertDialogBuilder.create();

        alertDialog.show();

        return alertDialog;
    }

    public static Intent applicationSettings(Context context) {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        Uri uri = Uri.fromParts("package", context.getPackageName(), null);
        intent.setData(uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        return intent;
    }

    /**
     * Activity's onRequestPermissionsResult
     *
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        int permissionsSize = permissions.length;
        int grantResultsSize = grantResults.length;
        Log.d(tag, "onRequestPermissionsResult: " + requestCode
                + " p" + permissionsSize + "/g" + grantResultsSize);

        switch (requestCode) {
            case PERMISSION_CODE: {
                if (permissionsSize > 0) {
                    showPermissionWarning(permissions, grantResults, permissionsSize, grantResultsSize);
                }
                return;
            }
            default:
                break;
        }
    }

    // designed to show an alert dialog warning
    // if a permission was set to "don't ask again"
    // after user returns to same point of check permission
    // note: can be expanded to show other states
    private void showPermissionWarning(String[] permissions, int[] grantResults,
                                       int permissionsSize, int grantResultsSize) {
        Set<String> permissionsGroupsDontAsked = new HashSet<>();
        Set<String> permissionsGroupsNotGranted = new HashSet<>();
        StringBuilder permissionsDontAsked = new StringBuilder("");
        StringBuilder permissionsNotGranted = new StringBuilder("");
        StringBuilder permissionsGranted = new StringBuilder("");
        boolean somePermissionGranted = false;
        boolean somePermissionNotGranted = false;
        SharedPreferences sharedPreferences = getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE);

        // sorts permissions that were not/granted and don't asked
        for (int i = 0; i < permissionsSize; i++) {
            try {
                Log.i(tag, permissions[i] + ": " + (grantResultsSize > i ? grantResults[i] : "-"));
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    somePermissionNotGranted = true;
                    String permissionGroup = MyPermissions.getPermissionGroup(MainActivity.this, permissions[i]);

                    boolean dontAsked = sharedPreferences.getInt(permissions[i], 0) == MyPermissions.DONTASK;
                    if (dontAsked) {
                        if (!permissionsGroupsDontAsked.contains(permissionGroup)) {
                            permissionsGroupsDontAsked.add(permissionGroup);
                            permissionsDontAsked.append("-").append(permissionGroup).append("\n");
                        }
                    } else {
                        if (!permissionsGroupsNotGranted.contains(permissionGroup)) {
                            permissionsGroupsNotGranted.add(permissionGroup);
                            permissionsNotGranted.append("-").append(permissionGroup).append("\n");
                        }
                    }
                } else {
                    somePermissionGranted = true;
                    String permissionName = MyPermissions.getPermissionName(permissions[i]);
                    permissionsGranted.append("-").append(permissionName).append("\n");
                }
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }

        Log.i(tag, "strGroupsDontAsked=" + permissionsDontAsked.toString());
        Log.i(tag, "strGroupsNotGranted=" + permissionsNotGranted.toString());
        Log.i(tag, "strGroupsGranted=" + permissionsGranted.toString());

        // shows which permissions groups marked as "don't asked" using alert dialog
        if (permissionsDontAsked.length() > 0) {
            MyTool.makeToastLog(this, "permissions were DISABLED");
            permissionsDontAsked.insert(0, "App functions may be affected!\n\nDisabled?\n")
                    .append("\nTo re-enable these permission(s),\n"
                            + "Please goto App's Permissions in Settings");
            permWarningDialog = MyPermissions.warnPermissions(MainActivity.this, permissionsDontAsked.toString());
        } else {
            String message = "permission(s) were"
                    + (somePermissionNotGranted ? " NOT " : " ") + "granted";
            MyTool.makeToastLog(this, message);
            permWarningDialog = null;
        }
    }
 */

}
