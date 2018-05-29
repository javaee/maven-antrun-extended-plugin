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

import org.apache.maven.artifact.Artifact;
import org.apache.tools.ant.BuildException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Filter {@link DependencyGraph} by excluding the specified set of artifacts. Any artifacts made
 * unreachable by this process will be also excluded.
 *
 * <p>
 * Informally, a node will remain in the graph
 * only when it's reachable from the root without going through
 * any of the excluded artifacts.
 *
 * <p>
 * Here's the format definition.
 * <p>
 * Let normalize(G={r,V,E}) -> G'={r,V',E'} be defined as follows.
 * This is an operation to remove unreachable nodes and edges.
 *
 * <pre>
 *  V' = { v | \exists r ->* v in G }
 *  E' = { (u,v) | u \in V' and v \in V' }
 * </pre>
 *
 * Given the graph G=(r,V,E) and exclusion nodes N,
 * the new graph G' is defined as follows:
 *
 * <pre>
 * G'=normalize(r,V-N),E)
 * </pre>
 *
 * @author Paul Sterk
 * @author Kohsuke Kawaguchi
 * @see RemoveSpecificArtifactsFilter
 */
public final class ExcludeArtifactsTransitivelyFilter extends AbstractArtifactsExclusionFilter {
    public ExcludeArtifactsTransitivelyFilter(Collection<String> artifactIds) throws IOException {
        super(artifactIds);
    }

    public ExcludeArtifactsTransitivelyFilter(String... artifactIds) throws IOException {
        super(artifactIds);
    }

    public ExcludeArtifactsTransitivelyFilter(String artifactId) throws IOException {
        super(artifactId);
    }

    public ExcludeArtifactsTransitivelyFilter() {
    }

    public boolean visit(DependencyGraph.Node node) {
        resolve();

        // If the artifact matches an artifact in the artifacts Set, do not
        // include in the subgraph. Indicate this by returning 'false'.
        return !ids.contains(node.getId());
    }
}
