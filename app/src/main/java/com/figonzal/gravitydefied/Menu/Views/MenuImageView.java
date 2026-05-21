package com.figonzal.gravitydefied.Menu.Views;

import android.content.Context;
import android.widget.ImageView;

import static com.figonzal.gravitydefied.Helpers.runOnUiThread;

public class MenuImageView extends ImageView {

	public MenuImageView(Context context) {
		super(context);
	}

	@Override
	public void setImageResource(final int resid) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				MenuImageView.super.setImageResource(resid);
			}
		});
	}

	@Override
	public void setVisibility(final int visibility) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				MenuImageView.super.setVisibility(visibility);
			}
		});
	}

}
