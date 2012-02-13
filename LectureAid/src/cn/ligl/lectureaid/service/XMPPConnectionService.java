package cn.ligl.lectureaid.service;

import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.MessageEventManager;
import org.jivesoftware.smackx.OfflineMessageManager;
import org.jivesoftware.smackx.packet.OfflineMessageInfo;
import org.jivesoftware.smackx.packet.OfflineMessageRequest;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import cn.ligl.lectureaid.R;
import cn.ligl.lectureaid.activity.LoginActivity;
import cn.ligl.lectureaid.util.Constant;
import cn.ligl.lectureaid.util.Utils;

public class XMPPConnectionService extends Service {
	private static final int DELAY = 10;
	private static final int PRESENCE_PERIOD = 5 * 60000;
	private XMPPConnection mConnection;
	private boolean mIsConnected;
	private PacketListener mBackgroundMsgPacketListener;
	private PacketFilter mToFilter;
	private ConnectionConfiguration mConnectionConfig;
	private boolean mIsThreadRunning;
	private long mCurrentThreadId;
	private Timer mPingTimer;

	public long getCurrentThreadId() {
		return mCurrentThreadId;
	}

	public synchronized void setCurrentThreadId(long mCurrentThreadId) {
		this.mCurrentThreadId = mCurrentThreadId;
	}

	public class XMPPConnectionBinder extends Binder {
		public XMPPConnectionService getService() {
			return XMPPConnectionService.this;
		}
	}

	@Override
	public void onCreate() {
		System.out.println("onCreate xmpp connection service");
		super.onCreate();
		mIsConnected = false;
		mIsThreadRunning = false;

		// TODO init variable
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		// TODO test
		Editor editor = preferences.edit();
		editor.putString(Constant.PREFERENCES_LOGIN_NAME, "like");
		editor.putString(Constant.PREFERENCES_LOGIN_PWD, "0");
		editor.commit();

		configureXMPP();

		IntentFilter connectivityFilter = new IntentFilter(
				ConnectivityManager.CONNECTIVITY_ACTION);
		registerReceiver(mConnectivityReceiver, connectivityFilter);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		System.out.println("onDestroy XMPPConnectionService");

		disablePingTimer();

		// set presence as unavailable
		if (mConnection != null && mConnection.isConnected()) {
			Presence presence = new Presence(Presence.Type.unavailable);
			mConnection.sendPacket(presence);
			mConnection.disconnect();
		}
		unregisterReceiver(mConnectivityReceiver);
		mConnection = null;
		mIsConnected = false;
		mIsThreadRunning = false;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		System.out.println("onStartCommand xmpp service mConnection "
				+ mConnection);
		if (mConnection == null) {
			connectionToServer();
		} else {
			System.out.println("mConnection isConnected "
					+ mConnection.isConnected());
			if (!mIsConnected) {
				connectionToServer();
			} else {
				addBackgroundPacketListener();
			}
		}
		return super.onStartCommand(intent, flags, startId);
	}

	private void configureXMPP() {
		SmackConfiguration.setKeepAliveInterval(PRESENCE_PERIOD);
		mConnectionConfig = new ConnectionConfiguration(
				Constant.XMPP_SERVER_URL, Constant.XMPP_SERVER_PORT,
				Constant.XMPP_SERVER_NAME);
		mConnectionConfig.setSendPresence(false);
		// mConnectionConfig.setDebuggerEnabled(true);
		mConnectionConfig.setTruststoreType("bks");
		mConnectionConfig.setTruststorePassword("pwd");
		mConnectionConfig.setTruststorePath("/system/etc/security/cacerts.bks");
		mConnectionConfig
				.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
		mConnectionConfig.setReconnectionAllowed(false);

		// Configure ProviderManager
		ProviderManager pm = ProviderManager.getInstance();
		// Offline Message Requests
		pm.addIQProvider("offline", "http://jabber.org/protocol/offline",
				new OfflineMessageRequest.Provider());
		// Offline Message Indicator
		pm.addExtensionProvider("offline",
				"http://jabber.org/protocol/offline",
				new OfflineMessageInfo.Provider());
	}

	public void connectionToServer() {
		System.out.println("connectionToServer " + mIsThreadRunning);
		if (!mIsThreadRunning) {
			if (Utils.checkNetwork(this)) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						long currentThreadId = Thread.currentThread().getId();
						setCurrentThreadId(currentThreadId);
						mIsThreadRunning = true;

						XMPPConnection connection = new XMPPConnection(
								mConnectionConfig);
						try {
							connection.connect();
							System.out
									.println(".........connection successed.........");
							setCurrentConnection(connection, currentThreadId);
						} catch (XMPPException ex) {
							mIsConnected = false;
							disablePingTimer();
							System.out.println("login exception ");
							ex.printStackTrace();
						}
						mIsThreadRunning = false;
					}
				}).start();
			}
		}
	}

	private synchronized void setCurrentConnection(XMPPConnection connection,
			long threadId) {
		if (threadId == mCurrentThreadId) {
			if (mConnection != null) {
				mConnection.disconnect();
				mConnection = null;
			}
			mConnection = connection;
			SharedPreferences preferences = PreferenceManager
					.getDefaultSharedPreferences(this);
			String userEmail = preferences.getString(
					Constant.PREFERENCES_LOGIN_NAME, "");
			String pwd = preferences.getString(Constant.PREFERENCES_LOGIN_PWD,
					"");
			try {
				mConnection.login(userEmail, pwd);
				sendConnectedBroadcast();
				System.out.println(userEmail + " is login ok.");
			} catch (Exception e) {
				e.printStackTrace();
				mConnection = null;
			}
		}
	}

	private void sendConnectedBroadcast() {
		setConnection();
		Intent broadcastIntent = new Intent(
				Constant.CONNECTION_ESTABLISHED_BROADCAST);
		sendBroadcast(broadcastIntent);
	}

	private final IBinder mBinder = new XMPPConnectionBinder();

	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		return super.onUnbind(intent);
	}

	private void setConnection() {
		mIsConnected = true;

		mBackgroundMsgPacketListener = new PacketListener() {
			@Override
			public void processPacket(Packet packet) {
				final Message message = (Message) packet;
				System.out.println("service got text : " + message.toXML());
				if (message.getBody() != null) {
					String subject = message.getSubject();
					String messageBody = message.getBody();
					// TODO action dependence of subject
					if (subject.equals(Constant.MESSAGE_NOTIFICATION_DELIVERED)) {

					} else if (subject
							.equals(Constant.MESSAGE_NOTIFICATION_DISPLAYED)) {
					} else if (!subject
							.equals(Constant.MESSAGE_NOTIFICATION_COMPOSING)
							&& !subject
									.equals(Constant.MESSAGE_NOTIFICATION_CANCELLED)) {

						if (message.getSubject().equals(
								Constant.XMPP_MESSAGE_TEXT)) {
							String fileName = message.getBody();
						} else {
							sendbroadcast(message);
						}
					}
				}
			}
		};

		mConnection.addConnectionListener(new ConnectionListener() {
			@Override
			public void reconnectionSuccessful() {
				mIsConnected = true;
				Intent broadcastIntent = new Intent(
						Constant.CONNECTION_ESTABLISHED_BROADCAST);
				sendBroadcast(broadcastIntent);
			}

			@Override
			public void reconnectionFailed(Exception arg0) {
				mIsConnected = false;
				disablePingTimer();
				System.out.println("reconnectionFailed");
			}

			@Override
			public void reconnectingIn(int arg0) {
				System.out.println("reconnectingIn");
			}

			@Override
			public void connectionClosedOnError(Exception exception) {
				System.out.println("connectionClosedOnError "
						+ exception.getMessage());
				exception.printStackTrace();
				mIsConnected = false;
				disablePingTimer();
				String exceptionMessage = exception.getMessage();
				if (exceptionMessage.equals("stream:error (conflict)")) {
					notifyConnectionConflict();
					System.out.println("CONNECTION_CONFLICT_BROADCAST");
					Intent broadcastIntent = new Intent(
							Constant.CONNECTION_CONFLICT_BROADCAST);
					sendBroadcast(broadcastIntent);
				} else {
					// send broadcast to chat activity to disable send button
					// connectionToServer();
					Intent broadcastIntent = new Intent(
							Constant.CONNECTION_CLOSED_BROADCAST);
					sendBroadcast(broadcastIntent);
				}
			}

			@Override
			public void connectionClosed() {
				System.out.println("connectionClosed ");
				mIsConnected = false;
				disablePingTimer();
				// send broadcast to chat activity to disable send button
				Intent broadcastIntent = new Intent(
						Constant.CONNECTION_CLOSED_BROADCAST);
				sendBroadcast(broadcastIntent);
			}
		});

		// TODO get offline message
		OfflineMessageManager offlineMessageManager = new OfflineMessageManager(
				mConnection);
		try {
			Iterator<Message> iterator = offlineMessageManager.getMessages();
			// ChatMessage lastOfflineMessage = null;
			// while (iterator.hasNext()) {
			// Message offlineMessage = iterator.next();
			// String subject = offlineMessage.getSubject();
			// if (!subject.equals(Constant.MESSAGE_NOTIFICATION_COMPOSING)
			// && !subject
			// .equals(Constant.MESSAGE_NOTIFICATION_CANCELLED)
			// && !subject
			// .equals(Constant.MESSAGE_NOTIFICATION_DISPLAYED)
			// && !subject
			// .equals(Constant.MESSAGE_NOTIFICATION_DELIVERED)) {
			// lastOfflineMessage = saveReceivedMessage(offlineMessage);
			// }
			// }
			// if (lastOfflineMessage != null) {
			// Intent broadcastIntent = new Intent(
			// Constant.NEW_MESSAGE_BROADCAST);
			// broadcastIntent.putExtra(Constant.INTENT_MESSAGE,
			// lastOfflineMessage);
			// sendOrderedBroadcast(broadcastIntent, null);
			// }
			// offlineMessageManager.deleteMessages();
		} catch (XMPPException e) {
			e.printStackTrace();
		}

		// setup ping timer
		disablePingTimer();
		mPingTimer = new Timer();
		mPingTimer.schedule(new SendPingTask(), DELAY, PRESENCE_PERIOD);

		addBackgroundPacketListener();
	}

	public void send(Message msg) {
		MessageEventManager.addNotificationsRequests(msg, true, true, true,
				true);
		mConnection.sendPacket(msg);
	}

	private void disablePingTimer() {
		if (mPingTimer != null) {
			mPingTimer.cancel();
			mPingTimer = null;
		}
	}

	private void sendbroadcast(Message message) {
		Intent broadcastIntent = new Intent(
				Constant.INCOMMING_MESSAGE_BROADCAST);
		// TODO set message
		// broadcastIntent.putExtra(Constant.INTENT_MESSAGE, chatMessage);
		System.out.println("incomming message ....... " + message.getBody());
		sendOrderedBroadcast(broadcastIntent, null);
	}

	public boolean isConnected() {
		return mIsConnected;
	}

	public XMPPConnection getXmppConnection() {
		return mConnection;
	}

	public void addBackgroundPacketListener() {
		if (mBackgroundMsgPacketListener != null) {
			// Add a packet listener for incoming message
			mToFilter = new PacketFilter() {
				@Override
				public boolean accept(Packet arg0) {
					return true;
				}
			};

			mConnection.addPacketListener(mBackgroundMsgPacketListener,
					mToFilter);

		}
	}

	public void removeOfflinePacketListener() {
		if (mBackgroundMsgPacketListener != null && mIsConnected) {
			mConnection.removePacketListener(mBackgroundMsgPacketListener);
		}
	}

	private void notifyConnectionConflict() {
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification = new Notification(R.drawable.icon,
				getString(R.string.notification_connection_conflict),
				System.currentTimeMillis());
		notification.flags = Notification.DEFAULT_LIGHTS
				| Notification.FLAG_AUTO_CANCEL;

		Intent mainIntent = new Intent(this, LoginActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				mainIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		notification.setLatestEventInfo(this, getText(R.string.app_name),
				getText(R.string.notification_connection_conflict),
				contentIntent);
		notificationManager.notify(R.string.notification_connection_conflict,
				notification);
	}

	private BroadcastReceiver mConnectivityReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
				NetworkInfo networkInfo = (NetworkInfo) intent
						.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
				if (networkInfo != null) {
					if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
						if (networkInfo.getState().equals(
								NetworkInfo.State.SUSPENDED)) {
							mIsConnected = false;
							disablePingTimer();
							if (mConnection != null) {
								mConnection.disconnect();
								mConnection = null;
							}
						} else if (networkInfo.getState().equals(
								NetworkInfo.State.CONNECTED)) {
							if (!mIsConnected) {
								// connect to server
								connectionToServer();
							}
						}
					} else if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
						if (networkInfo.getState().equals(
								NetworkInfo.State.CONNECTED)) {
							if (!mIsConnected) {
								// connect to server
								connectionToServer();
							}
						}
					}
				}
			}
		}
	};

	private class SendPingTask extends TimerTask {
		@Override
		public void run() {
			System.out.println("send presence");
			if (mIsConnected) {
				Presence presence = new Presence(Presence.Type.available);
				mConnection.sendPacket(presence);
			}
		}
	}
}
