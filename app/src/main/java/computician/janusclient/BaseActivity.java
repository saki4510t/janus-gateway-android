package computician.janusclient;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.serenegiant.dialog.MessageDialogFragment;
import com.serenegiant.utils.BuildCheck;
import com.serenegiant.utils.PermissionCheck;

/**
 * created by t_saki t_saki@serenegiant.com 2018/04/22
 */
public abstract class BaseActivity extends Activity
	implements MessageDialogFragment.MessageDialogListener {

	private static final boolean DEBUG = true;	// set false on  production
	private static final String TAG = BaseActivity.class.getSimpleName();

	static int ID_PERMISSION_REASON_AUDIO = R.string.permission_audio_reason;
	static int ID_PERMISSION_REQUEST_AUDIO = R.string.permission_audio_request;
	static int ID_PERMISSION_REASON_NETWORK = R.string.permission_network_reason;
	static int ID_PERMISSION_REQUEST_NETWORK = R.string.permission_network_request;
	static int ID_PERMISSION_REASON_EXT_STORAGE = R.string.permission_ext_storage_reason;
	static int ID_PERMISSION_REQUEST_EXT_STORAGE = R.string.permission_ext_storage_request;
	static int ID_PERMISSION_REASON_CAMERA = R.string.permission_camera_reason;
	static int ID_PERMISSION_REQUEST_CAMERA = R.string.permission_camera_request;

//================================================================================
	/**
	 * MessageDialogFragmentメッセージダイアログからのコールバックリスナー
	 * @param dialog
	 * @param requestCode
	 * @param permissions
	 * @param result
	 */
	@SuppressLint("NewApi")
	@Override
	public void onMessageDialogResult(final MessageDialogFragment dialog,
		final int requestCode, final String[] permissions, final boolean result) {

		if (result) {
			// メッセージダイアログでOKを押された時はパーミッション要求する
			if (BuildCheck.isMarshmallow()) {
				requestPermissions(permissions, requestCode);
				return;
			}
		}
		// メッセージダイアログでキャンセルされた時とAndroid6でない時は自前でチェックして#checkPermissionResultを呼び出す
		for (final String permission: permissions) {
			checkPermissionResult(requestCode, permission,
				PermissionCheck.hasPermission(this, permission));
		}
	}

	/**
	 * パーミッション要求結果を受け取るためのメソッド
	 * @param requestCode
	 * @param permissions
	 * @param grantResults
	 */
	@Override
	public void onRequestPermissionsResult(final int requestCode,
		@NonNull final String[] permissions, @NonNull final int[] grantResults) {

		super.onRequestPermissionsResult(requestCode, permissions, grantResults);	// 何もしてないけど一応呼んどく
		final int n = Math.min(permissions.length, grantResults.length);
		for (int i = 0; i < n; i++) {
			checkPermissionResult(requestCode, permissions[i],
				grantResults[i] == PackageManager.PERMISSION_GRANTED);
		}
	}

	/**
	 * パーミッション要求の結果をチェック
	 * ここではパーミッションを取得できなかった時にToastでメッセージ表示するだけ
	 * @param requestCode
	 * @param permission
	 * @param result
	 */
	protected void checkPermissionResult(final int requestCode,
		final String permission, final boolean result) {

		// パーミッションがないときにはメッセージを表示する
		if (!result && (permission != null)) {
			if (android.Manifest.permission.RECORD_AUDIO.equals(permission)) {
				Toast.makeText(getApplicationContext(),
					R.string.permission_audio, Toast.LENGTH_SHORT).show();
			}
			if (android.Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permission)) {
				Toast.makeText(getApplicationContext(),
					R.string.permission_ext_storage, Toast.LENGTH_SHORT).show();
			}
			if (android.Manifest.permission.CAMERA.equals(permission)) {
				Toast.makeText(getApplicationContext(),
					R.string.permission_camera, Toast.LENGTH_SHORT).show();
			}
			if (android.Manifest.permission.INTERNET.equals(permission)) {
				Toast.makeText(getApplicationContext(),
					R.string.permission_network, Toast.LENGTH_SHORT).show();
			}
		}
	}

	protected static final int REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 0x12345;
	protected static final int REQUEST_PERMISSION_AUDIO_RECORDING = 0x234567;
	protected static final int REQUEST_PERMISSION_CAMERA = 0x345678;
	protected static final int REQUEST_PERMISSION_NETWORK = 0x456789;

	/**
	 * 外部ストレージへの書き込みパーミッションが有るかどうかをチェック
	 * なければ説明ダイアログを表示する
	 * @return true 外部ストレージへの書き込みパーミッションが有る
	 */
	protected boolean checkPermissionWriteExternalStorage() {
		if (!PermissionCheck.hasWriteExternalStorage(this)) {
			MessageDialogFragment.showDialog(this, REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE,
				R.string.permission_title, ID_PERMISSION_REQUEST_EXT_STORAGE,
				new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE});
			return false;
		}
		return true;
	}

	/**
	 * 録音のパーミッションが有るかどうかをチェック
	 * なければ説明ダイアログを表示する
	 * @return true 録音のパーミッションが有る
	 */
	protected boolean checkPermissionAudio() {
		if (!PermissionCheck.hasAudio(this)) {
			MessageDialogFragment.showDialog(this, REQUEST_PERMISSION_AUDIO_RECORDING,
				R.string.permission_title, ID_PERMISSION_REQUEST_AUDIO,
				new String[]{android.Manifest.permission.RECORD_AUDIO});
			return false;
		}
		return true;
	}

	/**
	 * カメラアクセスのパーミッションが有るかどうかをチェック
	 * なければ説明ダイアログを表示する
	 * @return true カメラアクセスのパーミッションがある
	 */
	protected boolean checkPermissionCamera() {
		if (!PermissionCheck.hasCamera(this)) {
			MessageDialogFragment.showDialog(this, REQUEST_PERMISSION_CAMERA,
				R.string.permission_title, ID_PERMISSION_REQUEST_CAMERA,
				new String[]{android.Manifest.permission.CAMERA});
			return false;
		}
		return true;
	}

	/**
	 * ネットワークアクセスのパーミッションが有るかどうかをチェック
	 * なければ説明ダイアログを表示する
	 * @return true ネットワークアクセスのパーミッションが有る
	 */
	protected boolean checkPermissionNetwork() {
		if (!PermissionCheck.hasNetwork(this)) {
			MessageDialogFragment.showDialog(this, REQUEST_PERMISSION_NETWORK,
				R.string.permission_title, ID_PERMISSION_REQUEST_NETWORK,
				new String[]{android.Manifest.permission.INTERNET});
			return false;
		}
		return true;
	}

//================================================================================
	/**
	 * Get a value from the shared preference, if it does not
	 * exist the default is used.
	 * @param sharedPref
	 * @param attributeId
	 * @param defaultId
	 */
	protected String sharedPrefGetString(final SharedPreferences sharedPref,
		@StringRes final int attributeId, @StringRes final int defaultId) {

		return sharedPref.getString(getString(attributeId), getString(defaultId));
	}
	
	/**
	 * Get a value from the shared preference, if it does not
	 * exist the default is used.
	 * @param sharedPref
	 * @param attributeId
	 * @param defaultId
	 */
	protected boolean sharedPrefGetBoolean(final SharedPreferences sharedPref,
		@StringRes final int attributeId, @StringRes final int defaultId) {

		return sharedPref.getBoolean(getString(attributeId),
			Boolean.parseBoolean(getString(defaultId)));
	}
	
	/**
	 * Get a value from the shared preference, if it does not
	 * exist the default is used.
	 * @param sharedPref
	 * @param attributeId
	 * @param defaultId
	 */
	protected int sharedPrefGetInteger(final SharedPreferences sharedPref,
		@StringRes final int attributeId, @StringRes final int defaultId) {

		final String defaultString = getString(defaultId);
		final int defaultValue = Integer.parseInt(defaultString);
		final String attributeName = getString(attributeId);
		final String value = sharedPref.getString(attributeName, defaultString);
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			Log.e(TAG, "Wrong setting for: " + attributeName + ":" + value);
			return defaultValue;
		}
	}

//================================================================================
	@TargetApi(19)
	protected static int getSystemUiVisibility() {
		int flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			flags |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
		}
		return flags;
	}

}
