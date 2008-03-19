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
 * <p>
 * Informally, a node will remain in the graph
 * only when it's reachable from the root without going through
 * any of the excluded artifacts.
 *
 * <p>
 * Here's the format definition.
 * <p>
 * Let normalize(G={r,V,E}) -> G'={r,V',E'} be defined as follows.
 * This is an operation to remove unreachable nodes and edges.
 *
 * <pre>
 *  V' = { v | \exists r ->* v in G }
 *  E' = { (u,v) | u \in V' and v \in V' }
 * </pre>
 *
 * Given the graph G=(r,V,E) and exclusion nodes N,
 * the new graph G' is defined as follows:
 *
 * <pre>
 * G'=normalize(r,V-N),E)
 * </pre>
 *
 * @author Paul Sterk
 * @author Kohsuke Kawaguchi
 * @see RemoveSpecificArtifactsFilter
 */
public final class ExcludeArtifactsTransitivelyFilter extends AbstractArtifactsExclusionFilter {
    public ExcludeArtifactsTransitivelyFilter(Collection<String> artifactIds) throws IOException {
        super(artifactIds);
    }

    public ExcludeArtifactsTransitivelyFilter(String... artifactIds) throws IOException {
        super(artifactIds);
    }

    public ExcludeArtifactsTransitivelyFilter(String artifactId) throws IOException {
        super(artifactId);
    }

    public ExcludeArtifactsTransitivelyFilter() {
    }

    public boolean visit(DependencyGraph.Node node) {
        resolve();

        // If the artifact matches an artifact in the artifacts Set, do not
        // include in the subgraph. Indicate this by returning 'false'.
        return !ids.contains(node.getId());
    }
}
