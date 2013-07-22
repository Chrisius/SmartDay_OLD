package com.zehjot.smartday;

import org.json.JSONObject;

import com.zehjot.smartday.R;
import com.zehjot.smartday.TabListener.OnUpdateListener;
import com.zehjot.smartday.data_access.DataSet;
import com.zehjot.smartday.data_access.DataSet.onDataAvailableListener;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;

public class SectionTimelineFragment extends Fragment implements OnUpdateListener,onDataAvailableListener{
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		Log.d("Timeline", "CreateView");
		super.onCreateView(inflater, container, savedInstanceState);
		return inflater.inflate(R.layout.section_timeline_fragment, container, false);
	}

	@Override
	public void onResume(){
		super.onResume();
		DataSet.getInstance(getActivity()).getApps(this);	
	}
	
	public void onUpdate() {
		DataSet.getInstance(getActivity()).getApps(this);		
	}
	
	@Override
	public void onDataAvailable(JSONObject jObj, String requestedFunction) {
		ViewGroup root = (ViewGroup) getActivity().findViewById(R.id.timelinell);
		//ViewGroup root = (ViewGroup) getView();
		if(root!=null){
			root.removeAllViews();			
			TimeLineView timeline = new TimeLineView(getActivity());/*
			timeline.setLayoutParams(new LayoutParams(
					LayoutParams.WRAP_CONTENT,
		            LayoutParams.WRAP_CONTENT));
		            */
			timeline.setLayoutParams(new LayoutParams(
					6000,
		            300));
			root.addView(timeline);
		}
	}
}
