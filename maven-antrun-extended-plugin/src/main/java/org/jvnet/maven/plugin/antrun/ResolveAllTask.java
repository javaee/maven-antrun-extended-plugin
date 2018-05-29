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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.artifact.Artifact;
import org.jvnet.maven.plugin.antrun.DependencyGraph.Node;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Transitively resolve dependencies, perform some filtering first on the graph data model,
 * then on list data model, and deliver the resulting
 * set of artifacts in various forms (as a new {@link Path} object, into a directory, etc.)
 *
 * @author Kohsuke Kawaguchi
 * @author Paul Sterk
 */
public class ResolveAllTask extends DependencyGraphTask {
    
    private File todir;
    
    private String pathId;

    private GraphFilter filter;

    private String classifier;

    private final List<ListFilter> listFilters = new ArrayList<ListFilter>();

    private boolean stripVersion;

    public void setTodir(File todir) {
        this.todir = todir;
        todir.mkdirs();
    }
    
    public void setPathId(String pathId) {
        this.pathId = pathId;
    }

    /**
     * Instead of resolving the artifact as appeared in the dependency graph,
     * resolve a specific classifier.
     *
     * This is normally used to gather source jars.
     */
    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    /**
     * If set to true, destination file names won't have version
     * encoded in them.
     * @param stripVersion destination file name to have version or not
     */
    public void setStripVersion(boolean stripVersion) {
        this.stripVersion = stripVersion;
    }

    /**
     * Adds a {@link GraphFilter} child. Ant will invoke this for each child element given in build script.
     */
    public void add(GraphFilter child) {
        if(filter==null)
            this.filter = child;
        else {
            if (child instanceof ListFilter) {
                listFilters.add((ListFilter) child);
            } else {
                throw new BuildException(filter+" is not a list filter");
            }
        }
    }

    public void execute() throws BuildException {
        log("Starting ResolveAllTasks.execute ", Project.MSG_DEBUG);

        // first graph filtering
        DependencyGraph g = buildGraph(filter);
        List<Node> nodes = new ArrayList<Node>(g.getAllNodes());

        // further trim down the list by list filtering
        final DependencyGraph old = GraphFilter.CURRENT_INPUT.get();
        GraphFilter.CURRENT_INPUT.set(g);
        try {
            for (ListFilter listFilter : listFilters) {
                for (Iterator<Node> itr = nodes.iterator(); itr.hasNext();)
                    if(!listFilter.visit(itr.next()))
                        itr.remove();
            }
        } finally {
            GraphFilter.CURRENT_INPUT.set(old);
        }


        if(pathId!=null) {
            // collect all artifacts into a path and export
            Path path = new Path(getProject());
            for (Node n : nodes) {
                try {
                    File f = resolve(n);
                    if(f!=null)
                    path.createPathElement().setLocation(f);
                } catch (AbstractArtifactResolutionException e) {
                    throw new BuildException("Failed to resolve artifact. Trail="+n.getTrail(g),e);
                } catch (IOException e) {
                    throw new BuildException("Failed to resolve artifact. Trail="+n.getTrail(g),e);
                }
            }
            getProject().addReference(pathId,path);
        }

        if(todir!=null) {
            boolean hasSomethingToCopy=false;
            for (Node n : nodes) {
                try {
                    File f = resolve(n);
                    if(f!=null) {
                        // copy files to the specified target directory.
                        // use the <copy> task implementation to do up-to-date check.
                        Copy cp = new Copy();
                        cp.setTaskName(getTaskName());
                        cp.setProject(getProject());
                        cp.setTodir(todir);
                        if (stripVersion) {
                            cp.add(new VersionStripper(n.version));
                        }
                        FileSet fs = new FileSet();
                        fs.setFile(f);
                        cp.addFileset(fs);
                        cp.execute();
                        hasSomethingToCopy=true;
                    }
                } catch (AbstractArtifactResolutionException e) {
                    throw new BuildException("Failed to resolve artifact. Trail="+n.getTrail(g),e);
                } catch (IOException e) {
                    throw new BuildException("Failed to resolve artifact. Trail="+n.getTrail(g),e);
                }
            }

            if(!hasSomethingToCopy)
                log("Nothing to copy",Project.MSG_INFO);
        }

        log("Exiting ResolveAllTasks.execute ", Project.MSG_DEBUG);
    }

    private File resolve(Node n) throws AbstractArtifactResolutionException, IOException {
        if(classifier==null)
            return n.getArtifactFile();

        final MavenComponentBag w = MavenComponentBag.get();
        Artifact a = w.factory.createArtifactWithClassifier(n.groupId, n.artifactId, n.version, n.type, classifier);
        List remoteRepos=null;
        if(n.getProject()!=null)
            remoteRepos = n.getProject().getRemoteArtifactRepositories();
        w.resolveArtifact(a,remoteRepos);
        return a.getFile();
    }

    private class VersionStripper implements FileNameMapper {
        String version;
        public VersionStripper(String v) {
            version = v;
        }

        public void setFrom(String s) {
        }

        public void setTo(String s) {
        }

        public String[] mapFileName(String s) {
            int idx = s.lastIndexOf(version);
            String to = s;
            if (idx != -1) {
                // remove version in artifactId-version(-classifier).type
                String baseFilename = s.substring( 0, idx - 1 );
                String extension = s.substring( idx + version.length());
                to = baseFilename + extension;
            }
            log("mapFileName: " + s + " -> " + to, Project.MSG_DEBUG);
            return new String[]{to};
        }
    }
}
