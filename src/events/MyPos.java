package events;

import commun.Event;
import commun.GlobalData;
import helpers.Coords;

public class MyPos extends Event {
  private final boolean alive;
  public MyPos(Coords pt, int robotID, boolean alive) {
    setCoords(pt);
    setRobotID(robotID);
    this.alive = alive;
  }

  @Override
  public void updateGlobalData(GlobalData globalData) {
    globalData.getAllyPositions().put(getRobotID(), getCoords());
    globalData.getAllyStates().put(getRobotID(), alive);
  }

  @Override
  public String toString() {
    return "MyPos [coords=" + getCoords() + ", robotID=" + getRobotID() + "]";
  }
}
