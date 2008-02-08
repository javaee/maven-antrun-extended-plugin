package org.jvnet.maven.plugin.antrun;

import org.apache.tools.ant.BuildException;

/**
 * Computes a dependency graph by applying a filter, then store it to the current project
 * so that it can be used later with &lt;graphRef> filter.
 *
 * <p>
 * The name is chosen to match with other xxxDef tasks in Ant, like taskDef, typeDef, etc.
 * 
 * @author Kohsuke Kawaguchi
 */
public class GraphDefTask extends DependencyGraphTask {
    private String id;

    public void setId(String id) {
        this.id = id;
    }


    public void execute() throws BuildException {
        if(id==null)
            throw new BuildException("@id is required");
        
        getProject().addReference(id,buildGraph());
    }
}
