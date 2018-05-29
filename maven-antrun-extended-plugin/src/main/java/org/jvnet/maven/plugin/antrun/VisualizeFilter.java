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

import org.apache.tools.ant.Project;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.PrintWriter;
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

            for (Subgraph subGraph : subGraphs)
                viz.addColoredSubgraph( subGraph.process(), subGraph.color);

            g.accept(viz);
            viz.close();
        } catch (IOException e) {
            // report an error, but don't let this fail the build, so that it can still
            // work in environments that don't have GraphViz.
            log("Failed to create "+output, Project.MSG_WARN);
            StringWriter sw = new StringWriter(); 
            e.printStackTrace(new PrintWriter(sw));
            log(sw.toString(),Project.MSG_VERBOSE);
        }

        return g;
    }

    /**
     * Nested &lt;subgraph> element.
     * <p>
     * Each takes nested filtering specifier, and those subgraphs will be drawn in a different color.
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
