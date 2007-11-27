package org.jvnet.maven.plugin.antrun;

import org.apache.maven.artifact.Artifact;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.condition.ConditionBase;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.apache.tools.ant.types.Path;
import java.util.Set;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;

/**
 * @author Kohsuke Kawaguchi
 * @author Paul Sterk
 */
public class ResolveAllTask extends ConditionBase {
    
    private String todir;
    
    private String pathId;
    
    private String property,groupId,artifactId,version,type="jar",classifier;

    public void setProperty(String property) {
        this.property = property;
    }
    
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
    
    public void setTodir(String todir) {
        this.todir = todir;
    }
    
    public void setPathId(String pathId) {
        this.pathId = pathId;
    }
    
    public void execute() throws BuildException {   
        try {
            MavenComponentBag w = MavenComponentBag.get();
            ArtifactResolutionResult result = w.resolveTransitively(
                groupId,
                artifactId,
                version,
                type,
                classifier);
            Set<Artifact> artifacts = result.getArtifacts();
            // For each artifact, get the pom file and see if the value for
            // <packaging/> child element matches the Condition
            Path path = null;
            for (Artifact artifact : artifacts) {
                CURRENT_ARTIFACT.set(artifact);
                if (countConditions() > 1) {
                    throw new BuildException("You must not nest more than one "
                        + "condition into <condition>");
                }
                if (countConditions() < 1) {
                    throw new BuildException("You must nest a condition into "
                        + "<condition>");
                }
                Condition c = (Condition) getConditions().nextElement();            
                // The current Artifact is set as a ThreadLocal variable. Invoke
                // the Condition.eval method to see if this Artifact matches the
                // condition expression
                if (c.eval()) {
                    if (path == null) {
                        // Lazy instantiation
                        path = new Path(getProject());
                    }
                    path.createPathElement().setLocation(artifact.getFile());
                }
            }
            if (path != null) {
                getProject().addReference(pathId, path);
            }
        } catch (Throwable t) {
            throw new BuildException(t);
        }
    }

    public static final ThreadLocal<Artifact> CURRENT_ARTIFACT = new ThreadLocal<Artifact>();
}
