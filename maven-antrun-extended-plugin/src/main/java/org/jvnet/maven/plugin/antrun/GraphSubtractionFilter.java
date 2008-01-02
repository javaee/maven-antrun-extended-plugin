package org.jvnet.maven.plugin.antrun;


import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

import org.apache.maven.artifact.Artifact;
import org.apache.tools.ant.BuildException;

/**
 * Use this filter to create a subgraph a DependencyGraph. Indicate which artifacts
 * you wish to subtract by passing the artifactId(s) to one of the constructors.
 *
 * @author Paul Sterk
 */
public final class GraphSubtractionFilter extends GraphFilter implements GraphVisitor {
    private final Set<Artifact> artifacts = new HashSet<Artifact>();

    private final List<ArtifactElement> artifactElements = new ArrayList<ArtifactElement>();

    public GraphSubtractionFilter(Collection<String> artifactIds) {
        for (String artifactId : artifactIds) {
            this.artifacts.add(resolveArtifact(artifactId));
        }        
    }

    public GraphSubtractionFilter(String... artifactIds) {
        for (String artifactId : artifactIds) {
            this.artifacts.add(resolveArtifact(artifactId));
        }
    }

    public GraphSubtractionFilter(String artifactId) {
        this.artifacts.add(resolveArtifact(artifactId));
    }

    /**
     * Constructor for Ant.
     */
    public GraphSubtractionFilter() {
    }

    /**
     * Nested &lt;artifact> element can be used to specify what artifacts to exclude.
     */
    public void addConfiguredArtifact(ArtifactElement a) {
        // can't resolve this to artifact yet.
        artifactElements.add(a);
    }


    @Override
    public DependencyGraph process() {
        // Create a subgraph of the dependencyGraph by using this class as a
        // GraphVisitor.
        return evaluateChild().createSubGraph(this);
    }    
    
    public boolean visit(DependencyGraph.Node node) {
        // at this point we have a valid MavenComponentBag to perform this resolution.
        try {
            for (ArtifactElement a : artifactElements)
                artifacts.add(a.createArtifact(MavenComponentBag.get()));
            artifactElements.clear();
        } catch (IOException e) {
            throw new BuildException(e);
        }

        // If the artifact matches an artifact in the artifacts Set, do not 
        // include in the subgraph. Indicate this by returning 'false'.
        return !artifacts.contains(node.getProject().getArtifact());
    }

    public boolean visit(DependencyGraph.Edge edge) {
        return true;
    }

    private Artifact resolveArtifact(String artifactId) {
        try {
            return MavenComponentBag.get()
                                    .resolveArtifactUsingMavenProjectArtifacts(artifactId,
                                                                               null,
                                                                               null,
                                                                               null,
                                                                               null);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
