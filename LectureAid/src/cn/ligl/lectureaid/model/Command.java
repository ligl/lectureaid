package cn.ligl.lectureaid.model;

import org.json.JSONObject;

public abstract class Command {
	protected String mModule;
	protected String mName;
	protected String mOptions;
	protected String mArguments;

	public JSONObject toJSONObject() {
		// TODO translate
		return null;
	}
}
