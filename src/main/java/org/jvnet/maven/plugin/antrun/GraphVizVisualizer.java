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

    private final Map<Object/*Node|Edge*/,String> colors = new HashMap<Object,String>();

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

    /**
     * Paint all edges and nodes that belong to the given subgraph by using the specified color.
     */
    public void addColoredSubgraph(DependencyGraph g, final String color) {
        g.accept(new GraphVisitor() {
            public boolean visit(DependencyGraph.Edge edge) {
                colors.put(edge,color);
                return true;
            }

            public boolean visit(DependencyGraph.Node node) {
                colors.put(node,color);
                return true;
            }
        });
    }

    public void close() {
        out.println("}");
        out.close();
    }

    public boolean visit(DependencyGraph.Edge edge) {
        Map<String,String> attrs = new HashMap<String, String>();

        if(!edge.scope.equals("compile"))   // most of dependencies are compile, so skip them for brevity
            attrs.put("label",edge.scope);
        if(edge.optional)
            attrs.put("style","dotted");
        attrs.put("color",colors.get(edge));

        out.printf("%s -> %s ", id(edge.src), id(edge.dst));
        writeAttributes(attrs);
        return true;
    }

    public boolean visit(DependencyGraph.Node node) {
        Map<String,String> attrs = new HashMap<String, String>();
        attrs.put("label",node.groupId+':'+node.artifactId);
        attrs.put("color",colors.get(node));

        out.print(id(node)+' ');
        writeAttributes(attrs);
        return true;
    }

    private void writeAttributes(Map<String,String> attributes) {
        out.print('[');
        boolean first=true;
        for (Map.Entry<String,String> e : attributes.entrySet()) {
            if(e.getValue()==null)  continue;   // skip

            out.printf("%s=\"%s\"",e.getKey(),e.getValue());
            if(!first)
                out.print(',');
            else
                first = false;
        }
        out.println("];");
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
