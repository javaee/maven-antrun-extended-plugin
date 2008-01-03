package org.jvnet.maven.plugin.antrun;

import org.apache.tools.ant.Project;

import java.io.File;
import java.io.IOException;

/**
 * Filter that generates a graph by using GraphViz.
 * 
 * @author Kohsuke Kawaguchi
 */
public class VisualizeFilter extends GraphFilter {
    File output;

    public void setFile(File output) {
        this.output = output;
    }

    public DependencyGraph process() {
        DependencyGraph g = evaluateChild();

        try {
            GraphVizVisualizer viz = GraphVizVisualizer.createPng(output);
            g.createSubGraph(viz);
            viz.close();
        } catch (IOException e) {
            // report an error, but don't let this fail the build, so that it can still
            // work in environments that don't have GraphViz.
            log("Failed to create "+output, Project.MSG_WARN);
            e.printStackTrace();
        }

        return g;
    }
}
