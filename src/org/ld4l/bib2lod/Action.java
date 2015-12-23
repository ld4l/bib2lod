package org.ld4l.bib2lod;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ld4l.bib2lod.rdfconversion.BibframeConverter;
import org.ld4l.bib2lod.rdfconversion.RdfCleaner;
import org.ld4l.bib2lod.rdfconversion.ResourceDeduper;

public enum Action {

    // Not yet implemented:
    // GET_MARC("get_marc"),
    // MARC2MARCXML("marc2marcxml"),
    // Clean up MARCXML records: correct known errors, enhance with ??
    // PREPROCESS_MARCXML("preprocess"),
    // MARCXML2BIBFRAME("marcxml2bibframe"),
    
    // Remove these because they are not independent actions, but actions that
    // ResourceDeduper is dependent on.
    // CONVERT_BNODES("convert_bnodes", BnodeConverter.class),
    // SPLIT_TYPES("split_types", TypeSplitter.class),
    
    CLEAN_RDF("clean_rdf", RdfCleaner.class),
    DEDUPE_RESOURCES("dedupe", ResourceDeduper.class),
    CONVERT_BIBFRAME("convert_bibframe", BibframeConverter.class);
    // RESOLVE_TO_EXTERNAL_ENTITIES);
    
    private final String label;    
    private final Class<?> processorClass;

    Action(String action, Class<?> processorClass) {
        this.label = action;
        this.processorClass = processorClass;
    }

    public String label() {
        return this.label;
    }
    
    public Class<?> processorClass() {
        return this.processorClass;
    }
    
    public EnumSet<Action> prereqs() {
        EnumSet<Action> prereqs = EnumSet.noneOf(Action.class);
        if (this.equals(CONVERT_BIBFRAME)) {
            prereqs.add(DEDUPE_RESOURCES);
        } else if (this.equals(DEDUPE_RESOURCES)) {
            prereqs.add(CLEAN_RDF);
        }
        return prereqs;
    }
    
    public EnumSet<Action> recursivePrereqs() {
        EnumSet<Action> recursivePrereqs = this.prereqs();
        for (Action p : recursivePrereqs) {
            recursivePrereqs.addAll(p.recursivePrereqs());
        }     
        return recursivePrereqs;
    }
     
    private static final Map<String, Action> LOOKUP_BY_LABEL = 
            new HashMap<String, Action>();
    
    private static final List<String> VALID_ACTIONS = 
            new ArrayList<String>();

    static {
        for (Action action : Action.values()) {
            String label = action.label;
            LOOKUP_BY_LABEL.put(label, action);
            VALID_ACTIONS.add(label);
        }
    }

    public static Action get(String action) { 
        return LOOKUP_BY_LABEL.get(action); 
    }
    

    public static List<String> validActions() {
        return VALID_ACTIONS;
    }
    
}
