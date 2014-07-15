package com.mzh;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.example.sdcardlist.R;
import com.xunlei.cloud.extstorage.FileSearchManager;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 获取图片和视频的缩略图 这两个方法必须在2.2及以上版本使用，因为其中使用了ThumbnailUtils这个类
 */
public class SDCardList extends Activity {
//	private static final String bookDirectory = Environment
//			.getExternalStorageDirectory().getPath() + File.separator;
//	 + /* "Tencent" */"mzh" + File.separator;
	private static final String bookDirectory = "/mnt/usb/sda4";
	
	private final int SEARCH_FINISHED = 10000;
	private final int BACK_TIME_OUT = 1 + SEARCH_FINISHED;
	
	private int depth = 0;
	ArrayList<String> test_list;
	
	String currentPath;
	ArrayList<File> curShowingList;
	private ListView i;
	private LayoutInflater inflater;
	private Button btn;
	MyListViewAdapter ladapter;

	private Boolean isGotResult = false;
	private int backtimes=0;
	
	@SuppressLint("HandlerLeak")
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case SEARCH_FINISHED: {

				Bundle b = msg.getData();
				long t = b.getLong("time");
				String l = b.getString("paths");
				String str = "time:" + t + "\n" + l;
				((TextView) findViewById(R.id.tip)).setText(str);
				
				currentPath = bookDirectory + File.separator;
				depth = 1;
				genDirectory();
				break;
			}
			
			case BACK_TIME_OUT: {
				backtimes = 0;
				break;
			}
			default:
				break;
			}
		}
	};

	private void genDirectory() {
		File files[] = new File(currentPath).listFiles();
		curShowingList = new ArrayList<File>();
		for (File f : files) {
			if (f.isDirectory()) {
				int st = bookDirectory.length();
				String path = f.getAbsolutePath().substring(st);
				// Log.d("mzh", f.getName()+ "    "+path);
				if (FileSearchManager.instance().checkPath(path)) {
					curShowingList.add(f);
				}
			} else {
				if (FileSearchManager.instance().isVideo(f.getName())) {
					curShowingList.add(f);
				}
			}
		}

		if (ladapter == null || i.getAdapter() != ladapter) {
			ladapter = new MyListViewAdapter();
			i.setAdapter(ladapter);
		} else {
			ladapter.notifyDataSetChanged();
		}

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.thubnail_test_actitivy);
		btn = (Button) findViewById(R.id.btn);
		i = (ListView) findViewById(R.id.list);
		inflater = LayoutInflater.from(getApplicationContext());
		FileSearchManager.instance().setRootDir(bookDirectory);
		
		btn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View paramView) {
				if (isGotResult) {
					return;
				}
				
				new Thread(new Runnable() {
					public void run() {
						long total_time = 0;
						long start = System.currentTimeMillis();
						
						FileSearchManager.instance().checkFilesFrom(bookDirectory, FileSearchManager.Type.USEJAVA);
						
						long time = System.currentTimeMillis() - start;
						Log.d("mzh", "  passed time:" + time
								+ " path:" + bookDirectory);
						
						String paths = "searching totalNum" + FileSearchManager.instance().getFileNum() + " path: "
								+ bookDirectory + "\npaths:\n";
						paths += FileSearchManager.instance().traceFoundPaths();
						isGotResult = false;
						Bundle b = new Bundle();
						b.putLong("time", total_time);
						b.putString("paths", paths);
						Message msg = handler.obtainMessage();
						msg.what = SEARCH_FINISHED;
						msg.setData(b);
						msg.sendToTarget();
					}
				}).start();
				Log.d("mzh", "clicked show");
				isGotResult = true;
			}
		});

		findViewById(R.id.btn2).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				if (isGotResult) {
					return;
				}
				new Thread(new Runnable() {
					public void run() {
						long total_time = 0;
						long start = System.currentTimeMillis();
						Log.d("mzh", "get my sdcard files time:"
								+ FileSearchManager.instance().checkFilesFrom(bookDirectory) + " path:"
								+ bookDirectory);
						
						long time = System.currentTimeMillis() - start;
						Log.d("mzh", "  passed time:" + time + " path:"
								+ bookDirectory);
						total_time += time;
						Log.d("mzh", "used time:" + time);
						
						total_time += time;
						String paths = "searching path: " + bookDirectory
								+ "\npaths:\n";
						paths += FileSearchManager.instance().traceFoundPaths();
						
						isGotResult = false;
						Bundle b = new Bundle();
						b.putLong("time", total_time);
						b.putString("paths", paths);
						Message msg = handler.obtainMessage();
						msg.what = SEARCH_FINISHED;
						msg.setData(b);
						msg.sendToTarget();
					}
				}).start();
				Log.d("mzh", "clicked c_show");
				isGotResult = true;
			}
		});

		i.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos,
					long id) {
				File f = ((MyListViewAdapter) i.getAdapter()).getItem(pos);
				if (f.isDirectory()) {
					currentPath = f.getAbsolutePath();
					depth ++;
					genDirectory();
				}
			}

		});
		
		// 获取cid
		getCid();
		
		// 检测 u盘插拔状态；
		new UsbStateReceiver(this).registerReceiver();
		
		// 获取机器ip
		// getIp();
	}

	private void getCid() {
		String path = Environment.getExternalStorageDirectory().getPath()
				+ File.separator + "mzh" + File.separator + "oceans.mp4";// "test.txt";//
		String cid = FileCidUtility.instance.get_file_cid(path);
		Log.d("mzh", "got file cid:" + cid);
		((TextView) findViewById(R.id.iptext)).setText(cid);
		/*
		 * byte[] buffer = "hello".getBytes(); for(int i=0;i<buffer.length;i++){
		 * Log.d("mzh", i+":"+buffer[i]); } String ret = (new
		 * FileCidUtility.SHA1()).getDigestOfString(buffer); Log.d("mzh",
		 * "the hello cid:"+ret);
		 */
	}

	private void getIp() {
		// 获取wifi服务
		WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		// 判断wifi是否开启
		if (!wifiManager.isWifiEnabled()) {
			wifiManager.setWifiEnabled(true);
		}
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		int ipAddress = wifiInfo.getIpAddress();
		String ip = intToIp(ipAddress);
		TextView et = (TextView) findViewById(R.id.iptext);
		et.setText(ip);
	}

	private String intToIp(int i) {

		return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF)
				+ "." + (i >> 24 & 0xFF);
	}

	public class MyListViewAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return curShowingList.size();
		}

		@Override
		public File getItem(int paramInt) {

			return curShowingList.get(paramInt);
		}

		@Override
		public long getItemId(int paramInt) {
			return paramInt;
		}

		@Override
		public View getView(int position, View convertView,
				ViewGroup paramViewGroup) {
			ItemHolder itemHolder = null;
			
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.relative, null);
				itemHolder = new ItemHolder();
				itemHolder.iv_icon = (ImageView) convertView
						.findViewById(R.id.image_thumbnail);
				itemHolder.ItemTitle = (TextView) convertView
						.findViewById(R.id.ItemTitle);
				itemHolder.ItemText = (TextView) convertView
						.findViewById(R.id.ItemText);
				itemHolder.time = (TextView) convertView
						.findViewById(R.id.time);
				convertView.setTag(itemHolder);
			} else {
				itemHolder = (ItemHolder) convertView.getTag();
			}
			boolean isDir = curShowingList.get(position).isDirectory();
			String path = curShowingList.get(position).toString();

			itemHolder.ItemText.setText(path);
			itemHolder.time.setText(curShowingList.get(position).lastModified()
					+ "");

			if (isDir) {
				itemHolder.ItemTitle.setText("");
				itemHolder.iv_icon.setImageDrawable(getResources().getDrawable(
						R.drawable.folder));
			} else {
				String name = path.substring(path.lastIndexOf("/") + 1,
						path.length());
//				String ext = name.substring(name.lastIndexOf(".") + 1, name.length());
				// 保存每一格list单元格的数据 ，
				itemHolder.ItemTitle.setText(name);
				// itemHolder.ItemText.setText(path);
				// itemHolder.time.setText("time");
				// itemHolder.time.setText(fileList.get(position).lastModified()
				// + "");
				
				if (FileSearchManager.instance().isVideo(name)) {
					itemHolder.iv_icon.setImageBitmap(FileSearchManager.instance().getVideoThumbFrom(path));
				/*} else if (pic_formats.contains(ext)) {
					itemHolder.iv_icon.setImageBitmap(getImageThumbnail(path,
							150, 150));*/
				} else {
					itemHolder.iv_icon.setImageDrawable(getResources()
							.getDrawable(R.drawable.icon));
				}
			}

			return convertView;
		}

		class ItemHolder {
			public ImageView iv_icon;
			public TextView ItemTitle;
			public TextView ItemText;
			public TextView time;
		}

	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(event.getAction() == KeyEvent.ACTION_DOWN  
	            && event.getKeyCode() == KeyEvent.KEYCODE_BACK){
			if(depth > 1){
				depth --;
				String tmp = currentPath.substring(0,currentPath.lastIndexOf("/"));
				currentPath = tmp;
				genDirectory();
				return true;
			}
			else {
				if(backtimes == 1){
					finish();
					return super.onKeyDown(keyCode, event);
				}
				Toast.makeText(getApplicationContext(), "再按一次退出",
					     Toast.LENGTH_SHORT).show();
				backtimes ++;
				handler.sendEmptyMessageDelayed(BACK_TIME_OUT, 2000);
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}
}