package org.jvnet.maven.plugin.antrun;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.condition.Condition;

/**
 * @author Kohsuke Kawaguchi
 * @author Paul Sterk
 */
public class PackagingCondition implements Condition{
    
    private String is = "";
    
    public void setIs(String is) {
        this.is = is;
    }
    
    public boolean eval() throws BuildException {
        Artifact artifact = ResolveAllTask.CURRENT_ARTIFACT.get();
        // Get the pom.xml file for each artifact
        MavenComponentBag w = MavenComponentBag.get();
//            Artifact pom = w.createArtifactWithClassifier(
//                artifact.getGroupId(),
//                artifact.getArtifactId(),
//                artifact.getVersion(),
//                "pom",
//                artifact.getClassifier());
        // Get the value of the <packaging/> element
        try {
            // TODO: get MavenProjectBuilder into Bag
            MavenProjectBuilder builder = null;
            // TODO: reuse the laoded project among ohter conditions
            MavenProject p = builder.buildFromRepository(artifact, w.project.getRemoteArtifactRepositories(), w.localRepository);

            String packagingValue = p.getPackaging();
            if (packagingValue == null) {
                // Set to default Maven packaging type
                // TODO. Use a maven constant to set this value. Remove hard wiring value.
                packagingValue = "jar";
            }
            return is.matches(packagingValue);
        } catch (ProjectBuildingException e) {
            throw new BuildException("Failed to load POM for "+artifact,e);
        }
    }
}
