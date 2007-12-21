package org.jvnet.maven.plugin.antrun;


import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import org.apache.maven.artifact.Artifact;

/**
 * Use this filter to create a subgraph a DependencyGraph. Indicate which artifacts
 * you wish to subtract by passing the artifactId(s) to one of the constructors.
 *
 * @author Paul Sterk
 */
public final class GraphSubtractionFilter extends GraphVisitor implements GraphFilter {
    private final Collection<Artifact> artifacts;

    public GraphSubtractionFilter(Collection<String> artifactIds) {
        this.artifacts = new HashSet<Artifact>();
        for (String artifactId : artifactIds) {
            this.artifacts.add(resolveArtifact(artifactId));
        }        
    }

    public GraphSubtractionFilter(String... artifactIds) {
        this.artifacts = new HashSet<Artifact>();
        for (String artifactId : artifactIds) {
            this.artifacts.add(resolveArtifact(artifactId));
        }
    }

    public GraphSubtractionFilter(String artifactId) {
        this.artifacts = new HashSet<Artifact>();
        this.artifacts.add(resolveArtifact(artifactId));
    }

    public DependencyGraph process(DependencyGraph dependencyGraph) {
        // Create a subgraph of the dependencyGraph by using this class as a 
        // GraphVisitor.
        return dependencyGraph.createSubGraph(this);
    }    
    
    @Override
    public boolean visit(DependencyGraph.Node node) {
        // If the artifact matches an artifact in the artifacts Set, do not 
        // include in the subgraph. Indicate this by returning 'false'.
        return ! include(node.getProject().getArtifact());        
    }
    
/****************************************************************************
 * private methods
 ****************************************************************************/
    /*
     * Leverage the @link Comparable interface implementation in the Artifact 
     * class to determine if Artifacts are equal
     */
    private boolean include(Artifact a1) {
        boolean include = true;
        for (Artifact artifact : this.artifacts) {
            if (artifact.compareTo(a1) == 0) {
                // A 'zero' means that the objects are equal unless otherwise
                // documented in the Artifact interface
                include = false;
                break;
            }
        }
        return include;
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
