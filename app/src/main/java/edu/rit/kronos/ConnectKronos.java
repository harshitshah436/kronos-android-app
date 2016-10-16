package edu.rit.kronos;

import android.os.AsyncTask;
import android.util.Log;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;

/**
 * ConnectKronos.java
 * <p>
 * Extending AsyncTask class for connecting to Internet and sending/receiving http requests from android app.
 * <p>
 * Created by Harshit on 10/16/2016.
 */

class ConnectKronos extends AsyncTask<String, Void, String> {
    private String kronos_url, kronos_current_timeperiod_url, username, password;
    private String json = "";

    @Override
    protected String doInBackground(String... params) {

        if (params.length > 0) {
            kronos_url = params[0];
            kronos_current_timeperiod_url = params[1];
            username = params[2];
            password = params[3];
        }

        Log.d("ConnectKronos", kronos_url);

        try {
            Connection.Response loginForm = Jsoup.connect(kronos_url)
                    .method(Connection.Method.GET)
                    .execute();


            Connection.Response res = Jsoup.connect(kronos_url)
                    .data("cookieexists", "false")
                    .data("username", username)
                    .data("password", password)
                    .cookies(loginForm.cookies())
                    .method(Connection.Method.POST)
                    .execute();

            json = Jsoup.connect(kronos_current_timeperiod_url)
                    .ignoreContentType(true)
                    .cookies(res.cookies())
                    .execute()
                    .body();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d("ConnectKronos", json);

        return json;
    }
}