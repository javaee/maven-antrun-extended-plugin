package org.jvnet.maven.plugin.antrun;


import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import java.io.IOException;

/**
 * Use this filter to create a retention set from a DependencyGraph. Indicate 
 * which artifacts you wish to include in the set by passing the artifactId(s) 
 * to one of the constructors.
 *
 * @author Paul Sterk
 */
public final class GraphRetentionSetFilter extends GraphFilter {
    private ArtifactElement root;

    /**
     * Nested &lt;artifact> element can be used to specify what artifacts to exclude.
     */
    public void addConfiguredArtifact(ArtifactElement a) {
        // can't resolve this to artifact yet, because we don't have MavenComponentBag here.
        if(root!=null)
            throw new BuildException("Only one <artifact> is allowed");
        root = a;
    }

    public DependencyGraph process() {
        try {
            final DependencyGraph base = evaluateChild();

            // Step 1. Subtract out all the artifacts specified in the artifactIds
            // collection by doing set subtraction
            ExcludeArtifactsTransitivelyFilter sbf = new ExcludeArtifactsTransitivelyFilter();
            sbf.addConfiguredArtifact(root);
            final DependencyGraph subtractionSet = base.createSubGraph(sbf);

            // Step 2. Create the retention set by subtracting the artifacts in the
            // subtractionSet created in Step 1 from the original dependencyGraph set
            DependencyGraph g = base.createSubGraph(
                base.toNode(root.createArtifact()),
                new DefaultGraphVisitor() {
                    public boolean visit(DependencyGraph.Node node) {
                        return !subtractionSet.contains(node);
                    }
                });

            log(getClass().getSimpleName()+" -> "+g,Project.MSG_DEBUG);

            return g;
        } catch (ProjectBuildingException e) {
            throw new BuildException("Failed to resolve artifacts",e);
        } catch (AbstractArtifactResolutionException e) {
            throw new BuildException("Failed to resolve artifacts",e);
        } catch (IOException e) {
            throw new BuildException("Failed to resolve artifacts",e);
        }
    }
}
