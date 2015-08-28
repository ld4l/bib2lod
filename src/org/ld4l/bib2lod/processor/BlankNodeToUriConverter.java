package org.ld4l.bib2lod.processor;

import org.apache.jena.ontology.OntModel;

public class BlankNodeToUriConverter extends Processor {

    public BlankNodeToUriConverter(OntModel bfOntModelInf, 
            String localNamespace, String inputDir, String mainOutputDir) {
            
        super(bfOntModelInf, localNamespace, inputDir, mainOutputDir);
        // TODO Auto-generated constructor stub
    }

    @Override
    public String process() {
        // Convert blank nodes to URIs
        return null;
    }

}
