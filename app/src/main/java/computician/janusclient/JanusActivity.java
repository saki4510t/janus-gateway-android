package computician.janusclient;

import computician.janusclient.util.SystemUiHider;

import android.opengl.EGLContext;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;

/**
 * Modified by t_saki t_saki@serenegiant.com on 2018
 */
public class JanusActivity extends BaseActivity {
	private static final boolean DEBUG = true;	// set false on  production
	private static final String TAG = JanusActivity.class.getSimpleName();

    private static final boolean AUTO_HIDE = true;

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

    private GLSurfaceView vsv;
    private VideoRenderer.Callbacks localRender;
    private VideoRenderer.Callbacks remoteRender;
    private EchoTest echoTest;
    private VideoRoomTest videoRoomTest;

    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;

    private class MyInit implements Runnable {

        public void run() {
            init();
        }

        private void init() {
            try {
                EGLContext con = VideoRendererGui.getEGLContext();
                echoTest = new EchoTest(localRender, remoteRender);
                echoTest.initializeMediaContext(JanusActivity.this, true, true, true, con);
                echoTest.Start();

            } catch (final Exception ex) {
                Log.w(TAG, ex);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        java.lang.System.setProperty("java.net.preferIPv6Addresses", "false");
        java.lang.System.setProperty("java.net.preferIPv4Stack", "true");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_janus);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        vsv = findViewById(R.id.glview);
        vsv.setPreserveEGLContextOnPause(true);
        vsv.setKeepScreenOn(true);
        VideoRendererGui.setView(vsv, new MyInit());

        localRender = VideoRendererGui.create(72, 72, 25, 25, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false);
        remoteRender = VideoRendererGui.create(0, 0, 25, 25, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true);
    }
	
	@Override
	protected void onResume() {
		super.onResume();

		if (!checkPermissionNetwork()) return;
		if (!checkPermissionAudio()) return;
		checkPermissionCamera();

	}
}
