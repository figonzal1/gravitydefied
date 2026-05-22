package cl.figonzal.gravitydefied.Menu;

import android.content.Context;
import android.text.SpannableString;
import cl.figonzal.gravitydefied.Helpers;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import cl.figonzal.gravitydefied.Callback;
import cl.figonzal.gravitydefied.GDActivity;
import cl.figonzal.gravitydefied.Menu.Views.LevelNameLeadingMarginSpan2;
import cl.figonzal.gravitydefied.Menu.Views.MenuImageView;
import cl.figonzal.gravitydefied.Menu.Views.MenuLinearLayout;
import cl.figonzal.gravitydefied.Menu.Views.MenuRelativeLayout;
import cl.figonzal.gravitydefied.Menu.Views.MenuTextView;
import cl.figonzal.gravitydefied.R;
import cl.figonzal.gravitydefied.Storage.Level;
import cl.figonzal.gravitydefied.Storage.LevelsManager;

import static cl.figonzal.gravitydefied.Helpers.getDp;
import static cl.figonzal.gravitydefied.Helpers.getGDActivity;
import static cl.figonzal.gravitydefied.Helpers.getGameMenu;
import static cl.figonzal.gravitydefied.Helpers.getLevelsManager;
import static cl.figonzal.gravitydefied.Helpers.getString;
import static cl.figonzal.gravitydefied.Helpers.logDebug;
import static cl.figonzal.gravitydefied.Helpers.showConfirm;

public class LevelMenuElement
		extends ClickableMenuElement
		implements MenuHandler {

	protected static final int PADDING_TOP = 7;
	protected static final int PADDING_BOTTOM = 7;

	protected static final int INSTALLED_MARGIN = 15;
	protected static final int ACTIVE_MARGIN = 21;

	protected Level level;

	protected MenuTextView textView;
	protected MenuLinearLayout mainLayout;
	protected MenuRelativeLayout nameLayout;
	protected MenuTextView tracksCountTextView;
	protected MenuScreen screen;
	protected MenuImageView installedIcon = null;
	protected MenuImageView activeIcon = null;
	protected boolean installed = false;
	protected boolean active = false;
	protected boolean showDate = true;

	public LevelMenuElement() {
	}

	public LevelMenuElement(Level level, MenuScreen screen) {
		this.level = level;
		this.screen = screen;

		createAllViews();
	}

	@Override
	protected View createMainView() {
		Context context = getGDActivity();

		mainLayout = new MenuLinearLayout(context);
		mainLayout.setOrientation(LinearLayout.VERTICAL);
		mainLayout.setPadding(0, getDp(PADDING_TOP), 0, getDp(PADDING_BOTTOM));

		nameLayout = new MenuRelativeLayout(context);
		// nameLayout.setOrientation(LinearLayout.HORIZONTAL);

		// Text
		textView = new MenuTextView(context);
		// textView.setText(level.getName());
		updateNameLine();
		textView.setTextColor(context.getResources().getColorStateList(R.drawable.menu_item_color));
		textView.setLineSpacing(0f, 1.1f);
		textView.setLayoutParams(new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
		));
		textView.setPadding(0, getDp(PADDING_TOP), 0, 0);

		tracksCountTextView = new MenuTextView(context);
		updateLevelsLine();

		nameLayout.addView(textView);
		mainLayout.addView(nameLayout);
		mainLayout.addView(tracksCountTextView);

		return mainLayout;
	}

	public void updateNameLine() {
		String name = level.getName();
		// if (!active) {
		int margin = 0;
		if (installed)
			margin = INSTALLED_MARGIN;
		else if (active)
			margin = ACTIVE_MARGIN;

		SpannableString ss = new SpannableString(name);
		ss.setSpan(new LevelNameLeadingMarginSpan2(1, installed || active ? getDp(margin) : 0), 0, ss.length(), 0);
		textView.setTextOnUiThread(ss);
		/*} else {
			textView.setTextOnUiThread(Helpers.fromHtml(String.format(getString(R.string.active_name_tpl), name)));
		}*/
	}

	public void updateLevelsLine() {
		if (showDate) {
			tracksCountTextView.setText(Helpers.fromHtml(String.format(getString(R.string.levels_count_tpl),
					level.getCountEasy() + " - " + level.getCountMedium() + " - " + level.getCountHard(), level.getShortAddedDate())));
		} else {
			tracksCountTextView.setText(level.getCountEasy() + " - " + level.getCountMedium() + " - " + level.getCountHard());
		}
	}

	@Override
	protected void onHighlightChanged() {
		if (installed && installedIcon != null) {
			installedIcon.setImageResource(isHighlighted ? R.drawable.ic_downloaded_selected : R.drawable.ic_downloaded);
		}
		if (active && activeIcon != null) {
			activeIcon.setImageResource(isHighlighted ? R.drawable.ic_installed_selected : R.drawable.ic_installed);
		}
	}

	@Override
	protected void createAllViews() {
		super.createAllViews();

		helmet.setMeasuredHeight(true);

		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT
		);
		lp.setMargins(0, getDp(PADDING_TOP * 2 + 5), 0, 0);

		helmet.setLayoutParams(lp);
	}

	public void setInstalled(boolean installed) {
		this.installed = installed;
		if (installed) {
			if (installedIcon == null) {
				RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
						RelativeLayout.LayoutParams.WRAP_CONTENT,
						RelativeLayout.LayoutParams.WRAP_CONTENT
				);
				// params.setMargins(0, getDp(PADDING_TOP * 2 + 2), getDp(7), 0);
				params.setMargins(0, getDp(PADDING_TOP * 2 + 2), 0, 0);

				installedIcon = new MenuImageView(getGDActivity());
				installedIcon.setLayoutParams(params);
				installedIcon.setImageResource(R.drawable.ic_downloaded);
			}

			if (installedIcon.getParent() != nameLayout) {
				nameLayout.addView(installedIcon);
			}
		} else if (!installed && installedIcon != null && installedIcon.getParent() == nameLayout) {
			nameLayout.removeView(installedIcon);
		}

		updateNameLine();
	}

	public void setActive(boolean active) {
		this.active = active;
		if (active) {
			if (activeIcon == null) {
				RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
						RelativeLayout.LayoutParams.WRAP_CONTENT,
						RelativeLayout.LayoutParams.WRAP_CONTENT
				);
				// params.setMargins(0, getDp(PADDING_TOP * 2 + 2), getDp(7), 0);
				params.setMargins(0, getDp(PADDING_TOP * 2 + 2), 0, 0);

				activeIcon = new MenuImageView(getGDActivity());
				activeIcon.setLayoutParams(params);
				activeIcon.setImageResource(R.drawable.ic_installed);
			}

			if (activeIcon.getParent() != nameLayout) {
				nameLayout.addView(activeIcon);
			}
		} else if (!active && activeIcon != null && activeIcon.getParent() == nameLayout) {
			nameLayout.removeView(activeIcon);
		}

		updateNameLine();
	}

	public void setShowDate(boolean showDate) {
		this.showDate = showDate;
		updateLevelsLine();
	}

	@Override
	public void performAction(int k) {
		if (k == MenuScreen.KEY_FIRE) {
			buildScreen();
		}
	}

	protected void buildScreen() {
		Menu menu = getGameMenu();
		MenuScreen screen = menu.levelScreen;
		LevelsManager levelsManager = getLevelsManager();

		screen.clear();
		// System.gc();

		logDebug(level);

		screen.setNavTarget(this.screen);
		screen.setTitle(level.getName());

		if (!level.getAuthor().equals(""))
			screen.addItem(new BigTextMenuElement(Helpers.fromHtml(String.format(getString(R.string.author_tpl), level.getAuthor()))));
		if (level.getInstalledTs() > 0)
			screen.addItem(new BigTextMenuElement(Helpers.fromHtml(String.format(getString(R.string.installed_tpl), level.getFullInstalledDate()))));
		else if (level.getAddedTs() > 0)
			screen.addItem(new BigTextMenuElement(Helpers.fromHtml(String.format(getString(R.string.added_tpl), level.getFullAddedDate()))));
		screen.addItem(new BigTextMenuElement(Helpers.fromHtml(String.format(getString(R.string.tracks_tpl), level.getCountEasy() + " / " + level.getCountMedium() + " / " + level.getCountHard()))));
		screen.addItem(menu.createEmptyLine(true));

		if (!level.isInstalled()) {
			ActionMenuElement installAction = menu.createAction(ActionMenuElement.INSTALL);
			installAction.setText(String.format(
					getString(R.string.install_kb), level.getSizeKb()
			));
			installAction.setHandler(this);

			screen.addItem(installAction);
		} else {
			if (!level.isDefault()) {
				ActionMenuElement installed = new ActionMenuElement(getString(R.string.installed), null);
				installed.setDisabled(true);
				screen.addItem(installed);
			}

			if (level.getId() == levelsManager.getCurrentId()) {
				ActionMenuElement active = new ActionMenuElement(getString(R.string.active), null);
				active.setDisabled(true);
				screen.addItem(active);
			} else {
				ActionMenuElement loadAction = menu.createAction(ActionMenuElement.LOAD);
				loadAction.setHandler(this);
				screen.addItem(loadAction);
			}

			if (!level.isDefault() && levelsManager.getCurrentId() != level.getId()) {
				ActionMenuElement deleteAction = menu.createAction(ActionMenuElement.DELETE);
				deleteAction.setHandler(this);

				screen.addItem(deleteAction);
			}
		}

		screen.addItem(menu.createAction(ActionMenuElement.BACK));

		if (menu.getCurrentMenu() != screen) {
			menu.setCurrentMenu(screen, false);
		} else {
			screen.highlightElement();
		}
	}

	@Override
	public MenuScreen getCurrentMenu() {
		return null;
	}

	@Override
	public void setCurrentMenu(MenuScreen e1, boolean flag) {
	}

	@Override
	public void handleAction(MenuElement item) {
		if (item instanceof ActionMenuElement) {
			final GDActivity gd = getGDActivity();
			final Menu menu = getGameMenu();

			switch (((ActionMenuElement) item).getActionValue()) {
				case ActionMenuElement.DELETE:
					if (!level.isInstalled())
						break;

					showConfirm(
							getString(R.string.delete_levels),
							getString(R.string.delete_levels_confirmation),
							new Runnable() {
								@Override
								public void run() {
									gd.levelsManager.deleteAsync(level, new Runnable() {
										@Override
										public void run() {
											long id = level.getId();

											MenuScreen target = menu.getCurrentMenu().getNavTarget();
											if (target instanceof InstalledLevelsMenuScreen) {
												InstalledLevelsMenuScreen installedScreen = (InstalledLevelsMenuScreen) target;
												LevelMenuElement el = installedScreen.getElementByLevelId(id, 0);
												if (el != null)
													installedScreen.deleteElement(el);
												level.setId(0);
												menu.back();
											} else if (target instanceof DownloadLevelsMenuScreen) {
												DownloadLevelsMenuScreen downloadScreen = (DownloadLevelsMenuScreen) target;
												LevelMenuElement el = downloadScreen.getElementByLevelId(id, 0);
												if (el != null)
													el.setInstalled(false);

												level.setId(0);
												buildScreen();
											}
										}
									});
								}
							},
							null
					);
					break;

				case ActionMenuElement.INSTALL:
					gd.levelsManager.downloadLevel(level, new Callback() {
						@Override
						public void onDone(Object... objects) {
							long id = (long) objects[0];
							level.setId(id);

							MenuScreen target = menu.getCurrentMenu().getNavTarget();
							if (target instanceof DownloadLevelsMenuScreen) {
								DownloadLevelsMenuScreen downloadScreen = (DownloadLevelsMenuScreen) target;
								LevelMenuElement el = downloadScreen.getElementByLevelId(id, 0);
								if (el != null)
									el.setInstalled(true);
							}

							buildScreen();
						}
					});
					break;

				case ActionMenuElement.LOAD:
					gd.levelsManager.load(level);
					// buildScreen();
					break;
			}
		}
	}

	@Override
	public String toString() {
		return level.toString();
	}

}