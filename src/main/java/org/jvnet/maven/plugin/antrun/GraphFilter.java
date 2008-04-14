package org.jvnet.maven.plugin.antrun;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ProjectComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * Filter a {@link DependencyGraph} based on configuration by Ant.
 * 
 * @author psterk
 */
public abstract class GraphFilter extends ProjectComponent {
    protected final List<GraphFilter> children = new ArrayList<GraphFilter>();


    public abstract DependencyGraph process();

    /**
     * Adds another child. Ant will invoke this for each child element given in build script.
     */
    public void add(GraphFilter child) {
        children.add(child);
    }

    /**
     * Evaluate the n-th child {@link GraphFilter}. If omitted, it returns the input graph,
     * so that the full graph can be given as an input implicitly. Whether this defaulting
     * is a good idea or not, it's hard to say.
     */
    protected DependencyGraph evaluateChild(int index) {
        if(children.size()<=index)
            return CURRENT_INPUT.get();
        else
            return children.get(index).process();
    }

    /**
     * Short for {@code evaluateChild(0)}, for those fitlers that only have one child.
     */
    protected final DependencyGraph evaluateChild() {
        if(children.size()>1)
            throw new BuildException("Too many children in "+getClass().getName());
        return evaluateChild(0);
    }

    /*package*/ static final ThreadLocal<DependencyGraph> CURRENT_INPUT = new ThreadLocal<DependencyGraph>();

}
