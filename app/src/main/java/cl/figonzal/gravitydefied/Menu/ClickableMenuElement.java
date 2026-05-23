package cl.figonzal.gravitydefied.Menu;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import cl.figonzal.gravitydefied.Menu.Views.MenuHelmetView;
import cl.figonzal.gravitydefied.Menu.Views.MenuTextView;
import cl.figonzal.gravitydefied.R;
import cl.figonzal.gravitydefied.Settings;

import static cl.figonzal.gravitydefied.Helpers.getDp;
import static cl.figonzal.gravitydefied.Helpers.getGDActivity;

public class ClickableMenuElement
		implements MenuElement {

	public static final int PADDING_TOP = 5;
	protected View textView;
	protected String text;
	protected View.OnTouchListener onTouchListener;
	protected LinearLayout layout;
	protected MenuHelmetView helmet;
	protected OnMenuElementHighlightListener onMenuElementHighlightListener = null;
	protected boolean isHighlighted = false;
	protected Thread originalThread = null;
	protected boolean disabled = false;

	public ClickableMenuElement() {
	}

	public ClickableMenuElement(String text) {
		this.text = text;
		originalThread = Thread.currentThread();

		createAllViews();
	}

	protected boolean inViewBounds(View view, int x, int y) {
		Rect rect = new Rect();
		int location[] = new int[2];

		view.getDrawingRect(rect);
		view.getLocationOnScreen(location);
		rect.offset(location[0], location[1]);

		return rect.contains(x, y);
	}

	protected void createAllViews() {
		final ClickableMenuElement self = this;
		Context context = getGDActivity();

		layout = new LinearLayout(context);
		layout.setOrientation(LinearLayout.HORIZONTAL);
		layout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
		layout.setBackgroundResource(R.drawable.menu_item_ripple);

		helmet = new MenuHelmetView(context);
		helmet.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT));

		/*if (!isSDK11OrHigher()) {
			helmet.setMeasuredHeight(true);
		}*/

		onTouchListener = new View.OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent motionEvent) {
				if (disabled) return false;

				switch (motionEvent.getAction()) {
					case MotionEvent.ACTION_DOWN:
						view.setSelected(true);
						view.setPressed(true);
						helmet.setShow(true);

						if (onMenuElementHighlightListener != null)
							onMenuElementHighlightListener.onElementHighlight(self);

						setHighlighted(true);
						break;

					case MotionEvent.ACTION_CANCEL:
					case MotionEvent.ACTION_UP:
						view.setSelected(false);
						view.setPressed(false);

						if (motionEvent.getAction() == MotionEvent.ACTION_UP && inViewBounds(view, (int) motionEvent.getRawX(), (int) motionEvent.getRawY())) {
							performAction(MenuScreen.KEY_FIRE);
						}

						setHighlighted(false);
						break;

					case MotionEvent.ACTION_MOVE:
						if (!inViewBounds(view, (int) motionEvent.getRawX(), (int) motionEvent.getRawY())) {
							view.setSelected(false);
							view.setPressed(false);
							setHighlighted(false);
						} else {
							view.setSelected(true);
							view.setPressed(true);
							setHighlighted(true);
						}
						break;
				}
				return true;
			}
		};

		textView = createMainView();

		layout.addView(helmet);
		layout.addView(textView);
		layout.setOnTouchListener(onTouchListener);
	}

	protected View createMainView() {
		Context context = getGDActivity();
		MenuTextView mtv = new MenuTextView(context);
		mtv.setText(getTextForView());
		mtv.setTextColor(defaultColorStateList());
		mtv.setLayoutParams(new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
		));
		mtv.setPadding(0, getDp(PADDING_TOP), 0, getDp(PADDING_TOP));

		return mtv;
	}

	@SuppressWarnings("deprecation")
	protected ColorStateList defaultColorStateList() {
		boolean night = Settings.isNightModeEnabled();
		int textColor = getGDActivity().getResources().getColor(
				night ? R.color.menu_text_primary_night : R.color.menu_text_primary);
		int highlight = getGDActivity().getResources().getColor(R.color.menu_highlight);
		int[][] states = new int[][]{
				new int[]{android.R.attr.state_pressed},
				new int[]{android.R.attr.state_focused},
				new int[]{android.R.attr.state_selected},
				new int[]{}
		};
		return new ColorStateList(states, new int[]{highlight, highlight, highlight, textColor});
	}

	@Override
	public boolean isSelectable() {
		return true;
	}

	@Override
	public View getView() {
		return layout;
	}

	protected MenuTextView getMenuTextView() {
		return (MenuTextView) textView;
	}

	@Override
	public void setText(String text) {
		this.text = text;
		updateViewText();
	}

	public String getText() {
		return text;
	}

	protected void updateViewText() {
		if (textView != null && textView instanceof MenuTextView)
			((MenuTextView) textView).setTextOnUiThread(getTextForView());
	}

	protected String getTextForView() {
		return text;
	}

	@Override
	public void performAction(int k) {

	}

	public void setOnHighlightListener(OnMenuElementHighlightListener listener) {
		onMenuElementHighlightListener = listener;
	}

	public void showHelmet() {
		helmet.setShow(true);
	}

	private void setHighlighted(boolean highlighted) {
		isHighlighted = highlighted;
		onHighlightChanged();
	}

	protected void onHighlightChanged() {
	}

	public boolean isDisabled() {
		return disabled;
	}

}
