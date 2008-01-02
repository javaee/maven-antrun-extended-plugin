package org.jvnet.maven.plugin.antrun;


import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * Filter out a {@link DependencyGraph} by only traversing the given packaging.
 *
 * @author Paul Sterk
 */
public final class PackagingFilter extends GraphFilter implements GraphVisitor {
    private final Collection<String> packagings;

    public PackagingFilter(Collection<String> packagings) {
        this.packagings = packagings;
    }

    public PackagingFilter(String... packagings) {
        this.packagings = Arrays.asList(packagings);
    }

    public PackagingFilter(String packaging) {
        this.packagings = Collections.singleton(packaging);
    }

    public DependencyGraph process() {
        // Create a subgraph of the dependencyGraph by using this class as a 
        // GraphVisitor.  The visit(node) and visit(edge) methods are called
        // by the immutable DependencyGraph instance to construct the subgraph.
        return evaluateChild().createSubGraph(this);
    }    
    
    public boolean visit(DependencyGraph.Node node) {
        String packaging = node.getProject().getPackaging();
        return packagings.contains(packaging);
    }

    public boolean visit(DependencyGraph.Edge edge) {
        return true;
    }
}
