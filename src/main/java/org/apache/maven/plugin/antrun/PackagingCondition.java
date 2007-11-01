package org.apache.maven.plugin.antrun;

import org.apache.tools.ant.taskdefs.condition.Condition;
import org.apache.tools.ant.BuildException;
import org.apache.maven.artifact.Artifact;

/**
 * @author Kohsuke Kawaguchi
 */
public class PackagingCondition implements Condition{
    public boolean eval() throws BuildException {
        Artifact a = ResolveAllTask.CURRENT_ARTIFACT.get();
        // TODO: resolve POM
        //
        return true;
    }
}
