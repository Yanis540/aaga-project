package commun;

import characteristics.Parameters;
import helpers.Coords;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GlobalData {
  public static final Coords maxCoords = new Coords(3248, 2248);
  private final Map<Integer, Coords> allyPositions = new HashMap<>();
  private final Map<Integer, Boolean> allyStates = new HashMap<>();
  private final ArrayList<Coords> wreckPositions = new ArrayList<>();
  private final ArrayList<Bullet> bulletPositions = new ArrayList<>();
  private final ArrayList<Coords> opponentPositions = new ArrayList<>();

  public Map<Integer, Coords> getAllyPositions() { return allyPositions; }
  public List<Coords> getAllyPositionsList(Integer removed) {
    List<Coords> positions = new ArrayList<>();
    allyPositions.forEach((id, pos) -> {
      if (id != removed)
        positions.add(pos);
    });
    return positions;
  }
  public Map<Integer, Boolean> getAllyStates() { return allyStates; }
  public boolean isAllyAtPosition(Coords position) {
    return allyPositions.entrySet().stream().anyMatch(e -> {
      Integer id = e.getKey();
      Coords p = e.getValue();
      return position.inRange(p, GroCervo.isMain(id)
                                     ? Parameters.teamAMainBotSpeed
                                     : Parameters.teamASecondaryBotSpeed);
    });
  }

  public boolean areSecondariesAlive() {
    return allyStates.entrySet().stream().anyMatch(
        e -> !GroCervo.isMain(e.getKey()) && e.getValue());
  }

  public List<Coords> getOpponentPositions() { return opponentPositions; }

  public Coords getRobotPosition(int id) { return allyPositions.get(id); }

  public void clearGlobalData() {
    updateBullets();
    opponentPositions.clear();
  }

  public boolean isEnnemyPresent() { return !opponentPositions.isEmpty(); }

  public void addBullet(Coords newBullet) {
    Optional<Bullet> optBullet =
        bulletPositions.stream()
            .filter(
                b
                -> b.isInRangeBetween(newBullet, b.estimateTravelDistance(), 1))
            .findFirst();

    if (optBullet.isPresent()) {
      Bullet bullet = optBullet.get();
      if (!bullet.isTracked())
        bullet.computeAngle(newBullet);
    } else
      bulletPositions.add(new Bullet(newBullet));
  }

  public void updateBullets() {
    bulletPositions.forEach(Bullet::update);
    bulletPositions.removeIf(b -> !b.isValid());
  }

  public List<Bullet> getBulletPositions() { return bulletPositions; }

  public List<Coords> getWreckPositions() { return wreckPositions; }
}
