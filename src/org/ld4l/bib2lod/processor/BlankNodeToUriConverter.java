package org.ld4l.bib2lod.processor;

import org.apache.jena.ontology.OntModel;
import org.ld4l.bib2lod.RdfFormat;

public class BlankNodeToUriConverter extends Processor {

    public BlankNodeToUriConverter(OntModel bfOntModelInf,
            String localNamespace, RdfFormat rdfFormat, String inputDir,
            String mainOutputDir) {
        super(bfOntModelInf, localNamespace, rdfFormat, inputDir, mainOutputDir);
        // TODO Auto-generated constructor stub
    }

    @Override
    public String process() {
        // Convert blank nodes to URIs
        return null;
    }

}
