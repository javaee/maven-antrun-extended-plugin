package org.jvnet.maven.plugin.antrun;

/**
 * Traverses a {@link DependencyGraph} in a depth-first order.
 * All the reachable nodes and edges are visited.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class GraphVisitor {
    /**
     * Visits an edge.
     *
     * @return
     *      false to cut the traversal here and don't visit
     *      its destination node.
     */
    public boolean visit(DependencyGraph.Edge edge) {
        return true;
    }

    /**
     * Visits a node.
     *
     * @return
     *      false to cut the traversal here and don't visit
     *      any of forward edges from this node.
     */
    public boolean visit(DependencyGraph.Node node) {
        return true;
    }
}
