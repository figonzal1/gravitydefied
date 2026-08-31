package cl.figonzal.gravitydefied;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.*;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import cl.figonzal.gravitydefied.Game.*;
import cl.figonzal.gravitydefied.Levels.Loader;
import cl.figonzal.gravitydefied.R;
import cl.figonzal.gravitydefied.Menu.Views.MenuHelmetView;
import cl.figonzal.gravitydefied.Menu.Views.MenuImageView;
import cl.figonzal.gravitydefied.Menu.Views.MenuLinearLayout;
import cl.figonzal.gravitydefied.Menu.Views.MenuTextView;
import cl.figonzal.gravitydefied.Menu.Views.MenuTitleLinearLayout;
import cl.figonzal.gravitydefied.Menu.Views.ObservableScrollView;
import cl.figonzal.gravitydefied.Storage.LevelsDataSource;
import cl.figonzal.gravitydefied.Storage.LevelsManager;
import cl.figonzal.gravitydefied.Settings;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import static cl.figonzal.gravitydefied.Helpers.logDebug;

public class GDActivity extends Activity implements Runnable {

	public static volatile GDActivity shared = null;
	public static final int MENU_TITLE_LAYOUT_TOP_PADDING = 25;
	public static final int MENU_TITLE_LAYOUT_BOTTOM_PADDING = 13;
	public static final int MENU_TITLE_LAYOUT_X_PADDING = 30;
	public static final int GAME_MENU_BUTTON_LAYOUT_WIDTH = 40;
	public static final int GAME_MENU_BUTTON_LAYOUT_HEIGHT = 56;
	public static final int REQUEST_OPEN_MRG = 1001;

	private static final long IMAGES_DELAY = 1000L;
	private static final long IMAGES_DELAY_DEBUG = 100L;

	public int m_longI = 0;

	private boolean wasPaused = false;
	private boolean wasStarted = false;
	private boolean wasDestroyed = false;
	private boolean restartingStarted = false;
	public boolean alive = false;
	public boolean m_cZ = true;
	private boolean menuShown = false;
	public boolean fullResetting = false;
	public boolean exiting = false;

	public GameView gameView = null;
	// public MenuView menuView = null;
	public Loader levelLoader;
	public Physics physEngine;
	public cl.figonzal.gravitydefied.Menu.Menu menu;
	public boolean m_caseZ;
	public int m_nullI;
	public long m_forJ;
	// public long seconds;
	public long startedTime = 0;
	public long finishedTime = 0;
	public long pausedTime = 0;
	public long pausedTimeStarted = 0;
	public long m_byteJ;
	public boolean inited = false;
	public boolean m_ifZ;
	private Thread thread;
	private MenuImageView menuBtn;
	public MenuTitleLinearLayout titleLayout;
	public ObservableScrollView scrollView;
	private FrameLayout frame;
	private MenuLinearLayout menuLayout;
	private KeyboardController keyboardController;
	private boolean isNormalAndroid = true;
	private boolean buttonCoordsCalculated = false;
	public TextView menuTitleTextView;
	private boolean menuReady = false;
	private ArrayList<Command> commands = new ArrayList<Command>();
	private MenuLinearLayout keyboardLayout;
	private MenuLinearLayout gamepadLayout;
	private TiltController tiltController;
	private MenuTextView portedTextView;
	private int buttonHeight = 60;
	private int baseButtonHeight = 60;
	public LevelsManager levelsManager;

	// android.window.OnBackInvokedCallback when registered on API 33+, null otherwise.
	// Typed as Object so verifying GDActivity on pre-Tiramisu devices doesn't try to
	// resolve the API-33 framework class at class load.
	private Object onBackInvokedCallback;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		shared = this;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			registerOnBackInvokedCallback33();
		}

		if (Helpers.isSDK10OrLower()) {
			isNormalAndroid = false;
		}

		if (true) {
			gameView = new GameView(this);

			scrollView = new ObservableScrollView(this);
			scrollView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
			scrollView.setFillViewport(true);
			scrollView.setOnScrollListener(new ObservableScrollView.OnScrollListener() {
				@Override
				public void onScroll(ObservableScrollView scrollView, int x, int y, int oldx, int oldy) {
					if (isMenuShown() && menu != null && menu.currentMenu != null) {
						int h = scrollView.getChildAt(0).getHeight() - scrollView.getHeight();
						double p = 100.0 * y / h;
						if (p > 100f)
							p = 100f;

						menu.currentMenu.onScroll(p);
					}
				}
			});
			scrollView.setVisibility(View.GONE);

			boolean night = Settings.isNightModeEnabled();

			frame = new FrameLayout(this);
			frame.setBackgroundColor(getResources().getColor(night ? R.color.menu_background_night : R.color.menu_background));

			titleLayout = new MenuTitleLinearLayout(this);
			titleLayout.setBackgroundColor(android.graphics.Color.TRANSPARENT);
			titleLayout.setGravity(Gravity.TOP);
			titleLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
			titleLayout.setPadding(
					getResources().getDimensionPixelSize(R.dimen.menu_layout_padding_horizontal),
					getResources().getDimensionPixelSize(R.dimen.menu_title_padding_top),
					getResources().getDimensionPixelSize(R.dimen.menu_layout_padding_horizontal),
					getResources().getDimensionPixelSize(R.dimen.menu_title_padding_bottom));

			menuTitleTextView = new TextView(this);
			menuTitleTextView.setText(getString(R.string.main));
			menuTitleTextView.setTextColor(getResources().getColor(night ? R.color.title_text_night : R.color.title_text));
			menuTitleTextView.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
			menuTitleTextView.setTextSize(24);
			menuTitleTextView.setLineSpacing(0f, 1.1f);
			menuTitleTextView.setLayoutParams(new ViewGroup.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT
			));
			menuTitleTextView.setVisibility(android.view.View.GONE);

			titleLayout.addView(menuTitleTextView);

			scrollView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 1));

			// Keyboard
			if (getString(R.string.screen_type).equals("tablet")) {
				baseButtonHeight = 85;
			} else if (getResources().getDisplayMetrics().density < 1.5) {
				baseButtonHeight = 55;
			}
			buttonHeight = scaledButtonHeight();

			keyboardLayout = buildKeypadLayout(night);
			gamepadLayout = buildGamepadLayout(night);
			tiltController = new TiltController(this);

			hideKeyboardLayout();

			menuBtn = new MenuImageView(this);
			menuBtn.setImageResource(R.drawable.ic_menu);
			menuBtn.setScaleType(ImageView.ScaleType.CENTER);
			menuBtn.setLayoutParams(new FrameLayout.LayoutParams(Helpers.getDp(GAME_MENU_BUTTON_LAYOUT_WIDTH), Helpers.getDp(GAME_MENU_BUTTON_LAYOUT_HEIGHT), Gravity.RIGHT | Gravity.TOP));
			menuBtn.setOnClickListener(new android.view.View.OnClickListener() {
				@Override
				public void onClick(android.view.View v) {
					gameView.showMenu();
				}
			});
			menuBtn.setVisibility(android.view.View.GONE);

			menuLayout = new MenuLinearLayout(this);
			menuLayout.setOrientation(LinearLayout.VERTICAL);
			menuLayout.setLayoutParams(new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.MATCH_PARENT
			));

			portedTextView = new MenuTextView(this);
			portedTextView.setTextSize(15);
			portedTextView.setLineSpacing(0f, 1.2f);
			portedTextView.setText(Helpers.fromHtml(getString(R.string.ported_text)));
			portedTextView.setGravity(Gravity.CENTER);
			portedTextView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
			portedTextView.setPadding(0, 0, 0, Helpers.getDp(10));

			menuLayout.addView(titleLayout);
			menuLayout.addView(scrollView);

			frame.addView(menuLayout);
			frame.addView(keyboardLayout);
			frame.addView(gamepadLayout);
			frame.addView(menuBtn);
			frame.addView(portedTextView);

			gameView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, 1));
			frame.addView(gameView, 0);

			setContentView(frame);

			applyImmersiveMode();

			frame.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
				@Override
				public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
					int left, top, right, bottom;
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
						android.graphics.Insets sys = insets.getInsets(
								WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
						left = sys.left;
						top = sys.top;
						right = sys.right;
						bottom = sys.bottom;
					} else {
						left = insets.getSystemWindowInsetLeft();
						top = insets.getSystemWindowInsetTop();
						right = insets.getSystemWindowInsetRight();
						bottom = insets.getSystemWindowInsetBottom();
					}
					menuLayout.setPadding(left, top, right, 0);
					scrollView.setPadding(0, 0, 0, bottom);
					return insets;
				}
			});

			gameView._doIV(1); // flag for 1st image, as I understand..
			thread = null;
			m_caseZ = false;
			m_nullI = 2;
			m_forJ = 0;
			m_byteJ = 0;
			inited = false;
			m_ifZ = false;
			wasDestroyed = false;
			restartingStarted = false;

			frame.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
				@Override
				public boolean onPreDraw() {
					frame.getViewTreeObserver().removeOnPreDrawListener(this);
					// setButtonsLayoutHeight();
					doStart();
					return true;
				}
			});



		/* gameView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
			@Override
			public boolean onPreDraw() {
				gameView.getViewTreeObserver().removeOnPreDrawListener(this);
				doStart();
				return true;
			}
		}); */

			/* alive = true;
			m_cZ = false;

			Thread.currentThread().setName("main_thread");

			if (thread == null) {
				thread = new Thread(this);
				thread.setName("game_thread");
			} */

			/*synchronized (thread) {
				thread.start();
				try {
					thread.wait();
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}

			for (int i = 1; i <= 4; i++) {
				menu.load(i);
			}

			wasStarted = true;*/
		}
	}

	protected void doStart() {
		alive = true;
		m_cZ = false;

		Thread.currentThread().setName("main_thread");

		if (thread == null) {
			thread = new Thread(this);
			thread.setName("game_thread");
			thread.start();
		}

		wasStarted = true;
	}

	// protected boolean viewDone = false;

	@Override
	public void run() {
		Helpers.logDebug("!!! run()");
		long l1;

		if (!inited) {
			Helpers.logDebug("run(): initing");
			try {
				// Game view
				/* gameView = new GameView(shared);
				gameView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, 1));
				frame.addView(gameView, 0); */

				/* gameView._doIV(1);
				thread = null;
				m_caseZ = false;
				m_nullI = 2;
				m_forJ = 0L;
				seconds = 0L;
				m_byteJ = 0L;
				inited = false;
				m_ifZ = false; */

				long imageDelay = IMAGES_DELAY; // delay of first image
				Thread.yield();

				/*gameView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
					@Override
					public boolean onPreDraw() {
						gameView.getViewTreeObserver().removeOnPreDrawListener(this);
						viewDone = true;
						logDebug("gameView is ready");
						//doStart();
						return true;
					}
				});

				logDebug("before while..");
				while (!viewDone) {
					// Thread.sleep(1);
				}
				logDebug("after while..");*/

				// do we really need this?!
				/*while (gameView == null || gameView.getParent() == null) {
					try {
						Thread.sleep(100);
					} catch (Exception x) {}
				}*/

				MenuHelmetView.clearStaticFields();

				try {
					levelsManager = new LevelsManager();
				} catch (android.database.SQLException e) {
					e.printStackTrace();
					showDatabaseCorruptedDialog();
					return; // init aborted; user decides via the dialog
				}

				try {
					levelLoader = new Loader(levelsManager.getCurrentLevelsFile());
				} catch (Exception e) {
					e.printStackTrace();
					// logDebug("Reset level id now");
					levelsManager.resetId();
					levelsManager.reload();

					levelLoader = new Loader(levelsManager.getCurrentLevelsFile());
				}

				physEngine = new Physics(levelLoader);
				gameView.setPhysicsEngine(physEngine);


				/* synchronized (Thread.currentThread()) {
					Thread.currentThread().notify();
				} */
				menu = new cl.figonzal.gravitydefied.Menu.Menu();
				// menu = null;
				// menu.hideKeyboard();
				for (int i = 1; i <= 4; i++) {
					if (shared != this) return; // replaced by a newer activity; abandon this init
					menu.load(i);
				}

				// menu = new Menu();
				// menu.hideKeyboard();

				/*menu.load(1);
				menu.load(2);
				menu.load(3);

				Runnable createMenuRunnable = new Runnable() {
					@Override
					public void run() {
						menu.load(4);
						synchronized (this) {
							notify();
						}
					}
				};

				synchronized (createMenuRunnable) {
					// logDebug("before runOnUiThread()");
					runOnUiThread(createMenuRunnable);
					try {
						// logDebug("before wait()");
						createMenuRunnable.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}*/

				portedTextView.setVisibility(View.VISIBLE);

				gameView.setMenu(menu);
				gameView._doIIV(-50, 150);
				setMode(1);

				// Show first image
				Helpers.logDebug("show first image");
				long l2;
				for (; imageDelay > 0L; imageDelay -= l2)
					l2 = _avJ();

				// Show second image
				portedTextView.setVisibility(View.GONE);
				Helpers.logDebug("show second image");
				imageDelay = IMAGES_DELAY;
				gameView._doIV(2);
				long l3;
				for (long l4 = imageDelay; l4 > 0L; l4 -= l3)
					l3 = _avJ();

				while (m_longI < 10)
					_avJ();

				gameView._doIV(0);
				Helpers.logDebug("images DONE");
				inited = true;

			} catch (Exception _ex) {
				_ex.printStackTrace();
				// Log.w("GDTR", _ex);
				if (shared != this) {
					Helpers.logDebug("run(): init aborted, this activity was replaced");
					return;
				}
				throw new RuntimeException("init failed: " + _ex, _ex);
			}
		}

		// logDebug("inited, continue");

		restart(false);
		// logDebug("showMenu() now");

		/*if (menu != null) */
		menu.showMenu(0);
		if (/*menu != null && */menu.canStartTrack())
			restart(true);
		l1 = 0L;

		// try {
		Helpers.logDebug("start main loop");
		while (alive && shared == this) {
			/*if (!alive) {
				logDebug("!alive");
				break;
			}*/

			// try {
			if (physEngine._bytevI() != menu._jvI()) {
				int j = gameView._intII(menu._jvI());
				physEngine._doIV(j);
				menu._intIV(j);
			}

			if (menuShown) {
				menu.showMenu(1);
				if (menu.canStartTrack())
					restart(true);
			}

			for (int i1 = m_nullI; i1 > 0 && alive; i1--) {
			/* if (m_ifZ)
				seconds += 20L; */
				if (m_forJ == 0L)
					m_forJ = System.currentTimeMillis();
				int k = 0;
				if (/*physEngine != null && */(k = physEngine._dovI()) == 3 && m_byteJ == 0L) {
					m_byteJ = System.currentTimeMillis() + 3000L;
					gameView.showInfoMessage(getString(R.string.crashed), 3000);
					//m_di.postInvalidate();
					//m_di.serviceRepaints();
				}
				if (m_byteJ != 0L && m_byteJ < System.currentTimeMillis())
					restart(true);
				if (k == 5) {
					finishedTime = System.currentTimeMillis();
					gameView.showInfoMessage(getString(R.string.crashed), 3000);
					//m_di.postInvalidate();
					//m_di.serviceRepaints();
					try {
						long l2 = 1000L;
						if (m_byteJ > 0L)
							l2 = Math.min(m_byteJ - System.currentTimeMillis(), 1000L);
						if (l2 > 0L)
							Thread.sleep(l2);
					} catch (InterruptedException _ex) {
					}
					restart(true);
				} else if (k == 4) {
					// logDebug("k == 4");
					m_forJ = 0;
					// seconds = 0;
					startedTime = 0;
					finishedTime = 0;
					pausedTime = 0;
				} else if (k == 1 || k == 2) {
					finishedTime = System.currentTimeMillis();
					// logDebug("game-run: k = " + k);
				/* if (k == 2)
					seconds -= 10L; */
					goalLoop();
					// menu.setLastTrackTime(seconds / 10L);
					menu.setLastTrackTime((finishedTime - startedTime) / 10);
					menu.showMenu(2);

					if (menu.canStartTrack())
						restart(true);
					if (!alive) {
						Helpers.logDebug("!alive (2)");
						break;
					}
				}
				m_ifZ = k != 4;
				if (m_ifZ && startedTime == 0) {
					startedTime = System.currentTimeMillis();
				}
			}

			if (!alive) {
				Helpers.logDebug("!alive (3)");
				break;
			}

			//try {
			/*if (physEngine != null)*/
			physEngine._charvV();
			long l;
			if ((l = System.currentTimeMillis()) - l1 < 30L) {
				try {
					synchronized (this) {
						wait(Math.max(30L - (l - l1), 1L));
					}
				} catch (InterruptedException interruptedexception) {
				}
				l1 = System.currentTimeMillis();
			} else {
				l1 = l;
			}
			//m_di.postInvalidate();
		/*} catch (Exception exception) {
			exception.printStackTrace();
		}*/
		}
		// } catch (Exception e) {
		//	e.printStackTrace();
		//}

		Helpers.logDebug("game thread finished, destroyApp(false) next");

		// finish();
		destroyApp(false);
		// return;
	}

	@Override
	protected void onResume() {
		Helpers.logDebug("@@@ [GDActivity \"+hashCode()+\"] onResume()");
		super.onResume();
		applyImmersiveMode();
		if (tiltController != null) tiltController.onResume();
		Helpers.logDebug("[GDActivity \"+hashCode()+\"] onResume(), inited = " + inited);
		if (wasPaused && wasStarted) {
			// logDebug("onResume(): wasPaused && wasResumed");
			// start();
			m_cZ = false;
			wasPaused = false;

			// Menu.HelmetRotation.start();
		}
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus) {
			applyImmersiveMode();
		}
	}

	private void applyImmersiveMode() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			WindowManager.LayoutParams params = getWindow().getAttributes();
			params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
			getWindow().setAttributes(params);
		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
			WindowManager.LayoutParams params = getWindow().getAttributes();
			params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
			getWindow().setAttributes(params);
		}
		WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
		WindowInsetsControllerCompat controller =
				WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
		controller.hide(WindowInsetsCompat.Type.systemBars());
		controller.setSystemBarsBehavior(
				WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
	}

	@Override
	protected void onPause() {
		super.onPause();

		Helpers.logDebug("@@@ [GDActivity " + hashCode() + "] onPause()");

		if (tiltController != null) tiltController.onPause();

		wasPaused = true;
		m_cZ = true;
		Helpers.logDebug("inited : " + inited);
		if (!menuShown && inited)
			gameToMenu();

		// menu.helmetRotationStop();
		// Menu.HelmetRotation.stop();
		// if (menu != null)
		// 	menu.saveAll();
		// levelsManager.updateLevelSettings();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Helpers.logDebug("@@@ [GDActivity " + hashCode() + "] onDestroy()");
		destroyApp(false);
	}

	@Override
	protected void onStop() {
		super.onStop();
		Helpers.logDebug("@@@ [GDActivity " + hashCode() + "] onStop()");
	}

	@Override
	protected void onStart() {
		super.onStart();
		Helpers.logDebug("@@@ [GDActivity " + hashCode() + "] onStart()");
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		Helpers.logDebug("@@@ [GDActivity " + hashCode() + "] onRestart()");
	}

	@Override
	@android.annotation.SuppressLint("GestureBackNavigation")
	public void onBackPressed() {
		// On API 33+ the OnBackInvokedCallback registered in onCreate handles back
		// instead — this override only runs on API < 33.
		handleBack();
	}

	private void handleBack() {
		if (gameView != null && menu != null && inited) {
			if (menuShown)
				menu.back();
			else
				gameView.showMenu();
		}
	}

	@android.annotation.TargetApi(Build.VERSION_CODES.TIRAMISU)
	private void registerOnBackInvokedCallback33() {
		android.window.OnBackInvokedCallback cb = new android.window.OnBackInvokedCallback() {
			@Override
			public void onBackInvoked() {
				handleBack();
			}
		};
		onBackInvokedCallback = cb;
		getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
				android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, cb);
	}

	@Override
	public boolean onCreateOptionsMenu(android.view.Menu menu) {
		menu.clear();
		int id = 1;
		for (Command cmd : commands) {
			menu.add(0, id, 0, cmd.title);
			id++;
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		gameView.commandAction(commands.get(item.getItemId() - 1));
		return true;
	}

	public void setMode(int j) {
		physEngine._byteIV(j);
	}

	public boolean isMenuShown() {
		return menuShown;
	}

	// @UiThread
	public void setMenu(final LinearLayout layout) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				scrollView.removeAllViews();
				if (layout.getParent() != null) {
					((ViewManager) layout.getParent()).removeView(layout);
				}
				scrollView.addView(layout);
			}
		});
	}

	public void goalLoop() {
		if (!alive) {
			return;
		}

		long l1 = 0L;
		if (!physEngine.m_NZ)
			gameView.showInfoMessage(getString(R.string.wheelie), 1000);
		else
			gameView.showInfoMessage(getString(R.string.finished1), 1000);
		for (long l2 = System.currentTimeMillis() + 1000L; l2 > System.currentTimeMillis(); gameView.postInvalidate()) {
			if (menuShown) {
				//m_di.postInvalidate();
				return;
			}
			for (int j = m_nullI; j > 0; j--)
				if (physEngine._dovI() == 5)
					try {
						long l3;
						if ((l3 = l2 - System.currentTimeMillis()) > 0L)
							Thread.sleep(l3);
						return;
					} catch (InterruptedException _ex) {
						return;
					}

			physEngine._charvV();
			long l;
			if ((l = System.currentTimeMillis()) - l1 < 30L) {
				try {
					synchronized (this) {
						wait(Math.max(30L - (l - l1), 1L));
					}
				} catch (InterruptedException interruptedexception) {
				}
				l1 = System.currentTimeMillis();
			} else {
				l1 = l;
			}
		}
	}

	public void restart(boolean flag) {
		// logDebug("[GDActivity] restart()");
		if (!alive) {
			return;
		}

		physEngine._doZV(true);
		// logDebug("[GDActivity] restart(): 1");
		m_forJ = 0;
		// seconds = 0;
		startedTime = 0;
		finishedTime = 0;
		pausedTime = 0;
		m_byteJ = 0;
		if (flag)
			gameView.showInfoMessage(levelLoader.getLevelName(menu.getSelectedLevel(), menu.getSelectedTrack()), 3000);
		// logDebug("[GDActivity] restart(): 2");
		gameView._casevV();
		// logDebug("[GDActivity] restart(): 3");
	}

	public void destroyApp(final boolean restart) {
		if (wasDestroyed) {
			return;
		}

		wasDestroyed = true;
		alive = false;

		final GDActivity self = this;

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Helpers.logDebug("[GDActivity " + self.hashCode() + "] destroyApp()");
				inited = false;
				m_caseZ = true;

				synchronized (gameView) {
					destroyResources();

					if (exiting || restart) {
						finish();
					}

					if (restart) {
						doRestartApp();
					}
				}
			}
		});
	}

	private void destroyResources() {
		Helpers.logDebug("[GDActivity " + hashCode() + "]  destroyResources()");

		// if (thread != null) thread.interrupt();
		if (gameView != null) gameView.destroy();

		menuShown = false;
		if (menu != null) {
			if (!fullResetting) menu.saveAll();
			menu.destroy();
		}

		if (levelsManager != null) levelsManager.closeDataSource();
	}

	public int getButtonsLayoutHeight() {
		return buttonHeight * 3 + KeyboardController.PADDING * 2;
	}

	// Device-only variant (ignores the user's keyboard-size preference) so the in-game
	// camera offset doesn't shift when the user changes Options > Keyboard size.
	public int getButtonsLayoutHeightBase() {
		return baseButtonHeight * 3 + KeyboardController.PADDING * 2;
	}

	private int scaledButtonHeight() {
		return baseButtonHeight * Settings.getKeyboardScale() / 100;
	}

	private MenuLinearLayout buildKeypadLayout(boolean night) {
		int[] buttonResources = {
				R.drawable.btn_br, R.drawable.btn_br, R.drawable.btn_b,
				R.drawable.btn_br, R.drawable.btn_br, R.drawable.btn_b,
				R.drawable.btn_r, R.drawable.btn_r, R.drawable.btn_n
		};

		MenuLinearLayout layout = new MenuLinearLayout(this, true);
		layout.setOrientation(LinearLayout.VERTICAL);

		keyboardController = new KeyboardController(this);

		for (int i = 0; i < 3; i++) {
			LinearLayout row = new LinearLayout(this);
			row.setPadding(Helpers.getDp(KeyboardController.PADDING), i == 0 ? Helpers.getDp(KeyboardController.PADDING) : 0, Helpers.getDp(KeyboardController.PADDING), 0);
			row.setOrientation(LinearLayout.HORIZONTAL);
			row.setBackgroundColor(getResources().getColor(night ? R.color.keyboard_background_night : R.color.keyboard_background));
			for (int j = 0; j < 3; j++) {
				LinearLayout btn = new LinearLayout(this);
				TextView btnText = new TextView(this);
				btnText.setText(String.valueOf(i * 3 + j + 1));
				btnText.setTextColor(getResources().getColor(night ? R.color.keyboard_button_text_night : R.color.keyboard_button_text));
				btnText.setTextSize(17);
				btn.setBackgroundResource(buttonResources[i * 3 + j]);
				btn.addView(btnText, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
				btn.setGravity(Gravity.CENTER);
				btn.setWeightSum(1);

				row.addView(btn, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, Helpers.getDp(buttonHeight), 1));

				keyboardController.addButton(btn, j, i);
			}

			layout.addView(row);
		}

		layout.setGravity(Gravity.BOTTOM);
		layout.setPadding(0, 0, 0, Helpers.getDp(KeyboardController.PADDING));
		layout.setOnTouchListener(keyboardController);
		layout.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));

		return layout;
	}

	// Button control scheme: lean left/right and gas/brake pedals — identical bar in menus and
	// in-game. Emits the same ASCII key codes as keypad cells '2'/'4'/'6'/'8' (Keyset 1's
	// accelerate/lean/brake mapping and the menu's UP/LEFT/RIGHT/DOWN), so it needs no changes
	// below GameView.keyPressed/keyReleased. No OK button: tapping a menu row already fires
	// KEY_FIRE (ClickableMenuElement), and NameInputMenuScreen is confirmed via the back button.
	//
	// The tilt scheme reuses this same bar for its pedals: full-width GAS/FRENO in-game (tilting
	// the device leans the bike, see TiltController), the usual four buttons in menus (tilting
	// can't navigate a menu without changing values by accident) — see rebuildGamepadBar().
	private MenuLinearLayout buildGamepadLayout(boolean night) {
		MenuLinearLayout layout = new MenuLinearLayout(this, false);
		layout.setOrientation(LinearLayout.HORIZONTAL);

		fillGamepadLayout(layout, night, false, false);

		layout.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));

		return layout;
	}

	// pedalsOnly drops the lean buttons and stretches the pedals to full width (tilt scheme,
	// in-game). Otherwise Settings.getButtonLayout() decides which side gets the arrows vs. the
	// pedals. inGame swaps the pedal labels: GAS/FRENO while driving, up/down arrows while they're
	// really just moving the menu selection. Called again (after removeAllViews) whenever any of
	// this changes, so the bar updates immediately.
	private void fillGamepadLayout(MenuLinearLayout layout, boolean night, boolean pedalsOnly, boolean inGame) {
		int rowsHeightDp = Helpers.getDp(buttonHeight * 3);
		int pad = Helpers.getDp(KeyboardController.PADDING);
		layout.setPadding(pad, pad, pad, pad);

		int gasRes = inGame ? R.string.ctrl_gas : R.string.ctrl_nav_up;
		int brakeRes = inGame ? R.string.ctrl_brake : R.string.ctrl_nav_down;

		LinearLayout pedalContainer = new LinearLayout(this);
		pedalContainer.setOrientation(LinearLayout.VERTICAL);
		pedalContainer.addView(buildControlButton(night, gasRes, '2'), new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
		pedalContainer.addView(buildControlButton(night, brakeRes, '8'), new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

		if (pedalsOnly) {
			layout.addView(pedalContainer, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, rowsHeightDp));
			return;
		}

		View leanBack = buildControlButton(night, R.string.ctrl_nav_left, '4');
		View leanForward = buildControlButton(night, R.string.ctrl_nav_right, '6');

		boolean arrowsRight = Settings.getButtonLayout() == Settings.BUTTON_LAYOUT_ARROWS_RIGHT;
		if (arrowsRight) layout.addView(pedalContainer, new LinearLayout.LayoutParams(0, rowsHeightDp, 2));
		layout.addView(leanBack, new LinearLayout.LayoutParams(0, rowsHeightDp, 1.5f));
		layout.addView(leanForward, new LinearLayout.LayoutParams(0, rowsHeightDp, 1.5f));
		if (!arrowsRight) layout.addView(pedalContainer, new LinearLayout.LayoutParams(0, rowsHeightDp, 2));
	}

	private LinearLayout buildControlButton(boolean night, int textRes, int keyCode) {
		LinearLayout btn = new LinearLayout(this);
		TextView btnText = new TextView(this);
		btnText.setText(getString(textRes));
		btnText.setTextColor(getResources().getColor(night ? R.color.keyboard_button_text_night : R.color.keyboard_button_text));
		btnText.setTextSize(17);
		btn.setBackgroundResource(R.drawable.btn_br);
		btn.addView(btnText, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
		btn.setGravity(Gravity.CENTER);
		btn.setOnTouchListener(new KeyControlListener(this, keyCode));
		return btn;
	}

	// @UiThread
	// updateMenuMargin: false while the user is still dragging the slider — the pad rescales
	// live for preview, but the scrollView's bottom margin (and thus its content) only jumps
	// to match once the drag ends, so the slider itself doesn't move under the finger.
	public void applyKeyboardSize(final boolean updateMenuMargin) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (!Helpers.isActivityAlive()) return;

				buttonHeight = scaledButtonHeight();

				for (int i = 0; i < keyboardLayout.getChildCount(); i++) {
					LinearLayout row = (LinearLayout) keyboardLayout.getChildAt(i);
					for (int j = 0; j < row.getChildCount(); j++) {
						View btn = row.getChildAt(j);
						ViewGroup.LayoutParams params = btn.getLayoutParams();
						params.height = Helpers.getDp(buttonHeight);
						btn.setLayoutParams(params);
					}
				}

				if (updateMenuMargin && (keyboardLayout.getVisibility() == android.view.View.VISIBLE
						|| gamepadLayout.getVisibility() == android.view.View.VISIBLE))
					showKeyboardLayout();
				else
					rebuildGamepadBar(); // live preview during the drag: resizes without touching the margin
			}
		});
	}

	// @UiThread
	public void hideKeyboardLayout() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				keyboardLayout.setVisibility(android.view.View.GONE);
				gamepadLayout.setVisibility(android.view.View.GONE);

				LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) scrollView.getLayoutParams();
				params.setMargins(0, 0, 0, 0);
				scrollView.setLayoutParams(params);
			}
		});
	}

	// @UiThread
	// Button/Tilt schemes: same bar (lean buttons + pedals, or just pedals) in menus and
	// in-game — no context switching for the button scheme; the tilt scheme is the one
	// exception, see rebuildGamepadBar(). NameInputMenuScreen (letter entry, key-only — see its
	// performAction) works unmodified either way.
	public void showKeyboardLayout() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				rebuildGamepadBar();

				boolean gamepad = Settings.getControlScheme() != Settings.CONTROL_SCHEME_KEYPAD;

				keyboardLayout.setVisibility(gamepad ? android.view.View.GONE : android.view.View.VISIBLE);
				gamepadLayout.setVisibility(gamepad ? android.view.View.VISIBLE : android.view.View.GONE);

				LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) scrollView.getLayoutParams();
				params.setMargins(0, 0, 0, Helpers.getDp(getButtonsLayoutHeight()));
				scrollView.setLayoutParams(params);
			}
		});
	}

	// Clears latched keys before rebuilding: a finger resting on a button that's about to
	// disappear never gets an ACTION_UP, which would otherwise leave the key stuck down. Tilt
	// can't navigate a menu (it would change values by accident while just looking around), so
	// pedalsOnly (full-width pedals, no lean buttons) is only true for tilt, in-game.
	private void rebuildGamepadBar() {
		boolean pedalsOnly = Settings.getControlScheme() == Settings.CONTROL_SCHEME_TILT
				&& !menuShown && tiltController.isAvailable();

		gameView._avV();
		physEngine._nullvV();
		gamepadLayout.removeAllViews();
		fillGamepadLayout(gamepadLayout, Settings.isNightModeEnabled(), pedalsOnly, !menuShown);
	}

	// @UiThread
	// Button/Tilt schemes emit Keyset 1's codes, so switching to either forces inputOption to 0
	// (and back to the user's keyset otherwise); also clears keys latched by the previous scheme.
	public void applyControlScheme() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (!Helpers.isActivityAlive()) return;

				int scheme = Settings.getControlScheme();
				boolean isKeypad = scheme == Settings.CONTROL_SCHEME_KEYPAD;
				gameView.setInputOption(isKeypad ? Settings.getInputOption() : 0);
				tiltController.setEnabled(scheme == Settings.CONTROL_SCHEME_TILT);

				rebuildGamepadBar();

				// Not reachable before the boot splash finishes (inited==false the first time
				// Menu.load(3) calls this) — skip so the splash never gets a controls bar.
				if (!inited) return;

				if (menuShown && !Settings.isKeyboardInMenuEnabled()) hideKeyboardLayout();
				else showKeyboardLayout();
			}
		});
	}

	public void addCommand(Command cmd) {
		if (!commands.contains(cmd))
			commands.add(cmd);
		if (isNormalAndroid)
			invalidateOptionsMenu();
	}

	public void removeCommand(Command cmd) {
		commands.remove(cmd);
		if (isNormalAndroid)
			invalidateOptionsMenu();
	}

	public void gameToMenu() {
		Helpers.logDebug("gameToMenu()");

		if (gameView == null) {
			Helpers.logDebug("gameToMenu(): gameView == null");
			return;
		}

		pausedTimeStarted = System.currentTimeMillis();

		gameView.removeMenuCommand();
		menuShown = true;
		// menu.helmetRotationStart();
		// Menu.HelmetRotation.start();
		if (menu != null)
			menu.addCommands();

		// hideKeyboardLayout();
		if (!Settings.isKeyboardInMenuEnabled())
			hideKeyboardLayout();
		else
			showKeyboardLayout();

		gameToMenuUpdateUi();
	}

	// @UiThread
	protected void gameToMenuUpdateUi() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				menuBtn.setVisibility(android.view.View.GONE);
				menuTitleTextView.setVisibility(android.view.View.VISIBLE);
				scrollView.setVisibility(android.view.View.VISIBLE);
			}
		});
	}

	public void menuToGame() {
		Helpers.logDebug("menuToGame()");

		if (pausedTimeStarted > 0 && startedTime > 0) {
			pausedTime += (System.currentTimeMillis() - pausedTimeStarted);
			pausedTimeStarted = 0;
		}

		if (menu != null) menu.removeCommands();
		menuShown = false;
		// menu.helmetRotationStop();
		// Menu.HelmetRotation.stop();
		if (gameView != null) gameView.addMenuCommand();
		showKeyboardLayout();
		// menu.showKeyboard();

		menuToGameUpdateUi();
	}

	// @UiThread
	protected void menuToGameUpdateUi() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				menuBtn.setVisibility(android.view.View.VISIBLE);
				menuTitleTextView.setVisibility(android.view.View.GONE);
				scrollView.setVisibility(android.view.View.GONE);

				// Clear menu
				scrollView.removeAllViews();
				menuTitleTextView.setText("");
				menu.menuDisabled = true;
				// menu.currentMenu = null;
			}
		});
	}

	public void scrollTextMenuUp() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				int y = scrollView.getScrollY();
				scrollView.scrollTo(0, y - Helpers.getDp(20));
			}
		});
	}

	public void scrollTextMenuDown() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				int y = scrollView.getScrollY();
				scrollView.scrollTo(0, y + Helpers.getDp(20));
			}
		});
	}

	public void scrollToView(final View view) {
		final GDActivity gd = Helpers.getGDActivity();
		final ObservableScrollView scrollView = gd.scrollView;

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Rect scrollBounds = new Rect();
				scrollView.getHitRect(scrollBounds);

				if (!view.getLocalVisibleRect(scrollBounds)
						|| scrollBounds.height() < view.getHeight()) {
					int top = view.getTop(),
							height = view.getHeight(),
							scrollY = scrollView.getScrollY(),
							scrollHeight = scrollView.getHeight(),
							y = top;

					/*logDebug("top = " + top);
					logDebug("height = " + height);
					logDebug("scrollY = " + scrollY);
					logDebug("scrollHeight = " + scrollHeight);*/

					if (top < scrollY) {
						// scroll to y
					} else if (top + height > scrollY + scrollHeight) {
						y = top + height - scrollHeight;
						if (y < 0)
							y = 0;
					}

					// logDebug("View is not visible, scroll to y = " + y);
					scrollView.scrollTo(0, y);
				} else {
					// logDebug("View is visible");
				}
			}
		});
	}

	private long _avJ() {
		m_longI++;
		long l = System.currentTimeMillis();
		if (m_longI < 1 || m_longI > 10) { // maybe < 1 not needed?
			m_longI--;
			try {
				Thread.sleep(100L);
			} catch (InterruptedException _ex) {
			}
		}
		return System.currentTimeMillis() - l;
	}

	public void restartApp() {
		if (!restartingStarted) {
			destroyApp(true);
			restartingStarted = true;
		}
	}

	// @UiThread
	private void showDatabaseCorruptedDialog() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Helpers.showConfirm(
						getString(R.string.e_database_damaged_title),
						getString(R.string.e_database_damaged_message),
						new Runnable() { // OK: delete the db and restart
							@Override
							public void run() {
								LevelsDataSource.deleteDatabase(GDActivity.this);
								restartApp();
							}
						},
						new Runnable() { // Cancel: close
							@Override
							public void run() {
								finish();
							}
						});
			}
		});
	}

	private void doRestartApp() {
		Intent mStartActivity = new Intent(this, GDActivity.class);
		int mPendingIntentId = 123456;
		PendingIntent mPendingIntent = PendingIntent.getActivity(this, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
		AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
	}

	public void pickMrgFile() {
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("*/*");
		startActivityForResult(intent, REQUEST_OPEN_MRG);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_OPEN_MRG && resultCode == RESULT_OK && data != null) {
			Uri uri = data.getData();
			if (uri != null) menu.installMrgFromUri(uri);
		}
	}

	private class ButtonCoords {

		public int x = 0;
		public int y = 0;
		public int w = 0;
		public int h = 0;

		public ButtonCoords() {
		}

		public boolean in(float x, float y) {
			if (x < this.x || x > this.x + this.w || y < this.y || y > this.y + this.h) {
				return false;
			}
			return true;
		}

	}

}