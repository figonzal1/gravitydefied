package cl.figonzal.gravitydefied.Menu;

import android.text.Spanned;
import cl.figonzal.gravitydefied.Menu.Views.MenuTextView;

public class BigTextMenuElement
		extends TextMenuElement {

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
		textView.setLineSpacing(0f, 1.2f);
	}

}
