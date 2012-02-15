package cn.ligl.lectureaid.model;

/**
 * Command parser that parse pattern string to command object dependent on
 * {@link InformationCarrier}
 * 
 * @author ligl
 * @email ligl95403@gmail.com
 */
public interface ICommandParser {
	public Command parse(String pattern);
}
