package qiu.niorgai.refreshview.activity;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import qiu.niorgai.refreshview.bottom.AutoLoadRecyclerView;
import qiu.niorgai.refreshview.bottom.LoadMoreInterface;
import qiu.niorgai.refreshview.R;
import qiu.niorgai.refreshview.adapter.RecyclerAdapter;
import qiu.niorgai.refreshview.top.SuperRefreshLayout;

public class RecyclerViewActivity extends AppCompatActivity implements LoadMoreInterface.onLoadMoreListener {

    private SuperRefreshLayout refreshLayout;

    private AutoLoadRecyclerView recyclerView;

    private RecyclerAdapter mAdapter;

    private List<String> data;

    private int count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycler_view);
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

        recyclerView = (AutoLoadRecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setOnLoadMoreListener(this);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        data = new ArrayList<>();

        mAdapter = new RecyclerAdapter(this, data);
        recyclerView.setAdapter(mAdapter);

        refreshLayout = (SuperRefreshLayout) findViewById(R.id.refresh_layout);
        refreshLayout.setOnRefreshListener(new SuperRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Toast.makeText(RecyclerViewActivity.this, "start refresh", Toast.LENGTH_SHORT).show();
                refreshLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(RecyclerViewActivity.this, "refresh finish", Toast.LENGTH_SHORT).show();
                        refreshLayout.setRefreshing(false);

                        //add data
                        data.clear();
                        for (int i=0; i<10; i++) {
                            data.add("");
                        }
                        mAdapter.notifyDataSetChanged();
                        //more data
                        recyclerView.setIsHaveMore(true);
                    }
                }, 3000);
            }
        });
    }

    @Override
    public void onLoadMore() {
        Toast.makeText(this, "start loading more", Toast.LENGTH_SHORT).show();
        recyclerView.postDelayed(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(RecyclerViewActivity.this, "load more finish", Toast.LENGTH_SHORT).show();
                for (int i = 0; i < 30; i++) {
                    data.add("");
                }
                mAdapter.notifyDataSetChanged();

                count++;
                recyclerView.onSuccess(count != 4);
//                or load Error
//                recyclerView.onFailure();
            }
        }, 3000);
    }
}
