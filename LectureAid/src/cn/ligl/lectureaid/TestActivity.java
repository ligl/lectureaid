package cn.ligl.lectureaid;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import cx.hell.android.pdfview.ChooseFileActivity;

public class TestActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Button btn = new Button(this);
		btn.setText("PDF");
		btn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivity(new Intent(TestActivity.this, ChooseFileActivity.class));
			}
		});
		setContentView(btn);
	}

}
