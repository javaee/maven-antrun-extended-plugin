package org.jvnet.maven.plugin.antrun;

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
        ArtifactResolverWrapper w = ArtifactResolverWrapper.get();

        if(classifier==null) {
            if(type!=null)
                throw new BuildException("type is set but classifier is not set");
            log("Attaching "+file, Project.MSG_VERBOSE);
            w.getProject().getArtifact().setFile(file);
        } else {
            log("Attaching "+file+" as an attached artifact", Project.MSG_VERBOSE);

            String type = this.type;
            if(type==null)  type = getExtension(file.getName());

            w.projectHelper.attachArtifact(w.getProject(),type,classifier,file);
        }
    }

    private String getExtension(String name) {
        int idx = name.lastIndexOf('.');
        return name.substring(idx+1);
    }
}
