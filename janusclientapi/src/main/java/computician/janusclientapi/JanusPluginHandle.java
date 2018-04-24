package computician.janusclientapi;

import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.*;

import java.io.IOException;
import java.math.BigInteger;

import javax.annotation.Nullable;

/**
 * Created by ben.trent on 6/25/2015.
 */
public class JanusPluginHandle {
	private static final boolean DEBUG = true;    // set false on  production
	private static final String TAG = JanusPluginHandle.class.getSimpleName();
	
	private static final String VIDEO_TRACK_ID = "1929283";
	private static final String AUDIO_TRACK_ID = "1928882";
	private static final String LOCAL_MEDIA_ID = "1198181";

	private static final int HD_VIDEO_WIDTH = 1280;
	private static final int HD_VIDEO_HEIGHT = 720;

//================================================================================
	private static class WebRtcObserver implements SdpObserver, PeerConnection.Observer {
		@NonNull
		private final JanusPluginHandle mParent;
		private final IPluginHandleWebRTCCallbacks webRtcCallbacks;
		
		public WebRtcObserver(@NonNull final JanusPluginHandle parent,
			final IPluginHandleWebRTCCallbacks callbacks) {
			mParent = parent;
			this.webRtcCallbacks = callbacks;
		}
		
		@Override
		public void onSetSuccess() {
			if (DEBUG) Log.d(TAG, "On Set Success");
			if (mParent.mySdp == null) {
				mParent.createSdpInternal(webRtcCallbacks, false);
			}
		}
		
		@Override
		public void onSetFailure(final String error) {
			if (DEBUG) Log.d(TAG, "On set Failure");
			//todo JS api does not account for this
			webRtcCallbacks.onCallbackError(error);
		}
		
		@Override
		public void onCreateSuccess(final SessionDescription sdp) {
			if (DEBUG) Log.d(TAG, "Create success");
			mParent.onLocalSdp(sdp, webRtcCallbacks);
		}
		
		@Override
		public void onCreateFailure(String error) {
			if (DEBUG) Log.d(TAG, "Create failure");
			webRtcCallbacks.onCallbackError(error);
		}
		
		@Override
		public void onSignalingChange(final PeerConnection.SignalingState state) {
			if (DEBUG) Log.d(TAG, "Signal change " + state.toString());
			switch (state) {
			case STABLE:
				break;
			case HAVE_LOCAL_OFFER:
				break;
			case HAVE_LOCAL_PRANSWER:
				break;
			case HAVE_REMOTE_OFFER:
				break;
			case HAVE_REMOTE_PRANSWER:
				break;
			case CLOSED:
				break;
			}
		}
		
		@Override
		public void onIceConnectionChange(final PeerConnection.IceConnectionState state) {
			if (DEBUG) Log.d(TAG, "Ice Connection change " + state.toString());
			switch (state) {
			case DISCONNECTED:
				break;
			case CONNECTED:
				break;
			case NEW:
				break;
			case CHECKING:
				break;
			case CLOSED:
				break;
			case FAILED:
				break;
			default:
				break;
			}
		}
		
		@Override
		public void onIceConnectionReceivingChange(final boolean b) {
		
		}
		
		@Override
		public void onIceGatheringChange(final PeerConnection.IceGatheringState state) {
			switch (state) {
			case NEW:
				break;
			case GATHERING:
				break;
			case COMPLETE:
				if (!mParent.trickle) {
					mParent.mySdp = mParent.mPeerConnection.getLocalDescription();
					mParent.sendSdp(webRtcCallbacks);
				} else {
					mParent.sendTrickleCandidate(null);
				}
				break;
			default:
				break;
			}
			if (DEBUG) Log.d(TAG, "Ice Gathering " + state.toString());
		}
		
		@Override
		public void onIceCandidate(IceCandidate candidate) {
			if (mParent.trickle) {
				mParent.sendTrickleCandidate(candidate);
			}
		}
		
		@Override
		public void onIceCandidatesRemoved(final IceCandidate[] iceCandidates) {
		
		}
		
		@Override
		public void onAddStream(final MediaStream stream) {
			if (DEBUG) Log.d(TAG, "onAddStream " + stream.label());
			mParent.remoteStream = stream;
			mParent.onRemoteStream(stream);
		}
		
		@Override
		public void onRemoveStream(final MediaStream stream) {
			if (DEBUG) Log.d(TAG, "onRemoveStream");
		}
		
		@Override
		public void onDataChannel(final DataChannel channel) {
			if (DEBUG) Log.d(TAG, "onDataChannel");
		}
		
		@Override
		public void onRenegotiationNeeded() {
			if (DEBUG) Log.d(TAG,"Renegotiation needed");
		}
		
		@Override
		public void onAddTrack(final RtpReceiver rtpReceiver, final MediaStream[] mediaStreams) {
		
		}
		
	}
	
//================================================================================
	private static class AsyncPrepareWebRtc
		extends AsyncTask<IPluginHandleWebRTCCallbacks, Void, Void> {
		
		@NonNull
		private final JanusPluginHandle mParent;

		public AsyncPrepareWebRtc(@NonNull final JanusPluginHandle parent) {
			mParent = parent;
		}
		
		@Override
		protected Void doInBackground(final IPluginHandleWebRTCCallbacks... params) {
			final IPluginHandleWebRTCCallbacks cb = params[0];
			mParent.prepareWebRtc(cb);
			return null;
		}
	}
	
//================================================================================
	private static class AsyncHandleRemoteJsep
		extends AsyncTask<IPluginHandleWebRTCCallbacks, Void, Void> {
		
		@NonNull
		private final JanusPluginHandle mParent;
		public AsyncHandleRemoteJsep(@NonNull final JanusPluginHandle parent) {
			mParent = parent;
		}
		
		@Override
		protected Void doInBackground(final IPluginHandleWebRTCCallbacks... params) {
			final IPluginHandleWebRTCCallbacks webrtcCallbacks = params[0];
			if (mParent.peerConnectionFactory == null) {
				webrtcCallbacks.onCallbackError("WebRtc PeerFactory is not initialized. Please call initializeMediaContext");
				return null;
			}
			final JSONObject jsep = webrtcCallbacks.getJsep();
			if (jsep != null) {
				if (mParent.mPeerConnection == null) {
					if (DEBUG) Log.d(TAG,"could not set remote offer");
					mParent.callbacks.onCallbackError("No peerconnection created, if this is an answer please use createAnswer");
					return null;
				}
				try {
					
					final String sdpString = jsep.getString("sdp");
					if (DEBUG) Log.d(TAG, sdpString);
					SessionDescription.Type type = SessionDescription.Type.fromCanonicalForm(jsep.getString("type"));
					SessionDescription sdp = new SessionDescription(type, sdpString);
					mParent.mPeerConnection.setRemoteDescription(new WebRtcObserver(mParent, webrtcCallbacks), sdp);
				} catch (JSONException ex) {
					if (DEBUG) Log.d(TAG, ex.getMessage());
					webrtcCallbacks.onCallbackError(ex.getMessage());
				}
			}
			return null;
		}
	}
	
//================================================================================
	private boolean started = false;
	private MediaStream myStream = null;
	private MediaStream remoteStream = null;
	private SessionDescription mySdp = null;
	private PeerConnection mPeerConnection = null;
	private DataChannel dataChannel = null;
	@Nullable
	private VideoCapturer videoCapturer;
	private boolean trickle = true;
	private boolean iceDone = false;
	private boolean sdpSent = false;
	private PeerConnectionFactory peerConnectionFactory = null;
	private final JanusServer server;
	public final JanusSupportedPluginPackages plugin;
	public final BigInteger id;
	private final IJanusPluginCallbacks callbacks;
	@Nullable
	private VideoSink localRender;
	
	/**
	 * Constructor
	 * @param server
	 * @param localRender
	 * @param plugin
	 * @param handle_id
	 * @param callbacks
	 */
	public JanusPluginHandle(final JanusServer server,
		final VideoSink localRender,
		final JanusSupportedPluginPackages plugin,
		final BigInteger handle_id, final IJanusPluginCallbacks callbacks) {
		
		this.server = server;
		this.localRender = localRender;
		this.plugin = plugin;
		id = handle_id;
		this.callbacks = callbacks;
		peerConnectionFactory = PeerConnectionFactory.builder()
//			.setOptions(options)
//			.setAudioDeviceModule(adm)
//			.setVideoEncoderFactory(encoderFactory)
//			.setVideoDecoderFactory(decoderFactory)
			.createPeerConnectionFactory();
	}
	
	public void onMessage(final String msg) {
		try {
			final JSONObject obj = new JSONObject(msg);
			callbacks.onMessage(obj, null);
		} catch (final JSONException ex) {
			//TODO do we want to notify the GatewayHandler?
		}
	}
	
	public void onMessage(final JSONObject msg, final JSONObject jsep) {
		callbacks.onMessage(msg, jsep);
	}
	
	private void onLocalStream(final MediaStream stream) {
		callbacks.onLocalStream(stream);
	}
	
	private void onRemoteStream(final MediaStream stream) {
		callbacks.onRemoteStream(stream);
	}
	
	public void onDataOpen(final Object data) {
		callbacks.onDataOpen(data);
	}
	
	public void onData(final Object data) {
		callbacks.onData(data);
	}
	
	public void onCleanup() {
		callbacks.onCleanup();
	}
	
	public void onDetached() {
		callbacks.onDetached();
	}
	
	public void sendMessage(final IPluginHandleSendMessageCallbacks obj) {
		server.sendMessage(TransactionType.plugin_handle_message, id, obj, plugin);
	}
	
	private void streamsDone(final IPluginHandleWebRTCCallbacks webRTCCallbacks) {
		final MediaConstraints pc_cons = new MediaConstraints();
		pc_cons.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
		if (webRTCCallbacks.getMedia().getRecvAudio())
			pc_cons.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
		if (webRTCCallbacks.getMedia().getRecvVideo())
			pc_cons.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
		mPeerConnection = peerConnectionFactory.createPeerConnection(server.iceServers,
			 pc_cons, new WebRtcObserver(this, webRTCCallbacks));
		if (myStream != null)
			mPeerConnection.addStream(myStream);
		if (webRTCCallbacks.getJsep() == null) {
			createSdpInternal(webRTCCallbacks, true);
		} else {
			try {
				final JSONObject obj = webRTCCallbacks.getJsep();
				final String sdp = obj.getString("sdp");
				final SessionDescription.Type type
					= SessionDescription.Type.fromCanonicalForm(obj.getString("type"));
				final SessionDescription sessionDescription = new SessionDescription(type, sdp);
				mPeerConnection.setRemoteDescription(new WebRtcObserver(this, webRTCCallbacks), sessionDescription);
			} catch (final Exception ex) {
				webRTCCallbacks.onCallbackError(ex.getMessage());
			}
		}
	}
	
	public void createOffer(final IPluginHandleWebRTCCallbacks webrtcCallbacks) {
		new AsyncPrepareWebRtc(this).execute(webrtcCallbacks);
	}
	
	public void createAnswer(final IPluginHandleWebRTCCallbacks webrtcCallbacks) {
		new AsyncPrepareWebRtc(this).execute(webrtcCallbacks);
	}
	
	private void prepareWebRtc(final IPluginHandleWebRTCCallbacks callbacks) {
		if (mPeerConnection != null) {
			if (callbacks.getJsep() == null) {
				createSdpInternal(callbacks, true);
			} else {
				try {
					final JSONObject jsep = callbacks.getJsep();
					final String sdpString = jsep.getString("sdp");
					final SessionDescription.Type type
						= SessionDescription.Type.fromCanonicalForm(jsep.getString("type"));
					final SessionDescription sdp = new SessionDescription(type, sdpString);
					mPeerConnection.setRemoteDescription(new WebRtcObserver(this, callbacks), sdp);
				} catch (final JSONException ex) {
					Log.w(TAG, ex);
				}
			}
		} else {
			trickle = callbacks.getTrickle();
			AudioTrack audioTrack = null;
			VideoTrack videoTrack = null;
			MediaStream stream = null;
			if (callbacks.getMedia().getSendAudio()) {
				AudioSource source = peerConnectionFactory.createAudioSource(new MediaConstraints());
				audioTrack = peerConnectionFactory.createAudioTrack(AUDIO_TRACK_ID, source);
			}
			if (callbacks.getMedia().getSendVideo()) {
				videoCapturer = createVideoCapture();
//				if (videoCapturer instanceof CameraVideoCapturer) {
//					Log.d(TAG, "Switch camera");
//					CameraVideoCapturer cameraVideoCapturer = (CameraVideoCapturer) videoCapturer;
//					cameraVideoCapturer.switchCamera(null);
//				}
//				switch (callbacks.getMedia().getCamera()) {
//				case back:
//					capturer = VideoCapturerAndroid.create(VideoCapturerAndroid.getNameOfBackFacingDevice());
//					break;
//				case front:
//					capturer = VideoCapturerAndroid.create(VideoCapturerAndroid.getNameOfFrontFacingDevice());
//					break;
//				}
				if (videoCapturer != null) {
					final MediaConstraints constraints = new MediaConstraints();
					final JanusMediaConstraints.JanusVideo videoConstraints = callbacks.getMedia().getVideo();
	               /* constraints.mandatory.add(new MediaConstraints.KeyValuePair("maxHeight", Integer.toString(videoConstraints.getMaxHeight())));
	                constraints.optional.add(new MediaConstraints.KeyValuePair("minHeight", Integer.toString(videoConstraints.getMinHeight())));
	                constraints.mandatory.add(new MediaConstraints.KeyValuePair("maxWidth", Integer.toString(videoConstraints.getMaxWidth())));
	                constraints.optional.add(new MediaConstraints.KeyValuePair("minWidth", Integer.toString(videoConstraints.getMinWidth())));
	                constraints.mandatory.add(new MediaConstraints.KeyValuePair("maxFrameRate", Integer.toString(videoConstraints.getMaxFramerate())));
	                constraints.optional.add(new MediaConstraints.KeyValuePair("minFrameRate", Integer.toString(videoConstraints.getMinFramerate()))); */
					VideoSource source = peerConnectionFactory.createVideoSource(videoCapturer/*, constraints*/);
					videoCapturer.startCapture(HD_VIDEO_WIDTH, HD_VIDEO_HEIGHT, 30/*FIXME*/);
					videoTrack = peerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID, source);
					videoTrack.setEnabled(true);
					videoTrack.addSink(localRender);
				}
			}
			if (audioTrack != null || videoTrack != null) {
				stream = peerConnectionFactory.createLocalMediaStream(LOCAL_MEDIA_ID);
				if (audioTrack != null)
					stream.addTrack(audioTrack);
				if (videoTrack != null)
					stream.addTrack(videoTrack);
			}
			myStream = stream;
			if (stream != null)
				onLocalStream(stream);
			streamsDone(callbacks);
		}
	}

	private boolean screenCaptureEnabled = false;

	@Nullable
	private VideoCapturer createVideoCapture() {
		final VideoCapturer videoCapturer;
		final String videoFileAsCamera = null; // getIntent().getStringExtra(EXTRA_VIDEO_FILE_AS_CAMERA);
		if (videoFileAsCamera != null) {
			try {
				videoCapturer = new FileVideoCapturer(videoFileAsCamera);
			} catch (IOException e) {
				Log.e(TAG, "Failed to open video file for emulated camera");
				return null;
			}
		} else if (screenCaptureEnabled) {
			return createScreenCapturer();
		} else if (useCamera2()) {
			if (!captureToTexture()) {
				Log.e(TAG, "Camera2 only supports capturing to texture. Either disable Camera2 or enable capturing to texture in the options.");
				return null;
			}

			Logging.d(TAG, "Creating capturer using camera2 API.");
			videoCapturer = null; //			videoCapturer = createCameraCapturer(new Camera2Enumerator(this));
		} else {
    		Logging.d(TAG, "Creating capturer using camera1 API.");
    		videoCapturer = createCameraCapturer(new Camera1Enumerator(captureToTexture()));
  		}
		if (videoCapturer == null) {
			Log.e(TAG, "Failed to open camera");
			return null;
		}
		return videoCapturer;
	}

	@TargetApi(21)
	@Nullable
	private VideoCapturer createScreenCapturer() {
//		if (mediaProjectionPermissionResultCode != Activity.RESULT_OK) {
//			Log.e(TAG, "User didn't give permission to capture the screen.");
//			return null;
//		}
//		return new ScreenCapturerAndroid(
//			mediaProjectionPermissionResultData, new MediaProjection.Callback() {
//				@Override
//				public void onStop() {
//					Log.e(TAG, "User revoked permission to capture the screen.");
//				}
//		});
		return null;
	}

	@Nullable
	private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
		final String[] deviceNames = enumerator.getDeviceNames();

		// First, try to find front facing camera
		Logging.d(TAG, "Looking for front facing cameras.");
		for (String deviceName : deviceNames) {
			if (enumerator.isFrontFacing(deviceName)) {
				Logging.d(TAG, "Creating front facing camera capturer.");
				VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

				if (videoCapturer != null) {
					return videoCapturer;
				}
			}
		}

		// Front facing camera not found, try something else
		Logging.d(TAG, "Looking for other cameras.");
		for (String deviceName : deviceNames) {
			if (!enumerator.isFrontFacing(deviceName)) {
				Logging.d(TAG, "Creating other camera capturer.");
				VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

				if (videoCapturer != null) {
					return videoCapturer;
				}
			}
		}

		return null;
	}

	private boolean useCamera2() {
//		return Camera2Enumerator.isSupported(this) && getIntent().getBooleanExtra(EXTRA_CAMERA2, true);
		return false;
	}

	private boolean captureToTexture() {
//		return getIntent().getBooleanExtra(EXTRA_CAPTURETOTEXTURE_ENABLED, false);
		return true;
	}


	private void createSdpInternal(final IPluginHandleWebRTCCallbacks callbacks,
		final boolean isOffer) {

		final MediaConstraints pc_cons = new MediaConstraints();
		pc_cons.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
		if (callbacks.getMedia().getRecvAudio()) {
			pc_cons.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
		}
		if (callbacks.getMedia().getRecvVideo()) {
			if (DEBUG) Log.d(TAG, "Receiving video");
		}
		if (isOffer) {
			mPeerConnection.createOffer(new WebRtcObserver(this, callbacks), pc_cons);
		} else {
			mPeerConnection.createAnswer(new WebRtcObserver(this, callbacks), pc_cons);
		}
	}
	
	public void handleRemoteJsep(final IPluginHandleWebRTCCallbacks webrtcCallbacks) {
		new AsyncHandleRemoteJsep(this).execute(webrtcCallbacks);
	}
	
	public void hangUp() {
		if (remoteStream != null) {
			remoteStream.dispose();
			remoteStream = null;
		}
		if (myStream != null) {
			myStream.dispose();
			myStream = null;
		}
		if (mPeerConnection != null && mPeerConnection.signalingState() != PeerConnection.SignalingState.CLOSED)
			mPeerConnection.close();
		mPeerConnection = null;
		started = false;
		mySdp = null;
		if (dataChannel != null)
			dataChannel.close();
		dataChannel = null;
		trickle = true;
		iceDone = false;
		sdpSent = false;
	}
	
	public void detach() {
		hangUp();
		final JSONObject obj = new JSONObject();
		server.sendMessage(obj, JanusMessageType.detach, id);
	}
	
	private void onLocalSdp(final SessionDescription sdp,
		final IPluginHandleWebRTCCallbacks callbacks) {

		if (mPeerConnection != null) {
			if (mySdp == null) {
				mySdp = sdp;
				mPeerConnection.setLocalDescription(
					new WebRtcObserver(this, callbacks), sdp);
			}
			if (!iceDone && !trickle)
				return;
			if (sdpSent)
				return;
			try {
				sdpSent = true;
				final JSONObject obj = new JSONObject();
				obj.put("sdp", mySdp.description);
				obj.put("type", mySdp.type.canonicalForm());
				callbacks.onSuccess(obj);
			} catch (JSONException ex) {
				callbacks.onCallbackError(ex.getMessage());
			}
		}
	}
	
	private void sendTrickleCandidate(final IceCandidate candidate) {
		try {
			final JSONObject message = new JSONObject();
			final JSONObject cand = new JSONObject();
			if (candidate == null)
				cand.put("completed", true);
			else {
				cand.put("candidate", candidate.sdp);
				cand.put("sdpMid", candidate.sdpMid);
				cand.put("sdpMLineIndex", candidate.sdpMLineIndex);
			}
			message.put("candidate", cand);
			
			server.sendMessage(message, JanusMessageType.trickle, id);
		} catch (final JSONException ex) {
			Log.w(TAG, ex);
		}
	}
	
	private void sendSdp(final IPluginHandleWebRTCCallbacks callbacks) {
		if (mySdp != null) {
			mySdp = mPeerConnection.getLocalDescription();
			if (!sdpSent) {
				sdpSent = true;
				try {
					final JSONObject obj = new JSONObject();
					obj.put("sdp", mySdp.description);
					obj.put("type", mySdp.type.canonicalForm());
					callbacks.onSuccess(obj);
				} catch (final JSONException ex) {
					callbacks.onCallbackError(ex.getMessage());
				}
			}
		}
	}
}
