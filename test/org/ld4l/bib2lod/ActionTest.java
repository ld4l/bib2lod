package org.ld4l.bib2lod;

import java.util.EnumSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.ld4l.bib2lod.rdfconversion.BibframeConverter;
import org.ld4l.bib2lod.rdfconversion.ResourceDeduper;
import org.ld4l.bib2lod.rdfconversion.TypeSplitter;

public class ActionTest {

    /*
     * NB The following four tests are based on modified test values for 
     * Action values, since the current actual values do not demonstrate
     * recursive prereqs.
     * SPLIT_TYPES("split_types", TypeSplitter.class),  
     * DEDUPE_RESOURCES("dedupe", ResourceDeduper.class),
     * CONVERT_BIBFRAME("convert_bibframe", BibframeConverter.class);
     */
    
    @Test
    public void testPrereqs() {
        
        Set<Action> actions = EnumSet.of(Action.DEDUPE_RESOURCES);
        Action a = Action.CONVERT_BIBFRAME;       
        Set<Action> prereqs = a.prereqs();           
        Assert.assertEquals(actions, prereqs);   
    }

    @Test
    public void testNoPrereqs() {
        Set<Action> actions = EnumSet.noneOf(Action.class);
        Action a = Action.SPLIT_TYPES;
        Set<Action> prereqs = a.prereqs();
        Assert.assertEquals(actions, prereqs);        
    }
    
    @Test
    public void testRecursivePrereqs() {
        Set<Action> actions = EnumSet.of(Action.DEDUPE_RESOURCES, 
                Action.SPLIT_TYPES);
        Action a = Action.CONVERT_BIBFRAME;
        Set<Action> recursivePrereqs = a.recursivePrereqs();
        Assert.assertEquals(actions, recursivePrereqs);
    }
    
    @Test
    public void testNoRecursivePrereqs() {
        Set<Action> actions = EnumSet.noneOf(Action.class);
        Action a = Action.SPLIT_TYPES;
        Set<Action> recursivePrereqs = a.recursivePrereqs();
        Assert.assertEquals(actions, recursivePrereqs);
    }

}
