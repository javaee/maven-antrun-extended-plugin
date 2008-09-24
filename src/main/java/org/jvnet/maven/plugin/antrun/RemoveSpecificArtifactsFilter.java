package org.jvnet.maven.plugin.antrun;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.HashSet;

/**
 * Filter {@link DependencyGraph} by excluding artifacts that are specific to the given artifacts.
 *
 * <p>
 * By specific, I mean an artifact that's only depended on by one and the only one of the
 * given artifacts.
 *
 * @author Kohsuke Kawaguchi
 */
public class RemoveSpecificArtifactsFilter extends AbstractArtifactsExclusionFilter {

    /**
     * Nodes to be retained.
     */
    private Set<DependencyGraph.Node> nodes;

    public RemoveSpecificArtifactsFilter(Collection<String> artifactIds) throws IOException {
        super(artifactIds);
    }

    public RemoveSpecificArtifactsFilter(String... artifactIds) throws IOException {
        super(artifactIds);
    }

    public RemoveSpecificArtifactsFilter(String artifactId) throws IOException {
        super(artifactId);
    }

    public RemoveSpecificArtifactsFilter() {
    }

    public boolean visit(DependencyGraph.Node node) {
        resolve();

        if(!ids.isEmpty()) {
            final DependencyGraph base = evaluateChild();
            nodes = new HashSet<DependencyGraph.Node>(base.getAllNodes());
            for (String id : ids) {
                ExcludeArtifactsTransitivelyFilter exf = new ExcludeArtifactsTransitivelyFilter();
                exf.ids.add(id);
                DependencyGraph x = base.createSubGraph(exf);
                nodes.retainAll(x.getAllNodes());
            }
        }

        return nodes.contains(node);
    }
}
