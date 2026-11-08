package cl.figonzal.gravitydefied.Menu;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import cl.figonzal.gravitydefied.Menu.Views.MenuTextView;
import cl.figonzal.gravitydefied.R;

import static cl.figonzal.gravitydefied.Helpers.getDp;
import static cl.figonzal.gravitydefied.Helpers.getGDActivity;

public class SliderMenuElement
		extends ClickableMenuElement
		implements MenuElement {

	protected int min;
	protected int max;
	protected int step;
	protected int value;
	protected MenuHandler handler;
	protected MenuTextView label;
	protected SeekBar seekBar;
	protected boolean dragging = false;

	public SliderMenuElement(String text, int value, int min, int max, int step, MenuHandler handler) {
		this.text = text;
		this.min = min;
		this.max = max;
		this.step = step;
		this.value = value;
		this.handler = handler;

		createAllViews();
	}

	@Override
	@SuppressWarnings("deprecation")
	protected View createMainView() {
		Context context = getGDActivity();

		LinearLayout container = new LinearLayout(context);
		container.setOrientation(LinearLayout.VERTICAL);
		container.setLayoutParams(new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
		));
		container.setPadding(0, getDp(PADDING_TOP), 0, getDp(PADDING_TOP));

		label = new MenuTextView(context);
		label.setText(getTextForView());
		label.setTextColor(defaultColorStateList());

		seekBar = new SeekBar(context);
		seekBar.setPadding(0, seekBar.getPaddingTop(), 0, seekBar.getPaddingBottom());
		// AbsSeekBar normally lets the thumb hang half its width off each edge (mThumbOffset =
		// intrinsic width / 2), which is what clipped it once padding was removed above.
		// Zeroing the offset keeps the whole thumb inside the view at both min and max.
		seekBar.setThumbOffset(0);
		seekBar.setMax((max - min) / step);
		seekBar.setProgress((value - min) / step);
		ColorStateList tint = ColorStateList.valueOf(context.getResources().getColor(R.color.menu_highlight));
		seekBar.setProgressTintList(tint);
		seekBar.setThumbTintList(tint);
		seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (!fromUser) return;
				value = min + progress * step;
				label.setTextOnUiThread(getTextForView());
				handler.handleAction(SliderMenuElement.this);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				dragging = true;
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				dragging = false;
				handler.handleAction(SliderMenuElement.this);
			}
		});

		container.addView(label);
		container.addView(seekBar);

		return container;
	}

	@Override
	protected void updateViewText() {
		if (label != null) label.setTextOnUiThread(getTextForView());
	}

	@Override
	protected String getTextForView() {
		return text + ": " + value + "%";
	}

	@Override
	public void performAction(int k) {
		switch (k) {
			case MenuScreen.KEY_LEFT:
				setValue(value - step);
				break;
			case MenuScreen.KEY_RIGHT:
				setValue(value + step);
				break;
		}
	}

	private void setValue(int newValue) {
		if (newValue < min) newValue = min;
		if (newValue > max) newValue = max;
		if (newValue == value) return;

		value = newValue;
		seekBar.setProgress((value - min) / step);
		label.setTextOnUiThread(getTextForView());
		handler.handleAction(this);
	}

	public int getValue() {
		return value;
	}

	public boolean isDragging() {
		return dragging;
	}

}
