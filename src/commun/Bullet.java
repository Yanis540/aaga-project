package commun;

import characteristics.Parameters;
import helpers.Coords;
import helpers.MathHelp;

public class Bullet extends Coords {

    public Bullet(Coords coords) {
        super(coords.getX(), coords.getY());
    }

    protected int offsetTurn = 0;
    protected double maxSpeed = Parameters.bulletVelocity;
    protected double radius = Parameters.bulletRadius;
    protected Double computedAngle = null;

    public void update() {
        offsetTurn++;
    }

    public boolean isValid() {
        double maxOffSet = Math.ceil(Parameters.bulletRange / Parameters.bulletVelocity);
        return offsetTurn < maxOffSet;
    }

    public boolean isTracked() {
        return this.computedAngle != null;
    }

    public double estimateTravelDistance() {
        return this.maxSpeed * this.offsetTurn;
    }

    public void computeAngle(Coords otherBullet) {
        this.computedAngle = MathHelp.normalizeAngle(this.angleTo(otherBullet));
    }

    @Override
    public String toString() {
        return "Bullet{" + "\n" +
                "  traveledDistance=" + this.estimateTravelDistance() +
                "\n, offsetTurn=" + offsetTurn +
                "\n, radius=" + radius +
                "\n, computedAngle=" + computedAngle +
                "\n, isValid=" + isValid() +
                "\n, Coords=" + super.toString() +
                "}\n";
    }
}
