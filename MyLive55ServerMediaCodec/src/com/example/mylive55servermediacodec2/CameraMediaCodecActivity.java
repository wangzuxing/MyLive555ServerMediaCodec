package com.example.mylive55servermediacodec2;

import java.util.Arrays;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureRequest.Builder;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

public class CameraMediaCodecActivity extends Activity implements OnImageAvailableListener {

    private CameraManager mCameraManager;

	private SurfaceView mSurfaceView;

	private SurfaceHolder mSurfaceHolder;

	private Handler mHandler;

	private String mCameraId;

	private ImageReader mImageReader;

	private CameraDevice mCameraDevice;

	private CameraCaptureSession mSession;
	
	private int  mState;

	private String TAG = "CameraMediaCodecActivity";
	
	private int  STATE_PREVIEW = CaptureResult.CONTROL_AE_STATE_PRECAPTURE;
	private int  STATE_WAITING_CAPTUR =  CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED;;
	
	private Builder  mPreviewBuilder;

	//private OnImageAvailableListener mOnImageAvailableListener;
	
	/*
	 @see CaptureRequest#CONTROL_AF_MODE
     * @see #CONTROL_MODE_OFF
     * @see #CONTROL_MODE_AUTO
     * @see #CONTROL_MODE_USE_SCENE_MODE
     * @see #CONTROL_MODE_OFF_KEEP_STATE
	*/
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
    	Log.d(TAG , "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        mCameraManager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
        mSurfaceView = (SurfaceView)findViewById(R.id.mSurfaceview);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                initCameraAndPreview();
            }

			@Override
			public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
				// TODO Auto-generated method stub
				
			}
        });    
    }
    
	private void initCameraAndPreview() {
        Log.d("linc","init camera and preview");
        HandlerThread handlerThread = new HandlerThread("Camera2");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());
        try {
            mCameraId = ""+CameraCharacteristics.LENS_FACING_FRONT;
            mImageReader = ImageReader.newInstance(mSurfaceView.getWidth(), mSurfaceView.getHeight(),
                    ImageFormat.JPEG,/*maxImages*/7);
            mImageReader.setOnImageAvailableListener(this, mHandler); //mOnImageAvailableListener

            mCameraManager.openCamera(mCameraId, DeviceStateCallback, mHandler);
        } catch (CameraAccessException e) {
            Log.e("linc", "open camera failed." + e.getMessage());
        }
    }
	
	private CameraDevice.StateCallback DeviceStateCallback = new CameraDevice.StateCallback() {

		@Override
        public void onOpened(CameraDevice camera) {
            Log.d("linc","DeviceStateCallback:camera was opend.");
            //mCameraOpenCloseLock.release();
            mCameraDevice = camera;
            try {
                createCameraCaptureSession();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

		@Override
		public void onDisconnected(CameraDevice camera) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onError(CameraDevice camera, int error) {
			// TODO Auto-generated method stub
			
		}
    };
    
    private void createCameraCaptureSession() throws CameraAccessException {
        Log.d("linc","createCameraCaptureSession");

        mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        mPreviewBuilder.addTarget(mSurfaceHolder.getSurface());
        mState = STATE_PREVIEW;
        mCameraDevice.createCaptureSession(
                Arrays.asList(mSurfaceHolder.getSurface(), mImageReader.getSurface()),
                mSessionPreviewStateCallback, mHandler);
    }
    
    
    private CameraCaptureSession.StateCallback mSessionPreviewStateCallback = new
            CameraCaptureSession.StateCallback() {

        @Override
        public void onConfigured(CameraCaptureSession session) {
            Log.d("linc","mSessionPreviewStateCallback onConfigured");
            mSession = session;
            try {
                mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                session.setRepeatingRequest(mPreviewBuilder.build(), mSessionCaptureCallback, mHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
                Log.e("linc","set preview builder failed."+e.getMessage());
            }
        }

		@Override
		public void onConfigureFailed(CameraCaptureSession session) {
			// TODO Auto-generated method stub
			
		}
    };
    
    private CameraCaptureSession.CaptureCallback mSessionCaptureCallback =
            new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                                       TotalCaptureResult result) {
//            Log.d("linc","mSessionCaptureCallback, onCaptureCompleted");
            mSession = session;
            checkState(result);
        }

        @Override
        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request,
                                        CaptureResult partialResult) {
            Log.d("linc","mSessionCaptureCallback, onCaptureProgressed");
            mSession = session;
            checkState(partialResult);
        }

        private void checkState(CaptureResult result) {
            switch (mState) {
                case 5:  // STATE_PREVIEW  CONTROL_AE_STATE_PRECAPTURE
                    // NOTHING
                    break;
                default:
                // case STATE_WAITING_CAPTURE:
                    int afState = result.get(CaptureResult.CONTROL_AF_STATE);

                    if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState
                            || CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED == afState
                            || CaptureResult.CONTROL_AF_STATE_PASSIVE_UNFOCUSED == afState) {
                        //do something like save picture
                    }
                    break;
            }
        }

    };
    
    public void onCapture(View view) {
        try {
            Log.i("linc", "take picture");
            mState = CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED ;//STATE_WAITING_CAPTURE;
            mSession.setRepeatingRequest(mPreviewBuilder.build(), mSessionCaptureCallback, mHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

	@Override
	public void onImageAvailable(ImageReader reader) {
		// TODO Auto-generated method stub
		Log.i("linc", "   onImageAvailable   ");
	}
}
