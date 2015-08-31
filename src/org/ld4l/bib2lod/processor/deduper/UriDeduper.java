package org.ld4l.bib2lod.processor.deduper;

import org.apache.jena.ontology.OntModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.processor.Processor;
import org.ld4l.bib2lod.processor.filesplitter.TypeSplitter;

// May need to be abstract - we only instantiate PersonDeduper, WorkDeduper, etc.
public class UriDeduper extends Processor {

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger(TypeSplitter.class);
    private static final String outputSubDir = "deduped";
    
    // TODO Like TypeSplitter, we want to define types to dedupe on. Exclude,
    // for example, annotations, held items, titles, since these are not 
    // re-used.
    
    public UriDeduper(OntModel bfOntModelInf, String localNamespace, 
            String inputDir, String mainOutputDir) {
                 
        super(bfOntModelInf, localNamespace, inputDir, mainOutputDir);

    }
    


    @Override
    public String process() {
        return stubProcess(outputSubDir);
    }

}