package org.appspot.apprtc;

import android.support.annotation.Nullable;
import android.util.Log;

import org.webrtc.VideoFrame;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoSink;

public class ProxyVideoSink implements VideoSink, VideoRenderer.Callbacks {
	private static final boolean DEBUG = true;	// set false on  production
	private static final String TAG = ProxyVideoSink.class.getSimpleName();

	private Object target;

	@Override
	public synchronized void onFrame(final VideoFrame frame) {
		if (!(target instanceof VideoSink)) {
      			if (DEBUG) Log.d(TAG, "Dropping frame in proxy because target is null.");
			return;
		}

		((VideoSink)target).onFrame(frame);
	}

	public synchronized void setTarget(@Nullable final Object target) {
		if ((target == null)
			|| (target instanceof VideoSink)
			|| (target instanceof VideoRenderer.Callbacks)) {

			this.target = target;
		}
	}
	
	@Override
	public synchronized void renderFrame(final VideoRenderer.I420Frame frame) {
		if (!(target instanceof VideoRenderer.Callbacks)) {
      			if (DEBUG) Log.d(TAG, "Dropping frame in proxy because target is null.");
			return;
		}

		((VideoRenderer.Callbacks)target).renderFrame(frame);
	}
}
