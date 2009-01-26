package org.jvnet.maven.plugin.antrun;

/**
 * A pseudo filter that returns the current full dependency graph.
 * @author Kohsuke Kawaguchi
 */
public class FullGraphFilter extends GraphFilter {
    public DependencyGraph process() {
        return CURRENT_INPUT.get();
    }
}
