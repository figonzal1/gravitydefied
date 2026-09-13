package cl.figonzal.gravitydefied.Menu.Views;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import static cl.figonzal.gravitydefied.Helpers.runOnUiThread;

public class MenuLinearLayout extends LinearLayout {

	boolean interceptTouchEvents = false;

	public MenuLinearLayout(Context context) {
		super(context);
	}

	public MenuLinearLayout(Context context, boolean interceptTouchEvents) {
		super(context);
		this.interceptTouchEvents = interceptTouchEvents;
	}

	@Override
	public void removeAllViews() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				MenuLinearLayout.super.removeAllViews();
			}
		});
	}

	@Override
	public void setVisibility(final int visibility) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				MenuLinearLayout.super.setVisibility(visibility);
			}
		});
	}

	@Override
	public void addView(final View view) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				MenuLinearLayout.super.addView(view);
			}
		});
	}

	// Same UI-thread marshaling as addView(View)/removeAllViews() above - MenuScreen.addItem(item,
	// index)/removeItemAt(index) (mid-list insert/remove for async-populated rows) call these two-
	// and one-arg overloads, and without this override they'd run immediately on whatever thread
	// calls them (game_thread, for the synchronous part of Menu.saveCompletedTrack()) instead of
	// being queued in order after the plain addView()/removeAllViews() calls already queued ahead
	// of them - Activity.runOnUiThread() runs synchronously when already on the UI thread, so
	// callers on the main thread (e.g. a Ranking response) see no extra delay.
	@Override
	public void addView(final View view, final int index) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				MenuLinearLayout.super.addView(view, index);
			}
		});
	}

	@Override
	public void removeView(final View view) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				MenuLinearLayout.super.removeView(view);
			}
		});
	}

	@Override
	public void setPadding(final int left, final int top, final int right, final int bottom) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				MenuLinearLayout.super.setPadding(left, top, right, bottom);
			}
		});
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent evt) {
		return interceptTouchEvents;
	}

}
