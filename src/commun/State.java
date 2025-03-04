package commun;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;

import helpers.Pair;

public class State {

    public enum Action {
        MOVING, MOVING_BACK, LEFT, RIGHT, SHOOT, NOTHING
    }

    private ArrayList<Pair<Predicate<GroCervo>, State>> transitions = new ArrayList<>();
    private Consumer<GroCervo> sideEffects;
    private Action action;

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public State() {
        this.sideEffects = null;
        this.action = Action.NOTHING;
    }

    public State(Action action) {
        this.sideEffects = null;
        this.action = action;
    }

    public State(Consumer<GroCervo> sideEffects) {
        this.sideEffects = sideEffects;
        this.action = Action.NOTHING;
    }

    public State(Consumer<GroCervo> sideEffects, Action action) {
        this.sideEffects = sideEffects;
        this.action = action;
    }

    public State copy() {
        State copy = new State(this.sideEffects, this.action);
        for (Pair<Predicate<GroCervo>, State> transition : transitions) {
            copy.transitions.add(new Pair<>(transition.getFirst(), transition.getSecond()));
        }
        return copy;
    }

    public void appendTransition(Predicate<GroCervo> condition, State next) {
        transitions.add(new Pair<>(condition, next));
    }

    public void appendTransition(State next) {
        transitions.add(new Pair<>(null, next));
    }

    public void appendTransition(Strategie next) {
        transitions.add(new Pair<>(null, next.getInitState()));
    }

    public void appendTransition(Predicate<GroCervo> condition, Strategie next) {
        transitions.add(new Pair<>(condition, next.getInitState()));
    }

    public void pushTransition(Predicate<GroCervo> condition, State next) {
        transitions.add(0, new Pair<>(condition, next));
    }

    public void pushTransition(State next) {
        transitions.add(0, new Pair<>(null, next));
    }

    public void sideEffectBefore(Consumer<GroCervo> before) {
        if (this.sideEffects == null)
            this.sideEffects = before;
        else
            this.sideEffects = before.andThen(this.sideEffects);
    }

    public void sideEffectAfter(Consumer<GroCervo> after) {
        if (this.sideEffects == null)
            this.sideEffects = after;
        else
            this.sideEffects = this.sideEffects.andThen(after);
    }

    private State nextState(GroCervo data) {
        for (Pair<Predicate<GroCervo>, State> transition : transitions) {
            Predicate<GroCervo> condition = transition.getFirst();
            if (condition == null)
                return transition.getSecond();

            if (transition.getFirst().test(data))
                return transition.getSecond();

        }
        return this;
    }

    public static State travelGraph(GroCervo data, State start) {
        State current = start;
        while (true) {
            if (current.sideEffects != null)
                current.sideEffects.accept(data);
            State next = current.nextState(data);
            if (next == current)
                return current;
            current = next;
            if (current.action != Action.NOTHING)
                return current;
        }
    }

    @Override
    public String toString() {
        return "State{" + "action=" + action + '}';
    }
}
