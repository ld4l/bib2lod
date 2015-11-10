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
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfEventConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfFamilyConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfHeldItemConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfInstanceConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfLanguageConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfMeetingConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfOrganizationConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfPersonConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfPlaceConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfResourceConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfTemporalConverter;
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
//        CONVERTERS_BY_TYPE.put(BfType.BF_ANNOTATION, 
//                BfResourceConverter.class);    
        CONVERTERS_BY_TYPE.put(BfType.BF_EVENT, BfEventConverter.class);
        CONVERTERS_BY_TYPE.put(BfType.BF_FAMILY, BfFamilyConverter.class);
        CONVERTERS_BY_TYPE.put(BfType.BF_HELD_ITEM, 
                BfHeldItemConverter.class);
//        CONVERTERS_BY_TYPE.put(BfType.BF_IDENTIFIER, 
//                BfResourceConverter.class);
        CONVERTERS_BY_TYPE.put(BfType.BF_INSTANCE, BfInstanceConverter.class);
//        CONVERTERS_BY_TYPE.put(BfType.BF_JURISDICTION,  
//                BfResourceConverter.class);
        CONVERTERS_BY_TYPE.put(BfType.BF_LANGUAGE, BfLanguageConverter.class);
        CONVERTERS_BY_TYPE.put(BfType.BF_MEETING, BfMeetingConverter.class);
        CONVERTERS_BY_TYPE.put(BfType.BF_ORGANIZATION, 
                BfOrganizationConverter.class);        
        CONVERTERS_BY_TYPE.put(BfType.BF_PERSON, BfPersonConverter.class);
        CONVERTERS_BY_TYPE.put(BfType.BF_PLACE, BfPlaceConverter.class);
//        CONVERTERS_BY_TYPE.put(BfType.BF_PROVIDER, BfResourceConverter.class);
        CONVERTERS_BY_TYPE.put(BfType.BF_TEMPORAL, BfTemporalConverter.class);
//      CONVERTERS_BY_TYPE.put(BfType.BF_TITLE, BfResourceConverter.class);
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
        
        // No need to convert new statements that have been added during
        // processing.
        if (outputFile.equals(NEW_ASSERTIONS_FILENAME)) {
            LOGGER.debug("Copying file " + filename);
            copyFile(file);
            return;
        }

        LOGGER.debug("Processing file " + filename);
        
        Model inputModel = readModelFromFile(file);  
     
        BfType typeForFile = BfType.typeForFilename(filename);
        BfResourceConverter converter = getConverter(typeForFile);
        
        // If no converter for this type, write the input model as output and
        // return
        if (converter == null) {
            writeModelToFile(inputModel, outputFile);
            return;
        }
        
        Model outputModel = ModelFactory.createDefaultModel();
        
        
        LOGGER.debug(inputModel.toString());
        // Iterate over the subjects of the input model
        ResIterator subjects = inputModel.listSubjects();
        while (subjects.hasNext()) {

            Resource inputSubject = subjects.nextResource();
            
            LOGGER.debug("Found subject " + inputSubject.getURI());

            if (inputSubject.hasProperty(RDF.type, typeForFile.ontClass())) {

                LOGGER.debug("Converting " + typeForFile + " subject " 
                        + inputSubject.getURI());
                
                // Create a model of statements related to this subject
                Model modelForSubject = ModelFactory.createDefaultModel();
                
                // Start with statements of which this subject is the subject
                modelForSubject.add(inputSubject.listProperties());
    
                // Now add statements with the object of the previous statements 
                // as subject.
                NodeIterator nodes = modelForSubject.listObjects();
                while (nodes.hasNext()) {
                    RDFNode node = nodes.nextNode();
                    // NB We don't need node.isResource(), which includes bnodes 
                    // as well, since all nodes have been converted to URI 
                    // resources.
                    if (node.isURIResource()) {
                        modelForSubject.add(inputModel.listStatements(
                                (Resource) node, null, (RDFNode) null));
                    }
                }
                
                // Finally, add statements with this subject as object.
                // So far we only have these in bfLanguage.nt, where we get the
                // :work bf:language :language statement.
                modelForSubject.add(
                        inputModel.listStatements(null, null, inputSubject));
           
                // NB At this point, subject.getModel() is the inputModel, not 
                // the modelForSubject. Get the subject of the subjectModel 
                // instead.            
                Resource subject = 
                        modelForSubject.getResource(inputSubject.getURI());

                outputModel.add(converter.convert(subject));

                
            } else {
                LOGGER.debug("Skipping subject " + inputSubject.getURI()
                        + " since it's not of type " + typeForFile);
                        
            }
        }

        writeModelToFile(outputModel, outputFile);
        
    }
    
    
    private BfResourceConverter getConverter(BfType type) {
        
        Class<?> converterClass = CONVERTERS_BY_TYPE.get(type);
        if (converterClass != null) {
            try {
                BfResourceConverter converter = 
                        (BfResourceConverter) converterClass.newInstance();
                LOGGER.debug("Got converter for type " + type);
                return converter;
            } catch (Exception e) {
                LOGGER.debug("Can't instantiate class " 
                        + converterClass.getName());
                e.printStackTrace();
            } 
        }

        LOGGER.debug("No converter found for type " + type);
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
