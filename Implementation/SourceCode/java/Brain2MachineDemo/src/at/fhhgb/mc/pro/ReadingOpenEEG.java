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
import at.fhhgb.mc.pro.gesture.SlopeGesture;
import at.fhhgb.mc.pro.gesture.SlopeGestureEvent;
import at.fhhgb.mc.pro.gesture.SlopeGestureEventListener;
import at.fhhgb.mc.pro.reader.OpenEEGReader;
import lejos.remote.ev3.RMIRegulatedMotor;
import lejos.remote.ev3.RemoteEV3;

public class ReadingOpenEEG {
	
	private static long timeMouth = -1;
	private static boolean preventLook = false;
	private static boolean isBiting = false;
	
	private static Timer timer = new Timer();
	private static TimerTask task = null;
	
	private static Thread backgroundThread = null;
	private static boolean backroundThreadRunning = false;
	private static BlockingQueue<Runnable> backgroundTasks = new LinkedBlockingQueue<>();
	
	private static RMIRegulatedMotor rotateMotor;
	private static RMIRegulatedMotor liftMotor;
	private static RMIRegulatedMotor grabMotor;
	
	private static LookGestureDirection lastDirection = null;
	private static boolean isLifting = false;
	private static boolean switchLift = false;
	
	public static void main(String[] _args) throws RemoteException, MalformedURLException, NotBoundException, InterruptedException {		
		RemoteEV3 brick = new RemoteEV3("10.0.1.1");
		grabMotor = brick.createRegulatedMotor("C", 'M');
		grabMotor.setSpeed(70);
		rotateMotor = brick.createRegulatedMotor("A", 'L');
		rotateMotor.setSpeed(15);
		liftMotor = brick.createRegulatedMotor("D", 'L');
		
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
		
		OpenEEGReader reader = new OpenEEGReader("COM1");
		LookGesture lookGesture = new LookGesture();
		lookGesture.addGestureEventListener(new LookGestureEventListener() {
			@Override
			public void onLookTimeOut(LookGestureEvent _evt) {				
			}
			@Override
			public void onLook(LookGestureEvent _evt) {
				
				if (!isLifting && !isBiting && !preventLook && System.currentTimeMillis() - timeMouth > 250) {
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
						timer.schedule(task, 750);
					}
				}
			}
		});
		
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
				if (!switchLift) {
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
				switchLift = !switchLift;
				
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
		
		SlopeGesture mouthCloseGesture = new SlopeGesture(SlopeGesture.THRESHOLD_CH1_HIGH, SlopeGesture.THRESHOLD_CH1_LOW, SlopeGesture.THRESHOLD_CH2_HIGH, SlopeGesture.THRESHOLD_CH2_LOW, SlopeGesture.SLOPE_SECONDS, SlopeGesture.SAFE_OFFSET_SECONDS, SlopeGesture.SLOPE_MIN, SlopeGesture.SLOPE_MAX);
		mouthCloseGesture.addGestureEventListener(new SlopeGestureEventListener() {
			@Override
			public void onSlopeDetected(SlopeGestureEvent _evt) {
				if (!isBiting) {
					System.out.println("Mouth closed");
					timeMouth = System.currentTimeMillis();
					if (task != null) {
						task.cancel();
						task = null;
					}
				}
			}
		});
		
		/*FreqGesture smileFreqGesture = new FreqGesture(20, 128, 1, 0.6f, FreqGesture.THRESHOLD_SMILE_1_SEC_SAMPLES_CH1, FreqGesture.THRESHOLD_SMILE_1_SEC_SAMPLES_CH2);
		smileFreqGesture.addGestureEventListener(new FreqGestureEventListener() {
			@Override
			public void onFreqGestureEventStart(FreqGestureEvent _evt) {
				System.out.println("Start smile");
				isSmiling = true;
			}
			@Override
			public void onFreqGestureEventComplete(FreqGestureEvent _evt) {
				System.out.println("End smile");
				isSmiling = false;
			}
		});*/
		
		//reader.addGesture(mouthCloseGesture);
		reader.addGesture(lookGesture);
		reader.addGesture(preventLookGesture);
		reader.addGesture(biteFreqGesture);
		//reader.addGesture(smileFreqGesture);
		reader.connect();
		
		System.out.println("Exit");
		Scanner sc = new Scanner(System.in);
		sc.nextLine();
		
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