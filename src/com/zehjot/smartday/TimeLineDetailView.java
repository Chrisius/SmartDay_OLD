package com.zehjot.smartday;

import java.util.Arrays;
import java.util.Comparator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.zehjot.smartday.data_access.DataSet;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;

public class TimeLineDetailView extends View {
	private JSONObject jObj;
	private Paint mTextPaint;
	private float textSize;
	private JSONObject colors;
	private float totalDuration;
	private float longestDuration;
	private JSONObject[] orderedApps;
	private int pxLongestWord = 0;
	private int maxBarLength;
	private float yOffset;
	private float xOffset;

	private Paint mRectanglePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private String selectedApp;
	
	private GestureDetector tapDetector;
	
	public TimeLineDetailView(Context context) {
		super(context);
		textSize = 18;
		yOffset = textSize/3.f;
		xOffset = 20;
		
		mTextPaint = new Paint();
		mTextPaint.setTextSize(textSize);
		mTextPaint.setColor(getResources().getColor(android.R.color.white));
		
		tapDetector = new GestureDetector(getContext(), new TapListener());
		
		selectedApp ="";
	}
/*
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		//The Specified Mode (Exactly=hard coded pixels, at_most=match_parent, unspecified=wrap_content)
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);
		
		int width;
		int height;
		
		switch (widthMode) {
		case MeasureSpec.EXACTLY:
			width = widthSize;
			break;
		case MeasureSpec.AT_MOST:
			width = widthSize;
			break;
		case MeasureSpec.UNSPECIFIED:
			width = 0;
			break;
		default:
			width = 0;
			break;
		}
		
		switch (heightMode) {
		case MeasureSpec.EXACTLY:
			height = heightSize;
			break;
		case MeasureSpec.AT_MOST:
			height = Math.min(heightSize,300);
			break;
		case MeasureSpec.UNSPECIFIED:
			height = 300;
			break;
		default:
			height = 300;
			break;
		}	
		setMeasuredDimension(width, height);
	}
*/	
	@Override
	protected void onDraw(Canvas canvas) {
		float xpad = (float) (getPaddingLeft()+getPaddingRight());
		float ypad = (float) (getPaddingTop()+getPaddingBottom())+yOffset/2.f;
		if(maxBarLength<0){
			maxBarLength = (int)(getWidth()/2.f)+maxBarLength; // width available after after setData
		}
		String appName;
		float relativeBarLength;
		for(int i=0;i<orderedApps.length;i++){
			if(orderedApps[i].optString("app").equals(selectedApp)){
				mRectanglePaint.setColor(getResources().getColor(android.R.color.holo_blue_light));
				canvas.drawRect(
						xpad+xOffset-5, 
						ypad+(i)*(textSize+yOffset)+Math.round(textSize*0.2f+0.5)-yOffset/2.f,
						xpad+xOffset+pxLongestWord, 
						ypad+(i+1)*textSize+(yOffset)*i+Math.round(textSize*0.2f+0.5)+yOffset/2.f,
						mRectanglePaint);
			}

			float y=ypad+(i+1)*textSize+i*yOffset;
			canvas.drawText(orderedApps[i].optString("app", "Error"), xpad+20, y, mTextPaint);
			
			appName = orderedApps[i].optString("app", "Error");
			relativeBarLength = orderedApps[i].optLong("duration")/longestDuration;
			mRectanglePaint.setColor(colors.optInt(appName));
			canvas.drawRect(
					xpad+pxLongestWord+xOffset,
					ypad+(i)*(textSize+yOffset)+Math.round(textSize*0.2f+0.5),
					xpad+pxLongestWord+xOffset+relativeBarLength*maxBarLength,
					ypad+(i+1)*textSize+(yOffset)*i+Math.round(textSize*0.2f+0.5),//crazy way to find top and bottom of drawn Text
					mRectanglePaint);
			float percent = ((float)orderedApps[i].optLong("duration")/totalDuration)*100.f;
			percent = Math.round(percent*100.f);
			percent /= 100.f;
			canvas.drawText(percent+"%", xpad+20+10+pxLongestWord+relativeBarLength*maxBarLength, ypad+(i+1)*textSize+(yOffset)*i, mTextPaint);
		}
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		tapDetector.onTouchEvent(event);
		return true;
	}
	public void setData(JSONObject jObj){
		if(jObj == null)
			return;
		this.jObj = jObj;
		JSONObject selectedApps = DataSet.getInstance(getContext()).getSelectedApps();
		/**
		 * Get App Bar Length
		 */
		try{
			JSONArray apps = jObj.getJSONArray("result");
			totalDuration=0;
			longestDuration=0;
			int numberSelectedApps=0;
			for(int i=0;i<apps.length();i++){
				if(selectedApps.optBoolean(apps.getJSONObject(i).getString("app"),true))
					numberSelectedApps += 1;
			}
			int j=0;
			orderedApps = new JSONObject[numberSelectedApps];
			for(int i=0;i<apps.length();i++){
				if(selectedApps.optBoolean(apps.getJSONObject(i).getString("app"),true)){
					totalDuration += apps.getJSONObject(i).optLong("duration", 0);
					orderedApps[j] = (new JSONObject()
						.put("app", apps.getJSONObject(i).getString("app"))
						.put("duration", apps.getJSONObject(i).optLong("duration", 0))
					);
					j++;
					
					Rect bounds = new Rect();
					String text = apps.getJSONObject(i).getString("app");
					mTextPaint.getTextBounds(text, 0, text.length(), bounds);
					if(bounds.width()>pxLongestWord){
						pxLongestWord = bounds.width();
					}
					
					if(apps.getJSONObject(i).optLong("duration", 0)>longestDuration){
						longestDuration = apps.getJSONObject(i).optLong("duration", 0);
					}
				}
			}
			Arrays.sort(orderedApps, new Comparator<JSONObject>() {
				@Override
				public int compare(JSONObject lhs, JSONObject rhs) {
					int i= ((Long)rhs.optLong("duration", 0)).compareTo(lhs.optLong("duration",0));
					return i;
				}
			});
			
			for(int i=0;i<orderedApps.length;i++){
				float length = orderedApps[i].getLong("duration")/longestDuration;
				orderedApps[i].put("barLength",length);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		Rect bounds = new Rect();
		String text = "99.99%";
		mTextPaint.getTextBounds(text, 0, text.length(), bounds);
		int percentageSize = bounds.width();
		int offset = 10 ;
		
		maxBarLength = -pxLongestWord-percentageSize-offset-(int)xOffset;
		/**
		 * GetColors for Bars
		 */
		if(colors==null)
			colors=new JSONObject();
		JSONObject rawObj =	DataSet.getInstance(getContext()).getColorsOfApps(jObj);
		try {
			JSONArray jArray = rawObj.getJSONArray("colors");
			String appName;
			int appColor;
			JSONObject color;
			for(int i = 0; i<jArray.length();i++){
				color = jArray.getJSONObject(i);
				appName = color.getString("app");
				appColor = color.getInt("color");
				colors.put(appName, appColor);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		getParent().requestLayout();
		if(orderedApps!=null){
			int height = (int) ((orderedApps.length)*textSize+(yOffset)*(orderedApps.length+1));
			this.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, height));
			//requestLayout();
		}
		invalidate();
	}
	
	private class TapListener extends GestureDetector.SimpleOnGestureListener{
		@Override
		public boolean onDown(MotionEvent e) {
			return true;
		}
		
		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
			return true;
		}
		@Override
		public boolean onDoubleTap(MotionEvent e) {
			setId(1);
			LinearLayout linearLayout = (LinearLayout)getParent().getParent();
			((TimeLineView)linearLayout.getChildAt(0)).selectApp("");
			linearLayout.removeView(linearLayout.getChildAt(1));
			return true;
		}
		
		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			String app = getAppAtPos(e);
			selectApp(app);
			LinearLayout linearLayout = (LinearLayout)getParent().getParent();
			((TimeLineView)linearLayout.getChildAt(0)).selectApp(app);
			addDetails();
			return true;
		}

		private void addDetails() {
			// TODO Auto-generated method stub
			
		}
	}
	public void selectApp(String app){
		selectedApp = app;
		invalidate();
	}
	private String getAppAtPos(MotionEvent e){
		float ypad = (float) (getPaddingTop()+getPaddingBottom())+yOffset/2.f;
		float y = e.getY();
		for(int i=0;i<orderedApps.length;i++){
			float start = ypad+(i)*(textSize+yOffset)+Math.round(textSize*0.2f+0.5)-yOffset/2.f;
			float end = ypad+(i+1)*textSize+(yOffset)*i+Math.round(textSize*0.2f+0.5)+yOffset/2.f;
			if(y>=start && y<=end){
				return orderedApps[i].optString("app");
			}
			//canvas.drawText(orderedApps[i].optString("app", "Error"), xpad+20, ypad+(i+1)*textSize+(yOffset)*i, mTextPaint);
		}
		return "";
	}
}
