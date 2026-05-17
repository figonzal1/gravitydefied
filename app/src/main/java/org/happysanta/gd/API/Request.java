package org.happysanta.gd.API;

import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.happysanta.gd.Helpers.getAppVersion;
import static org.happysanta.gd.Helpers.logDebug;

public class Request {

	private static final ExecutorService executor = Executors.newCachedThreadPool();
	private static final Handler mainHandler = new Handler(Looper.getMainLooper());

	private List<String[]> params;
	private ResponseHandler handler;
	private Future<?> task;
	private String apiURL;

	public Request(String method, List<String[]> params, ResponseHandler handler, boolean useDebugURL) {
		construct(method, params, handler, useDebugURL ? API.DEBUG_URL : API.URL);
	}

	public Request(String method, List<String[]> params, ResponseHandler handler) {
		construct(method, params, handler, API.URL);
	}

	private void construct(String method, List<String[]> params, ResponseHandler handler, String apiURL) {
		this.apiURL = apiURL;

		params.add(new String[]{"v", String.valueOf(API.VERSION)});
		params.add(new String[]{"method", method});
		params.add(new String[]{"app_version", getAppVersion()});
		params.add(new String[]{"app_lang", Locale.getDefault().getDisplayLanguage()});

		this.params = params;
		this.handler = handler;

		go();
	}

	private void go() {
		task = executor.submit(() -> {
			String result = doRequest();
			mainHandler.post(() -> onDone(result));
		});
	}

	public void cancel() {
		if (task != null) {
			task.cancel(true);
			task = null;
		}
	}

	private void onDone(String result) {
		Response response;
		logDebug("API.Request.onDone()");

		try {
			response = new Response(result);
		} catch (APIException e) {
			handler.onError(e);
			return;
		} catch (Exception e) {
			handler.onError(new APIException(result == null ? "Network error" : "JSON parsing error"));
			return;
		}

		if (response != null)
			handler.onResponse(response);
		else
			handler.onError(new APIException("JSON parsing error"));
	}

	private String doRequest() {
		HttpURLConnection connection = null;
		InputStream is = null;

		try {
			StringBuilder body = new StringBuilder();
			for (String[] pair : params) {
				if (body.length() > 0) body.append('&');
				body.append(URLEncoder.encode(pair[0], "UTF-8"));
				body.append('=');
				body.append(URLEncoder.encode(pair[1], "UTF-8"));
			}
			byte[] bodyBytes = body.toString().getBytes("UTF-8");

			connection = (HttpURLConnection) new URL(apiURL).openConnection();
			connection.setRequestMethod("POST");
			connection.setDoOutput(true);
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			connection.setFixedLengthStreamingMode(bodyBytes.length);
			connection.connect();

			OutputStream os = connection.getOutputStream();
			os.write(bodyBytes);
			os.flush();
			os.close();

			is = connection.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"), 8);
			StringBuilder sb = new StringBuilder();

			String line;
			while ((line = reader.readLine()) != null) {
				if (Thread.currentThread().isInterrupted()) break;
				sb.append(line).append("\n");
			}
			return sb.toString();
		} catch (java.lang.Exception e) {
			logDebug("API request failed: " + e.getMessage());
			return null;
		} finally {
			try {
				if (is != null) is.close();
			} catch (IOException e) {
			}
			if (connection != null) connection.disconnect();
		}
	}

}
