import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

/**
 * <pre>
 * https://developer.android.com/training/permissions/requesting.html
 * https://developer.android.com/guide/topics/security/permissions.html
 * use example:
    String[] permissions = new String[] {Manifest.permission.READ_CONTACTS};
    MyPermissions.checkPermissions(MainActivity.this, permissions, PERMISSION_CODE, "please!");
 * </pre>
 */
public class MyPermissions {
    private static final String TAG = MyPermissions.class.getSimpleName();

    // for firstTime detection
    private static final String PREFERENCE_KEY = TAG;

    /**
     * before or after permission request stages for getPermissionState()
     */
    public static class RequestStage {
        public static final int BEFORE = 1;
        public static final int AFTER = 2;
    }

    /**
     * <pre>
     * before and after states for getPermissionState():
     * checkSelfPermission()
     * shouldShowRequestPermissionRationale()
     *
     * BEFORE
     * F F = FIRST
     * F T = LATER
     * T x = GRANTED
     *
     * AFTER
     * F F = DONT
     * F T = DENY
     * T x = ALLOW
     *
     * if AFTER = DONT, then permission = "don't ask again"
     *
     * BEFORE | AFTER
     * F F | F F = already don't ask again
     * F F | F T = 1st time deny
     * F F | T x = 1st time allow
     * F T | F F = later don't ask again
     * F T | F T = later deny
     * F T | T x = later allow
     * T x | x x = already allow (granted)
     * </pre>
     */
    public static class RequestState {
        public static final int FIRST = 1;
        public static final int LATER = 2;
        public static final int GRANTED = 3;
        public static final int DONT = 4;
        public static final int DENY = 5;
        public static final int ALLOW = 6;
        public static final int RATIONALE = 7;
        public static final int NO_RATIONALE = 8;
    }

    /**
     * TRUE if granted
     */
    public static boolean checkSelfPermission(@NonNull Context context,
                                              @NonNull String permission) {
        if (TextUtils.isEmpty(permission)) return false;

        boolean granted = (ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED);
        Log.i(TAG, "checkSelf " + getPermissionName(permission) + " Granted:" + granted);

        return granted;
    }

    public static boolean shouldShowRequestPermissionRationale(@NonNull Activity activity,
                                                               @NonNull String permission) {
        if (TextUtils.isEmpty(permission)) return false;

        boolean rationale = ActivityCompat.shouldShowRequestPermissionRationale(activity, permission);
        Log.i(TAG, "shouldShow " + getPermissionName(permission) + " Rationale:" + rationale);

        return rationale;
    }

    public static void requestPermissions(@NonNull Activity activity,
                                          @NonNull String[] permissions, int requestCode) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode);
    }

    /**
     * used to determine various states of the permission request,
     * refer to RequestState for details
     */
    public static int getPermissionState(@NonNull Activity activity, @NonNull String permission,
                                         int stage) {
        if (TextUtils.isEmpty(permission)) {
            throw new IllegalArgumentException("invalid permission");
        }

        Log.d(TAG, "getState " + getPermissionName(permission) + " Stage:" + stage);
        boolean granted = MyPermissions.checkSelfPermission(activity, permission);
        boolean rationale = MyPermissions.shouldShowRequestPermissionRationale(activity, permission);
        int state;

        if (stage == RequestStage.BEFORE) {
            if (granted) {
                state = RequestState.GRANTED;
            } else {
                if (rationale) {
                    state = RequestState.LATER;
                } else {
                    state = RequestState.FIRST;
                }
            }
        } else if (stage == RequestStage.AFTER) {
            if (granted) {
                state = RequestState.ALLOW;
            } else {
                if (rationale) {
                    state = RequestState.DENY;
                } else {
                    state = RequestState.DONT;
                }
            }
        } else {
            if (granted) {
                state = RequestState.GRANTED;
            } else {
                if (rationale) {
                    state = RequestState.RATIONALE;
                } else {
                    state = RequestState.NO_RATIONALE;
                }
            }
        }

        return state;
    }

    /**
     * only permission check (single), no rationale
     */
    public static void checkPermissions(@NonNull Activity activity, @NonNull String permission,
                                        int requestCode) {
        String[] permissions = new String[] {permission};

        MyPermissions.checkPermissions(activity, permissions, requestCode, null);
    }

    /**
     * only permission check (multi), no rationale
     */
    public static void checkPermissions(@NonNull Activity activity, @NonNull String[] permissions,
                                        int requestCode) {

        MyPermissions.checkPermissions(activity, permissions, requestCode, null);
    }

    /**
     * both permission check (single) and rationale
     */
    public static void checkPermissions(@NonNull Activity activity, @NonNull String permission,
                                        int requestCode, String rationaleMessage) {
        String[] permissions = new String[] {permission};

        MyPermissions.checkPermissions(activity, permissions, requestCode, rationaleMessage);
    }

    /**
     * base permissions check and rationale
     * @param activity context
     * @param permissions permissions: Manifest.permission.PERMISSION_NAME
     * @param requestCode request code
     * @param rationaleMessage optional pre-request message explaining need for permissions
     */
    public static void checkPermissions(@NonNull Activity activity, @NonNull String[] permissions,
                                        int requestCode, String rationaleMessage) {
        Log.d(TAG, "checkingPermissions[] x" + permissions.length);
        if (permissions.length <= 0) return;

        Set<String> toAskPermissions = new HashSet<>();
        Set<String> toShowRationales = new HashSet<>();

        for (String permission : permissions) {
            Log.i(TAG, permission);

            if (!TextUtils.isEmpty(permission)) {
                int permissionState = MyPermissions.getPermissionState(activity, permission,
                        RequestStage.BEFORE);

                if (permissionState != RequestState.GRANTED) {
                    toAskPermissions.add(permission);

                    boolean firstState = permissionState == RequestState.FIRST;

                    // IMPORTANT!
                    // if showing rationale is desired for First Time requests
                    // without knowing getPermissionState() @ Stage.AFTER yet
                    // sharedPreferences is needed to differentiate First Time and Don't Ask Again
                    SharedPreferences sharedPreferences = activity.getSharedPreferences(PREFERENCE_KEY,
                            Context.MODE_PRIVATE);
                    boolean firstTime = sharedPreferences.getBoolean(permission, true);

                    // to also include rationale for First Time
                    // (because shouldShowRequestPermissionRationale() will return false)
                    // but not if permissions are Don't Ask Again
                    // (warnPermissions() will handle that)
                    // firstState | firstTime
                    // T F = Don't Ask Again
                    // T T = First Time
                    // F F = Not First Time
                    // F T = should never happen
                    Log.d(TAG, "firstState " + firstState + ", firstTime " + firstTime);
                    if (!(firstState & !firstTime)) {
                        toShowRationales.add(permission);
                        sharedPreferences.edit().putBoolean(permission, false).apply();
                    }
                }
            }
        }

        int toAskSize = toAskPermissions.size();
        int toShowSize = toShowRationales.size();
        Log.d(TAG, "check >> p" + toAskSize + "/r" + toShowSize);

        if (toAskSize > 0) {
            String[] doCheckPermissions = toAskPermissions.toArray(new String[toAskSize]);

            if (toShowSize > 0 && !TextUtils.isEmpty(rationaleMessage)) {
                FragmentManager fm = activity.getFragmentManager();
                PermissionsDialog dialog = PermissionsDialog.rationale(rationaleMessage,
                        doCheckPermissions, requestCode);
                dialog.show(fm, "rationale");
            } else {
                MyPermissions.requestPermissions(activity, doCheckPermissions, requestCode);
            }
        } else {
            // onRequestPermissionsResult to handle permitted
            MyPermissions.requestPermissions(activity, permissions, requestCode);
        }
    }

    /**
     * get short-name from full permission string-name
     */
    public static String getPermissionName(@NonNull String permission) {
        int index = permission.lastIndexOf(".");
        if (index != -1) {
            return permission.substring(index + 1);
        } else {
            return permission;
        }
    }

    /**
     * get permission's group short-name
     */
    public static String getPermissionGroup(@NonNull Context context, @NonNull String permission) {
        PackageManager packageManager = context.getPackageManager();
        String permissionGroup = null;

        try {
            PermissionInfo permissionGroupInfo = packageManager.getPermissionInfo(permission, 0);
            permissionGroup = permissionGroupInfo.group;
            if (!TextUtils.isEmpty(permissionGroup)) {
                int index = permissionGroup.lastIndexOf(".");
                if (index != -1) {
                    permissionGroup = permissionGroup.substring(index + 1);
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return permissionGroup;
    }

    /**
     * AlertDialog via DialogFragment for:
     * permissions rationale
     * warning of disabled permissions (Don't Ask Again)
     */
    public static class PermissionsDialog extends DialogFragment {
        private static final String MESSAGE = "MESSAGE";
        private static final String PERMISSIONS = "PERMISSIONS";
        private static final String REQUESTCODE = "REQUESTCODE";

        public PermissionsDialog() {
            setCancelable(false);
        }

        public static PermissionsDialog rationale(String message, @NonNull String[] permissions,
                                                  int requestCode) {
            PermissionsDialog frag = new PermissionsDialog();
            Bundle args = new Bundle();
            args.putString(MESSAGE, message);
            args.putStringArray(PERMISSIONS, permissions);
            args.putInt(REQUESTCODE, requestCode);
            frag.setArguments(args);
            return frag;
        }

        public static PermissionsDialog warning(String message) {
            PermissionsDialog frag = new PermissionsDialog();
            Bundle args = new Bundle();
            args.putString(MESSAGE, message);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            String message = getArguments().getString(MESSAGE);
            final String[] permissions = getArguments().getStringArray(PERMISSIONS);
            final int requestCode = getArguments().getInt(REQUESTCODE);
            final boolean continuePermissions = permissions != null;

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
            alertDialogBuilder.setMessage(message);

            String posName = "Proceed";
            if (!continuePermissions) {
                posName = "Close";

                final Intent intent = applicationSettings(getActivity());
                if (intent != null) {
                    alertDialogBuilder.setNeutralButton("Settings", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dismiss();
                            getActivity().startActivity(intent);
                        }
                    });
                }
            }

            alertDialogBuilder.setPositiveButton(posName,  new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (continuePermissions) {
                        MyPermissions.requestPermissions(getActivity(), permissions, requestCode);
                    }
                }
            });

            return alertDialogBuilder.create();
        }
    }

    /**
     * <pre>
     * designed for activity:
     * public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
     *
     * returns a functional AlertDialog warning, warnPermissions()
     * if a permission was set to "don't ask again",
     * note: can be expanded to show other states
     *
        @Override
        public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
            switch (requestCode) {
                case PERMISSION_CODE: {
                    MyPermissions.PermissionsDenyResult permissionsDenyResult =
                            MyPermissions.permissionsDenied(MainActivity.this, permissions, grantResults);

                    // disable functions and warn about permissions not granted
                    Set<String> permissionGroupsNotGranted = permissionsDenyResult.getAllNotGranted();
                    if (permissionGroupsNotGranted != null) {
                        for (String permissionGroup : permissionGroupsNotGranted) {
                            // disable functions here
                        }

                        // warnings
                        Set<String> permissionGroupsDontAsked = permissionsDenyResult.getPermissionGroupsDontAsked();
                        MyPermissions.warnPermissions(MainActivity.this, permissionGroupsDontAsked);
                    }
                    return;
                }
                default:
                    break;
            }
        }
     * </pre>
     */
    public static PermissionsDenyResult permissionsDenied(@NonNull Activity activity,
                                                          String[] permissions, int[] grantResults) {
        if (permissions == null || grantResults == null) return null;
        int permissionsSize = permissions.length;
        int grantResultsSize = grantResults.length;
        if (permissionsSize <= 0 || grantResultsSize <= 0) return null;
        if (permissionsSize != grantResultsSize) {
            Log.w(TAG, "permissionsDenied[] x" + permissionsSize
                    + " != grantResults[] x" + grantResultsSize);
            return null;
        }

        // compiling results for possible notification creation
        Set<String> permissionGroupsDontAsked = null;
        Set<String> permissionGroupsNotGranted = null;

        for (int i = 0; i < permissionsSize; i++) {
            Log.d(TAG, permissions[i] + " = " + grantResults[i]);
            int permissionState = MyPermissions.getPermissionState(activity,
                    permissions[i],
                    MyPermissions.RequestStage.AFTER);
            String permissionGroup = MyPermissions.getPermissionGroup(activity, permissions[i]);

            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                if (permissionState == MyPermissions.RequestState.DONT) {
                    if (permissionGroupsDontAsked == null) {
                        permissionGroupsDontAsked = new HashSet<>();
                    }

                    permissionGroupsDontAsked.add(permissionGroup);
                    Log.i(TAG, " DONT: " + permissions[i] + ", " + permissionGroup);
                } else {
                    if (permissionGroupsNotGranted == null) {
                        permissionGroupsNotGranted = new HashSet<>();
                    }

                    permissionGroupsNotGranted.add(permissionGroup);
                    Log.i(TAG, " DENY: " + permissions[i] + ", " + permissionGroup);
                }
            } else {
                Log.i(TAG, " GRANT: " + permissions[i] + ", " + permissionGroup);
            }
        }

        return new PermissionsDenyResult(permissionGroupsDontAsked, permissionGroupsNotGranted);
    }

    /**
     * custom class to return denied results, open to design
     */
    public static class PermissionsDenyResult {
        Set<String> permissionGroupsDontAsked, permissionGroupsNotGranted;

        public PermissionsDenyResult(Set<String> permissionGroupsDontAsked,
                                     Set<String> permissionGroupsNotGranted) {
            this.permissionGroupsDontAsked = permissionGroupsDontAsked;
            this.permissionGroupsNotGranted = permissionGroupsNotGranted;
            boolean dontAsked = !(permissionGroupsDontAsked == null
                    || permissionGroupsDontAsked.isEmpty());
            boolean notGranted = !(permissionGroupsNotGranted == null
                    || permissionGroupsNotGranted.isEmpty());
            Log.d(TAG, "dontAsked " + dontAsked + ", notGranted " + notGranted);
        }

        public Set<String> getPermissionGroupsDontAsked() {
            return permissionGroupsDontAsked;
        }

        public Set<String> getAllNotGranted() {
            if (permissionGroupsNotGranted == null || permissionGroupsDontAsked == null) {
                if (permissionGroupsNotGranted != null) {
                    return permissionGroupsNotGranted;
                } else if (permissionGroupsDontAsked != null) {
                    return permissionGroupsDontAsked;
                } else {
                    return null;
                }
            } else {
                permissionGroupsNotGranted.addAll(permissionGroupsDontAsked);
                return permissionGroupsNotGranted;
            }
        }
    }

    /**
     * warning of denied permissions based on level of deny
     */
    public static void warnPermissions(@NonNull final Activity activity,
                                       Set<String> permissionsDisabled) {
        if (permissionsDisabled == null || permissionsDisabled.isEmpty()) {
            // none Don't Ask Again, so a simple warning
            String message = "permission(s) were NOT granted";
            Snackbar snackbar = Snackbar.make(activity.findViewById(android.R.id.content),
                    message, Snackbar.LENGTH_LONG);
            snackbar.show();
            return;
        }

        StringBuilder permissionsDontAsked = new StringBuilder("");
        for (String permissionGroup : permissionsDisabled) {
            permissionsDontAsked.append("-").append(permissionGroup).append("\n");
        }

        permissionsDontAsked.insert(0, "There are disabled Permissions:\n")
                .append("\nTo re-enable these permission(s),\n"
                        + "Please goto the App's Permissions in Settings");

        FragmentManager fm = activity.getFragmentManager();
        PermissionsDialog dialog = PermissionsDialog.warning(permissionsDontAsked.toString());
        dialog.show(fm, "warning");
    }

    /**
     * intent to go to application's settings
     */
    public static Intent applicationSettings(Context context) {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

        try {
            Uri uri = Uri.fromParts("package", context.getPackageName(), null);
            intent.setData(uri);

            PackageManager packageManager = context.getPackageManager();
            ComponentName componentName = intent.resolveActivity(packageManager);
            if (componentName == null) {
                return null;
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
            return null;
        }

        return intent;
    }

}
