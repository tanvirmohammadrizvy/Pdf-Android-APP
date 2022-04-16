package com.fulcrumy.pdfeditor.fragments;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.fulcrumy.pdfeditor.MyApp;
import com.fulcrumy.pdfeditor.data.Constants;
import com.fulcrumy.pdfeditor.helper.DataUpdatedEvent;
import com.fulcrumy.pdfeditor.R;
import com.fulcrumy.pdfeditor.helper.SessionManager;
import com.fulcrumy.pdfeditor.adapters.DevicePdfsAdapter;
import com.fulcrumy.pdfeditor.data.DbHelper;
import com.fulcrumy.pdfeditor.models.PdfModel;
import com.fulcrumy.pdfeditor.helper.MaterialSearchView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

public class DevicePdfFragment extends Fragment implements MaterialSearchView.OnQueryTextListener {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    public static String MORE_OPTIONS_TIP = "prefs_more_options_tip";
    final String TAG = DevicePdfFragment.class.getSimpleName();
    public FragmentActivity activityCompat;
    public DbHelper dbHelper;
    public RecyclerView devicePdfRecyclerView;
    public LinearLayout emptyStateDevice;
    public boolean isGridViewEnabled;

    public LinearLayout loadingProgressBar;
    public DevicePdfsAdapter pdfsAdapter;
    public boolean showMoreOptionsTip;
    public SwipeRefreshLayout swipeRefresh;
    List<PdfModel> myPdfModels = new ArrayList();
    int numberOfColumns;
    SharedPreferences sharedPreferences;
    private boolean isFragmentVisibleToUser;
    private String mParam1;
    private String mParam2;
    private MaterialSearchView searchView;
    SessionManager sessionManager;
    private View view;

    public static DevicePdfFragment newInstance(String str, String str2) {
        DevicePdfFragment devicePdfFragment = new DevicePdfFragment();
        Bundle bundle = new Bundle();
        bundle.putString(ARG_PARAM1, str);
        bundle.putString(ARG_PARAM2, str2);
        devicePdfFragment.setArguments(bundle);
        return devicePdfFragment;
    }


    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (MyApp.getInstance().isNightModeEnabled()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        FragmentActivity activity = getActivity();
        sessionManager = new SessionManager(getContext());
        this.activityCompat = activity;
        this.dbHelper = DbHelper.getInstance(activity);
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        this.sharedPreferences = defaultSharedPreferences;
        this.isGridViewEnabled = defaultSharedPreferences.getBoolean(Constants.GRID_VIEW_ENABLED, false);
        this.numberOfColumns = this.sharedPreferences.getInt(Constants.GRID_VIEW_NUM_OF_COLUMNS, 2);
        this.showMoreOptionsTip = this.sharedPreferences.getBoolean(MORE_OPTIONS_TIP, true);
        if (getArguments() != null) {
            this.mParam1 = getArguments().getString(ARG_PARAM1);
            this.mParam2 = getArguments().getString(ARG_PARAM2);
        }


    }

    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        this.searchView = (MaterialSearchView) this.activityCompat.findViewById(R.id.search_view);
        this.emptyStateDevice = (LinearLayout) view.findViewById(R.id.empty_state_device);

        this.loadingProgressBar = (LinearLayout) view.findViewById(R.id.pd);
        AppCompatImageView appCompatImageView = (AppCompatImageView) view.findViewById(R.id.info_close);

        this.swipeRefresh = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh);

        if (ActivityCompat.checkSelfPermission(this.activityCompat, "android.permission.READ_EXTERNAL_STORAGE") != 0) {
            requestStoragePermission();
        } else {
            loadingProgressBar.setVisibility(View.VISIBLE);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    loadPdfFiles();
                }
            }, 100);
        }
        this.swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            public void onRefresh() {
                loadingProgressBar.setVisibility(View.VISIBLE);
                swipeRefresh.setVisibility(View.GONE);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        swipeRefresh.setVisibility(View.VISIBLE);
                        loadPdfFiles();
                    }
                }, 100);

            }
        });
    }

    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        if (i == 1 && iArr.length >= 1 && iArr[0] == 0) {
            Log.d(this.TAG, "Permission read External storage permission granted");
            loadPdfFiles();
            return;
        }
        Log.d(this.TAG, "Permission read External storage permission not granted");
        super.onRequestPermissionsResult(i, strArr, iArr);
        MaterialAlertDialogBuilder matD = new MaterialAlertDialogBuilder(this.activityCompat).setTitle((int) R.string.app_name).setMessage((int) R.string.exit_app_has_no_permission).setCancelable(false).setPositiveButton((int) R.string.ok, (DialogInterface.OnClickListener) new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                DevicePdfFragment.this.activityCompat.finish();
            }
        });
        Drawable aa = ContextCompat.getDrawable(getContext(), R.drawable.popup_bg);
        aa.setTint(ContextCompat.getColor(getContext(), R.color.background_color_day_night));
        matD.setBackground(aa);
        matD.show();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void loadPdfFiles() {

        myPdfModels = dbHelper.getAllPdfs();

        pdfsAdapter = new DevicePdfsAdapter(getActivity());

        pdfsAdapter.addData(myPdfModels);
        devicePdfRecyclerView.setAdapter(pdfsAdapter);
        swipeRefresh.setRefreshing(false);
        swipeRefresh.setVisibility(View.VISIBLE);
        loadingProgressBar.setVisibility(View.GONE);
        Log.d(TAG, "loadPdfFiles: " + myPdfModels.size());

    }

    @Subscribe
    public void onPermanetlyDeleteEvent(DataUpdatedEvent.PermanetlyDeleteEvent permanetlyDeleteEvent) {
        Log.d(this.TAG, "onPermanetlyDeleteEvent from device");


    }

    @Subscribe
    public void onPdfRenameEvent(DataUpdatedEvent.PdfRenameEvent pdfRenameEvent) {
        Log.d(this.TAG, "onPdfRenameEvent from recent");


    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Subscribe
    public void onPDFStaredEvent(DataUpdatedEvent.PDFStaredEvent pDFStaredEvent) {
        if (!TextUtils.equals(pDFStaredEvent.source, "device")) {
            Log.d(this.TAG, "onDevicePDFStaredEvent");
            loadPdfFiles();
        }
    }


    @Subscribe
    public void onSortListEvent(DataUpdatedEvent.SortListEvent sortListEvent) {
        loadingProgressBar.setVisibility(View.VISIBLE);
        swipeRefresh.setVisibility(View.GONE);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                loadPdfFiles();
                swipeRefresh.setVisibility(View.VISIBLE);
            }
        }, 100);
        Log.d(TAG, "onSortListEvent: ss");
    }

    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        view = layoutInflater.inflate(R.layout.fragment_device_pdf, viewGroup, false);
        this.devicePdfRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view_device_pdf);
        return view;
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    public void requestStoragePermission() {
        String[] strArr = {"android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.CAMERA"};
        if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), "android.permission.READ_EXTERNAL_STORAGE")) {
            Toast.makeText(this.activityCompat, "Read storage permission is required to list files", 0).show();
        }
        requestPermissions(strArr, 1);


        ActivityCompat.requestPermissions(
                getActivity(),
                new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.MANAGE_EXTERNAL_STORAGE
                },
                2
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Environment.isExternalStorageManager()) {

// If you don't have access, launch a new activity to show the user the system's dialog
// to allow access to the external storage
            } else {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        }

    }

    public void setUserVisibleHint(boolean z) {
        super.setUserVisibleHint(z);
        if (z) {
            this.isFragmentVisibleToUser = true;
            MaterialSearchView materialSearchView = this.searchView;
            if (materialSearchView != null) {
                materialSearchView.setOnQueryTextListener(this);
                return;
            }
            return;
        }
        this.isFragmentVisibleToUser = false;
        MaterialSearchView materialSearchView2 = this.searchView;
        if (materialSearchView2 != null) {
            materialSearchView2.setOnQueryTextListener((MaterialSearchView.OnQueryTextListener) null);
        }
    }

    public boolean onQueryTextSubmit(String str) {
        if (!this.isFragmentVisibleToUser) {
            return true;
        }
        searchPDFFiles(str);
        return true;
    }

    public boolean onQueryTextChange(String str) {
        if (!this.isFragmentVisibleToUser) {
            return true;
        }
        searchPDFFiles(str);
        return true;
    }

    public void setupForGridView(Context context, RecyclerView recyclerView, int i) {
        Float valueOf = Float.valueOf(getResources().getDisplayMetrics().density);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(context, i, 1, false);
        recyclerView.setBackgroundColor(getResources().getColor(R.color.background_color_day_night));
        recyclerView.setPadding((int) (valueOf.floatValue() * 4.0f), (int) (valueOf.floatValue() * 4.0f), (int) (valueOf.floatValue() * 4.0f), (int) (valueOf.floatValue() * 80.0f));
        recyclerView.setLayoutManager(gridLayoutManager);
    }

    public void setupForListView(Context context, RecyclerView recyclerView) {
        Float valueOf = Float.valueOf(getResources().getDisplayMetrics().density);
        recyclerView.setBackgroundColor(getResources().getColor(R.color.background_color_day_night));
        recyclerView.setPadding(0, 0, (int) (valueOf.floatValue() * 4.0f), (int) (valueOf.floatValue() * 80.0f));
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
    }

    public void searchPDFFiles(String str) {
        ArrayList arrayList = new ArrayList();
        for (PdfModel next : this.myPdfModels) {
            if (next.getName().toLowerCase().contains(str.toLowerCase())) {
                arrayList.add(next);
            }
            this.pdfsAdapter.filter(arrayList);
        }
    }


}
