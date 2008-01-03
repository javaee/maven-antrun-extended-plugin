package org.jvnet.maven.plugin.antrun;


import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;

/**
 * Filter out a {@link DependencyGraph} by only traversing the given packaging.
 *
 * @author Paul Sterk
 */
public final class PackagingFilter extends GraphFilter implements GraphVisitor {
    private final Set<String> packagings = new HashSet<String>();

    public PackagingFilter(Collection<String> packagings) {
        this.packagings.addAll(packagings);
    }

    public PackagingFilter(String... packagings) {
        this(Arrays.asList(packagings));
    }

    // needed for Ant
    public PackagingFilter() {
    }

    public void setValue(String v) {
        packagings.add(v);
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
