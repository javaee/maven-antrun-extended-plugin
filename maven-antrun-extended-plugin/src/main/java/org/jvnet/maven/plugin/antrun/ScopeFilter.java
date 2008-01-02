package org.jvnet.maven.plugin.antrun;


import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * Filter out a {@link DependencyGraph} by only traversing the given scope.
 *
 * @author Kohsuke Kawaguchi
 * @author Paul Sterk
 */
public final class ScopeFilter extends GraphFilter implements GraphVisitor {
    private final Collection<String> scopes;

    public ScopeFilter(Collection<String> scopes) {
        this.scopes = scopes;
    }

    public ScopeFilter(String... scopes) {
        this.scopes = Arrays.asList(scopes);
    }

    public ScopeFilter(String scope) {
        this.scopes = Collections.singleton(scope);
    }

    public DependencyGraph process() {
        // Create a subgraph of the dependencyGraph by using this class as a 
        // GraphVisitor.
        return evaluateChild().createSubGraph(this);
    }    
    
    public boolean visit(DependencyGraph.Edge edge) {
        return scopes.contains(edge.scope);
    }

    public boolean visit(DependencyGraph.Node node) {
        return true;
    }
}
