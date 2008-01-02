package org.jvnet.maven.plugin.antrun;


import org.apache.tools.ant.BuildException;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.io.IOException;

/**
 * Use this filter to create a retention set from a DependencyGraph. Indicate 
 * which artifacts you wish to include in the set by passing the artifactId(s) 
 * to one of the constructors.
 *
 * @author Paul Sterk
 */
public final class GraphRetentionSetFilter extends GraphFilter {
    private final Collection<String> artifactIds = new HashSet<String>();

    public GraphRetentionSetFilter(Collection<String> artifactIds) {
        this.artifactIds.addAll(artifactIds);
    }

    public GraphRetentionSetFilter(String... artifactIds) {
        this(Arrays.asList(artifactIds));
    }

    public GraphRetentionSetFilter() {
    }

    public DependencyGraph process() {
        try {
// Step 1. Subtract out all the artifacts specified in the artifactIds
            // collection by doing set subtraction
            ExcludeArtifactsTransitivelyFilter sbf = new ExcludeArtifactsTransitivelyFilter(artifactIds);
            DependencyGraph subtractionSet = evaluateChild().createSubGraph(sbf);

            // Step 2. Create the retention set by subtracting the artifacts in the
            // subtractionSet created in Step 1 from the original dependencyGraph set
            Collection<String> subSetArtifactIds = getArtifactIds(subtractionSet);
            ExcludeArtifactsTransitivelyFilter sbf2 = new ExcludeArtifactsTransitivelyFilter(subSetArtifactIds);
            return subtractionSet.createSubGraph(sbf2);
        } catch (IOException e) {
            throw new BuildException("Failed to resolve artifacts",e);
        }
    }
    
    private Collection<String> getArtifactIds(DependencyGraph graph) {
        Collection<String> ids = new HashSet<String>();
        Collection<DependencyGraph.Node> nodes = graph.getAllNodes();
        for (DependencyGraph.Node node : nodes) {
            ids.add(node.getProject().getArtifact().getArtifactId());
        }
        return ids;
    }
    
}
