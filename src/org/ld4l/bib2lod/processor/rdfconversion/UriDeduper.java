package org.ld4l.bib2lod.processor.rdfconversion;

import java.util.Arrays;
import java.util.List;

import org.apache.jena.ontology.OntModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.OntologyType;
import org.ld4l.bib2lod.RdfFormat;
import org.ld4l.bib2lod.processor.Processor;

// May need to be abstract - we only instantiate PersonDeduper, WorkDeduper, etc.
public class UriDeduper extends Processor {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LogManager.getLogger(UriDeduper.class);

    /* Define types eligible for deduping.  Two kinds of types are excluded:
     * those where instances are not reused (e.g., Annotation, Title, HeldItem),
     * and those which are not likely to have independent significance (e.g.,
     * Provider is just a carrier for publication data - and more aptly named 
     * Provision). Some of these types are included in the triples for their
     * related type so that they can be used as a basis for deduping: e.g., 
     * Identifiers for Instances, Titles for Works and Instances).
     */
    // Maybe should be List<OntologyType>?
    private static final List<String> TYPES_TO_DEDUPE = Arrays.asList(
            // OntologyType.BF_ANNOTATION.uri(),
            OntologyType.BF_FAMILY.uri(),
            // OntologyType.BF_HELD_ITEM.uri(),
            OntologyType.BF_INSTANCE.uri(),
            OntologyType.BF_JURISDICTION.uri(),
            OntologyType.BF_MEETING.uri(),
            OntologyType.BF_ORGANIZATION.uri(),
            OntologyType.BF_PERSON.uri(),
            //OntologyType.BF_PROVIDER.uri(),
            OntologyType.BF_PLACE.uri(),
            OntologyType.BF_TOPIC.uri(),
            OntologyType.BF_WORK.uri()         
    );

    private static final RdfFormat RDF_OUTPUT_FORMAT = RdfFormat.NTRIPLES;
    
    public UriDeduper(OntModel bfOntModelInf, String localNamespace, 
            String inputDir, String mainOutputDir) {
                 
        super(bfOntModelInf, localNamespace, inputDir, mainOutputDir);

    }
    
    protected static List<String> getTypesToDedupe() {
        return TYPES_TO_DEDUPE;
    }

    @Override
    protected RdfFormat getRdfOutputFormat() {
        return RDF_OUTPUT_FORMAT;
    }
    
    @Override
    public String process() {
        LOGGER.trace("Start process");
        String outputDir = stubProcess();
        LOGGER.trace("End process");
        return outputDir;
    }

}