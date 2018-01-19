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
        super.onResume();

        loadData();
    }

    private void loadData() {
        mArticle = BaseApplication.getInstance().getArticle();
        if (mArticle != null) {
            renderData();
        }
    }

    public void renderData() {
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

        Log.e(mTag, mArticle.getHtml());

        // https://medium.com/@rajeefmk/android-textview-and-image-loading-from-url-part-1-a7457846abb6
        Spannable spannable = ImageUtil.getSpannableHtmlWithImageGetter(mContext, mTvContent, mArticle.getHtml());
        mTvContent.setText(spannable);
        mTvContent.setMovementMethod(LinkMovementMethod.getInstance()); // URL 클릭 시 이동

        goTop();
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

