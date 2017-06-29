package com.example.mapdemo;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class HttpUtil {
	//create HttpClient obj
	public static HttpClient httpClient = new DefaultHttpClient();
	public static final String BASE_URL = 
			//"http://192.168.1.105:8080/erhuoServer/";
			"http://172.16.7.105:8080/erhuoServer/";
			//"http://59.78.45.120:8080/erhuoServer/";
	/* 
	 * 
	 * @param url post request
	 * @return Server respond to the string
	 * @throws Exception
	 */
	
	public static String getRequest(final String url)
	throws Exception
	{
		FutureTask<String> task = new FutureTask<String>(
		new Callable<String>()
		{
			@Override
			public String call() throws Exception
			{
				//create HttpGet obj
				HttpGet get = new HttpGet(url);
				//send GET request
				HttpResponse httpResponse = httpClient.execute(get);
				//if the server returns the response successful
				if(httpResponse.getStatusLine()
					.getStatusCode() == 200)
				{
					//receive the string from the server
					String result = EntityUtils
							.toString(httpResponse.getEntity());
					return result;
				}
				return null;
			}
		});
		new Thread(task).start();
		return task.get();
	}
	
	public static String AnotherpostRequest(final String url,
			final Map<String,String> rawParams)throws Exception
	{
		String addPost = url;
		addPost += setPost(rawParams);
		Log.i("url+map", addPost);
		return getRequest(addPost);
	}
	private static String setPost(Map<String, String> map)
	{
		String post = "?";
		for(String key : map.keySet()){
			post += (key+"="+map.get(key)+"&");
		}
		post = post.substring(0,post.length()-1);
		return post;
	}
	/*
	 * @param url post request
	 * @param params request parameters
	 * @return Server respond to the string
	 * @throws Exception
	 */
	public static String postRequest(final String url,
			final Map<String,String> rawParams)throws Exception
	{
		FutureTask<String> task = new FutureTask<String>(
		new Callable<String>()
		{
			@Override
			public String call() throws Exception
			{
				//create HttpPost obj
				HttpPost post = new HttpPost(url);
				//if there are many params, encapsulate them.
				List<NameValuePair> params =
					new ArrayList<NameValuePair>();
				for(String key : rawParams.keySet())
				{
					//encapsulate the params
					params.add(new BasicNameValuePair(
						key,rawParams.get(key)));
				}
				//set the request params
				post.setEntity(new UrlEncodedFormEntity(
					params,"gbk"));
				//send POST request
				HttpResponse httpResponse = httpClient.execute(post);
				
				//if the server returns the response successful
				if(httpResponse.getStatusLine()
					.getStatusCode() == 200)
				{
					//receive the string from the server
					String result = EntityUtils
							.toString(httpResponse.getEntity());
					return result;
				}
				return null;
			}
		});
		new Thread(task).start();
		return task.get();
	}
}

