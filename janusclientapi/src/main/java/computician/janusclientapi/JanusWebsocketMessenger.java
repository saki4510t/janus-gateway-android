package computician.janusclientapi;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.math.BigInteger;

import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.callback.WritableCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;

import org.json.JSONObject;


/**
 * Created by ben.trent on 5/7/2015.
 * Modified by t_saki t_saki@serenegiant.com on 2018
 */
public class JanusWebsocketMessenger implements IJanusMessenger {
	private static final boolean DEBUG = true;	// set false on  production
	private static final String TAG = JanusWebsocketMessenger.class.getSimpleName();
 
	private static final JanusMessengerType type = JanusMessengerType.websocket;

 	@NonNull
    private final String uri;
    @NonNull
    private final IJanusMessageObserver handler;
    @Nullable
    private WebSocket client = null;

    public JanusWebsocketMessenger(@NonNull final String uri,
    	@NonNull final  IJanusMessageObserver handler) {

        this.uri = uri;
        this.handler = handler;
    }

    @Override
    public JanusMessengerType getMessengerType() {
        return type;
    }

    @Override
    public void connect() {
		if (DEBUG) Log.v(TAG, "connect:");

        AsyncHttpClient.getDefaultInstance().websocket(uri, "janus-protocol",
        	new AsyncHttpClient.WebSocketConnectCallback() {

            @Override
            public void onCompleted(@Nullable final Exception ex, @Nullable final WebSocket webSocket) {
            	if (DEBUG) Log.v(TAG, "onCompleted:webSocket=" + webSocket);
                if (ex != null) {
                    handler.onError(ex);
                    return;
                }
                if (webSocket == null) {
                    handler.onError(new RuntimeException("webSocket is null"));
                    return;
                }
                client = webSocket;
                client.setWriteableCallback(new WritableCallback() {
                    @Override
                    public void onWriteable() {
                        Log.d(TAG, "On writable");
                    }
                });
                client.setPongCallback(new WebSocket.PongCallback() {

                    @Override
                    public void onPongReceived(String s) {
                        Log.d(TAG, "Pong callback");
                    }
                });
                client.setDataCallback(new DataCallback() {

                    @Override
                    public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
                        Log.d(TAG, "New Data");
                    }
                });
                client.setEndCallback(new CompletedCallback() {

                    @Override
                    public void onCompleted(Exception ex) {
                        Log.d(TAG, "Client End");
                    }
                });
                client.setStringCallback(new WebSocket.StringCallback() {
                    @Override
                    public void onStringAvailable(String s) {
                        onMessage(s);
                    }
                });
                client.setClosedCallback(new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        Log.d(TAG, "Socket closed for some reason");
                        if (ex != null) {
							Log.w(TAG, ex);
                        }
                        if (ex != null) {
                            onError(ex);
                        } else {
                            onClose(-1, "unknown", true);
                        }
                    }
                });

                handler.onOpen();
            }
        });
    }

    @Override
    public void disconnect() {
    	if (DEBUG) Log.v(TAG, "disconnect:");
    	if (client != null) {
        	client.close();
		} else {
			throw new IllegalStateException("WebSocket is null");
		}
    }

    @Override
    public void sendMessage(@NonNull String message) {
        Log.d(TAG, "Sent: \n\t" + message);
		if (client != null) {
	        client.send(message);
		} else {
			throw new IllegalStateException("WebSocket is null");
		}
    }

    @Override
    public void sendMessage(@NonNull final  String message, BigInteger session_id) {
        sendMessage(message);
    }

    @Override
    public void sendMessage(@NonNull final  String message, BigInteger session_id, BigInteger handle_id) {
        sendMessage(message);
    }

    @Override
    public void receivedMessage(@NonNull final  String msg) {
		if (DEBUG) Log.v(TAG, "receivedMessage:" + msg);
        try {
            JSONObject obj = new JSONObject(msg);
            handler.receivedNewMessage(obj);
        } catch (Exception ex) {
            handler.onError(ex);
        }
    }

    private void onMessage(final String message) {
        if (DEBUG) Log.d(TAG, "Recv: \n\t" + message);
        receivedMessage(message);
    }

    private void onClose(final int code, final String reason, final boolean remote) {
		if (DEBUG) Log.v(TAG, "onClose:");
        handler.onClose();
    }

    private void onError(@NonNull final  Exception ex) {
		if (DEBUG) Log.v(TAG, "onError:" + ex);
        handler.onError(ex);
    }

}
