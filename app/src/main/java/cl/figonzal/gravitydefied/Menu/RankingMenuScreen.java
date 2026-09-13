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

import static cl.figonzal.gravitydefied.Helpers.fromHtml;
import static cl.figonzal.gravitydefied.Helpers.getGDActivity;
import static cl.figonzal.gravitydefied.Helpers.getGameMenu;
import static cl.figonzal.gravitydefied.Helpers.getLevelsManager;
import static cl.figonzal.gravitydefied.Helpers.getString;

/**
 * World Top-10 (with medals for the top 3, like the old local highscore screen) for the
 * pack/difficulty/track currently selected in Menu (playMenu's selectors), reached directly from
 * playMenu - this is the only highscore/ranking screen left, local per-device highscores having
 * been removed in favor of this being the single source of truth.
 *
 * League can be browsed independently of the actual play-menu selection via left/right (see
 * Menu.keyPressed()), the same way the old local highscore screen worked - it resyncs to the
 * current play-menu league every time the screen is (re)entered.
 *
 * Always reloads on entry rather than caching - the selection (track/league) can differ between
 * visits and a leaderboard query is cheap.
 */
public class RankingMenuScreen extends MenuScreen {

	private static final int LIMIT = 10;

	private Request request;
	private int league;

	public RankingMenuScreen(String title, MenuScreen navTarget) {
		super(title, navTarget);
		setIsTextScreen(true);
	}

	@Override
	public void onShow() {
		super.onShow();
		league = getGameMenu().getSelectedLeague();
		load();
	}

	@Override
	public void onHide(MenuScreen newMenu) {
		if (request != null) {
			request.cancel();
			request = null;
		}
	}

	/**
	 * Browses a different league without touching the actual play-menu league selection - called
	 * from Menu.keyPressed() on left/right while this screen is shown.
	 */
	public void cycleLeague(int delta) {
		int max = getGameMenu().getUnlockedLeagueCount();
		league = Math.max(0, Math.min(max, league + delta));
		load();
	}

	private void load() {
		clear();

		LevelsManager levelsManager = getLevelsManager();
		String pack = levelsManager.packKey();

		if (pack == null) {
			showMessage(getString(R.string.ranking_unavailable_sideload));
			return;
		}

		if (!Settings.isRankingEnabled()) {
			showMessage(getString(R.string.ranking_disabled));
			return;
		}

		// May be null: the backend's leaderboard read is auth-optional (OptionalJwtAuthGuard) -
		// an anonymous viewer still sees the Top-10, just without the "You: #n" row below it.
		String token = Settings.getRankingToken();

		Menu menu = getGameMenu();
		int difficulty = menu.getSelectedLevel();
		int track = menu.getSelectedTrack();

		addSubtitle();
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

	private void addSubtitle() {
		HighScoreTextMenuElement subtitle = new HighScoreTextMenuElement(
				fromHtml(getString(R.string.league) + ": " + getGameMenu().getLeagueName(league)));
		subtitle.setIsSubtitle(true);
		addItem(subtitle);
	}

	private void renderBoard(RankingResponse.Leaderboard board) {
		clear();
		addSubtitle();

		if (board.entries.length == 0) {
			addItem(new TextMenuElement(getString(R.string.no_highscores)));
		} else {
			for (int i = 0; i < board.entries.length; i++) {
				RankingResponse.Entry entry = board.entries[i];
				HighScoreTextMenuElement row = new HighScoreTextMenuElement(
						(i + 1) + ". " + entry.name + "  " + formatTime(entry.timeCs));
				if (i < 3) row.setMedal(true, i);
				row.setLayoutPadding(true);
				addItem(row);
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

	// Same "MM:SS.cc" format as Menu.getDurationString() - kept local (package-visible, see
	// Menu.saveCompletedTrack()'s Top-3 mini-board) rather than a private/instance-bound method,
	// for a one-line format with no leading-space quirk.
	static String formatTime(long centiseconds) {
		int wholeSeconds = (int) (centiseconds / 100);
		int hundredths = (int) (centiseconds % 100);
		int minutes = wholeSeconds / 60;
		int seconds = wholeSeconds % 60;
		return String.format("%02d:%02d.%02d", minutes, seconds, hundredths);
	}

}
