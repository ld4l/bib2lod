package org.ld4l.bib2lod.rdfconversion;

public enum Vocabulary {

    FAST("http://id.worldcat.org/fast/", "fast"),
    WORLDCAT("http://www.worldcat.org/oclc/", "worldcat"),
    
    // Invented vocabularies for demonstration purposes 
    DDC("http://id.loc.gov/ddc/", "ddc"),
    LCC("http://id.loc.gov/lcc/", "lcc"),
    NLM("http://id.loc.gov/nlm/", "nlm"),
    UDC("http://id.loc.gov/udc/", "udc");
    
    private final String uri;
    private final String label;
    
    Vocabulary(String uri, String label) {
        this.uri = uri;
        this.label = label;
    }
    
    Vocabulary(String uri) {
        this.uri = uri;
        this.label = null;
    }
    
    public String uri() {
        return uri;
    }
    
    public String label() {
        return label;
    }
    
}
