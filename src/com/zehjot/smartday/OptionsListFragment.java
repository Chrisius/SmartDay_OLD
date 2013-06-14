package com.zehjot.smartday;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import com.zehjot.smartday.data_access.DataSet;
import com.zehjot.smartday.data_access.DataSet.onDataAvailableListener;
import com.zehjot.smartday.helper.Utilities;
import com.zehjot.smartday.TabListener.OnSectionSelectedListener;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class OptionsListFragment extends ListFragment implements onDataAvailableListener, OnSectionSelectedListener {
	private OnOptionSelectedListener mCallback;
	private SimpleAdapter optionsListAdapter;
	private List<Map<String,String>> displayedOptions;
	private static final String TEXT1 = "text1";
	private static final String TEXT2 = "text2";
	private int startview = -1;
	private DataSet dataSet = null;
	public interface OnOptionSelectedListener{
		public void onOptionSelected(int pos);
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		dataSet = DataSet.getInstance(getActivity());
		/*---Initialize data for list---*/
		final String[] fromMapKey = new String[] {TEXT1, TEXT2};
		final int[] toLayoutId = new int[] {android.R.id.text1, android.R.id.text2};
		if(displayedOptions==null){
			displayedOptions = new ArrayList<Map<String,String>>();
			displayedOptions.add(displayDate());
			displayedOptions.add(toMap(getResources().getString(R.string.options_app_text1), getResources().getString(R.string.options_app_text2)));
		}
		setStartView(startview);
		/*---Set up list adapter---*/
		optionsListAdapter = new SimpleAdapter(
				getActivity(), 
				displayedOptions, //the data
				android.R.layout.simple_list_item_2, //the layout
				fromMapKey, 
				toLayoutId);
	}
	
	@Override
	public void onStart(){
		super.onStart();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		View view = inflater.inflate(R.layout.options_list, container, false);
		((ListView)view).addHeaderView(inflater.inflate(R.layout.header_view,null),null,false);
		setListAdapter(optionsListAdapter);
		return view;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		//getListView().addHeaderView(getActivity().getLayoutInflater().inflate(R.layout.header_view,null),null,false); //Inflates the Header View und attaches it to the List		
		//setListAdapter(optionsListAdapter);
	}
	
	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		mCallback = (OnOptionSelectedListener) activity;
	}
	
	@Override
	public void onListItemClick(ListView l,View v,int position, long id){
		switch (position-1) {
		case 0:
			mCallback.onOptionSelected(position-1);	//-1 to eliminate the unclickable header	
			break;
		case 1:
			dataSet.getApps(this);			
			break;
		case 2:
			dataSet.getContext(dataSet.getSelectedDateAsTimestamp(),dataSet.getNextDayAsTimestamp(),this);
			break;
		default:
			break;
		}
		//mCallback.onOptionSelected(position-1); 
	}
	public void onDataAvailable(JSONObject jObj, String request){
		//check if fragment is added to activity
		if(!isAdded())
			return;
		if(request.equals("values")){			
			SelectAppsDialogFragment apps = new SelectAppsDialogFragment();
			apps.setStrings(Utilities.jObjValuesToArrayList(jObj).toArray(new String[0]));
			apps.setMode(SelectAppsDialogFragment.SELECT_APPS);
			apps.show(getFragmentManager(), getString(R.string.datepicker));
		}
		if(request.equals("events")){
			Utilities.showDialog(jObj.toString(), getActivity());
		}
	}
	@Override
	public void onSectionSelected(int pos) {
		if(displayedOptions == null){
			startview = pos;
		}else
		updateOptions(pos);
	}

	public void updateDate(){
		displayedOptions.set(0, displayDate());
		optionsListAdapter.notifyDataSetChanged();
	}
	
	private void updateOptions(int pos){
		switch (pos) {
		case 0:
			if(displayedOptions.size()<3){//check if exists because of nullpointer
				displayedOptions.add(toMap(getString(R.string.options_map_text1), getString(R.string.options_map_text2)));}
			else{
				displayedOptions.set(2,toMap(getString(R.string.options_map_text1), getString(R.string.options_map_text2)));}
			optionsListAdapter.notifyDataSetChanged();
			break;
		case 1:
			if(displayedOptions.size()<3){//check if exists because of nullpointer
				displayedOptions.add(toMap(getString(R.string.options_chart_text1), getString(R.string.options_chart_text2)));}
			else{
				displayedOptions.set(2,toMap(getString(R.string.options_chart_text1), getString(R.string.options_chart_text2)));}
			optionsListAdapter.notifyDataSetChanged();
			break;
		case 2:
			if(displayedOptions.size()>2){
				displayedOptions.remove(2);
				optionsListAdapter.notifyDataSetChanged();}
			break;
		default:
			break;
		}
	}
	
	private Map<String,String> displayDate(){
		return toMap(dataSet.getSelectedDate(),getResources().getString(R.string.options_date_text2));
	}
	
	private Map<String,String> toMap(String text1, String text2){
		Map<String, String> map = new HashMap<String, String>();
		map.put(TEXT1, text1);
		map.put(TEXT2, text2);
		return map;
	}
	
	/*private List<Map<String,String>> toListMap(String[] text1, String[] text2){
		List<Map<String,String>> listItem = new ArrayList<Map<String,String>>(2);
		
		for(int i = 0; i<text1.length;i++){
			Map<String, String> listItemMap = new HashMap<String, String>();
			listItemMap.put(TEXT1, text1[i]);
			listItemMap.put(TEXT2, text2[i]);
			listItem.add(listItemMap);
		}
		return listItem;
	}*/
	private void setStartView(int pos){
		switch (pos) {
		case 0:
			displayedOptions.add(toMap(getResources().getString(R.string.options_map_text1), getResources().getString(R.string.options_map_text2)));			
			break;
		case 1:
			displayedOptions.add(toMap(getResources().getString(R.string.options_chart_text1), getResources().getString(R.string.options_chart_text2)));
			break;
		default:
			break;
		}
		
	}
}
