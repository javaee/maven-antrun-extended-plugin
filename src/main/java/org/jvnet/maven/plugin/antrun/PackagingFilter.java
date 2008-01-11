package org.jvnet.maven.plugin.antrun;


/**
 * Filter out a {@link DependencyGraph} by only traversing the given packaging.
 *
 * @author Paul Sterk
 */
public final class PackagingFilter extends GraphFilter implements GraphVisitor {
    private String packaging;
    private String packagingNot;

    public void setValue(String v) {
        packaging = v;
    }

    public void setNot(String v) {
        packagingNot = v;
    }

    public DependencyGraph process() {
        // Create a subgraph of the dependencyGraph by using this class as a 
        // GraphVisitor.  The visit(node) and visit(edge) methods are called
        // by the immutable DependencyGraph instance to construct the subgraph.
        return evaluateChild().createSubGraph(this);
    }    
    
    public boolean visit(DependencyGraph.Node node) {
        String p = node.getProject().getPackaging();
        if(packaging!=null && packaging.equals(p))
            return true;    // positive match
        if(packagingNot!=null && !packagingNot.equals(p))
            return true;    // negative match

        return false;
    }

    public boolean visit(DependencyGraph.Edge edge) {
        return true;
    }
}
