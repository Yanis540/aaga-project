package events;

import commun.Event;
import commun.GlobalData;
import helpers.Coords;

public class Orders extends Event {

    private final OrdersType type;

    public enum OrdersType {
        FLEE_FROM,
        SHOOT_AT,
        DEATH_AT
    }

    public Orders(Coords pt, int robotID, OrdersType type) {
        setCoords(pt);
        setRobotID(robotID);
        this.type = type;
    }

    public boolean isType(OrdersType type) {
        return this.type == type;
    }

    public boolean isFleeFrom() {
        return isType(OrdersType.FLEE_FROM);
    }

    @Override
    public void updateGlobalData(GlobalData globalData) {
        // Nothing to do
    }

}