package cl.figonzal.gravitydefied.API;

import java.util.LinkedList;
import java.util.List;

import cl.figonzal.gravitydefied.R;

import static cl.figonzal.gravitydefied.Helpers.getGDActivity;

/**
 * Client for the world-ranking backend (separate NestJS service, not gdtr.net). Same transport as
 * API.java - Request handles the form-urlencoded POST, threading and ["ok"/"error", ...] envelope.
 *
 * All three calls are POST because API/Request only speaks POST (see Request.java); leaderboard
 * is a read but the transport doesn't distinguish.
 */
public class Ranking {

	// Per-build-type resValue (app/build.gradle.kts): debug points at a local dev backend over
	// plain HTTP (see src/debug's network_security_config.xml cleartext override), release at the
	// real HTTPS host - same pattern already used for R.string.app_name.
	private static String baseUrl() {
		return getGDActivity().getString(R.string.ranking_base_url);
	}

	private static String authUrl() {
		return baseUrl() + "/v1/auth/pgs";
	}

	private static String scoresUrl() {
		return baseUrl() + "/v1/scores";
	}

	private static String leaderboardUrl() {
		return baseUrl() + "/v1/leaderboard";
	}

	/**
	 * Exchanges a Play Games server-side-access authCode for this app's own JWT. The backend
	 * verifies the code with Google and resolves the real PGS player id/name - the client never
	 * declares who it is.
	 */
	public static Request auth(String authCode, ResponseHandler handler) {
		List<String[]> params = new LinkedList<String[]>();
		params.add(new String[]{"authCode", authCode});

		return new Request(authUrl(), "auth", params, handler, null);
	}

	public static Request submitScore(String pack, int difficulty, int track, int league,
			long timeCs, String bearerToken, ResponseHandler handler) {
		List<String[]> params = new LinkedList<String[]>();
		params.add(new String[]{"pack", pack});
		params.add(new String[]{"difficulty", String.valueOf(difficulty)});
		params.add(new String[]{"track", String.valueOf(track)});
		params.add(new String[]{"league", String.valueOf(league)});
		params.add(new String[]{"timeCs", String.valueOf(timeCs)});

		return new Request(scoresUrl(), "submitScore", params, handler, bearerToken);
	}

	// Exact messages thrown by the backend's JwtAuthGuard (auth/jwt-auth.guard.ts) for a
	// missing/expired/tampered Bearer token - lets callers tell "not signed in (yet)" apart from
	// every other APIException and react by clearing the stale token instead of just logging it.
	public static boolean isAuthError(String message) {
		return "missing token".equals(message) || "invalid token".equals(message);
	}

	/**
	 * @param bearerToken may be null (anonymous view: entries only, no "me").
	 */
	public static Request leaderboard(String pack, int difficulty, int track, int league,
			int limit, String bearerToken, ResponseHandler handler) {
		List<String[]> params = new LinkedList<String[]>();
		params.add(new String[]{"pack", pack});
		params.add(new String[]{"difficulty", String.valueOf(difficulty)});
		params.add(new String[]{"track", String.valueOf(track)});
		params.add(new String[]{"league", String.valueOf(league)});
		params.add(new String[]{"limit", String.valueOf(limit)});

		return new Request(leaderboardUrl(), "leaderboard", params, handler, bearerToken);
	}

}
