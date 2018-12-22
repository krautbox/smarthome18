package de.hsb.smarthome.client.controller;

public class ConnectionCheckTrigger implements Runnable {
  public ConnectionCheckTrigger(int triggerInterval, ConnectionCheck connectionCheck) {
    this.mtriggerInterval = triggerInterval;
    this.mConnectionCheck = connectionCheck;
    this.mRunning = true;
  }

  @Override
  public void run() {
    while (mRunning) {
      this.mConnectionCheck.checkConnection();
      try {
        Thread.sleep(mtriggerInterval);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
  
  /**
   * stops the trigger and ends the thread
   */
  public void stopTrigger() {
	  this.mRunning = false;
  }

  private ConnectionCheck mConnectionCheck;
  private int mtriggerInterval;
  private volatile boolean mRunning; 
}
