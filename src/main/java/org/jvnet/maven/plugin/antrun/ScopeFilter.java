package org.jvnet.maven.plugin.antrun;


import org.apache.tools.ant.BuildException;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Filter out a {@link DependencyGraph} by only traversing the given scope.
 *
 * @author Kohsuke Kawaguchi
 * @author Paul Sterk
 */
public final class ScopeFilter extends GraphFilter implements GraphVisitor {
    private final Set<String> scopes = new HashSet<String>();

    public ScopeFilter(Collection<String> scopes) {
        this.scopes.addAll(scopes);
    }

    public ScopeFilter(String... scopes) {
        this(Arrays.asList(scopes));
    }

    // needed for Ant
    public ScopeFilter() {
    }

    public void setLevel(String level) {
        if(level.equals("compile")) {
            scopes.addAll(Arrays.asList("provided","system","compile"));
            return;
        }
        if(level.equals("runtime")) {
            scopes.addAll(Arrays.asList("provided","system","compile","runtime"));
            return;
        }
        if(level.equals("test")) {
            scopes.addAll(Arrays.asList("provided","system","compile","runtime","test"));
            return;
        }
        throw new BuildException("Illegal value: "+level);
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
