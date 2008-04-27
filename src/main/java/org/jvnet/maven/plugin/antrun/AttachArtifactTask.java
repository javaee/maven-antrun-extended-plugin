package org.jvnet.maven.plugin.antrun;

import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import java.io.File;

/**
 * Attaches the artifact to Maven.
 *
 * @author Kohsuke Kawaguchi
 */
public class AttachArtifactTask extends Task {

    private File file;

    private String classifier;

    private String type;

    /**
     * The file to be treated as an artifact.
     */
    public void setFile(File file) {
        this.file = file;
    }

    /**
     * Optional classifier. If left unset, the task will
     * attach the main artifact.
     */
    public void setClassifier(String classifier) {
        if(classifier!=null && classifier.length()==0)
            classifier = null;
        this.classifier = classifier;
    }

    /**
     * Artifact type. Think of it as a file extension.
     * Optional, and if omitted, infered from the file extension.
     */
    public void setType(String type) {
        this.type = type;
    }

    public void execute() throws BuildException {
        final MavenComponentBag w = MavenComponentBag.get();

        if(classifier==null) {
            if(type!=null)
                throw new BuildException("type is set but classifier is not set");
            log("Attaching "+file, Project.MSG_VERBOSE);
            w.project.getArtifact().setFile(file);

            // Even if you define ArtifactHandlers as components, often because of the
            // initialization order, a proper ArtifactHandler won't be discovered.
            // so force our own ArtifactHandler that gets the extension right.
            ArtifactHandler handler = new ArtifactHandler() {
                public String getExtension() {
                    return AttachArtifactTask.this.getExtension(file.getName());
                }

                public String getDirectory() {
                    return null;
                }

                public String getClassifier() {
                    return null;
                }

                public String getPackaging() {
                    return w.project.getPackaging();
                }

                public boolean isIncludesDependencies() {
                    return false;
                }

                public String getLanguage() {
                    return null;
                }

                public boolean isAddedToClasspath() {
                    return false;
                }
            };
            w.project.getArtifact().setArtifactHandler(handler);
        } else {
            log("Attaching "+file+" as an attached artifact", Project.MSG_VERBOSE);

            String type = this.type;
            if(type==null)  type = getExtension(file.getName());

            w.projectHelper.attachArtifact(w.project,type,classifier,file);
        }
    }

    private String getExtension(String name) {
        int idx = name.lastIndexOf('.');
        return name.substring(idx+1);
    }
}
