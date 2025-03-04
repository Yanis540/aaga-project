package commun;

import helpers.Coords;

public class Env {
  private final Env before;
  private double headingTarget;
  private Coords targetPosition;

  public void setHeadingTarget(double headingTarget) {
    this.headingTarget = headingTarget;
  }

  public void setTargetPosition(Coords targetPosition) {
    this.targetPosition = targetPosition;
  }

  public double getHeadingTarget() { return headingTarget; }

  public Coords getTargetPosition() { return targetPosition; }

  public Env(Coords targetPosition, Env before) {
    this.headingTarget = 0;
    this.targetPosition = targetPosition;
    this.before = before;
  }

  public Env(double headingTarget, Env before) {
    this.headingTarget = headingTarget;
    this.targetPosition = null;
    this.before = before;
  }

  public Env(Coords targetPosition, double headingTarget, Env before) {
    this.headingTarget = headingTarget;
    this.targetPosition = targetPosition;
    this.before = before;
  }

  public Env getBefore() { return before; }
}