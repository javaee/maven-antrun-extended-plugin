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

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Arrays;
import java.io.IOException;

/**
 * Base class for {@link ListFilter}s that takes several nested &lt;artifact> elements as parameters
 * to identify artifacts. 
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class AbstractArtifactsExclusionFilter extends ListFilter {
    /**
     * IDs of the artifacts to exclude. "groupId:artifactId:classifier".
     * These three are sufficient to identify an artifact uniquely within the context of single project
     * and its dependency.
     */
    protected final Set<String> ids = new HashSet<String>();

    private final List<ArtifactElement> artifactElements = new ArrayList<ArtifactElement>();

    protected AbstractArtifactsExclusionFilter(Collection<String> artifactIds) throws IOException {
        for (String artifactId : artifactIds)
            addArtifactId(artifactId);
    }

    protected AbstractArtifactsExclusionFilter(String... artifactIds) throws IOException {
        this(Arrays.asList(artifactIds));
    }

    protected AbstractArtifactsExclusionFilter(String artifactId) throws IOException {
        addArtifactId(artifactId);
    }

    // for Ant
    protected AbstractArtifactsExclusionFilter() {}

    /**
     * Resolves all the artifacts and computes {@link #ids}.
     *
     * This normally needs to be done at {@link #visit(DependencyGraph.Node)},
     * because this implementation could be used as a filter.
     * Can be invoked multiple times safely.
     */
    protected final void resolve() {
        try {
            for (ArtifactElement ae : artifactElements)
                addArtifact(ae.createArtifact());
            artifactElements.clear();
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Adds the artifact ID to {@link #ids}. Note that this requires us to infer other parameters like
     * groupId, version, etc.
     */
    private void addArtifactId(String artifactId) throws IOException {
        addArtifact(MavenComponentBag.get().resolveArtifactUsingMavenProjectArtifacts(artifactId));
    }

    protected void addArtifact(Artifact a) {
        ids.add(a.getGroupId()+':'+a.getArtifactId()+':'+a.getClassifier());
    }

    /**
     * Nested &lt;artifact> element can be used to specify what artifacts to exclude.
     */
    public void addConfiguredArtifact(ArtifactElement a) {
        // can't resolve this to artifact yet, because we don't have MavenComponentBag here.
        artifactElements.add(a);
    }
}
