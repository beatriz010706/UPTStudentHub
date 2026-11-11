package com.example.uptstudenthub;
import android.content.Context;
import androidx.appcompat.app.AlertDialog;
/** Shows the "About" dialog. Used by both activities (avoids duplication). */
public final class AboutDialog {
    private AboutDialog() { }
    public static void show(Context context) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.about_title)
                .setMessage(R.string.about_message)
                .setPositiveButton(R.string.ok, null)
                .show();
    }
}
