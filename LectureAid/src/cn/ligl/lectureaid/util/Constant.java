package cn.ligl.lectureaid.util;

public class Constant {
	// perference
	public static final String PREFERENCES_LOGIN_NAME = "username";
	public static final String PREFERENCES_LOGIN_PWD = "userpwd";

	// local server
	public static final String SERVER = "http://localhost:8080";
	// XMPP Server
	public static final String XMPP_SERVER_URL = "192.168.0.109";
	public static final int XMPP_SERVER_PORT = 5222;
	public static final String XMPP_SERVER_NAME = "lectureaid";
	public static final String XMPP_SERVER_DOMAIN_NAME = "@localhost";

	// XMPP message subject
	public static final String XMPP_MESSAGE_TEXT = "text";
	public static final String XMPP_MESSAGE_AUDIO = "audio";
	public static final String XMPP_MESSAGE_VIDEO = "video";
	public static final String XMPP_MESSAGE_PHOTO = "photo";

	// Message send state
	public static final int MESSAGE_SEND_STATE_OFFLINE = 1;
	public static final int MESSAGE_SEND_STATE_DELIVERED = 2;
	public static final int MESSAGE_SEND_STATE_DISPLAYED = 3;

	// message send state
	public static final String MESSAGE_NOTIFICATION_COMPOSING = "composing";
	public static final String MESSAGE_NOTIFICATION_CANCELLED = "cancelled";
	public static final String MESSAGE_NOTIFICATION_DELIVERED = "delivered";
	public static final String MESSAGE_NOTIFICATION_DISPLAYED = "displayed";

	// braodcast
	public static final String CONNECTION_ESTABLISHED_BROADCAST = "cn.ligl.lectureaid.connectionestablished";
	public static final String CONNECTION_CONFLICT_BROADCAST = "cn.ligl.lectureaid.accountconfilict";
	public static final String CONNECTION_CLOSED_BROADCAST = "cn.ligl.lectureaid.connectionclosed";
	public static final String INCOMMING_MESSAGE_BROADCAST = "cn.ligl.lectureaid.incommingmessage";
}
