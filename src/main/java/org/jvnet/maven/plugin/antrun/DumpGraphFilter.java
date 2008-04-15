package org.jvnet.maven.plugin.antrun;

import org.apache.tools.ant.Project;

/**
 * Special {@link GraphFilter} that dumps the graph. Useful for debugging.
 * 
 * @author Kohsuke Kawaguchi
 */
public class DumpGraphFilter extends GraphFilter {
    public DependencyGraph process() {
        DependencyGraph g = evaluateChild();
        log(g.toString(), Project.MSG_INFO);
        return g;
    }
}
