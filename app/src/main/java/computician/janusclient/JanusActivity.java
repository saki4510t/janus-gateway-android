package computician.janusclient;

import computician.janusclient.util.SystemUiHider;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

/**
 * Modified by t_saki t_saki@serenegiant.com on 2018
 */
public class JanusActivity extends BaseActivity {
	private static final boolean DEBUG = true;	// set false on  production
	private static final String TAG = JanusActivity.class.getSimpleName();

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

//	private static class ProxyRenderer implements VideoRenderer.Callbacks {
//		private VideoRenderer.Callbacks target;
//
//		@Override
//		public synchronized void renderFrame(final VideoRenderer.I420Frame frame) {
//			if (target == null) {
//				if (DEBUG) Log.d(TAG, "Dropping frame in proxy because target is null.");
//				VideoRenderer.renderFrameDone(frame);
//				return;
//			}
//
//			target.renderFrame(frame);
//		}
//
//		public synchronized void setTarget(final VideoRenderer.Callbacks target) {
//			this.target = target;
//		}
//	}
//
//	private static class ProxyVideoSink implements VideoSink {
//		private VideoSink target;
//
//		@Override
//		public synchronized void onFrame(final VideoFrame frame) {
//			if (target == null) {
//       			if (DEBUG) Log.d(TAG, "Dropping frame in proxy because target is null.");
//				return;
//			}
//
//			target.onFrame(frame);
//		}
//
//		public synchronized void setTarget(@Nullable final VideoSink target) {
//			this.target = target;
//		}
//	}
//
//	private final ProxyRenderer remoteProxyRenderer = new ProxyRenderer();
//	private final ProxyVideoSink localProxyVideoSink = new ProxyVideoSink();
//	@Nullable
//	private SurfaceViewRenderer pipRenderer;
//	@Nullable
//	private SurfaceViewRenderer fullscreenRenderer;
//	@Nullable
//	private VideoFileRenderer videoFileRenderer;
//	private final List<VideoRenderer.Callbacks> remoteRenderers = new ArrayList<>();
//
//    private VideoRenderer.Callbacks localRender;
//    private VideoRenderer.Callbacks remoteRender;

    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;

    private class MyInit implements Runnable {

		@Override
        public void run() {
            init();
        }

        private void init() {
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
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_janus);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		final GLSurfaceView vsv = findViewById(R.id.glview);
        vsv.setPreserveEGLContextOnPause(true);
        vsv.setKeepScreenOn(true);
//        VideoRendererGui.setView(vsv, new MyInit());
//
//        localRender = VideoRendererGui.create(72, 72, 25, 25, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false);
//        remoteRender = VideoRendererGui.create(0, 0, 25, 25, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true);
    }
	
	@Override
	protected void onResume() {
		super.onResume();

		if (!checkPermissionNetwork()) return;
		if (!checkPermissionAudio()) return;
		checkPermissionCamera();

	}
}
