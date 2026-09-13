package cl.figonzal.gravitydefied.API;

import com.google.android.gms.games.GamesSignInClient;
import com.google.android.gms.games.PlayGames;

import cl.figonzal.gravitydefied.GDActivity;
import cl.figonzal.gravitydefied.Settings;

/**
 * Play Games sign-in -> server-side authCode -> Ranking.auth() -> our own JWT cached in Settings.
 * One-shot flow, triggered from the "Sign in" option item (ActionMenuElement.SIGN_IN_RANKING,
 * wired in Menu.handleAction()). The backend verifies the authCode with Google itself and resolves
 * the real PGS player id/name - this class never declares who the player is, it only forwards the code.
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

	public static void signIn(GDActivity activity, Callback callback) {
		GamesSignInClient client = PlayGames.getGamesSignInClient(activity);

		client.signIn().addOnCompleteListener(signInTask -> {
			if (!signInTask.isSuccessful()) {
				callback.onFailed("Play Games sign-in failed");
				return;
			}

			client.requestServerSideAccess(WEB_CLIENT_ID, false).addOnCompleteListener(codeTask -> {
				String authCode = codeTask.isSuccessful() ? codeTask.getResult() : null;
				if (authCode == null) {
					callback.onFailed("Could not get a server access code");
					return;
				}

				Ranking.auth(authCode, new ResponseHandler() {
					@Override
					public void onResponse(Response response) {
						try {
							Settings.setRankingToken(RankingResponse.parseAuthToken(response));
							callback.onSignedIn();
						} catch (Exception e) {
							callback.onFailed("Malformed response");
						}
					}

					@Override
					public void onError(APIException error) {
						callback.onFailed(error.getMessage());
					}
				});
			});
		});
	}

}
