package com.bartoszlewandowski.uberclone;

import android.app.Application;

import com.parse.Parse;

/**
 * Created by Bartosz Lewandowski on 10.03.2019.
 */
public class App extends Application {

    // Initializes Parse SDK as soon as the application is created
    @Override
    public void onCreate() {
        super.onCreate();

        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId("jH5eF3KdhBb4da9lGqg5YBpnz7esavBER2o9T7Ud")
                .clientKey("xn7uw3xw0g11G9L6eJU6KrjSDqkuRl74kzGli2zo")
                .server("https://parseapi.back4app.com")
                .build()
        );
    }
}
