package com.leonamin.humandetector;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.ViewHolder> {
    private Context context;
    private List<BluetoothDevice> itemList;
    private int selectedColor = Color.parseColor("#abcdef");
    private int selectedIndex;

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tv;

        public ViewHolder(View itemView) {
            super(itemView);
            tv = itemView.findViewById(R.id.item_devcie_name);
        }

        public TextView getTv() {
            return tv;
        }
    }

    public DeviceListAdapter(Context ctx, int resource, int textViewResourceId, List<BluetoothDevice> objects) {
        context = ctx;
        itemList = objects;
        selectedIndex = -1;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(DeviceListAdapter.ViewHolder holder, int position) {
        BluetoothDevice device = itemList.get(position);
        String deviceName = device.getName() + "\n " + device.getAddress();
        holder.tv.setText(context.getString(R.string.device_list_device_name, deviceName));

        if (selectedIndex != -1 && position == selectedIndex) {
            holder.tv.setBackgroundColor(selectedColor);
        } else {
            holder.tv.setBackgroundColor(Color.WHITE);
        }

        holder.itemView.setOnClickListener((View v) -> {
                setSelectedIndex(position);
                notifyDataSetChanged();
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

    public BluetoothDevice getSelectedItem() {
        return itemList.get(selectedIndex);
    }

    public BluetoothDevice getItem(int position) {
        return itemList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void replaceItems(List<BluetoothDevice> list) {
        itemList = list;
        notifyDataSetChanged();
    }

    public List<BluetoothDevice> getEntireList() {
        return itemList;
    }
}
