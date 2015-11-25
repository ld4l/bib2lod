package org.ld4l.bib2lod.rdfconversion;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfAuthorityConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfEventConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfHeldItemConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfInstanceConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfLanguageConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfMeetingConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfPersonConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfResourceConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfTopicConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfWorkConverter;

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
        CONVERTERS_BY_TYPE.put(BfType.BF_AGENT, BfAuthorityConverter.class);
//        CONVERTERS_BY_TYPE.put(BfType.BF_ANNOTATION, 
//                BfResourceConverter.class);    
        CONVERTERS_BY_TYPE.put(BfType.BF_EVENT, BfEventConverter.class);
        CONVERTERS_BY_TYPE.put(BfType.BF_FAMILY, BfAuthorityConverter.class);
        CONVERTERS_BY_TYPE.put(BfType.BF_HELD_ITEM, 
                BfHeldItemConverter.class);
//        CONVERTERS_BY_TYPE.put(BfType.BF_IDENTIFIER, 
//                BfResourceConverter.class);
        CONVERTERS_BY_TYPE.put(BfType.BF_INSTANCE, BfInstanceConverter.class);
        CONVERTERS_BY_TYPE.put(BfType.BF_JURISDICTION,  
                BfAuthorityConverter.class);
        CONVERTERS_BY_TYPE.put(BfType.BF_LANGUAGE, BfLanguageConverter.class);
        CONVERTERS_BY_TYPE.put(BfType.BF_MEETING, BfMeetingConverter.class);
        CONVERTERS_BY_TYPE.put(BfType.BF_ORGANIZATION, 
                BfAuthorityConverter.class);        
        CONVERTERS_BY_TYPE.put(BfType.BF_PERSON, BfPersonConverter.class);
        CONVERTERS_BY_TYPE.put(BfType.BF_PLACE, BfAuthorityConverter.class);
//        CONVERTERS_BY_TYPE.put(BfType.BF_PROVIDER, BfResourceConverter.class);
        CONVERTERS_BY_TYPE.put(BfType.BF_TEMPORAL, BfAuthorityConverter.class);
//      CONVERTERS_BY_TYPE.put(BfType.BF_TITLE, BfResourceConverter.class);       
        // Maybe can just use BfAuthorityConverter for Topics?
        CONVERTERS_BY_TYPE.put(BfType.BF_TOPIC, BfTopicConverter.class);
        CONVERTERS_BY_TYPE.put(BfType.BF_WORK,  BfWorkConverter.class);
//        CONVERTERS_BY_TYPE.put(BfType.MADSRDF_AUTHORITY, 
//                BfResourceConverter.class);
//        CONVERTERS_BY_TYPE.put(BfType.MADSRDF_COMPLEX_SUBJECT,  
//                BfResourceConverter.class); 
    }
    
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
        
        LOGGER.debug("Processing file " + filename);

        BfType typeForFile = BfType.typeForFilename(filename);
        
        // Includes newAssertions.nt, other.nt
        if (typeForFile == null) {
            LOGGER.debug("No type for file " + filename);
            copyFile(file);
            return;
        }

        Model inputModel = readModelFromFile(file); 
        
        LOGGER.debug("Got model of size : " + inputModel.size() 
                + " for type " + typeForFile);
        
        BfResourceConverter converter = getConverter(typeForFile);
        
        // If no converter for this type, write the input model as output and
        // return
        if (converter == null) {
            writeModelToFile(inputModel, outputFile);
            return;
        }
        
        Model outputModel = ModelFactory.createDefaultModel();
        
        // Iterate over the subjects of the input model
        ResIterator subjects = inputModel.listSubjects();
        while (subjects.hasNext()) {

            Resource inputSubject = subjects.nextResource();
            
            LOGGER.debug("Found subject " + inputSubject.getURI());

            if (inputSubject.hasProperty(RDF.type, typeForFile.ontClass())) {

                LOGGER.debug("Converting " + typeForFile + " subject " 
                        + inputSubject.getURI());
                
                Resource subject = getSubjectModelToConvert(inputSubject);
                
                outputModel.add(converter.convert(subject));
                
            } else {
                LOGGER.debug("Skipping subject " + inputSubject.getURI()
                        + " since it's not of type " + typeForFile);                        
            }
        }

        writeModelToFile(outputModel, outputFile);       
    }
    
    /**
     * Create a subset of the input model that pertains to the specified 
     * resource. This includes statements where the resource is the subject
     * or the object.
     */
    public static Resource getSubjectModelToConvert(Resource resource) {
        
        Model inputModel = resource.getModel();
        
        // Create the new model of statements related to this subject
        Model modelForSubject = ModelFactory.createDefaultModel();
        
        // Start with statements of which this subject is the subject
        modelForSubject.add(resource.listProperties());

        // Now add statements with the object of the previous statements 
        // as subject. Which statements are included has been determined by
        // the query for the resource type in TypeSplitter.
        NodeIterator nodes = modelForSubject.listObjects();
        while (nodes.hasNext()) {
            RDFNode node = nodes.nextNode();
            // NB We don't need node.isResource(), which includes bnodes 
            // as well, since all nodes have been converted to URI 
            // resources.
            if (node.isURIResource()) {
                modelForSubject.add(inputModel.listStatements(
                        node.asResource(), null, (RDFNode) null));
            }
        }
        
        // Finally, add statements with the current resource as object.
        // Examples:
        // :work bf:language :language
        // :annotation bf:annotates :work
        modelForSubject.add(
                inputModel.listStatements(null, null, resource));
        
        // The converted model, with assertions and retractions applied,  will 
        // be returned by the converter. Other converters will not need to 
        // process these statements, so they can be removed from the input 
        // model.
        LOGGER.debug(resource.getURI());
        LOGGER.debug("before removing modelForSubject: " + inputModel.size());
        inputModel.remove(modelForSubject);
        LOGGER.debug("after removing modelForSubject: " + inputModel.size());
           
        // NB At this point, resource.getModel() is the inputModel, not 
        // the modelForSubject. Get the subject of the modelForSubject 
        // instead.            
        return modelForSubject.getResource(resource.getURI());               
    } 
    
    private BfResourceConverter getConverter(BfType bfType) {
        
        Class<?> converterClass = CONVERTERS_BY_TYPE.get(bfType);
        if (converterClass != null) {
            try {
                BfResourceConverter converter = 
                        (BfResourceConverter) converterClass
                        .getConstructor(BfType.class, String.class)
                        .newInstance(bfType, localNamespace);   
                LOGGER.debug("Got converter for type " + bfType);
                return converter;
            } catch (Exception e) {
                LOGGER.debug("Can't instantiate class " 
                        + converterClass.getName());
                e.printStackTrace();
            } 
        }

        LOGGER.debug("No converter found for type " + bfType);
        return null;
    }

}
