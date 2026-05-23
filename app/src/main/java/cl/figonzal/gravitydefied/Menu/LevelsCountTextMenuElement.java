package cl.figonzal.gravitydefied.Menu;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import cl.figonzal.gravitydefied.Menu.Views.MenuImageView;
import cl.figonzal.gravitydefied.Menu.Views.MenuLinearLayout;
import cl.figonzal.gravitydefied.Menu.Views.MenuTextView;
import cl.figonzal.gravitydefied.R;

import static cl.figonzal.gravitydefied.Helpers.getDp;
import static cl.figonzal.gravitydefied.Helpers.getGDActivity;

public class LevelsCountTextMenuElement
		extends BigTextMenuElement {

	protected static final int PADDING_LEFT = 4;
	protected static final int PADDING_RIGHT = 8;
	protected static final int MARGIN_RIGHT = 3;

	protected int tracks[];

	protected MenuLinearLayout layout;
	protected MenuImageView tracksImages[];
	protected MenuTextView tracksTexts[];

	public LevelsCountTextMenuElement(String s, int easy, int medium, int hard) {
		super(s);

		tracks = new int[3];
		tracks[0] = easy;
		tracks[1] = medium;
		tracks[2] = hard;

		createViews();
	}

	protected void createViews() {
		Context context = getGDActivity();

		layout = new MenuLinearLayout(context);
		layout.setOrientation(LinearLayout.HORIZONTAL);
		layout.setLayoutParams(new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT
		));

		tracksImages = new MenuImageView[3];
		for (int i = 0; i < 3; i++) {
			tracksImages[i] = new MenuImageView(context);
			tracksImages[i].setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT));
		}

		tracksImages[0].setImageResource(R.drawable.levels_wheel0);
		tracksImages[0].setColorFilter(context.getResources().getColor(R.color.menu_icon_tint), android.graphics.PorterDuff.Mode.SRC_IN);
		tracksImages[1].setImageResource(R.drawable.levels_wheel1);
		tracksImages[2].setImageResource(R.drawable.levels_wheel2);

		// Tracks texts
		tracksTexts = new MenuTextView[3];
		for (int i = 0; i < 3; i++) {
			tracksTexts[i] = new MenuTextView(context);
			setTextParams(tracksTexts[i]);

			tracksTexts[i].setText(String.valueOf(tracks[i]));
			tracksTexts[i].setPadding(getDp(PADDING_LEFT), 0, getDp(PADDING_RIGHT), 0);
		}

		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.setMargins(0, 0, getDp(MARGIN_RIGHT), 0);

		textView.setLayoutParams(params);

		layout.addView(textView);

		// Add tracks to layout
		for (int i = 0; i < 3; i++) {
			layout.addView(tracksImages[i]);
			layout.addView(tracksTexts[i]);
		}
	}

	@Override
	public View getView() {
		return layout;
	}
}
