package com.summertaker.blog;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.summertaker.blog.common.BaseApplication;
import com.summertaker.blog.common.BaseFragment;
import com.summertaker.blog.common.Config;
import com.summertaker.blog.data.Article;
import com.summertaker.blog.data.Member;
import com.summertaker.blog.parser.Keyakizaka46Parser;
import com.summertaker.blog.parser.Nogizaka46Parser;
import com.summertaker.blog.util.Util;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class ArticleListFragment extends BaseFragment implements ArticleListInterface {

    private ArticleListFragment.Callback mCallback;

    private int mPosition = 0;

    private Member mMember;
    private ArrayList<Article> mArticles;

    private ArticleListAdapter mAdapter;
    private ListView mListView;

    public interface Callback {
        void onArticleListCallback(String message, Article article);
    }

    public ArticleListFragment() {
    }

    public static ArticleListFragment newInstance(int position) {
        ArticleListFragment fragment = new ArticleListFragment();

        Bundle args = new Bundle();
        args.putInt("position", position);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.article_list_fragment, container, false);

        mContext = inflater.getContext();

        Bundle bundle = getArguments();
        mPosition = bundle.getInt("position", 0);

        mListView = rootView.findViewById(R.id.listView);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Article article = (Article) adapterView.getItemAtPosition(i);

                BaseApplication.getInstance().setArticle(article);
                mCallback.onArticleListCallback("onArticleClick", article);
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
                mCallback = (ArticleListFragment.Callback) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity.toString() + " must implement Listener for Fragment.");
            }
        }
    }

    @Override
    public void onResume() {
        //Log.e(mTag, ">> onResume()...");
        super.onResume();

        // Activity 에서 호출하는 refresh() 함수에서 loadData() 를 실행하니 여기서는 실행하지 않는다.
        //loadData();
    }

    private void loadData() {
        mMember = BaseApplication.getInstance().getMember();
        if (mMember != null) { // && mMember.getBlogUrl() != null && !mMember.getBlogUrl().isEmpty()) {
            String fileName = Util.getUrlToFileName(mMember.getBlogUrl()) + ".html";
            //Log.e(mTag, "loadData().fileName: " + fileName);

            File file = new File(Config.DATA_PATH, fileName);
            //Date lastModDate = new Date(file.lastModified());
            //Log.e(mTag, "File last modified: " + lastModDate.toString());

            //boolean isSameDate = Util.isSameDate(lastModDate, Calendar.getInstance().getTime());
            //if (isSameDate) {
            //Log.e(mTag, ">>>>> parseData()...");
            parseData(Util.readFile(fileName));
            //}
        }
    }

    /*
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
        parseData(response);
    }
    */

    private void parseData(String response) {
        if (response.isEmpty()) {
            Util.alert(mContext, getString(R.string.error), "response is empty.", null);
        } else {
            mArticles = new ArrayList<>();

            if (mMember.getBlogUrl().contains("nogizaka46")) {
                Nogizaka46Parser nogizaka46Parser = new Nogizaka46Parser();
                nogizaka46Parser.parseBlogDetail(response, mArticles);
            } else if (mMember.getBlogUrl().contains("keyakizaka46")) {
                Keyakizaka46Parser keyakizaka46Parser = new Keyakizaka46Parser();
                keyakizaka46Parser.parseBlogDetail(response, mArticles);
            }

            renderData();
        }
    }

    public void renderData() {
        mAdapter = new ArticleListAdapter(mContext, mArticles, this);
        mListView.setAdapter(mAdapter);

        //--------------------------
        // 최종 블로그 일자 저장하기
        //--------------------------
        if (mArticles.size() > 0) {
            Article article = mArticles.get(0);
            ArrayList<Member> favorites = BaseApplication.getInstance().getFavorites();

            for (Member member : favorites) {
                if (member.getBlogUrl().equals(mMember.getBlogUrl())) {
                    member.setLastDate(article.getDate());
                    member.setUpdated(false);
                    //Log.e(mTag, member.getName() + ".setUpdated(false)");
                }
            }

            BaseApplication.getInstance().saveMember(Config.PREFERENCE_KEY_FAVORITES, favorites);
        }
    }

    @Override
    public void onTitleClick(Article article) {
        BaseApplication.getInstance().setArticle(article);
        mCallback.onArticleListCallback("onArticleClick", article);
    }

    @Override
    public void onContentClick(Article article) {
        BaseApplication.getInstance().setArticle(article);
        mCallback.onArticleListCallback("onArticleClick", article);
    }

    @Override
    public void onImageClick(Article article, String imageUrl) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(imageUrl));
        startActivity(intent);
    }

    public void goTop() {
        //Log.e(mTag, "goTop()..." + mPosition);
        mListView.setSelection(0);
        //mListView.smoothScrollToPosition(0);
        //mListView.setSelectionAfterHeaderView();
    }

    public void refresh() {
        //Log.e(mTag, "refresh()..." + mPosition);
        loadData();
    }
}
