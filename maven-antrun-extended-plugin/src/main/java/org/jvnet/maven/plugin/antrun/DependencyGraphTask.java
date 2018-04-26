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

import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import java.io.IOException;
import java.util.Collection;

/**
 * Base class for tasks that work with a dependency graph defined as a nested structure.
 *
 * @author Kohsuke Kawaguchi
 * @author Paul Sterk
 */
public abstract class DependencyGraphTask extends Task {
    private String groupId,artifactId,version,type="jar",classifier;
    private String baseGraph;
    private boolean tolerateBrokenPOMs;

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    public void setBaseGraph(String id) {
        this.baseGraph = id;
    }

    /**
     * If true, ignore an artifact that fails to resolve.
     */
    public void setTolerateBrokenPOMs(boolean tolerateBrokenPOMs) {
        this.tolerateBrokenPOMs = tolerateBrokenPOMs;
    }

    /**
     * Transforms a graph by applying the given filter to thtese "source graph",
     * which is determined by the various parameter to this task.
     */
    protected DependencyGraph buildGraph(GraphFilter filter) {
        try {
            DependencyGraph g = buildSourceGraph();
            if(filter==null)    return g;

            // apply transformation to g
            final DependencyGraph old = GraphFilter.CURRENT_INPUT.get();
            GraphFilter.CURRENT_INPUT.set(g);
            try {
                g = filter.process();
            } finally {
                GraphFilter.CURRENT_INPUT.set(old);
            }

            Collection<DependencyGraph.Node> nodes = g.getAllNodes();
            log("Filtered down to "+ nodes.size()+" artifact(s)",Project.MSG_DEBUG);
            for (DependencyGraph.Node n : nodes)
                log("  "+n,Project.MSG_DEBUG);
            return g;
        } catch (AbstractArtifactResolutionException e) {
            throw new BuildException(e);
        } catch (IOException e) {
            throw new BuildException(e);
        } catch (ProjectBuildingException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Computes the source grpah.
     */
    private DependencyGraph buildSourceGraph() throws ProjectBuildingException, ArtifactResolutionException, ArtifactNotFoundException, IOException {
        MavenComponentBag w = MavenComponentBag.get();

        DependencyGraph g;

        if(baseGraph!=null) {
            g = (DependencyGraph)getProject().getReference(baseGraph);
            if(g==null)
                throw new BuildException("There's no graph with id="+baseGraph);
        } else
        if(groupId==null && artifactId==null && version==null) {
            // if no clue is given whatsoever, use all the project dependencies
            g = new DependencyGraph(w.project.getArtifact(), tolerateBrokenPOMs);
        } else {
            // otherwise pick up dependencies from the specified artifact
            g = new DependencyGraph(w.createArtifactWithClassifier(groupId,artifactId,version,type,classifier), tolerateBrokenPOMs);
            log("artifactId "+artifactId,  Project.MSG_DEBUG);
        }

        g = new DependencyExclusionFilter().filter(g);

        log("Graph="+g,Project.MSG_DEBUG);
        return g;
    }
}
