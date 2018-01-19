package com.summertaker.blog;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.summertaker.blog.common.BaseActivity;
import com.summertaker.blog.common.BaseApplication;
import com.summertaker.blog.common.Config;
import com.summertaker.blog.data.Article;
import com.summertaker.blog.data.Member;

import java.io.File;

public class MainActivity extends BaseActivity implements /*NavigationView.OnNavigationItemSelectedListener,*/
        FavoriteListFragment.Callback, ArticleListFragment.Callback, ArticleDetailFragment.Callback {

    private static final int REQUEST_PERMISSION_CODE = 100;
    private boolean mIsPermissionGranted = false;

    private Toolbar mToolbar;
    private ActionBar mActionBar;

    private DrawerLayout mDrawer;
    private int mNavItemId = 0;

    private SectionsPagerAdapter mPagerAdapter;
    private ViewPager mViewPager;
    private int mViewPagerTotal = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        mContext = MainActivity.this;

        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        mToolbar.setNavigationIcon(R.drawable.ic_menu_white_24dp);
        mToolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onToolbarClick();
            }
        });

        mActionBar = getSupportActionBar();
        if (mActionBar != null) {
            mActionBar.setDisplayShowHomeEnabled(true);
            //actionBar.setDisplayHomeAsUpEnabled(true); // Left Arrow Icon
        }

        mDrawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mDrawer, null, R.string.drawer_open, R.string.drawer_close) {
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                //Log.e(mTag, "mNavItemId: " + mNavItemId);
                if (mNavItemId == R.id.nav_member) {
                    Intent intent = new Intent(mContext, GroupListActivity.class);
                    startActivityForResult(intent, Config.REQUEST_CODE);
                }
                mNavItemId = 0;
            }

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }
        };
        mDrawer.addDrawerListener(toggle);
        //toggle.syncState();

        NavigationView view = (NavigationView) findViewById(R.id.navigation_view);
        view.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                //Log.e(mTag, "onNavigationItemSelected()...");
                mNavItemId = item.getItemId();
                mDrawer.closeDrawer(Gravity.START);
                return false;
            }
        });

        mViewPager = findViewById(R.id.view_pager);

        //----------------------------------------------------------------------------
        // 런타임에 권한 요청
        // https://developer.android.com/training/permissions/requesting.html?hl=ko
        //----------------------------------------------------------------------------
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        }, REQUEST_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        //Log.e(mTag, ">> onRequestPermissionsResult()...");

        switch (requestCode) {
            case REQUEST_PERMISSION_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    String path = Config.DATA_PATH;
                    File dir = new File(path);
                    if (!dir.exists()) {
                        boolean isSuccess = dir.mkdirs(); // 캐쉬 파일 저장 위치 생성
                    }
                    mIsPermissionGranted = true;
                } else {
                    onPermissionDenied();
                }
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    void onPermissionDenied() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.app_name));
        builder.setMessage(R.string.access_denied);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        builder.show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Log.e(mTag, "onCreateOptionsMenu()...");

        int id = -1;
        int position = mViewPager.getCurrentItem();

        switch (position) {
            case 0:
                id = R.menu.main;
                break;
            case 1:
                //id = R.menu.article_list;
                break;
            case 2:
                //id = R.menu.article_detail;
                break;
        }
        if (id > -1) {
            getMenuInflater().inflate(id, menu);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                int position = mViewPager.getCurrentItem();
                if (position == 0) {
                    mDrawer.openDrawer(Gravity.START);
                } else {
                    mViewPager.setCurrentItem(position - 1);
                }
                return true;
            case R.id.action_add:
                Intent groupList = new Intent(mContext, GroupListActivity.class);
                startActivityForResult(groupList, Config.REQUEST_CODE);
                return true;
            case R.id.action_refresh:
                runFragment("refresh", 0);
                return true;
            case R.id.action_open_in_new:
                // 데스크탑용 페이지를 크롤링해 왔기에 모바일 브라우저에서 접속하면 404 발생
                Member member = BaseApplication.getInstance().getMember();
                if (member != null) {
                    String uri = member.getBlogUrl();
                    Log.e(mTag, "uri: " + uri);
                    Intent openInNew = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                    startActivity(openInNew);
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /*
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Log.e(mTag, "onNavigationItemSelected()...");
        mNavItemId = item.getItemId();
        mDrawer.closeDrawer(GravityCompat.START);
        return false;
    }
    */

    @Override
    public void onResume() {
        super.onResume();

        if (mIsPermissionGranted) {
            init();
        }
    }

    @Override
    public void onBackPressed() {
        if (mDrawer.isDrawerOpen(GravityCompat.START)) {
            mDrawer.closeDrawer(GravityCompat.START);
        } else {
            int position = mViewPager.getCurrentItem();
            if (position > 0) {
                mViewPager.setCurrentItem(position - 1);
            } else {
                super.onBackPressed();
            }
        }
    }

    public void onToolbarClick() {
        //Log.e(mTag, "onToolbarClick()...");
        runFragment("goTop", mViewPager.getCurrentItem());
    }

    private void init() {
        mPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.setOffscreenPageLimit(mViewPagerTotal);

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                //Log.e(mTag, "onPageScrolled().position: " + position);
            }

            @Override
            public void onPageSelected(int position) {
                if (position == 0) {
                    //Log.e(mTag, "onPageSelected().position: " + position);
                    mToolbar.setNavigationIcon(R.drawable.ic_menu_white_24dp);
                    mActionBar.setTitle(R.string.app_name);
                    runFragment("refreshAdapter", 0);
                } else {
                    mToolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
                    Member member = BaseApplication.getInstance().getMember();
                    if (member != null) {
                        mActionBar.setTitle(member.getName());
                    }
                }

                invalidateOptionsMenu(); // 툴바 오른쪽 메뉴 설정
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                //Log.e(mTag, "onPageScrollStateChanged().state: " + state);
            }
        });
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            //return PlaceholderFragment.newInstance(position + 1);
            switch (position) {
                case 0:
                    return FavoriteListFragment.newInstance(position + 1);
                case 1:
                    return ArticleListFragment.newInstance(position + 1);
                case 2:
                    return ArticleDetailFragment.newInstance(position + 1);
            }
            return null;
        }

        @Override
        public int getCount() {
            return mViewPagerTotal;
        }
    }

    public void runFragment(String command, int positon) {
        //--------------------------------------------------------------------------------------------
        // 프레그먼트에 이벤트 전달하기
        // https://stackoverflow.com/questions/34861257/how-can-i-set-a-tag-for-viewpager-fragments
        //--------------------------------------------------------------------------------------------
        //Fragment fragment = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.view_pager + ":" + mViewPager.getCurrentItem());
        Fragment fragment = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.view_pager + ":" + positon);

        // based on the current position you can then cast the page to the correct Fragment class
        // and call some method inside that fragment to reload the data:
        //if (0 == mViewPager.getCurrentItem() && null != f) {
        if (fragment != null) {
            if (positon == 0) {
                FavoriteListFragment favoriteListFragment = (FavoriteListFragment) fragment;
                switch (command) {
                    case "goTop":
                        favoriteListFragment.goTop();
                        break;
                    case "refresh":
                        favoriteListFragment.refresh();
                        break;
                    case "refreshAdapter":
                        favoriteListFragment.refreshAdapter();
                        break;
                }
            } else if (positon == 1) {
                ArticleListFragment articleListFragment = (ArticleListFragment) fragment;
                switch (command) {
                    case "goTop":
                        articleListFragment.goTop();
                        break;
                    case "refresh":
                        articleListFragment.refresh();
                        break;
                }
            } else if (positon == 2) {
                ArticleDetailFragment articleDetailFragment = (ArticleDetailFragment) fragment;
                switch (command) {
                    case "goTop":
                        articleDetailFragment.goTop();
                        break;
                    case "refresh":
                        articleDetailFragment.refresh();
                        break;
                }
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Config.REQUEST_CODE && data != null) { // && resultCode == Activity.RESULT_OK) {
            boolean isDataChanged = data.getBooleanExtra("isDataChanged", false);
            BaseApplication.getInstance().setFavoriteChanged(isDataChanged);
        }
    }

    @Override
    public void onFavoriteListCallback(String message, Member member) {
        switch (message) {
            case "onFavoriteClick":
                //mActionBar.setTitle(member.getName());
                if (mViewPagerTotal == 1) {
                    mViewPagerTotal = 2;
                }
                mPagerAdapter.notifyDataSetChanged();

                mViewPager.setCurrentItem(1);
                runFragment("refresh", 1);
                break;
        }
    }

    @Override
    public void onArticleListCallback(String message, Article article) {
        switch (message) {
            case "onArticleClick":
                if (mViewPagerTotal == 2) {
                    mViewPagerTotal = 3;
                }
                mPagerAdapter.notifyDataSetChanged();

                mViewPager.setCurrentItem(2);
                runFragment("refresh", 2);
                break;
        }
    }

    @Override
    public void onArticleDetailCallback(String message, Article article) {

    }
}
