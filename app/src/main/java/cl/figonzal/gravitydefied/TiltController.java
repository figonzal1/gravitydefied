package cl.figonzal.gravitydefied;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Tilt control scheme: roll the device to lean the bike, read from TYPE_ACCELEROMETER rather
 * than the gyroscope — a gyroscope measures angular *velocity*, so getting an absolute lean
 * angle out of it means integrating over time and drifting within seconds. At rest the
 * accelerometer already gives the gravity vector, which is exactly the absolute angle needed.
 * Emits the same '4'/'6' codes as the button scheme's lean buttons (Game/GameView.keyPressed),
 * so Game/, Levels/ and Physics stay untouched.
 *
 * Registration follows two independent triggers — the selected control scheme and the activity
 * lifecycle — so it's tracked as two flags rather than calling register/unregister directly from
 * both places, which would risk registering twice or leaving the sensor listening in background.
 */
class TiltController implements SensorEventListener {

	// Roll angle (degrees) that starts a lean, per Settings.TILT_SENSITIVITY_*.
	private static final float[] THRESHOLDS_DEG = {22f, 15f, 9f};
	// Once leaning, the angle must fall this many degrees below the threshold before it
	// releases — prevents jitter (rapid press/release) when holding right at the threshold.
	private static final float HYSTERESIS_DEG = 4f;

	private final GDActivity gd;
	private final SensorManager sensorManager;
	private final Sensor accelerometer;

	private boolean schemeSelected = false;
	private boolean activityResumed = false;
	private boolean registered = false;

	private int heldKey = 0; // '4', '6', or 0 (centered)

	TiltController(GDActivity gd) {
		this.gd = gd;
		sensorManager = (SensorManager) gd.getSystemService(Context.SENSOR_SERVICE);
		accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	}

	boolean isAvailable() {
		return accelerometer != null;
	}

	void setEnabled(boolean enabled) {
		schemeSelected = enabled;
		updateRegistration();
	}

	void onResume() {
		activityResumed = true;
		updateRegistration();
	}

	void onPause() {
		activityResumed = false;
		updateRegistration();
	}

	private void updateRegistration() {
		boolean shouldRegister = schemeSelected && activityResumed && accelerometer != null;
		if (shouldRegister == registered) return;
		registered = shouldRegister;

		if (registered) {
			sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
		} else {
			sensorManager.unregisterListener(this);
			apply(0); // don't leave the bike leaning if the sensor stops mid-tilt
		}
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (gd.isMenuShown()) {
			apply(0);
			return;
		}

		float roll = (float) Math.toDegrees(Math.atan2(event.values[0], event.values[1]));
		float limit = THRESHOLDS_DEG[Settings.getTiltSensitivity()];
		if (heldKey != 0) limit -= HYSTERESIS_DEG;

		apply(roll > limit ? '4' : roll < -limit ? '6' : 0);
	}

	private void apply(int want) {
		if (want == heldKey) return;
		if (heldKey != 0) gd.gameView.keyReleased(heldKey);
		if (want != 0) gd.gameView.keyPressed(want);
		heldKey = want;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}
}
