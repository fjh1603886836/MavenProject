package com.enjoytechsz.mavenproject.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PermissionUtil {
    private static final String TAG = PermissionUtil.class.getSimpleName();
    private Activity activity;
    private Listener permissionListener;
    public int requestCode;

    public static final PermissionUtil getInstance() {
        return PermissionUtilHolder.instance;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public void checkAndRequestPermissions(String[] permissions, Listener permissionListener) {
        PermissionUtil permissionUtil = getInstance();
        permissionUtil.permissionListener = permissionListener;
        activity.requestPermissions(permissions, ++this.requestCode);
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.e(TAG, String.format("onRequestPermissionsResult requestCode(%d) permissions(%s) grantResults(%s)", requestCode, Arrays.toString(permissions), Arrays.toString(grantResults)));
        if (requestCode != this.requestCode) {
            return;
        }

        List<String> notGrantedList = new ArrayList<>();
        List<String> permanentProhibitList = new ArrayList<>();
        for (int i = 0; i < permissions.length; i++) {
            String permission = permissions[i];
            boolean shouldShowRequestPermissionRationale = ActivityCompat.shouldShowRequestPermissionRationale(activity, permission);
            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                if (shouldShowRequestPermissionRationale == false) {
                    // permanent prohibit...
                    permanentProhibitList.add(permission);
                } else {
                    // not granted at this time
                    notGrantedList.add(permission);
                }
            }
        }

        if (notGrantedList.size() > 0) {
            String[] notGranted = notGrantedList.toArray(new String[notGrantedList.size()]);
            Log.e("jjj", String.format("!!do requestPermissions..%s", Arrays.toString(notGranted)));
            ActivityCompat.requestPermissions(activity, notGranted, ++this.requestCode);
        } else if (permanentProhibitList.size() != 0) {
            showAlertDialog();
        } else {
            permissionListener.onAllPermissionGranted();
        }
    }

    private void showAlertDialog() {
        if (activity == null) {
            throw new RuntimeException();
        }
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("警告！")
                .setMessage("请前往设置中打开相关权限，否则功能无法正常运行！")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                        intent.setData(uri);
                        activity.startActivity(intent);
                        activity.finish();
                    }
                }).show();
    }

    public interface Listener {
        void onAllPermissionGranted();
    }

    private static class PermissionUtilHolder {
        private static final PermissionUtil instance = new PermissionUtil();
    }
}
