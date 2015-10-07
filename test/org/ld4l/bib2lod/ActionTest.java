package org.ld4l.bib2lod;

import java.util.EnumSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

public class ActionTest {


    
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
        Action a = Action.DEDUPE_RESOURCES;
        Set<Action> prereqs = a.prereqs();
        Assert.assertEquals(actions, prereqs);        
    }
    
    @Test
    public void testRecursivePrereqs() {
        Set<Action> actions = EnumSet.of(Action.DEDUPE_RESOURCES);
        Action a = Action.CONVERT_BIBFRAME;
        Set<Action> recursivePrereqs = a.recursivePrereqs();
        Assert.assertEquals(actions, recursivePrereqs);
    }
    
    @Test
    public void testNoRecursivePrereqs() {
        Set<Action> actions = EnumSet.noneOf(Action.class);
        Action a = Action.DEDUPE_RESOURCES;
        Set<Action> recursivePrereqs = a.recursivePrereqs();
        Assert.assertEquals(actions, recursivePrereqs);
    }

}
