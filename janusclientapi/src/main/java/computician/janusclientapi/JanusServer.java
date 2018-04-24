package computician.janusclientapi;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.VideoSink;

import java.math.BigInteger;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

/**
 * Created by ben.trent on 5/7/2015.
 */
public class JanusServer implements Runnable,
    IJanusMessageObserver, IJanusSessionCreationCallbacks,
    IJanusAttachPluginCallbacks {

    private static final boolean DEBUG = true;	// set false on  production
   	private static final String TAG = JanusServer.class.getSimpleName();

    private static class RandomString {
        final String str = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        final Random rnd = new Random();

        public String randomString(Integer length) {
            StringBuilder sb = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                sb.append(str.charAt(rnd.nextInt(str.length())));
            }
            return sb.toString();
        }
    }

    private static class AsyncAttach extends AsyncTask<IJanusPluginCallbacks, Void ,Void> {
        private final JanusServer mParent;
        public AsyncAttach(@NonNull final JanusServer parent) {
            mParent = parent;
        }
        
        protected Void doInBackground(final IJanusPluginCallbacks... cbs) {
            final IJanusPluginCallbacks cb = cbs[0];
            try {
                final JSONObject obj = new JSONObject();
                obj.put("janus", JanusMessageType.attach);
                obj.put("plugin", cb.getPlugin());
                if (mParent.serverConnection.getMessengerType() == JanusMessengerType.websocket)
                    obj.put("session_id", mParent.sessionId);
                final ITransactionCallbacks tcb
                	= JanusTransactionCallbackFactory
                		.createNewTransactionCallback(mParent,
                			TransactionType.attach, cb.getPlugin(), cb);
                String transaction = mParent.putNewTransaction(tcb);
                obj.put("transaction", transaction);
                mParent.serverConnection.sendMessage(obj.toString(), mParent.sessionId);
            } catch (final JSONException ex) {
                mParent.onCallbackError(ex.getMessage());
            }
            return null;
        }
    }

	private final RandomString stringGenerator = new RandomString();
	private final ConcurrentHashMap<BigInteger, JanusPluginHandle> attachedPlugins = new ConcurrentHashMap<BigInteger, JanusPluginHandle>();
	private final Object attachedPluginsLock = new Object();
	private final ConcurrentHashMap<String, ITransactionCallbacks> transactions = new ConcurrentHashMap<String, ITransactionCallbacks>();
	private final Object transactionsLock = new Object();
	public final String serverUri;
	public final IJanusGatewayCallbacks gatewayObserver;
	public final List<PeerConnection.IceServer> iceServers;
	public final Boolean ipv6Support;
	public final Integer maxPollEvents;
	private BigInteger sessionId;
	private Boolean connected;
	private final IJanusMessenger serverConnection;
	private volatile Thread keep_alive;
	private Boolean peerConnectionFactoryInitialized = false;
	private VideoSink localRender;

	/**
	 * Constructor
	 * @param gatewayCallbacks
	 */
    public JanusServer(final VideoSink localRender, final IJanusGatewayCallbacks gatewayCallbacks) {
    	this.localRender = localRender;
        gatewayObserver = gatewayCallbacks;
        serverUri = gatewayObserver.getServerUri();
        iceServers = gatewayObserver.getIceServers();
        ipv6Support = gatewayObserver.getIpv6Support();
        maxPollEvents = gatewayObserver.getMaxPollEvents();
        connected = false;
        sessionId = new BigInteger("-1");
        serverConnection = JanusMessengerFactory.createMessenger(serverUri, this);
    }

    private String putNewTransaction(ITransactionCallbacks transactionCallbacks) {
        String transaction = stringGenerator.randomString(12);
        synchronized (transactionsLock) {
            while (transactions.containsKey(transaction))
                transaction = stringGenerator.randomString(12);
            transactions.put(transaction, transactionCallbacks);
        }
        return transaction;
    }

    private void createSession() {
        try {
            final JSONObject obj = new JSONObject();
            obj.put("janus", JanusMessageType.create);
            final ITransactionCallbacks cb
            	= JanusTransactionCallbackFactory
            		.createNewTransactionCallback(this, TransactionType.create);
            final String transaction = putNewTransaction(cb);
            obj.put("transaction", transaction);
            serverConnection.sendMessage(obj.toString());
        } catch (JSONException ex) {
            onCallbackError(ex.getMessage());
        }
    }

    public boolean initializeMediaContext(final Context context,
    	final boolean audio, final boolean video,
    	final boolean videoHwAcceleration/*, final EGLContext eglContext*/) {

//		if (!PeerConnectionFactory.initializeAndroidGlobals(context,
//			audio, video, videoHwAcceleration, eglContext)) {
//
//			return false;
//		}
		PeerConnectionFactory.initialize(
			PeerConnectionFactory.InitializationOptions.builder(context)
//          	.setFieldTrials(fieldTrials)
          	.setEnableVideoHwAcceleration(videoHwAcceleration)
          	.setEnableInternalTracer(true)
          	.createInitializationOptions());
        peerConnectionFactoryInitialized = true;
        return true;
    }

	@Override
    public void run() {
        while (keep_alive == Thread.currentThread()) {
            try {
				Thread.sleep(25000);
            } catch (InterruptedException ex) {
            }
            if (!connected || serverConnection.getMessengerType() != JanusMessengerType.websocket)
                return;
            final JSONObject obj = new JSONObject();
            try {
                obj.put("janus", JanusMessageType.keepalive.toString());
                if (serverConnection.getMessengerType() == JanusMessengerType.websocket)
                    obj.put("session_id", sessionId);
                obj.put("transaction", stringGenerator.randomString(12));
                serverConnection.sendMessage(obj.toString(), sessionId);
            } catch (final JSONException ex) {
                gatewayObserver.onCallbackError("Keep alive failed is Janus online?" + ex.getMessage());
                connected = false;
                return;
            }
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public BigInteger getSessionId() {
        return sessionId;
    }

    public void attach(final IJanusPluginCallbacks callbacks) {
        if (!peerConnectionFactoryInitialized) {
            callbacks.onCallbackError("Peerconnection factory is not initialized," +
             	"please initialize via initializeMediaContext" +
             	 "so that Peerconnection can be made by the plugins");
            return;
        }
        new AsyncAttach(this).execute(callbacks);
    }

    public void destroy() {
        serverConnection.disconnect();
        keep_alive = null;
        connected = false;
        gatewayObserver.onDestroy();
        for (final ConcurrentHashMap.Entry<BigInteger, JanusPluginHandle> handle
        	: attachedPlugins.entrySet()) {

            handle.getValue().detach();
        }
        synchronized (transactionsLock) {
            for (final Object trans : transactions.entrySet())
                transactions.remove(trans);
        }
    }

    public void connect() {
        serverConnection.connect();
    }

    public void newMessageForPlugin(final String message, final BigInteger plugin_id) {
        JanusPluginHandle handle = null;
        synchronized (attachedPluginsLock) {
            handle = attachedPlugins.get(plugin_id);
        }
        if (handle != null) {
            handle.onMessage(message);
        }
    }

    @Override
    public void onCallbackError(final String msg) {
        gatewayObserver.onCallbackError(msg);
    }

    public void sendMessage(final JSONObject msg,
    	final JanusMessageType type, final BigInteger handle) {

        try {
            msg.put("janus", type.toString());
            if (serverConnection.getMessengerType() == JanusMessengerType.websocket) {
                msg.put("session_id", sessionId);
                msg.put("handle_id", handle);
            }
            msg.put("transaction", stringGenerator.randomString(12));
            if (connected)
                serverConnection.sendMessage(msg.toString(), sessionId, handle);
            if (type == JanusMessageType.detach) {
                synchronized (attachedPluginsLock) {
                    if (attachedPlugins.containsKey(handle))
                        attachedPlugins.remove(handle);
                }
            }
        } catch (JSONException ex) {
            gatewayObserver.onCallbackError(ex.getMessage());
        }
    }

    //TODO not sure if the send message functions should be Asynchronous

    public void sendMessage(final TransactionType type, final BigInteger handle,
    	final IPluginHandleSendMessageCallbacks callbacks,
    	final JanusSupportedPluginPackages plugin) {
    	
		final JSONObject msg = callbacks.getMessage();
        if (msg != null) {
            try {
                final JSONObject newMessage = new JSONObject();
                newMessage.put("janus", JanusMessageType.message.toString());

                if (serverConnection.getMessengerType() == JanusMessengerType.websocket) {
                    newMessage.put("session_id", sessionId);
                    newMessage.put("handle_id", handle);
                }
                final ITransactionCallbacks cb = JanusTransactionCallbackFactory
                	.createNewTransactionCallback(this,
                		TransactionType.plugin_handle_message, plugin, callbacks);
                final String transaction = putNewTransaction(cb);
                newMessage.put("transaction", transaction);
                if (msg.has("message"))
                    newMessage.put("body", msg.getJSONObject("message"));
                if (msg.has("jsep"))
                    newMessage.put("jsep", msg.getJSONObject("jsep"));
                serverConnection.sendMessage(newMessage.toString(), sessionId, handle);
            } catch (final JSONException ex) {
                callbacks.onCallbackError(ex.getMessage());
            }
        }
    }

    public void sendMessage(final TransactionType type, final BigInteger handle,
    	final IPluginHandleWebRTCCallbacks callbacks,
    	final JanusSupportedPluginPackages plugin) {

        try {
            final JSONObject msg = new JSONObject();
            msg.put("janus", JanusMessageType.message.toString());
            if (serverConnection.getMessengerType() == JanusMessengerType.websocket) {
                msg.put("session_id", sessionId);
                msg.put("handle_id", handle);
            }
            final ITransactionCallbacks cb = JanusTransactionCallbackFactory
            	.createNewTransactionCallback(this,
            		TransactionType.plugin_handle_webrtc_message, plugin, callbacks);
            String transaction = putNewTransaction(cb);
            msg.put("transaction", transaction);
            if (callbacks.getJsep() != null) {
                msg.put("jsep", callbacks.getJsep());
            }
            serverConnection.sendMessage(msg.toString(), sessionId, handle);
        } catch (JSONException ex) {
            callbacks.onCallbackError(ex.getMessage());
        }
    }

    //region MessageObserver
    @Override
    public void receivedNewMessage(final JSONObject obj) {
        try {
            final JanusMessageType type = JanusMessageType.fromString(obj.getString("janus"));
            String transaction = null;
            BigInteger sender = null;
            if (obj.has("transaction")) {
                transaction = obj.getString("transaction");
            }
            if (obj.has("sender"))
                sender = new BigInteger(obj.getString("sender"));
            JanusPluginHandle handle = null;
            if (sender != null) {
                synchronized (attachedPluginsLock) {
                    handle = attachedPlugins.get(sender);
                }
            }
            switch (type) {
                case keepalive:
                    break;
                case ack:
                case success:
                case error: {
                    if (transaction != null) {
                        ITransactionCallbacks cb = null;
                        synchronized (transactionsLock) {
                            cb = transactions.get(transaction);
                            if (cb != null)
                                transactions.remove(transaction);
                        }
                        if (cb != null) {
                            cb.reportSuccess(obj);
                            transactions.remove(transaction);
                        }
                    }
                    break;
                }
                case hangup: {
                    if (handle != null) {
                        handle.hangUp();
                    }
                    break;
                }
                case detached: {
                    if (handle != null) {
                        handle.onDetached();
                        handle.detach();
                    }
                    break;
                }
                case event: {
                    if (handle != null) {
                        JSONObject plugin_data = null;
                        if (obj.has("plugindata"))
                            plugin_data = obj.getJSONObject("plugindata");
                        if (plugin_data != null) {
                            JSONObject data = null;
                            JSONObject jsep = null;
                            if (plugin_data.has("data"))
                                data = plugin_data.getJSONObject("data");
                            if (obj.has("jsep"))
                                jsep = obj.getJSONObject("jsep");
                            handle.onMessage(data, jsep);
                        }
                    }
                }
            }
        } catch (final JSONException ex) {
            gatewayObserver.onCallbackError(ex.getMessage());
        }
    }

    @Override
    public void onOpen() {
        createSession();
    }

    @Override
    public void onClose() {
        connected = false;
        gatewayObserver.onCallbackError("Connection to janus server is closed");
    }

    @Override
    public void onError(final Exception ex) {
        gatewayObserver.onCallbackError("Error connected to Janus gateway. Exception: " + ex.getMessage());
        Log.w(TAG, ex);
    }
    //endregion

    //region SessionCreationCallbacks
    @Override
    public void onSessionCreationSuccess(final JSONObject obj) {
        try {
            sessionId = new BigInteger(obj.getJSONObject("data").getString("id"));
            keep_alive = new Thread(this, "KeepAlive");
            keep_alive.start();
            connected = true;
            //TODO do we want to keep track of multiple sessions and servers?
            gatewayObserver.onSuccess();
        } catch (final JSONException ex) {
            gatewayObserver.onCallbackError(ex.getMessage());
        }
    }

    //endregion

    //region AttachPluginCallbacks

    @Override
    public void attachPluginSuccess(final JSONObject obj,
    	final JanusSupportedPluginPackages plugin,
    	final IJanusPluginCallbacks pluginCallbacks) {

        try {
            final BigInteger handle
            	= new BigInteger(obj.getJSONObject("data").getString("id"));
            final JanusPluginHandle pluginHandle
            	= new JanusPluginHandle(this, localRender, plugin, handle, pluginCallbacks);
            synchronized (attachedPluginsLock) {
                attachedPlugins.put(handle, pluginHandle);
            }
            pluginCallbacks.success(pluginHandle);
        } catch (final JSONException ex) {
            //or do we want to use the pluginCallbacks.error(ex.getMessage());
            gatewayObserver.onCallbackError(ex.getMessage());
        }
    }

    //endregion

}
