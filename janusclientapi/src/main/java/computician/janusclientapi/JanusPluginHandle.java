package computician.janusclientapi;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.*;

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

	private boolean started = false;
	private MediaStream myStream = null;
	private MediaStream remoteStream = null;
	private SessionDescription mySdp = null;
	private PeerConnection pc = null;
	private DataChannel dataChannel = null;
	@Nullable
	private VideoCapturer videoCapturer;
	private boolean trickle = true;
	private boolean iceDone = false;
	private boolean sdpSent = false;
	
	private class WebRtcObserver implements SdpObserver, PeerConnection.Observer {
		private final IPluginHandleWebRTCCallbacks webRtcCallbacks;
		
		public WebRtcObserver(final IPluginHandleWebRTCCallbacks callbacks) {
			this.webRtcCallbacks = callbacks;
		}
		
		@Override
		public void onSetSuccess() {
			if (DEBUG) Log.d(TAG, "On Set Success");
			if (mySdp == null) {
				createSdpInternal(webRtcCallbacks, false);
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
			onLocalSdp(sdp, webRtcCallbacks);
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
				if (!trickle) {
					mySdp = pc.getLocalDescription();
					sendSdp(webRtcCallbacks);
				} else {
					sendTrickleCandidate(null);
				}
				break;
			default:
				break;
			}
			if (DEBUG) Log.d(TAG, "Ice Gathering " + state.toString());
		}
		
		@Override
		public void onIceCandidate(IceCandidate candidate) {
			if (trickle) {
				sendTrickleCandidate(candidate);
			}
		}
		
		@Override
		public void onIceCandidatesRemoved(final IceCandidate[] iceCandidates) {
		
		}
		
		@Override
		public void onAddStream(final MediaStream stream) {
			if (DEBUG) Log.d(TAG, "onAddStream " + stream.label());
			remoteStream = stream;
			onRemoteStream(stream);
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
	
	private PeerConnectionFactory sessionFactory = null;
	private final JanusServer server;
	public final JanusSupportedPluginPackages plugin;
	public final BigInteger id;
	private final IJanusPluginCallbacks callbacks;
	
	/**
	 * FIXME this class should be static otherwise this will leak
	 */
	private class AsyncPrepareWebRtc
		extends AsyncTask<IPluginHandleWebRTCCallbacks, Void, Void> {
		
		@Override
		protected Void doInBackground(final IPluginHandleWebRTCCallbacks... params) {
			final IPluginHandleWebRTCCallbacks cb = params[0];
			prepareWebRtc(cb);
			return null;
		}
	}
	
	/**
	 * FIXME this class should be static otherwise this will leak
 	 */
	private class AsyncHandleRemoteJsep
		extends AsyncTask<IPluginHandleWebRTCCallbacks, Void, Void> {
		
		@Override
		protected Void doInBackground(final IPluginHandleWebRTCCallbacks... params) {
			final IPluginHandleWebRTCCallbacks webrtcCallbacks = params[0];
			if (sessionFactory == null) {
				webrtcCallbacks.onCallbackError("WebRtc PeerFactory is not initialized. Please call initializeMediaContext");
				return null;
			}
			final JSONObject jsep = webrtcCallbacks.getJsep();
			if (jsep != null) {
				if (pc == null) {
					if (DEBUG) Log.d(TAG,"could not set remote offer");
					callbacks.onCallbackError("No peerconnection created, if this is an answer please use createAnswer");
					return null;
				}
				try {
					
					final String sdpString = jsep.getString("sdp");
					if (DEBUG) Log.d(TAG, sdpString);
					SessionDescription.Type type = SessionDescription.Type.fromCanonicalForm(jsep.getString("type"));
					SessionDescription sdp = new SessionDescription(type, sdpString);
					pc.setRemoteDescription(new WebRtcObserver(webrtcCallbacks), sdp);
				} catch (JSONException ex) {
					if (DEBUG) Log.d(TAG, ex.getMessage());
					webrtcCallbacks.onCallbackError(ex.getMessage());
				}
			}
			return null;
		}
	}
	
	public JanusPluginHandle(final JanusServer server,
		final JanusSupportedPluginPackages plugin,
		final BigInteger handle_id, final IJanusPluginCallbacks callbacks) {
		
		this.server = server;
		this.plugin = plugin;
		id = handle_id;
		this.callbacks = callbacks;
		sessionFactory = new PeerConnectionFactory();
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
		pc = sessionFactory.createPeerConnection(server.iceServers, pc_cons, new WebRtcObserver(webRTCCallbacks));
		if (myStream != null)
			pc.addStream(myStream);
		if (webRTCCallbacks.getJsep() == null) {
			createSdpInternal(webRTCCallbacks, true);
		} else {
			try {
				final JSONObject obj = webRTCCallbacks.getJsep();
				final String sdp = obj.getString("sdp");
				final SessionDescription.Type type
					= SessionDescription.Type.fromCanonicalForm(obj.getString("type"));
				final SessionDescription sessionDescription = new SessionDescription(type, sdp);
				pc.setRemoteDescription(new WebRtcObserver(webRTCCallbacks), sessionDescription);
			} catch (final Exception ex) {
				webRTCCallbacks.onCallbackError(ex.getMessage());
			}
		}
	}
	
	public void createOffer(final IPluginHandleWebRTCCallbacks webrtcCallbacks) {
		new AsyncPrepareWebRtc().execute(webrtcCallbacks);
	}
	
	public void createAnswer(final IPluginHandleWebRTCCallbacks webrtcCallbacks) {
		new AsyncPrepareWebRtc().execute(webrtcCallbacks);
	}
	
	private void prepareWebRtc(final IPluginHandleWebRTCCallbacks callbacks) {
		if (pc != null) {
			if (callbacks.getJsep() == null) {
				createSdpInternal(callbacks, true);
			} else {
				try {
					final JSONObject jsep = callbacks.getJsep();
					final String sdpString = jsep.getString("sdp");
					final SessionDescription.Type type
						= SessionDescription.Type.fromCanonicalForm(jsep.getString("type"));
					final SessionDescription sdp = new SessionDescription(type, sdpString);
					pc.setRemoteDescription(new WebRtcObserver(callbacks), sdp);
				} catch (JSONException ex) {
				
				}
			}
		} else {
			trickle = callbacks.getTrickle();
			AudioTrack audioTrack = null;
			VideoTrack videoTrack = null;
			MediaStream stream = null;
			if (callbacks.getMedia().getSendAudio()) {
				AudioSource source = sessionFactory.createAudioSource(new MediaConstraints());
				audioTrack = sessionFactory.createAudioTrack(AUDIO_TRACK_ID, source);
			}
			if (callbacks.getMedia().getSendVideo()) {
				if (videoCapturer instanceof CameraVideoCapturer) {
					Log.d(TAG, "Switch camera");
					CameraVideoCapturer cameraVideoCapturer = (CameraVideoCapturer) videoCapturer;
					cameraVideoCapturer.switchCamera(null);
				}
//				switch (callbacks.getMedia().getCamera()) {
//				case back:
//					capturer = VideoCapturerAndroid.create(VideoCapturerAndroid.getNameOfBackFacingDevice());
//					break;
//				case front:
//					capturer = VideoCapturerAndroid.create(VideoCapturerAndroid.getNameOfFrontFacingDevice());
//					break;
//				}
				final MediaConstraints constraints = new MediaConstraints();
				JanusMediaConstraints.JanusVideo videoConstraints = callbacks.getMedia().getVideo();
               /* constraints.mandatory.add(new MediaConstraints.KeyValuePair("maxHeight", Integer.toString(videoConstraints.getMaxHeight())));
                constraints.optional.add(new MediaConstraints.KeyValuePair("minHeight", Integer.toString(videoConstraints.getMinHeight())));
                constraints.mandatory.add(new MediaConstraints.KeyValuePair("maxWidth", Integer.toString(videoConstraints.getMaxWidth())));
                constraints.optional.add(new MediaConstraints.KeyValuePair("minWidth", Integer.toString(videoConstraints.getMinWidth())));
                constraints.mandatory.add(new MediaConstraints.KeyValuePair("maxFrameRate", Integer.toString(videoConstraints.getMaxFramerate())));
                constraints.optional.add(new MediaConstraints.KeyValuePair("minFrameRate", Integer.toString(videoConstraints.getMinFramerate()))); */
				VideoSource source = sessionFactory.createVideoSource(videoCapturer/*, constraints*/);
				videoTrack = sessionFactory.createVideoTrack(VIDEO_TRACK_ID, source);
			}
			if (audioTrack != null || videoTrack != null) {
				stream = sessionFactory.createLocalMediaStream(LOCAL_MEDIA_ID);
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
			pc.createOffer(new WebRtcObserver(callbacks), pc_cons);
		} else {
			pc.createAnswer(new WebRtcObserver(callbacks), pc_cons);
		}
	}
	
	public void handleRemoteJsep(final IPluginHandleWebRTCCallbacks webrtcCallbacks) {
		new AsyncHandleRemoteJsep().execute(webrtcCallbacks);
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
		if (pc != null && pc.signalingState() != PeerConnection.SignalingState.CLOSED)
			pc.close();
		pc = null;
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

		if (pc != null) {
			if (mySdp == null) {
				mySdp = sdp;
				pc.setLocalDescription(new WebRtcObserver(callbacks), sdp);
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
			mySdp = pc.getLocalDescription();
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
