package com.gyx.recording;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {


	private ListView listview;
	private List<String> mList = new ArrayList<>();
	private MyAdapter myAdapter;
	private Context mContext;

	//1，new一个播放器，处于IDLE状态
	private MediaPlayer mediaPlayer = new MediaPlayer();
	private String path;
	private File file;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.activity_main);
		RecordButton mRecordButton = (RecordButton) findViewById(R.id.record_button);
		listview = (ListView) findViewById(R.id.listview);
		mContext = this;
		path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/录音文件夹";
		file = new File(path);
		if (!file.exists()) {
			//创建新的文件夹
			file.mkdir();
		}
		//遍历得到已有文件名
		setData(path, file);


		//文件夹路径
		final String path1 = file.getPath();

		//保存存储的文件夹路径
		mRecordButton.setSavePath(path1);

		mRecordButton.setOnFinishedRecordListener(new RecordButton.OnFinishedRecordListener() {
			@Override
			public void onFinishedRecord(String audioPath) {
				Log.e("RECORD!!!", "finished!!!!!!!!!! save to " + audioPath);
				mList.add(audioPath);

				/*if (myAdapter != null) {
					myAdapter.notifyDataSetChanged();
				} else {
					myAdapter = new MyAdapter();
					listview.setAdapter(myAdapter);
				}*/

				myAdapter.notifyDataSetChanged();

			}
		});


		listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				playMusic(mList.get(position));
			}
		});

	}

	/**
	 * 设置数据
	 * @param path
	 * @param file
	 */
	private void setData(String path, File file) {
		String[] list = file.list();
		for (String name : list) {
			mList.add(path + "/" + name);
			Log.e("已有录音",name);
		}
		if (myAdapter != null) {
			myAdapter.notifyDataSetChanged();
		} else {
			myAdapter = new MyAdapter();
			listview.setAdapter(myAdapter);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		myAdapter.notifyDataSetChanged();


	}

	/**
	 * 播放
	 * @param path
	 */
	private void playMusic(String path) {

		try {
			if (mediaPlayer.isPlaying()) {
				return;
			}

			//2，初始化状态，和new一样的效果
			mediaPlayer.reset();
			//3，设置播放的文件
			mediaPlayer.setDataSource(path);
			//4，同步的准备 运行在主线程中，
			mediaPlayer.prepare();
			//5，开始播放
			mediaPlayer.start();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}


	class MyAdapter extends BaseAdapter {
		@Override
		public int getCount() {
			return mList.size();
		}

		@Override
		public Object getItem(int position) {
			return mList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = View.inflate(mContext, R.layout.item_listview, null);
			TextView textView = (TextView) view.findViewById(R.id.time);
			//ImageView image = (ImageView) view.findViewById(R.id.image);
			//image.setBackgroundResource(R.drawable.abc);
			String str = mList.get(position);
			String substring = str.substring(20);
			textView.setText(substring);

			return view;
		}
	}

}
