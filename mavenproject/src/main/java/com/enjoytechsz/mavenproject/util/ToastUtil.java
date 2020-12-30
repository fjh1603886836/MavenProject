package com.enjoytechsz.mavenproject.util;

import android.content.Context;
import android.widget.Toast;

public class ToastUtil {
    private Context context;
    private Toast toast;

    private ToastUtil() {
        this.context = AppContext.getInstance().getApplicationContext();
        this.toast = Toast.makeText(context, null, Toast.LENGTH_SHORT);
    }

    public void showShort(String content) {
        toast.setText(content);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.show();
    }

    public void showLong(String content) {
        toast.setText(content);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.show();
    }

    public void showShort(int stringId) {
        toast.setText(context.getString(stringId));
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.show();
    }

    public void showLong(int stringId) {
        toast.setText(context.getString(stringId));
        toast.setDuration(Toast.LENGTH_LONG);
        toast.show();
    }

    public static final ToastUtil getInstance() {
        return ToastUtilHolder.instance;
    }

    private static class ToastUtilHolder {
        private static final ToastUtil instance = new ToastUtil();
    }
}
