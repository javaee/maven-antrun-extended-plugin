package org.jvnet.maven.plugin.antrun;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.jvnet.maven.plugin.antrun.DependencyGraph.Edge;
import org.jvnet.maven.plugin.antrun.DependencyGraph.Node;

/**
 * Class of {@link Condition}s that visits {@link Node} or {@link Edge} of
 * {@link DependencyGraph}.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class GraphVisitingCondition extends GraphVisitor implements Condition {

    public final boolean eval() throws BuildException {
        return CURRENT.get().handle(this);
    }

    /**
     * Used after the graph is traversed via the <tt>visit</tt> method to
     * further filter out items from the remaining node set.
     *
     * <p>
     * The difference between this and {@link #visit(Node)} is that
     * the returning false from the latter would cut the traversal (thus
     * a single "return false" could eliminate a big subtree), whereas
     * the former would only eliminate a single node and without affecting
     * the traversal.
     *
     * @return
     *      true to retain the node, false to eliminate.
     */
    public boolean filter(Node n) {
        return true;
    }

    private interface Handler {
        boolean handle(GraphVisitingCondition _this);
    }

    /**
     * A hack to pass around the currently evaluated node for {@link Condition}
     * implementations, since {@link Condition#eval()} doesn't take any parameter.
     */
    private static final ThreadLocal<Handler> CURRENT = new ThreadLocal<Handler>();

    public static void setCurrent(final Edge e) {
        CURRENT.set(new Handler() {
            public boolean handle(GraphVisitingCondition _this) {
                return _this.visit(e);
            }
        });
    }

    public static void setCurrent(final Node n) {
        CURRENT.set(new Handler() {
            public boolean handle(GraphVisitingCondition _this) {
                return _this.visit(n);
            }
        });
    }

    public static void setCurrentFilterNode(final Node n) {
        CURRENT.set(new Handler() {
            public boolean handle(GraphVisitingCondition _this) {
                return _this.filter(n);
            }
        });
    }
}
