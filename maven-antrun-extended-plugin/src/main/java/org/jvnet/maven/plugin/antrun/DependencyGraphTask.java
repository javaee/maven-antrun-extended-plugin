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
            g = new DependencyGraph(w.project.getArtifact());
        } else {
            // otherwise pick up dependencies from the specified artifact
            g = new DependencyGraph(w.createArtifactWithClassifier(groupId,artifactId,version,type,classifier));
            log("artifactId "+artifactId,  Project.MSG_DEBUG);
        }

        g = new DependencyExclusionFilter().filter(g);

        log("Graph="+g,Project.MSG_DEBUG);
        return g;
    }
}
