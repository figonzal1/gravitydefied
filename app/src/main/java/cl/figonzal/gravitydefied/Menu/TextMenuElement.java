package cl.figonzal.gravitydefied.Menu;

// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) fieldsfirst ansi 

import android.content.Context;
import android.text.Spanned;
import cl.figonzal.gravitydefied.Helpers;
import android.text.SpannedString;
import android.text.util.Linkify;
import android.view.View;
import android.view.ViewGroup;
import cl.figonzal.gravitydefied.Menu.Views.MenuTextView;
import cl.figonzal.gravitydefied.R;
import cl.figonzal.gravitydefied.Settings;

import static cl.figonzal.gravitydefied.Helpers.getGDActivity;

public class TextMenuElement
		implements MenuElement {

	protected Spanned spanned;
	protected MenuTextView textView;

	public TextMenuElement(String text) {
		this.spanned = SpannedString.valueOf(text);
		textView = createTextView();
	}

	public TextMenuElement(Spanned text) {
		this.spanned = text;
		textView = createTextView();
	}

	protected MenuTextView createTextView() {
		Context activity = getGDActivity();

		MenuTextView textView = new MenuTextView(activity);
		textView.setText(spanned);
		textView.setTextColor(activity.getResources().getColor(
				Settings.isNightModeEnabled() ? R.color.menu_text_primary_night : R.color.menu_text_primary));
		textView.setLineSpacing(0f, 1.5f);
		textView.setLayoutParams(new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
		));

		Linkify.addLinks(textView, Linkify.WEB_URLS);
		textView.setLinksClickable(true);

		return textView;
	}

	@Override
	public View getView() {
		return textView;
	}

	public String getText() {
		return spanned.toString();
	}

	@Override
	public void setText(String text) {
		this.spanned = Helpers.fromHtml(text);
		textView.setTextOnUiThread(spanned);
	}

	@Override
	public boolean isSelectable() {
		return false;
	}

	@Override
	public void performAction(int k) {
	}

}
