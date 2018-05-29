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

import org.apache.maven.project.MavenProject;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;

import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.HashSet;
import java.util.Collections;

/**
 * Filters out a graph by honoring dependency exclusion.
 *
 * <p>
 * A care is taken to handle a diamond dependency with exclusion correctly.
 * For example, in the following situation X shouldn't be excluded
 * because there's a valid reachable path to X.
 *
 * <pre>
 *   A -+-> B[exclude=X] -+
 *      |                 |
 *      +-> C ------------+-> D ---> X
 * </pre>
 * 
 * @author Kohsuke Kawaguchi
 */
public class DependencyExclusionFilter extends GraphFilter {
    public DependencyGraph process() {
        final DependencyGraph g = evaluateChild();
        return filter(g);
    }

    public DependencyGraph filter(final DependencyGraph g) {
        // All the reachable nodes will be accumulated here.
        final Set<DependencyGraph.Node> reachables = new HashSet<DependencyGraph.Node>();

        new Runnable() {
            private final Stack<Set<String>> exclusions = new Stack<Set<String>>();

            /**
             * Nodes that are already visited without any exclusion.
             * This is optimization, as if this is the case we know there's no point in visiting
             * such node twice.
             */
            private final Set<DependencyGraph.Node> visitedWithNoExclusion = new HashSet<DependencyGraph.Node>();

            private boolean noExclusionSoFar = true;

            public void run() {
                visit(g.getRoot());
            }

            /**
             * Visits all the paths.
             */
            private void visit(DependencyGraph.Node node) {
                // is this node excluded in the current path? If so, return.
                String id = node.groupId + ':' + node.artifactId;
                for (Set<String> e : exclusions)
                    if(e.contains(id))
                        return;

                // now we know that this is reachable
                reachables.add(node);

                if(noExclusionSoFar && !visitedWithNoExclusion.add(node))
                    // optimization. this node has already been visited with no exclusion,
                    // so no point in doing it twice
                    return;

                // create a new environment
                final boolean old = noExclusionSoFar;
                Set<String> newExc = computeExclusionSet(node);
                exclusions.push(newExc);
                noExclusionSoFar &= newExc.isEmpty();

                // recurse
                for (DependencyGraph.Edge e : node.getForwardEdges(g)) {
                    if(!e.optional)
                        visit(e.dst);
                }

                // then restore the old environment
                exclusions.pop();
                noExclusionSoFar = old;
            }

            /**
             * Computes the exclusion set added by this node. They are strings of the form 'groupId:artifactId'.
             */
            private Set<String> computeExclusionSet(DependencyGraph.Node node) {
                MavenProject p = node.getProject();
                if(p==null) return Collections.emptySet();

                Set<String> excSet = new HashSet<String>();
                for( Dependency d : (List<Dependency>)p.getDependencies() )
                    for( Exclusion exc : (List<Exclusion>)d.getExclusions() )
                        excSet.add(exc.getGroupId()+':'+exc.getArtifactId());
                return excSet;
            }
        }.run();

        return g.createSubGraph(g.getRoot(), reachables);
    }

}
