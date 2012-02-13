package cn.ligl.lectureaid.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import cn.ligl.lectureaid.service.XMPPConnectionService;

public class LoginActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle("login");
		startService(new Intent(this, XMPPConnectionService.class));
	}

}
