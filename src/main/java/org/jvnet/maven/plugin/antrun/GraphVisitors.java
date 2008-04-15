package org.jvnet.maven.plugin.antrun;

import java.util.Arrays;
import java.util.Collection;

/**
 * Factories for {@link GraphVisitor}.
 *
 * @author Kohsuke Kawaguchi
 */
public class GraphVisitors {
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
            public boolean visit(DependencyGraph.Edge edge) {
                for (GraphVisitor v : visitors) {
                    if(!v.visit(edge))
                        return false;
                }
                return true;
            }

            public boolean visit(DependencyGraph.Node node) {
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
            public boolean visit(DependencyGraph.Edge edge) {
                for (GraphVisitor v : visitors) {
                    if(v.visit(edge))
                        return true;
                }
                return false;
            }

            public boolean visit(DependencyGraph.Node node) {
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
    public static GraphVisitor not(final GraphVisitor graph) {
        return new GraphVisitor() {
            public boolean visit(DependencyGraph.Edge edge) {
                return !graph.visit(edge);
            }

            public boolean visit(DependencyGraph.Node node) {
                return !graph.visit(node);
            }
        };
    }
}
