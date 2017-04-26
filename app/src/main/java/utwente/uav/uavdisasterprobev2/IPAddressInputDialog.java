package utwente.uav.uavdisasterprobev2;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import dji.sdk.sdkmanager.DJISDKManager;

/**
 * Created by Mathijs on 4/20/2017.
 */

public class IPAddressInputDialog {

    private Context context;
    private String IP = null;
    private OnFinishedListener onFinishedListener;

    public IPAddressInputDialog(Context context, OnFinishedListener onFinishedListener) {
        this.context = context;
        this.onFinishedListener = onFinishedListener;
    }

    public void show() {
        final View inputView = LayoutInflater.from(context).inflate(R.layout.bridge_ip_input_dialog, null);

        final EditText input = (EditText) inputView.findViewById(R.id.edit_text_ip);

        InputFilter[] filters = new InputFilter[1];
        filters[0] = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end,
                                       android.text.Spanned dest, int dstart, int dend) {
                if (end > start) {
                    String destTxt = dest.toString();
                    String resultingTxt = destTxt.substring(0, dstart)
                            + source.subSequence(start, end)
                            + destTxt.substring(dend);
                    if (!resultingTxt
                            .matches("^\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3})?)?)?)?)?)?")) {
                        return "";
                    } else {
                        String[] splits = resultingTxt.split("\\.");
                        for (int i = 0; i < splits.length; i++) {
                            if (Integer.valueOf(splits[i]) > 255) {
                                return "";
                            }
                        }
                    }
                }
                return null;
            }
        };

        input.setFilters(filters);

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setTitle("IP Address");
        builder.setMessage("Please enter an IP address");
        builder.setView(inputView);
        builder.setCancelable(false);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                onFinishedListener.onFinished(input.getText().toString());
            }
        });
        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
        builder.create();
        builder.show();
    }

    public interface OnFinishedListener {
        void onFinished(String IP);
    }
}
