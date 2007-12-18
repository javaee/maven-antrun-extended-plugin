package org.jvnet.maven.plugin.antrun;

import org.jvnet.maven.plugin.antrun.DependencyGraph.Edge;
import org.jvnet.maven.plugin.antrun.DependencyGraph.Node;

import java.util.Arrays;
import java.util.Collection;

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

    /**
     * Combines multiple {@link GraphVisitor} by AND-ing its output.
     * Can be used to create intersections.
     */
    public static GraphVisitor and(GraphVisitor... visitors) {
        return and(Arrays.asList(visitors));
    }

    /**
     * Combines multiple {@link GraphVisitor} by AND-ing its output.
     * Can be used to create intersections.
     */
    public static GraphVisitor and(final Collection<? extends GraphVisitor> visitors) {
        return new GraphVisitor() {
            public boolean visit(Edge edge) {
                for (GraphVisitor v : visitors) {
                    if(!v.visit(edge))
                        return false;
                }
                return true;
            }

            public boolean visit(Node node) {
                for (GraphVisitor v : visitors) {
                    if(!v.visit(node))
                        return false;
                }
                return true;
            }
        };
    }

    /**
     * Combines multiple {@link GraphVisitor} by OR-ing its output.
     * Can be used to create unions.
     */
    public static GraphVisitor or(GraphVisitor... visitors) {
        return or(Arrays.asList(visitors));
    }

    /**
     * Combines multiple {@link GraphVisitor} by OR-ing its output.
     * Can be used to create unions.
     */
    public static GraphVisitor or(final Collection<? extends GraphVisitor> visitors) {
        return new GraphVisitor() {
            public boolean visit(Edge edge) {
                for (GraphVisitor v : visitors) {
                    if(v.visit(edge))
                        return true;
                }
                return false;
            }

            public boolean visit(Node node) {
                for (GraphVisitor v : visitors) {
                    if(!v.visit(node))
                        return true;
                }
                return false;
            }
        };
    }

    /**
     * Obtains a {@link GraphVisitor} that does boolean-negation of the current {@link GraphVisitor}.
     */
    public GraphVisitor not() {
        final GraphVisitor outer = this;
        return new GraphVisitor() {
            public boolean visit(Edge edge) {
                return !outer.visit(edge);
            }

            public boolean visit(Node node) {
                return !outer.visit(node);
            }

            @Override
            public GraphVisitor not() {
                return outer;
            }
        };
    }
}
