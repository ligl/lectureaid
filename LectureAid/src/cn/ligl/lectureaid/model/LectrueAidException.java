package cn.ligl.lectureaid.model;

public class LectrueAidException extends Exception {

	private static final long serialVersionUID = 1L;

	public LectrueAidException() {
		super();
	}

	public LectrueAidException(String msg) {
		super(msg);
	}

	public LectrueAidException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public LectrueAidException(Throwable cause) {
		super(cause);
	}

}
