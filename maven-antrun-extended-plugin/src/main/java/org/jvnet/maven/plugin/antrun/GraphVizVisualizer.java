/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

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
        if(edge.src.groupId.equals(edge.dst.groupId))
            attrs.put("weight","10");

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
