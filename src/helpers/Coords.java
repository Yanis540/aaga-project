package helpers;

public class Coords implements java.io.Serializable {
  protected double x;
  protected double y;

  public Coords(double x, double y) {
    this.x = x;
    this.y = y;
  }
  public Coords(Coords other) {
    this.x = other.getX();
    this.y = other.getY();
  }

  public void setAs(Coords other) {
    this.x = other.getX();
    this.y = other.getY();
  }

  public boolean inRange(Coords other, double range) {
    return distanceTo(other) <= range;
  }

  public boolean isInRangeBetween(Coords other, double range,
                                  double threasold) {
    double distance = distanceTo(other);
    double minRange = range - threasold;
    double maxRange = range + threasold;
    return distance >= minRange && distance <= maxRange;
  }

  public boolean isValid() { return x > 100 && x < 2900 && y > 100 && y < 1900; }

  public double getX() { return x; }

  public double getY() { return y; }

  public double distanceTo(Coords other) {
    return Math.sqrt(Math.pow(other.getX() - x, 2) +
                     Math.pow(other.getY() - y, 2));
  }

  public double angleTo(Coords other) {
    return MathHelp.normalizeAngle(
        Math.atan2(other.getY() - y, other.getX() - x));
  }

  public void add(Coords other) {
    x += other.getX();
    y += other.getY();
  }

  public void add(double angle, double speed) {
    x += speed * Math.cos(angle);
    y += speed * Math.sin(angle);
  }

  public void multiply(double factor) {
    x *= factor;
    y *= factor;
  }

  public void substract(Coords other) {
    x -= other.getX();
    y -= other.getY();
  }

  public void incrX(double x) { this.x += x; }

  public void incrY(double y) { this.y += y; }

  @Override
  public String toString() {
    return "Coords [x=" + x + ", y=" + y + "]";
  }
}
