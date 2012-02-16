package cn.ligl.lectureaid.activity;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import cn.ligl.lectureaid.R;
import cn.ligl.lectureaid.service.XMPPConnectionService;

public class LoginActivity extends Activity {
	private EditText mUserNameEt;
	private EditText mPwdEt;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		mUserNameEt = (EditText) findViewById(R.id.et_login_username);
		mPwdEt = (EditText) findViewById(R.id.et_login_pwd);
	}

	public void onLoginClick(View v) {
		String userName = mUserNameEt.getText().toString().trim();
		String pwd = mPwdEt.getText().toString().trim();
		XMPPConnection con = XMPPConnectionService.getXMPPConnectionInstance();
		try {
			con.login(userName, pwd);
		} catch (XMPPException e) {
			// TODO deal exception
			e.printStackTrace();
		}
	}

	public void onSiginClick(View v) {
		startActivity(new Intent(this, SigninActivity.class));
	}

}
