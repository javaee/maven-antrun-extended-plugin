package org.jvnet.maven.plugin.antrun;

import org.jvnet.maven.plugin.antrun.DependencyGraph.Edge;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * Filter out a {@link DependencyGraph} by only traversing the given scope.
 *
 * @author Kohsuke Kawaguchi
 */
public final class ScopeFilter extends GraphVisitor {
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

    public boolean visit(Edge edge) {
        return scopes.contains(edge.scope);
    }
}
