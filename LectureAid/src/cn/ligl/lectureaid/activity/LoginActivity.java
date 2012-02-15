package cn.ligl.lectureaid.activity;

import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import cn.ligl.lectureaid.service.XMPPConnectionService;
import cn.ligl.lectureaid.util.Constant;

public class LoginActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle("login");
		startService(new Intent(this, XMPPConnectionService.class));
	}

	private void createAccount() {
		String userName = "test";
		String pwd = "0";
		XMPPConnection con = XMPPConnectionService.getXMPPConnectionInstance();
		AccountManager am = con.getAccountManager();
		try {
			am.createAccount(userName, pwd);
		} catch (XMPPException e) {
			if (e.getXMPPError().getCode() == Constant.XMPP_ERROR_CODE_CONFLICT) {
				Toast.makeText(this, "conflict,please post another account",
						Toast.LENGTH_SHORT).show();
			}
			System.out.println("account already exists.............."
					+ e.getXMPPError().getCode());
			e.printStackTrace();
		}
	}

}
