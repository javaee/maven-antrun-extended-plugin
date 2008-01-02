package org.jvnet.maven.plugin.antrun;

import org.apache.maven.artifact.Artifact;

import java.io.IOException;

/**
 * Represents &lt;artifact> element in build.xml
 *
 * <p>
 * This Java bean is a part of the XML configuration binding via Ant.
 *
 * @author Kohsuke Kawaguchi
 */
public class ArtifactElement {
    private String groupId, artifactId, version, type, classifier;

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getClassifier() {
        return classifier;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    /**
     * Creates an {@link Artifact} from the configured information,
     * by possibly guessing parameters that were missing.
     */
    public Artifact createArtifact(MavenComponentBag bag) throws IOException {
        return bag.createArtifactWithClassifier(groupId,artifactId,version,type,classifier);
    }
}
