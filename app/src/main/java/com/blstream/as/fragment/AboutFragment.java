package com.blstream.as.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.blstream.as.R;


public class AboutFragment extends Fragment {

    public AboutFragment() {

    }

    public static AboutFragment newInstance() {
        return new AboutFragment();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.about_fragment, container, false);
    }
}
