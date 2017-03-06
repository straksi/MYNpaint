package com.deitel.doodlz;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import java.io.Serializable;

public class PrefActivity extends PreferenceActivity implements Serializable
{

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref);
    }
}