package commun;

import characteristics.IFrontSensorResult;
import characteristics.Parameters;
import characteristics.Parameters.Direction;
import commun.State.Action;
import events.LocalizedRadar;
import events.MyPos;
import events.Orders;
import events.Orders.OrdersType;
import helpers.Coords;
import helpers.MathHelp;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import robotsimulator.Brain;

public abstract class GroCervo extends Brain {
  private static int robotId = 0;

  protected GroCervo() {
    id = robotId;
    robotId = (robotId + 1) % 5;
  }

  private final int id;
  private BigInteger turn = BigInteger.ZERO;

  private void pushEnv(Env strategieEnv) { this.currentEnv = strategieEnv; }

  private void pushEnv(double headingTarget) {
    Env strategieEnv = new Env(headingTarget, currentEnv);
    pushEnv(strategieEnv);
  }

  private void pushEnv(Coords targetPosition) {
    Env strategieEnv = new Env(targetPosition, currentEnv);
    pushEnv(strategieEnv);
  }

  public Env popEnv() {
    Env current = this.currentEnv;
    currentEnv = currentEnv.getBefore();
    return current;
  }

  public boolean isEnvEmpty() { return currentEnv == null; }

  private Env currentEnv = null;
  protected State currentState = new State();
  private final GlobalData globalData = new GlobalData();
  private final List<Event> createdEvents = new ArrayList<>();
  private final List<Event> incomingEvents = new ArrayList<>();

  public void initStrategie(Strategie strategie) {
    currentState = strategie.getInitState();
  }

  public void initStrategie(Strategie strategie, Consumer<GroCervo> baseEnv) {
    strategie.setEnv(baseEnv);
    currentState = strategie.getInitState();
  }

  public void compute() {
    List<String> incomingComs = fetchAllMessages();
    incomingComs.stream().forEach(com -> {
      Event event = Event.fromMsg(com);
      if (event.getTargetTurn().compareTo(turn) == 0) {
        if (event.getRobotID() == id)
          createdEvents.add(event);
        incomingEvents.add(event);
      }
    });
    Collections.sort(incomingEvents);
    for (Event event : incomingEvents){
      sendLogMessage(id+" Event : "+event);
      event.updateGlobalData(globalData);
    }

    createdEvents.add(
        new LocalizedRadar(getPosition(), detectRadar(), getRobotID()));
  }

  @Override
  public final void step() {
    globalData.clearGlobalData();
    compute(); // fetch and process incoming events
    currentState = State.travelGraph(this, currentState); // compute next state
    makeAction(currentState.getAction());                 // execute action
    broadcastEvents(); // broadcast created events
    /*
     * log("Incoming events : " + incomingEvents);
     * log("Created events : " + createdEvents);
     */
    incomingEvents.clear();
    createdEvents.clear();
    showTurnLogs(turn);
    turn = turn.add(BigInteger.ONE);
  }

  private void broadcastEvents() {
    for (Event event : createdEvents) {
      if (event.getTargetTurn() == null)
        event.setTargetTurn(turn.add(BigInteger.ONE));
      broadcast(Event.toMsg(event));
    }
  }

  private void makeAction(Action action) {
    Event lastEvent = null;
    Coords calulatedPos;
    switch (action) {
    case MOVING:
    sendLogMessage(getRobotID()+"  is moving");
      calulatedPos = calulateMoveResult(true);
      if (!isStuck() && !isDead() && calulatedPos.isValid()) {
        getPosition().setAs(calulatedPos);
        lastEvent = new MyPos(getPosition(), id, !isDead());
        move();
      }
      lastEvent = new MyPos(getPosition(), id, isDead());
      break;
    case MOVING_BACK:
      calulatedPos = calulateMoveResult(false);
      System.err.println(getRobotID()+"  is trying to Moving back");
      sendLogMessage(getRobotID()+"  is trying to Moving back");
      if (!isStuck() && !isDead() && calulatedPos.isValid()) {
        getPosition().setAs(calulatedPos);
        lastEvent = new MyPos(getPosition(), id, !isDead());
        moveBack();
      } else
        lastEvent = new MyPos(getPosition(), id, isDead());
      break;
    case LEFT:
      stepTurn(Direction.LEFT);
      break;
    case RIGHT:
      stepTurn(Direction.RIGHT);
      break;
    case SHOOT:
    sendLogMessage(getRobotID()+"  shooting");

      fire(getPosition().angleTo(getTargetPosition()));
      break;
    case NOTHING:
    sendLogMessage(getRobotID()+"  is doing nothing");
      break;
    }
    if (lastEvent != null)
      createdEvents.add(lastEvent);
  }
  
  @Override
  public void activate() {
    Coords initialPosition;
    switch (getRobotID()) {
    case 0:
      initialPosition = (isHeading(MathHelp.EAST)
                             ? new Coords(Parameters.teamAMainBot1InitX,
                                          Parameters.teamAMainBot1InitY)
                             : new Coords(Parameters.teamBMainBot1InitX,
                                          Parameters.teamBMainBot1InitY));
      break;
    case 1:
      initialPosition = (isHeading(MathHelp.EAST)
                             ? new Coords(Parameters.teamAMainBot2InitX,
                                          Parameters.teamAMainBot2InitY)
                             : new Coords(Parameters.teamBMainBot2InitX,
                                          Parameters.teamBMainBot2InitY));
      break;
    case 2:
      initialPosition = (isHeading(MathHelp.EAST)
                             ? new Coords(Parameters.teamAMainBot3InitX,
                                          Parameters.teamAMainBot3InitY)
                             : new Coords(Parameters.teamBMainBot3InitX,
                                          Parameters.teamBMainBot3InitY));
      break;
    case 3:
      initialPosition = (isHeading(MathHelp.EAST)
                             ? new Coords(Parameters.teamASecondaryBot1InitX,
                                          Parameters.teamASecondaryBot1InitY)
                             : new Coords(Parameters.teamBSecondaryBot1InitX,
                                          Parameters.teamBSecondaryBot1InitY));
      break;
    case 4:
      initialPosition = (isHeading(MathHelp.EAST)
                             ? new Coords(Parameters.teamASecondaryBot2InitX,
                                          Parameters.teamASecondaryBot2InitY)
                             : new Coords(Parameters.teamBSecondaryBot2InitX,
                                          Parameters.teamBSecondaryBot2InitY));
      break;
    default:
      throw new IllegalArgumentException("Invalid robot ID");
    }
    Event initialEvent = new MyPos(initialPosition, getRobotID(), !isDead());
    createdEvents.add(initialEvent);
    initialEvent.updateGlobalData(globalData);
    log("Robot " + getRobotID() + " activated at position " + initialPosition);
  }

  public Coords calulateMoveResult(boolean frontal) {
    Coords currentPos = new Coords(getPosition());
    Coords move = new Coords(Math.cos(getHeading()), Math.sin(getHeading()));
    move.multiply(getSpeed());
    if (frontal)
      currentPos.add(move);
    else
      currentPos.substract(move);
    return currentPos;
  }

  public void sendOrder(OrdersType type, Coords target, int robotID) {
    createdEvents.add(new Orders(target, robotID, type));
  }

  // Setters
  public void setHeadingTarget(double headingTarget) {
    this.pushEnv(MathHelp.normalizeAngle(headingTarget));
  }

  public void setHeadingTarget(Coords target) {
    this.setHeadingTarget(getPosition().angleTo(target));
  }

  public void setTargetPosition(Coords targetPosition) {
    this.pushEnv(targetPosition);
  }

  public void editTargetPosition(Coords targetPosition) {
    this.currentEnv.setTargetPosition(targetPosition);
  }

  // Getters
  public int getRobotID() { return id; }

  public BigInteger getTurn() { return turn; }

  public GlobalData getGlobalData() { return globalData; }

  public List<Event> getIncomingEvents() { return incomingEvents; }

  public Stream<Orders> getOrders() {
    return incomingEvents.stream()
        .filter(Orders.class ::isInstance)
        .map(Orders.class ::cast);
  }

  public Orders getFirstMatchingOrder(Orders.OrdersType type) {
    return getOrders()
        .filter(order -> order.isType(type))
        .findFirst()
        .orElse(null);
  }

  public boolean hasReceivedOrder(Orders.OrdersType type) {
    return getFirstMatchingOrder(type) != null;
  }

  public static boolean isMain(Integer id) { return id < 3; }

  public static boolean isSecondary(Integer id) { return !isMain(id); }

  public double getRange() {
    if (isMain(id))
      return Parameters.teamAMainBotFrontalDetectionRange;
    else
      return Parameters.teamASecondaryBotFrontalDetectionRange;
  }

  public double getSpeed() {
    if (isMain(id))
      return Parameters.teamAMainBotSpeed;
    else
      return Parameters.teamASecondaryBotSpeed;
  }

  public double getRadius() {
    if (isMain(id))
      return Parameters.teamAMainBotRadius;
    else
      return Parameters.teamASecondaryBotRadius;
  }

  public Coords getPosition() { return globalData.getRobotPosition(id); }

  @Override
  public double getHeading() {
    return MathHelp.normalizeAngle(super.getHeading());
  }

  public double getHeadingTarget() { return currentEnv.getHeadingTarget(); }

  public Coords getTargetPosition() {
    if (currentEnv == null)
      return getPosition();
    return currentEnv.getTargetPosition();
  }

  // Tests

  public boolean isHeading(double angle) {
    angle = MathHelp.normalizeAngle(angle);
    return MathHelp.isClose(getHeading(), angle);
  }

  public boolean isAtHeadingTarget() {
    return MathHelp.isClose(getHeading(), currentEnv.getHeadingTarget());
  }

  public boolean isAtPositionTarget() {
    return getPosition().inRange(currentEnv.getTargetPosition(), getSpeed());
  }

  public boolean isCalled() {
    if (hasReceivedOrder(OrdersType.SHOOT_AT) && getRobotID() < 3) {
      Coords attacked = getFirstMatchingOrder(OrdersType.SHOOT_AT).getCoords();
      return (getPosition().getY() < 1000 && attacked.getY() < 1000) ||
          (getPosition().getY() >= 1000 && attacked.getY() >= 1000);
    } else
      return false;
  }

  public List<Coords> obstaclesAround() {
    List<Coords> enemies = getGlobalData().getOpponentPositions();
    List<Coords> wrecks = getGlobalData().getWreckPositions();
    Collection<Coords> allies =
        getGlobalData().getAllyPositionsList(getRobotID());
    List<Coords> obstacles =
        new ArrayList<>(allies.size() + enemies.size() + wrecks.size());
    obstacles.addAll(enemies);
    obstacles.addAll(wrecks);
    obstacles.addAll(allies);
    return obstacles;
  }

  public boolean isMoveAllowed(Coords mvResult) {
    List<Coords> obstacles = obstaclesAround();
    return !isDead() && obstacles.stream().noneMatch(
                            coord -> mvResult.inRange(coord, getRadius() * 3));
  }

  public boolean isFireLineBlocked() {
    Coords me = getPosition();
    double shootingAngle = me.angleTo(getTargetPosition());
    List<Coords> obstacles = obstaclesAround();
    obstacles.removeIf(
        c
        -> getGlobalData().getOpponentPositions().stream().anyMatch(
            opp -> opp.inRange(c, 6)));

    for (Coords coord : obstacles)
      if (MathHelp.lineCrossCircle(me, shootingAngle, coord, getRadius()))
        return true;
    return false;
  }

  public Coords blocker() {
    Coords me = getPosition();
    List<Coords> obstacles = obstaclesAround();
    obstacles = obstacles.stream()
                    .filter(coord -> coord.distanceTo(me) <= getRadius() * 2)
                    .collect(Collectors.toList());
    for (Coords coord : obstacles)
      if (MathHelp.lineCrossCircle(me, getHeading(), coord, getRadius()))
        return coord;
    return null;
  }

  public boolean isBlocked() {
    Coords me = getPosition();
    double heading = getHeading();
    List<Coords> obstacles = obstaclesAround();

    Coords frontPosition =
        new Coords(Math.round(me.getX() + Math.cos(heading) * getSpeed()),
                   Math.round(me.getY() + Math.sin(heading) * getSpeed()));

    if (!frontPosition.isValid())
      return true;
    for (Coords coord : obstacles) {
      if (coord.inRange(frontPosition, (getRadius() + 1) * 2)) {
        return true;
      }
    }
    return false;
  }

  public boolean isNextCoordValid(Coords nextCoord) {
    return nextCoord.isValid();
  }

  public boolean isStuck() { return isBlocked(); }
  public boolean areSecondariesDead() { return !getGlobalData().areSecondariesAlive(); }
  public boolean enemyBecomeWreck() {
    return getGlobalData().getWreckPositions().stream().anyMatch(
        b -> b.inRange(getTargetPosition(), getSpeed() * 2));
  }

  public Coords enemyDetected() {
    Coords me = getPosition();
    Optional<Coords> enemy =
        getGlobalData()
            .getOpponentPositions()
            .stream()
            .sorted((coord1, coord2)
                        -> Double.compare(me.distanceTo(coord1),
                                          me.distanceTo(coord2)))
            .findFirst();
    return enemy.isPresent() ? enemy.get() : null;
  }

  public Coords enemyNear() {
    Coords enemy = enemyDetected();
    if (enemy == null || enemy.distanceTo(getPosition()) > getRange())
      return null;
    return enemy;
  }

  public Coords worstEnnmy() {
    Coords me = getPosition();
    List<Coords> c = getGlobalData()
                         .getOpponentPositions()
                         .stream()
                         .filter(b -> b.distanceTo(getPosition()) <= getRange())
                         .sorted((coord1, coord2)
                                     -> Double.compare(me.distanceTo(coord1),
                                                       me.distanceTo(coord2)))
                         .collect(Collectors.toList());

    Collections.reverse(c);
    if (c.size() == 0)
      return null;
    return c.get(0);
  }

  // Legacy local sensors
  public boolean isFrontEnnemyRobot() {
    return detectFront().getObjectType().equals(
               IFrontSensorResult.Types.OpponentMainBot) ||
        detectFront().getObjectType().equals(
            IFrontSensorResult.Types.OpponentSecondaryBot);
  }

  public boolean isFrontAllyRobot() {
    return detectFront().getObjectType().equals(
               IFrontSensorResult.Types.TeamMainBot) ||
        detectFront().getObjectType().equals(
            IFrontSensorResult.Types.TeamSecondaryBot);
  }

  public boolean isFrontEmpty() {
    return detectFront().getObjectType().equals(
        IFrontSensorResult.Types.NOTHING);
  }

  public boolean isFrontWall() {
    return detectFront().getObjectType().equals(IFrontSensorResult.Types.WALL);
  }

  // Debug

  private boolean debugFlag = false;
  private final Map<BigInteger, List<String>> logger =
      new java.util.HashMap<>();

  public void log(String msg, int rate) {
    boolean turnFlag =
        rate == 1 || turn.mod(BigInteger.valueOf(rate)).equals(BigInteger.ZERO);
    if (debugFlag && turnFlag)
      logger.computeIfAbsent(turn, k -> new ArrayList<>()).add(msg);
  }

  public void log(String msg) { log(msg, 1); }

  public void setDebugFlag(boolean debugFlag) { this.debugFlag = debugFlag; }

  public void showTurnLogs(BigInteger turn) {
    if (debugFlag) {
      List<String> logs = logger.get(turn);
      if (logs != null) {
        String msgInfo = "Robot " + getRobotID() + " at turn " + turn + " : ";
        StringBuilder sb = new StringBuilder(msgInfo);
        String endInfo = "End of logs for turn " + turn + " for robot " +
                         getRobotID() + " ----------------- ";
        logs.stream().forEach(line -> sb.append(line).append("\n"));
        sb.append(endInfo);
        System.out.println(sb.toString());
      }
    }
  }

  public boolean isDead() { return getHealth() <= 0; }
}