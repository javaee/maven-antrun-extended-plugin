package org.jvnet.maven.plugin.antrun;

/**
 * {@link ListFilter} is a special kind of {@link GraphFilter}
 * that only filters graph based on {@link DependencyGraph.Node}.
 *
 * <p>
 * This kind of filters can be applied on a list of nodes.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class ListFilter extends GraphFilter implements GraphVisitor {
    public final DependencyGraph process() {
        // Create a subgraph of the dependencyGraph by using this class as a
        // GraphVisitor.  The visit(node) and visit(edge) methods are called
        // by the immutable DependencyGraph instance to construct the subgraph.

        // when this class is used as a list filter, this method won't be invoked,
        // so don't let derived classes shoot themselves in the foot by overriding this.
        return evaluateChild().createSubGraph(this);
    }

    public final boolean visit(DependencyGraph.Edge edge) {
        return true;
    }
}
