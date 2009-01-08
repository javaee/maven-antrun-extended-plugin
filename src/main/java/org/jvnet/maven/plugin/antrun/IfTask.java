package org.jvnet.maven.plugin.antrun;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Sequential;

/**
 * Conditional execution.
 * 
 * @author Kohsuke Kawaguchi
 */
public class IfTask extends Sequential {
    private Boolean test;

    public void setTest(boolean value) {
        this.test = value;
    }

    public void execute() throws BuildException {
        if(test())
            super.execute();
    }

    private boolean test() throws BuildException {
        if(test!=null)
            return test;

        return false;
    }
}
