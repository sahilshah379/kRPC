package com.krpc.math;

public class PIDController {
    private double kP;
    private double kI;
    private double kD;

    private double target;
    private double upperBound = 1;
    private double lowerBound = -1;

    private double P = 0;
    private double I = 0;
    private double D = 0;

    public PIDController(double kP, double kI, double kD, double target) {
        this.kP = kP;
        this.kI = kI;
        this.kD = kD;
        this.target = target;
    }
    public PIDController(double kP, double kI, double kD, double target, double upperBound, double lowerBound) {
        this.kP = kP;
        this.kI = kI;
        this.kD = kD;
        this.target = target;
        this.upperBound = upperBound;
        this.lowerBound = lowerBound;
    }
    public double getPIDOutput(double current, double deltaTime) {
        P = target - current;
        I += P * deltaTime;
        D = ((target - current) - P) / deltaTime;

        double output = kP * P + kI * I + kD * D;
        output = MathUtils.clamp(output, lowerBound, upperBound);
        return output;
    }

    public void setKP(double kP) {
        this.kP = kP;
    }

    public void setKI(double kI) {
        this.kI = kI;
    }

    public void setKD(double kD) {
        this.kD = kD;
    }
    public void setTarget(double target) {
        this.target = target;
    }
}