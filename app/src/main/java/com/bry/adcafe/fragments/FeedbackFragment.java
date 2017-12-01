package com.bry.adcafe.fragments;

import android.app.DialogFragment;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.bry.adcafe.Constants;
import com.bry.adcafe.R;
import com.bry.adcafe.Variables;
import com.bry.adcafe.models.User;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class FeedbackFragment extends DialogFragment implements View.OnClickListener {
    private Button submitButton;
    private Button cancelButton;
    private EditText editText;
    private Spinner spinner;

    private Context mContext;
    private String mKey = "";


    public void setfragContext(Context context){
        mContext = context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView =  inflater.inflate(R.layout.fragment_feedbck, container, false);
        submitButton = (Button)rootView.findViewById(R.id.SubmitAll);
        cancelButton = (Button)rootView.findViewById(R.id.cancelAll);
        editText = (EditText)rootView.findViewById(R.id.feedback);
        spinner = (Spinner)rootView.findViewById(R.id.SpinnerFeedbackType);

        submitButton.setOnClickListener(this);
        cancelButton.setOnClickListener(this);


        return rootView;
    }


    @Override
    public void onClick(View v) {
        if(v == cancelButton){
            dismiss();
        }else if(v == submitButton){
            String feedback = editText.getText().toString();
            String feedbackType = spinner.getSelectedItem().toString();
            uploadFeedBackToDatabase(feedback,feedbackType);
        }
    }

    private void uploadFeedBackToDatabase(String feedback, String feedbackType) {
        DatabaseReference mRef3 = FirebaseDatabase.getInstance().getReference(Constants.FEEDBACK).child(feedbackType);
        mRef3.push();
        mRef3.setValue(feedback);
        Toast.makeText(mContext,"Feedback received.",Toast.LENGTH_SHORT).show();
        dismiss();
    }
}