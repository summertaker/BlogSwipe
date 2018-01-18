package com.summertaker.blog;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.summertaker.blog.common.BaseApplication;
import com.summertaker.blog.common.BaseFragment;
import com.summertaker.blog.data.Article;
import com.summertaker.blog.util.ImageUtil;
import com.summertaker.blog.util.Util;

import java.util.Calendar;
import java.util.Date;

public class ArticleDetailFragment extends BaseFragment {

    private ArticleListFragment.Callback mCallback;

    private int mPosition = 0;

    private Article mArticle;

    private ScrollView mScrollView;
    private TextView mTvTitle;
    private TextView mTvDate;
    private TextView mTvToday;
    private TextView mTvYesterday;
    private TextView mTvContent;

    public interface Callback {
        void onArticleDetailCallback(String message, Article article);
    }

    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(int position) {
        ArticleDetailFragment fragment = new ArticleDetailFragment();

        Bundle args = new Bundle();
        args.putInt("position", position);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.article_detail_fragment, container, false);

        mContext = inflater.getContext();

        Bundle bundle = getArguments();
        mPosition = bundle.getInt("position", 0);

        mScrollView = rootView.findViewById(R.id.scrollView);
        mTvTitle = rootView.findViewById(R.id.tvTitle);
        mTvDate = rootView.findViewById(R.id.tvDate);
        mTvToday = rootView.findViewById(R.id.tvToday);
        mTvYesterday = rootView.findViewById(R.id.tvYesterday);
        mTvContent = rootView.findViewById(R.id.tvContent);

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

        //--------------------------------------------------------------------------------------------
        // 앱이 최소화되거나 화면 분할 모드에 진입하면 Activity 의 Adapter 가 null 이 된다.
        // 앱 화면이 복귀되면 onResume() 이 실행되는데 이 때 데이터를 다시 읽어와 Adapter 를 설정해 준다.
        // 다시 읽어온 데이터를 Fragment 에도 적용하기 위해 Activity 와 똑같이 onResume() 에서 처리한다.
        //--------------------------------------------------------------------------------------------
        //ArrayList<Member> favorites = BaseApplication.getInstance().getFavorites();
        //mBlogUrl = members.get(mPosition).getBlogUrl();
        //Log.e(mTag, "onCreateView(): "+ mPosition + " / " + mBlogUrl);

        loadData();
    }

    private void loadData() {
        //Log.e(mTag, "loadData()...");

        mArticle = BaseApplication.getInstance().getArticle();
        if (mArticle != null) {
            renderData();
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

    private void parseData(String response) {
        if (response.isEmpty()) {
            Util.alert(mContext, getString(R.string.error), "response is empty.", null);
        } else {
            mArticles.clear();

            if (mBlogUrl.contains("nogizaka46")) {
                Nogizaka46Parser nogizaka46Parser = new Nogizaka46Parser();
                nogizaka46Parser.parseBlogDetail(response, mArticles);
            } else if (mBlogUrl.contains("keyakizaka46")) {
                Keyakizaka46Parser keyakizaka46Parser = new Keyakizaka46Parser();
                keyakizaka46Parser.parseBlogDetail(response, mArticles);
            }

            renderData();
        }
    }
    */

    public void renderData() {
        mScrollView.scrollTo(0, 0);
        //mScrollView.smoothScrollTo(0, 0);
        //mScrollView.fullScroll(ScrollView.FOCUS_UP);

        mTvTitle.setText(mArticle.getTitle());

        String blogDate = Util.parseDate(mArticle.getDate());
        mTvDate.setText(blogDate);

        Date date = Util.getDate(mArticle.getDate());
        Date today = new Date();
        if (Util.isSameDate(today, date)) {
            mTvToday.setVisibility(View.VISIBLE);
        }

        Calendar c = Calendar.getInstance();
        c.setTime(today);
        c.add(Calendar.DATE, -1);
        Date yesterday = c.getTime();
        if (Util.isSameDate(yesterday, date)) {
            mTvYesterday.setVisibility(View.VISIBLE);
        }

        // https://medium.com/@rajeefmk/android-textview-and-image-loading-from-url-part-1-a7457846abb6
        Spannable spannable = ImageUtil.getSpannableHtmlWithImageGetter(mContext, mTvContent, mArticle.getHtml());
        mTvContent.setText(spannable);
        mTvContent.setMovementMethod(LinkMovementMethod.getInstance()); // URL 클릭 시 이동
    }

    public void goTop() {
        //Log.e(mTag, "goTop()..." + mPosition);
        mScrollView.scrollTo(0, 0);
        //mScrollView.smoothScrollTo(0, 0);
        //mScrollView.fullScroll(ScrollView.FOCUS_UP);
    }

    public void refresh() {
        //Log.e(mTag, "refresh()..." + mPosition);
        loadData();
    }
}

