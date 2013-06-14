package com.zehjot.smartday.helper;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import com.zehjot.smartday.Config;
import com.zehjot.smartday.MainActivity;
import com.zehjot.smartday.R;

public class Utilities {
	public static String getFileName(String request, JSONObject user,JSONObject jObj,Activity activity){
		String fileName = request;
		
		try {
			fileName += user.getString(activity.getString(R.string.user_pass));
			fileName += user.getString(activity.getString(R.string.user_name));
			fileName += jObj.getString("start");
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}/*
		try {
			fileName += jObj.getString("end");
		} catch (JSONException e) {
			e.printStackTrace();
		}*/
		return Security.sha1(fileName);
	}
	public static long getTimestamp(int year, int month, int day, int h, int m, int s){
		Calendar c = Calendar.getInstance();
		c.set(year, month, day,h,m,s);
		TimeZone tz = c.getTimeZone();
		long date = c.getTimeInMillis();
		date += tz.getOffset(date);
		return date/1000;//c.getTimeInMillis()+seconds*1000+minutes*60000+hour*360000;
	}
	
	public static long getSystemTime(){
		Calendar c = Calendar.getInstance();
		return c.getTimeInMillis();
	}

	public static Boolean writeFile(String filename, String data, Activity activity){
		String encryptedData = Security.encrypt(data);
		FileOutputStream fos;
		try{
			fos = activity.openFileOutput(filename, Context.MODE_PRIVATE);
			fos.write(encryptedData.getBytes());
			fos.close();
		}catch(IOException e){
			showDialog("Error while storing data to file: "+filename, activity);
			return false;
		}		
		return true;
	}
	public static String readFile(String filename, Activity activity){
		FileInputStream fis;
		StringBuilder sb = null;
		String data = null;
		try{
			fis = activity.openFileInput(filename);
			InputStreamReader inputStreamReader = new InputStreamReader(fis);
			BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
			sb = new StringBuilder();
			while ((data = bufferedReader.readLine()) != null) {
				sb.append(data);
			}
			fis.close();
		}catch(IOException e){
			showDialog("File: "+filename+" not found",activity);
			return null;
		}
		data=sb.toString();
		String decryptedData = Security.decrypt(data);
		return decryptedData;
	}
	
	public static void showDialog(String message, Activity activity){
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setMessage(message)
			.setCancelable(false)
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
		           @Override
		           public void onClick(DialogInterface dialog, int id) {
		           }
		       });
		AlertDialog dialog = builder.create();
		if(((MainActivity) activity).isRunning())
			dialog.show();
	}
	public static String getURL(String queryType,String data,JSONObject user, Activity activity){
		if(user==null||activity==null){
			return null;
		}
		try {
		    List<NameValuePair> params = new LinkedList<NameValuePair>();
		    String nonce = Security.getNonce();
    	    if(data != null){
    	    	params.add(new BasicNameValuePair("data", data));
    	    }else{
    	    	data="";
    	    }
	    	params.add(new BasicNameValuePair("nonce", nonce));		    
	    	params.add(new BasicNameValuePair("aid", Config.getAppID()));
			params.add(new BasicNameValuePair("user", user.getString(activity.getString(R.string.user_name))));
			String dataAsURL ="";
			try {
				dataAsURL = URLEncoder.encode(data, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			params.add(new BasicNameValuePair("h", 
				Security.sha1(	
					dataAsURL
					+Config.getAppID()
					+user.getString(activity.getString(R.string.user_name))
					+nonce
					+Config.getAppSecret()
					+user.getString(activity.getString(R.string.user_pass))
				)
			));
			return Config.getDomain()
					+Config.getApiVersion()
					+queryType
					+"?"
					+URLEncodedUtils.format(params, "utf-8");
				
		}catch (JSONException e) {
			showDialog("JSONError in getURL",activity);
			e.printStackTrace();
			return null;
		}
	}
	public static ArrayList<String> jObjValuesToArrayList(JSONObject jObj){
		ArrayList<String> list = new ArrayList<String>();
		JSONArray jArray;
		try {
			jArray = jObj.getJSONArray("result");
			for(int i=0;i < jArray.length();i++ ){
				list.add(jArray.getJSONObject(i).getString("app"));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return list;
	}
}