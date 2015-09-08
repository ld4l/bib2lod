package org.ld4l.bib2lod.processor.deduper;

import org.apache.jena.ontology.OntModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.processor.Processor;
import org.ld4l.bib2lod.processor.filesplitter.TypeSplitter;

// May need to be abstract - we only instantiate PersonDeduper, WorkDeduper, etc.
public class UriDeduper extends Processor {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LogManager.getLogger(TypeSplitter.class);
    
    // TODO Like TypeSplitter, we want to define types to dedupe on. Exclude,
    // for example, annotations, held items, titles, since these are not 
    // re-used. BUT they must be the same. We only split on the types we want
    // to dedupe. Other triples need to go with those items. E.g., the Titles
    // need to go with their Works/Instances.
    
    public UriDeduper(OntModel bfOntModelInf, String localNamespace, 
            String inputDir, String mainOutputDir) {
                 
        super(bfOntModelInf, localNamespace, inputDir, mainOutputDir);

    }
    
    @Override
    public String process() {
        return stubProcess();
    }

}