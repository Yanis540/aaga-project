package commun;

import commun.State.Action;
import events.Orders.OrdersType;
import helpers.Coords;
import helpers.MathHelp;

import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import characteristics.Parameters;

public class Strategie {
  private State initState;
  private State end;
  private boolean requireEnv;
  private boolean envSet = false;
  private String name;

  private Strategie(State initState, State end, String name, boolean requireEnv,
                    Consumer<GroCervo> newMsg) {
    this.end = end;
    this.name = name;
    this.requireEnv = requireEnv;

    this.initState = new State();
    this.initState.sideEffectBefore(
        data -> data.log("Strategie " + name + " is running"));
    if (newMsg != null)
      this.initState.sideEffectBefore(newMsg);
    this.initState.appendTransition(initState);
  }

  private Strategie(State initState, State end, boolean requireEnv) {
    this.initState = initState;
    this.end = end;
    this.requireEnv = requireEnv;
  }

  public Strategie setNext(Strategie next) {
    if (next.requireEnv && !envSet)
      throw new IllegalArgumentException(
          "Strategie " + next.name +
          " requires an environment can't be merged");
    this.name = next.name;
    setNext(next.initState);
    this.end = next.end; /* NEVER TOUCH THIS */
    return this;
  }

  public void setNext(State next) { setNext(next, true); }

  public void setNext(State next, boolean withPop) {
    this.end.appendTransition(data -> {
      if (envSet && withPop)
        data.popEnv();
      return true;
    }, next);
    this.end = next;
  }

  public Strategie setNext(Strategie next, Consumer<GroCervo> env) {
    next.setEnv(env);
    return setNext(next);
  }

  public void setEnv(Consumer<GroCervo> env) {
    initState.sideEffectBefore(env);
    envSet = true;
  }

  public void resetEnv(boolean isNowRequired) {
    envSet = false;
    requireEnv = isNowRequired;
  }

  public void resetEnv() { envSet = false; }

  public State getInitState() {
    if (requireEnv && !envSet)
      throw new IllegalStateException("Strategie " + name +
                                      " requires an environment");
    return initState;
  }

  public static Strategie turnUntilAngle() {
    State choseAngle = new State();
    State turnLeft = new State(Action.LEFT);
    State turnRight = new State(Action.RIGHT);
    State end = new State();
    choseAngle.appendTransition(
        data
        -> MathHelp.choseBestTurn(data.getHeading(), data.getHeadingTarget()) ==
               Action.LEFT,
        turnLeft);
    choseAngle.appendTransition(
        data
        -> MathHelp.choseBestTurn(data.getHeading(), data.getHeadingTarget()) ==
               Action.RIGHT,
        turnRight);
    choseAngle.appendTransition(end);
    turnLeft.appendTransition(GroCervo::isDead, end);
    turnLeft.appendTransition(GroCervo::isAtHeadingTarget, end);
    turnRight.appendTransition(GroCervo::isDead, end);
    turnRight.appendTransition(GroCervo::isAtHeadingTarget, end);
    return new Strategie(choseAngle, end, true);
  }

  public static Strategie goToPosition() {
    State start = new State();
    Strategie turnUntilAngle = turnUntilAngle();
    turnUntilAngle.setEnv(
        data -> data.setHeadingTarget(data.getTargetPosition()));
    State moving =
        new State(data
                  -> data.log("Current position : " + data.getPosition(), 100),
                  Action.MOVING);
    State end = new State(Action.NOTHING);
    Strategie blocked = blocked();
    blocked.setNext(end);
    turnUntilAngle.setNext(moving);

    moving.appendTransition(GroCervo::isDead, end);
    moving.appendTransition(GroCervo::isStuck, blocked);
    moving.appendTransition(GroCervo::isAtPositionTarget, end);
    moving.appendTransition(turnUntilAngle);
    start.appendTransition(turnUntilAngle);
    start.pushTransition(
        data -> data.getPosition().inRange(data.getTargetPosition(), 0), end);

    return new Strategie(start, end, "goToPosition", true, null);
  }
  public static Strategie scout() {
    State start = new State();
    Strategie goTo = goToPosition();
    goTo.setEnv(data -> {
      Coords me = data.getPosition();
      double angle = me.angleTo(data.getTargetPosition());
      data.setTargetPosition(
          new Coords(me.getX() + Math.cos(angle) * data.getSpeed(),
                     me.getY() + Math.sin(angle) * data.getSpeed()));
    });
    start.appendTransition(goTo);
    Strategie scoutLoop = goTo.setNext(detect());
    scoutLoop.end.pushTransition(start);

    return scoutLoop;
  }

  private static Strategie moveNStep(int n) {
    State start = new State();
    State end = new State();
    State save = start;
    for (int i = 0; i < n; i++) {
      State move = new State(Action.MOVING);
      save.appendTransition(move);
      save = move;
    }
    save.appendTransition(end);
    return new Strategie(start, end, "moveNStep", false, null);
  }

  public static Strategie blocked() {
    State start = new State(
        data -> {
            System.err.println(data.getRobotID() + " is blocked");
            data.log("Entering blocked state");
        });

    Strategie turn = turnUntilAngle();
    turn.setEnv(
      data -> {
        // data.setHeadingTarget(data.getHeading() + (0.7853982 * 2));
        // System.out.println(data.getRobotID()+" is Turning to new heading: " + data.getHeadingTarget());
      
          // Si c'est un autre obstacle, tourner de 90 degrés (1.5708 radians)
          data.setHeadingTarget(data.getHeading() +MathHelp.SOUTH);
          data.log("Turning 90 degrees to avoid obstacle");
          System.out.println(data.getRobotID()+" Turning 90 degrees to avoid obstacle");
      });

    State endMove = new State(Action.NOTHING);
    Strategie runBack = moveNStep(30);
    runBack.setNext(endMove);
    State end = new State();
    endMove.appendTransition(GroCervo::isAtPositionTarget, end);
    endMove.appendTransition(GroCervo::isStuck, start);
    endMove.appendTransition(end);
    start.appendTransition(turn);
    turn.setNext(runBack);

    return new Strategie(start, end, "blocked", false, null);
  }

  public static Strategie detect() {
    Strategie dodge = goToPosition();
    dodge.setEnv(data -> {
      Coords attacked = data.getTargetPosition();
      double maxRange = data.getRange() + (data.getSpeed() * Parameters.teamASecondaryBotSpeed);
      double angle = MathHelp.normalizeAngle(
          attacked.angleTo(data.getPosition()) + MathHelp.EPSILON);
      Coords target = new Coords(maxRange * Math.cos(angle) + attacked.getX(),
                                 maxRange * Math.sin(angle) + attacked.getY());
      if (data.enemyNear() != null)
        data.setTargetPosition(target);
      else
        data.setTargetPosition(data.getPosition());
    });

    State wait = new State(data -> {
      Coords neabyEnemy = data.enemyNear();
      if (neabyEnemy != null)
        data.setTargetPosition(neabyEnemy);
    });
    State end = new State();
    wait.pushTransition(GroCervo::isDead, end);
    wait.appendTransition(GroCervo::enemyBecomeWreck, end);
    wait.appendTransition(data -> data.enemyNear() == null, end);
    wait.appendTransition(dodge);
    Strategie suicide = goToPosition();
    suicide.setEnv(data -> {
      Coords attacked = data.worstEnnmy();
      if (attacked == null) {
        data.setTargetPosition(data.getPosition());
        return;
      }
      double angle = attacked.angleTo(data.getPosition());
      double dist = data.getRadius() * 3;
      Coords target = new Coords(dist * Math.cos(angle) + attacked.getX(),
                                 dist * Math.sin(angle) + attacked.getY());
      if (attacked.distanceTo(data.getPosition()) <= data.getRadius() * 2) {
        data.setTargetPosition(data.getPosition());
      } else
        data.setTargetPosition(target);
    });
    wait.pushTransition(
        data -> data.getHealth() <= 50 && !data.isDead(), suicide.initState);
    suicide.setNext(end);
    end.appendTransition(data -> data.enemyNear() != null, dodge.initState);
    return dodge.setNext(new Strategie(wait, end, "Detect", false, null));
  }

  public static Strategie shootAt() {
    Strategie goToCoords = goToPosition();
    goToCoords.setEnv(data -> {
      Coords target = new Coords(data.getPosition());
      if (data.getTargetPosition().distanceTo(data.getPosition()) > 1100) {
        double angle = data.getPosition().angleTo(data.getTargetPosition());
        target.add(angle, data.getSpeed());
      }
      data.setTargetPosition(target);
    });

    Strategie dodge = goToPosition();
    dodge.setEnv(data -> {
      Coords attacked = data.getTargetPosition();
      double offset =
          (data.getRobotID() % 2 == 0 ? 1 : -1) * (MathHelp.EPSILON * 2);
      double maxRange = 1000;
      maxRange = data.getRobotID() == 1 ? maxRange + 100 : maxRange;
      double angle =
          MathHelp.addAngle(attacked.angleTo(data.getPosition()), offset);
      double dist = Math.min(maxRange, attacked.distanceTo(data.getPosition()));
      Coords target = new Coords(dist * Math.cos(angle) + attacked.getX(),
                                 dist * Math.sin(angle) + attacked.getY());
      data.setTargetPosition(target);
    });

    State shoot = new State(data -> {
      Coords enemy = data.enemyDetected();
      if (enemy != null) {
        data.setTargetPosition(enemy);
      }
    }, Action.SHOOT);
    State end = new State(Action.NOTHING);
    State start = new State();

    start.appendTransition(data -> data.enemyDetected() == null, end);
    start.appendTransition(
        data -> !data.enemyBecomeWreck() && data.isFireLineBlocked(), dodge);
    dodge.setNext(end);
    start.appendTransition(
        data
        -> data.enemyDetected().distanceTo(data.getPosition()) > 1100,
        end);
    start.appendTransition(shoot);

    shoot.appendTransition(GroCervo::enemyBecomeWreck, end);
    shoot.appendTransition(GroCervo::isFireLineBlocked, dodge);
    shoot.appendTransition(start);

    Strategie shootAt =
        goToCoords.setNext(new Strategie(start, end, "ShootAt", false, null));
    shootAt.requireEnv = true;
    return shootAt;
  }

  public static Strategie flee() {
    Strategie turnUntilAngle = turnUntilAngle();
    turnUntilAngle.setEnv(data -> {
      Coords fleed =
          data.getFirstMatchingOrder(OrdersType.FLEE_FROM).getCoords();
      data.setHeadingTarget(fleed);
    });

    State start = new State();
    State emitflee = new State(
        data
        -> data.sendOrder(OrdersType.FLEE_FROM,
                          data.getGlobalData().getOpponentPositions().get(0),
                          data.getRobotID()));
    State waitFlee = new State();
    State moving = new State(Action.MOVING_BACK);
    start.appendTransition(
        data -> data.hasReceivedOrder(OrdersType.FLEE_FROM), turnUntilAngle);
    waitFlee.appendTransition(
        data -> data.hasReceivedOrder(OrdersType.FLEE_FROM), turnUntilAngle);
    start.appendTransition(
        data -> data.getGlobalData().isEnnemyPresent(), emitflee);
    emitflee.appendTransition(waitFlee);
    turnUntilAngle.setNext(moving);
    return new Strategie(start, moving, "flee", false,
                         data -> data.log("Fleeing"));
  }
}
