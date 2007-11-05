package org.jvnet.maven.plugin.antrun;

import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.apache.tools.ant.BuildException;
import org.apache.maven.artifact.Artifact;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
        boolean matchesIs = false;        
        String packagingValue = null;
        try {
            Artifact artifact = ResolveAllTask.CURRENT_ARTIFACT.get();
            // Get the pom.xml file for each artifact
            ArtifactResolverWrapper w = ArtifactResolverWrapper.get();
            Artifact pom = w.createArtifactWithClassifier(
                artifact.getGroupId(),
                artifact.getArtifactId(),
                artifact.getVersion(),
                "pom",
                artifact.getClassifier());
            // Get the value of the <packaging/> element
            File pomFile = pom.getFile();
            DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = parser.parse(pomFile);
            Element element = document.getElementById("packaging");
            packagingValue = element.getNodeValue();
            if (packagingValue == null) {
                // Set to default Maven packaging type
                // TODO. Use a maven constant to set this value. Remove hard wiring value.
                packagingValue = "jar";
            }
            if (is.matches(packagingValue)) {
                matchesIs = true;
            }
        } catch (Throwable t) {
            throw new BuildException(t);
        }
        return matchesIs;
    }
}
