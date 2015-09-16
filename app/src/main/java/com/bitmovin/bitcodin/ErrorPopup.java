package com.bitmovin.bitcodin;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.text.Html;

public class ErrorPopup {
  private AlertDialog mAlertDialog;
  private Context context;

  public ErrorPopup(Context context) {
    this.mAlertDialog = new AlertDialog.Builder(context).create();
    this.mAlertDialog.setCancelable(false);
    this.mAlertDialog.setMessage("alert");
    this.context = context;
    this.mAlertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, "OK",
        new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        dialog.dismiss();
      }
    });
  }

  public void show(String message) {
    show(message, false);
  }

  public void show(String message, boolean showRegisterButton) {
    this.mAlertDialog.setMessage(Html.fromHtml(message));
    if (showRegisterButton) {
      this.mAlertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "sign up",
          new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          Intent viewHomepageIntent = new Intent(Intent.ACTION_VIEW);
          viewHomepageIntent.setData(Uri.parse("https://www.bitcodin.com/sign-up/"));
          context.startActivity(viewHomepageIntent);
          dialog.dismiss();
        }
      });
    }
    this.mAlertDialog.show();
  }
}