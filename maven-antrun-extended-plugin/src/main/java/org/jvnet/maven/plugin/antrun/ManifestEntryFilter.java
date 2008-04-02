package org.jvnet.maven.plugin.antrun;

import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.tools.ant.BuildException;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.jar.Attributes;

/**
 * Filter out a {@link DependencyGraph} by looking at manifest entries in the jar file.
 *
 * @author Kohsuke Kawaguchi
 */
public class ManifestEntryFilter extends ListFilter {
    private String entry;

    /**
     * The manifest main entry name that the jar has to have.
     */
    public void setHas(String v) {
        entry = v;
    }

    public boolean visit(DependencyGraph.Node node) {
        try {
            File v = node.getArtifactFile();
            if(v==null)     return false;   // whether this is the right behavior is worth an argument

            JarFile jar = new JarFile(v);
            try {
                Manifest m = jar.getManifest();
                if(m==null)     return false;
                Attributes att = m.getMainAttributes();
                if(att==null)   return false;   // don't know if this can ever happen

                return att.getValue(entry)!=null;
            } finally {
                jar.close();
            }
        } catch (AbstractArtifactResolutionException e) {
            throw new BuildException("Failed to filter "+node,e);
        } catch (IOException e) {
            throw new BuildException("Failed to filter "+node,e);
        }
    }
}
