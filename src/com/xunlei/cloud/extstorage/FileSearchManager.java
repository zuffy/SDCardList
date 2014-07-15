package com.xunlei.cloud.extstorage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import com.mzh.FileCidUtility;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

public class FileSearchManager {
	private static FileSearchManager _instance;

	public static enum Type {
		USEJNI, USEJAVA
	}

	public static FileSearchManager instance() {
		if (_instance == null) {
			_instance = new FileSearchManager();
		}
		return _instance;
	}

	private ArrayList<String> formats = new ArrayList<String>();
	private ArrayList<String> video_formats = new ArrayList<String>();
	private ArrayList<String> pic_formats = new ArrayList<String>();
	private Map<String, SoftReference<Bitmap>> bitmapCaches;

	public FileSearchManager() {
		video_formats.add("avi");
		video_formats.add("rmvb");
		video_formats.add("mp4");
		video_formats.add("wmv");

		// pic_formats.add("jpg");

		formats.addAll(video_formats);
		formats.addAll(pic_formats);

		bitmapCaches = new HashMap<String, SoftReference<Bitmap>>();
	}

	// java 搜索时搜出队文件数目;
	private int _fileNum = 0;

	// 视频列表
	ArrayList<String> video_list;
	HashMap<String, Integer> _video_table;

	public String getFixedPath(String path){
		int st = root_dir.length();
		int ed = path.lastIndexOf("/");
		return  path.substring(st, ed);
	}
	// 生成path：value表
	public void genPathTable() {
		final int MAX_LOOP = 1024;
		_video_table = new HashMap<String, Integer>();
		for (String path : video_list) {
			String tmp = getFixedPath(path);
			
			int loop = 0, st = 0, ed = 0;
			
			while (!tmp.equals("") && loop++ < MAX_LOOP) {
				// Log.d("mzh", "wihle:"+tmp);
				path = tmp;
				if (_video_table.containsKey(tmp)) {
					int v = _video_table.get(tmp) + 1;
					_video_table.put(tmp, v);
				} else {
					_video_table.put(tmp, 1);
				}
				st = 0;
				ed = path.lastIndexOf("/");
				tmp = path.substring(st, ed);
				// Log.d("mzh", "get table:" + tmp + " s:" + st + " e:" + ed);
			}
		}

		ArrayList<Map.Entry<String, Integer>> entryList = new ArrayList<Map.Entry<String, Integer>>(
				_video_table.entrySet());
		Collections.sort(entryList,
				new Comparator<Map.Entry<String, Integer>>() {
					@Override
					public int compare(final Map.Entry<String, Integer> arg0,
							final Map.Entry<String, Integer> arg1) {
						Map.Entry<String, Integer> entry1 = arg0;
						Map.Entry<String, Integer> entry2 = arg1;
						/*
						 * String str1 = entry1.getKey().replaceAll("[^/]", "");
						 * String str2 = entry2.getKey().replaceAll("[^/]", "");
						 * if(str1.length() > str2.length()){ return 1; } else
						 * if(str1.length() < str2.length()){ return -1; }
						 */
						return (entry1.getKey().compareToIgnoreCase(entry2
								.getKey()));
						// return 0;
					}

				});
		// print start.....
		/*
		 * Iterator<Map.Entry<String, Integer>> iterator = entryList.iterator();
		 * String str; while (iterator.hasNext()) { Map.Entry<String, Integer>
		 * entry = (Map.Entry<String, Integer>) iterator .next(); str =
		 * entry.getKey(); int v = entry.getValue(); Log.d("mzh", "maps:" + str
		 * + "  value:" + v); }
		 */
		// print end......
	}

	public boolean isVideo(String name) {
		String ext = name.substring(name.lastIndexOf(".") + 1, name.length())
				.toLowerCase(Locale.getDefault());

		if (formats.contains(ext)) {
			return true;
		}
		return false;
	}

	private void pickOutVideoPath() {
		video_list = new ArrayList<String>();
		for (String str : _arrayList) {
			if (isVideo(str)) {
				video_list.add(str.replace("//", "/"));
			}
		}

		Collections.sort(video_list, new Comparator<String>() {
			@Override
			public int compare(final String arg0, String arg1) {
				String str1 = arg0.replaceAll("[^/]", "");
				String str2 = arg1.replaceAll("[^/]", "");
				if (str1.length() > str2.length()) {
					return 1;
				} else if (str1.length() < str2.length()) {
					return -1;
				}
				return 0;
			}

		});

		/*
		 * for (String str : video_list) { Log.d("mzh", "sort:"+str); }
		 */

		genPathTable();
		genCidMap();
		genThumbnails();
	}

	private String save_dir = ".xunleitv";
	private String root_dir = Environment.getExternalStorageDirectory()
			.getPath();
	private String save_path = root_dir + File.separator + save_dir;

	public void setRootDir(String dir) {
		root_dir = dir;
	}

	private void genThumbnails() {
		new Thread(new Runnable() {
			@Override
			public void run() {

				File f = new File(save_path);
				if (!(f.exists() && f.isDirectory())) {
					f.mkdir();
				}
				for (String str : video_list) {
					createThumLocalCache(str);
				}
				// print msg
				/*
				 * for(File fe : f.listFiles()){ Log.i("mzh",
				 * "file:"+fe.getAbsolutePath()); }
				 */
			}

		}).start();
	}

	/*
	 * 使用文件 cid, 大小, px格式缓存缩略图到本地
	 */
	public static final int THUMB_PX_150 = 150;
	private int thumb_size = THUMB_PX_150;
	private int quality = 100;

	private boolean createThumLocalCache(String video_path) {
		File f;
		FileOutputStream out;
		String cid = getFilePathCid(video_path);
		String thumb_path;
		if (!cid.equals("")) {
			f = new File(video_path);
			thumb_path = String.format("%s/%s_%d_%d", save_path, cid,
					f.length(), thumb_size);
			f = new File(thumb_path);
			if (!f.exists()) {
				Bitmap bitmap = getVideoThumbnail(video_path, thumb_size,
						thumb_size, MediaStore.Images.Thumbnails.MICRO_KIND);
				try {
					f.createNewFile();
					out = new FileOutputStream(f);
					if (bitmap
							.compress(Bitmap.CompressFormat.PNG, quality, out)) {
						out.flush();
						out.close();
					}
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

				bitmapCaches.put(video_path, new SoftReference<Bitmap>(bitmap));

				Log.i("mzh", "th:" + thumb_path);
			}
		}
		return false;
	}

	/*
	 * 用文件路径取出缩略图
	 */
	public Bitmap getVideoThumbFrom(String path) {
		Bitmap btm = null;
		// 读内存缓存
		if (bitmapCaches.containsKey(path)) {
			SoftReference<Bitmap> softBitmap = bitmapCaches.get(path);
			Bitmap bmp = softBitmap.get();
			if (bmp != null) {
				return bmp;
			}
		}
		// 读文件缓存
		File f = new File(path); // 视频文件
		FileInputStream in; // 输入流
		String cid = getFilePathCid(path); // 获取文件cid
		String thumb_path; // 缩略图位置
		if (!cid.equals("") && f.exists()) { // 如果算出文件cid
			thumb_path = String.format("%s/%s_%d_%d", save_path, cid, f.length(), thumb_size);
			try {
				f = new File(thumb_path);
				in = new FileInputStream(f);
				btm = BitmapFactory.decodeStream(in);
				bitmapCaches.put(path, new SoftReference<Bitmap>(btm));
				return btm;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		// 临时生成
		btm = getVideoThumbnail(path, thumb_size,	thumb_size, MediaStore.Images.Thumbnails.MICRO_KIND);
		bitmapCaches.put(path, new SoftReference<Bitmap>(btm));
		return btm;
	}

	/*
	 * 生成 map 文件路径cid 对应 文件cid
	 */
	private HashMap<String, String> cid_map;

	private void genCidMap() {
		String cid, key;
		cid_map = new HashMap<String, String>();
		for (String str : video_list) {
			key = FileCidUtility.instance.get_data_block_cid(str.getBytes());
			cid = FileCidUtility.instance.get_file_cid(str);
			cid_map.put(key, cid);
		}
	}

	/*
	 * 用文件路径 取出 文件cid
	 */
	public String getFilePathCid(String path) {
		String ret = "", key = FileCidUtility.instance.get_data_block_cid(path
				.getBytes());
		if (cid_map.containsKey(key))
			ret = cid_map.get(key);
		return ret;
	}

	/**
	 * 获取视频的缩略图 先通过ThumbnailUtils来创建一个视频的缩略图，然后再利用ThumbnailUtils来生成指定大小的缩略图。
	 * 如果想要的缩略图的宽和高都小于MICRO_KIND，则类型要使用MICRO_KIND作为kind的值，这样会节省内存。
	 * 
	 * @param videoPath
	 *            视频的路径
	 * @param width
	 *            指定输出视频缩略图的宽度
	 * @param height
	 *            指定输出视频缩略图的高度度
	 * @param kind
	 *            参照MediaStore.Images.Thumbnails类中的常量MINI_KIND和MICRO_KIND。
	 *            其中，MINI_KIND: 512 x 384，MICRO_KIND: 96 x 96
	 * @return 指定大小的视频缩略图
	 */
	private Bitmap getVideoThumbnail(String videoPath, int width, int height,
			int kind) {
		Bitmap bitmap = null;
		// 获取视频的缩略图
		bitmap = ThumbnailUtils.createVideoThumbnail(videoPath, kind);
		System.out.println("w" + bitmap.getWidth());
		System.out.println("h" + bitmap.getHeight());
		bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height,
				ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
		return bitmap;
	}

	/**
	 * 根据指定的图像路径和大小来获取缩略图 此方法有两点好处： 1.
	 * 使用较小的内存空间，第一次获取的bitmap实际上为null，只是为了读取宽度和高度，
	 * 第二次读取的bitmap是根据比例压缩过的图像，第三次读取的bitmap是所要的缩略图。 2.
	 * 缩略图对于原图像来讲没有拉伸，这里使用了2.2版本的新工具ThumbnailUtils，使 用这个工具生成的图像不会被拉伸。
	 * 
	 * @param imagePath
	 *            图像的路径
	 * @param width
	 *            指定输出图像的宽度
	 * @param height
	 *            指定输出图像的高度
	 * @return 生成的缩略图
	 */
	private Bitmap getImageThumbnail(String imagePath, int width, int height) {
		Bitmap bitmap = null;
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		// 获取这个图片的宽和高，注意此处的bitmap为null
		bitmap = BitmapFactory.decodeFile(imagePath, options);
		options.inJustDecodeBounds = false; // 设为 false
		// 计算缩放比
		int h = options.outHeight;
		int w = options.outWidth;
		int beWidth = w / width;
		int beHeight = h / height;
		int be = 1;
		if (beWidth < beHeight) {
			be = beWidth;
		} else {
			be = beHeight;
		}
		if (be <= 0) {
			be = 1;
		}
		options.inSampleSize = be;
		// 重新读入图片，读取缩放后的bitmap，注意这次要把options.inJustDecodeBounds 设为 false
		bitmap = BitmapFactory.decodeFile(imagePath, options);
		// 利用ThumbnailUtils来创建缩略图，这里要指定要缩放哪个Bitmap对象
		bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height,
				ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
		return bitmap;
	}

	public String traceFoundPaths() {
		String paths = "";
		for (String str : video_list) {
			paths += str + "\n";
			// Log.d("mzh", str);
		}
		return paths;
	}

	public boolean checkPath(String path) {
		return _video_table.containsKey(path);
	}

	private static ArrayList<String> _arrayList;

	public float checkFilesFrom(String path) {
		return checkFilesFrom(path, Type.USEJNI);
	}

	public float checkFilesFrom(String path, Type t) {
		_arrayList = new ArrayList<String>();
		float ret = System.currentTimeMillis();
		if (null == t) {
			t = Type.USEJNI;
		}
		switch (t) {
		case USEJNI: {
			ret = searchFiles(path);
			break;
		}
		case USEJAVA: {
			_fileNum = 0;
			getAllFiles(new File(path));
			ret = System.currentTimeMillis() - ret;
			break;
		}
		default:
			break;
		}
		pickOutVideoPath();
		return ret;
	}

	private void getAllFiles(File directory) {
		File files[] = directory.listFiles();

		if (files != null) {
			for (File f : files) {
				_fileNum++;
				addToList(f.getAbsolutePath());
				if (f.isDirectory()) {
					getAllFiles(f);
				} /*
				 * else { // Log.d("---", f.getAbsolutePath()); }
				 */
			}
		}
	}
	
	public int getVideosNum(String path){
		int ret = 0;
		String key = getFixedPath(path);
		if(_video_table.containsKey(key))
			ret = _video_table.get(key);
		return ret;
	}

	public int getFileNum() {
		return _fileNum;
	}

	// c call
	private void addToList(String p) {
		_arrayList.add(p);
	}

	// c
	private native float searchFiles(String path);

	static {
		System.loadLibrary("filesearch");
	}
}
