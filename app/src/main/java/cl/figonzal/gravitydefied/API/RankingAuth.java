package cl.figonzal.gravitydefied.API;

import com.google.android.gms.games.GamesSignInClient;
import com.google.android.gms.games.PlayGames;
import com.google.android.gms.games.Player;

import cl.figonzal.gravitydefied.GDActivity;
import cl.figonzal.gravitydefied.R;
import cl.figonzal.gravitydefied.Settings;

import static cl.figonzal.gravitydefied.Helpers.getString;
import static cl.figonzal.gravitydefied.Helpers.logDebug;

/**
 * Play Games sign-in -> server-side authCode -> Ranking.auth() -> our own JWT cached in Settings.
 * The backend verifies the authCode with Google itself and resolves the real PGS player id/name -
 * this class never declares who the player is, it only forwards the code (and separately caches
 * the display name client-side, for UI only - see refreshPlayerName()).
 */
public class RankingAuth {

	// OAuth 2.0 *web* client id, same GCP project as the Play Games Services link (Play Console >
	// Play Games Services > Configuration > Credentials > Servidor de juegos). Not the Android
	// client id - requestServerSideAccess() needs the web one so the backend can exchange it.
	private static final String WEB_CLIENT_ID = "144764087426-k7uhj4m3v4sbn2b4cf0eu784l8lgleoa.apps.googleusercontent.com";

	public interface Callback {
		void onSignedIn();

		void onFailed(String message);
	}

	/**
	 * Manual sign-in, triggered by the "Sign in with Play Games" row in Options. Always re-runs
	 * the full flow (even if a backend token is already cached) so it also works as "switch
	 * account" / "re-authenticate". Per Google's own guidance for Play Games Services v2
	 * (developer.android.com/games/pgs/android/android-signin), a manual button is only meant as
	 * a fallback for when automatic sign-in (see ensureSignedIn()) didn't already work.
	 */
	public static void signIn(GDActivity activity, Callback callback) {
		PlayGames.getGamesSignInClient(activity).signIn().addOnCompleteListener(signInTask -> {
			if (!signInTask.isSuccessful()) {
				callback.onFailed(getString(R.string.ranking_sign_in_failed));
				return;
			}

			refreshPlayerName(activity);
			requestBackendToken(activity, callback);
		});
	}

	/**
	 * Called once at startup (Menu.load(), step 4). PlayGamesSdk.initialize() (GDApplication)
	 * already signs the player into Play Games itself automatically on launch - this only checks
	 * whether that succeeded and, if so, silently gets our own backend token too (skipped if one
	 * is already cached), with no dialog and no user action. This is what lets the Options row
	 * show "Signed in as X" without the player ever touching "Sign in with Play Games" - that
	 * button is only needed as the fallback Google's guidance describes, when this comes back
	 * not-authenticated.
	 *
	 * @param onSettled always called exactly once when this is done, regardless of outcome - the
	 *                  caller uses it to refresh the Options row, never to show an error (a
	 *                  player who simply hasn't signed in yet is not a failure worth reporting).
	 */
	public static void ensureSignedIn(GDActivity activity, Runnable onSettled) {
		PlayGames.getGamesSignInClient(activity).isAuthenticated().addOnCompleteListener(task -> {
			boolean signedIn = task.isSuccessful() && task.getResult().isAuthenticated();
			if (!signedIn) {
				onSettled.run();
				return;
			}

			refreshPlayerName(activity);

			if (Settings.getRankingToken() != null) {
				onSettled.run(); // already have a backend session from a previous launch
				return;
			}

			requestBackendToken(activity, new Callback() {
				@Override
				public void onSignedIn() {
					onSettled.run();
				}

				@Override
				public void onFailed(String message) {
					// Silent by design (see onSettled's doc) - logged so a misconfigured backend
					// still shows up somewhere, retried automatically on the next launch.
					logDebug("Automatic ranking sign-in failed: " + message);
					onSettled.run();
				}
			});
		});
	}

	private static void requestBackendToken(GDActivity activity, Callback callback) {
		GamesSignInClient client = PlayGames.getGamesSignInClient(activity);

		client.requestServerSideAccess(WEB_CLIENT_ID, false).addOnCompleteListener(codeTask -> {
			String authCode = codeTask.isSuccessful() ? codeTask.getResult() : null;
			if (authCode == null) {
				callback.onFailed(getString(R.string.ranking_auth_code_failed));
				return;
			}

			Ranking.auth(authCode, new ResponseHandler() {
				@Override
				public void onResponse(Response response) {
					try {
						Settings.setRankingToken(RankingResponse.parseAuthToken(response));
						callback.onSignedIn();
					} catch (Exception e) {
						callback.onFailed(getString(R.string.ranking_malformed_response));
					}
				}

				@Override
				public void onError(APIException error) {
					callback.onFailed(error.getMessage());
				}
			});
		});
	}

	/**
	 * Refreshes the cached Play Games display name shown in Options ("Signed in as X") - purely
	 * for UI, best-effort. The backend never trusts this value; it resolves its own copy of the
	 * name server-side from the authCode (see auth.service.ts#fetchPgsPlayer).
	 */
	private static void refreshPlayerName(GDActivity activity) {
		PlayGames.getPlayersClient(activity).getCurrentPlayer().addOnCompleteListener(task -> {
			if (!task.isSuccessful()) return;

			Player player = task.getResult();
			Settings.setRankingPlayerName(player.getDisplayName());
		});
	}

}
