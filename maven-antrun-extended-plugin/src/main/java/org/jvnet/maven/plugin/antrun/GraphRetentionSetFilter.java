package org.jvnet.maven.plugin.antrun;


import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * Use this filter to create a retention set from a DependencyGraph. Indicate 
 * which artifacts you wish to include in the set by passing the artifactId(s) 
 * to one of the constructors.
 *
 * @author Paul Sterk
 */
public final class GraphRetentionSetFilter extends GraphVisitor implements GraphFilter {
    private final Collection<String> artifactIds;

    public GraphRetentionSetFilter(Collection<String> artifactIds) {
        this.artifactIds = artifactIds;
    }

    public GraphRetentionSetFilter(String... artifactIds) {
        this.artifactIds = Arrays.asList(artifactIds);
    }

    public GraphRetentionSetFilter(String artifactId) {
        this.artifactIds = Collections.singleton(artifactId);
    }

    public DependencyGraph process(DependencyGraph dependencyGraph) {
        // Step 1. Subtract out all the artifacts specified in the artifactIds
        // collection by doing set subtraction
        GraphSubtractionFilter sbf = new GraphSubtractionFilter(artifactIds);
        DependencyGraph subtractionSet = sbf.process(dependencyGraph);
        // Step 2. Create the retention set by subtracting the artifacts in the
        // subtractionSet created in Step 1 from the original dependencyGraph set
        Collection<String> subSetArtifactIds = getArtifactIds(subtractionSet);
        GraphSubtractionFilter sbf2 = new GraphSubtractionFilter(subSetArtifactIds);
        return sbf2.process(dependencyGraph);
    }
    
    private Collection<String> getArtifactIds(DependencyGraph graph) {
        Collection<String> ids = new HashSet();
        Collection<DependencyGraph.Node> nodes = graph.getAllNodes();
        for (DependencyGraph.Node node : nodes) {
            ids.add(node.getProject().getArtifact().getArtifactId());
        }
        return ids;
    }
    
}
