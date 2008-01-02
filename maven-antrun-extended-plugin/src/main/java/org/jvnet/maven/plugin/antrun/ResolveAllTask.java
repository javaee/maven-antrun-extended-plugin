package org.jvnet.maven.plugin.antrun;

import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.FileSet;
import org.jvnet.maven.plugin.antrun.DependencyGraph.Node;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Transitively resolve dependencies, perform some filtering, and deliver the resulting
 * set of artifacts in various forms (as a new {@link Path} object, into a directory, etc.)
 *
 * @author Kohsuke Kawaguchi
 * @author Paul Sterk
 */
public class ResolveAllTask extends Task {
    
    private File todir;
    
    private String pathId;
    
    private String groupId,artifactId,version,type="jar",classifier;
    private final List<GraphFilter> children = new ArrayList<GraphFilter>();

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
    
    public void setTodir(File todir) {
        this.todir = todir;
        todir.mkdirs();
    }
    
    public void setPathId(String pathId) {
        this.pathId = pathId;
    }

    /**
     * Adds a {@link GraphFilter} child. Ant will invoke this for each child element given in build script.
     */
    public void add(GraphFilter child) {
        children.add(child);
    }

    public void execute() throws BuildException {  
        log("Starting ResolveAllTasks.execute ", Project.MSG_DEBUG);
        try {
            MavenComponentBag w = MavenComponentBag.get();

            DependencyGraph g;

            if(groupId==null && artifactId==null && version==null) {
                // if no clue is given whatsoever, use all the project dependencies
                g = new DependencyGraph(w.project.getArtifact());
            } else {
                // otherwise pick up dependencies from the specified artifact
                g = new DependencyGraph(w.createArtifactWithClassifier(groupId,artifactId,version,type,classifier));
                log("artifactId "+artifactId,  Project.MSG_DEBUG);
            }

            log("Graph="+g,Project.MSG_DEBUG);

            if(children.size()>1)
                throw new BuildException("Too many filters are given");

            if(!children.isEmpty()) {
                // apply transformation to g
                final DependencyGraph old = GraphFilter.CURRENT_INPUT.get();
                GraphFilter.CURRENT_INPUT.set(g);
                try {
                    g = children.get(0).process();
                } finally {
                    GraphFilter.CURRENT_INPUT.set(old);
                }
            }

            Collection<Node> nodes = g.getAllNodes();
            log("Filtered down to "+ nodes.size()+" artifact(s)",Project.MSG_DEBUG);
            for (Node n : nodes)
                log("  "+n,Project.MSG_DEBUG);

            if(pathId!=null) {
                // collect all artifacts into a path and export
                Path path = new Path(getProject());
                for (Node n : nodes) {
                    File f = n.getArtifactFile();
                    if(f!=null)
                        path.createPathElement().setLocation(f);
                }
                getProject().addReference(pathId,path);
            }

            if(todir!=null) {
                // copy files to the specified target directory.
                // use the <copy> task implementation to do up-to-date check.
                Copy cp = new Copy();
                cp.setTaskName(getTaskName());
                cp.setProject(getProject());
                cp.setTodir(todir);

                for (Node n : nodes) {
                    File f = n.getArtifactFile();
                    if(f!=null) {
                        FileSet fs = new FileSet();
                        fs.setFile(f);
                        cp.addFileset(fs);
                    }
                }
                cp.execute();
            }
        } catch (AbstractArtifactResolutionException e) {
            throw new BuildException(e);
        } catch (IOException e) {
            throw new BuildException(e);
        } catch (ProjectBuildingException e) {
            throw new BuildException(e);
        }
        
        log("Exiting ResolveAllTasks.execute ", Project.MSG_DEBUG);
    }
}
