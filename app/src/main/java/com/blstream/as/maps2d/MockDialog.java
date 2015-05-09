package com.blstream.as.maps2d;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.blstream.as.R;
import com.blstream.as.data.BuildConfig;
import com.blstream.as.data.rest.service.Server;

/**
 * Created by Konrad on 2015-03-26.
 * Edited by Rafal Soudani
 */
public class MockDialog extends android.support.v4.app.DialogFragment implements View.OnClickListener {

    private EditText latitudeEditText, longitudeEditText, titleEditText;
    private Callbacks activity;

    public interface Callbacks {
        void goToPosition(double longitude, double latitude);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.mock_dialog_layout, container, false);

        latitudeEditText = (EditText) view.findViewById(R.id.editLat);
        longitudeEditText = (EditText) view.findViewById(R.id.editLng);
        titleEditText = (EditText) view.findViewById(R.id.editDialogTitle);

        Button okButton = (Button) view.findViewById(R.id.buttonOK);
        Button cancelButton = (Button) view.findViewById(R.id.button);

        okButton.setOnClickListener(this);
        cancelButton.setOnClickListener(this);

        setCancelable(true);
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (BuildConfig.DEBUG && (!(activity instanceof MockDialog.Callbacks)))
            throw new AssertionError("Activity: " + activity.getClass().getSimpleName() + " must implement Callbacks");
        this.activity = (Callbacks) activity;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonOK:
                if (isEmpty(latitudeEditText) || isEmpty(longitudeEditText) || isEmpty(titleEditText)) {
                    Toast.makeText(getActivity(), getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show();
                } else {
                    Server.addPoi(stringValue(titleEditText), doubleValue(latitudeEditText), doubleValue(longitudeEditText));
                    dismiss();
                    activity.goToPosition(doubleValue(longitudeEditText), doubleValue(latitudeEditText));
                }
                break;
            case R.id.button:
                dismiss();
                break;
        }
    }

    private Double doubleValue(EditText editText) {
        return Double.parseDouble(stringValue(editText));
    }

    private String stringValue(EditText editText) {
        return editText.getText().toString();
    }

    private boolean isEmpty(EditText editText) {
        return editText.getText().toString().trim().length() == 0;
    }
}
