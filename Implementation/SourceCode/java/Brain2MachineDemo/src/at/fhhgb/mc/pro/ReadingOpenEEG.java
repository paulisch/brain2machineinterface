package at.fhhgb.mc.pro;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import at.fhhgb.mc.pro.gesture.FreqGesture;
import at.fhhgb.mc.pro.gesture.FreqGestureEvent;
import at.fhhgb.mc.pro.gesture.FreqGestureEventListener;
import at.fhhgb.mc.pro.gesture.LookGesture;
import at.fhhgb.mc.pro.gesture.LookGestureDirection;
import at.fhhgb.mc.pro.gesture.LookGestureEvent;
import at.fhhgb.mc.pro.gesture.LookGestureEventListener;
import at.fhhgb.mc.pro.reader.OpenEEGReader;
import lejos.remote.ev3.RMIRegulatedMotor;
import lejos.remote.ev3.RemoteEV3;

/**
 * The main class that initializes all gesture listeners and controls the Lego Mindstorms crane.
 * @author Boris Fuchs, Paul Schmutz
 */
public class ReadingOpenEEG {
	
	/**
	 * Indicates whether look gesture should be prevented (e. g. when biting).
	 */
	private static boolean preventLook = false;
	
	/**
	 * Indicates whether user is currently executing bite gesture.
	 */
	private static boolean isBiting = false;
	
	
	
	/**
	 * A timer to start the rotate motor after a certain period of time after a look gesture to wait a secure amount of time to cancel the rotate motor in case another gesture is being detected meanwhile.
	 */
	private static Timer timer = new Timer();
	
	/**
	 * The timer task being submitted to the timer that will control the rotate motor.
	 */
	private static TimerTask task = null;
	
	
	
	/**
	 * A background thread running the lifting tasks for the crane.
	 */
	private static Thread backgroundThread = null;
	
	/**
	 * Indicates whether background thread is running.
	 */
	private static boolean backroundThreadRunning = false;
	
	/**
	 * Queue that handles the lifting tasks.
	 */
	private static BlockingQueue<Runnable> backgroundTasks = new LinkedBlockingQueue<>();
	
	
	
	/**
	 * The motor to rotate the crane's arm.
	 */
	private static RMIRegulatedMotor rotateMotor;
	
	/**
	 * The motor lift the crane's arm.
	 */
	private static RMIRegulatedMotor liftMotor;
	
	/**
	 * The motor to grab or release an object with the crane.
	 */
	private static RMIRegulatedMotor grabMotor;
	
	
	
	/**
	 * Variable to remember the direction the user has last looked at.
	 */
	private static LookGestureDirection lastDirection = null;
	
	/**
	 * Indicates whether the crane is currently in a lifting task.
	 */
	private static boolean isLifting = false;
	
	/**
	 * Indicates whether an object is currently held and should be released; switches between grabbing and releasing an object.
	 */
	private static boolean shouldReleaseItem = false;
	
	/**
	 * The program's entry point.
	 * @param _args
	 * @throws RemoteException
	 * @throws MalformedURLException
	 * @throws NotBoundException
	 * @throws InterruptedException
	 */
	public static void main(String[] _args) throws RemoteException, MalformedURLException, NotBoundException, InterruptedException {
		//Prepare Lego motors
		RemoteEV3 brick = new RemoteEV3("10.0.1.1");
		grabMotor = brick.createRegulatedMotor("C", 'M');
		grabMotor.setSpeed(70);
		rotateMotor = brick.createRegulatedMotor("A", 'L');
		rotateMotor.setSpeed(15);
		liftMotor = brick.createRegulatedMotor("D", 'L');
		
		//Initialize background thread
		backgroundThread = new Thread() {			
			@Override
			public void run() {
				while (backroundThreadRunning) {
					try {
						Runnable r = backgroundTasks.take();
						r.run();
					} catch (InterruptedException e) {
						backroundThreadRunning = false;
					}
				}
				System.out.println("Background thread exited");
			}
		};
		backroundThreadRunning = true;
		backgroundThread.start();
		
		
		//Create EEG reader
		OpenEEGReader reader = new OpenEEGReader("COM1");
		
		//Look gesture
		LookGesture lookGesture = new LookGesture();
		lookGesture.addGestureEventListener(new LookGestureEventListener() {
			@Override
			public void onLook(LookGestureEvent _evt) {
				if (!isLifting && !isBiting && !preventLook) {
					if (task == null) {
						task = new TimerTask() {
							@Override
							public void run() {
								try {
								System.out.println("Look " + _evt.getDirection());
								if (lastDirection == null) {
									if (_evt.getDirection()==LookGestureDirection.LEFT) {
										rotateMotor.forward();
									} else { 
										rotateMotor.backward();
									}
									lastDirection = _evt.getDirection();
								}
								else {
									if (lastDirection != _evt.getDirection()) {
										rotateMotor.stop(true);
										lastDirection = null;
									}
								}
								
								} catch (RemoteException e) {
									e.printStackTrace();
								}
								task = null;
							}
						};
						timer.schedule(task, 600);
					}
				}
			}
		});
		
		//Prevent look gesture (when a strength of high frequencies is too high)
		FreqGesture preventLookGesture = new FreqGesture(20, 128, 1, 0.6f, FreqGesture.THRESHOLD_PREVENT_LOOK_1_SEC_SAMPLES_CH1, FreqGesture.THRESHOLD_PREVENT_LOOK_1_SEC_SAMPLES_CH2);
		preventLookGesture.addGestureEventListener(new FreqGestureEventListener() {
			@Override
			public void onFreqGestureEventStart(FreqGestureEvent _evt) {
				System.out.println("Prevent look start");
				preventLook = true;
				if (task != null) {
					task.cancel();
					task = null;
				}
			}
			@Override
			public void onFreqGestureEventComplete(FreqGestureEvent _evt) {
				System.out.println("Prevent look end");
				preventLook = false;
				if (task != null) {
					task.cancel();
					task = null;
				}
			}
		});
		
		//Bite gesture
		FreqGesture biteFreqGesture = new FreqGesture(20, 128, 1, 0.6f, FreqGesture.THRESHOLD_BITE_1_SEC_SAMPLES_CH1, FreqGesture.THRESHOLD_BITE_1_SEC_SAMPLES_CH2);
		biteFreqGesture.addGestureEventListener(new FreqGestureEventListener() {
			@Override
			public void onFreqGestureEventStart(FreqGestureEvent _evt) {
				System.out.println("Start bite");
				isBiting = true;
				if (task != null) {
					task.cancel();
					task = null;
				}
				
				if (isLifting) {
					return;
				}
				
				isLifting = true;
				
				Runnable r = null;
				if (!shouldReleaseItem) {
					r = new Runnable() {
						@Override
						public void run() {
							try {
								rotateMotor.stop(true);
								
								//Go down
								liftMotor.setSpeed(22);
								liftMotor.forward();
								Thread.sleep(4000);
								liftMotor.stop(true);
								
								//Grab item
								grabMotor.forward();
								Thread.sleep(2500);
								grabMotor.stop(true);
								
								//Go back up
								liftMotor.setSpeed(44);
								liftMotor.backward();
								Thread.sleep(2000);
								liftMotor.stop(true);
							} catch (Exception e) {
								try {
									grabMotor.stop(true);
								} catch (RemoteException e1) {
									e1.printStackTrace();
								}
								try {
									liftMotor.stop(true);
								} catch (RemoteException e1) {
									e1.printStackTrace();
								}
								e.printStackTrace();
							}
							isLifting = false;
						}
					};
				}
				else {
					r = new Runnable() {
						@Override
						public void run() {
							try {
								rotateMotor.stop(true);
								
								//Go down
								liftMotor.setSpeed(22);
								liftMotor.forward();
								Thread.sleep(4000);
								liftMotor.stop(true);
								
								//Release item
								grabMotor.backward();
								Thread.sleep(2500);
								grabMotor.stop(true);
								
								//Go back up
								liftMotor.setSpeed(44);
								liftMotor.backward();
								Thread.sleep(2000);
								liftMotor.stop(true);
							} catch (Exception e) {
								try {
									grabMotor.stop(true);
								} catch (RemoteException e1) {
									e1.printStackTrace();
								}
								try {
									liftMotor.stop(true);
								} catch (RemoteException e1) {
									e1.printStackTrace();
								}
								e.printStackTrace();
							}
							isLifting = false;
						}
					};
				}
				try {
					backgroundTasks.put(r);
				} catch (InterruptedException e) { }
				shouldReleaseItem = !shouldReleaseItem;
				
			}
			@Override
			public void onFreqGestureEventComplete(FreqGestureEvent _evt) {
				System.out.println("End bite");
				isBiting = false;
				if (task != null) {
					task.cancel();
					task = null;
				}
			}
		});
		
		//Add gestures and start reader
		reader.addGesture(lookGesture);
		reader.addGesture(preventLookGesture);
		reader.addGesture(biteFreqGesture);
		reader.connect();
		
		//Handle enter press to exit program
		System.out.println("Press ENTER to exit");
		Scanner sc = new Scanner(System.in);
		sc.nextLine();
		sc.close();
		
		//Stop background thread, close RMI motors and disconnect reader
		backroundThreadRunning = false;
		backgroundThread.interrupt();
		rotateMotor.close();
		liftMotor.close();
		grabMotor.close();
		reader.disconnect();
		Thread.sleep(3000);
		System.exit(0);
	}
}