package org.jvnet.maven.plugin.antrun;

import org.apache.tools.ant.Project;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

/**
 * Filter that generates a graph by using GraphViz.
 * 
 * @author Kohsuke Kawaguchi
 */
public class VisualizeFilter extends GraphFilter {
    private File output;

    private final List<Subgraph> subGraphs = new ArrayList<Subgraph>();

    /**
     * PNG file to be created.
     */
    public void setFile(File output) {
        this.output = output;
    }

    public void addConfiguredSubgraph(Subgraph g) {
        subGraphs.add(g);
    }

    public DependencyGraph process() {
        DependencyGraph g = evaluateChild();

        try {
            GraphVizVisualizer viz = GraphVizVisualizer.createPng(output);
            g.accept(viz);
            viz.close();
        } catch (IOException e) {
            // report an error, but don't let this fail the build, so that it can still
            // work in environments that don't have GraphViz.
            log("Failed to create "+output, Project.MSG_WARN);
            e.printStackTrace();
        }

        return g;
    }

    /**
     * Nested &lt;subgraph> element.
     * <p>
     * Each takes nested filtering specifier.
     */
    public static final class Subgraph extends GraphFilter {
        private String color;

        public void setColor(String color) {
            this.color = color;
        }

        public DependencyGraph process() {
            return evaluateChild();
        }
    }
}
