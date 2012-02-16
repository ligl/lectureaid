package cn.ligl.lectureaid.activity;

import java.util.HashMap;

import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import cn.ligl.lectureaid.R;
import cn.ligl.lectureaid.service.XMPPConnectionService;
import cn.ligl.lectureaid.util.Constant;

public class SigninActivity extends Activity {
	private EditText mUserNameEt;
	private EditText mPwdEt;
	private EditText mRePwdEt;
	private EditText mEmailEt;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_signin);
		mUserNameEt = (EditText) findViewById(R.id.et_signin_username);
		mPwdEt = (EditText) findViewById(R.id.et_signin_pwd);
		mRePwdEt = (EditText) findViewById(R.id.et_signin_repwd);
		mEmailEt = (EditText) findViewById(R.id.et_signin_email);
	}

	public void onSiginClick(View v) {
		// TODO change it to async
		boolean isSubmit = validate();
		if (isSubmit) {
			createAccount();
		}
	}

	private boolean validate() {
		// TODO validate information
		return true;
	}

	private void createAccount() {
		String userName = mUserNameEt.getText().toString().trim();
		String pwd = mPwdEt.getText().toString().trim();
		String email = mEmailEt.getText().toString().trim();
		XMPPConnection con = XMPPConnectionService.getXMPPConnectionInstance();
		AccountManager am = con.getAccountManager();
		HashMap<String, String> attrs = new HashMap<String, String>();
		attrs.put("email", email);
		try {
			am.createAccount(userName, pwd, attrs);
			con.login(userName, pwd);
			// TODO transfer to main
			finish();
		} catch (XMPPException e) {
			if (e.getXMPPError().getCode() == Constant.XMPP_ERROR_CODE_CONFLICT) {
				Toast.makeText(this, R.string.signin_toast_userexists,
						Toast.LENGTH_SHORT).show();
			}
			e.printStackTrace();
		}
	}
}
