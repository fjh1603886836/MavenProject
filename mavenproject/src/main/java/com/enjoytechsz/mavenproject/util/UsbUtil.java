package com.enjoytechsz.mavenproject.util;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class UsbUtil {
    private static final String TAG = UsbUtil.class.getSimpleName();
    private static final String ACTION_DEVICE_PERMISSION = UsbUtil.class.getSimpleName() + ".USB_PERMISSION";
    UsbHelperSessionHashMap usbHelperSessionHashMap = new UsbHelperSessionHashMap();
    private Context context;
    private UsbManager usbManager = null;
    private PendingIntent permissionIntent;
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_DEVICE_PERMISSION.equals(action)) {
                Log.e(TAG, "ACTION_DEVICE_PERMISSION");
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            connectDevice(device);
                        }
                    }
                }
            }
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    Log.d(TAG, String.format("ACTION_USB_DEVICE_ATTACHED (%s)%s", device.getProductName(), device.getDeviceName()));
                    if (device != null) {
                        if (usbManager.hasPermission(device)) {
                            connectDevice(device);
                        } else {
                            usbManager.requestPermission(device, permissionIntent);
                        }
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    Log.d(TAG, String.format("ACTION_USB_DEVICE_DETACHED (%s)%s", device.getProductName(), device.getDeviceName()));
                    disconnectDevice(device);
                }
            }
        }
    };

    private UsbUtil() {
        this.context = AppContext.getInstance().getApplicationContext();
        this.usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

        //注册插拔广播
        IntentFilter usbFilter = new IntentFilter();
        usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        context.registerReceiver(broadcastReceiver, usbFilter);

        //注册usb权限广播
        permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_DEVICE_PERMISSION), 0);
        IntentFilter permissionFilter = new IntentFilter(ACTION_DEVICE_PERMISSION);
        context.registerReceiver(broadcastReceiver, permissionFilter);
    }

    public static final UsbUtil getInstance() {
        return UsbHelperHolder.instance;
    }

    public void enumerate() {
        for (Map.Entry<String, UsbDevice> entry : usbManager.getDeviceList().entrySet()) {
            String name = entry.getKey();
            UsbDevice device = entry.getValue();
            UsbHelperSession session = usbHelperSessionHashMap.get(device.getVendorId(), device.getProductId());
            if (session != null) {
                if (usbManager.hasPermission(device)) {
                    Log.d(TAG, "already has permission, try to connect");
                    connectDevice(device);
                } else {
                    Log.d(TAG, "request permission");
                    usbManager.requestPermission(device, permissionIntent);
                }
            }
        }
    }

    public void dump() {
        for (Map.Entry<String, UsbDevice> entry : usbManager.getDeviceList().entrySet()) {
            String name = entry.getKey();
            UsbDevice device = entry.getValue();
            Log.d(TAG, String.format("(%s)%s: 0x%04X:0x%04X", device.getProductName(), device.getDeviceName(), device.getVendorId(), device.getProductId()));
        }
    }

    public void registerListener(int vid, int pid, Listener listener) {
        UsbHelperSession session = new UsbHelperSession(vid, pid);
        session.setListener(listener);
        usbHelperSessionHashMap.put(vid, pid, session);
    }

    private void connectDevice(UsbDevice usbDevice) {
        if (usbDevice != null) {
            String deviceName = usbDevice.getDeviceName();
            UsbHelperSession session = usbHelperSessionHashMap.get(usbDevice.getVendorId(), usbDevice.getProductId());
            if (session != null) {
                UsbDeviceConnection usbDeviceConnection = usbManager.openDevice(usbDevice);
                if (usbDeviceConnection != null) {
                    int fd = usbDeviceConnection.getFileDescriptor();
                    int vid = usbDevice.getVendorId();
                    int pid = usbDevice.getProductId();
                    session.setUsbDeviceConnection(usbDeviceConnection);
                    Listener listener = session.getListener();
                    session.setConnected(true);
                    if (listener != null) {
                        listener.onConnected(usbDevice, usbDeviceConnection);
                    }
                }
            }
        }
    }

    public void destroy() {
        Log.d(TAG, String.format("destroy"));
        for (Map.Entry<String, UsbDevice> entry : usbManager.getDeviceList().entrySet()) {
            String name = entry.getKey();
            UsbDevice device = entry.getValue();
            UsbHelperSession session = usbHelperSessionHashMap.get(device.getVendorId(), device.getProductId());
            if (session != null) {
                UsbDeviceConnection usbDeviceConnection = session.getUsbDeviceConnection();
                if (usbDeviceConnection != null) {
                    Log.d(TAG, String.format("close fd(%d)", session.usbDeviceConnection.getFileDescriptor()));
                    usbDeviceConnection.close();
                }
            }
        }
        if (broadcastReceiver != null) {
            HashMap<String, UsbDevice> devices = usbManager.getDeviceList();
            Iterator<UsbDevice> iterator = devices.values().iterator();
            while (iterator.hasNext()) {
                UsbDevice device = iterator.next();
//                Log.d(TAG, String.format("(%s)%s: 0x%04X:0x%04X", device.getProductName(), device.getDeviceName(), device.getVendorId(), device.getProductId()));
            }
            context.unregisterReceiver(broadcastReceiver);
        }
    }

    private class UsbHelperSessionHashMap {
        private HashMap<Integer, UsbHelperSession> SessionHashMap = new HashMap<Integer, UsbHelperSession>();

        private Integer sessionHashKey(int vid, int pid) {
            Integer key = (vid << 8) | pid;
            return key;
        }

        public UsbHelperSession get(int vid, int pid) {
            for (Map.Entry<Integer, UsbHelperSession> entry : SessionHashMap.entrySet()) {
                UsbHelperSession session = entry.getValue();
                if (session.getVid() == vid && session.getPid() == pid) {
                    return session;
                }
            }
            return null;
        }

        public void put(int vid, int pid, UsbHelperSession session) {
            SessionHashMap.put(sessionHashKey(vid, pid), session);
        }
    }

    private void disconnectDevice(UsbDevice device) {
        if (device != null) {
            String deviceName = device.getDeviceName();
            UsbHelperSession session = usbHelperSessionHashMap.get(device.getVendorId(), device.getProductId());
            if (session != null) {
                if (session.isConnected() == true) {
                    UsbDeviceConnection usbDeviceConnection = session.getUsbDeviceConnection();
                    if (usbDeviceConnection != null) {
                        Listener listener = session.getListener();
                        if (listener != null) {
                            listener.onDisconnected();
                        }
                        usbDeviceConnection.close();
                    }
                    session.setConnected(false);
                }
            }
        }
    }

    public interface Listener {
        void onConnected(UsbDevice usbDevice, UsbDeviceConnection usbDeviceConnection);

        void onDisconnected();
    }

    private static class UsbHelperHolder {
        private static final UsbUtil instance = new UsbUtil();
    }

    private class UsbHelperSession {
        private int vid;
        private int pid;
        private Listener listener;
        private UsbDevice device;
        private UsbDeviceConnection usbDeviceConnection;
        private boolean connected;

        public UsbHelperSession(int vid, int pid) {
            this.vid = vid;
            this.pid = pid;
        }

        public int getVid() {
            return vid;
        }

        public int getPid() {
            return pid;
        }

        public Listener getListener() {
            return listener;
        }

        public void setListener(Listener listener) {
            this.listener = listener;
        }

        public UsbDeviceConnection getUsbDeviceConnection() {
            return usbDeviceConnection;
        }

        public void setUsbDeviceConnection(UsbDeviceConnection usbDeviceConnection) {
            this.usbDeviceConnection = usbDeviceConnection;
        }

        public boolean isConnected() {
            return connected;
        }

        public void setConnected(boolean connected) {
            this.connected = connected;
        }
    }
}


