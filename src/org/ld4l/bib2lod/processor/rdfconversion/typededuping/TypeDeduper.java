package org.ld4l.bib2lod.processor.rdfconversion.typededuping;

import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// TODO If only abstract methods, change to an interface
public abstract class TypeDeduper {

    private static final Logger LOGGER = LogManager.getLogger(TypeDeduper.class);
    
    public abstract Map<String, String> dedupe(Model model);

}
