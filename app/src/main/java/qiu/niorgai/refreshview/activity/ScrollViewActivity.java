package qiu.niorgai.refreshview.activity;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import qiu.niorgai.refreshview.bottom.AutoAutoLoadScrollView;
import qiu.niorgai.refreshview.bottom.Interface;
import qiu.niorgai.refreshview.R;
import qiu.niorgai.refreshview.top.SuperRefreshLayout;

public class ScrollViewActivity extends AppCompatActivity implements Interface.LoadMoreListener {

    private SuperRefreshLayout refreshLayout;

    private AutoAutoLoadScrollView scrollView;

    private View content;

    private int count;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scroll_view);
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

        scrollView = (AutoAutoLoadScrollView) findViewById(R.id.scroll_view);
        content = findViewById(R.id.content);

        scrollView.setLoadMoreListener(this);

        refreshLayout = (SuperRefreshLayout) findViewById(R.id.refresh_layout);
        refreshLayout.setOnRefreshListener(new SuperRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Toast.makeText(ScrollViewActivity.this, "start refresh", Toast.LENGTH_SHORT).show();
                refreshLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(ScrollViewActivity.this, "refresh finish", Toast.LENGTH_SHORT).show();
                        refreshLayout.setRefreshing(false);

                        //add data
                        ViewGroup.LayoutParams params = content.getLayoutParams();
                        params.height = (int) (getResources().getDisplayMetrics().density * 300);
                        content.setLayoutParams(params);

                        //more data
                        scrollView.setIsHaveMore(true);
                    }
                }, 3000);
            }
        });
    }

    @Override
    public void loadMore() {
        Toast.makeText(this, "start loading more", Toast.LENGTH_SHORT).show();
        scrollView.postDelayed(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ScrollViewActivity.this, "load more finish", Toast.LENGTH_SHORT).show();

                ViewGroup.LayoutParams params = content.getLayoutParams();
                params.height += 600;
                content.setLayoutParams(params);

                count++;

                scrollView.onComplete(count != 4);
//                or Error
//                scrollView.onError();
            }
        }, 3000);
    }
}
