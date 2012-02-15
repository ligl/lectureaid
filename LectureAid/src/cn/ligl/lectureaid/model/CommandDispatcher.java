package cn.ligl.lectureaid.model;

public class CommandDispatcher {
	private ICommandParser mICommandParser;
	private Command mCommand;
	private String mPattern;

	public void setPattern(String pattern) {
		mPattern = pattern;
	}

	public Command dispatch() throws LectrueAidException {
		// TODO parse pattern
		mCommand = mICommandParser.parse(mPattern);
		// return mCommand;
		throw new LectrueAidException("unkown command --- "
				+ mCommand.generalExceptionMessage());
	}
}
