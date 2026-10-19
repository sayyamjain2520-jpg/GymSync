package com.example.gymsync;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "GymSyncUserSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_EMAIL = "userEmail";
    private static final String KEY_USER_NAME = "userName";

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void createLoginSession(String email, String name) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_NAME, name);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getUserEmail() {
        return pref.getString(KEY_USER_EMAIL, "");
    }

    public String getUserName() {
        return pref.getString(KEY_USER_NAME, "Athlete");
    }

    public void updateUserName(String name) {
        editor.putString(KEY_USER_NAME, name);
        editor.apply();
    }

    public void logoutUser() {
        editor.clear();
        editor.apply();
    }
}
