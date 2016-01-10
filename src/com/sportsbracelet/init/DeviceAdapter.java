package com.sportsbracelet.init;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class DeviceAdapter extends BaseAdapter {
	private ArrayList<Device> devices;
	private Context mContext;
	private LayoutInflater inflater;

	public DeviceAdapter(Context context, ArrayList<Device> devices) {
		this.devices = devices;
		mContext = context;
		inflater = LayoutInflater.from(mContext);
	}

	public void setDevices(ArrayList<Device> devices) {
		this.devices = devices;
	}

	@Override
	public int getCount() {
		return devices.size();
	}

	@Override
	public Object getItem(int position) {
		return devices.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder = null;
		Device device = devices.get(position);
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.device_item, parent, false);
			holder = new ViewHolder();
			holder.tv_device_name = (TextView) convertView
					.findViewById(R.id.tv_device_name);
			holder.tv_device_address = (TextView) convertView
					.findViewById(R.id.tv_device_address);
			holder.tv_device_status = (TextView) convertView
					.findViewById(R.id.tv_device_status);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		holder.tv_device_name.setText(device.name);
		holder.tv_device_address.setText(device.address);
		if (device.isConnected) {
			holder.tv_device_status.setText("已连接");
		} else {
			holder.tv_device_status.setText("");
		}
		return convertView;
	}

	class ViewHolder {
		TextView tv_device_name;
		TextView tv_device_address;
		TextView tv_device_status;
	}
}
