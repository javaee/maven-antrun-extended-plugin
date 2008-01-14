package org.jvnet.maven.plugin.antrun;

import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Base class for tasks that work with a dependency graph defined as a nested structure.
 *
 * @author Kohsuke Kawaguchi
 * @author Paul Sterk
 */
public abstract class DependencyGraphTask extends Task {
    protected String groupId,artifactId,version,type="jar",classifier;
    protected final List<GraphFilter> children = new ArrayList<GraphFilter>();

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

    /**
     * Adds a {@link GraphFilter} child. Ant will invoke this for each child element given in build script.
     */
    public void add(GraphFilter child) {
        children.add(child);
    }

    protected DependencyGraph buildGraph() {
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
}
