package cl.figonzal.gravitydefied;

import android.app.Application;
import com.google.android.gms.games.PlayGamesSdk;

public class GDApplication extends Application {
	@Override
	public void onCreate() {
		super.onCreate();

		// World ranking sign-in (Menu/RankingMenuScreen) needs Play Games initialized before any
		// PlayGames.* call. Firebase itself still auto-initializes from google-services.json - this
		// is the only reason this method is no longer empty.
		PlayGamesSdk.initialize(this);
	}
}
