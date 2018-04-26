/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

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
