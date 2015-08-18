package org.ld4l.bib2lod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum Action {

    // GET_MARC ("get_marc", 1),
    // MARC2MARCXML ("marc2marcxml", 2),
    // Clean up MARCXML records: correct known errors, enhance with ??
    // PREPROCESS_MARCXML ("preprocess", 3),
    // MARCXML2BIBFRAME ("marcxml2bibframe", 4),
    DEDUPE_BIBFRAME_URIS ("dedupe", 5);
    // BIBFRAME2LD4L ("bibframe2ld4l, 6");
    
    private final String action;    
    // Use for ordering actions
    private final Integer rank;

    // TODO Can we make this an int rather than an Integer
    Action(String action, Integer rank) {
        this.action = action;
        this.rank = rank;
    }
    
    public String action() {
        return this.action;
    }
    
    public Integer rank() {
        return this.rank;
    }
    
    private static final Map<String, Action> lookup = 
            new HashMap<String, Action>();

    private static final Map<Integer, Action> byRank = 
            new HashMap<Integer, Action>();
    
    private static List<String> validActions = 
            new ArrayList<String>();

    static {
        for (Action actionValue : Action.values()) {
            String action = actionValue.action();
            lookup.put(action, actionValue);
            Integer actionRank = actionValue.rank();
            byRank.put(actionRank, actionValue);
            validActions.add(action);
        }
    }

    public static Action get(String action) { 
        return lookup.get(action); 
    }
    
    public static Action getByRank(Integer rank) {
        return byRank.get(rank);
    }
    
    public static List<String> validActions() {
        return validActions;
    }
    

    

    
    public static Action getByRank(int rank) {
        return byRank.get(rank);
        
    }
    
}
