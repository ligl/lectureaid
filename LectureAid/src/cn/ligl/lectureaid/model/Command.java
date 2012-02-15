package cn.ligl.lectureaid.model;

import org.json.JSONObject;

public abstract class Command {
	protected int mId;
	protected String mModule;
	protected String mName;
	protected String mOptions;
	protected String mArguments;

	public JSONObject toJSONObject() {
		// TODO translate
		return null;
	}

	String generalExceptionMessage() {
		StringBuilder msgSb = new StringBuilder();
		msgSb.append("command id: ").append(mId).append("\r\n")
				.append("name: ").append(mName).append("\r\n")
				.append("options: ").append(mOptions).append("\r\n")
				.append("arguments: ").append(mArguments);
		return msgSb.toString();
	}
}
