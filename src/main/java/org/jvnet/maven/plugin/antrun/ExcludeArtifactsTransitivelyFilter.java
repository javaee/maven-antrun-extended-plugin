package org.jvnet.maven.plugin.antrun;


import org.apache.maven.artifact.Artifact;
import org.apache.tools.ant.BuildException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Filter {@link DependencyGraph} by excluding the specified set of artifacts. Any artifacts made
 * unreachable by this process will be also excluded.
 *
 * @author Paul Sterk
 */
public final class ExcludeArtifactsTransitivelyFilter extends GraphFilter implements GraphVisitor {
    /**
     * IDs of the artifacts to exclude. "groupId:artifactId:classifier".
     * These three are sufficient to identify an artifact uniquely within the context of single project
     * and its dependency.
     */
    private final Set<String> ids = new HashSet<String>();

    private final List<ArtifactElement> artifactElements = new ArrayList<ArtifactElement>();

    public ExcludeArtifactsTransitivelyFilter(Collection<String> artifactIds) throws IOException {
        for (String artifactId : artifactIds)
            addArtifactId(artifactId);
    }

    public ExcludeArtifactsTransitivelyFilter(String... artifactIds) throws IOException {
        this(Arrays.asList(artifactIds));
    }

    public ExcludeArtifactsTransitivelyFilter(String artifactId) throws IOException {
        addArtifactId(artifactId);
    }

    /**
     * Constructor for Ant.
     */
    public ExcludeArtifactsTransitivelyFilter() {
    }

    /**
     * Adds the artifact ID to {@link #ids}. Note that this requires us to infer other parameters like
     * groupId, version, etc.
     */
    private void addArtifactId(String artifactId) throws IOException {
        addArtifact(MavenComponentBag.get().resolveArtifactUsingMavenProjectArtifacts(artifactId));
    }

    private void addArtifact(Artifact a) {
        ids.add(a.getGroupId()+':'+a.getArtifactId()+':'+a.getClassifier());
    }

    /**
     * Nested &lt;artifact> element can be used to specify what artifacts to exclude.
     */
    public void addConfiguredArtifact(ArtifactElement a) {
        // can't resolve this to artifact yet, because we don't have MavenComponentBag here.
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
            for (ArtifactElement ae : artifactElements)
                addArtifact(ae.createArtifact());
            artifactElements.clear();
        } catch (IOException e) {
            throw new BuildException(e);
        }

        // If the artifact matches an artifact in the artifacts Set, do not 
        // include in the subgraph. Indicate this by returning 'false'.
        return !ids.contains(node.getId());
    }

    public boolean visit(DependencyGraph.Edge edge) {
        return true;
    }
}
