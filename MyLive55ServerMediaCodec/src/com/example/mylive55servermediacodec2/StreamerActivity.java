package com.example.mylive55servermediacodec2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

public class StreamerActivity extends Activity {
    private static final String TAG = "StreamerActivity"; 
    private CameraPreview mPreview;
    private Camera mCamera;
    private Streamer mStreamer;

    //URL "rtsp://192.168.1.102:8554/streamer"

    static {
    	System.loadLibrary("live555");
        System.loadLibrary("streamer");
    }
    private native void init(String filename, int width, int height, int frameRate);
    private native void encode(byte[] data);
    private native void deinit();
    private native void loop(String addr);
    private native void end();
    
    private native void RtspServer(String filename);
    private native void RtspServerEnd();
    private native void WriteFrame(byte[] data, int size);
    
    
    VideoView  videoView;
    boolean isPlaying=false;
    
    MediaCodec mediaCodec;
    private BufferedOutputStream outputStream;
    private byte[] h264 = new byte[Streamer.WIDTH*Streamer.HEIGHT*3/2]; 
    private byte[] buf;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_streamer);

     
        if (Build.VERSION.SDK_INT < 20){ //Build.VERSION_CODES.LOLLIPOP) {
            // your code using Camera API here - is between 1-20
        } else if(Build.VERSION.SDK_INT >= 21){//Build.VERSION_CODES.LOLLIPOP) {
            // your code using Camera2 API here - is api 21 or higher
        }
        
        // Create an instance of Camera
        mCamera = getCameraInstance();

        Camera.Parameters params = mCamera.getParameters();

        List<Integer> supportedPreviewFormats = params.getSupportedPreviewFormats();
        for (int i = 0; i < supportedPreviewFormats.size(); i++) {
            Log.d(TAG, "supportedPreviewFormats[" + i + "]="
                    + getImageFormatString(supportedPreviewFormats.get(i)));
        }
        params.setPreviewFormat(ImageFormat.YV12); //NV21

        List<Size> supportedPreviewSizes = params.getSupportedPreviewSizes();
        for (int i = 0; i < supportedPreviewSizes.size(); i++) {
            Log.d(TAG, "supportedPreviewSizes[" + i + "]=" + supportedPreviewSizes.get(i).width
                    + "x" + supportedPreviewSizes.get(i).height);
        }
        params.setPreviewSize(Streamer.WIDTH, Streamer.HEIGHT);

        List<int[]> fpsRange = params.getSupportedPreviewFpsRange();
		for (int[] temp3 : fpsRange) {
		     System.out.println(Arrays.toString(temp3));
		}
		
		//params.setPreviewFpsRange(10000, 30000);  

		//camera.setDisplayOrientation(90);  
		/*
        List<Integer> supportedPreviewFrameRates = params.getSupportedPreviewFrameRates();
        for (int i = 0; i < supportedPreviewFrameRates.size(); i++) {
            Log.d(TAG, "supportedPreviewFrameRates[" + i + "]=" + supportedPreviewFrameRates.get(i));
        }
        */

		params.setPreviewFrameRate(Streamer.FRAME_RATE);
		
        mCamera.setParameters(params);
         
        mStreamer = new Streamer();
		   
		buf = new byte[Streamer.WIDTH*Streamer.HEIGHT*3/2];
		mCamera.addCallbackBuffer(buf);
		mCamera.setPreviewCallbackWithBuffer(mStreamer);

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera, mStreamer);
        
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        isSupportMediaCodecHardDecoder();
		
	    MediaCodecInfo[] mediaCodecInfo = getCodecs();
	    if(mediaCodecInfo.length>0){
	    	for(int i=0; i<mediaCodecInfo.length; i++){
	    		Log.i("Encoder", "selectCodec = "+ mediaCodecInfo[i].getName());
	    	}
	    }
	    
        Button captureButton = (Button) findViewById(R.id.record);
        captureButton.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        if (!mStreamer.isStarted()) {
                        	//showAddrDialog(v);
                        	String addr = "192.168.1.5";
                            Log.w("StreamerActivity", " ip = "+addr);
                            mStreamer.start(addr);
                            ((Button) v).setText("Stop");
                            
                            Toast.makeText(StreamerActivity.this, "IP "+addr, Toast.LENGTH_SHORT).show();
                        } else {
                            mStreamer.stop();
                            end();
                            ((Button) v).setText("Record");
                        }
                    }
                }
                );

        Button focusButton = (Button) findViewById(R.id.focus);
        focusButton.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        mCamera.autoFocus(new AutoFocusCallback() {

                            @Override
                            public void onAutoFocus(boolean success, Camera camera) {
                            }
                        });
                    }
                }
                );
        
        Button converBtn = (Button) findViewById(R.id.conver_btn);
		converBtn.setOnClickListener(new OnClickListener() {
			@Override
		    public void onClick(View v){
				// Do something in response to button click
				if(!mStreamer.isStarted()){
					MediaCodecEncodeInit();
				    RtspPlayH264(v);
				    if(isPlaying){
				       mStreamer.doRtspServer(); 
				    }
				}else{
					isPlaying = false;
				    close();
					((Button) v).setText("Server Start");
					RtspServerEnd();
					mStreamer.stop();
				}
		    }
		});
		
		videoView = (VideoView)this.findViewById(R.id.conver_vv);

		Button playBtn = (Button) findViewById(R.id.conver_btn0);
		playBtn.setOnClickListener(new OnClickListener() {
			@Override
		    public void onClick(View v){
				// Do something in response to button click
				PlayRtspStream();
		    }
		});
	}

    /*
    public void doEncode0(String name){
    	MediaCodec codec = MediaCodec.createByCodecName(name);
    	 codec.configure(format, …);
    	 MediaFormat outputFormat = codec.getOutputFormat(); // option B
    	 codec.start();
    	 for (;;) {
    	   int inputBufferId = codec.dequeueInputBuffer(timeoutUs);
    	   if (inputBufferId >= 0) {
    	     ByteBuffer inputBuffer = codec.getInputBuffer(…);
    	     // fill inputBuffer with valid data
    	     …
    	     codec.queueInputBuffer(inputBufferId, …);
    	   }
    	   int outputBufferId = codec.dequeueOutputBuffer(…);
    	   if (outputBufferId >= 0) {
    	     ByteBuffer outputBuffer = codec.getOutputBuffer(outputBufferId);
    	     MediaFormat bufferFormat = codec.getOutputFormat(outputBufferId); // option A
    	     // bufferFormat is identical to outputFormat
    	     // outputBuffer is ready to be processed or rendered.
    	     …
    	     codec.releaseOutputBuffer(outputBufferId, …);
    	   } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
    	     // Subsequent data will conform to new format.
    	     // Can ignore if using getOutputFormat(outputBufferId)
    	     outputFormat = codec.getOutputFormat(); // option B
    	   }
    	 }
    	 codec.stop();
    	 codec.release();
    }
    public void doEncode(String name){
    	MediaCodec codec = MediaCodec.createByCodecName(name);
	    MediaFormat mOutputFormat; // member variable
	    codec.setCallback(new MediaCodec.Callback() {
	      @Override
	      void onInputBufferAvailable(MediaCodec mc, int inputBufferId) {
	        ByteBuffer inputBuffer = codec.getInputBuffer(inputBufferId);
	        // fill inputBuffer with valid data
	        codec.queueInputBuffer(inputBufferId, …);
	      }

	      @Override
	      void onOutputBufferAvailable(MediaCodec mc, int outputBufferId, …) {
	        ByteBuffer outputBuffer = codec.getOutputBuffer(outputBufferId);
	        MediaFormat bufferFormat = codec.getOutputFormat(outputBufferId); // option A
	        // bufferFormat is equivalent to mOutputFormat
	        // outputBuffer is ready to be processed or rendered.
	        codec.releaseOutputBuffer(outputBufferId, …);
	      }

	      @Override
	      void onOutputFormatChanged(MediaCodec mc, MediaFormat format) {
	        // Subsequent data will conform to new format.
	        // Can ignore if using getOutputFormat(outputBufferId)
	        mOutputFormat = format; // option B
	      }

	      @Override
	      void onError()) {
	        
	      }
	    });
	    codec.configure(format,);
	    mOutputFormat = codec.getOutputFormat(); // option B
	    codec.start();
	    // wait for processing to complete
	    codec.stop();
	    codec.release();    
    }
    */
    
    public static MediaCodecInfo[] getCodecs() {

	    //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
	    //    MediaCodecList mediaCodecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
	    //    return mediaCodecList.getCodecInfos();
	    //} else {
	        int numCodecs = MediaCodecList.getCodecCount();
	        MediaCodecInfo[] mediaCodecInfo = new MediaCodecInfo[numCodecs];

	        for (int i = 0; i < numCodecs; i++) {
	            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
	            mediaCodecInfo[i] = codecInfo;
	        }

	        return mediaCodecInfo;
	    //}       
	}
    
    public boolean isSupportMediaCodecHardDecoder(){
	    boolean isHardcode = false;
	    //读取系统配置文件/system/etc/media_codecc.xml
	    File file = new File("/system/etc/media_codecs.xml");
	    InputStream inFile = null;
	    try {
	      inFile = new FileInputStream(file);
	    } catch (Exception e) {
	        // TODO: handle exception
	    }

	    if(inFile != null) { 
	        XmlPullParserFactory pullFactory;
	        try {
	            pullFactory = XmlPullParserFactory.newInstance();
	            XmlPullParser xmlPullParser = pullFactory.newPullParser();
	            xmlPullParser.setInput(inFile, "UTF-8");
	            int eventType = xmlPullParser.getEventType();
	            while (eventType != XmlPullParser.END_DOCUMENT) {
	                String tagName = xmlPullParser.getName();
	                switch (eventType) {
	                case XmlPullParser.START_TAG:
	                    if ("MediaCodec".equals(tagName)) {
	                        String componentName = xmlPullParser.getAttributeValue(0);
	                        
	                        Log.i("MediaCodec", "MediaCodec = "+componentName);
	                        
	                        if(componentName.startsWith("OMX."))
	                        {
	                            if(!componentName.startsWith("OMX.google."))
	                            {
	                                isHardcode = true;
	                            }
	                        }
	                    }
	                }
	                eventType = xmlPullParser.next();
	            }
	        } catch (Exception e) {
	            // TODO: handle exception
	        }
	    }
	    return isHardcode;
    }

    public void RtspPlayH264(View view) {
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

		Button btn = (Button)view;
		btn.setText("Server Play");

		new Thread() {
			@Override
			public void run() {
				isPlaying = true;
				Log.w("StreamerActivity", " isPlaying = "+h264Path);
				RtspServer(h264Path);
				//pd.dismiss();
			}

		}.start();
	}
    
	public void PlayRtspStream(){
		//String rtspUrl = "rtsp://218.204.223.237:554/live/1/67A7572844E51A64/f68g2mj7wjua3la7.sdp";
		
		String sdp = "rtsp://192.168.1.3:8558/h264test";
		if(sdp != null){
		 //Create media controller
        //mMediaController = new MediaController(MainActivity.this);
        //videoView.setMediaController(mMediaController);
        
		videoView.setVideoURI(Uri.parse(sdp));
		videoView.requestFocus();
		
		//Log.w("MainActivity", "PlayRtspStream sdp = "+sdp);
		Toast.makeText(this, "sdp = "+sdp, 1).show();
		
		videoView.start();
		}
	}
    
    
    private void showAddrDialog(final View v) {
        final EditText input = new EditText(this);
        input.setText("192.168.1.3");//SDP address: "
        new AlertDialog.Builder(this)
                .setTitle("Destination IP: *.*.*.*")
                .setView(input)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String addr = input.getText().toString();
                        
                        Log.w("StreamerActivity", "ip = "+addr);
                        //addr = "192.168.1.102";
                        //if (addr.matches("^([1-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(\\.([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])){3}$")) 
                        if (addr.matches("^([1-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(\\.([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])){3}$")) 
                        {
                        	Log.w("StreamerActivity", "00 ip = "+addr);
                            mStreamer.start(addr);
                            ((Button) v).setText("Stop");
                            
                        } else {
                            Toast.makeText(StreamerActivity.this, "Check IP!", Toast.LENGTH_SHORT).show();
                        }
                    }
                }).setNegativeButton(android.R.string.cancel, null).show();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy()");
        end();
        close();
        super.onDestroy();
    }


    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
            //camera = Camera.open(Camera.getNumberOfCameras()-1);
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
        	c = null;
            e.printStackTrace();
        }
        return c; // returns null if camera is unavailable
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera(); // release the camera immediately on pause event
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release(); // release the camera for other applications
            mCamera = null;
        }
    }

    public static String getImageFormatString(int imageFormat) {
        switch (imageFormat) {
            case ImageFormat.JPEG:
                return "JPEG";
            case ImageFormat.NV16:
                return "NV16";
            case ImageFormat.NV21:
                return "NV21";
            case ImageFormat.RGB_565:
                return "RGB_565";
            case ImageFormat.YUY2:
                return "YUY2";
            case ImageFormat.YV12:
                return "YV12";
            default:
                return "UNKNOWN";
        }
    }
    
    public class Streamer implements PreviewCallback {
        private static final String TAG = "Streamer";

        public static final int WIDTH = 320;//640;
        public static final int HEIGHT = 240;//480;
        public static final int FRAME_RATE = 6;
        
        private boolean mIsStarted;
        private byte[] mData;
        
        public void start(final String addr) {
        	//final String h264Path = Environment.getExternalStorageDirectory() + "/streamer.h264";
        	File f = new File(Environment.getExternalStorageDirectory(), "streamer.h264");
    		try {
    			 if(!f.exists()){
    			    f.createNewFile();
    			    Log.w("StreamerActivity", " mp4 file "+f.getPath());
    			 }else{
    				if(f.delete()){
    				   Log.w("StreamerActivity", " mp4 file create again! ");
    				   f.createNewFile();
    				}
    			}
    		} catch (IOException e) {
    			 e.printStackTrace();
    		}
            init(Environment.getExternalStorageDirectory() + "/streamer.h264", WIDTH, HEIGHT, FRAME_RATE);
            mIsStarted = true;
            
            new Thread(new Runnable() {
                
                @Override
                public void run() {
                    loop(addr);
                }
            }).start();
        }
        
        public void doRtspServer(){
        	mIsStarted = true;
        	Log.w("StreamerActivity", "       doRtspServer       ");
        }
        
        public void stop() {
            mIsStarted = false;
            deinit();
        }
        
        public boolean isStarted() {
            return mIsStarted;
        }

        public int frameCount = 0;
        public long lastTimestamp = System.currentTimeMillis();
        
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            if (mIsStarted) {
            	//Log.w("StreamerActivity", "    rtsp  encoding     ");
            	
            	//encode(data);
            	onFrame(data, data.length);
            }
            mCamera.addCallbackBuffer(buf);	
//            frameCount++;
//            
//            long currentTime = System.currentTimeMillis();
//            if (currentTime - lastTimestamp > 1000) {
//                lastTimestamp = currentTime;
//                Log.d(TAG, "onPreviewFrame() frames=" + frameCount + ", bytes=" + data.length);
//                
//                frameCount = 0;
//            }
        }
    }
    
    public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
        private static final String TAG = "CameraPreview";

        private SurfaceHolder mHolder;

        private Camera mCamera;
        private Streamer mStreamer;

        public CameraPreview(Context context, Camera camera, Streamer streamer) {
            super(context);
            mCamera = camera;
            mStreamer = streamer;

            // Install a SurfaceHolder.Callback so we get notified when the
            // underlying surface is created and destroyed.
            mHolder = getHolder();
            mHolder.addCallback(this);
            // deprecated setting, but required on Android versions prior to 3.0
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            
        }

        public void surfaceCreated(SurfaceHolder holder) {
            try {
				mCamera.setPreviewDisplay(holder);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			mCamera.setPreviewCallback(mStreamer);
			mCamera.startPreview();
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            // empty. Take care of releasing the Camera preview in your activity.
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            // If your preview can change or rotate, take care of those events here.
            // Make sure to stop the preview before resizing or reformatting it.
            /*
            if (mHolder.getSurface() == null) {
                // preview surface does not exist
                return;
            }

            // stop preview before making changes
            try {
                mCamera.stopPreview();
            } catch (Exception e) {
                // ignore: tried to stop a non-existent preview
            }

            // set preview size and make any resize, rotate or
            // reformatting changes here

            // start preview with new settings
            try {
                mCamera.setPreviewDisplay(mHolder);
                mCamera.setPreviewCallback(mStreamer);
                mCamera.startPreview();

            } catch (Exception e) {
                Log.d(TAG, "Error starting camera preview: " + e.getMessage());
            }
            */
        }
    }
    
    /**
     * Returns a color format that is supported by the codec and by this test code.  If no
     * match is found, this throws a test failure -- the set of formats known to the test
     * should be expanded for new platforms.
     */
    private static int selectColorFormat(MediaCodecInfo codecInfo, String mimeType) {
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mimeType);
        for (int i = 0; i < capabilities.colorFormats.length; i++) {
            int colorFormat = capabilities.colorFormats[i];
            if (isRecognizedFormat(colorFormat)) {
            	Log.i("Encoder", "selectColorFormat = "+colorFormat);
                return colorFormat;
            }
        }
        Log.e("Encoder","couldn't find a good color format for " + codecInfo.getName() + " / " + mimeType);
        return 0;   // not reached
    }

    /**
     * Returns true if this is a color format that this test code understands (i.e. we know how
     * to read and generate frames in this format).
     */
    private static boolean isRecognizedFormat(int colorFormat) {
        switch (colorFormat) {
            // these are the formats we know how to handle for this test
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Returns the first codec capable of encoding the specified MIME type, or null if no
     * match was found.
     */
	private static MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            if (!codecInfo.isEncoder()) {
                continue;
            }

            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                	Log.i("Encoder", "selectCodec = "+codecInfo.getName());
                    return codecInfo;
                }
            }
        }
        return null;
    }
    
	public void MediaCodecEncodeInit(){
		String type = "video/avc";
		
		File f = new File(Environment.getExternalStorageDirectory(), "rtsp.264");
		try {
			 if(!f.exists()){
			    f.createNewFile();
			    Log.w("StreamerActivity", " rtsp file "+f.getPath());
			 }else{
				if(f.delete()){
				   Log.w("StreamerActivity", " rtsp file create again! ");
				   f.createNewFile();
				}
			}
		} catch (IOException e) {
			 e.printStackTrace();
		}
	    try {
	        outputStream = new BufferedOutputStream(new FileOutputStream(f));
	        Log.i("Encoder", "outputStream initialized");
	    } catch (Exception e){ 
	        e.printStackTrace();
	    }
	    
	    int colorFormat = selectColorFormat(selectCodec("video/avc"), "video/avc");
		try {
			mediaCodec = MediaCodec.createEncoderByType(type);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
		MediaFormat mediaFormat = MediaFormat.createVideoFormat(type, Streamer.WIDTH, Streamer.HEIGHT);
		mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 250000);//125kbps  
		mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 15);  
		mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);
		//mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, 
		//		MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);  //COLOR_FormatYUV420Planar
		mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5); //关键帧间隔时间 单位s  
		mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);  
		mediaCodec.start();	
		Log.i("Encoder", "--------------MediaCodecEncodeInit--------------");
	}
	
	public void close() {
	    try {
	    	if(mediaCodec != null){
	        mediaCodec.stop();
	        mediaCodec.release();
	        outputStream.flush();
	        outputStream.close();
	        mediaCodec = null;        
	    	}
	        Log.i("Encoder", "--------------close--------------");
	    } catch (Exception e){ 
	        e.printStackTrace();
	    }
	}

	 //yv12 转 yuv420p  yvu -> yuv  
    private void swapYV12toI420(byte[] yv12bytes, byte[] i420bytes, int width, int height)   
    {        
        System.arraycopy(yv12bytes, 0, i420bytes, 0,width*height);  
        System.arraycopy(yv12bytes, width*height+width*height/4, i420bytes, width*height,width*height/4);  
        System.arraycopy(yv12bytes, width*height, i420bytes, width*height+width*height/4,width*height/4);    
    } 

    
    int  frame_count, frame_count1;
	// encode
	public void onFrame(byte[] buf, int length) {	
		 
		    swapYV12toI420(buf, h264, Streamer.WIDTH, Streamer.HEIGHT); 
		    
		    ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
		    ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
		    int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);
		    if (inputBufferIndex >= 0) {
		        ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
		        inputBuffer.clear();
		        inputBuffer.put(h264, 0, length);
		        mediaCodec.queueInputBuffer(inputBufferIndex, 0, length, 0, 0);
		    }
		    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
		    int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo,0);
		    while (outputBufferIndex >= 0) {
		        ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];

	            byte[] outData = new byte[bufferInfo.size];
	            outputBuffer.get(outData);

	            try {
	            	frame_count++;
	            	if(frame_count>50){
	            		frame_count = 0;
	            		frame_count1++;
	            		Log.i("Encoder", " onFrame = "+frame_count1);
	            	}
					outputStream.write(outData, 0, outData.length);
					//WriteFrame(outData, outData.length);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				 
				// write into h264 file
	            //Log.i("Encoder", outData.length + " bytes written");
		        
		        mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
		        outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
		    }
	 }  
	 
}
