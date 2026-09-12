package cl.figonzal.gravitydefied;

import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

/**
 * One-button-one-key touch listener, used by the button control scheme
 * (gamepadLayout in GDActivity). Unlike KeyboardController, each button gets its
 * own listener instance, so Android's split-motion-events dispatch already gives
 * each finger to the right button and multitouch (e.g. gas + lean) just works.
 */
class KeyControlListener implements View.OnTouchListener {

	private final GDActivity gd;
	private final int keyCode;

	KeyControlListener(GDActivity gd, int keyCode) {
		this.gd = gd;
		this.keyCode = keyCode;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
				v.setPressed(true);
				if (Settings.isVibrateOnTouchEnabled()) {
					v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
				}
				gd.gameView.keyPressed(keyCode);
				return true;

			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				v.setPressed(false);
				gd.gameView.keyReleased(keyCode);
				return true;
		}
		return true;
	}
}
