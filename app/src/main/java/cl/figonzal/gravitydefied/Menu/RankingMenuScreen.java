package cl.figonzal.gravitydefied.Menu;

import cl.figonzal.gravitydefied.API.APIException;
import cl.figonzal.gravitydefied.API.Ranking;
import cl.figonzal.gravitydefied.API.RankingResponse;
import cl.figonzal.gravitydefied.API.Request;
import cl.figonzal.gravitydefied.API.Response;
import cl.figonzal.gravitydefied.API.ResponseHandler;
import cl.figonzal.gravitydefied.R;
import cl.figonzal.gravitydefied.Settings;
import cl.figonzal.gravitydefied.Storage.LevelsManager;

import static cl.figonzal.gravitydefied.Helpers.getGDActivity;
import static cl.figonzal.gravitydefied.Helpers.getGameMenu;
import static cl.figonzal.gravitydefied.Helpers.getLevelsManager;
import static cl.figonzal.gravitydefied.Helpers.getString;

/**
 * World Top-10 for the pack/difficulty/track/league currently selected in Menu (playMenu's
 * selectors), reached from highScoreMenu. Read-only: rows are plain TextMenuElements, no clicking,
 * navigation is the usual back/scroll (see setIsTextScreen(true) in MenuScreen).
 *
 * Always reloads on entry rather than caching - the selection (track/league) can differ between
 * visits and a leaderboard query is cheap.
 */
public class RankingMenuScreen extends MenuScreen {

	private static final int LIMIT = 10;

	private Request request;

	public RankingMenuScreen(String title, MenuScreen navTarget) {
		super(title, navTarget);
		setIsTextScreen(true);
	}

	@Override
	public void onShow() {
		super.onShow();
		load();
	}

	@Override
	public void onHide(MenuScreen newMenu) {
		if (request != null) {
			request.cancel();
			request = null;
		}
	}

	private void load() {
		clear();

		LevelsManager levelsManager = getLevelsManager();
		String pack = levelsManager.packKey();

		if (pack == null) {
			showMessage(getString(R.string.ranking_unavailable_sideload));
			return;
		}

		String token = Settings.getRankingToken();
		if (!Settings.isRankingEnabled() || token == null) {
			showMessage(getString(R.string.ranking_sign_in_required));
			return;
		}

		Menu menu = getGameMenu();
		int difficulty = menu.getSelectedLevel();
		int track = menu.getSelectedTrack();
		int league = menu.getSelectedLeague();

		addItem(new TextMenuElement(getString(R.string.ranking_loading)));

		request = Ranking.leaderboard(pack, difficulty, track, league, LIMIT, token, new ResponseHandler() {
			@Override
			public void onResponse(Response response) {
				request = null;
				try {
					renderBoard(RankingResponse.parseLeaderboard(response));
				} catch (Exception e) {
					e.printStackTrace();
					showMessage(getString(R.string.ranking_error));
				}
			}

			@Override
			public void onError(APIException error) {
				request = null;
				showMessage(getString(R.string.ranking_error));
			}
		});
	}

	private void renderBoard(RankingResponse.Leaderboard board) {
		clear();

		if (board.entries.length == 0) {
			addItem(new TextMenuElement(getString(R.string.no_highscores)));
		} else {
			for (int i = 0; i < board.entries.length; i++) {
				RankingResponse.Entry entry = board.entries[i];
				addItem(new TextMenuElement((i + 1) + ". " + entry.name + "  " + formatTime(entry.timeCs)));
			}
		}

		if (board.myRank != null) {
			addItem(new EmptyLineMenuElement(10));
			addItem(new TextMenuElement(getGDActivity().getString(R.string.ranking_your_place, board.myRank, formatTime(board.myTimeCs))));
		}

		addItem(getGameMenu().createAction(ActionMenuElement.BACK));
		highlightElement();
	}

	private void showMessage(String text) {
		clear();
		addItem(new TextMenuElement(text));
		addItem(getGameMenu().createAction(ActionMenuElement.BACK));
		highlightElement();
	}

	// Same "MM:SS.cc" format as HighScores.getScores()/Menu.getDurationString() - kept local
	// rather than reusing those (private, instance-bound to Menu/HighScores) for a one-line format.
	private static String formatTime(long centiseconds) {
		int wholeSeconds = (int) (centiseconds / 100);
		int hundredths = (int) (centiseconds % 100);
		int minutes = wholeSeconds / 60;
		int seconds = wholeSeconds % 60;
		return String.format("%02d:%02d.%02d", minutes, seconds, hundredths);
	}

}
