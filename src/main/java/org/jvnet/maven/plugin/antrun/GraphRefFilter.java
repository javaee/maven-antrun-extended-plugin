package org.jvnet.maven.plugin.antrun;

import org.apache.tools.ant.BuildException;

/**
 * Obtains a graph stored by {@link GraphDefTask}. 
 *
 * @author Kohsuke Kawaguchi
 */
public class GraphRefFilter extends GraphFilter {
    private String ref;

    public void setRefid(String ref) {
        this.ref = ref;
    }

    public DependencyGraph process() {
        Object o = getProject().getReference(ref);
        if(o==null)
            throw new BuildException("No graph exists with id="+ref);
        if (o instanceof DependencyGraph)
            return (DependencyGraph) o;

        throw new BuildException("id="+ref+" is not a graph but "+o);
    }
}
