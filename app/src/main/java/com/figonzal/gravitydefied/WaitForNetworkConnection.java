package com.figonzal.gravitydefied;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.figonzal.gravitydefied.Helpers.isOnline;
import static com.figonzal.gravitydefied.Helpers.logDebug;

public class WaitForNetworkConnection {

	private static final ExecutorService executor = Executors.newCachedThreadPool();
	private static final Handler mainHandler = new Handler(Looper.getMainLooper());

	private Future<?> task;

	public void execute(Object... params) {
		final Runnable callback = (Runnable) params[1];
		task = executor.submit(() -> {
			while (!isOnline()) {
				logDebug("Waiting for network...");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
			logDebug("Network OK, callback.run() now...");
			mainHandler.post(callback);
		});
	}

	public void cancel() {
		if (task != null) {
			task.cancel(true);
			task = null;
		}
	}

}
