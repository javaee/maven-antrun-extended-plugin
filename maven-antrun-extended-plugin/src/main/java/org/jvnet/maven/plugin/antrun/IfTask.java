package org.jvnet.maven.plugin.antrun;

import org.apache.tools.ant.taskdefs.Sequential;
import org.apache.tools.ant.BuildException;
import org.apache.maven.profiles.Profile;
import org.apache.maven.profiles.activation.ProfileActivationException;

import java.util.List;

/**
 * Conditional execution.
 * 
 * @author Kohsuke Kawaguchi
 */
public class IfTask extends Sequential {
    private String isProfileActive;
    private Boolean test;

    public void setIsProfileActive(String profileName) {
        isProfileActive = profileName;
    }

    public void setTest(boolean value) {
        this.test = value;
    }

    public void execute() throws BuildException {
        if(test())
            super.execute();
    }

    private boolean test() throws BuildException {
        if(isProfileActive!=null) {
            try {
                for( Profile p : (List<Profile>) MavenComponentBag.get().profileManager.getActiveProfiles() ) {
                    if(p.getId().equals(isProfileActive))
                        return true;
                }
            } catch (ProfileActivationException e) {
                throw new BuildException(e);
            }
        }

        if(test!=null)
            return test;

        return false;
    }
}
