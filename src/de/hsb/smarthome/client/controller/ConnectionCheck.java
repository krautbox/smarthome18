package de.hsb.smarthome.client.controller;

import java.io.IOException;
import java.net.InetAddress;
import de.hsb.smarthome.util.log.Logger;
import de.hsb.smarthome.util.log.Logger.LoggerMode;

public class ConnectionCheck {
	/**
	 * 
	 * @param ip - describes the connection target which is called with a ping
	 * @param ipCheckTimeout - ping timeout
	 * @param triggerInterval - interval to trigger the ping command 
	 * @param minutesTillShutdown - timeout in case that no answer from ping is received
	 * @param iConnectState - interface that gets the callback
	 */
	public ConnectionCheck(String ip, int ipCheckTimeout, int triggerInterval, int minutesTillShutdown,
			IConnectionState iConnectState) {
		this.mIP = ip;
		this.mIPCheckTimeout = ipCheckTimeout;
		this.mCommitSend = false;
		this.mStatus = Status.OK;
		this.mIConnectState = iConnectState;
		this.mShutdown = minutesTillShutdown * 60000000000l;
		this.mTrigger = new ConnectionCheckTrigger(triggerInterval, this);
		new Thread(mTrigger).start();
	}

	public int getTimeoutForCheck() {
		return mIPCheckTimeout;
	}

	public void setTimeoutForCheck(int mTimeoutForCheck) {
		this.mIPCheckTimeout = mTimeoutForCheck;
	}

	public synchronized boolean isCommitSend() {
		return mCommitSend;
	}

	public synchronized void setCommitSend(boolean mCommitSend) {
		this.mCommitSend = mCommitSend;
	}

	public String getIP() {
		return mIP;
	}

	/**
	 * These method sends a ping to the given IP. It will be triggerd by a internal Thread by the given interval.
	 */
	public synchronized void checkConnection() {
		boolean reachable = false;
		try {
			reachable = InetAddress.getByName(mIP).isReachable(mIPCheckTimeout);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (reachable) {
			this.mStatus = Status.OK;
			this.stateChanged(this.mStatus);
			this.mLogger.write(this, "Connection to " + this.mIP + " is now " + this.mStatus, LoggerMode.INFO);
			setCommitSend(false);
			this.mLastTimeStatusUnclear = -1;
		} else {
			if (this.mStatus == Status.UNCLEAR && (System.nanoTime() - this.mLastTimeStatusUnclear) > mShutdown) {
				this.mStatus = Status.LOST;
				this.stateChanged(this.mStatus);
				this.mLogger.write(this, "Connection to " + this.mIP + " is now " + this.mStatus, LoggerMode.ERROR);
				return;
			}
			if (mCommitSend) {
				mStatus = Status.UNCLEAR;
				this.stateChanged(this.mStatus);
				this.mLogger.write(this, "Connection to " + this.mIP + " is now " + this.mStatus, LoggerMode.WARN);
				if (this.mLastTimeStatusUnclear == -1) {
					this.mLastTimeStatusUnclear = System.nanoTime();
				}
			} else {
				mStatus = Status.LOST;
				this.stateChanged(this.mStatus);
				this.mLogger.write(this, "Connection to " + this.mIP + " is now " + this.mStatus, LoggerMode.ERROR);
			}
		}
	}

	/**
	 * state changed - including a null-pointer check
	 * @param status
	 */
	private void stateChanged(Status status) {
		if (this.mIConnectState != null) {
			this.mIConnectState.connectionStateChanged(this.mStatus);
		}
	}

	public enum Status {
		OK, UNCLEAR, LOST
	}

	/**
	 * ends the connection check
	 */
	public void stopConnectionCheck() {
		mTrigger.stopTrigger();
	}

	public interface IConnectionState {
		/**
		 * The connection check will verify the network connection in given intervals.
		 * If the state of the connection will change, these method will be called.
		 * 
		 * @param status
		 */
		void connectionStateChanged(Status status);
	}

	private int mIPCheckTimeout;
	private long mShutdown;
	private long mLastTimeStatusUnclear = -1l;
	private volatile boolean mCommitSend;

	private final String mIP;
	private Logger mLogger = Logger.getLogger();
	private volatile Status mStatus;
	private IConnectionState mIConnectState;
	private ConnectionCheckTrigger mTrigger;
}
