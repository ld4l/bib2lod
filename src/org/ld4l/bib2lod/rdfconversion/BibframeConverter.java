package org.ld4l.bib2lod.rdfconversion;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfEventConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfFamilyConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfHeldItemConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfMeetingConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfOrganizationConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfPersonConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfPlaceConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfResourceConverter;

/**
 * Converts Bibframe RDF to LD4L RDF.
 * @author rjy7
 *
 */
public class BibframeConverter extends RdfProcessor {

    private static final Logger LOGGER = 
            LogManager.getLogger(BibframeConverter.class);
      
    private static final Map<BfType, Class<?>> CONVERTERS_BY_TYPE =
            new HashMap<BfType, Class<?>>();
    static {
//        CONVERTERS_BY_TYPE.put(BfType.BF_ANNOTATION, 
//                BfResourceConverter.class);    
        CONVERTERS_BY_TYPE.put(BfType.BF_EVENT, BfEventConverter.class);
        CONVERTERS_BY_TYPE.put(BfType.BF_FAMILY, BfFamilyConverter.class);
        CONVERTERS_BY_TYPE.put(BfType.BF_HELD_ITEM, 
                BfHeldItemConverter.class);
//        CONVERTERS_BY_TYPE.put(BfType.BF_IDENTIFIER, 
//                BfResourceConverter.class);
//        CONVERTERS_BY_TYPE.put(BfType.BF_INSTANCE, BfResourceConverter.class);
//        CONVERTERS_BY_TYPE.put(BfType.BF_JURISDICTION,  
//                BfResourceConverter.class);
        CONVERTERS_BY_TYPE.put(BfType.BF_MEETING, BfMeetingConverter.class);
        CONVERTERS_BY_TYPE.put(BfType.BF_ORGANIZATION, 
                BfOrganizationConverter.class);        
        CONVERTERS_BY_TYPE.put(BfType.BF_PERSON, BfPersonConverter.class);
        CONVERTERS_BY_TYPE.put(BfType.BF_PLACE, BfPlaceConverter.class);
//        CONVERTERS_BY_TYPE.put(BfType.BF_PROVIDER, BfResourceConverter.class);
//        CONVERTERS_BY_TYPE.put(BfType.BF_TITLE, BfResourceConverter.class);
//        CONVERTERS_BY_TYPE.put(BfType.BF_TOPIC, BfResourceConverter.class);
//        CONVERTERS_BY_TYPE.put(BfType.BF_WORK,  BfResourceConverter.class);
//        CONVERTERS_BY_TYPE.put(BfType.MADSRDF_AUTHORITY, 
//                BfResourceConverter.class);
//        CONVERTERS_BY_TYPE.put(BfType.MADSRDF_COMPLEX_SUBJECT,  
//                BfResourceConverter.class); 
    }
    
//    private static final String IN_PROGRESS_DIR = "inProgress";
    private static final String NEW_ASSERTIONS_FILENAME = 
            ResourceDeduper.getNewAssertionsFilename();
    
    
    public BibframeConverter(OntModel bfOntModelInf,        
            String localNamespace, String inputDir, String mainOutputDir) {
        super(bfOntModelInf, localNamespace, inputDir, mainOutputDir);
    }

    public BibframeConverter(String localNamespace, String inputDir,
            String mainOutputDir) {
        super(localNamespace, inputDir, mainOutputDir);
    }


    
    @Override
    public String process() {

        LOGGER.info("Start Bibframe RDF conversion.");
        
        String outputDir = getOutputDir();  

        convertFiles(inputDir, outputDir);

        LOGGER.info("End Bibframe RDF conversion.");
        return outputDir;        
    }
    
    private void convertFiles(String inputDir, String outputDir) {
        
        File[] inputFiles = new File(inputDir).listFiles();
        
        for ( File file : inputFiles ) {
            convertFile(file);
        }  

        /*
         * Possibly we need other steps here: go back through files to do URI
         * replacements. This entails keeping separate models for statements 
         * that shouldn't undergo URI replacement: e.g., the new statements
         * we create linking ld4l entities to bf entities, where the latter
         * shouldn't undergo URI replacement.
         */
    }

    private void convertFile(File file) {
        
        String filename = file.getName();
        String outputFile = FilenameUtils.getBaseName(file.toString());
        
        // No need to convert new statements that have been added during
        // processing.
        if (outputFile.equals(NEW_ASSERTIONS_FILENAME)) {
            LOGGER.debug("Copying file " + filename);
            copyFile(file);
            return;
        }

        LOGGER.trace("Processing file " + filename);
        
        Model inputModel = readModelFromFile(file);  
        Model outputModel = ModelFactory.createDefaultModel();
        
        // Iterate over the subjects of the input model
        ResIterator subjects = inputModel.listSubjects();
        while (subjects.hasNext()) {

            Resource inputSubject = subjects.nextResource();
            
            // Create a model of statements related to this subject
            Model modelForSubject = ModelFactory.createDefaultModel();
            
            // Start with statements of which this subject is the subject
            modelForSubject.add(
                    inputModel.listStatements(inputSubject, null, 
                            (RDFNode) null));
            
            // Now add statements with the object of the previous statements as
            // subject.
            NodeIterator nodes = modelForSubject.listObjects();
            while (nodes.hasNext()) {
                RDFNode node = nodes.nextNode();
                // NB We don't need node.isResource(), which includes bnodes as
                // well, since all nodes have been converted to URI resources.
                if (node.isURIResource()) {
                    modelForSubject.add(inputModel.listStatements(
                            (Resource) node, null, (RDFNode) null));
                }
            }

            
            // NB At this point, subject.getModel() is the inputModel, not the
            // modelForSubject. Get the subject of the subjectModel instead.            
            Resource subject = 
                    modelForSubject.getResource(inputSubject.getURI());
            
            // Get the converter according to the type of the subject
            // *** TODO Don't need to pass in model - get from subject
            BfResourceConverter converter = 
                    getConverter(subject, modelForSubject);
            
            if (converter == null) {
                LOGGER.trace("No converter found for subject " 
                        + subject.getURI());
                outputModel.add(modelForSubject);
            } else {
                outputModel.add(converter.convert());
            }
        }

        writeModelToFile(outputModel, outputFile);
        
    }
    
    /*
     * If as the last step in ResourceDeduper we type-split again, separating 
     * out all statements by subject type (i.e., put Titles into their own file
     * rather than combined with bfWork or bfInstance, etc.), then we could
     * derive the converter type from the filename, as for ResourceDeduper.
     * (However, if there's still an other.nt file we'd have to deal with that
     * as here.) Consider implementing this step.
     * On the other hand, there's an advantage that in the current 
     * implementation it doesn't matter how the input files are structured - we
     * wouldn't even need the initial type-splitting or deduping, so the 
     * processes are more independent. (We could eliminate the prerequisite on
     * Bibframe conversion, though deduping still depends on type-splitting.)
     */
    private BfResourceConverter getConverter(Resource subject, Model model) {
            
        // Loop through types to find one that is the subject's type.
        // Will need modification if ordering of types is crucial.
        for (Map.Entry<BfType, Class<?>> entry : 
                CONVERTERS_BY_TYPE.entrySet()) {
            
            BfType type = entry.getKey();
            Resource ontClass = model.createResource(type.uri());

            if (model.contains(null, RDF.type, ontClass)) {
                LOGGER.debug("Found converter class for subject " 
                        + subject.getURI() + " of type " + type.uri());                        
                return createConverter(type, entry.getValue(), subject);
            }
        }
        
        LOGGER.debug("No converter found for subject " + subject.getURI());
        return null;
    }

    private BfResourceConverter createConverter(
            BfType type, Class<?> converterClass, Resource subject) {
        
        // NB Currently a new converter is created for each subject resource,
        // so the subject can be assigned to an instance variable. If we 
        // change the flow so that a converter is created for an entire model of
        // subjects of a certain type, the subject will have to be passed to the
        // convert method rather than the constructor.
        try {
            // Possibly a converter could be shared by multiple types - e.g., 
            // BfFamilyConverter and BfOrganizationConverter could both be
            // BfAuthorityConverter. Then the original rdf:type must be passed
            // to the constructor so that we know what new type should be 
            // assigned.
            BfResourceConverter converter = 
                    (BfResourceConverter) converterClass
                    .getConstructor(Resource.class)
                    .newInstance(subject);   
            return converter;      
        } catch (Exception e) {
            LOGGER.trace("No converter created for class " 
                    + converterClass.getName());
            e.printStackTrace();
        }    
        
        return null;
    }
    


}
