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

import qiu.niorgai.refreshview.bottom.AutoLoadListView;
import qiu.niorgai.refreshview.bottom.Interface;
import qiu.niorgai.refreshview.R;
import qiu.niorgai.refreshview.adapter.ListAdapter;
import qiu.niorgai.refreshview.top.SuperRefreshLayout;

public class ListViewActivity extends AppCompatActivity implements Interface.LoadMoreListener {

    private SuperRefreshLayout refreshLayout;

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

        mAdapter = new ListAdapter(this, data);
        listView.setAdapter(mAdapter);
        listView.setEmptyView(findViewById(R.id.empty_view));

        refreshLayout = (SuperRefreshLayout) findViewById(R.id.refresh_layout);
        refreshLayout.setOnRefreshListener(new SuperRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Toast.makeText(ListViewActivity.this, "start refresh", Toast.LENGTH_SHORT).show();
                refreshLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(ListViewActivity.this, "refresh finish", Toast.LENGTH_SHORT).show();
                        refreshLayout.setRefreshing(false);

                        //add data
                        data.clear();
                        for (int i=0; i<10; i++) {
                            data.add("");
                        }
                        mAdapter.notifyDataSetChanged();
                        //more data
                        listView.setIsHaveMore(true);
                    }
                }, 3000);
            }
        });
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

                listView.setIsHaveMore(count != 4);
//                or Error
//                listView.onError();
            }
        }, 3000);
    }
}
