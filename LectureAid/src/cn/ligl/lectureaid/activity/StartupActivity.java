package cn.ligl.lectureaid.activity;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Process;
import android.preference.PreferenceManager;
import cn.ligl.lectureaid.R;
import cn.ligl.lectureaid.service.XMPPConnectionService;
import cn.ligl.lectureaid.util.Constant;

public class StartupActivity extends Activity {
	SharedPreferences mSharedPreferences;
	Thread mWorkThread;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_startup);
		// TODO settings startup animation and start XMPP service
		mWorkThread = new Thread(mRunnable);
		mWorkThread.start();
	}

	private Runnable mRunnable = new Runnable() {

		@Override
		public void run() {
			Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
			// start XMPP service
			startService(new Intent(StartupActivity.this,
					XMPPConnectionService.class));
			mSharedPreferences = PreferenceManager
					.getDefaultSharedPreferences(StartupActivity.this);
			String userName = mSharedPreferences.getString(
					Constant.PREFERENCES_LOGIN_NAME, null);
			if (userName != null) {
				String pwd = mSharedPreferences.getString(
						Constant.PREFERENCES_LOGIN_PWD, "");
				XMPPConnection con = XMPPConnectionService
						.getXMPPConnectionInstance();
				try {
					con.login(userName, pwd);
					// entrance main if login success
					startActivity(new Intent(StartupActivity.this,
							MainActivity.class));
				} catch (XMPPException e) {
					// TODO deal exception
					e.printStackTrace();
				}
			}

		}
	};
}
