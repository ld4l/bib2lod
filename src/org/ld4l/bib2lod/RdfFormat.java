package org.ld4l.bib2lod;

import org.apache.jena.riot.RDFFormat;

public enum RdfFormat {

    NTRIPLES("ntriples", RDFFormat.NTRIPLES, "nt"),
    RDFXML("rdfxml", RDFFormat.RDFXML, "rdf");

    private final String label;
    private final RDFFormat format;
    private final String extension;
    
    RdfFormat(String label, RDFFormat format, String extension) {
        this.label = label;
        this.format = format;
        this.extension = extension;
    }
    
    public String label() {
        return this.label;
    }
    
    public RDFFormat format() {
        return this.format;
    }
    
    public String extension() {
        return this.extension;
    }
    
}
