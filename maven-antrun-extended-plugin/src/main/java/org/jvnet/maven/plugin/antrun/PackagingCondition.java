package org.jvnet.maven.plugin.antrun;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.jvnet.maven.plugin.antrun.DependencyGraph.Node;

/**
 * Filtering {@link Condition} that removes all artifacts that don't match
 * the given packaging type.
 *
 * @author Kohsuke Kawaguchi
 * @author Paul Sterk
 */
public class PackagingCondition extends GraphVisitingCondition {
    
    private String is;
    
    public void setIs(String is) {
        this.is = is;
    }

    @Override
    public boolean filter(Node n) {
        if(is==null)
            throw new BuildException("<packaging> condition requires @is");

        MavenProject p = n.getProject();
        if(p==null)     return false;

        String pkg = p.getPackaging();
        if(pkg==null) {
            // Set to default Maven packaging type
            // TODO. Use a maven constant to set this value. Remove hard wiring value.
            pkg = "jar";
        }
        return is.matches(pkg);
    }
}
