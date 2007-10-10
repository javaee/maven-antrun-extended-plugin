package org.apache.maven.plugin.antrun;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.maven.artifact.Artifact;

/**
 * Ant task that resolves an artifact through Maven.
 *
 * TODO: implement the actual resolution logic
 * TODO: support more convenient syntax
 * TODO: resolve dependency transitively.
 * 
 * @author Paul Sterk
 * @author Kohsuke Kawaguchi
 */
public class ResolveArtifactTask extends Task {
    private String groupId,artifactId,version,classifier;

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    public void execute() throws BuildException {
        log("Starting execute", Project.MSG_DEBUG);
        try {
            ArtifactResolverWrapper w = ArtifactResolverWrapper.get();
            Artifact a = w.getFactory().createArtifactWithClassifier(groupId, artifactId, version, null, classifier);
            w.getResolver().resolve(a, w.getRemoteRepositories(), w.getLocalRepository());
        } catch (Throwable ex) {
            log("Problem resolving artifact: "+ex.getMessage(), Project.MSG_ERR);
            throw new BuildException(ex);
        }
        log("Exiting execute", Project.MSG_DEBUG);
    }
}
