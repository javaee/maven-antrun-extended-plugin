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
import org.apache.maven.project.ProjectBuildingException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import java.io.IOException;

/**
 * Use this filter to create a retention set from a DependencyGraph. Indicate 
 * which artifacts you wish to include in the set by passing the artifactId(s) 
 * to one of the constructors.
 *
 * @author Paul Sterk
 */
public final class RetentionSetFilter extends GraphFilter {
    private ArtifactElement root;

    /**
     * Nested &lt;artifact> element can be used to specify what artifacts to exclude.
     */
    public void addConfiguredArtifact(ArtifactElement a) {
        // can't resolve this to artifact yet, because we don't have MavenComponentBag here.
        if(root!=null)
            throw new BuildException("Only one <artifact> is allowed");
        root = a;
    }

    public DependencyGraph process() {
        try {
            final DependencyGraph base = evaluateChild();

            // Step 1. Subtract out all the artifacts specified in the artifactIds
            // collection by doing set subtraction
            ExcludeArtifactsTransitivelyFilter sbf = new ExcludeArtifactsTransitivelyFilter();
            sbf.addConfiguredArtifact(root);
            final DependencyGraph subtractionSet = base.createSubGraph(sbf);

            // Step 2. Create the retention set by subtracting the artifacts in the
            // subtractionSet created in Step 1 from the original dependencyGraph set
            DependencyGraph g = base.createSubGraph(
                base.toNode(root.createArtifact()),
                new DefaultGraphVisitor() {
                    public boolean visit(DependencyGraph.Node node) {
                        return !subtractionSet.contains(node);
                    }
                });

            log(getClass().getSimpleName()+" -> "+g,Project.MSG_DEBUG);

            return g;
        } catch (ProjectBuildingException e) {
            throw new BuildException("Failed to resolve artifacts",e);
        } catch (AbstractArtifactResolutionException e) {
            throw new BuildException("Failed to resolve artifacts",e);
        } catch (IOException e) {
            throw new BuildException("Failed to resolve artifacts",e);
        }
    }
}
