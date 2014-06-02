package com.example.ubiplug_sme;

import android.support.v4.app.Fragment;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


public class DetailsFragment extends Fragment{
	
	public TextView lblPeakCurrent;
	public TextView lblTransientRMS;
	public TextView lblTransientPeak;
	public TextView lblPeakCurrentValue;
	public TextView lblTransientRMSValue;
	public TextView lblTransientPeakValue;	
	
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
 
        View rootView = inflater.inflate(R.layout.fragment_details, container, false);
         
        return rootView;
    }
	
	@Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        lblPeakCurrent = (TextView)getActivity().findViewById(R.id.lblPeakCurrent);
        lblPeakCurrentValue = (TextView)getActivity().findViewById(R.id.lblPeakCurrentValue);
        lblTransientRMS = (TextView)getActivity().findViewById(R.id.lblTransientRMS);
        lblTransientRMSValue = (TextView)getActivity().findViewById(R.id.lblTransientRMSValue);
        lblTransientPeak = (TextView)getActivity().findViewById(R.id.lblTransientPeak);
        lblTransientPeakValue = (TextView)getActivity().findViewById(R.id.lblTransientPeakValue);
        
        Typeface tf = Typeface.createFromAsset(getActivity().getApplicationContext().getAssets(),"fonts/roboto-regular.ttf");
        lblPeakCurrent.setTypeface(tf);
        lblPeakCurrentValue.setTypeface(tf);
        lblTransientRMS.setTypeface(tf);
        lblTransientRMSValue.setTypeface(tf);
        lblTransientPeak.setTypeface(tf);
        lblTransientPeakValue.setTypeface(tf);
	}
}
