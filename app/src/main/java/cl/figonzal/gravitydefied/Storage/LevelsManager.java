package cl.figonzal.gravitydefied.Storage;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import cl.figonzal.gravitydefied.API.API;
import cl.figonzal.gravitydefied.API.DownloadFile;
import cl.figonzal.gravitydefied.API.DownloadHandler;
import cl.figonzal.gravitydefied.Callback;
import cl.figonzal.gravitydefied.DoubleCallback;
import cl.figonzal.gravitydefied.GDActivity;
import cl.figonzal.gravitydefied.Levels.LevelHeader;
import cl.figonzal.gravitydefied.Levels.Reader;
import cl.figonzal.gravitydefied.Menu.Menu;
import cl.figonzal.gravitydefied.Menu.MenuScreen;
import cl.figonzal.gravitydefied.R;
import cl.figonzal.gravitydefied.Settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import static cl.figonzal.gravitydefied.Helpers.dismissDialog;
import static cl.figonzal.gravitydefied.Helpers.getGDActivity;
import static cl.figonzal.gravitydefied.Helpers.getGameMenu;
import static cl.figonzal.gravitydefied.Helpers.getString;
import static cl.figonzal.gravitydefied.Helpers.getTimestamp;
import static cl.figonzal.gravitydefied.Helpers.isActivityAlive;
import static cl.figonzal.gravitydefied.Helpers.isOnline;
import static cl.figonzal.gravitydefied.Helpers.logDebug;
import static cl.figonzal.gravitydefied.Helpers.showAlert;

public class LevelsManager {

	private static final ExecutorService executor = Executors.newCachedThreadPool();
	private static final Handler mainHandler = new Handler(Looper.getMainLooper());

	private LevelsDataSource dataSource;
	private Level currentLevel;

	public LevelsManager() {
		GDActivity gd = getGDActivity();
		dataSource = new LevelsDataSource(gd);

		try {
			dataSource.open();

			if (!dataSource.isDefaultLevelCreated()) {
				Level level = dataSource.createLevel("GDTR original", "Codebrew Software", 10, 10, 10, 0, 0, true, 1);
				logDebug("LevelsManager: Default level created!");
				logDebug(level);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			logDebug("LevelsManager: db feels bad :(");
			// return;
		}

		logDebug("LevelsManager: db feels OK :)");

		// Shared prefs
		// SharedPreferences settings = getSharedPreferences();
		// long levelId = settings.getLong(PREFS_LEVEL_ID, 0);
		long levelId = Settings.getLevelId();
		if (levelId < 1 || !mrgIsAvailable(levelId)) {
			logDebug("LevelsManager: levelId = " + levelId + ", < 1 or mrg is not available; now: reset id");
			/*SharedPreferences.Editor editor = settings.edit();
			editor.putLong(PREFS_LEVEL_ID, 1);
			editor.commit();*/
			resetId();
		}

		reload();
	}

	public void resetId() {
		Settings.setLevelId(1);
	}

	public void reload() {
		long id = Settings.getLevelId();
		currentLevel = dataSource.getLevel(id);

		if (currentLevel == null) {
			logDebug("LevelsManager: failed to load currentLevel; currentId = " + id);
		} else {
			logDebug("LevelsManager: level = " + currentLevel);
		}

	}

	public void closeDataSource() {
		dataSource.close();
	}

	public long getCurrentId() {
		return currentLevel.getId();
	}

	public void setCurrentId(long id) {
		// currentId = id;
		Settings.setLevelId(id);
		/*SharedPreferences settings = getSharedPreferences();
		SharedPreferences.Editor edit = settings.edit();
		edit.putLong(PREFS_LEVEL_ID, id);
		edit.commit();*/
	}

	public Level getCurrentLevel() {
		return currentLevel;
	}

	public File getCurrentLevelsFile() {
		if (currentLevel.getId() > 1)
			return getMrgFileById(currentLevel.getId());

		return null;
	}

	private boolean mrgIsAvailable(long id) {
		if (id == 1) // This is default built-in levels.mrg
			return true;

		File file = getMrgFileById(id);
		return isExternalStorageReadable() && file.exists();
	}

	public long install(File file, String name, String author, long apiId) throws Exception {
		if (!isSpaceAvailable(file.length())) {
			throw new Exception(getString(R.string.e_no_space_left));
		}

		InputStream inputStream = new FileInputStream(file);
		LevelHeader header = Reader.readHeader(inputStream);
		try {
			inputStream.close();
		} catch (IOException e) {
		}

		if (!header.isCountsOk()) {
			throw new IOException(file.getName() + " is not valid");
		}

		Level level = dataSource.createLevel(name, author, header.getCount(0), header.getCount(1), header.getCount(2), 0, getTimestamp(), false, apiId);
		long id = level.getId();
		if (id < 1) {
			throw new Exception(getString(R.string.e_cannot_save_level));
		}

		File newFile = getMrgFileById(id);
		copy(file, newFile);

		return id;
	}

	public void installAsync(File file, String name, String author, long apiId, final DoubleCallback callback) {
		final ProgressDialog progressDialog;
		if (isActivityAlive()) {
			GDActivity gd = getGDActivity();
			progressDialog = ProgressDialog.show(gd, getString(R.string.install), getString(R.string.installing), true);
		} else {
			progressDialog = null;
		}

		executor.submit(() -> {
			Object result;
			try {
				result = install(file, name, author, apiId);
			} catch (Throwable e) {
				result = e;
			}
			final Object finalResult = result;
			mainHandler.post(() -> {
				dismissDialog(progressDialog);

				if (finalResult instanceof Throwable) {
					Throwable throwable = (Throwable) finalResult;
					throwable.printStackTrace();
					showAlert(getString(R.string.error), throwable.getMessage(), null);
					if (callback != null) callback.onFail();
					return;
				}

				if (callback != null) callback.onDone((long) finalResult);
			});
		});
	}

	public void load(Level level) throws RuntimeException {
		/*File file = getMrgFileById(level.getId());
		if (!mrgIsAvailable(level.getId())) {
			throw new RuntimeException("Unable to load levels \"" +level.getName() + "\"");
		}*/

		// Loader loader = getLevelLoader();
		// Menu menu = getGameMenu();

		// loader.setLevelsFile(file);
		// menu.reloadLevels();

		setCurrentId(level.getId());
		getGDActivity().restartApp();
	}

	public boolean isApiIdInstalled(long apiId) {
		return dataSource.isApiIdInstalled(apiId);
	}

	public Level getLeveL(long id) {
		return dataSource.getLevel(id);
	}

	public Level[] getAllInstalledLevels() {
		return dataSource.getAllLevels().toArray(new Level[0]);
	}

	public void delete(Level level) {
		dataSource.deleteLevel(level);
		// getMrgFileById returns null for the built-in levels.mrg (id == 1)
		File file = getMrgFileById(level.getId());
		try {
			if (file != null && file.exists()) {
				file.delete();
			}
		} catch (Exception e) {
			logDebug("LevelsManager.delete: " + e);
			e.printStackTrace();
		}
	}

	public void deleteAsync(Level level, final Runnable callback) {
		final ProgressDialog progressDialog;
		if (isActivityAlive()) {
			GDActivity gd = getGDActivity();
			progressDialog = ProgressDialog.show(gd, getString(R.string.delete), getString(R.string.deleting), true);
		} else {
			progressDialog = null;
		}

		executor.submit(() -> {
			delete(level);
			mainHandler.post(() -> {
				dismissDialog(progressDialog);
				if (callback != null) callback.run();
			});
		});
	}

	public void updateLevelSettings() {
		dataSource.updateLevel(currentLevel);
	}

	public void downloadLevel(final Level level, final Callback successCallback) {
		final GDActivity gd = getGDActivity();
		File outputDir = gd.getCacheDir();

		try {
			boolean readable = isExternalStorageReadable();
			if (!readable) {
				throw new Exception(getString(R.string.e_external_storage_is_not_readable));
			}

			if (!isOnline()) {
				throw new Exception(getString(R.string.e_no_network_connection));
			}

			if (!isSpaceAvailable(level.getSize())) {
				throw new Exception(getString(R.string.e_no_space_left));
			}

			final File outputFile = File.createTempFile("levels" + level.getApiId(), "mrg", outputDir);
			FileOutputStream out = new FileOutputStream(outputFile);

			// logDebug("downloadLevel: 4");
			// final API api = new API();
			final ProgressDialog progress;
			final DownloadFile downloadFile = new DownloadFile(API.getMrgURL(level.getApiId()), out);

			progress = new ProgressDialog(gd);
			progress.setMessage(getString(R.string.downloading));
			progress.setIndeterminate(true);
			progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progress.setCancelable(true);

			final DownloadHandler handler = new DownloadHandler() {
				private boolean finished = false;

				@Override
				public void onFinish(Throwable error) {
					if (finished) return;
					finished = true;

					dismissDialog(progress);

					if (error != null) {
						// error.printStackTrace();
						error.printStackTrace();
						showAlert(getString(R.string.error), error.getMessage(), null);

						outputFile.delete();
						return;
					}

					// Install
					installAsync(outputFile, level.getName(), level.getAuthor(), level.getApiId(), new DoubleCallback() {
						@Override
						public void onDone(Object... objects) {
							long id = (long) objects[0];
							outputFile.delete();

							if (successCallback != null)
								successCallback.onDone(id);
						}

						@Override
						public void onFail() {
							outputFile.delete();
						}
					});
				}

				@Override
				public void onStart() {
					if (isActivityAlive()) progress.show();
				}

				@Override
				public void onProgress(int pr) {
					progress.setIndeterminate(false);
					progress.setMax(100);
					progress.setProgress(pr);
				}
			};
			progress.setOnCancelListener(new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					downloadFile.cancel();
					handler.onFinish(new InterruptedException(getString(R.string.e_downloading_was_interrupted)));
				}
			});

			downloadFile.setDownloadHandler(handler);
			downloadFile.start();
		} catch (Exception e) {
			showAlert(getString(R.string.error), e.getMessage(), null);
		}
	}

	public void showSuccessfullyInstalledDialog() {
		if (!isActivityAlive()) return;

		GDActivity gd = getGDActivity();
		AlertDialog success = new AlertDialog.Builder(gd)
				.setTitle(getString(R.string.installed))
				.setMessage(getString(R.string.successfully_installed))
				.setPositiveButton(getString(R.string.ok), null)
				.setNegativeButton(getString(R.string.open_installed), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Menu menu = getGameMenu();
						MenuScreen currentMenu = getGameMenu().getCurrentMenu(),
								newMenu = menu.managerInstalledScreen;

						if (currentMenu == menu.managerDownloadScreen || currentMenu.getNavTarget() == menu.managerDownloadScreen) {
							menu.managerDownloadScreen.onHide(menu.managerScreen);
						}

						menu.setCurrentMenu(newMenu, false);
					}
				})
				.create();
		success.show();
	}

	public HashMap<Long, Long> findInstalledLevels(ArrayList<Long> apiIds) {
		return dataSource.findInstalledLevels(apiIds);
	}

	public HighScores getHighScores(int level, int track) {
		HighScores scores = dataSource.getHighScores(currentLevel.getId(), level, track);
		// logDebug("LevelsManager.getHighScores: " + scores);
		return scores;
	}

	public void saveHighScores(HighScores scores) {
		dataSource.updateHighScores(scores);
	}

	public void clearHighScores() {
		dataSource.clearHighScores(currentLevel.getId());
	}

	public void clearAllHighScores() {
		dataSource.clearHighScores(0);
	}

	public void resetAllLevelsSettings() {
		dataSource.resetAllLevelsSettings();

		logDebug("All levels now: " + dataSource.getAllLevels());
		logDebug("Level#1: " + dataSource.getLevel(1));
	}

	public static boolean isExternalStorageReadable() {
		return getGDActivity().getExternalFilesDir(null) != null;
	}

	public static File getLevelsDirectory() {
		File file = new File(getGDActivity().getExternalFilesDir(null), "GDLevels");
		if (!file.mkdirs()) {
			logDebug("LevelsManager.getLevelsDirectory: directory not created");
		}
		return file;
	}

	public static String getMrgFileNameById(long id) {
		return getLevelsDirectory().getAbsolutePath() + "/" + id + ".mrg";
	}

	public static File getMrgFileById(long id) {
		if (id == 1) return null;
		return new File(getMrgFileNameById(id));
	}

	public static void copy(File src, File dst) throws IOException {
		InputStream in = new FileInputStream(src);
		OutputStream out = new FileOutputStream(dst);

		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}

		in.close();
		out.close();
	}

	public static boolean isSpaceAvailable(long bytes) {
		StatFs stat = new StatFs(getLevelsDirectory().getPath());
		long bytesAvailable = (long) stat.getBlockSize() * (long) stat.getAvailableBlocks();
		return bytesAvailable >= bytes;
	}

}
