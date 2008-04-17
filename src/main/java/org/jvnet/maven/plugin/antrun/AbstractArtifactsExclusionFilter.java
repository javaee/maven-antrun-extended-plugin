package org.jvnet.maven.plugin.antrun;

import org.apache.maven.artifact.Artifact;
import org.apache.tools.ant.BuildException;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Arrays;
import java.io.IOException;

/**
 * Base class for {@link ListFilter}s that takes several nested &lt;artifact> elements as parameters
 * to identify artifacts. 
 *
 * @author Kohsuke Kawaguchi
 */
abstract class AbstractArtifactsExclusionFilter extends ListFilter {
    /**
     * IDs of the artifacts to exclude. "groupId:artifactId:classifier".
     * These three are sufficient to identify an artifact uniquely within the context of single project
     * and its dependency.
     */
    protected final Set<String> ids = new HashSet<String>();

    private final List<ArtifactElement> artifactElements = new ArrayList<ArtifactElement>();

    protected AbstractArtifactsExclusionFilter(Collection<String> artifactIds) throws IOException {
        for (String artifactId : artifactIds)
            addArtifactId(artifactId);
    }

    protected AbstractArtifactsExclusionFilter(String... artifactIds) throws IOException {
        this(Arrays.asList(artifactIds));
    }

    protected AbstractArtifactsExclusionFilter(String artifactId) throws IOException {
        addArtifactId(artifactId);
    }

    // for Ant
    protected AbstractArtifactsExclusionFilter() {}

    /**
     * Resolves all the artifacts and computes {@link #ids}.
     *
     * This normally needs to be done at {@link #visit(DependencyGraph.Node)},
     * because this implementation could be used as a filter.
     * Can be invoked multiple times safely.
     */
    protected final void resolve() {
        try {
            for (ArtifactElement ae : artifactElements)
                addArtifact(ae.createArtifact());
            artifactElements.clear();
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Adds the artifact ID to {@link #ids}. Note that this requires us to infer other parameters like
     * groupId, version, etc.
     */
    private void addArtifactId(String artifactId) throws IOException {
        addArtifact(MavenComponentBag.get().resolveArtifactUsingMavenProjectArtifacts(artifactId));
    }

    protected void addArtifact(Artifact a) {
        ids.add(a.getGroupId()+':'+a.getArtifactId()+':'+a.getClassifier());
    }

    /**
     * Nested &lt;artifact> element can be used to specify what artifacts to exclude.
     */
    public void addConfiguredArtifact(ArtifactElement a) {
        // can't resolve this to artifact yet, because we don't have MavenComponentBag here.
        artifactElements.add(a);
    }
}
