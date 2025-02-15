/* ******************************************************
 * Simovies - Eurobot 2015 Robomovies Simulator.
 * Copyright (C) 2014 <Binh-Minh.Bui-Xuan@ens-lyon.org>.
 * GPL version>=3 <http://www.gnu.org/licenses/>.
 * $Id: algorithms/BrainCanevas.java 2014-10-19 buixuan.
 * ******************************************************/
package algorithms;

import robotsimulator.Bot;
import robotsimulator.Brain;
import robotsimulator.SimulatorEngine;
import supportGUI.FileLoader;

import java.util.ArrayList;
import java.util.List;

import characteristics.IBrain;
import characteristics.IFrontSensorResult;
import characteristics.Parameters;
import playground.PlayingArea;
import java.lang.reflect.Field;
public class CustomBrain extends Brain {
  public CustomBrain() {
    super();
  }

  private double oldAngle = 0;
  private boolean updated_old_angle = false;
  private boolean is_initial = true;
  private int playground_height, playground_width;
  private double quarterSouth, halfSouth, threeQuarterSouth;
  private boolean hasTurned = false;
  private Bot bot;
  private boolean isTurning = false;
  private double initialAngle = 0;
  private boolean hasCompltedRightQuarterTurn = false;

  protected void bind(Bot bot) {
    this.bot = bot;
    this.bot.getTeam();
    super.bind(bot);
  }

  public void activate()  {
    // ---PARTIE A MODIFIER/ECRIRE---//
    // moveBack();
    var p = new PlayingArea();
    this.playground_height = p.getHeight();
    this.playground_width = p.getWidth();
    this.quarterSouth = this.playground_width / 4.0;
    this.halfSouth = this.playground_width / 2.0;
    this.threeQuarterSouth = 3 * this.playground_width / 4.0;
    try{
   
      System.out.println("Engine: " + engine);
    }
    catch(Exception e){
      System.out.println("Error");
    }
  }

  public void step() {
    // ---PARTIE A MODIFIER/ECRIRE---//
    sendLogMessage(String.valueOf(getHeading()));
    if (is_initial) {
      if (isHeading(Parameters.NORTH)) {
        is_initial = false;
      } else {
        stepTurn(Parameters.Direction.LEFT);
      }
      return;
    }
    
    stage3();

  }
  private void campFire(){
    
  }
  public void stage1() {
    if (updated_old_angle) {
      if (!isHeading(oldAngle + Math.PI / 2)) {
        stepTurn(Parameters.Direction.RIGHT);
      } else {
        updated_old_angle = false;
      }
      return;
    }
    if (detectFront().getObjectType() == IFrontSensorResult.Types.TeamMainBot
        || detectFront().getObjectType() == IFrontSensorResult.Types.TeamSecondaryBot) {
      return;
    }
    if (detectFront().getObjectType() != IFrontSensorResult.Types.WALL) {
      move();
      return;
    }
    if (detectFront().getObjectType() == IFrontSensorResult.Types.WALL) {
      oldAngle = getHeading();
      updated_old_angle = true;
      return;
    }
  }


  public void stage3() {
    if (updated_old_angle) {
      if (!isHeading(oldAngle + Math.PI / 2)) {
        stepTurn(Parameters.Direction.RIGHT);
      } else {
        updated_old_angle = false;
      }
      return;
    }
    if (isTurning) {
      if(!hasCompltedRightQuarterTurn){
        System.out.println("is heading back quarter "+isHeading(Parameters.RIGHTTURNFULLANGLE) +" "+ getHeading());
        if (!isHeading(Parameters.RIGHTTURNFULLANGLE)) {
          stepTurn(Parameters.Direction.RIGHT);
        } else {
          hasCompltedRightQuarterTurn = true;
        }
        return;
      }
      System.out.println("is heading back west "+isHeading(initialAngle + 2 * Math.PI));
      boolean isFinishedTurn = isHeadingNormalized(initialAngle);
      // boolean isFinishedTurn = Math.abs(normalizedHeading - targetAngle)>= Parameters.teamAMainBotStepTurnAngle
      if (!isFinishedTurn) {
        stepTurn(Parameters.Direction.RIGHT);
        System.out.println("Tour Incomplet !");
      } else {
        System.out.println("Tour complet terminé ! " + getHeading() + " "+ initialAngle);
          isTurning = false;  // Turn completed
      }
      return;
    }

    if (isHeading(Parameters.WEST)) {
      double currentX = this.bot.getX();
      double quarterWidth = playground_width / 4.0;

      if (isAtRendezvousPoint(currentX, quarterWidth) && !hasTurned) {
        isTurning = true;
        initialAngle = getHeading();
        hasTurned = true;
        hasCompltedRightQuarterTurn = false; 
        return; 
      } else if (!isAtRendezvousPoint(currentX, quarterWidth)) {
        hasTurned = false;
      }
    }

    if (detectFront().getObjectType() == IFrontSensorResult.Types.TeamMainBot
        || detectFront().getObjectType() == IFrontSensorResult.Types.TeamSecondaryBot) {
      return;
    }
    if (detectFront().getObjectType() != IFrontSensorResult.Types.WALL) {
      move();
      return;
    }
    if (detectFront().getObjectType() == IFrontSensorResult.Types.WALL) {
      oldAngle = getHeading();
      updated_old_angle = true;
      return;
    }
  }

  // Fonction pour détecter si le robot est en bordure sud (approximation)
  private boolean isAtRendezvousPoint(double x, double quarterWidth) {
    return Math.abs(x - quarterWidth) < Parameters.teamAMainBotRadius ||
        Math.abs(x - 2 * quarterWidth) < Parameters.teamAMainBotRadius ||
        Math.abs(x - 3 * quarterWidth) < Parameters.teamAMainBotRadius;
  }

  private double normalizeAngle(double angle) {
    angle = angle % (2 * Math.PI);
    if (angle < 0) {
        angle += 2 * Math.PI;
    }
    return angle;
  }
  private boolean isHeadingNormalized(double dir){
    double targetAngle = normalizeAngle(dir);
    double normalizedHeading = normalizeAngle(getHeading());
    return Math.abs(normalizedHeading - targetAngle) < Parameters.teamAMainBotStepTurnAngle 
    ||  Math.abs(normalizedHeading - targetAngle) > 2 * Math.PI - Parameters.teamAMainBotStepTurnAngle;
  }
  private boolean isHeading(double dir) {
    return Math.abs(Math.sin(getHeading() - dir)) < Parameters.teamAMainBotStepTurnAngle;
  }
  public static Object doesSomethingNottOfFun(Object obj, String fieldName) throws Exception {
    Field field = obj.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    return field.get(obj);
  }
  private SimulatorEngine engine;
  private Bot aMain1;
  private Bot aMain2;
  private Bot aMain3;
  private Bot aSecondary1;
  private Bot aSecondary2;
  private Bot bMain1;
  private Bot bMain2;
  private Bot bMain3;
  private Bot bSecondary1;
  private Bot bSecondary2;
  private ArrayList<Bot> teamA = new ArrayList<Bot>();
  private ArrayList<Bot> teamB = new ArrayList<Bot>();
  private void NotSoFun(){
    try{
      engine = (SimulatorEngine)doesSomethingNottOfFun(bot, "engine");
      aMain1 = (Bot)doesSomethingNottOfFun(engine, "aMain1");
      aMain2 = (Bot)doesSomethingNottOfFun(engine, "aMain2");
      aMain3 = (Bot)doesSomethingNottOfFun(engine, "aMain3");
      aSecondary1 = (Bot)doesSomethingNottOfFun(engine, "aSecondary1");
      aSecondary2 = (Bot)doesSomethingNottOfFun(engine, "aSecondary2");
      bMain1 = (Bot)doesSomethingNottOfFun(engine, "bMain1");
      bMain2 = (Bot)doesSomethingNottOfFun(engine, "bMain2");
      bMain3 = (Bot)doesSomethingNottOfFun(engine, "bMain3");
      bSecondary1 = (Bot)doesSomethingNottOfFun(engine, "bSecondary1");
      bSecondary2 = (Bot)doesSomethingNottOfFun(engine, "bSecondary2");
      teamA.addAll(List.of(aMain1, aMain2, aMain3, aSecondary1, aSecondary2));
      teamB.addAll(List.of(bMain1, bMain2, bMain3, bSecondary1, bSecondary2));
    }
    catch(Exception e){
      System.out.println("Error");
    }

  } 
}
