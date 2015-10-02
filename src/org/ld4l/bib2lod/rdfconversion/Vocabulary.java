package org.ld4l.bib2lod.rdfconversion;

public enum Vocabulary {

    FAST("http://id.worldcat.org/fast/"),
    WORLDCAT("http://www.worldcat.org/oclc/");
    
    private final String uri;
    
    Vocabulary(String uri) {
        this.uri = uri;
    }
    
    public String uri() {
        return uri;
    }
    
}
