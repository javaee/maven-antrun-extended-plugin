package org.jvnet.maven.plugin.antrun;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.HashMap;

/**
 * Generates a dependency diagram by using GraphViz.
 *
 * @author Kohsuke Kawaguchi
 */
public class GraphVizVisualizer implements GraphVisitor {

    private final PrintWriter out;

    /**
     * Unique IDs given to GraphViz for each node.
     */
    private Map<DependencyGraph.Node,String> ids = new HashMap<DependencyGraph.Node, String>();

    public GraphVizVisualizer(PrintWriter out) {
        this.out = out;
        out.println("digraph G {");
    }

    public GraphVizVisualizer(OutputStream out) {
        this(new PrintWriter(out));
    }

    public void close() {
        out.println("}");
        out.close();
    }

    public boolean visit(DependencyGraph.Edge edge) {
        String label="";
        if(!edge.scope.equals("compile"))
            label = edge.scope;

        out.printf("%s -> %s [label=\"%s\",style=%s];", id(edge.src), id(edge.dst), label,
            edge.optional?"dotted":"filled");
        return true;
    }

    public boolean visit(DependencyGraph.Node node) {
        out.printf("%s [label=\"%s\"];", id(node), node.groupId+':'+node.artifactId);
        return true;
    }

    private String id(DependencyGraph.Node n) {
        String id = ids.get(n);
        if(id==null) {
            id = "n"+ids.size();
            ids.put(n,id);
        }
        return id;
    }

    /**
     * Returns a {@link GraphVizVisualizer} that generates a PNG file.
     */
    public static GraphVizVisualizer createPng(final File pngFile) throws IOException {
        ProcessBuilder pb = new ProcessBuilder("dot","-Tpng");
        final Process proc = pb.start();

        final Thread stdoutCopier = new Thread() {
            public void run() {
                try {
                    FileOutputStream out = new FileOutputStream(pngFile);
                    IOUtils.copy(proc.getInputStream(), out);
                    IOUtils.closeQuietly(out);
                } catch (IOException e) {
                    throw new Error(e);
                }
            }
        };

        stdoutCopier.start();

        // copy stderr
        new Thread() {
            public void run() {
                try {
                    IOUtils.copy(proc.getErrorStream(), System.err);
                } catch (IOException e) {
                    throw new Error(e);
                }
            }
        }.start();

        return new GraphVizVisualizer(proc.getOutputStream()) {
            @Override
            public void close() {
                super.close();
                try {
                    stdoutCopier.join();
                } catch (InterruptedException e) {
                    // handle interruption later
                    Thread.currentThread().interrupt();
                }
            }
        };
    }
}
