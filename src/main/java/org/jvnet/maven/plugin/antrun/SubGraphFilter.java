package org.jvnet.maven.plugin.antrun;

import org.apache.tools.ant.BuildException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.project.ProjectBuildingException;

import java.io.IOException;

/**
 * Computes a full subgraph rooted on the given node.
 * @author Kohsuke Kawaguchi
 */
public class SubGraphFilter extends GraphFilter {
    private String groupId, artifactId, version, type, classifier;

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    public DependencyGraph process() {
        try {
            MavenComponentBag bag = MavenComponentBag.get();
            Artifact a = bag.createArtifactWithClassifier(groupId, artifactId, version, type, classifier);

            DependencyGraph g = evaluateChild();
            return g.createSubGraph(g.toNode(a));
        } catch (IOException e) {
            throw new BuildException("Failed to resolve artifacts",e);
        } catch (ProjectBuildingException e) {
            throw new BuildException("Failed to resolve artifacts",e);
        } catch (AbstractArtifactResolutionException e) {
            throw new BuildException("Failed to resolve artifacts",e);
        }
    }
}
