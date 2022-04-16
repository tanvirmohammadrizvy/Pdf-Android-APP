package com.fulcrumy.pdfeditor.adapters;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.fulcrumy.pdfeditor.data.Constants;
import com.fulcrumy.pdfeditor.helper.DataUpdatedEvent;
import com.fulcrumy.pdfeditor.R;
import com.fulcrumy.pdfeditor.data.DbHelper;
import com.fulcrumy.pdfeditor.data.FileDiffCallback;
import com.fulcrumy.pdfeditor.fragments.BottomSheetDialogFragment;
import com.fulcrumy.pdfeditor.models.PdfModel;
import com.fulcrumy.pdfeditor.utils.Utils;
import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

public class RecentPdfsAdapter extends RecyclerView.Adapter<RecentPdfsAdapter.PdfViewHolder> {

    private final String TAG = RecentPdfsAdapter.class.getSimpleName();
    public boolean isGridViewEnabled;
    DbHelper dbHelper;
    private final OnHistoryPdfClickListener historyPdfClickListener;
    private final Context mContext;
    private List<PdfModel> pdfModelFiles;

    public RecentPdfsAdapter(List<PdfModel> list, Context context) {
        this.pdfModelFiles = list;
        this.mContext = context;
        this.dbHelper = DbHelper.getInstance(context);
        this.isGridViewEnabled = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Constants.GRID_VIEW_ENABLED, false);
        Context context2 = this.mContext;
        if (context2 instanceof OnHistoryPdfClickListener) {
            this.historyPdfClickListener = (OnHistoryPdfClickListener) context2;
            return;
        }
        throw new RuntimeException(this.mContext.toString() + " must implement OnHistoryPdfClickListener");
    }

    public PdfViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view;
        if (this.isGridViewEnabled) {
            view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item_pdf_grid, viewGroup, false);
        } else {
            view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item_pdf, viewGroup, false);
        }
        return new PdfViewHolder(view);
    }

    public void onBindViewHolder(PdfViewHolder pdfViewHolder, int i) {
        PdfModel pdfModel = this.pdfModelFiles.get(i);
        String absolutePath = pdfModel.getAbsolutePath();
        String name = pdfModel.getName();
        Long length = pdfModel.getLength();
        pdfViewHolder.pdfHeader.setText(name);
        pdfViewHolder.fileSize.setText(Formatter.formatShortFileSize(this.mContext, length.longValue()));
        pdfViewHolder.lastModified.setText(Utils.formatDateToHumanReadable(pdfModel.getLastModified()));
        if (pdfModel.isStarred()) {
            pdfViewHolder.toggleStar.setImageDrawable(ContextCompat.getDrawable(this.mContext, R.drawable.ic_bookmark2));
        }
        if (this.isGridViewEnabled) {
            Picasso.get().load(pdfModel.getThumbUri()).into((ImageView) pdfViewHolder.pdfThumbnail);
        }
        pdfViewHolder.toggleStar.setOnClickListener(new View.OnClickListener() {
            public final /* synthetic */ String f$1;
            public final /* synthetic */ PdfModel f$2;
            public final /* synthetic */ PdfViewHolder f$3;
            public final /* synthetic */ int f$4;

            {
                this.f$1 = absolutePath;
                this.f$2 = pdfModel;
                this.f$3 = pdfViewHolder;
                this.f$4 = i;
            }

            public final void onClick(View view) {
                RecentPdfsAdapter.this.lambda$onBindViewHolder$0$RecentPdfsAdapter(this.f$1, this.f$2, this.f$3, this.f$4, view);
            }
        });
        pdfViewHolder.pdfWrapper.setOnClickListener(new View.OnClickListener() {
            public final /* synthetic */ PdfViewHolder f$1;

            {
                this.f$1 = pdfViewHolder;
            }

            public final void onClick(View view) {
                RecentPdfsAdapter.this.lambda$onBindViewHolder$1$RecentPdfsAdapter(this.f$1, view);
            }
        });
        pdfViewHolder.menu.setOnClickListener(new View.OnClickListener() {
            public final /* synthetic */ PdfViewHolder f$1;

            {
                this.f$1 = pdfViewHolder;
            }

            public final void onClick(View view) {
                RecentPdfsAdapter.this.lambda$onBindViewHolder$2$RecentPdfsAdapter(this.f$1, view);
            }
        });
    }

    public /* synthetic */ void lambda$onBindViewHolder$0$RecentPdfsAdapter(String str, PdfModel pdfModel, PdfViewHolder pdfViewHolder, int i, View view) {
        if (this.dbHelper.isStared(str)) {
            this.dbHelper.removeStaredPDF(str);
            pdfModel.setStarred(false);
            pdfViewHolder.toggleStar.setImageDrawable(ContextCompat.getDrawable(this.mContext, R.drawable.ic_bookmark));
        } else {
            this.dbHelper.addStaredPDF(str);
            pdfModel.setStarred(true);
            pdfViewHolder.toggleStar.setImageDrawable(ContextCompat.getDrawable(this.mContext, R.drawable.ic_bookmark2));
        }
        notifyItemChanged(i);
        EventBus.getDefault().post(new DataUpdatedEvent.PDFStaredEvent("recent"));
    }

    public /* synthetic */ void lambda$onBindViewHolder$1$RecentPdfsAdapter(PdfViewHolder pdfViewHolder, View view) {
        int adapterPosition = pdfViewHolder.getAdapterPosition();
        historyPdfClicked(adapterPosition);
        String str = this.TAG;
        Log.d(str, "Pdf " + adapterPosition + " clicked");
    }

    public /* synthetic */ boolean lambda$onBindViewHolder$2$RecentPdfsAdapter(PdfViewHolder pdfViewHolder, View view) {
        showBottomSheet(pdfViewHolder.getAdapterPosition());
        return true;
    }

    public int getItemCount() {
        return this.pdfModelFiles.size();
    }

    public void filter(List<PdfModel> list) {
        this.pdfModelFiles = list;
        notifyDataSetChanged();
    }

    private void historyPdfClicked(int i) {
        OnHistoryPdfClickListener onHistoryPdfClickListener = this.historyPdfClickListener;
        if (onHistoryPdfClickListener != null && i >= 0) {
            onHistoryPdfClickListener.onHistoryPdfClicked(this.pdfModelFiles.get(i), i);
        }
    }

    public void updateData(List<PdfModel> list) {
        DiffUtil.calculateDiff(new FileDiffCallback(this.pdfModelFiles, list)).dispatchUpdatesTo((RecyclerView.Adapter) this);
        this.pdfModelFiles = list;
    }

    public void showBottomSheet(int i) {
        String absolutePath = this.pdfModelFiles.get(i).getAbsolutePath();
        Bundle bundle = new Bundle();
        bundle.putString("com.example.slimpdfapp.PDF_PATH", absolutePath);
        bundle.putBoolean(BottomSheetDialogFragment.FROM_RECENT, true);
        BottomSheetDialogFragment bottomSheetDialogFragment = new BottomSheetDialogFragment(new BottomSheetDialogFragment.onClickLisner() {
            @Override
            public void onClick() {

            }

            @Override
            public void onClickRename(String name, String f$3) {

            }
        });
        bottomSheetDialogFragment.setArguments(bundle);
        bottomSheetDialogFragment.show(((AppCompatActivity) this.mContext).getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
    }

    public interface OnHistoryPdfClickListener {
        void onHistoryPdfClicked(PdfModel pdfModel, int i);
    }

    public class PdfViewHolder extends RecyclerView.ViewHolder {
        public TextView fileSize;
        public TextView lastModified;
        public TextView pdfHeader;
        public AppCompatImageView pdfThumbnail;
        public RelativeLayout pdfWrapper;
        public ImageView menu;
        public AppCompatImageView toggleStar;

        private PdfViewHolder(View view) {
            super(view);
            if (RecentPdfsAdapter.this.isGridViewEnabled) {
                this.pdfThumbnail = (AppCompatImageView) view.findViewById(R.id.pdf_thumbnail);
            }
            this.pdfHeader = (TextView) view.findViewById(R.id.pdf_header);
            this.lastModified = (TextView) view.findViewById(R.id.pdf_last_modified);
            this.fileSize = (TextView) view.findViewById(R.id.pdf_file_size);
            this.toggleStar = (AppCompatImageView) view.findViewById(R.id.toggle_star);
            this.pdfWrapper = (RelativeLayout) view.findViewById(R.id.pdf_wrapper);
            this.menu = (ImageView) view.findViewById(R.id.menu);
        }
    }
}
