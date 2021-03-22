package com.masonsoft.imsdk.sample.app.main;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.idonans.systeminsets.SystemUiHelper;
import com.masonsoft.imsdk.sample.app.FragmentDelegateActivity;

public class MainActivity extends FragmentDelegateActivity {

    public static void start(Context context) {
        Intent starter = new Intent(context, MainActivity.class);
        context.startActivity(starter);
    }

    private static final String FRAGMENT_TAG_MAIN = "fragment_main_20210322";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SystemUiHelper.from(getWindow())
                .layoutStatusBar()
                .layoutStable()
                .setLightStatusBar()
                .setLightNavigationBar()
                .apply();

        setFragmentDelegate(FRAGMENT_TAG_MAIN, MainFragment::newInstance);
    }

}