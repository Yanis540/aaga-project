package events;

import characteristics.IRadarResult;
import commun.Event;
import commun.GlobalData;
import helpers.Coords;
import helpers.MathHelp;
import java.util.List;

public class LocalizedRadar extends Event {

  private final List<IRadarResult> radarResults;

  @Override
  public void updateGlobalData(GlobalData globalData) {
    for (IRadarResult object : getRadarResults()) {
      Coords coord = MathHelp.getLocation(object.getObjectDistance(),
                                          object.getObjectDirection());
      coord.add(getCoords());
      switch (object.getObjectType()) {
      case TeamMainBot:
        break;
      case TeamSecondaryBot:
        break;
      case OpponentMainBot:
        globalData.getOpponentPositions().add(coord);
        break;
      case OpponentSecondaryBot:
        globalData.getOpponentPositions().add(coord);
        break;
      case Wreck:
        if (globalData.getWreckPositions().stream().noneMatch(
                c -> c.inRange(coord, 10)))
          globalData.getWreckPositions().add(coord);
        break;
      case BULLET:
        globalData.addBullet(coord);
        break;
      default:
        break;
      }
    }
  }

  public LocalizedRadar(Coords pt, List<IRadarResult> radarResults,
                        int robotID) {
    setCoords(pt);
    this.radarResults = radarResults;
    setRobotID(robotID);
  }

  public List<IRadarResult> getRadarResults() { return radarResults; }

  @Override
  public String toString() {
    final StringBuilder sb =
        new StringBuilder("\nLocalizedRadar{R_ID = " + getRobotID() +
                          "}{TargetTurn = " + getTargetTurn() + "}\n");
    sb.append("\tradarResults{\n");
    for (IRadarResult result : radarResults)
      sb.append("\t\t" + radarResultToString(result));
    sb.append("\t}");
    return sb.toString();
  }

  private String radarResultToString(IRadarResult result) {
    return "Type: " + result.getObjectType() +
        " Direction: " + result.getObjectDirection() +
        " Distance: " + result.getObjectDistance() +
        " Radius: " + result.getObjectRadius() + "\n";
  }
}