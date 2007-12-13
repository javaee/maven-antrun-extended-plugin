package org.jvnet.maven.plugin.antrun;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.apache.tools.ant.taskdefs.condition.ConditionBase;
import org.apache.tools.ant.types.Path;
import org.jvnet.maven.plugin.antrun.DependencyGraph.Edge;
import org.jvnet.maven.plugin.antrun.DependencyGraph.Node;

import java.io.File;
import java.util.Iterator;
import java.util.Set;

/**
 * Transitively resolve dependencies, perform some filtering, and deliver the resulting
 * set of artifacts in various forms (as a new {@link Path} object, into a directory, etc.)
 *
 * @author Kohsuke Kawaguchi
 * @author Paul Sterk
 */
public class ResolveAllTask extends ConditionBase {
    
    private File todir;
    
    private String pathId;
    
    private String groupId,artifactId,version,type="jar",classifier;

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
    
    public void execute() throws BuildException {  
        log("Starting ResolveAllTasks.execute ", Project.MSG_DEBUG);
        try {
            MavenComponentBag w = MavenComponentBag.get();

            final DependencyGraph g;

            if(groupId==null && artifactId==null && version==null) {
                // if no clue is given whatsoever, use all the project dependencies
                g = new DependencyGraph(w.project.getArtifact());
            } else {
                // otherwise pick up dependencies from the specified artifact
                g = new DependencyGraph(w.createArtifactWithClassifier(groupId,artifactId,version,type,classifier));
                log("artifactId "+artifactId,  Project.MSG_DEBUG);
            }

            log("Graph="+g,Project.MSG_DEBUG);

            final Condition c = getCondition();

            // visit the graph and determine the subset of artifacts
            Set<Node> nodes = g.accept(new GraphVisitor() {
                public boolean visit(Edge edge) {
                    GraphVisitingCondition.setCurrent(edge);
                    return c.eval();
                }

                public boolean visit(Node node) {
                    GraphVisitingCondition.setCurrent(node);
                    return c.eval();
                }
            });

            // further filter out nodes 
            for (Iterator<Node> itr = nodes.iterator(); itr.hasNext();) {
                GraphVisitingCondition.setCurrentFilterNode(itr.next());
                if(!c.eval())
                    itr.remove();
            }

            log("Filtered down to "+nodes.size()+" artifact(s)",Project.MSG_DEBUG);
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
                for (Node n : nodes) {
                    File f = n.getArtifactFile();
                    if(f!=null) {
                        Copy cp = new Copy();
                        cp.setProject(getProject());
                        cp.setFile(f);
                        cp.setTofile(new File(todir,f.getName()));
                        cp.execute();
                    }
                }
            }


        } catch (Throwable t) {
            throw new BuildException(t);
        }
        log("Exiting ResolveAllTasks.execute ", Project.MSG_DEBUG);
    }

    /**
     * Determines the configured condition.
     */
    private Condition getCondition() {
        if (countConditions() > 1) {
            throw new BuildException("You must not nest more than one "
                + "condition into <condition>");
        }
        if (countConditions()==1) {
            return (Condition) getConditions().nextElement();
        }

        // no condition given, meaning all artifacts.
        return new Condition() {
            public boolean eval() throws BuildException {
                return true;
            }
        };
    }
}
