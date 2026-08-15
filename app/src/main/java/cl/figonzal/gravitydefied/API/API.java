package cl.figonzal.gravitydefied.API;

import java.util.LinkedList;
import java.util.List;

public class API {

	public static final String URL = "http://gdtr.net/api.php";
	public static final String MRG_URL = "http://gdtr.net/mrg/%d.mrg";
	public static final int VERSION = 2;

	public static Request getLevels(int offset, int limit, LevelsSortType sort, ResponseHandler handler)
			throws Exception {
		List<String[]> params = new LinkedList<String[]>();
		params.add(new String[]{"sort", sort.toString()});
		params.add(new String[]{"offset", String.valueOf(offset)});
		params.add(new String[]{"limit", String.valueOf(limit)});

		return new Request("getLevels", params, handler);
	}

	public static String getMrgURL(long id) {
		return String.format(MRG_URL, id);
	}

	public static enum LevelsSortType {
		POPULAR("popular"), TRACKS("tracks"), RECENT("recent"), OLDEST("oldest");

		private final String text;

		private LevelsSortType(final String text) {
			this.text = text;
		}

		@Override
		public String toString() {
			return text;
		}
	}

	public static LevelsSortType getSortTypeById(int id) {
		switch (id) {
			case 0:
				return LevelsSortType.POPULAR;
			case 1:
				return LevelsSortType.RECENT;
			case 2:
				return LevelsSortType.OLDEST;
			case 3:
				return LevelsSortType.TRACKS;
		}
		return null;
	}

	public static int getIdBySortType(LevelsSortType type) {
		switch (type) {
			case POPULAR:
				return 0;
			case RECENT:
				return 1;
			case OLDEST:
				return 2;
			case TRACKS:
				return 3;
		}
		return 0;
	}

}
