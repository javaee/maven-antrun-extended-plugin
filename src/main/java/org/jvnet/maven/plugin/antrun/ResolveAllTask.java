package org.jvnet.maven.plugin.antrun;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.jvnet.maven.plugin.antrun.DependencyGraph.Node;

import java.io.File;
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

    private final List<ListFilter> listFilters = new ArrayList<ListFilter>();
    
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
