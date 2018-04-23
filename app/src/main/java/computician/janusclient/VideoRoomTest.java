package computician.janusclient;

import android.content.Context;
import android.opengl.EGLContext;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.VideoRenderer;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;

import computician.janusclientapi.IJanusGatewayCallbacks;
import computician.janusclientapi.IJanusPluginCallbacks;
import computician.janusclientapi.IPluginHandleWebRTCCallbacks;
import computician.janusclientapi.JanusMediaConstraints;
import computician.janusclientapi.JanusPluginHandle;
import computician.janusclientapi.JanusServer;
import computician.janusclientapi.JanusSupportedPluginPackages;
import computician.janusclientapi.PluginHandleSendMessageCallbacks;
import computician.janusclientapi.PluginHandleWebRTCCallbacks;

import static computician.janusclient.Const.JANUS_URI;

//TODO create message classes unique to this plugin

/**
 * Created by ben.trent on 7/24/2015.
 * Modified by t_saki t_saki@serenegiant.com on 2018
 */
public class VideoRoomTest {
	private static final boolean DEBUG = true;    // set false on  production
	private static final String TAG = VideoRoomTest.class.getSimpleName();
	
	public static final String REQUEST = "request";
	public static final String MESSAGE = "message";
	public static final String PUBLISHERS = "publishers";
	private static final String user_name = "android";
	private static final int roomid = 1234;
	
	private JanusPluginHandle handle = null;
	private VideoRenderer.Callbacks localRender;
	private Deque<VideoRenderer.Callbacks> availableRemoteRenderers = new ArrayDeque<>();
	private HashMap<BigInteger, VideoRenderer.Callbacks> remoteRenderers = new HashMap<>();
	private JanusServer janusServer;
	private BigInteger myid;
	
	public VideoRoomTest(final VideoRenderer.Callbacks localRender,
		final VideoRenderer.Callbacks[] remoteRenders) {
		
		this.localRender = localRender;
		for (int i = 0; i < remoteRenders.length; i++) {
			this.availableRemoteRenderers.push(remoteRenders[i]);
		}
		janusServer = new JanusServer(new JanusGlobalCallbacks());
	}
	
	private class ListenerAttachCallbacks implements IJanusPluginCallbacks {
		final private VideoRenderer.Callbacks renderer;
		final private BigInteger feedid;
		private JanusPluginHandle listener_handle = null;
		
		public ListenerAttachCallbacks(final BigInteger id, final VideoRenderer.Callbacks renderer) {
			this.renderer = renderer;
			this.feedid = id;
		}
		
		public void success(final JanusPluginHandle handle) {
			if (DEBUG) Log.v(TAG, "success:");
			listener_handle = handle;
			try {
				final JSONObject body = new JSONObject();
				final JSONObject msg = new JSONObject();
				body.put(REQUEST, "join");
				body.put("room", roomid);
				body.put("ptype", "listener");
				body.put("feed", feedid);
				msg.put(MESSAGE, body);
				handle.sendMessage(new PluginHandleSendMessageCallbacks(msg));
			} catch (Exception ex) {
			
			}
		}
		
		@Override
		public void onMessage(final JSONObject msg, final JSONObject jsep) {
			if (DEBUG) Log.v(TAG, "onMessage:" + msg);
			
			try {
				final String event = msg.getString("videoroom");
				if (event.equals("attached") && jsep != null) {
					final JSONObject remoteJsep = jsep;
					listener_handle.createAnswer(new IPluginHandleWebRTCCallbacks() {
						@Override
						public void onSuccess(final JSONObject obj) {
							try {
								final JSONObject mymsg = new JSONObject();
								final JSONObject body = new JSONObject();
								body.put(REQUEST, "start");
								body.put("room", roomid);
								mymsg.put(MESSAGE, body);
								mymsg.put("jsep", obj);
								listener_handle.sendMessage(new PluginHandleSendMessageCallbacks(mymsg));
							} catch (Exception ex) {
							
							}
						}
						
						@Override
						public JSONObject getJsep() {
							return remoteJsep;
						}
						
						@Override
						public JanusMediaConstraints getMedia() {
							final JanusMediaConstraints cons = new JanusMediaConstraints();
							cons.setVideo(null);
							cons.setRecvAudio(true);
							cons.setRecvVideo(true);
							cons.setSendAudio(false);
							return cons;
						}
						
						@Override
						public boolean getTrickle() {
							return true;
						}
						
						@Override
						public void onCallbackError(final String error) {
							Log.w(TAG, error);
						}
					});
				}
			} catch (Exception ex) {
			
			}
		}
		
		@Override
		public void onLocalStream(final MediaStream stream) {
			if (DEBUG) Log.v(TAG, "onLocalStream:" + stream);
		}
		
		@Override
		public void onRemoteStream(final MediaStream stream) {
			if (DEBUG) Log.v(TAG, "onRemoteStream:" + stream);
			stream.videoTracks.get(0).addRenderer(new VideoRenderer(renderer));
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
		public void onDetached() {
			if (DEBUG) Log.v(TAG, "onDetached:");
		}
		
		@Override
		public JanusSupportedPluginPackages getPlugin() {
			if (DEBUG) Log.v(TAG, "getPlugin:");
			return JanusSupportedPluginPackages.JANUS_VIDEO_ROOM;
		}
		
		@Override
		public void onCallbackError(final String error) {
			Log.v(TAG, "onCallbackError:" + error);
		}
	}
	
	public class JanusPublisherPluginCallbacks implements IJanusPluginCallbacks {
		
		private void publishOwnFeed() {
			if (handle != null) {
				handle.createOffer(new IPluginHandleWebRTCCallbacks() {
					@Override
					public void onSuccess(final JSONObject obj) {
						try {
							final JSONObject msg = new JSONObject();
							final JSONObject body = new JSONObject();
							body.put(REQUEST, "configure");
							body.put("audio", true);
							body.put("video", true);
							msg.put(MESSAGE, body);
							msg.put("jsep", obj);
							handle.sendMessage(new PluginHandleSendMessageCallbacks(msg));
						} catch (Exception ex) {
						
						}
					}
					
					@Override
					public JSONObject getJsep() {
						return null;
					}
					
					@Override
					public JanusMediaConstraints getMedia() {
						final JanusMediaConstraints cons = new JanusMediaConstraints();
						cons.setRecvAudio(false);
						cons.setRecvVideo(false);
						cons.setSendAudio(true);
						return cons;
					}
					
					@Override
					public boolean getTrickle() {
						return true;
					}
					
					@Override
					public void onCallbackError(String error) {
					
					}
				});
			}
		}
		
		private void registerUsername() {
			if (handle != null) {
				final JSONObject obj = new JSONObject();
				final JSONObject msg = new JSONObject();
				try {
					obj.put(REQUEST, "join");
					obj.put("room", roomid);
					obj.put("ptype", "publisher");
					obj.put("display", user_name);
					msg.put(MESSAGE, obj);
				} catch (Exception ex) {
				
				}
				handle.sendMessage(new PluginHandleSendMessageCallbacks(msg));
			}
		}
		
		private void newRemoteFeed(final BigInteger id) { //todo attach the plugin as a listener
			VideoRenderer.Callbacks myrenderer;
			if (!remoteRenderers.containsKey(id)) {
				if (availableRemoteRenderers.isEmpty()) {
					//TODO no more space
					return;
				}
				remoteRenderers.put(id, availableRemoteRenderers.pop());
			}
			myrenderer = remoteRenderers.get(id);
			janusServer.attach(new ListenerAttachCallbacks(id, myrenderer));
		}
		
		@Override
		public void success(final JanusPluginHandle pluginHandle) {
			handle = pluginHandle;
			registerUsername();
		}
		
		@Override
		public void onMessage(final JSONObject msg, final JSONObject jsepLocal) {
			try {
				final String event = msg.getString("videoroom");
				if (!TextUtils.isEmpty(event)) {
					switch (event) {
					case "joined": {
						myid = new BigInteger(msg.getString("id"));
						publishOwnFeed();
						if (msg.has(PUBLISHERS)) {
							final JSONArray pubs = msg.getJSONArray(PUBLISHERS);
							for (int i = 0; i < pubs.length(); i++) {
								final JSONObject pub = pubs.getJSONObject(i);
								final BigInteger tehId = new BigInteger(pub.getString("id"));
								newRemoteFeed(tehId);
							}
						}
						break;
					}
					case "destroyed": {
						if (DEBUG) Log.v(TAG, "destroyed:");
						break;
					}
					case "event": {
						if (msg.has(PUBLISHERS)) {
							final JSONArray pubs = msg.getJSONArray(PUBLISHERS);
							for (int i = 0; i < pubs.length(); i++) {
								final JSONObject pub = pubs.getJSONObject(i);
								newRemoteFeed(new BigInteger(pub.getString("id")));
							}
						} else if (msg.has("leaving")) {
							if (DEBUG) Log.v(TAG, "leaving:");
						} else if (msg.has("unpublished")) {
							if (DEBUG) Log.v(TAG, "unpublished:");
						} else {
							if (DEBUG) Log.v(TAG, "unknown event:" + event);
							//todo error
						}
						break;
					}
					default:
						break;
					}
				}
				if (jsepLocal != null) {
					handle.handleRemoteJsep(new PluginHandleWebRTCCallbacks(null, jsepLocal, false));
				}
			} catch (Exception ex) {
			
			}
		}
		
		@Override
		public void onLocalStream(final MediaStream stream) {
			if (DEBUG) Log.v(TAG, "onLocalStream:" + stream);
			stream.videoTracks.get(0).addRenderer(new VideoRenderer(localRender));
		}
		
		@Override
		public void onRemoteStream(final MediaStream stream) {
			if (DEBUG) Log.v(TAG, "onRemoteStream:" + stream);
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
			return JanusSupportedPluginPackages.JANUS_VIDEO_ROOM;
		}
		
		@Override
		public void onCallbackError(final String error) {
			Log.w(TAG, error);
		}
		
		@Override
		public void onDetached() {
		
		}
	}
	
	public class JanusGlobalCallbacks implements IJanusGatewayCallbacks {
		public void onSuccess() {
			if (DEBUG) Log.v(TAG, "onSuccess:");
			janusServer.attach(new JanusPublisherPluginCallbacks());
		}
		
		@Override
		public void onDestroy() {
			if (DEBUG) Log.v(TAG, "onDestroy:");
		}
		
		@Override
		public String getServerUri() {
			if (DEBUG) Log.v(TAG, "getServerUri:");
			return JANUS_URI;
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
			Log.w(TAG, error);
		}
	}
	
	public boolean initializeMediaContext(Context context, boolean audio, boolean video, boolean videoHwAcceleration, EGLContext eglContext) {
		return janusServer.initializeMediaContext(context, audio, video, videoHwAcceleration, eglContext);
	}
	
	public void Start() {
		janusServer.connect();
	}
	
}
