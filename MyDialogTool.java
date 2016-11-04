package com.samp.ling.sampleapp.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

public class MyDialogTool {
    private static final String tag = MyDialogTool.class.getSimpleName();

    public static AlertDialog alertDialogJustMsg(@NonNull Context context, String message) {
        return alertDialog(context, null, null, message, null, null, null, null, true, null, -1, -1);
    }

    public static AlertDialog alertDialogJustSel(@NonNull Context context,
                                                 String posName, String neuName, String negName,
                                                 final Handler actionHandler, final int posId, final int neuId) {
        return alertDialog(context, null, null, null, null, posName, neuName, negName, true, actionHandler, posId, neuId);
    }

    public static AlertDialog alertDialogSimple(@NonNull Context context, String title, String message,
                                                String posName, String negName,
                                                final Handler actionHandler, final int posId) {
        return alertDialog(context, null, title, message, null, posName, null, negName, true, actionHandler, posId, -1);
    }

    public static AlertDialog alertDialog(@NonNull Context context, Drawable icon, String title, String message, final View view,
                                          String posName, String neuName, String negName, boolean cancelable,
                                          final Handler actionHandler, final int posId, final int neuId) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

        if (icon != null)
            alertDialogBuilder.setIcon(icon);

        if (!MyStrTool.isReallyEmpty(title))
            alertDialogBuilder.setTitle(title);

        if (!MyStrTool.isReallyEmpty(message))
            alertDialogBuilder.setMessage(message);

        if (view != null)
            alertDialogBuilder.setView(view);

        if (!MyStrTool.isReallyEmpty(posName))
            alertDialogBuilder.setPositiveButton(posName, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (actionHandler != null) {
                        Message message = new Message();
                        message.what = posId;
                        if (view instanceof EditText) {
                            message.obj = ((EditText) view).getText().toString();
                        }
                        actionHandler.sendMessage(message);
                    }
                    dialog.dismiss();
                }
            });

        if (!MyStrTool.isReallyEmpty(neuName))
            alertDialogBuilder.setNeutralButton(neuName, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (actionHandler != null) {
                        Message message = new Message();
                        message.what = neuId;
                        if (view instanceof EditText) {
                            message.obj = ((EditText) view).getText().toString();
                        }
                        actionHandler.sendMessage(message);
                    }
                    dialog.dismiss();
                }
            });

        if (!MyStrTool.isReallyEmpty(negName)) {
            alertDialogBuilder.setNegativeButton(negName, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
        } else {
            if (!cancelable) cancelable = true;
        }

        alertDialogBuilder.setCancelable(cancelable);

        alertDialogBuilder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                Log.d(tag, "alertDialog onCancel");
                dialog.dismiss();
            }
        });

        alertDialogBuilder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                Log.d(tag, "alertDialog onDismiss");
            }
        });

        AlertDialog alertDialog = alertDialogBuilder.create();

        alertDialog.show();

        return alertDialog;
    }

}
