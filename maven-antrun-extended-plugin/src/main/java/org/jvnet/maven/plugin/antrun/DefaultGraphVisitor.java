package org.jvnet.maven.plugin.antrun;

/**
 * Partial {@link GraphVisitor} implementation that returns true.
 * 
 * @author Kohsuke Kawaguchi
 */
public class DefaultGraphVisitor implements GraphVisitor {
    public boolean visit(DependencyGraph.Edge edge) {
        return true;
    }

    public boolean visit(DependencyGraph.Node node) {
        return true;
    }
}
