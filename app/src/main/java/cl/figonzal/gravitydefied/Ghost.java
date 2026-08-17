package cl.figonzal.gravitydefied;

import cl.figonzal.gravitydefied.Game.GameView;
import cl.figonzal.gravitydefied.Game.Physics;
import cl.figonzal.gravitydefied.Storage.HighScores;
import cl.figonzal.gravitydefied.Storage.LevelsManager;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import static cl.figonzal.gravitydefied.Helpers.getGDActivity;
import static cl.figonzal.gravitydefied.Helpers.getLevelsManager;
import static cl.figonzal.gravitydefied.Helpers.logDebug;

// Records the bike + rider state of the current run and, when it beats the local highscore,
// saves it as a "ghost" replayed alongside the next attempt at that track/league.
//
// Timing: both recording and playback advance in tick(), called once per game-thread iteration
// of GDActivity.run() (~33 Hz). The playback cursor must NOT advance in draw(): that runs on the
// UI thread, which repaints at the display refresh rate (GameView.onDraw ends with invalidate),
// so the ghost would replay 2-4x too fast. draw() is read-only by design.
public class Ghost {

	// Bumped from 1: the frame layout grew from 3 nodes to the full render state. Files written
	// by the old format fail the version check in load() and are ignored.
	private static final int FORMAT_VERSION = 2;
	// 6 nodes x (x, y), then wheel angles of nodes 1 and 2, then rider lean.
	private static final int FIELDS_PER_FRAME = 15;
	private static final int MAX_FRAMES = 6000; // ~3 minutes at one frame per 30ms tick

	private static final int[] recordBuffer = new int[FIELDS_PER_FRAME * MAX_FRAMES];
	private static int recordCount = 0;

	private static int[] playbackBuffer = null;
	private static int playbackFrameCount = 0;
	private static int playbackIndex = 0;

	private Ghost() {
	}

	// Game thread, once per frame. Advances the recording and the playback cursor independently:
	// playback must keep running after the recording hits MAX_FRAMES.
	public static void tick(Physics physics) {
		if (!Settings.isGhostEnabled()) return;

		if (recordCount < MAX_FRAMES) {
			int base = recordCount * FIELDS_PER_FRAME;
			for (int i = 0; i < 6; i++) {
				recordBuffer[base + i * 2] = physics.m_Hak[i].m_ifan[5].x;
				recordBuffer[base + i * 2 + 1] = physics.m_Hak[i].m_ifan[5].y;
			}
			recordBuffer[base + 12] = physics.m_Hak[1].m_ifan[5].m_bI;
			recordBuffer[base + 13] = physics.m_Hak[2].m_ifan[5].m_bI;
			recordBuffer[base + 14] = physics.getRiderLean();
			recordCount++;
		}

		if (playbackBuffer != null && playbackIndex < playbackFrameCount) {
			playbackIndex++;
		}
	}

	// UI thread, inside GameView.drawGame(). Read-only: never advances the cursor.
	public static void draw(GameView view) {
		if (!Settings.isGhostEnabled() || playbackBuffer == null) return;

		int frame = playbackIndex;
		if (frame >= playbackFrameCount) return;

		try {
			getGDActivity().physEngine.drawGhostFrame(view, playbackBuffer, frame * FIELDS_PER_FRAME);
		} catch (Exception e) {
			logDebug("Ghost.draw: " + e);
		}
	}

	// Call on every restart: rewinds playback and clears the in-progress recording.
	public static void reset() {
		recordCount = 0;
		playbackIndex = 0;
	}

	// Call on every restart, after reset(): loads the saved ghost (if any) for this track/league.
	public static void load(long levelId, int level, int track, int league) {
		playbackBuffer = null;
		playbackFrameCount = 0;

		if (!Settings.isGhostEnabled()) return;

		DataInputStream in = null;
		try {
			File file = getGhostFile(levelId, level, track, league);
			if (!file.exists()) return;

			in = new DataInputStream(new FileInputStream(file));
			if (in.readInt() != FORMAT_VERSION) return;

			int frames = in.readInt();
			if (frames <= 0 || frames > MAX_FRAMES) return;

			int[] buffer = new int[frames * FIELDS_PER_FRAME];
			for (int i = 0; i < buffer.length; i++) {
				buffer[i] = in.readInt();
			}

			playbackBuffer = buffer;
			playbackFrameCount = frames;
		} catch (Exception e) {
			logDebug("Ghost.load: " + e);
		} finally {
			closeQuietly(in);
		}
	}

	// Call once the finish line is crossed, with the same time (centiseconds) used for the
	// highscore table. Saves the just-recorded run only if it is a new local record.
	public static void onFinish(long timeCentis) {
		if (!Settings.isGhostEnabled() || recordCount == 0) return;

		try {
			GDActivity gd = getGDActivity();
			int level = gd.menu.getSelectedLevel();
			int track = gd.menu.getSelectedTrack();
			int league = gd.menu.getSelectedLeague();

			HighScores scores = getLevelsManager().getHighScores(level, track);
			if (scores.getPlace(league, timeCentis) != 0) return; // not a new record

			save(getLevelsManager().getCurrentId(), level, track, league);
		} catch (Exception e) {
			logDebug("Ghost.onFinish: " + e);
		}
	}

	private static void save(long levelId, int level, int track, int league) {
		DataOutputStream out = null;
		try {
			File file = getGhostFile(levelId, level, track, league);
			out = new DataOutputStream(new FileOutputStream(file));
			out.writeInt(FORMAT_VERSION);
			out.writeInt(recordCount);
			for (int i = 0; i < recordCount * FIELDS_PER_FRAME; i++) {
				out.writeInt(recordBuffer[i]);
			}
		} catch (Exception e) {
			logDebug("Ghost.save: " + e);
		} finally {
			closeQuietly(out);
		}
	}

	// Deletes every saved ghost. Called from the Options > Clear highscore > Full Reset flow.
	public static void clearAll() {
		try {
			File[] files = LevelsManager.getLevelsDirectory().listFiles();
			if (files == null) return;

			for (File file : files) {
				if (file.getName().startsWith("ghost_") && file.getName().endsWith(".bin")) {
					file.delete();
				}
			}
		} catch (Exception e) {
			logDebug("Ghost.clearAll: " + e);
		}

		playbackBuffer = null;
		playbackFrameCount = 0;
	}

	private static File getGhostFile(long levelId, int level, int track, int league) {
		return new File(LevelsManager.getLevelsDirectory(),
				"ghost_" + levelId + "_" + level + "_" + track + "_" + league + ".bin");
	}

	private static void closeQuietly(Closeable c) {
		if (c == null) return;
		try {
			c.close();
		} catch (IOException e) {
		}
	}

	// Round-trips a synthetic buffer through the exact save()/load() wire format. Called once
	// from GDActivity.onCreate() on debuggable builds only; throws loudly if the codec breaks.
	public static void selfCheck() {
		File temp = null;
		DataOutputStream out = null;
		DataInputStream in = null;
		try {
			temp = File.createTempFile("ghost_selfcheck", ".bin");
			int frames = 3;
			int[] sample = new int[frames * FIELDS_PER_FRAME];
			for (int i = 0; i < sample.length; i++) sample[i] = i * 7 - 100;

			out = new DataOutputStream(new FileOutputStream(temp));
			out.writeInt(FORMAT_VERSION);
			out.writeInt(frames);
			for (int v : sample) out.writeInt(v);
			out.close();
			out = null;

			in = new DataInputStream(new FileInputStream(temp));
			if (in.readInt() != FORMAT_VERSION) throw new AssertionError("Ghost.selfCheck: version mismatch");
			if (in.readInt() != frames) throw new AssertionError("Ghost.selfCheck: frame count mismatch");
			for (int v : sample) {
				if (in.readInt() != v) throw new AssertionError("Ghost.selfCheck: payload mismatch");
			}
		} catch (IOException e) {
			throw new AssertionError(e);
		} finally {
			closeQuietly(out);
			closeQuietly(in);
			if (temp != null) temp.delete();
		}
	}

	// ponytail: 60 B/frame uncompressed (~118 KB/min). Delta-encode to short if disk use matters.
}
