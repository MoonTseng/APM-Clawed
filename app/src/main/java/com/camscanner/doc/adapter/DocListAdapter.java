package com.camscanner.doc.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 文档列表适配器，展示用户的扫描文档。
 */
public class DocListAdapter extends RecyclerView.Adapter<DocListAdapter.DocViewHolder> {

    private List<DocItem> mItems = new ArrayList<>();
    private OnDocClickListener mListener;
    private SimpleDateFormat mDateFormat;
    private boolean mIsSelectionMode = false;
    private List<Integer> mSelectedPositions = new ArrayList<>();

    public interface OnDocClickListener {
        void onDocClick(DocItem item, int position);
        void onDocLongClick(DocItem item, int position);
    }

    public static class DocItem {
        public String id;
        public String title;
        public String thumbnailPath;
        public long createTime;
        public long updateTime;
        public int pageCount;
        public boolean isSynced;
        public String tags;
    }

    public DocListAdapter(OnDocClickListener listener) {
        mListener = listener;
        mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    }

    public void setItems(List<DocItem> items) {
        mItems = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void addItems(List<DocItem> items) {
        if (items == null || items.isEmpty()) return;
        int start = mItems.size();
        mItems.addAll(items);
        notifyItemRangeInserted(start, items.size());
    }

    public void removeItem(int position) {
        if (position >= 0 && position < mItems.size()) {
            mItems.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void toggleSelectionMode() {
        mIsSelectionMode = !mIsSelectionMode;
        if (!mIsSelectionMode) {
            mSelectedPositions.clear();
        }
        notifyDataSetChanged();
    }

    public List<DocItem> getSelectedItems() {
        List<DocItem> selected = new ArrayList<>();
        for (int pos : mSelectedPositions) {
            if (pos < mItems.size()) {
                selected.add(mItems.get(pos));
            }
        }
        return selected;
    }

    @NonNull
    @Override
    public DocViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);
        return new DocViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DocViewHolder holder, int position) {
        // BUG: mItems 可能在异步更新后大小改变，
        // 而 RecyclerView 的 position 基于旧数据，导致越界
        // 典型场景：后台线程删除数据 + 前台 notifyDataSetChanged 延迟
        DocItem item = mItems.get(position);    // line 142: IndexOutOfBoundsException

        holder.titleView.setText(item.title);
        String dateStr = mDateFormat.format(new Date(item.updateTime));
        String subtitle = item.pageCount + " pages | " + dateStr;
        if (item.isSynced) {
            subtitle += " | Synced";
        }
        holder.subtitleView.setText(subtitle);

        // 选中状态
        if (mIsSelectionMode) {
            boolean isSelected = mSelectedPositions.contains(position);
            holder.itemView.setAlpha(isSelected ? 1.0f : 0.6f);
        } else {
            holder.itemView.setAlpha(1.0f);
        }

        // 点击事件
        holder.itemView.setOnClickListener(v -> {
            if (mIsSelectionMode) {
                toggleSelection(position);
            } else if (mListener != null) {
                mListener.onDocClick(item, position);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (mListener != null) {
                mListener.onDocLongClick(item, position);
            }
            return true;
        });
    }

    private void toggleSelection(int position) {
        if (mSelectedPositions.contains(position)) {
            mSelectedPositions.remove(Integer.valueOf(position));
        } else {
            mSelectedPositions.add(position);
        }
        notifyItemChanged(position);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    static class DocViewHolder extends RecyclerView.ViewHolder {
        TextView titleView;
        TextView subtitleView;

        DocViewHolder(View itemView) {
            super(itemView);
            titleView = itemView.findViewById(android.R.id.text1);
            subtitleView = itemView.findViewById(android.R.id.text2);
        }
    }
}
