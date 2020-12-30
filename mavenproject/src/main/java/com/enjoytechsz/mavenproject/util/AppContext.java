package com.enjoytechsz.mavenproject.util;

import android.content.Context;

public class AppContext {
    private static AppContext instance;
    private Context applicationContext;

    public AppContext(Context application) {
        this.applicationContext = application;
    }

    public static AppContext getInstance() {
        if (instance == null) {
            throw new RuntimeException();
        }
        return instance;
    }

    public static void init(Context context) {
        if (instance != null) {
            throw new RuntimeException();
        }
        instance = new AppContext(context);
    }

    public static boolean isInitialized() {
        return (instance != null);
    }

    public Context getApplicationContext() {
        return applicationContext;
    }
}
