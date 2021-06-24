package com.leonamin.humandetector;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ceylonlabs.imageviewpopup.ImagePopup;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class EventListAdapter extends RecyclerView.Adapter<EventListAdapter.ViewHolder> {
    private Context context;
    private List<EventData> itemList;
    private int selectedIndex;

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNumber;
        TextView tvTime;
        ImageView ivThumbnail;

        public ViewHolder(View itemView) {
            super(itemView);
            tvNumber = itemView.findViewById(R.id.item_tv_number);
            tvTime = itemView.findViewById(R.id.item_tv_event_time);
            ivThumbnail = itemView.findViewById(R.id.item_iv_event_thumbnail);
        }

        public TextView getTvNumber() {
            return tvNumber;
        }

        public TextView getTvTime() {
            return tvTime;
        }

        public ImageView getIvThumbnail() {
            return ivThumbnail;
        }
    }

    public EventListAdapter(Context ctx, List<EventData> objects) {
        context = ctx;
        itemList = objects;
        selectedIndex = -1;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(EventListAdapter.ViewHolder holder, int position) {
        EventData data = itemList.get(position);

        holder.tvNumber.setText(context.getString(R.string.event_list_item_number, Integer.toString(position)));
        holder.tvTime.setText(context.getString(R.string.event_list_item_time, getTimestampToDate(data.getTimeStamp())));

        File image = new File(data.getThumbnailPath());

        if (image.exists()) {
//            Bitmap myBitmap = BitmapFactory.decodeFile(image.getAbsolutePath());
            Bitmap myBitmap = getResizedBitmap(BitmapFactory.decodeFile(image.getAbsolutePath()), 200);
            holder.ivThumbnail.setImageBitmap(myBitmap);
        }

        final ImagePopup imagePopup = new ImagePopup(context);
        imagePopup.setWindowHeight(800); // Optional
        imagePopup.setWindowWidth(800); // Optional
        imagePopup.setBackgroundColor(Color.BLACK);  // Optional
        imagePopup.setFullScreen(true); // Optional
        imagePopup.setHideCloseIcon(true);  // Optional
        imagePopup.setImageOnClickClose(true);  // Optional

        imagePopup.initiatePopup(holder.ivThumbnail.getDrawable());

        holder.itemView.setOnClickListener((View v) -> {
            setSelectedIndex(position);
            notifyDataSetChanged();
        });

        holder.ivThumbnail.setOnClickListener((View v) -> {
            // FIXME Sometimes crashes occurred
            // java.lang.IllegalStateException: The specified child already has a parent. You must call removeView() on the child's parent first.
            imagePopup.viewPopup();
        });
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public void setSelectedIndex(int position) {
        selectedIndex = position;
        notifyDataSetChanged();
    }

    public EventData getSelectedItem() {
        return itemList.get(selectedIndex);
    }

    public EventData getItem(int position) {
        return itemList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void replaceItems(List<EventData> list) {
        itemList = list;
        notifyDataSetChanged();
    }

    public List<EventData> getEntireList() {
        return itemList;
    }

    public void addItem(EventData eventData) {
        if (itemList.size() >= 20) {
            itemList.remove(0);
        }
        itemList.add(eventData);
        notifyDataSetChanged();
    }

    public void clearItems() {
        itemList = new ArrayList<>();
        notifyDataSetChanged();
    }

    private static String getTimestampToDate(long timestamp){
        Date date = new java.util.Date(timestamp*1000L);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("GMT+9"));
        return sdf.format(date);
    }

    public Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float)width / (float) height;
        if (bitmapRatio > 0) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }
}
