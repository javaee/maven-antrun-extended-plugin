package org.jvnet.maven.plugin.antrun;


import java.util.ArrayList;
import java.util.List;

/**
 * Use this filter to create a retention set from a DependencyGraph. Indicate 
 * which artifacts you wish to include in the set by passing the artifactId(s) 
 * to one of the constructors.
 *
 * @author Paul Sterk
 */
public final class GraphRetentionSetFilter extends GraphFilter {
    private final List<ArtifactElement> artifactElements = new ArrayList<ArtifactElement>();

    /**
     * Nested &lt;artifact> element can be used to specify what artifacts to exclude.
     */
    public void addConfiguredArtifact(ArtifactElement a) {
        // can't resolve this to artifact yet, because we don't have MavenComponentBag here.
        artifactElements.add(a);
    }

    public DependencyGraph process() {
        // Step 1. Subtract out all the artifacts specified in the artifactIds
        // collection by doing set subtraction
        ExcludeArtifactsTransitivelyFilter sbf = new ExcludeArtifactsTransitivelyFilter();
        for (ArtifactElement ae : artifactElements)
            sbf.addConfiguredArtifact(ae);
        DependencyGraph base = evaluateChild();

        final DependencyGraph subtractionSet = base.createSubGraph(sbf);

        // Step 2. Create the retention set by subtracting the artifacts in the
        // subtractionSet created in Step 1 from the original dependencyGraph set
        return base.createSubGraph(new DefaultGraphVisitor() {
            public boolean visit(DependencyGraph.Node node) {
                return !subtractionSet.contains(node);
            }
        });
    }
}
