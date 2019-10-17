package com.krpc.rocket.threads;

import com.krpc.math.PIDController;
import com.krpc.rocket.Rocket;
import org.javatuples.Triplet;

@Deprecated
public class AttitudeThread extends Thread {
    final long time = 100;
    private PIDController pitchController;
    private PIDController yawController;
    private PIDController rollController;
    private Triplet<Double,Double,Double> target;
    private Rocket rocket;
    public AttitudeThread(Rocket rocket, Triplet<Double,Double,Double> target)  {
        this.target = target;

        this.pitchController = new PIDController(1,0,0, target.getValue0());
        this.yawController = new PIDController(1,0,0, target.getValue1());
        this.rollController = new PIDController(1,0,0, target.getValue2());

        this.rocket = rocket;
    }
    public void run() {
        while (true) {
            try {
                Triplet<Double,Double,Double> currentAngularVelocity = rocket.getAngularVelocity();
                System.out.println(currentAngularVelocity.toString());

//                Triplet<Double, Double, Double> current = rocket.getAttitude();
//                double pitchCorrection = pitchController.getPIDOutput(current.getValue0(),time/1000.0);
//                double yawCorrection = yawController.getPIDOutput(current.getValue1(),time/1000.0);
//                double rollCorrection = rollController.getPIDOutput(current.getValue2(), time / 1000.0);
//
//                rocket.getVessel().getControl().setPitch((float) pitchCorrection);
//                rocket.getVessel().getControl().setYaw((float) yawCorrection);
//                rocket.getVessel().getControl().setRoll((float) rollCorrection);

                Thread.sleep(time);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
