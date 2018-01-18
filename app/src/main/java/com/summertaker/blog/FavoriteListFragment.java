package com.summertaker.blog;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.summertaker.blog.common.BaseApplication;
import com.summertaker.blog.common.BaseFragment;
import com.summertaker.blog.common.Config;
import com.summertaker.blog.data.Article;
import com.summertaker.blog.data.Member;
import com.summertaker.blog.parser.Keyakizaka46Parser;
import com.summertaker.blog.parser.Nogizaka46Parser;
import com.summertaker.blog.util.Util;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class FavoriteListFragment extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener {

    private Callback mCallback;

    private int mPosition = 0;

    private LinearLayout mLoLoading;
    private TextView tvLoadingCount;
    private ProgressBar mPbLoadingHorizontal;

    private SwipeRefreshLayout mSwipeRefresh;

    private int mLoadCount = 0;

    private ArrayList<Member> mFavorites;
    private FavoriteListAdapter mAdapter;
    private GridView mGridView;

    public interface Callback {
        void onFavoriteListCallback(String message, Member member);
    }

    public FavoriteListFragment() {
    }

    public static FavoriteListFragment newInstance(int position) {
        FavoriteListFragment fragment = new FavoriteListFragment();

        Bundle args = new Bundle();
        args.putInt("position", position);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.favorite_list_fragment, container, false);

        mContext = inflater.getContext();

        Bundle bundle = getArguments();
        mPosition = bundle.getInt("position", 0);

        mLoLoading = rootView.findViewById(R.id.loLoading);
        tvLoadingCount = rootView.findViewById(R.id.tvLoadingCount);
        mPbLoadingHorizontal = rootView.findViewById(R.id.pbLoadingHorizontal);

        mSwipeRefresh = rootView.findViewById(R.id.swipeRefreshLayout);
        mSwipeRefresh.setOnRefreshListener(this);

        mGridView = rootView.findViewById(R.id.gridView);
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Member member = (Member) adapterView.getItemAtPosition(i);

                BaseApplication.getInstance().setMember(member);
                mCallback.onFavoriteListCallback("onFavoriteClick", member);
            }
        });

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Activity activity;
        if (context instanceof Activity) {
            activity = (Activity) context;

            try {
                mCallback = (Callback) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity.toString() + " must implement Listener for Fragment.");
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (BaseApplication.getInstance().isFavoriteChanged()) {
            loadData();
        } else {
            if (mFavorites == null) {
                mFavorites = BaseApplication.getInstance().getFavorites();
            }
            renderData();
        }
    }

    private void loadData() {
        mLoLoading.setVisibility(View.VISIBLE);
        mSwipeRefresh.setVisibility(View.GONE);

        tvLoadingCount.setText("");
        mPbLoadingHorizontal.setProgress(0);

        mFavorites = BaseApplication.getInstance().getFavorites();

        for (Member member : mFavorites) {
            requestData(member.getBlogUrl());
        }
    }

    private void requestData(final String url) {
        //Log.e(mTag, "url: " + url);

        StringRequest strReq = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                //Log.d(mTag, response.toString());
                writeData(url, response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Util.alert(mContext, getString(R.string.error), error.getMessage(), null);
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                //headers.put("Content-Type", "application/json; charset=utf-8");
                headers.put("User-agent", Config.USER_AGENT_DESKTOP);
                return headers;
            }
        };

        BaseApplication.getInstance().addToRequestQueue(strReq, mVolleyTag);
    }

    private void writeData(String url, String response) {
        Util.writeToFile(Util.getUrlToFileName(url) + ".html", response);
        parseData(url, response);
    }

    private void parseData(String url, String response) {
        if (!response.isEmpty()) {
            ArrayList<Article> articles = new ArrayList<>();
            if (url.contains("nogizaka46")) {
                Nogizaka46Parser nogizaka46Parser = new Nogizaka46Parser();
                nogizaka46Parser.parseBlogDetail(response, articles);
            } else if (url.contains("keyakizaka46")) {
                Keyakizaka46Parser keyakizaka46Parser = new Keyakizaka46Parser();
                keyakizaka46Parser.parseBlogDetail(response, articles);
            }

            //Log.e(mTag, "articles.size(): " + articles.size());

            /*
            Member member = null;
            for (Member m : mFavorites) {
                if (url.equals(m.getBlogUrl())) {
                    member = m;
                    break;
                }
            }

            if (member != null) {
                if (articles.size() > 0) {
                    Article article = articles.get(0);
                    Date date = Util.getDate(article.getDate());
                    //Log.e(mTag, member.getName() + " " + date.toString());

                    // https://stackoverflow.com/questions/22039991/how-to-compare-two-dates-along-with-time-in-java
                    int compareTo = date.compareTo(mLastCheckDate);
                    if (compareTo > 0) {
                        member.setUpdated(true);
                    } else if (compareTo < 0) {
                        member.setUpdated(false);
                    }
                } else {
                    member.setUpdated(false);
                }
            }
            */

            mLoadCount++;

            if (mLoadCount < mFavorites.size()) {
                //loadData();
                updateProgress();
            } else {
                //Collections.sort(mArticles, Collections.reverseOrder());
                renderData();
            }
        }
    }

    private void updateProgress() {
        int count = mLoadCount + 1;

        String text = "( " + count + " / " + mFavorites.size() + " )";
        tvLoadingCount.setText(text);

        float progress = (float) count / (float) mFavorites.size();
        int progressValue = (int) (progress * 100.0);

        mPbLoadingHorizontal.setProgress(progressValue);
    }

    public void renderData() {
        mLoLoading.setVisibility(View.GONE);
        mSwipeRefresh.setVisibility(View.VISIBLE);

        mAdapter = new FavoriteListAdapter(mContext, mFavorites);
        mGridView.setAdapter(mAdapter);

        mLoadCount = 0;
        mSwipeRefresh.setRefreshing(false);

        BaseApplication.getInstance().setFavoriteChanged(false);
    }

    @Override
    public void onRefresh() {
        //Log.e(mTag, "onRefresh()...");
        //mSwipeRefresh.setRefreshing(false);
        loadData();
    }

    public void goTop() {
        //Log.e(mTag, "goTop()..." + mPosition);
        mGridView.setSelection(0);
        //mListView.smoothScrollToPosition(0);
        //mListView.setSelectionAfterHeaderView();
    }

    public void refresh() {
        //Log.e(mTag, "refresh()..." + mPosition);
        loadData();
    }
}
