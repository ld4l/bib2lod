package org.ld4l.bib2lod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum Action {

    DEDUPE ("dedupe");

    private final String action;

    Action(String action) {
        this.action = action;
    }
    
    public String action() {
        return this.action;
    }
    
    private static final Map<String, Action> lookup = 
            new HashMap<String, Action>();
    
    private static List<String> validActions = 
            new ArrayList<String>();
    
    static {
        for(Action actionValue : Action.values()) {
            String action = actionValue.action();
            lookup.put(action, actionValue);
            validActions.add(action);
        }
    }

    public static Action get(String action) { 
        return lookup.get(action); 
    }
    
    public static List<String> validFormats() {
        return validActions;
    }
    
}
