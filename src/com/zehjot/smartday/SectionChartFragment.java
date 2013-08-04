package com.zehjot.smartday;

import java.util.Arrays;
import java.util.Random;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.CategorySeries;
import org.achartengine.model.SeriesSelection;
import org.achartengine.renderer.DefaultRenderer;
import org.achartengine.renderer.SimpleSeriesRenderer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.zehjot.smartday.R;
import com.zehjot.smartday.TabListener.OnUpdateListener;
import com.zehjot.smartday.data_access.DataSet;
import com.zehjot.smartday.data_access.DataSet.onDataAvailableListener;
import com.zehjot.smartday.helper.Utilities;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class SectionChartFragment extends Fragment implements onDataAvailableListener, OnUpdateListener{
//	private MyChart chart1=null;
	private MyChart[] charts=null;
	private static double minTimeinPercent = 0.05;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		return inflater.inflate(R.layout.section_chart_fragment, container, false);
	}
	
	@Override
	public void onResume(){
		super.onResume();
		DataSet.getInstance(getActivity()).getApps((onDataAvailableListener) getActivity());	
	}
	@Override
	public void onDestroy(){
		super.onDestroy();
	}
	@Override
	public void onSaveInstanceState(Bundle outState){
		super.onSaveInstanceState(outState);
	}
	private void removeHighlights(){
		for(int i=0; i<charts.length;i++){
			charts[i].removeHighlight();
		}
	}
	@Override
	public void onDataAvailable(JSONObject[] jObjs, String request) {
		LinearLayout layout = (LinearLayout) getActivity().findViewById(R.id.chart_1);
		if(charts==null || charts.length!=jObjs.length){
			charts = new MyChart[jObjs.length];
			layout.removeAllViews();
		}
		for(int i=0;i<jObjs.length;i++){
			if(layout.findViewById(i+10)==null){
				LinearLayout chartDrawContainer = new LinearLayout(getActivity());
				chartDrawContainer.setLayoutParams(new LayoutParams(600, 600));
				chartDrawContainer.setId(i+10);
				layout.addView(chartDrawContainer);
			}
			if(layout.findViewById(i+10).getParent()==null)
				layout.addView(layout.findViewById(i+10));
			if(charts[i]==null)
				charts[i] = new MyChart();
			boolean highlight=false;
			if(i==0)
				highlight=true;
			charts[i].draw(jObjs[i],(LinearLayout)(layout.findViewById(i+10)),R.id.chart_1_details,highlight);			
		}
//		
//		if(chart1 == null)
//			chart1 = new MyChart();
//		chart1.draw(jObjs[0], R.id.chart_1,R.id.chart_1_details);
	}

	public void onUpdate(JSONObject[] jObjs) {
		onDataAvailable(jObjs, "");
	}
	
	@Override
	public void putExtra(JSONObject jObj) {
		// TODO Auto-generated method stub
		
	}
	
	private class MyChart{
		private CategorySeries categories; 
		private DefaultRenderer renderer;
		private String[] apps = {"No Data available"};
		private JSONObject data;
		private double[] time = {1.0};
		private int[] colors = {0xA4A4A4FF};
		private GraphicalView chartView;
		private JSONObject rendererToArrayIndex;
		private JSONArray otherRendererToArrayIndex;
		private int otherColor;
		private String selectedApp="";
		private boolean highlight = false;
		private boolean wasClicked = false;
		private long date=-1;		
		private boolean repaint=true;
		
		public void removeHighlight(){
      	  SimpleSeriesRenderer[] renederers = renderer.getSeriesRenderers();
      	  for(SimpleSeriesRenderer renderer : renederers){
      		  renderer.setHighlighted(false);
      		  wasClicked =false;
      	  }
      	  if(repaint)
      		  chartView.repaint();
      	  else
      		  repaint = true;
		}
		
		private void processData(JSONObject jObj){
			data = jObj;
			JSONArray jArray = null;
			try {
				jArray = jObj.getJSONArray("result");
				apps = new String[jArray.length()];
				time = new double[jArray.length()];
				for(int i=0; i<jArray.length(); i++){
					JSONObject app = jArray.getJSONObject(i);
					apps[i] = app.getString("app");
					time[i] = 0;
					JSONArray usages = app.getJSONArray("usage");
					for(int j=0; j<usages.length();j++){
						JSONObject usage = usages.getJSONObject(j);
						long start = usage.optLong("start", -1);
						long end = usage.optLong("end", -1);
						if(start!=-1 && end!=-1)
							time[i] += end-start;
					}
					time[i] /=60.f;
					time[i] = Math.round(time[i]*100.f);
					time[i] /=100;
				}	
			} catch (JSONException e) {
				apps = new String[]{"No Data available"};
				time = new double[]{1.0};
				colors = new int[]{0xA4A4A4FF};
				e.printStackTrace();
				return;
			}
			
			JSONArray colorsOfApps = DataSet.getInstance(getActivity()).getColorsOfApps().optJSONArray("colors");
			if(colorsOfApps==null)
				colorsOfApps = new JSONArray();
			/**
			 * {
			 * 	"colors":
			 * 		[
			 * 			{
			 * 				"app":String,
			 * 				"color": int
			 * 			}
			 * 			...
			 * 		]
			 * }
			 */
			Random rnd = new Random();
			colors = new int[apps.length];
			boolean found = false;
			try{
				for(int i=0;i<apps.length;i++){
					found = false;
					for(int j=0; j<colorsOfApps.length();j++){
						JSONObject color =  colorsOfApps.getJSONObject(j);
						if(color.getString("app").equals(apps[i])){
							colors[i] = color.getInt("color");
							found = true;
							break;
						}
					}
					if(!found){
						colors[i]=rnd.nextInt();
						colorsOfApps.put(new JSONObject()
							.put("app",apps[i])
							.put("color",colors[i])							
						);
						
					}
				}
				
				
				found = false;
				for(int j=0; j<colorsOfApps.length();j++){
					JSONObject color =  colorsOfApps.getJSONObject(j);
					if(color.getString("app").equals("Other")){
						otherColor = color.getInt("color");
						found = true;
						break;
					}
				}
				if(!found){
					otherColor=rnd.nextInt();
					colorsOfApps.put(new JSONObject()
						.put("app","Other")
						.put("color",otherColor)							
					);
					
				}
				
				
				DataSet.getInstance(getActivity()).storeColorsOfApps(new JSONObject()
																	.put("colors", colorsOfApps));
			}catch(JSONException e){
			}
		}
		
		
		
		public void draw(JSONObject jObj, LinearLayout drawContainer, final int detailContainer, boolean mhighlight){
			this.highlight = mhighlight;
			date = jObj.optLong("dateTimestamp");
			processData(jObj);
			if(highlight){
				((LinearLayout)((ScrollView)((LinearLayout) getActivity().findViewById(detailContainer)).getChildAt(0)).getChildAt(0)).removeAllViews();
				((LinearLayout)((ScrollView)((LinearLayout) getActivity().findViewById(detailContainer)).getChildAt(1)).getChildAt(0)).removeAllViews();
			}
			rendererToArrayIndex = new JSONObject();
			otherRendererToArrayIndex = new JSONArray();
			double totaltime = 0;
			JSONObject selectedApps = DataSet.getInstance(getActivity()).getSelectedApps();
			for(int i=0; i < apps.length; i++){
				if(selectedApps.optBoolean(apps[i], true)){
					totaltime += time[i];
				}
			}
			totaltime = Math.round(totaltime*100.f);
			totaltime /=100;
			if(chartView==null){
				double otherTime = 0;
				categories = new CategorySeries("Number1");
				renderer = new DefaultRenderer();
				for(int i=0; i < apps.length; i++){
					if(selectedApps.optBoolean(apps[i], true)){
						if(time[i]/totaltime>minTimeinPercent){
						SimpleSeriesRenderer r = new SimpleSeriesRenderer();
						categories.add(apps[i], Math.round((time[i]/totaltime)*10000.f)/100);
						r.setColor(colors[i]);
						renderer.addSeriesRenderer(r);						
						try {
							rendererToArrayIndex.put(""+(renderer.getSeriesRendererCount()-1), i);
						} catch (JSONException e) {
							e.printStackTrace();
						}
						}else{
							otherTime += time[i];
							otherRendererToArrayIndex.put(i);
						}
					}
				}
				if(apps.length>0){
					if(otherTime > 0){
						SimpleSeriesRenderer r = new SimpleSeriesRenderer();
						otherTime = Math.round((otherTime/totaltime)*10000.f);
						otherTime /=100;
						categories.add("Other", otherTime);
						r.setColor(otherColor);
						r.setHighlighted(highlight);
						renderer.addSeriesRenderer(r);	
					}else{					
//						if(apps.length-1<0)
//							renderer.getSeriesRendererAt(apps.length).setHighlighted(true);
//						else
							renderer.getSeriesRendererAt(renderer.getSeriesRendererCount()-1).setHighlighted(highlight);
					}
					if(highlight){
						addDetail(renderer.getSeriesRendererCount()-1, detailContainer);
						wasClicked=true;
					}
				}
				renderer.setFitLegend(true);	
				renderer.setDisplayValues(true);
				renderer.setPanEnabled(false);
				renderer.setZoomEnabled(false);
				renderer.setClickEnabled(true);
				renderer.setInScroll(true);
				renderer.setChartTitle(Utilities.getDate(date)+", Total time "+totaltime+" min");
				chartView = ChartFactory.getPieChartView(getActivity(), categories, renderer);	
				chartView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
				          SeriesSelection seriesSelection = chartView.getCurrentSeriesAndPoint();
				          if (seriesSelection != null) {
				        	  repaint = false;
				        	  removeHighlights();
				        	  wasClicked = true;
				        	  addDetail(seriesSelection.getPointIndex(),detailContainer);
				        	  renderer.getSeriesRendererAt(seriesSelection.getPointIndex()).setHighlighted(true);
				        	  chartView.repaint();
				          }
					}
				});
				
				LinearLayout layout = drawContainer;
				layout.addView(chartView);
			}else{
				
				double otherTime = 0;
				categories.clear();
				renderer.removeAllRenderers();
				boolean highlighted=false;
				int selectedRenderer = -1;
				for(int i=0; i < apps.length; i++){
					if(selectedApps.optBoolean(apps[i], true)){
						if(time[i]/totaltime>minTimeinPercent){
						SimpleSeriesRenderer r = new SimpleSeriesRenderer();
						categories.add(apps[i],  Math.round((time[i]/totaltime)*10000.f)/100);
						if(apps[i].equals(selectedApp)){
							r.setHighlighted(wasClicked);
							highlighted=true;
							selectedRenderer = renderer.getSeriesRendererCount();
						}
						r.setColor(colors[i]);
						renderer.addSeriesRenderer(r);						
						try {
							rendererToArrayIndex.put(""+(renderer.getSeriesRendererCount()-1), i);
						} catch (JSONException e) {
							e.printStackTrace();
						}
						}else{
							otherTime += time[i];
							otherRendererToArrayIndex.put(i);
						}
					}
				}
				if(apps.length>0){
					if(otherTime > 0){
						SimpleSeriesRenderer r = new SimpleSeriesRenderer();
						otherTime = Math.round((otherTime/totaltime)*10000.f);
						otherTime /=100;
						categories.add("Other", otherTime);
						r.setColor(otherColor);
						if(!highlighted&&wasClicked){
							r.setHighlighted(wasClicked);
							highlighted=true;
							selectedRenderer =renderer.getSeriesRendererCount();
							selectedApp="";
						}
						renderer.addSeriesRenderer(r);				
					}else if(!highlighted&&wasClicked){
						renderer.getSeriesRendererAt(renderer.getSeriesRendererCount()-1).setHighlighted(highlight);
						highlighted=true;
						selectedRenderer = renderer.getSeriesRendererCount()-1;
						selectedApp="";
					}
				}
				if(wasClicked)
					addDetail(selectedRenderer, detailContainer);
				renderer.setChartTitle(Utilities.getDate(date)+", Total time "+totaltime+" min");
				chartView.repaint();
			}
		}
		private void addDetail(int selectedSeries,int detailViewContainer){
			((LinearLayout)((ScrollView)((LinearLayout) getActivity().findViewById(detailViewContainer)).getChildAt(0)).getChildAt(0)).removeAllViews();
			((LinearLayout)((ScrollView)((LinearLayout) getActivity().findViewById(detailViewContainer)).getChildAt(1)).getChildAt(0)).removeAllViews();
			if(apps.length<1)
				return;
			/*
			((LinearLayout)((LinearLayout) getActivity().findViewById(detailViewContainer)).getChildAt(0)).removeAllViews();
			((LinearLayout)((LinearLayout) getActivity().findViewById(detailViewContainer)).getChildAt(1)).removeAllViews();*/
			LinearLayout appNames = (LinearLayout)((ScrollView)((LinearLayout) getActivity().findViewById(detailViewContainer)).getChildAt(0)).getChildAt(0);		
			if(selectedSeries==renderer.getSeriesRendererCount()-1&&otherRendererToArrayIndex.length()>0){
				selectedApp="";
				String[] sortedArray = new String[otherRendererToArrayIndex.length()];
				for(int i = 0; i<otherRendererToArrayIndex.length(); i++){
					sortedArray[i]=apps[otherRendererToArrayIndex.optInt(i)];
				}
				Arrays.sort(sortedArray);
				for(int i = 0; i<otherRendererToArrayIndex.length(); i++){
				    TextView valueTV = getView(sortedArray[i]);//getView(apps[otherRendererToArrayIndex.optInt(i)]);
				    /*
				    valueTV.setText(apps[otherRendererToArrayIndex.optInt(i)]);
				    valueTV.setLayoutParams(new LayoutParams(
				            LayoutParams.MATCH_PARENT,
				            LayoutParams.WRAP_CONTENT));
				    valueTV.setTextSize(18);*/
				    valueTV.setPadding(10, 5, 10, 5);
				    valueTV.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							String appName = ((TextView)v).getText().toString();
							selectedApp = appName;
							LinearLayout apps = (LinearLayout)v.getParent();
							for(int i=0;i<apps.getChildCount();i++){
								apps.getChildAt(i).setBackgroundResource(0);
							}
							v.setBackgroundResource(android.R.color.holo_blue_dark);
							LinearLayout details = (LinearLayout)((ScrollView)((LinearLayout) v.getParent().getParent().getParent()).getChildAt(1)).getChildAt(0);
							details.removeAllViews();
							JSONObject appTime = getTimesOfApp(appName);
							JSONArray appUsages = appTime.optJSONArray("times");
							if(appUsages==null)
								return;
							/*
							TextView header = new TextView(getActivity());
							header.setText("Total time "+ appTime.optInt("total"));
							header.setLayoutParams(new LayoutParams(
						            LayoutParams.MATCH_PARENT,
						            LayoutParams.WRAP_CONTENT));
							header.setTextSize(18);
							header.setPadding(10, 5, 10, 5);*/
							TextView header = getView("Total time:"+"\n"+"    "+Utilities.getTimeString(appTime.optInt("total")));
							header.setPadding(10, 5, 10, 5);
						    details.addView(header);
						    
							for(int i = 0; i<appUsages.length();i++){
								JSONObject appUsage = appUsages.optJSONObject(i);
								long start = appUsage.optLong("start");
								long duration = appUsage.optLong("duration");
								TextView view = getView("Used at "+ Utilities.getTimeFromTimeStamp(start));
								view.setOnClickListener(new View.OnClickListener() {									
									@Override
									public void onClick(View v) {
										String time = ((TextView)v).getText().toString();
										String times[] = time.split("Used at ");
										times = times[1].split(":");
										for(int i=0;i<times.length;i++){
											Log.d("Time Strings",times[i]);
										}
										int h = Integer.valueOf(times[0]);
										int m = Integer.valueOf(times[1]);
										int s = Integer.valueOf(times[2]);
										int timestamp = h*60*60 + m*60 + s;
										JSONObject jObject = new JSONObject();
										try {
											jObject.put("time", timestamp).put("app", selectedApp ).put("date", date);
										} catch (JSONException e) {
											e.printStackTrace();
										}
										((MainActivity)getActivity()).switchTab(2, jObject);
										Log.d("Time selected",""+timestamp);
										
									}
								});
								/*		
										new TextView(getActivity());
								view.setText("Used at "+ Utilities.getTimeFromTimeStamp(start));
								view.setLayoutParams(new LayoutParams(
							            LayoutParams.MATCH_PARENT,
							            LayoutParams.WRAP_CONTENT));
								view.setTextSize(18);*/
								view.setPadding(10, 5, 10, 0);
								view.setId((i*2));
							    details.addView(view);
							
							    view = getView("    for "+Utilities.getTimeString(duration));/*
							    String durationAsString = Utilities.getTimeString(duration);
							    view.setText("    for "+durationAsString);
							    view.setLayoutParams(new LayoutParams(
							            LayoutParams.MATCH_PARENT,
							            LayoutParams.WRAP_CONTENT));
								view.setTextSize(18);	*/								
							    view.setOnClickListener(new View.OnClickListener() {									
									@Override
									public void onClick(View v) {
										int id = v.getId();
										v = ((LinearLayout)v.getParent()).findViewById(id-1);
										String time = ((TextView)v).getText().toString();
										String times[] = time.split("Used at ");
										times = times[1].split(":");
										for(int i=0;i<times.length;i++){
											Log.d("Time Strings",times[i]);
										}
										int h = Integer.valueOf(times[0]);
										int m = Integer.valueOf(times[1]);
										int s = Integer.valueOf(times[2]);
										int timestamp = h*60*60 + m*60 + s;
										JSONObject jObject = new JSONObject();
										try {
											jObject.put("time", timestamp).put("app", selectedApp ).put("date", date);
										} catch (JSONException e) {
											e.printStackTrace();
										}
										((MainActivity)getActivity()).switchTab(2, jObject);
										Log.d("Time selected",""+timestamp);
										
									}
								});
								view.setPadding(10, 0, 10, 5);
								view.setId((i*2)+1);						    
							    details.addView(view);
							}
							
						}
					});
				    appNames.addView(valueTV);
				}
			}else{
				TextView valueTV = getView(apps[rendererToArrayIndex.optInt(""+selectedSeries)]);
			   /* valueTV.setText(apps[rendererToArrayIndex.optInt(""+selectedSeries)]);
			    valueTV.setLayoutParams(new LayoutParams(
			            LayoutParams.WRAP_CONTENT,
			            LayoutParams.WRAP_CONTENT));
			    valueTV.setTextSize(18);			*/
			    valueTV.setPadding(10, 5, 10, 5);	
			    appNames.addView(valueTV);
			    

				String appName = ((TextView)valueTV).getText().toString();
				selectedApp = appName;
				valueTV.setBackgroundResource(android.R.color.holo_blue_dark);
				LinearLayout details = (LinearLayout)((ScrollView)((LinearLayout) getActivity().findViewById(detailViewContainer)).getChildAt(1)).getChildAt(0);
				details.removeAllViews();
				JSONObject appTime = getTimesOfApp(appName);
				if(appTime == null)
					return;
				JSONArray appUsages = appTime.optJSONArray("times");				
				if(appUsages==null)
					return;
				/*
				TextView header = new TextView(getActivity());
				header.setText("Total time "+ appTime.optInt("total"));
				header.setLayoutParams(new LayoutParams(
			            LayoutParams.MATCH_PARENT,
			            LayoutParams.WRAP_CONTENT));
				header.setTextSize(18);
				header.setPadding(10, 5, 10, 5);*/
				TextView header = getView("Total time:"+"\n"+"    "+Utilities.getTimeString(appTime.optInt("total")));
				header.setPadding(10, 5, 10, 5);
			    details.addView(header);
			    
				for(int i = 0; i<appUsages.length();i++){
					JSONObject appUsage = appUsages.optJSONObject(i);
					long start = appUsage.optLong("start");
					long duration = appUsage.optLong("duration");
					TextView view = getView("Used at "+ Utilities.getTimeFromTimeStamp(start));
					/*		
							new TextView(getActivity());
					view.setText("Used at "+ Utilities.getTimeFromTimeStamp(start));
					view.setLayoutParams(new LayoutParams(
				            LayoutParams.MATCH_PARENT,
				            LayoutParams.WRAP_CONTENT));
					view.setTextSize(18);*/								
					view.setOnClickListener(new View.OnClickListener() {									
						@Override
						public void onClick(View v) {
							String time = ((TextView)v).getText().toString();
							String times[] = time.split("Used at ");
							times = times[1].split(":");
							for(int i=0;i<times.length;i++){
								Log.d("Time Strings",times[i]);
							}
							int h = Integer.valueOf(times[0]);
							int m = Integer.valueOf(times[1]);
							int s = Integer.valueOf(times[2]);
							int timestamp = h*60*60 + m*60 + s;
							JSONObject jObject = new JSONObject();
							try {
								jObject.put("time", timestamp).put("app", selectedApp ).put("date", date);
							} catch (JSONException e) {
								e.printStackTrace();
							}
							((MainActivity)getActivity()).switchTab(2, jObject);
							Log.d("Time selected",""+timestamp);
							
						}
					});
					view.setPadding(10, 5, 10, 0);
					view.setId((i*2));
				    details.addView(view);
				    
				    view = getView("    for "+Utilities.getTimeString(duration));/*
				    String durationAsString = Utilities.getTimeString(duration);
				    view.setText("    for "+durationAsString);
				    view.setLayoutParams(new LayoutParams(
				            LayoutParams.MATCH_PARENT,
				            LayoutParams.WRAP_CONTENT));
					view.setTextSize(18);	*/							    
				    view.setOnClickListener(new View.OnClickListener() {									
						@Override
						public void onClick(View v) {
							int id = v.getId();
							v = ((LinearLayout)v.getParent()).findViewById(id-1);
							String time = ((TextView)v).getText().toString();
							String times[] = time.split("Used at ");
							times = times[1].split(":");
							for(int i=0;i<times.length;i++){
								Log.d("Time Strings",times[i]);
							}
							int h = Integer.valueOf(times[0]);
							int m = Integer.valueOf(times[1]);
							int s = Integer.valueOf(times[2]);
							int timestamp = h*60*60 + m*60 + s;
							JSONObject jObject = new JSONObject();
							try {
								jObject.put("time", timestamp).put("app", selectedApp ).put("date", date);
							} catch (JSONException e) {
								e.printStackTrace();
							}
							((MainActivity)getActivity()).switchTab(2, jObject);
							Log.d("Time selected",""+timestamp);
							
						}
					});
					view.setPadding(10, 0, 10, 5);
					view.setId((i*2)+1);				    
				    details.addView(view);
				}
				
			}
		}
		private JSONObject getTimesOfApp(String appName){
			/**
			 * returns
			 * {
			 * 	"times":[
			 * 		{
			 * 		"start":long
			 * 		"duration":long
			 * 		}
			 * 		...
			 * 			],
			 * 	"total":int
			 * }
			 */
			JSONObject result = new JSONObject();
			JSONArray jArray = data.optJSONArray("result");
			int totalTime = 0;
			if(jArray == null)
				return null;
			for(int i=0; i<jArray.length();i++){
				JSONObject app = jArray.optJSONObject(i);
				if(app.optString("app").equals(appName)){
					JSONArray usages = app.optJSONArray("usage");
					JSONArray output = new JSONArray();
					try {
						for(int j = 0 ; j<usages.length();j++){
							JSONObject usage = usages.optJSONObject(j);
							long start = usage.optLong("start",-1);
							long end = usage.optLong("end",-1);
							if(start!=-1 && end!=-1){
								output.put(
									new JSONObject()
										.put("start", start)
										.put("duration", end-start)
								);
								totalTime += end-start;
							}
						}
						result.put("times", output);
						result.put("total", totalTime);
						return result;
					} catch (JSONException e) {
						e.printStackTrace();
						return null;
					}	
				}					
			}
			return result;
		}
		
		private TextView getView(String headerString){			
			TextView header = new TextView(getActivity());
			header.setText(headerString);
			header.setLayoutParams(new LayoutParams(
		            LayoutParams.MATCH_PARENT,
		            LayoutParams.WRAP_CONTENT));
			header.setTextSize(18);
			header.setTextColor(getResources().getColor(android.R.color.white));
			return header;
		}
	}
}
