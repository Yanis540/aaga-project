package homebots;

import characteristics.Parameters;
import commun.GroCervo;
import commun.State;
import commun.State.Action;
import commun.Strategie;
import helpers.Coords;

public class Main extends GroCervo {
  int[] initY = {500, 1000, 1500};
  private boolean is_teamA = true;

  @Override
  public void activate() {
    super.activate();
    if (getHeading() == Parameters.WEST) {
      is_teamA = false;
    }
    Coords target = new Coords((is_teamA ? 700 : 2200), initY[getRobotID()]);
    Strategie init = Strategie.goToPosition();
    Strategie shootAt = Strategie.shootAt();
    State idle = new State();

    State shoot = new State(data -> {
      Coords enemy = data.enemyDetected();
      if (enemy != null) {
        data.setTargetPosition(enemy);
      }
    }, Action.SHOOT);

    init.setEnv(data -> data.setTargetPosition(target));
    init.setNext(idle);
    idle.appendTransition(data -> data.enemyDetected() != null, shootAt);
    shoot.appendTransition(data -> data.enemyDetected() == null, idle);
    shoot.appendTransition(GroCervo::enemyBecomeWreck, idle);

    shootAt.setEnv(data -> { data.setTargetPosition(data.enemyDetected()); });
    shootAt.setNext(idle);

    initStrategie(init);
  }
}
