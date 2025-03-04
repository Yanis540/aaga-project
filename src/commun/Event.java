package commun;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Base64;

import helpers.Coords;

public abstract class Event implements Serializable, Comparable<Event> {

    private Coords coords;
    private int robotID;
    private BigInteger targetTurn = null;

    public int getRobotID() {
        return robotID;
    }

    public Coords getCoords() {
        return coords;
    }

    protected void setCoords(Coords coords) {
        this.coords = coords;
    }

    protected void setRobotID(int robotID) {
        this.robotID = robotID;
    }

    public void setTargetTurn(BigInteger turn) {
        this.targetTurn = turn;
    }

    public BigInteger getTargetTurn() {
        return targetTurn;
    }

    public abstract void updateGlobalData(GlobalData globalData);

    public static Event fromMsg(String msg) {
        byte[] data = Base64.getDecoder().decode(msg);
        try (ObjectInputStream ois = new ObjectInputStream(
                new ByteArrayInputStream(data))) {
            return (Event) ois.readObject();
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String toMsg(Event toSend) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(toSend);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    @Override
    public int compareTo(Event o) {
        return this.getRobotID() - o.getRobotID();
    }
}
