/* ******************************************************
 * Simovies - Eurobot 2015 Robomovies Simulator.
 * Copyright (C) 2014 <Binh-Minh.Bui-Xuan@ens-lyon.org>.
 * GPL version>=3 <http://www.gnu.org/licenses/>.
 * $Id: algorithms/Main.java 2014-10-19 buixuan.
 * ******************************************************/
package algorithms.home;

import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;
import characteristics.Parameters;
import commun.GroCervo;
import helpers.MathHelp;
import robotsimulator.Bot;
import robotsimulator.Brain;
import robotsimulator.SimulatorEngine;

import javax.swing.Timer;
import java.util.*;
import java.util.stream.Stream;

  

public class Main extends Brain {
    private static final double POS_PRECISION = 1;
    private static final double ANGLE_PRECISION = 0.001;
    private static final double BOT_RADIUS = 50;
    private static final double BULLET_RADIUS = 5;
    private static final double BOT_BULLET_RADIUS = BOT_RADIUS + BULLET_RADIUS;
    private static boolean isTeamA = true;
    private static int robotId = 0;
    private int myId = 0;

    public Main() {
        super();
        this.myId = robotId;
        robotId++;
    }

    public static boolean isMain(Integer id) { return id < 3; }

    private static final Random gen = new Random(1);

    private static abstract class Task {
        public Task(int priority, boolean persistent) {
            this.priority = priority;
            this.persistent = persistent;
        }
        public Task(int priority) {
            this(priority, false);
        }
        public final int priority;
        public final boolean persistent;

        abstract boolean step();
    }
    private static abstract class TimeTask extends Task {
        protected int turnsLeft;

        public TimeTask(int priority, int turnsLeft) {
            super(priority);
            this.turnsLeft = turnsLeft;
        }
        public abstract void execute();
        @Override
        boolean step() {
            if (turnsLeft <= 0) {
                return false;
            }
            turnsLeft--;
            execute();
            return true;
        }
    }

    private LinkedList<Task> tasks = new LinkedList<>();
    private void addTask(Task task) {
        int i = 0;
        for (var it = tasks.iterator(); it.hasNext(); i++) {
            var t = it.next();
            if (t.priority < task.priority) {
                tasks.add(i, task);
                return;
            }
        }
        tasks.add(task);
    }

    protected boolean isScout = false;
    private final String id = Integer.toString(gen.nextInt(36 * 36, 36 * 36 * 36), 36);
    private Coords myPos = Coords.ZERO;

    private double getSpeed() {
        return isScout ? Parameters.teamASecondaryBotSpeed : Parameters.teamAMainBotSpeed;
    }

    static boolean comparePosition(double a, double b) {
        return Math.abs(a - b) < POS_PRECISION;
    }

    static boolean compareAngles(double a, double b) {
        double value = Math.abs(a - b) % (Math.PI * 2);
        return value < ANGLE_PRECISION || value > Math.PI * 2 - ANGLE_PRECISION;
    }

    private static boolean angleIsBetween(double min, double max, double a) {
        return (min < max) == (a >= min && a <= max);
    }

    private static boolean angleIntersects(double mi1, double ma1, double mi2, double ma2) {
        return angleIsBetween(mi1, ma1, mi2) || angleIsBetween(mi1, ma1, ma2) ||
            angleIsBetween(mi2, ma2, mi1) || angleIsBetween(mi2, ma2, ma1);
    }

    private void setDelay(int delay) {
        try {
            var botField = Brain.class.getDeclaredField("bot");
            botField.setAccessible(true);
            var bot = botField.get(this);
            var engineField = Bot.class.getDeclaredField("engine");
            engineField.setAccessible(true);
            var engine = engineField.get(bot);
            var timerField = SimulatorEngine.class.getDeclaredField("gameClock");
            timerField.setAccessible(true);
            Timer timer = (Timer) timerField.get(engine);
            timer.setDelay(delay);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);  
        }
    }

    public void activate() {
        if (getHeading() == Parameters.WEST) {
            isTeamA = false;
        }
        setDelay(5);

        Boolean foundTeamA = null, foundUp = null;
        for (var o: detectRadar()) {
            if (compareAngles(o.getObjectDirection(), Parameters.NORTH)) {
                foundUp = true;
            }
            if (compareAngles(o.getObjectDirection(), Parameters.SOUTH)) {
                foundUp = false;
            }
            if (compareAngles(o.getObjectDirection(), Parameters.EAST)) {
                foundTeamA = !isScout;
            }
            if (compareAngles(o.getObjectDirection(), Parameters.WEST)) {
                foundTeamA = isScout;
            }
        }
        var prevFoundTeamA = foundTeamA;
        var msgs = getMessages();
        for (String message : msgs) {
            if (message.startsWith("team")) {
                String[] split = message.split(" ");
                foundTeamA = split[2].equals("A");
                break;
            }
        }

        if (foundTeamA == null || foundUp == null) {
            throw new RuntimeException("Could not locate itself");
        }
        broadcast("team " + id + " " + (foundTeamA ? "A" : "B"));

        if (isScout) {
            var coords = new Coords[] {
                new Coords(Parameters.teamASecondaryBot1InitX, Parameters.teamASecondaryBot1InitY),
                new Coords(Parameters.teamASecondaryBot2InitX, Parameters.teamASecondaryBot2InitY),
                new Coords(Parameters.teamBSecondaryBot1InitX, Parameters.teamBSecondaryBot1InitY),
                new Coords(Parameters.teamBSecondaryBot2InitX, Parameters.teamBSecondaryBot2InitY),
            };
            myPos = coords[(foundTeamA ? 0 : 2) + (foundUp ? 1 : 0)];
        } else if (prevFoundTeamA == null) {
            myPos = foundTeamA ?
                new Coords(Parameters.teamAMainBot2InitX, Parameters.teamAMainBot2InitY) :
                new Coords(Parameters.teamBMainBot2InitX, Parameters.teamBMainBot2InitY);
        } else {
            var coords = new Coords[] {
                new Coords(Parameters.teamAMainBot1InitX, Parameters.teamAMainBot1InitY),
                new Coords(Parameters.teamAMainBot3InitX, Parameters.teamAMainBot3InitY),
                new Coords(Parameters.teamBMainBot1InitX, Parameters.teamBMainBot1InitY),
                new Coords(Parameters.teamBMainBot3InitX, Parameters.teamBMainBot3InitY),
            };
            myPos = coords[(foundTeamA ? 0 : 2) + (foundUp ? 1 : 0)];
        }

        addTask(new Task(11_000_000, true) {
            @Override
            boolean step() {
                detectEnemies();
                return false;
            }
        });

        if (isScout) {
            addTask(foundUp == foundTeamA ?
                new TimeTask(10_000_002, 25) {
                    @Override
                    public void execute() {
                        stepTurn(Parameters.Direction.RIGHT);
                    }
                }
                :
                new TimeTask(10_000_002, 25) {
                    @Override
                    public void execute() {
                        stepTurn(Parameters.Direction.LEFT);
                    }
                }
            );
            addTask(new TimeTask(10_000_001, 141) {
                @Override
                public void execute() {
                    move();
                }
            });
            addTask(foundUp == foundTeamA ?
                new TimeTask(10_000_000, 25) {
                    @Override
                    public void execute() {
                        stepTurn(Parameters.Direction.LEFT);
                    }
                }
                :
                new TimeTask(10_000_000, 25) {
                    @Override
                    public void execute() {
                        stepTurn(Parameters.Direction.RIGHT);
                    }
                }
            );
        }

        if (isScout) addTask(new Task(9_000_000, true) {
            @Override
            boolean step() {
                var threat = getRelativeTargets()
                    .filter(t -> turn - t.turn < 10)
                    .filter(Target::enemy)
                    .filter(t -> t.coords().norm() < 400)
                    .min(Comparator.comparingDouble(t -> t.coords().norm())).orElse(null);

                if (threat != null) {
                    var task = new TimeTask(priority+1, 70) {
                        @Override
                        public void execute() {
                            sendLogMessage(id + " retreating from " + threat.coords());
                            moveBack();
                        }
                    };
                    addTask(task);
                    task.step();
                    return true;
                }
                return false;
            }
        });
        if (!isScout) addTask(new Task(9_000_000, true) {
            @Override
            boolean step() {
                var targetClear = getRelativeTargets()
                    .filter(t -> turn - t.turn < 200)
                    .filter(Target::enemy)
                    .filter(t -> t.coords().norm() < Parameters.bulletRange - BOT_RADIUS)
                    .filter(t -> tryFindAngle(Coords.ZERO, t) != null)
                    .min(Comparator.comparingDouble(t -> t.coords().norm())).orElse(null);

                if (targetClear != null) {
                    var nearestTarget = getRelativeTargets()
                        .filter(t -> turn - t.turn < 200)
                        .filter(Target::enemy)
                        .min(Comparator.comparingDouble(t -> t.coords().norm()))
                        .orElse(null);

                    if (nearestTarget != null && nearestTarget.coords().norm() < targetClear.coords().norm()) {
                        targetClear = nearestTarget; // Change de cible si une plus proche est trouvée
                    }

                    Coords t = tryFindAngle(Coords.ZERO, targetClear);
                    if (t != null) {
                        sendLogMessage(id + " firing at " + t);
                        if (tryFire(t.angle())) return true;
                        return false;
                    }
                }
                return false;
            }
        });

        if (!isScout) addTask(new Task(7_500_000, true) {
            @Override
            boolean step() {
                double angle = getHeading(), coeff = 0.2;
                double minAngle = normalizeAngle(angle - 0.2 * coeff), maxAngle = normalizeAngle(angle + 0.5 * coeff);
                if (getRelativeTargets().filter(t -> turn - t.turn < 10).filter(Target::friendly).anyMatch(t -> {
                    if (t.coords.norm() < 99) return false;
                    double tAngle = t.coords().angle();
                    double delta = Math.atan(BOT_RADIUS / t.coords().norm());
                    double min = normalizeAngle(tAngle - delta), max = normalizeAngle(tAngle + delta);
                    return angleIntersects(minAngle, maxAngle, min, max);
                })) {
                    return false;
                }
                if (tryFire(angle + (Math.random() - 0.5) * coeff)) return true;
                return false;
            }
        });

        addTask(new Task(7_000_000, true) {

            @Override
            boolean step() {
                var foundTarget = getRelativeTargets()
                    .filter(t -> turn - t.turn < 1000)
                    .filter(Target::enemy)
                    .map(Target::coords)
                    .min(Comparator.comparingDouble(Coords::norm)).orElse(null);

                var lastAngle = getHeading();
                if (foundTarget != null) lastAngle = foundTarget.angle();

                /*boolean tryingToFire = !isScout && target.norm() < Parameters.bulletRange;
                var obstruction = getRelativeTargets().filter(
                    o -> !o.enemy() &&
                        !o.coords().equals(Coords.ZERO) &&
                        distanceToLine(Coords.ZERO, target, o.coords()) < (tryingToFire ? (BOT_BULLET_RADIUS + 1) : (2 * BOT_RADIUS + 1))
                ).findAny().orElse(null);*/
                if (findObstacle(true)) {
                    addTask(new Task(priority + 1) {
                        private double leftToGo = 0;
                        private int leftToTurn = -2;
                        @Override
                        boolean step() {
                            if (leftToTurn >= 0) {
                                stepTurn(Parameters.Direction.RIGHT);
                                leftToTurn--;
                                return true;
                            }
                            if (findObstacle(true)) {
                                sendLogMessage(id + " turning to avoid obstacle");
                                stepTurn(Parameters.Direction.RIGHT);
                                if (leftToTurn == -2) leftToTurn = 20;
                                leftToGo = 120;
                                return true;
                            }
                            leftToTurn = -2;
                            if (leftToGo > 0) {
                                sendLogMessage(id + " avoiding obstacle");
                                move();
                                leftToGo -= getSpeed();
                                return true;
                            }
                            return false;
                        }
                    });
                    return true;
                }
                if (!isScout) {
                    if (turnUntil(lastAngle)) {
                        sendLogMessage(id + " turning");
                        return true;
                    }
                }
                sendLogMessage(id + " moving");
                move();
                return true;
            }
        });
    }

    private final ArrayList<String> messages = new ArrayList<>();

    private ArrayList<String> getMessages() {
        messages.addAll(fetchAllMessages());
        return messages;
    }

    private void discardMessages() {
        messages.clear();
    }

    public double getHeading() {
        return normalizeAngle(super.getHeading());
    }

    private boolean findObstacle(boolean forwards) {
        if (getHealth() <= 0) {
            return true;
        }
        if (forwards && detectFront().getObjectType() == IFrontSensorResult.Types.WALL ||
            detectRadar().stream()
            .filter(t -> t.getObjectType() != IRadarResult.Types.BULLET)
            .map(t -> Coords.fromPolar(t.getObjectDirection(), t.getObjectDistance()))
            .anyMatch(t ->
                t.norm() > 49 &&
                Coords.fromPolar(getHeading(), 1).dot(t) > 0 == forwards &&
                t.norm() <= 105
            )) {
            return true;
        }
        if (myPos.x() < 55 && forwards == (getHeading() > Math.PI / 2 || getHeading() < -Math.PI / 2)) {
            return true;
        }
        if (myPos.x() > 2945 && forwards == (getHeading() < Math.PI / 2 && getHeading() > -Math.PI / 2)) {
            return true;
        }
        if (myPos.y() < 55 && forwards == (getHeading() < 0)) {
            return true;
        }
        if (myPos.y() > 1945 && forwards == (getHeading() > 0)) {
            return true;
        }
        return false;
    }

    private void tryMove(boolean forwards) {
        if (findObstacle(forwards)) {
            return;
        }

        if (forwards) super.move(); else super.moveBack();
        myPos = myPos.plus(Coords.fromPolar(getHeading(), forwards ? getSpeed() : -getSpeed()));
    }

    public void move() {
        tryMove(true);
    }

    public void moveBack() {
        tryMove(false);
    }

    private boolean typeIsOpponent(IRadarResult.Types type) {
        return type == IRadarResult.Types.OpponentMainBot || type == IRadarResult.Types.OpponentSecondaryBot;
    }

    private double distanceToLine(Coords a, Coords b, Coords x) {
        Coords ab = b.minus(a);
        Coords ax = x.minus(a);
        if (ab.dot(ax) < 0) return ax.norm();
        Coords projection = ax.projectOnto(ab);
        if (projection.norm() > ab.norm()) return x.minus(b).norm();
        return ax.minus(projection).norm();
    }

    private double positiveMod(double a, double b) {
        return (a % b + b) % b;
    }

    private double normalizeAngle(double angle) {
        return positiveMod(angle + Math.PI, Math.PI * 2) - Math.PI;
    }

    record Target(Coords coords, int turn, Coords prevCoords, int prevTurn, IRadarResult.Types type) {
        boolean equals(Target other) {
            return coords.minus(other.coords).norm() < 100 && type == other.type;
        }
        boolean enemy() {
            return type == IRadarResult.Types.OpponentMainBot || type == IRadarResult.Types.OpponentSecondaryBot;
        }
        boolean friendly() {
            return type == IRadarResult.Types.TeamMainBot || type == IRadarResult.Types.TeamSecondaryBot;
        }
        boolean bullet() {
            return type == IRadarResult.Types.BULLET;
        }
        boolean notWreck() {
            return type != IRadarResult.Types.Wreck;
        }
        Coords coordsAtTurn(int t) {
            if (prevCoords == null) return coords;
            return prevCoords.plus(coords.minus(prevCoords).times((t - prevTurn) / (double) (turn - prevTurn)));
        }
        Target relativeTo(Coords pos) {
            return new Target(coords.minus(pos), turn, prevCoords == null ? null : prevCoords.minus(pos), prevTurn, type);
        }
    }
  
    private static class TargetSet extends ArrayList<Target> implements Set<Target> {
        @Override
        public boolean add(Target target) {
            if (contains(target)) return false;
            return super.add(target);
        }
        public void add(Coords coords, int turn, IRadarResult.Types type) {
            int found = -1;
            for (int i = 0; i < size(); i++) {
                if (get(i).coords.minus(coords).norm() < 100) {
                    found = i;
                    break;
                }
            }
            if (found == -1) {
                add(new Target(coords, turn, null, 0, type));
                return;
            }
            Target old = get(found);
            if (old.turn >= turn) return;
            Target newTarget = new Target(coords, turn, old.coords, old.turn, type);
            set(found, newTarget);
        }
    }
    private TargetSet absoluteTargets = new TargetSet();
    private Stream<Target> getRelativeTargets() {
        return absoluteTargets.stream().map(t -> t.relativeTo(myPos));
    }

    private void detectEnemies() {
        var foundTargets = new TargetSet();
        foundTargets.add(myPos, turn, isScout ? IRadarResult.Types.TeamSecondaryBot : IRadarResult.Types.TeamMainBot);

        for (IRadarResult r : detectRadar()) {
            Coords target = Coords.fromPolar(r.getObjectDirection(), r.getObjectDistance()).plus(myPos);
            foundTargets.add(target, turn, r.getObjectType());
        }
        StringBuilder targetsBroadcast = new StringBuilder("coords ").append(id);
        for (Target target : foundTargets) {
            targetsBroadcast
                .append(" ").append(target.coords.x())
                .append(";").append(target.coords.y())
                .append(";").append(target.type.name());
        }
        broadcast(targetsBroadcast.toString());
        for (var t: foundTargets) {
            absoluteTargets.add(t.coords, t.turn, t.type);
        }
        for (String message : getMessages()) {
            if (message.startsWith("coords")) {
                String[] split = message.split(" ");
                String teamId = split[1];
                if (id.equals(teamId)) continue;
                for (int i = 2; i < split.length; i++) {
                    String[] data = split[i].split(";");
                    double x = Double.parseDouble(data[0]), y = Double.parseDouble(data[1]);
                    var type = IRadarResult.Types.valueOf(data[2]);
                    absoluteTargets.add(new Coords(x, y), turn, type);
                }
            }
        }
        discardMessages();
    }

    private boolean turnUntil(double angle) {
        angle = normalizeAngle(angle - getHeading());
        if (Math.abs(angle) > ANGLE_PRECISION * 50) {
            stepTurn(angle < 0 ? Parameters.Direction.LEFT : Parameters.Direction.RIGHT);
            return true;
        }
        return false;
    }

    private int firedTurn = -20;
    private boolean tryFire(double direction) {
        if (turn - firedTurn < 21) return false;
        fire(direction);
        firedTurn = turn;
        return true;
    }
 
    private Coords tryFindAngle(Coords from, Target to) {
        double time = to.coords.minus(from).norm() / Parameters.bulletVelocity;
        Coords futureCoords = to.coordsAtTurn(turn + (int) Math.ceil(time));
        Coords vector = futureCoords.minus(from);
        double center = vector.angle();
        double maxDeviation = Math.atan((BOT_BULLET_RADIUS - 1) / vector.norm());
        Coords leftVector = Coords.fromPolar(center - maxDeviation, vector.norm());
        Coords rightVector = Coords.fromPolar(center + maxDeviation, vector.norm());
        var options = new Coords[] { vector, leftVector, rightVector };
        for (var option: options) {
            if (
                absoluteTargets.stream().map(t -> t.relativeTo(myPos)).allMatch(
                    o -> o.enemy() || o.bullet() || o.coords().equals(Coords.ZERO) ||
                        distanceToLine(Coords.ZERO, option, o.coords()) >= (BOT_BULLET_RADIUS + 1)
                )
            ) {
                return option;
            }
        }
        return null;
    }

    private int turn = 0;
    public void step() {
        turn++;

        for (var it = tasks.iterator(); it.hasNext();) {
            var t = it.next();
            if (t.step()) {
                return;
            }
            if (!t.persistent) it.remove();
        }

       System.out.println("Dont have anything to do !");
    }
}

record Coords(double x, double y) {
    public Coords {
        if (Double.isNaN(x) || Double.isNaN(y)) throw new IllegalArgumentException();
    }
    
    public double getX() { return x; }

    public double getY() { return y; }

    public Coords plus(Coords other) {
        return new Coords(x + other.x, y + other.y);
    }

    public Coords minus(Coords other) {
        return new Coords(x - other.x, y - other.y);
    }

    public double norm() {
        return Math.hypot(x, y);
    }

    public double norm2() {
        return x * x + y * y;
    }

    public double angle() {
        return Math.atan2(y, x);
    }

    public Coords normalize() {
        double n = norm();
        return new Coords(x / n, y / n);
    }

    public Coords times(double factor) {
        return new Coords(x * factor, y * factor);
    }

    public static Coords fromPolar(double angle, double distance) {
        return new Coords(distance * Math.cos(angle), distance * Math.sin(angle));
    }

    public double dot(Coords other) {
        return x * other.x + y * other.y;
    }

    public Coords projectOnto(Coords other) {
        return other.times(dot(other) / other.norm2());
    }

    public Coords turn(double angle) {
        return fromPolar(angle() + angle, norm());
    }

    public static final Coords ZERO = new Coords(0, 0);

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Coords coords = (Coords) o;

        if (!Main.comparePosition(x, coords.x))return false;
        return Main.comparePosition(y, coords.y);
    }
    public boolean inRange(Coords other, double range) {
        return distanceTo(other) <= range;
      }
      public double distanceTo(Coords other) {
        return Math.sqrt(Math.pow(other.getX() - x, 2) +
                         Math.pow(other.getY() - y, 2));
      }
    

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(x);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(y);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "(" + ((long) x) + ", " + ((long) y) + ")";
    }
}