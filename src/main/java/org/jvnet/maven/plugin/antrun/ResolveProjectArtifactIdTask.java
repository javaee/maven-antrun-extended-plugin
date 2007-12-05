package org.jvnet.maven.plugin.antrun;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * This class Ant Task is used to obtain the name of the fully qualified
 * Maven artifactId (<artifactId>-<version>.<extension> ) from the local
 * project pom.xml file.
 * 
 * @author psterk
 */
public class ResolveProjectArtifactIdTask extends Task {
    
    // Avoid NPEs by setting a default value
    private String property = "artifactId";
    
    public void setProperty(String property) {
        this.property = property;
    }
    
    public void execute() throws BuildException {
        log("Project artifactId property name: "+property, Project.MSG_VERBOSE);
        final MavenComponentBag w = MavenComponentBag.get();
        String artifactId = w.project.getArtifactId();
        String version = w.project.getVersion();
        String packaging = w.project.getPackaging();
        String fileExtension = "";
        if (packaging.equalsIgnoreCase("distribution-base-zip") ||
            packaging.equalsIgnoreCase("glassfish-distribution")) {
            fileExtension = "zip";
        } else {
            fileExtension = packaging;
        }
        StringBuffer sb = new StringBuffer(artifactId);
        sb.append("-").append(version).append(".").append(fileExtension);
        log("Project artifactId: "+sb.toString(), Project.MSG_VERBOSE);
        getProject().setProperty(property, sb.toString());
    }
}
