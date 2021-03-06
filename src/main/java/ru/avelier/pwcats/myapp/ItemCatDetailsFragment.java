package ru.avelier.pwcats.myapp;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import ru.adelier.pw.PwItemCat;
import ru.adelier.pw.PwcatsRequester;

import java.util.List;

public class ItemCatDetailsFragment extends Fragment {
    private Activity parentActivity;
    private ViewGroup rootView;

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parentActivity = getActivity();
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public ViewGroup onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.item_details_cat);
        rootView = (ViewGroup) inflater.inflate(R.layout.item_details_cat, container, false);

        // load params from intent extras
        PwcatsRequester.Server server = PwcatsRequester.Server.valueOf(getArguments().getString("server"));
        if (server == null) {
            Log.wtf(this.toString(), "server not passed :(");
            return rootView;
        }
        Integer id = getArguments().getInt("id", -1);
        if (id == -1) {
            Log.wtf(this.toString(), "id not passed :(");
            return rootView;
        }

        // asynk ask pwcats.info and fill view with nodes
        AsyncTask<Object, Void, List<PwItemCat>> asyncTask = new RetrievePwItemCatTask().execute(new Object[]{id, server});
        return rootView;
    }

    private void fillViewWithNodes(List<PwItemCat> infos) {
        if (infos == null || infos.isEmpty()) {
            LayoutInflater vi = (LayoutInflater) parentActivity.getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View v = vi.inflate(R.layout.message, null);
            int message_id;
            if (infos == null)
                message_id = R.string.network_error;
            else
                message_id = R.string.nothing_found;
            ((TextView)v.findViewById(R.id.messageText)).setText(message_id);
// insert into main view
            ViewGroup insertPoint = (ViewGroup) rootView.findViewById(R.id.scrolledLinearView);
            insertPoint.addView(v, insertPoint.getChildCount(), new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            rootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
            return;
        }
        for (PwItemCat info : infos) {
            add_item_node_cat(info);
            Log.d(this.toString(), "item info added " + info.toString());
        }
        rootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
    }

    class RetrievePwItemCatTask extends AsyncTask<Object, Void, List<PwItemCat>> {
        List<PwItemCat> infos = null;
        Exception exception = null;
        protected List<PwItemCat> doInBackground(Object... id_server) {
            try {
                String ci_session = PreferenceManager.getDefaultSharedPreferences(parentActivity.getApplicationContext())
                        .getString(getString(R.string.ci_session), null);
                infos = PwcatsRequester.itemsCat((PwcatsRequester.Server)(id_server[1]), (Integer)id_server[0], ci_session);
                return infos;
            } catch (Exception e) {
                exception = e;
                Log.e(this.getClass().toString(), "error while requesting pwcats.info", e);
                return null;
            }
        }

        protected void onPostExecute(List<PwItemCat> feed) {
            if (exception == null)
                fillViewWithNodes(infos);
            else {
                rootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
            }
        }
    }

    public void add_item_node_cat(PwItemCat itemInfo) {
        LayoutInflater vi = (LayoutInflater) parentActivity.getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = vi.inflate(R.layout.item_details_node_cat, null);

// fill in any details dynamically here
        TextView itemCount = (TextView) v.findViewById(R.id.textItemCount);
        itemCount.setText("x" + itemInfo.getCount());

        TextView textCatTitle = (TextView) v.findViewById(R.id.textCatTitle);
        textCatTitle.setText(itemInfo.getCatTitle());
        TextView textCatName = (TextView) v.findViewById(R.id.textCatNickname);
        textCatName.setText(itemInfo.getNickname());

        // TODO http://www.pwmap.ru/
        TextView textLocationName = (TextView) v.findViewById(R.id.textLocationName);
        textLocationName.setText(itemInfo.getLocation().toString());
        TextView textLocationCoordinates = (TextView) v.findViewById(R.id.textLocationCoordinates);
        textLocationCoordinates.setText(String.format("%d %d", itemInfo.getCoord()[0], itemInfo.getCoord()[1]));

        TextView textItemCostLo = (TextView) v.findViewById(R.id.textItemCostLo);
        if (itemInfo.getPriceLo() != null)
            textItemCostLo.setText( String.format("%,d", itemInfo.getPriceLo()) );
        TextView textItemCostHi = (TextView) v.findViewById(R.id.textItemCostHi);
        if (itemInfo.getPriceHi() != null)
            textItemCostHi.setText( String.format("%,d", itemInfo.getPriceHi()) );

// sizes
        DisplayMetrics metrics = new DisplayMetrics();
        parentActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int width = metrics.widthPixels;
        View textItemCount = v.findViewById(R.id.textItemCount);
        textItemCount.setMinimumWidth((int) (width * 0.13f));
        View catNameLayout = v.findViewById(R.id.catNameLayout);
        catNameLayout.setMinimumWidth((int) (width * 0.47f));
        View locationLayout = v.findViewById(R.id.locationLayout);
        locationLayout.setMinimumWidth((int) (width * 0.20f));
        View itemCostLayout = v.findViewById(R.id.itemCostLayout);
        itemCostLayout.setMinimumWidth((int) (width * 0.23f));

// insert into main view
        ViewGroup insertPoint = (ViewGroup) rootView.findViewById(R.id.scrolledLinearView);
        insertPoint.addView(v, insertPoint.getChildCount(), new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    }
}

