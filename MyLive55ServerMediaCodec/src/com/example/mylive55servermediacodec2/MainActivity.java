package com.example.mylive55servermediacodec2;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

public class MainActivity extends Activity 
    implements MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener  
{

	static {
		System.loadLibrary("live555");
		System.loadLibrary("live555server");
	}
	
	static native int RtspServer(String h264);
	
	private Button converBtn; 
	private Button playBtn; 
	private VideoView videoView;
	
	private MediaController mMediaController;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) { 
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		converBtn = (Button) findViewById(R.id.conver_btn);
		converBtn.setOnClickListener(new OnClickListener() {
			@Override
		    public void onClick(View v){
				// Do something in response to button click
		    	convert(v);
		    }
		});
		videoView = (VideoView)this.findViewById(R.id.conver_vv);

		playBtn = (Button) findViewById(R.id.conver_btn0);
		playBtn.setOnClickListener(new OnClickListener() {
			@Override
		    public void onClick(View v){
				// Do something in response to button click
				PlayRtspStream();
		    }
		});
	}

	public void PlayRtspStream(){
		//String rtspUrl = "rtsp://218.204.223.237:554/live/1/67A7572844E51A64/f68g2mj7wjua3la7.sdp";
		
		String sdp = "rtsp://192.168.1.102:8554/h264test";
		rtsp://192.168.1.102:8554/streamer
		if(sdp != null){
		 //Create media controller
        mMediaController = new MediaController(MainActivity.this);
        videoView.setMediaController(mMediaController);
        
		videoView.setVideoURI(Uri.parse(sdp));
		videoView.requestFocus();
		
		Log.w("MainActivity", "PlayRtspStream sdp = "+sdp);
		Toast.makeText(this, "sdp = "+sdp, 1).show();
		
		videoView.start();
		}
	}

	//监听MediaPlayer上报的错误信息

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
	  // TODO Auto-generated method stub
	  return false;
	}

	//Video播完的时候得到通知

	@Override
	public void onCompletion(MediaPlayer mp) {
	   //this.finish();
	}
	
	
	public void convert(View view) {
		final String h264Path = Environment.getExternalStorageDirectory() + "/butterfly.h264";
		File file = new File(h264Path);
		if(file.exists()){
		    Log.w("MainActivity", "      h264 file is exists!   ");
		}

		int size = (int) file.length();

		System.out.println("h264Path =  " + size);
		if ("".equals(h264Path)) {
			Toast.makeText(this, "路径不能为空", 1).show();
			return;
		}

		new Thread() {

			@Override
			public void run() {
				RtspServer(h264Path);
				//pd.dismiss();
			}

		}.start();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
