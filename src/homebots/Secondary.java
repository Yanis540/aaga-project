package homebots;

import characteristics.Parameters;
import commun.GroCervo;
import commun.Strategie;
import helpers.Coords;

public class Secondary extends GroCervo {
  enum TargetScout {
    TOPLEFT, TOPRIGHT, BOTLEFT, BOTRIGHT
  }

  private TargetScout target = TargetScout.TOPRIGHT;
  private boolean is_teamA = true;

  @Override
  public void activate() {
    super.activate();
    if (getHeading() == Parameters.WEST) {
      is_teamA = false;
      target = (getRobotID() == 4 ? TargetScout.BOTLEFT : TargetScout.TOPLEFT);
    } else if (getRobotID() == 4) {
      target = TargetScout.BOTRIGHT;
    }
    Coords target = new Coords(1500, getRobotID() == 4 ? 1500 : 500);
    Strategie init = Strategie.goToPosition();
    init.setEnv(data -> data.setTargetPosition(target));
    Strategie scout = Strategie.scout();
    scout.setEnv(
        data -> data.setTargetPosition(chooseNextScoutDestination(data)));
    init.setNext(scout);
    initStrategie(init);
  }

  private Coords chooseNextScoutDestination(GroCervo data) {
    Coords me = data.getPosition();
    if (is_teamA) {
      if (target == TargetScout.TOPRIGHT && (me.getY() > 490 && me.getY() < 510))
        target = getRobotID() == 4 ? TargetScout.BOTRIGHT : TargetScout.TOPLEFT;

      if (target == TargetScout.BOTRIGHT && (me.getX() > 2490 && me.getX() < 2510))
        target = getRobotID() == 4 ? TargetScout.TOPRIGHT : TargetScout.TOPRIGHT;

      if (target == TargetScout.BOTLEFT && (me.getY() > 1490 && me.getY() < 1510))
        target = getRobotID() == 4 ? TargetScout.TOPLEFT : TargetScout.BOTRIGHT;

      if (target == TargetScout.TOPLEFT && (me.getX() > 1490 && me.getX() < 1510))
        target = getRobotID() == 4 ? TargetScout.TOPRIGHT : TargetScout.BOTLEFT;

      switch (this.target) {
        case TOPLEFT:
          return new Coords(1500, me.getY());
        case TOPRIGHT:
          return new Coords(me.getX(), 500);
        case BOTLEFT:
          return new Coords(me.getX(), 1500);
        default:
          return new Coords(2500, me.getY());
      }
    } else {
      if (target == TargetScout.TOPRIGHT && (me.getY() > 490 && me.getY() < 510))
        target = getRobotID() == 5 ? TargetScout.TOPLEFT : TargetScout.BOTRIGHT;

      if (target == TargetScout.BOTRIGHT && (me.getX() > 1490 && me.getX() < 1510))
        target = getRobotID() == 5 ? TargetScout.TOPRIGHT : TargetScout.BOTLEFT;

      if (target == TargetScout.BOTLEFT && (me.getY() > 1490 && me.getY() < 1510))
        target = getRobotID() == 5 ? TargetScout.BOTRIGHT : TargetScout.TOPLEFT;

      if (target == TargetScout.TOPLEFT && (me.getX() > 490 && me.getX() < 510))
        target = getRobotID() == 5 ? TargetScout.BOTLEFT : TargetScout.TOPRIGHT;

      switch (this.target) {
        case TOPLEFT:
          return new Coords(500, me.getY());
        case TOPRIGHT:
          return new Coords(me.getX(), 500);
        case BOTLEFT:
          return new Coords(me.getX(), 1500);
        default:
          return new Coords(1500, me.getY());
      }
    }
  }
}
