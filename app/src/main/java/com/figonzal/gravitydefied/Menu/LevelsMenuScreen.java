package com.figonzal.gravitydefied.Menu;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.figonzal.gravitydefied.GDActivity;
import com.figonzal.gravitydefied.Menu.Views.MenuLinearLayout;
import com.figonzal.gravitydefied.R;
import com.figonzal.gravitydefied.Storage.Level;
import com.figonzal.gravitydefied.Storage.LevelsManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.figonzal.gravitydefied.Helpers.getGDActivity;
import static com.figonzal.gravitydefied.Helpers.getGDView;
import static com.figonzal.gravitydefied.Helpers.getGameMenu;
import static com.figonzal.gravitydefied.Helpers.getLevelsManager;
import static com.figonzal.gravitydefied.Helpers.getString;
import static com.figonzal.gravitydefied.Helpers.logDebug;

public class LevelsMenuScreen extends MenuScreen {

	protected static final ExecutorService executor = Executors.newCachedThreadPool();
	protected static final Handler mainHandler = new Handler(Looper.getMainLooper());

	enum Statuses {NORMAL, DOWNLOADING, ERROR}

	protected final static int ERROR_COLOR = 0xff777777;

	protected Statuses status = Statuses.NORMAL;
	protected int savedScrollY = 0;

	protected Vector elements;
	protected ArrayList<Level> levels;

	protected FrameLayout progressWrap;
	protected ProgressBar progressBar;

	protected MenuLinearLayout listLayout;
	protected TextMenuElement errorText;
	protected Future<?> addElements = null;
	protected boolean leftFromScreen = false;

	public LevelsMenuScreen(String title, MenuScreen navTarget) {
		super(title, navTarget);

		elements = new Vector();
		levels = new ArrayList<>();

		Context context = getGDActivity();

		// Create progress
		progressWrap = new FrameLayout(context);
		progressWrap.setLayoutParams(new LinearLayout.LayoutParams(
				FrameLayout.LayoutParams.MATCH_PARENT,
				FrameLayout.LayoutParams.MATCH_PARENT
		));

		progressBar = new ProgressBar(context);
		progressBar.setIndeterminate(true);

		// Create error
		errorText = new TextMenuElement(getString(R.string.download_error));
		TextView errorTextView = (TextView) errorText.getView();

		errorTextView.setTextColor(ERROR_COLOR);
		errorTextView.setLayoutParams(new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.MATCH_PARENT
		));
		errorTextView.setGravity(Gravity.CENTER);

		listLayout = new MenuLinearLayout(context);
		listLayout.setOrientation(LinearLayout.VERTICAL);
		listLayout.setLayoutParams(new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.MATCH_PARENT
		));

		layout.addView(listLayout);

		// List view
		// listView = new ListView(context);
		/*listView.setLayoutParams(new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT
		));*/

		// adapter = new LevelsAdapter(context, levels);
		// listView.setAdapter(adapter);

		// layout.addView(listView);
	}

	public int addListItem(MenuElement item) {
		elements.addElement(item);
		listLayout.addView(item.getView());

		if (item instanceof ClickableMenuElement)
			((ClickableMenuElement) item).setOnHighlightListener(this);

		return elements.size() - 1;
	}

	public void setStatus(Statuses status) {
		this.status = status;

		getGDView().invalidate();
	}

	protected void clearList() {
		elements.removeAllElements();
		levels.clear();
		listLayout.removeAllViews();
		lastHighlighted = null;
		// System.gc();
	}

	protected void showError(String error) {
		clearList();
		errorText.setText(error);
		addListItem(errorText);

		setStatus(Statuses.ERROR);
	}

	protected void showLoading() {
		// clearList();

		if (elements.isEmpty()) {
			if (progressBar.getParent() == listLayout) {
				listLayout.removeView(progressBar);
			} else if (progressBar.getParent() != progressWrap) {
				progressWrap.addView(progressBar, new FrameLayout.LayoutParams(
						FrameLayout.LayoutParams.WRAP_CONTENT,
						FrameLayout.LayoutParams.WRAP_CONTENT,
						Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL
				));
			}

			listLayout.addView(progressWrap);
		} else {
			if (progressBar.getParent() == progressWrap) {
				progressWrap.removeView(progressBar);
			} else if (progressBar.getParent() != listLayout) {
				listLayout.addView(progressBar, new LinearLayout.LayoutParams(
						LinearLayout.LayoutParams.MATCH_PARENT,
						LinearLayout.LayoutParams.WRAP_CONTENT,
						Gravity.CENTER_HORIZONTAL
				));
			}
		}

		// setStatus(Statuses.DOWNLOADING);
	}

	protected void hideLoading() {
		if (progressBar.getParent() == listLayout) {
			listLayout.removeView(progressBar);
		} else if (progressBar.getParent() == progressWrap) {
			// progressWrap.setVisibility(GameView.GONE);
			listLayout.removeView(progressWrap);
		}
	}

	public void highlightFirstElement() {
		for (int i = 0; i < elements.size(); i++) {
			if (elements.elementAt(i) instanceof LevelMenuElement) {
				highlightElementAt(i);
				break;
			}
		}
	}

	public void highlightElementAt(int index) {
		LevelMenuElement item = null;
		try {
			item = (LevelMenuElement) elements.elementAt(index);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		item.showHelmet();
		lastHighlighted = item;
		selectedIndex = index;
	}

	protected void load() {
		clearList();
		loadLevels();
	}

	protected void loadLevels() {

	}

	@Override
	public void onShow() {
		super.onShow();
		GDActivity activity = getGDActivity();

		if (leftFromScreen) {
			clearList();
			leftFromScreen = false;
		}

		switch (status) {
			case DOWNLOADING:
				break;

			case NORMAL:
				if (elements.isEmpty()) {
					load();
				}
				break;
		}

		if (lastHighlighted != null) {
			lastHighlighted.showHelmet();
		}

		if (savedScrollY != 0) {
			activity.scrollView.scrollTo(0, savedScrollY);
		}
	}

	@Override
	public void onHide(MenuScreen newMenu) {
		GDActivity activity = getGDActivity();

		if (newMenu != getGameMenu().levelScreen) {
			if (addElements != null) {
				addElements.cancel(true);
				addElements = null;
			}

			savedScrollY = 0;
			status = Statuses.NORMAL;

			hideLoading();
			clearList();

			leftFromScreen = true;
		} else {
			savedScrollY = activity.scrollView.getScrollY();
		}
	}

	protected boolean hideDate() {
		return false;
	}

	protected LevelsMenuScreen getThis() {
		return this;
	}

	public void reloadLevels() {
		if (addElements != null) {
			addElements.cancel(true);
			addElements = null;
		}

		clearList();
		loadLevels();
	}

	public LevelMenuElement getElementByLevelId(long id, long apiId) {
		for (Object _el : elements) {
			LevelMenuElement el = (LevelMenuElement) _el;

			if ((id > 0 && el.level.getId() == id) || apiId > 0 && el.level.getApiId() == apiId)
				return el;
		}

		return null;
	}

	public void deleteElement(LevelMenuElement el) {
		View view = el.getView();
		listLayout.removeView(view);

		int index = elements.indexOf(el);

		elements.remove(el);
		levels.remove(el.level);

		if (el == lastHighlighted) {
			index--;
			if (index < 0)
				index = 0;

			highlightElementAt(index);
		}
	}

	@Override
	public void performAction(int k) {
		logDebug("LevelsMenuScreen.performAction: k = " + k);
		int from = 0;
		switch (k) {
			default:
				if (selectedIndex != -1) {
					for (int i = selectedIndex; i < elements.size(); i++) {
						MenuElement item;
						if ((item = (MenuElement) elements.elementAt(i)) != null && item.isSelectable()) {
							item.performAction(k);
							return;
						}
					}
				}
				break;

			case KEY_UP:
				if (selectedIndex > 0) {
					from = selectedIndex - 1;
				} else {
					from = elements.size() - 1;
				}

				for (int i = from; i >= 0; i--) {
					MenuElement el = (MenuElement) elements.elementAt(i);
					if (!(el instanceof LevelMenuElement)) {
						continue;
					}

					highlightElementAt(i);
					scrollToItem(el);
					break;
				}
				break;

			case KEY_DOWN:
				if (selectedIndex < elements.size() - 1) {
					from = selectedIndex + 1;
				} else {
					from = 0;
				}

				for (int i = from; i < elements.size(); i++) {
					MenuElement el = (MenuElement) elements.elementAt(i);
					if (!(el instanceof LevelMenuElement)) {
						continue;
					}

					highlightElementAt(i);
					scrollToItem(el);
					break;
				}
				break;
		}
	}

	protected void scrollToItem(int index) {
		LevelMenuElement el = (LevelMenuElement) elements.elementAt(index);
		// logDebug(el);
		scrollToItem(el);
	}

	protected Future<?> submitAddElements(Level[] _levels, Runnable onComplete) {
		return executor.submit(() -> {
			boolean checkInstalled = getThis() instanceof DownloadLevelsMenuScreen;
			boolean checkActive = getThis() instanceof InstalledLevelsMenuScreen;

			ArrayList<Long> ids;
			HashMap<Long, Long> installed = null;
			LevelsManager levelsManager = getLevelsManager();

			if (checkInstalled) {
				ids = new ArrayList<>();
				for (Level level : _levels) {
					ids.add(level.getApiId());
				}
				installed = getGDActivity().levelsManager.findInstalledLevels(ids);
			}

			boolean alreadyHl = false;

			for (Level level : _levels) {
				if (Thread.currentThread().isInterrupted()) {
					clearList();
					return;
				}

				LevelMenuElement el = new LevelMenuElement(level, getThis());
				boolean toHl = false;

				if (hideDate())
					el.setShowDate(false);

				if (checkInstalled && installed.containsKey(level.getApiId())) {
					level.setId(installed.get(level.getApiId()));
					el.setInstalled(true);
				}
				if (checkActive && level.getId() == levelsManager.getCurrentId()) {
					el.setActive(true);
					toHl = true;
				}

				if (!Thread.currentThread().isInterrupted()) {
					int index = addListItem(el);
					if (toHl && !alreadyHl) {
						highlightElementAt(index);
						alreadyHl = true;
					}
					if (lastHighlighted == null)
						highlightFirstElement();
				}
			}

			levels.addAll(Arrays.asList(_levels));
			mainHandler.post(() -> {
				if (onComplete != null) onComplete.run();
			});
		});
	}

}
