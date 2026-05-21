package com.figonzal.gravitydefied.Menu;

import android.content.Context;
import android.view.LayoutInflater;
import com.figonzal.gravitydefied.Helpers;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.Typeface;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.figonzal.gravitydefied.R;
import com.figonzal.gravitydefied.Storage.Level;

import java.util.ArrayList;

import static com.figonzal.gravitydefied.Helpers.getString;

public class LevelsAdapter extends ArrayAdapter<Level> {

	private ArrayList<Level> levels;

	public LevelsAdapter(Context context, ArrayList<Level> levels) {
		super(context, R.layout.levels_list_item, levels);
		this.levels = levels;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		if (v == null) {
			LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = inflater.inflate(R.layout.levels_list_item, null);
		}

		Level level = levels.get(position);
		if (level != null) {
			TextView name = (TextView) v.findViewById(R.id.level_name);
			TextView count = (TextView) v.findViewById(R.id.level_count);

			Typeface condensed = Typeface.create("sans-serif-condensed", Typeface.NORMAL);
			name.setTypeface(condensed);
			count.setTypeface(condensed);
			name.setText(level.getName());
			count.setText(Helpers.fromHtml(String.format(getString(R.string.levels_count_tpl),
					level.getCountEasy() + " - " + level.getCountMedium() + " - " + level.getCountHard(), level.getShortAddedDate())));
		}

		return v;
	}

}


