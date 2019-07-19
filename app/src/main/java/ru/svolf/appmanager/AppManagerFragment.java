package ru.svolf.appmanager;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import apk.tool.patcher.R;
import ru.svolf.melissa.swipeback.SwipeBackFragment;
import ru.svolf.melissa.swipeback.SwipeBackLayout;


public class AppManagerFragment extends SwipeBackFragment implements SearchView.OnQueryTextListener {
    // General variables
    private List<AppInfo> appList;
    private List<AppInfo> appSystemList;

    private AppAdapter appAdapter;
    private AppAdapter appSystemAdapter;

    // Configuration variables
    private View rootView;
    private Toolbar toolbar;
    private Activity activity;
    private Context context;
    private RecyclerView recyclerView;
    private MenuItem searchItem;
    private SearchView searchView;
    private static LinearLayout noResults;

    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressWheel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEdgeLevel(SwipeBackLayout.EdgeLevel.MED);
        this.activity = getActivity();
        this.context = getContext();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.activity_app_manage, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setInitialConfiguration();
        checkAndAddPermissions(activity);
        setAppDir();

        recyclerView = view.findViewById(R.id.appList);
        swipeRefresh = view.findViewById(R.id.pull_to_refresh);
        progressWheel = view.findViewById(R.id.progress);
        noResults = view.findViewById(R.id.noResults);

        swipeRefresh.setEnabled(false);

        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);

        progressWheel.setVisibility(View.VISIBLE);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        new getInstalledApps().execute();
    }

    private void setInitialConfiguration() {
        toolbar = rootView.findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.app_search);
        onCreateOptionsMenu(toolbar.getMenu());

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                return false;
            }
        });
    }

    class getInstalledApps extends AsyncTask<Void, String, Void> {
        private Integer totalApps;
        private Integer actualApps;

        getInstalledApps() {
            actualApps = 0;

            appList = new ArrayList<>();
            appSystemList = new ArrayList<>();
        }

        @Override
        protected Void doInBackground(Void... params) {
            final PackageManager packageManager = getContext().getPackageManager();
            List<PackageInfo> packages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA);
            totalApps = packages.size();
            // Get Sort Mode
            switch ("1") {
                default:
                    // Comparator by Name (default)
                    Collections.sort(packages, new Comparator<PackageInfo>() {
                        @Override
                        public int compare(PackageInfo p1, PackageInfo p2) {
                            return packageManager.getApplicationLabel(p1.applicationInfo).toString().toLowerCase().compareTo(packageManager.getApplicationLabel(p2.applicationInfo).toString().toLowerCase());
                        }
                    });
                    break;
                case "2":
                    // Comparator by Size
                    Collections.sort(packages, new Comparator<PackageInfo>() {
                        @Override
                        public int compare(PackageInfo p1, PackageInfo p2) {
                            Long size1 = new File(p1.applicationInfo.sourceDir).length();
                            Long size2 = new File(p2.applicationInfo.sourceDir).length();
                            return size2.compareTo(size1);
                        }
                    });
                    break;
                case "3":
                    // Comparator by Installation Date (default)
                    Collections.sort(packages, new Comparator<PackageInfo>() {
                        @Override
                        public int compare(PackageInfo p1, PackageInfo p2) {
                            return Long.toString(p2.firstInstallTime).compareTo(Long.toString(p1.firstInstallTime));
                        }
                    });
                    break;
                case "4":
                    // Comparator by Last Update
                    Collections.sort(packages, new Comparator<PackageInfo>() {
                        @Override
                        public int compare(PackageInfo p1, PackageInfo p2) {
                            return Long.toString(p2.lastUpdateTime).compareTo(Long.toString(p1.lastUpdateTime));
                        }
                    });
                    break;
            }

            // Installed & System Apps
            for (PackageInfo packageInfo : packages) {
                if (!(packageManager.getApplicationLabel(packageInfo.applicationInfo).equals("") || packageInfo.packageName.equals(""))) {

                    if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                        try {
                            // Non System Apps
                            AppInfo tempApp = new AppInfo(packageManager.getApplicationLabel(packageInfo.applicationInfo).toString(),
                                    packageInfo.packageName,
                                    packageInfo.versionName,
                                    packageInfo.applicationInfo.sourceDir,
                                    packageInfo.applicationInfo.dataDir,
                                    packageManager.getApplicationIcon(packageInfo.applicationInfo),
                                    false);
                            appList.add(tempApp);
                        } catch (OutOfMemoryError e) {
                            //TODO Workaround to avoid FC on some devices (OutOfMemoryError). Drawable should be cached before.
                            AppInfo tempApp = new AppInfo(packageManager.getApplicationLabel(packageInfo.applicationInfo).toString(),
                                    packageInfo.packageName,
                                    packageInfo.versionName,
                                    packageInfo.applicationInfo.sourceDir,
                                    packageInfo.applicationInfo.dataDir,
                                    getResources().getDrawable(R.mipmap.ic_launcher),
                                    false);
                            appList.add(tempApp);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            // System Apps
                            AppInfo tempApp = new AppInfo(packageManager.getApplicationLabel(packageInfo.applicationInfo).toString(), packageInfo.packageName, packageInfo.versionName, packageInfo.applicationInfo.sourceDir, packageInfo.applicationInfo.dataDir, packageManager.getApplicationIcon(packageInfo.applicationInfo), true);
                            appSystemList.add(tempApp);
                        } catch (OutOfMemoryError e) {
                            //TODO Workaround to avoid FC on some devices (OutOfMemoryError). Drawable should be cached before.
                            AppInfo tempApp = new AppInfo(packageManager.getApplicationLabel(packageInfo.applicationInfo).toString(), packageInfo.packageName, packageInfo.versionName, packageInfo.applicationInfo.sourceDir, packageInfo.applicationInfo.dataDir, getResources().getDrawable(R.mipmap.ic_launcher), false);
                            appSystemList.add(tempApp);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                actualApps++;
                publishProgress(Integer.toString((actualApps * 100) / totalApps));
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            super.onProgressUpdate(progress);
            //progressWheel.setProgress(Integer.parseInt(progress[0]));
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            appAdapter = new AppAdapter(appList, context);
            appSystemAdapter = new AppAdapter(appSystemList, context);

            recyclerView.setAdapter(appAdapter);
            swipeRefresh.setEnabled(true);
            progressWheel.setVisibility(View.GONE);
            searchItem.setVisible(true);

            setSwipeRefresh(swipeRefresh);
        }
    }

        private void setSwipeRefresh(final SwipeRefreshLayout swipeRefresh) {
            swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    appAdapter.clear();
                    appSystemAdapter.clear();
                    recyclerView.setAdapter(null);
                    new getInstalledApps().execute();

                    swipeRefresh.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            swipeRefresh.setRefreshing(false);
                        }
                    }, 2000);
                }
            });
        }

        private void checkAndAddPermissions(Activity activity) {
            UtilsApp.checkPermissions(activity);
        }

        private void setAppDir() {
            File appDir = UtilsApp.getAppFolder();
            if (!appDir.exists()) {
                appDir.mkdir();
            }
        }

        @Override
        public boolean onQueryTextChange(String search) {
            if (search.isEmpty()) {
                ((AppAdapter) recyclerView.getAdapter()).getFilter().filter("");
            } else {
                ((AppAdapter) recyclerView.getAdapter()).getFilter().filter(search.toLowerCase());
            }

            return false;
        }

        public static void setResultsMessage(Boolean result) {
            if (result) {
                noResults.setVisibility(View.VISIBLE);
            } else {
                noResults.setVisibility(View.GONE);
            }
        }


    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    public void onCreateOptionsMenu(Menu menu) {

        searchItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(this);

        SearchManager searchManager = (SearchManager) getContext().getSystemService(Context.SEARCH_SERVICE);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
    }
}
