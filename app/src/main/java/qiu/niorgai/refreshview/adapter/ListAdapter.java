package qiu.niorgai.refreshview.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import qiu.niorgai.refreshview.R;


/**
 * 简单的ListAdapter
 * Created by qiu on 9/8/15.
 */
public class ListAdapter extends ArrayAdapter<Integer> {

    private List<String> data;

    public ListAdapter(Context context, List<String> data) {
        super(context, R.layout.list_item);
        this.data = data;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = View.inflate(getContext(), R.layout.list_item, null);
        }
        ((TextView)convertView).setText(String.valueOf(position));
        return convertView;
    }
}
