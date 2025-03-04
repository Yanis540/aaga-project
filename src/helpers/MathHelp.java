package helpers;

import characteristics.Parameters;
import commun.State.Action;

public final class MathHelp {
  /* If the robots start turning weird edit this only */
  public static final double EPSILON = 0.01 * Math.PI;
  public static double getRandAngle() { return normalizeAngle(Math.random()); }
  private MathHelp() {}

  public static double angleDistance(double angle1, double angle2) {
    return normalizeAngle(angle1 - angle2);
  }

  public static boolean isClose(double value, double target) {
    return angleDistance(value, target) <= EPSILON;
  }

  public static Coords getLocation(double dist, double rad) {
    double x = dist * Math.cos(rad);
    double y = dist * Math.sin(rad);

    return new Coords(x, y);
  }

  public static double normalizeAngle(double angle) {
    return (angle % (2 * Math.PI) + 2 * Math.PI) % (2 * Math.PI);
  }

  public static boolean isNormalized(double angle) {
    return angle >= 0 && angle < 2 * Math.PI;
  }

  public static Action choseBestTurn(double rad, double targetRad) {
    if (isClose(rad, targetRad)) {
      return Action.NOTHING;
    }
    if (rad < targetRad) {
      if (targetRad - rad < Math.PI)
        return Action.RIGHT;
      return Action.LEFT;
    }

    if (rad - targetRad < Math.PI)
      return Action.LEFT;
    return Action.RIGHT;
  }

  public static boolean lineCrossCircle(Coords start, double heading,
                                        Coords center, double radius) {
    double a = Math.pow(Math.cos(heading), 2) + Math.pow(Math.sin(heading), 2);
    double b = 2 * (Math.cos(heading) * (start.getX() - center.getX()) +
                    Math.sin(heading) * (start.getY() - center.getY()));
    double c = Math.pow(start.getX() - center.getX(), 2) +
               Math.pow(start.getY() - center.getY(), 2) -
               Math.pow(radius + 5, 2);

    double delta = Math.pow(b, 2) - 4 * a * c;
    if (delta < 0)
      return false;
    double t1 = (-b + Math.sqrt(delta)) / (2 * a);
    double t2 = (-b - Math.sqrt(delta)) / (2 * a);
    return t1 >= 0 || t2 >= 0;
  }

  public static final double EAST = normalizeAngle(Parameters.EAST);
  public static final double WEST = normalizeAngle(Parameters.WEST);
  public static final double SOUTH = normalizeAngle(Parameters.SOUTH);
  public static final double NORTH = normalizeAngle(Parameters.NORTH);

  public static double addAngle(double angle, double add) {
    return normalizeAngle(angle + add);
  }
}
