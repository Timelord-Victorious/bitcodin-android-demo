package com.bitmovin.bitcodin;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class ErrorPopup {
  private AlertDialog mAlertDialog;

  public ErrorPopup(Context context) {
    this.mAlertDialog = new AlertDialog.Builder(context).create();
    this.mAlertDialog.setCancelable(false);
    this.mAlertDialog.setMessage("alert");
    this.mAlertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, "OK",
        new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        dialog.dismiss();
      }
    });
  }

  public void show(String message) {
    this.mAlertDialog.setMessage(message);
    this.mAlertDialog.show();
  }
}