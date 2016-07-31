package com.gyx.recording;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;


import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by GYX-S on 2016/7/29.
 */
public class RecordButton extends Button {
	//最终文件路径
	private String fileName;

	public RecordButton(Context context) {
		super(context);
		init();
	}

	public RecordButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public RecordButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	/**
	 * 设置保存的文件夹
	 * @param path
	 */
	public void setSavePath(String path) {
		mFileName = path;
	}

	public void setOnFinishedRecordListener(OnFinishedRecordListener listener) {
		finishedListener = listener;
	}

	private String mFileName = null;

	private OnFinishedRecordListener finishedListener;

	private static final int MIN_INTERVAL_TIME = 1000;// 1s
	private long startTime;

	private Dialog recordIndicator;

	private static int[] res = {R.drawable.mic_2, R.drawable.mic_3, R.drawable.mic_4, R.drawable.mic_5};

	private static ImageView view;

	private MediaRecorder recorder;

	private ObtainDecibelThread thread;

	private Handler volumeHandler;

	private void init() {
		volumeHandler = new ShowVolumeHandler();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		//路径名
		if (mFileName == null)
			return false;

		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				//初始化对话框和录音
				initDialogAndStartRecord();
/*
				if (finishedListener != null) {
					finishedListener.onFinishedRecord(mFileName);

				}*/
				Log.e("BUTTON", "按下了 " );

				break;
			case MotionEvent.ACTION_UP:
				//完成录音
				finishRecord();
				break;
			case MotionEvent.ACTION_CANCEL:// 当手指移动到view外面，会cancel
				//取消录音
				cancelRecord();
				break;
		}

		return true;
	}


	/**
	 * 初始化对话框和录音
	 */
	private void initDialogAndStartRecord() {

		//记录开始时间
		startTime = System.currentTimeMillis();
		//创建一个对话框
		recordIndicator = new Dialog(getContext(), R.style.like_toast_dialog_style);
		//创建一个图片
		view = new ImageView(getContext());
		view.setImageResource(R.drawable.mic_2);
		//设置对话框显示
		recordIndicator.setContentView(view, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		//设置消失的监听
		recordIndicator.setOnDismissListener(onDismiss);
		//拿到屏幕的属性
		LayoutParams lp = recordIndicator.getWindow().getAttributes();
		//设置布局
		lp.gravity = Gravity.CENTER;

		//开始录音
		startRecording();
		//展示出来
		recordIndicator.show();
	}

	/**
	 * 完成录音
	 */
	private void finishRecord() {
		//停止录音
		stopRecording();
		//对话框消失
		recordIndicator.dismiss();
		//计算按下多久了
		long intervalTime = System.currentTimeMillis() - startTime;
		if (intervalTime < MIN_INTERVAL_TIME) {
			Toast.makeText(getContext(), "时间太短！", Toast.LENGTH_SHORT).show();
			//时间太短就删除路径文件
			File file = new File(fileName);
			file.delete();
			return;
		}
		//回调，返回文件的路径
		if (finishedListener != null)
			finishedListener.onFinishedRecord(fileName);
	}

	/**
	 * 取消录音
	 */
	private void cancelRecord() {

		stopRecording();
		recordIndicator.dismiss();

		Toast.makeText(getContext(), "取消录音！", Toast.LENGTH_SHORT).show();
		File file = new File(fileName);
		file.delete();
	}

	/**
	 * 开始录音
	 */
	private void startRecording() {
		//获取媒体录音者
		recorder = new MediaRecorder();
		recorder.setAudioSource(MediaRecorder.AudioSource.MIC);//设置音频源
		recorder.setOutputFormat(MediaRecorder.OutputFormat.RAW_AMR);//输出格式
		recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);//音频编码方式

		String time = setTime();
		fileName = mFileName + "/" + time + ".amr";
		recorder.setOutputFile(fileName);//输出文件

		try {
			recorder.prepare();//准备
		} catch (IOException e) {
			e.printStackTrace();
		}

		recorder.start();//开始
		//获取分贝的线程
		thread = new ObtainDecibelThread();
		thread.start();

	}

	/**
	 * 停止录音
	 */
	private void stopRecording() {
		if (thread != null) {
			thread.exit();//线程退出
			thread = null;
		}
		if (recorder != null) {
			recorder.stop();//停止
			recorder.release();//释放
			recorder = null;//释放资源
		}
	}

	/**
	 * 设置当前时间
	 * @return time
	 */
	private String setTime() {
		SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
		Date curDate = new Date(System.currentTimeMillis());//获取当前时间
		String str = formatter.format(curDate);
		return str;
	}

	/**
	 * 获取分贝的线程
	 */
	private class ObtainDecibelThread extends Thread {

		private volatile boolean running = true;

		//退出
		public void exit() {
			running = false;
		}

		@Override
		public void run() {
			while (running) {
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (recorder == null || !running) {
					break;
				}
				//获取最大音频振幅
				int x = recorder.getMaxAmplitude();
				Log.e(">>>>>>>录音<<<<<<", "" + x);

				if (x != 0) {
					//对数函数，以e为底，10的对数，也就是说e的多少次方等于10
					int f = (int) (10 * Math.log(x) / Math.log(10));
					if (f < 26)
						volumeHandler.sendEmptyMessage(0);
					else if (f < 32)
						volumeHandler.sendEmptyMessage(1);
					else if (f < 38)
						volumeHandler.sendEmptyMessage(2);
					else
						volumeHandler.sendEmptyMessage(3);

				}

			}
		}

	}

	/**
	 * 系统Api Set a listener to be invoked when the dialog is dismissed.
	 * dialog消失后悔唤醒这个监听
	 */
	private OnDismissListener onDismiss = new OnDismissListener() {

		@Override
		public void onDismiss(DialogInterface dialog) {
			//停止录音
			stopRecording();
		}
	};

	static class ShowVolumeHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			view.setImageResource(res[msg.what]);
		}
	}

	//接口
	public interface OnFinishedRecordListener {
		public void onFinishedRecord(String audioPath);
	}

}