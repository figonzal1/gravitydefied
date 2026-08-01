package cl.figonzal.gravitydefied.API;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static cl.figonzal.gravitydefied.Helpers.getGDActivity;

public class DownloadFile {

	private static final ExecutorService executor = Executors.newCachedThreadPool();
	private static final Handler mainHandler = new Handler(Looper.getMainLooper());

	private String urlString;
	private DownloadHandler handler;
	private FileOutputStream output;
	private Future<?> task;
	private PowerManager.WakeLock lock;

	public DownloadFile(String url, FileOutputStream output) {
		this.urlString = url;
		this.output = output;
	}

	public DownloadFile(String url, FileOutputStream output, DownloadHandler handler) {
		this(url, output);
		this.handler = handler;
	}

	public void setDownloadHandler(DownloadHandler handler) {
		this.handler = handler;
	}

	public void start() {
		PowerManager pm = (PowerManager) getGDActivity().getSystemService(Context.POWER_SERVICE);
		lock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
		lock.acquire();
		handler.onStart();

		task = executor.submit(() -> {
			Throwable error = doDownload();
			mainHandler.post(() -> {
				if (lock != null && lock.isHeld()) lock.release();
				handler.onFinish(error);
			});
		});
	}

	public void cancel() {
		if (task != null) {
			task.cancel(true);
			task = null;
		}
		if (lock != null && lock.isHeld()) {
			lock.release();
		}
		lock = null;
	}

	private Throwable doDownload() {
		InputStream input = null;
		HttpURLConnection connection = null;

		try {
			URL url = new URL(urlString);
			connection = (HttpURLConnection) url.openConnection();
			connection.connect();

			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
				return new IOException("Server returned HTTP " + connection.getResponseCode()
						+ " " + connection.getResponseMessage());
			}

			int fileLength = connection.getContentLength();
			input = connection.getInputStream();

			byte[] data = new byte[4096];
			long total = 0;
			int count;
			while ((count = input.read(data)) != -1) {
				if (Thread.currentThread().isInterrupted()) {
					input.close();
					return null;
				}

				total += count;

				if (fileLength > 0) {
					final int pr = (int) (total * 100 / fileLength);
					mainHandler.post(() -> handler.onProgress(pr));
				}

				output.write(data, 0, count);
			}
		} catch (Exception e) {
			return e;
		} finally {
			try {
				if (output != null) output.close();
				if (input != null) input.close();
			} catch (IOException ignored) {
			}
			if (connection != null) connection.disconnect();
		}

		return null;
	}

}
