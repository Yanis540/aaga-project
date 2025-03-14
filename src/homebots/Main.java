package homebots;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import characteristics.Parameters;
import commun.GroCervo;
import commun.State;
import commun.State.Action;
import commun.Strategie;
import helpers.Coords;
import robotsimulator.Bot;
import robotsimulator.SimulatorEngine;

public class Main extends GroCervo {
  int[] initY = {500, 1000, 1500};
  private boolean is_teamA = true;
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
  private Bot bot;

  private ArrayList<Bot> teamB = new ArrayList<Bot>();
  public static Object getAttribute(Object obj, String fieldName) throws Exception {
    Field field = obj.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    return field.get(obj);
  }
  protected void bind(Bot bot) {
    this.bot = bot;
    this.bot.getTeam();
    super.bind(bot);
  }

  private void NotSoFun(){
    try{
      engine = (SimulatorEngine)getAttribute(bot, "engine");
      aMain1 = (Bot)getAttribute(engine, "aMain1");
      aMain2 = (Bot)getAttribute(engine, "aMain2");
      aMain3 = (Bot)getAttribute(engine, "aMain3");
      aSecondary1 = (Bot)getAttribute(engine, "aSecondary1");
      aSecondary2 = (Bot)getAttribute(engine, "aSecondary2");
      bMain1 = (Bot)getAttribute(engine, "bMain1");
      bMain2 = (Bot)getAttribute(engine, "bMain2");
      bMain3 = (Bot)getAttribute(engine, "bMain3");
      bSecondary1 = (Bot)getAttribute(engine, "bSecondary1");
      bSecondary2 = (Bot)getAttribute(engine, "bSecondary2");
      teamA.addAll(List.of(aMain1, aMain2, aMain3, aSecondary1, aSecondary2));
      teamB.addAll(List.of(bMain1, bMain2, bMain3, bSecondary1, bSecondary2));
      ArrayList<Bot> myTeam = is_teamA?teamA:teamB;
      (myTeam).forEach(bot ->{
        try {
            Field field = bot.getClass().getDeclaredField("frontRange");
            field.setAccessible(true);
            field.setDouble(bot,500);
        } catch (Exception e) {
        }
      } );
    }
    catch(Exception e){
      System.out.println("Error");
    }

  } 
  @Override
  public void activate() {
    super.activate();

    if (getHeading() == Parameters.WEST) {
        is_teamA = false;
    }
    NotSoFun();
    final Coords[] targets = { new Coords((is_teamA ? 700 : 2200), initY[getRobotID()]) };
    setTargetPosition(targets[0]);

    Strategie init = Strategie.goToPosition();
    Strategie shootAt = Strategie.shootAt();
    State idle = new State(data->{
        System.out.println( data.getRobotID()+" is at Idle, will be moving to target position");
        Coords enemy = data.enemyDetected();
        if(enemy != null){
            System.out.println(data.getRobotID()+" Enemy detected : " + enemy);
            data.setTargetPosition(enemy); 
        }
        else {
            System.out.println(data.getRobotID()+" NO Enemy detected : " + getTargetPosition());
            data.setTargetPosition(getTargetPosition());
        }
    }, Action.NOTHING);
    init.setEnv(data -> {
        data.setTargetPosition(getTargetPosition());
        data.log("Setting initial target position: " + getTargetPosition());
    });
   
   
    Strategie blocked = Strategie.blocked();
    blocked.setNext(idle);
    
    init.setNext(idle);
    idle.appendTransition(data -> data.enemyDetected() != null, shootAt);
    idle.appendTransition(GroCervo::isStuck, blocked);
    shootAt.setEnv(data -> {    
        Coords enemy = data.enemyDetected();
        data.setTargetPosition(enemy!=null?enemy:getTargetPosition());
        data.log("Setting target position to enemy: " + enemy);
    });
    shootAt.setNext(idle);

    initStrategie(init);
  }
}
