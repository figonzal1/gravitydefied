package com.figonzal.gravitydefied.Menu.Views;

import android.content.Context;
import android.graphics.Typeface;
import android.widget.TextView;

import static com.figonzal.gravitydefied.Helpers.runOnUiThread;

public class MenuTextView extends TextView {

	protected boolean isAttached = false;

	public MenuTextView(Context context) {
		super(context);
		super.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
		super.setTextSize(15);
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		isAttached = true;
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		isAttached = false;
	}

	@Override
	public boolean isAttachedToWindow() {
		return isAttached;
	}

	public void setTextOnUiThread(final CharSequence sequence) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				MenuTextView.super.setText(sequence);
			}
		});
	}

	@Override
	public void setTextSize(final float size) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				MenuTextView.super.setTextSize(size);
			}
		});
	}

	@Override
	public void setTypeface(final Typeface typeface) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				MenuTextView.super.setTypeface(typeface);
			}
		});
	}

	@Override
	public void setVisibility(final int visibility) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				MenuTextView.super.setVisibility(visibility);
			}
		});
	}

}
