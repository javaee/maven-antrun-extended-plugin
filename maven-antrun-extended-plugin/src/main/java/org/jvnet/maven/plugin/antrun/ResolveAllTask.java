package org.jvnet.maven.plugin.antrun;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.apache.tools.ant.taskdefs.condition.ConditionBase;
import org.apache.tools.ant.types.Path;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * Transitively resolve dependencies, perform some filtering, and deliver the resulting
 * set of artifacts in various forms (as a new {@link Path} object, into a directory, etc.)
 *
 * @author Kohsuke Kawaguchi
 * @author Paul Sterk
 */
public class ResolveAllTask extends ConditionBase {
    
    private File todir;
    
    private String pathId;
    
    private String property;

    private String groupId,artifactId,version,type="jar",classifier;

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
    
    public void setTodir(File todir) {
        this.todir = todir;
        todir.mkdirs();
    }
    
    public void setPathId(String pathId) {
        this.pathId = pathId;
    }
    
    public void execute() throws BuildException {  
        log("Starting ResolveAllTasks.execute ", Project.MSG_DEBUG);
        try {
            MavenComponentBag w = MavenComponentBag.get();
            Set<Artifact> artifacts;

            DependencyGraph g;
            // TODO: we need to be able to specify scope of the resolution

            if(groupId==null && artifactId==null && version==null) {
                // if no clue is given whatsoever, use all the project dependencies
                artifacts = w.project.getArtifacts();
                g = new DependencyGraph(w.project.getArtifact());
            } else {
                // otherwise pick up dependencies from the specified artifact
                ArtifactResolutionResult result = w.resolveTransitively(
                    groupId,
                    artifactId,
                    version,
                    type,
                    classifier);
                artifacts = result.getArtifacts();
                g = new DependencyGraph(w.createArtifactWithClassifier(groupId,artifactId,version,type,classifier));
                log("artifactId "+artifactId,  Project.MSG_DEBUG);
            }
            System.out.println(g);

            log("number of artifacts "+artifacts.size(), Project.MSG_DEBUG);
            // For each artifact, get the pom file and see if the value for
            // <packaging/> child element matches the Condition
            Path path = new Path(getProject());
            for (Artifact artifact : artifacts) {
                CURRENT_ARTIFACT.set(artifact);
                if (countConditions() > 1) {
                    throw new BuildException("You must not nest more than one "
                        + "condition into <condition>");
                }
                if (countConditions()==1) {
                    Condition c = (Condition) getConditions().nextElement();
                    // The current Artifact is set as a ThreadLocal variable. Invoke
                    // the Condition.eval method to see if this Artifact matches the
                    // condition expression
                    if (!c.eval())
                        continue;   // rejected
                }
                
                handleArtifact(artifact, path);
            }
            if (pathId != null) {
                getProject().addReference(pathId, path);
            }
        } catch (Throwable t) {
            throw new BuildException(t);
        }
        log("Exiting ResolveAllTasks.execute ", Project.MSG_DEBUG);
    }
    
    private void handleArtifact(Artifact artifact, Path path) throws IOException {
        log("Starting ResolveAllTasks.handleArtifact "+todir, Project.MSG_DEBUG);
        File artifactFile = artifact.getFile();
        path.createPathElement().setLocation(artifactFile);
        // If todir is not null, copy each artifact to the todir directory
        if (todir != null) {
            File outFile = new File(todir,artifactFile.getName());
            FileUtils.copyFile(artifactFile,outFile);
        }
        log("Exiting ResolveAllTasks.handleArtifact "+todir, Project.MSG_DEBUG);
    }

    public static final ThreadLocal<Artifact> CURRENT_ARTIFACT = new ThreadLocal<Artifact>();
}
