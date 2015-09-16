package com.bitmovin.bitcodin;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.text.Html;
import android.view.View;

public class ErrorPopup {
  private AlertDialog mAlertDialog;

  public ErrorPopup(final Context context) {
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

  public void show(String message) {
    show(message, false);
  }

  public void show(String message, boolean showRegisterButton) {
    this.mAlertDialog.setMessage(Html.fromHtml(message));
    this.mAlertDialog.show();
    this.mAlertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setVisibility(
        showRegisterButton ? View.VISIBLE : View.GONE
    );
  }
}