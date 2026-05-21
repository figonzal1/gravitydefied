package cl.figonzal.gravitydefied.Menu;

import android.view.ViewTreeObserver;
import java.util.concurrent.Future;
import cl.figonzal.gravitydefied.Storage.Level;
import cl.figonzal.gravitydefied.Storage.LevelsManager;

import static cl.figonzal.gravitydefied.Helpers.getGDActivity;

public class InstalledLevelsMenuScreen extends LevelsMenuScreen {

	LevelsManager levelsManager;
	protected boolean isLoading = false;
	Future<?> asyncLoadLevels = null;

	public InstalledLevelsMenuScreen(String title, MenuScreen navTarget) {
		super(title, navTarget);
		levelsManager = getGDActivity().levelsManager;
	}

	@Override
	public void loadLevels() {
		showLoading();
		isLoading = true;

		asyncLoadLevels = executor.submit(() -> {
			Level[] loadedLevels = levelsManager.getAllInstalledLevels();
			mainHandler.post(() -> {
				if (status != Statuses.NORMAL) {
					clearList();
					setStatus(Statuses.NORMAL);
				}
				hideLoading();

				addElements = submitAddElements(loadedLevels, () -> {
					isLoading = false;
					if (selectedIndex != -1) {
						final ViewTreeObserver obs = listLayout.getViewTreeObserver();
						obs.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
							public boolean onPreDraw() {
								scrollToItem(selectedIndex);
								try {
									obs.removeOnPreDrawListener(this);
								} catch (IllegalStateException e) {
								}
								return true;
							}
						});
					}
				});
			});
		});
	}

	@Override
	public void onShow() {
		super.onShow();
	}

	@Override
	public void onHide(MenuScreen newMenu) {
		super.onHide(newMenu);
	}

	@Override
	protected boolean hideDate() {
		return true;
	}

	@Override
	public void reloadLevels() {
		if (asyncLoadLevels != null) {
			asyncLoadLevels.cancel(true);
			asyncLoadLevels = null;
		}

		isLoading = false;
		super.reloadLevels();
	}

}
