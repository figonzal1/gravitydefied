package cl.figonzal.gravitydefied.API;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Parses the three payload shapes the world-ranking backend returns inside the ["ok", payload]
 * envelope (see API/Response.java) - one static parse method per Ranking.java call.
 */
public class RankingResponse {

	public static class Entry {
		public final String name;
		public final long timeCs;

		Entry(String name, long timeCs) {
			this.name = name;
			this.timeCs = timeCs;
		}
	}

	public static class Leaderboard {
		public final Entry[] entries;
		public final Integer myRank;   // null: not authenticated, or no score submitted yet
		public final Long myTimeCs;

		Leaderboard(Entry[] entries, Integer myRank, Long myTimeCs) {
			this.entries = entries;
			this.myRank = myRank;
			this.myTimeCs = myTimeCs;
		}
	}

	public static class SubmitResult {
		public final int rank;
		public final long best;

		SubmitResult(int rank, long best) {
			this.rank = rank;
			this.best = best;
		}
	}

	public static String parseAuthToken(Response response) throws JSONException {
		return response.getJSON().getJSONObject(1).getString("token");
	}

	public static SubmitResult parseSubmit(Response response) throws JSONException {
		JSONObject payload = response.getJSON().getJSONObject(1);
		return new SubmitResult(payload.getInt("rank"), payload.getLong("best"));
	}

	public static Leaderboard parseLeaderboard(Response response) throws JSONException {
		JSONObject payload = response.getJSON().getJSONObject(1);

		JSONArray items = payload.getJSONArray("entries");
		Entry[] entries = new Entry[items.length()];
		for (int i = 0; i < items.length(); i++) {
			JSONObject item = items.getJSONObject(i);
			entries[i] = new Entry(item.getString("name"), item.getLong("timeCs"));
		}

		Integer myRank = null;
		Long myTimeCs = null;
		if (!payload.isNull("me")) {
			JSONObject me = payload.getJSONObject("me");
			myRank = me.getInt("rank");
			myTimeCs = me.getLong("timeCs");
		}

		return new Leaderboard(entries, myRank, myTimeCs);
	}

}
