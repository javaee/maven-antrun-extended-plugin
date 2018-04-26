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

import java.util.Arrays;
import java.util.Collection;

/**
 * Factories for {@link GraphVisitor}.
 *
 * @author Kohsuke Kawaguchi
 */
public class GraphVisitors {
    /**
     * Combines multiple {@link GraphVisitor} by AND-ing its output.
     * Can be used to create intersections.
     */
    public static GraphVisitor and(GraphVisitor... visitors) {
        return and(Arrays.asList(visitors));
    }

    /**
     * Combines multiple {@link GraphVisitor} by AND-ing its output.
     * Can be used to create intersections.
     */
    public static GraphVisitor and(final Collection<? extends GraphVisitor> visitors) {
        return new GraphVisitor() {
            public boolean visit(DependencyGraph.Edge edge) {
                for (GraphVisitor v : visitors) {
                    if(!v.visit(edge))
                        return false;
                }
                return true;
            }

            public boolean visit(DependencyGraph.Node node) {
                for (GraphVisitor v : visitors) {
                    if(!v.visit(node))
                        return false;
                }
                return true;
            }
        };
    }

    /**
     * Combines multiple {@link GraphVisitor} by OR-ing its output.
     * Can be used to create unions.
     */
    public static GraphVisitor or(GraphVisitor... visitors) {
        return or(Arrays.asList(visitors));
    }

    /**
     * Combines multiple {@link GraphVisitor} by OR-ing its output.
     * Can be used to create unions.
     */
    public static GraphVisitor or(final Collection<? extends GraphVisitor> visitors) {
        return new GraphVisitor() {
            public boolean visit(DependencyGraph.Edge edge) {
                for (GraphVisitor v : visitors) {
                    if(v.visit(edge))
                        return true;
                }
                return false;
            }

            public boolean visit(DependencyGraph.Node node) {
                for (GraphVisitor v : visitors) {
                    if(!v.visit(node))
                        return true;
                }
                return false;
            }
        };
    }

    /**
     * Obtains a {@link GraphVisitor} that does boolean-negation of the current {@link GraphVisitor}.
     */
    public static GraphVisitor not(final GraphVisitor graph) {
        return new GraphVisitor() {
            public boolean visit(DependencyGraph.Edge edge) {
                return !graph.visit(edge);
            }

            public boolean visit(DependencyGraph.Node node) {
                return !graph.visit(node);
            }
        };
    }
}
