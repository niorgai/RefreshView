package qiu.niorgai.refreshview.activity;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import qiu.niorgai.refreshview.AutoLoadListView;
import qiu.niorgai.refreshview.LoadMoreInterface;
import qiu.niorgai.refreshview.R;
import qiu.niorgai.refreshview.adapter.ListAdapter;

public class ListViewActivity extends AppCompatActivity implements LoadMoreInterface.LoadMoreListener {

    private AutoLoadListView listView;

    private ListAdapter mAdapter;

    private List<String> data;

    private int count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_view);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        listView = (AutoLoadListView) findViewById(R.id.list_view);
        listView.setLoadMoreListener(this);

        data = new ArrayList<>();
        for (int i=0; i<10; i++) {
            data.add("");
        }

        mAdapter = new ListAdapter(this, data);
        listView.setAdapter(mAdapter);
        listView.setIsHaveMore(true);
    }

    @Override
    public void loadMore() {
        Toast.makeText(this, "start loading more", Toast.LENGTH_SHORT).show();
        listView.postDelayed(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ListViewActivity.this, "load more finish", Toast.LENGTH_SHORT).show();
                for (int i=0; i<10; i++) {
                    data.add("");
                }
                mAdapter.notifyDataSetChanged();
                count ++;
                listView.setIsHaveMore(count != 3);
            }
        }, 3000);
    }
}
