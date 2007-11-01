package org.apache.maven.plugin.antrun;

import org.apache.maven.artifact.Artifact;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.condition.ConditionBase;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.apache.tools.ant.types.Path;

import java.io.File;
import java.util.List;

/**
 * @author Kohsuke Kawaguchi
 */
public class ResolveAllTask extends ConditionBase {
    public void execute() throws BuildException {
        // exmaple --- how to expose a path

        File[] files = new File[5];
        Path p = new Path(getProject());

        for (File f : files) {
            p.createPathElement().setLocation(f);
        }

        getProject().addReference("id",p);


        // example -- how to evaluate condition?
        List<Artifact> artifacts = null;

        for (Artifact a : artifacts) {
            CURRENT_ARTIFACT.set(a);
            if (countConditions() > 1) {
                throw new BuildException("You must not nest more than one "
                    + "condition into <condition>");
            }
            if (countConditions() < 1) {
                throw new BuildException("You must nest a condition into "
                    + "<condition>");
            }
            Condition c = (Condition) getConditions().nextElement();
            if (c.eval())
                ; // TODO: include in the list
        }
    }

    public static final ThreadLocal<Artifact> CURRENT_ARTIFACT = new ThreadLocal<Artifact>();
}
