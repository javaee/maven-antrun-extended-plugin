package org.jvnet.maven.plugin.antrun;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.maven.artifact.Artifact;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.condition.ConditionBase;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.apache.tools.ant.types.Path;
import java.util.Set;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.tools.ant.Project;

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
        log("Starting ResolveAllTasks.execute ", Project.MSG_DEBUG);
        try {
            MavenComponentBag w = MavenComponentBag.get();
            ArtifactResolutionResult result = w.resolveTransitively(
                groupId,
                artifactId,
                version,
                type,
                classifier);
            Set<Artifact> artifacts = result.getArtifacts();
            log("artifactId "+artifactId,  Project.MSG_DEBUG);
            log("number of artifacts "+artifacts.size(), Project.MSG_DEBUG);
            // For each artifact, get the pom file and see if the value for
            // <packaging/> child element matches the Condition
            Path path = null;
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
            if (path != null) {
                getProject().addReference(pathId, path);
            }
        } catch (Throwable t) {
            throw new BuildException(t);
        }
        log("Exiting ResolveAllTasks.execute ", Project.MSG_DEBUG);
    }
    
    private void handleArtifact(Artifact artifact, Path path) throws IOException {
        log("Starting ResolveAllTasks.handleArtifact "+todir, Project.MSG_DEBUG);
        if (path == null) {
            // Lazy instantiation
            path = new Path(getProject());
        }
        File artifactFile = artifact.getFile();
        path.createPathElement().setLocation(artifactFile);
        if (todir != null) {
            // If todir is not null, copy each artifact to the todir directory
            // Verify if todir exists
            File todirFile = new File(todir);
            if (! todirFile.exists()) {
                todirFile.mkdirs();
            }
            String outFileName = todir + File.separator + artifactFile.getName();
            FileInputStream in = null;
            FileOutputStream out = null;
            try {
                in = new FileInputStream(artifactFile);
                out = new FileOutputStream(outFileName);

                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (Throwable t) {
                        //ignore
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (Throwable t) {
                        //ignore
                    }
                }
            }
        }
        log("Exiting ResolveAllTasks.handleArtifact "+todir, Project.MSG_DEBUG);
    }

    public static final ThreadLocal<Artifact> CURRENT_ARTIFACT = new ThreadLocal<Artifact>();
}
