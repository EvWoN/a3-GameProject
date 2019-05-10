package myGameEngine.actions;

import javafx.beans.property.SimpleBooleanProperty;
import net.java.games.input.Event;
import ray.input.action.AbstractInputAction;

public class ToggleHostAction extends AbstractInputAction {

    private SimpleBooleanProperty host;

    public ToggleHostAction(SimpleBooleanProperty host) { this.host = host; }

    @Override
    public void performAction(float v, Event event) { host.set(!host.get()); }
}
