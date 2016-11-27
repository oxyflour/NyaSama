package com.nyasama.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.toolbox.NetworkImageView;
import com.nyasama.R;
import com.nyasama.ThisApp;
import com.nyasama.fragment.DiscuzComicListFragment;
import com.nyasama.fragment.DiscuzForumIndexFragment;
import com.nyasama.fragment.DiscuzThreadListFragment;
import com.nyasama.fragment.SimpleLayoutFragment;
import com.nyasama.util.Discuz;
import com.nyasama.util.Helper;

import org.json.JSONObject;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        DiscuzThreadListFragment.OnThreadListInteraction {

    public void loadUserInfo() {
        // TODO: find a better way to refresh group name
        Discuz.execute("forumindex", new HashMap<String, Object>(),
                null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObject) {
                        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
                        View headerView = navigationView.getHeaderView(0);
                        if (Discuz.sHasLogined) {
                            String avatar_url = Discuz.DISCUZ_URL +
                                    "uc_server/avatar.php?uid=" + Discuz.sUid + "&size=medium";
                            ((NetworkImageView) headerView.findViewById(R.id.drawer_avatar))
                                    .setImageUrl(avatar_url, ThisApp.imageLoader);
                            ((TextView) headerView.findViewById(R.id.drawer_username)).setText(Discuz.sUsername);
                            ((TextView) headerView.findViewById(R.id.drawer_group)).setText(Discuz.sGroupName);
                        }
                        findViewById(R.id.nav_view).findViewById(R.id.show_logined).setVisibility(Discuz.sHasLogined ? View.VISIBLE : View.GONE);
                        findViewById(R.id.nav_view).findViewById(R.id.hide_logined).setVisibility(Discuz.sHasLogined ? View.GONE : View.VISIBLE);
                    }
                });
    }

    public void gotoLogin(View view) {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivityForResult(intent, LoginActivity.REQUEST_CODE_LOGIN);
    }

    public void gotoProfile(View view) {
        startActivity(new Intent(MainActivity.this, UserProfileActivity.class));
    }

    public void gotoDonate(View view) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://thwiki.cc/THBWiki:%E6%8D%90%E6%AC%BE")));
    }

    // REF: http://stackoverflow.com/questions/8430805/android-clicking-twice-the-back-button-to-exit-activity
    private boolean doubleBackToExitPressedOnce;
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }

        if (doubleBackToExitPressedOnce) {
            finish();
            return;
        }

        doubleBackToExitPressedOnce = true;
        Helper.toast(getString(R.string.toast_click_again_to_exit));

        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000);
    }

    BroadcastReceiver mLoginReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadUserInfo();
        }
    };

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(ThisApp.context).unregisterReceiver(mLoginReceiver);
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Uri uri = intent.getData();
        if (intent.getData() != null) {
            // TODO: deal with pages, titles, etc
            final int tid = Helper.toSafeInteger(uri.getQueryParameter("tid"), 0);
            final int fid = Helper.toSafeInteger(uri.getQueryParameter("fid"), 0);
            if (tid > 0) startActivity(new Intent(this, PostListActivity.class) {{
                putExtra("tid", tid);
            }});
            else if (fid > 0) startActivity(new Intent(this, ThreadListActivity.class) {{
                putExtra("fid", fid);
            }});
        }
        else {
            super.onNewIntent(intent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setItemIconTintList(null);

        /*
        TODO: enable this to use umeng
        PushAgent mPushAgent = PushAgent.getInstance(this);
        mPushAgent.enable();
        Log.d("DEVICETOKEN", UmengRegistrar.getRegistrationId(this));
        */

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, PlaceholderFragment.newInstance(1))
                .commit();

        loadUserInfo();

        LocalBroadcastManager.getInstance(ThisApp.context)
                .registerReceiver(mLoginReceiver, new IntentFilter(Discuz.BROADCAST_FILTER_LOGIN));

        onNewIntent(getIntent());
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId(), section = 0;

        if (id == R.id.nav_forum) {
            section = 1;
        }
        else if (id == R.id.nav_pref) {
            startActivity(new Intent(this, SettingActivity.class));
            return true;
        }
        else if (id == R.id.nav_about) {
            startActivity(new Intent(this, AboutActivity.class));
            return true;
        }
        else if (id == R.id.nav_thb) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://thwiki.cc")));
            return true;
        }
        else if (id == R.id.nav_thv) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://thvideo.tv")));
            return true;
        }
        else if (id == R.id.nav_donate) {
            gotoDonate(null);
            return true;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, PlaceholderFragment.newInstance(section))
                .commit();
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_search) {
            startActivity(new Intent(this, SearchActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onGetThreadData(DiscuzThreadListFragment fragment) {
        if (fragment.getMessage() != null) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(R.string.error_no_internet)
                    .setMessage(fragment.getMessage())
                    .setPositiveButton(android.R.string.yes, null)
                    .show();
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
        public MainActivity mActivity;

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            if (getArguments().getInt(ARG_SECTION_NUMBER) == 1) {
                View rootView = inflater.inflate(R.layout.fragment_main_home, container, false);
                ViewPager pager = (ViewPager) rootView.findViewById(R.id.view_pager);
                pager.setAdapter(new FragmentStatePagerAdapter(mActivity.getSupportFragmentManager()) {
                    @Override
                    public android.support.v4.app.Fragment getItem(int i) {
                        if (i == 0)
                            return new DiscuzForumIndexFragment();
                        else if (i == 1)
                            // Note: Discuz returns 50 hot threads by default.
                            // we set the page size to be 60 so that it will not load more
                            return DiscuzThreadListFragment.getNewFragment(0, 0, 60);
                        else if (i == 2)
                            return DiscuzComicListFragment.getNewFragment();
                        else
                            return new SimpleLayoutFragment();
                    }
                    @Override
                    public int getCount() {
                        return 3;
                    }
                    @Override
                    public CharSequence getPageTitle(int position) {
                        int[] titles = {
                                R.string.title_main_home,
                                R.string.title_main_hot_threads,
                                R.string.title_main_translated,
                        };
                        return position < titles.length ? getString(titles[position]) : "Blank "+position;
                    }
                });
                PagerTabStrip tab = (PagerTabStrip) rootView.findViewById(R.id.view_pager_tab);
                ((ViewPager.LayoutParams) tab.getLayoutParams()).isDecor = true;
                return rootView;
            }
            return inflater.inflate(R.layout.fragment_blank, container, false);
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            mActivity = (MainActivity) activity;
        }
    }

}
