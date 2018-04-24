package computician.janusclient;

import computician.janusclient.util.SystemUiHider;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import org.appspot.apprtc.DataChannelParameters;
import org.appspot.apprtc.ProxyVideoSink;
import org.appspot.apprtc.UnhandledExceptionHandler;
import org.webrtc.EglBase;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoFileRenderer;
import org.webrtc.VideoRenderer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.appspot.apprtc.Const.*;

/**
 * Modified by t_saki t_saki@serenegiant.com on 2018
 */
public class JanusActivity extends BaseActivity {
	private static final boolean DEBUG = true;	// set false on  production
	private static final String TAG = JanusActivity.class.getSimpleName();

	private static final int CAPTURE_PERMISSION_REQUEST_CODE = 1;

    private static final boolean AUTO_HIDE = true;
	private static final int TEST = 1;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = true;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

	private static class ProxyRenderer implements VideoRenderer.Callbacks {
		private VideoRenderer.Callbacks target;

		@Override
		public synchronized void renderFrame(final VideoRenderer.I420Frame frame) {
			if (target == null) {
				if (DEBUG) Log.d(TAG, "Dropping frame in proxy because target is null.");
				VideoRenderer.renderFrameDone(frame);
				return;
			}

			target.renderFrame(frame);
		}

		public synchronized void setTarget(final VideoRenderer.Callbacks target) {
			this.target = target;
		}
	}

	private final ProxyRenderer remoteProxyRenderer = new ProxyRenderer();
	private final ProxyVideoSink localProxyVideoSink = new ProxyVideoSink();
	@Nullable
	private SurfaceViewRenderer pipRenderer;
	@Nullable
	private SurfaceViewRenderer fullscreenRenderer;
	@Nullable
	private VideoFileRenderer videoFileRenderer;
	private final List<VideoRenderer.Callbacks> remoteRenderers = new ArrayList<>();

	private boolean iceConnected;
	private boolean isError;
	// True if local view is in the fullscreen renderer.
	private boolean isSwappedFeeds;
	private boolean screenCaptureEnabled = false;

    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;

//    private class MyInit implements Runnable {
//
//		@Override
//        public void run() {
//            init();
//        }
//
//        private void init() {
//            try {
//                final EGLContext con = VideoRendererGui.getEGLContext();
//                switch (TEST) {
//				case 0:
//				{
//					final EchoTest echoTest = new EchoTest(localRender, remoteRender);
//					echoTest.initializeMediaContext(JanusActivity.this, true, true, true, con);
//					echoTest.Start();
//					break;
//				}
//				case 1:
//				{
//					final VideoRenderer.Callbacks[] renderers = new VideoRenderer.Callbacks[1];
//					renderers[0] = remoteRender;
//					final VideoRoomTest videoRoomTest = new VideoRoomTest(localRender, renderers);
//					videoRoomTest.initializeMediaContext(JanusActivity.this, true, true, true, con);
//					videoRoomTest.Start();
//					break;
//				}
//				default:
//					throw new IllegalArgumentException("unsupported test index " + TEST);
//
//				}
//
//            } catch (final Exception ex) {
//                Log.w(TAG, ex);
//            }
//        }
//    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		Thread.setDefaultUncaughtExceptionHandler(new UnhandledExceptionHandler(this));

		// Set window styles for fullscreen-window size. Needs to be done before
		// adding content.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
			| WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		getWindow().getDecorView().setSystemUiVisibility(getSystemUiVisibility());
        setContentView(R.layout.activity_janus);

		// Create UI controls.
		pipRenderer = findViewById(R.id.pip_video_view);
		pipRenderer.setKeepScreenOn(true);
		fullscreenRenderer = findViewById(R.id.fullscreen_video_view);
		fullscreenRenderer.setKeepScreenOn(true);

		// Swap feeds on pip view click.
		pipRenderer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				setSwappedFeeds(!isSwappedFeeds);
			}
		});
		remoteRenderers.add(remoteProxyRenderer);

		final Intent intent = getIntent();
		final EglBase eglBase = EglBase.create();

		// Create video renderers.
		pipRenderer.init(eglBase.getEglBaseContext(), null);
		pipRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
		final String saveRemoteVideoToFile = intent.getStringExtra(EXTRA_SAVE_REMOTE_VIDEO_TO_FILE);

		// When saveRemoteVideoToFile is set we save the video from the remote to a file.
		if (!TextUtils.isEmpty(saveRemoteVideoToFile)) {
			final int videoOutWidth = intent.getIntExtra(EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_WIDTH, 0);
			final int videoOutHeight = intent.getIntExtra(EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_HEIGHT, 0);
			try {
				videoFileRenderer = new VideoFileRenderer(
					saveRemoteVideoToFile, videoOutWidth, videoOutHeight, eglBase.getEglBaseContext());
				remoteRenderers.add(videoFileRenderer);
			} catch (final IOException e) {
				throw new RuntimeException(
				"Failed to open video file for output: " + saveRemoteVideoToFile, e);
			}
		}
		fullscreenRenderer.init(eglBase.getEglBaseContext(), null);
		fullscreenRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);

		pipRenderer.setZOrderMediaOverlay(true);
		pipRenderer.setEnableHardwareScaler(true /* enabled */);
		fullscreenRenderer.setEnableHardwareScaler(false /* enabled */);
		// Start with local feed in fullscreen and swap it to the pip when the call is connected.
		setSwappedFeeds(true /* isSwappedFeeds */);

		// Get Intent parameters.
		final String roomId = intent.getStringExtra(EXTRA_ROOMID);
		if (DEBUG) Log.d(TAG, "Room ID: " + roomId);
		if (roomId == null || roomId.length() == 0) {
			Log.e(TAG, "Incorrect room ID in intent!");
			setResult(RESULT_CANCELED);
			finish();
			return;
		}
		int videoWidth = intent.getIntExtra(EXTRA_VIDEO_WIDTH, 0);
		int videoHeight = intent.getIntExtra(EXTRA_VIDEO_HEIGHT, 0);
		screenCaptureEnabled = intent.getBooleanExtra(EXTRA_SCREENCAPTURE, false);
		// If capturing format is not specified for screencapture, use screen resolution.
		if (screenCaptureEnabled
			&& (videoWidth == 0) && (videoHeight == 0)) {

			final DisplayMetrics displayMetrics = getDisplayMetrics();
			videoWidth = displayMetrics.widthPixels;
			videoHeight = displayMetrics.heightPixels;
		}
		DataChannelParameters dataChannelParameters = null;
		if (intent.getBooleanExtra(EXTRA_DATA_CHANNEL_ENABLED, false)) {
			dataChannelParameters = new DataChannelParameters(intent.getBooleanExtra(EXTRA_ORDERED, true),
        	intent.getIntExtra(EXTRA_MAX_RETRANSMITS_MS, -1),
        	intent.getIntExtra(EXTRA_MAX_RETRANSMITS, -1), intent.getStringExtra(EXTRA_PROTOCOL),
        	intent.getBooleanExtra(EXTRA_NEGOTIATED, false), intent.getIntExtra(EXTRA_ID, -1));
		}
		if (screenCaptureEnabled) {
			startScreenCapture();
		} else {
			startCall();
		}
    }
	
	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		Thread.setDefaultUncaughtExceptionHandler(null);
		disconnect();
		super.onDestroy();
	}

	private void setSwappedFeeds(final boolean isSwappedFeeds) {
		if (DEBUG) Log.d(TAG, "setSwappedFeeds: " + isSwappedFeeds);
		this.isSwappedFeeds = isSwappedFeeds;
		localProxyVideoSink.setTarget(isSwappedFeeds ? fullscreenRenderer : pipRenderer);
		remoteProxyRenderer.setTarget(isSwappedFeeds ? pipRenderer : fullscreenRenderer);
		fullscreenRenderer.setMirror(isSwappedFeeds);
		pipRenderer.setMirror(!isSwappedFeeds);
	}

	@TargetApi(17)
	private DisplayMetrics getDisplayMetrics() {
		final DisplayMetrics displayMetrics = new DisplayMetrics();
		final WindowManager windowManager =
			(WindowManager) getApplication().getSystemService(Context.WINDOW_SERVICE);
		windowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
		return displayMetrics;
	}

	@TargetApi(21)
	private void startScreenCapture() {
		final MediaProjectionManager mediaProjectionManager =
			(MediaProjectionManager) getApplication().getSystemService(
			Context.MEDIA_PROJECTION_SERVICE);
		startActivityForResult(
			mediaProjectionManager.createScreenCaptureIntent(), CAPTURE_PERMISSION_REQUEST_CODE);
	}

	private void startCall() {
		final VideoRoomTest videoRoomTest = new VideoRoomTest(localProxyVideoSink, remoteRenderers, null);
		videoRoomTest.initializeMediaContext(JanusActivity.this,
			true, true, true/*, con*/);
		videoRoomTest.Start();
	}

	private void disconnect() {
		remoteProxyRenderer.setTarget(null);
		localProxyVideoSink.setTarget(null);
		if (pipRenderer != null) {
			pipRenderer.release();
			pipRenderer = null;
		}
		if (videoFileRenderer != null) {
			videoFileRenderer.release();
			videoFileRenderer = null;
		}
		if (fullscreenRenderer != null) {
			fullscreenRenderer.release();
			fullscreenRenderer = null;
		}
		if (iceConnected && !isError) {
			setResult(RESULT_OK);
		} else {
			setResult(RESULT_CANCELED);
		}
		finish();
	}
}
