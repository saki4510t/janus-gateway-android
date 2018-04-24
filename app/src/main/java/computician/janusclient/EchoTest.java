package computician.janusclient;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import org.appspot.apprtc.ProxyVideoSink;
import org.json.JSONObject;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.VideoRenderer;

import java.util.ArrayList;
import java.util.List;

import computician.janusclientapi.IJanusGatewayCallbacks;
import computician.janusclientapi.IJanusPluginCallbacks;
import computician.janusclientapi.IPluginHandleWebRTCCallbacks;
import computician.janusclientapi.JanusMediaConstraints;
import computician.janusclientapi.JanusPluginHandle;
import computician.janusclientapi.JanusServer;
import computician.janusclientapi.JanusSupportedPluginPackages;
import computician.janusclientapi.PluginHandleSendMessageCallbacks;

/**
 * Created by ben.trent on 7/24/2015.
 * Modified by t_saki t_saki@serenegiant.com on 2018
 */

//TODO create message classes unique to this plugin

public class EchoTest {
    private static final boolean DEBUG = true;	// set false on  production
   	private static final String TAG = EchoTest.class.getSimpleName();

	private final ProxyVideoSink localRenderer;
    private final VideoRenderer.Callbacks remoteRender;
    private final JanusServer janusServer;
    @NonNull
    private final String serverUrl;
    private JanusPluginHandle handle = null;

    public class JanusGlobalCallbacks implements IJanusGatewayCallbacks {

        @Override
        public void onSuccess() {
            if (DEBUG) Log.v(TAG, "onSuccess:");
            janusServer.attach(new JanusPluginCallbacks());
        }

        @Override
        public void onDestroy() {
            if (DEBUG) Log.v(TAG, "onDestroy:");
        }

        @Override
        public String getServerUri() {
            if (DEBUG) Log.v(TAG, "getServerUri:");
            return serverUrl;
        }

        @Override
        public List<PeerConnection.IceServer> getIceServers() {
            if (DEBUG) Log.v(TAG, "getIceServers:");
            return new ArrayList<PeerConnection.IceServer>();
        }

        @Override
        public boolean getIpv6Support() {
            if (DEBUG) Log.v(TAG, "getIpv6Support:");
            return false;
        }

        @Override
        public int getMaxPollEvents() {
            if (DEBUG) Log.v(TAG, "getMaxPollEvents:");
            return 0;
        }

        @Override
        public void onCallbackError(final String error) {
            if (DEBUG) Log.v(TAG, "onCallbackError:" + error);
        }
    }

    public class JanusPluginCallbacks implements IJanusPluginCallbacks {

        @Override
        public void success(final JanusPluginHandle pluginHandle) {
            EchoTest.this.handle = pluginHandle;

                final JSONObject msg = new JSONObject();
                final JSONObject obj = new JSONObject();
                try {
                    obj.put("audio", true);
                    obj.put("video", true);
                    msg.put("message", obj);
                    handle.sendMessage(new PluginHandleSendMessageCallbacks(msg));
                } catch (Exception ex) {

                }

            handle.createOffer(new IPluginHandleWebRTCCallbacks() {
                @Override
                public JSONObject getJsep() {
                    if (DEBUG) Log.v(TAG, "getJsep:");
                    return null;
                }

                @Override
                public void onCallbackError(final String error) {
                    Log.w(TAG, error);
                }

                @Override
                public boolean getTrickle() {
                    if (DEBUG) Log.v(TAG, "getTrickle:");
                    return true;
                }

				@Override
                public JanusMediaConstraints getMedia() {
                    if (DEBUG) Log.v(TAG, "getMedia:");
                    return new JanusMediaConstraints();
                }

                @Override
                public void onSuccess(final JSONObject obj) {
                    if (DEBUG) Log.d(TAG, "OnSuccess for CreateOffer called");
                    try {
                        final JSONObject body = new JSONObject();
                        final JSONObject msg = new JSONObject();
                        body.put("audio", true);
                        body.put("video", true);
                        msg.put("message", body);
                        msg.put("jsep", obj);
                        handle.sendMessage(new PluginHandleSendMessageCallbacks(msg));
                    } catch (Exception ex) {

                    }
                }
            });

        }

        @Override
        public void onMessage(final JSONObject msg, final JSONObject jsepLocal) {
            if (jsepLocal != null)
            {
                handle.handleRemoteJsep(new IPluginHandleWebRTCCallbacks() {
                    final JSONObject myJsep = jsepLocal;
                    @Override
                    public void onSuccess(final JSONObject obj) {
                        if (DEBUG) Log.v(TAG, "onSuccess:" + obj);
                    }

                    @Override
                    public JSONObject getJsep() {
                        if (DEBUG) Log.v(TAG, "getJsep:");
                        return myJsep;
                    }

                    @Override
                    public JanusMediaConstraints getMedia() {
                        if (DEBUG) Log.v(TAG, "getMedia:");
                        return null;
                    }

                    @Override
                    public boolean getTrickle() {
                        if (DEBUG) Log.v(TAG, "getTrickle:");
                        return false;
                    }

					@Override
                    public void onCallbackError(String error) {
                        Log.w(TAG, error);
                    }
                });
            }
        }

		@Override
        public void onLocalStream(final MediaStream stream) {
			if (DEBUG) Log.v(TAG, "onLocalStream:" + stream);
			stream.videoTracks.get(0).addRenderer(new VideoRenderer(localRenderer));
//			VideoRendererGui.update(localRender, 0, 0, 25, 25, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false);
        }

        @Override
        public void onRemoteStream(final MediaStream stream) {
			if (DEBUG) Log.v(TAG, "onRemoteStream:" + stream);
			stream.videoTracks.get(0).setEnabled(true);
			if (stream.videoTracks.get(0).enabled()) {
                Log.d(TAG, "video tracks enabled");
			}
			stream.videoTracks.get(0).addRenderer(new VideoRenderer(remoteRender));
//			VideoRendererGui.update(remoteRender, 0, 0, 25, 25, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true);
//			VideoRendererGui.update(localRender, 72, 72, 25, 25, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false);
        }

        @Override
        public void onDataOpen(final Object data) {
            if (DEBUG) Log.v(TAG, "onDataOpen:" + data);
        }

        @Override
        public void onData(final Object data) {
            if (DEBUG) Log.v(TAG, "onData:" + data);
        }

        @Override
        public void onCleanup() {
            if (DEBUG) Log.v(TAG, "onCleanup:");
        }

        @Override
        public JanusSupportedPluginPackages getPlugin() {
            if (DEBUG) Log.v(TAG, "getPlugin:");
            return JanusSupportedPluginPackages.JANUS_ECHO_TEST;
        }

        @Override
        public void onCallbackError(final String error) {
            Log.w(TAG, error);
        }

        @Override
        public void onDetached() {
            if (DEBUG) Log.v(TAG, "onDetached:");
        }

    }

    public EchoTest(@NonNull final String serverUrl,
    	 final ProxyVideoSink localRenderer,
        final VideoRenderer.Callbacks remoteRender) {

		if (DEBUG) Log.v(TAG, "ctor:" + serverUrl);
		this.serverUrl = serverUrl;
        this.localRenderer = localRenderer;
        this.remoteRender = remoteRender;
        janusServer = new JanusServer(localRenderer, new JanusGlobalCallbacks());
    }

	public boolean initializeMediaContext(final Context context,
		final boolean audio,
		final boolean video, final boolean videoHwAcceleration/*, EGLContext eglContext*/) {

        return janusServer.initializeMediaContext(context, audio, video, videoHwAcceleration/*, eglContext*/);
    }

    public void Start() {
        janusServer.connect();
    }
}
