package ca.benwu.drivingevaluatorandroid.ui;

import android.content.Context;
import android.database.DataSetObserver;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import ca.benwu.drivingevaluatorandroid.R;

public class HistoryActivity extends AppCompatActivity {

    private JSONArray array;

    private ArrayList<String> scores = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        try {
            array = new JSONArray(getIntent().getStringExtra("HISTORY_RESPONSE"));
        } catch (JSONException e) {
            array = new JSONArray();
        }

        for(int i = 0 ; i < array.length() ; i++) {
            try {
                scores.add(array.getJSONObject(i).getString("score"));
            } catch (Exception e) {

            }
        }

        ((ListView) findViewById(R.id.historyList)).setAdapter(new ArrayAdapter<String>(this, R.layout.list_row, scores));
    }

    //private ArrayAdapter<JSONObject> a = new ArrayAdapter<JSONObject>(this, R.layout.item_history) {
    //    @NonNull
    //    @Override
    //    public View getView(int position, View convertView, ViewGroup parent) {
    //        if(convertView == null) {
    //            //convertView = inflater.inflate(R.layout.item_history, parent, false);
    //        }
    //        try {
    //            ((TextView) convertView.findViewById(R.id.tripId)).setText(array.getJSONObject(position).getInt("tripId"));
    //            ((TextView) convertView.findViewById(R.id.score)).setText(array.getJSONObject(position).getInt("score"));
    //        } catch (JSONException e) {
//
    //        }
    //        return convertView;
    //    }
    //};
}
