package org.jvnet.maven.plugin.antrun;

import org.apache.tools.ant.BuildException;

/**
 * Filter that takes two children G1 and G2, and compute G1-G2.
 *
 * @author Kohsuke Kawaguchi
 */
public class SubtractFilter extends GraphFilter {
    public DependencyGraph process() {
        if(children.size()!=2)
            throw new BuildException("<subtract> needs two children");
        DependencyGraph base = evaluateChild(0);

        return base.createSubGraph(new DefaultGraphVisitor() {
            DependencyGraph excess = evaluateChild(1);

            public boolean visit(DependencyGraph.Node node) {
                return !excess.contains(node);
            }
        });
    }
}
