package org.jvnet.maven.plugin.antrun;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.jvnet.maven.plugin.antrun.DependencyGraph.Node;

import java.io.File;
import java.util.Collection;

/**
 * Transitively resolve dependencies, perform some filtering, and deliver the resulting
 * set of artifacts in various forms (as a new {@link Path} object, into a directory, etc.)
 *
 * @author Kohsuke Kawaguchi
 * @author Paul Sterk
 */
public class ResolveAllTask extends DependencyGraphTask {
    
    private File todir;
    
    private String pathId;
    
    public void setTodir(File todir) {
        this.todir = todir;
        todir.mkdirs();
    }
    
    public void setPathId(String pathId) {
        this.pathId = pathId;
    }

    public void execute() throws BuildException {
        log("Starting ResolveAllTasks.execute ", Project.MSG_DEBUG);
        Collection<Node> nodes = buildGraph().getAllNodes();

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

            boolean hasSomethingToCopy=false;
            for (Node n : nodes) {
                File f = n.getArtifactFile();
                if(f!=null) {
                    FileSet fs = new FileSet();
                    fs.setFile(f);
                    cp.addFileset(fs);
                    hasSomethingToCopy=true;
                }
            }

            if(hasSomethingToCopy)
                cp.execute();
            else
                cp.log("Nothing to copy",Project.MSG_INFO);
        }

        log("Exiting ResolveAllTasks.execute ", Project.MSG_DEBUG);
    }
}
