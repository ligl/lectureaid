package cn.ligl.lectureaid.util;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class Utils {
	public static String md5(String string) {
		java.security.MessageDigest messageDigest = null;
		try {
			messageDigest = java.security.MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		messageDigest.update(string.getBytes(), 0, string.length());
		return new BigInteger(1, messageDigest.digest()).toString(16);
	}

	public static String getTimeFromString(String text) {
		return android.text.format.DateFormat.format("yyyy-MM-dd kk:mm:ss",
				Long.valueOf(text)).toString();
	}

	public static boolean checkNetwork(Context context) {
		boolean result = false;
		ConnectivityManager connectivityManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
		if (networkInfo != null
				&& networkInfo.getState().equals(NetworkInfo.State.CONNECTED)) {
			result = true;
		}
		return result;
	}
}
