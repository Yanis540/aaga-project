/* ******************************************************
 * Simovies - Eurobot 2015 Robomovies Simulator.
 * Copyright (C) 2014 <Binh-Minh.Bui-Xuan@ens-lyon.org>.
 * GPL version>=3 <http://www.gnu.org/licenses/>.
 * $Id: algorithms/BrainCanevas.java 2014-10-19 buixuan.
 * ******************************************************/
package algorithms;

import robotsimulator.Brain;
import characteristics.IFrontSensorResult;
import characteristics.Parameters;
import playground.PlayingArea;

public class CustomBrain extends Brain {
  public CustomBrain() { super(); }
  private double oldAngle=0;
  private boolean updated_old_angle = false;
  private boolean is_initial = true;
  private int playground_height,playground_width;
  public void activate() {
    //---PARTIE A MODIFIER/ECRIRE---//
      //moveBack(); 
    var p = new PlayingArea();
    this.playground_height = p.getHeight();  
    this.playground_width = p.getWidth();  


      
  }
  public void step() {
    //---PARTIE A MODIFIER/ECRIRE---//
    sendLogMessage(String.valueOf(getHeading()));
    if(is_initial){
      if(isHeading(Parameters.NORTH)){
        is_initial = false;
      }else{
        stepTurn(Parameters.Direction.LEFT);
      }
      return ; 
    }
    if(updated_old_angle){
      if(!isHeading(oldAngle+Math.PI/2)){
        stepTurn(Parameters.Direction.RIGHT);
      }else{
        updated_old_angle=false;
      }
      return;
    }
    if(detectFront().getObjectType() ==IFrontSensorResult.Types.TeamMainBot || detectFront().getObjectType() ==IFrontSensorResult.Types.TeamSecondaryBot ){
      return;
    }
    if(detectFront().getObjectType() != IFrontSensorResult.Types.WALL){
      move();
      return ;
    }
    if(detectFront().getObjectType()==IFrontSensorResult.Types.WALL){
      oldAngle = getHeading();
      updated_old_angle = true;
      return;
    }
   
  }
  private boolean isHeading(double dir){
    return Math.abs(Math.sin(getHeading()-dir))<Parameters.teamAMainBotStepTurnAngle;
  }
}
