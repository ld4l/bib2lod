package org.ld4l.bib2lod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ld4l.bib2lod.processor.BNodeToUriConverter;
import org.ld4l.bib2lod.processor.Processor;
import org.ld4l.bib2lod.processor.deduper.UriDeduper;
import org.ld4l.bib2lod.processor.filesplitter.TypeSplitter;

public enum Action {

    // GET_MARC ("get_marc"),
    // MARC2MARCXML ("marc2marcxml"),
    // Clean up MARCXML records: correct known errors, enhance with ??
    // PREPROCESS_MARCXML ("preprocess"),
    // MARCXML2BIBFRAME ("marcxml2bibframe"),
    CONVERT_BNODES("convert_bnodes", BNodeToUriConverter.class),
    SPLIT_TYPES("split_types", TypeSplitter.class),
    DEDUPE_BIBFRAME_URIS ("dedupe", UriDeduper.class);
    // BIBFRAME2LD4L ("bibframe2ld4l");
    // RESOLVE_TO_EXTERNAL_ENTITIES);
    
    private final String action;    
    private final Class<?> processorClass;

    // TODO What is the type on Class? Could we make it <Processor>?
    Action(String action, Class<?> processorClass) {
        this.action = action;
        this.processorClass = processorClass;
    }
    
    public String action() {
        return this.action;
    }
    
    public Class<?> processorClass() {
        return this.processorClass;
    }
     
    private static final Map<String, Action> lookup = 
            new HashMap<String, Action>();
    
    private static List<String> validActions = 
            new ArrayList<String>();

    static {
        for (Action actionValue : Action.values()) {
            String action = actionValue.action();
            lookup.put(action, actionValue);
            validActions.add(action);
        }
    }

    public static Action get(String action) { 
        return lookup.get(action); 
    }
    

    public static List<String> validActions() {
        return validActions;
    }
    
}
