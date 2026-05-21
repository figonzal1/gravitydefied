package com.figonzal.gravitydefied.Menu;

import android.text.Spanned;
import com.figonzal.gravitydefied.Menu.Views.MenuTextView;

public class BigTextMenuElement
		extends TextMenuElement {

	public static final int TEXT_SIZE = 19;

	public BigTextMenuElement(String s) {
		super(s);
		createTextView();
		setTextParams(textView);
	}

	public BigTextMenuElement(Spanned s) {
		super(s);
		createTextView();
		setTextParams(textView);
	}

	protected static void setTextParams(MenuTextView textView) {
		textView.setTextSize(TEXT_SIZE);
		textView.setLineSpacing(0f, 1.2f);
	}

}
