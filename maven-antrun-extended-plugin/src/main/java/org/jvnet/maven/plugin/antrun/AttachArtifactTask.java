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

import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import java.io.File;

/**
 * Attaches the artifact to Maven.
 *
 * @author Kohsuke Kawaguchi
 */
public class AttachArtifactTask extends Task {

    private File file;

    private String classifier;

    private String type;

    /**
     * The file to be treated as an artifact.
     */
    public void setFile(File file) {
        this.file = file;
    }

    /**
     * Optional classifier. If left unset, the task will
     * attach the main artifact.
     */
    public void setClassifier(String classifier) {
        if(classifier!=null && classifier.length()==0)
            classifier = null;
        this.classifier = classifier;
    }

    /**
     * Artifact type. Think of it as a file extension.
     * Optional, and if omitted, infered from the file extension.
     */
    public void setType(String type) {
        this.type = type;
    }

    public void execute() throws BuildException {
        final MavenComponentBag w = MavenComponentBag.get();

        if(classifier==null) {
            if(type!=null)
                throw new BuildException("type is set but classifier is not set");
            log("Attaching "+file, Project.MSG_VERBOSE);
            w.project.getArtifact().setFile(file);

            // Even if you define ArtifactHandlers as components, often because of the
            // initialization order, a proper ArtifactHandler won't be discovered.
            // so force our own ArtifactHandler that gets the extension right.
            ArtifactHandler handler = new ArtifactHandler() {
                public String getExtension() {
                    return AttachArtifactTask.this.getExtension(file.getName());
                }

                public String getDirectory() {
                    return null;
                }

                public String getClassifier() {
                    return null;
                }

                public String getPackaging() {
                    return w.project.getPackaging();
                }

                public boolean isIncludesDependencies() {
                    return false;
                }

                public String getLanguage() {
                    return null;
                }

                public boolean isAddedToClasspath() {
                    return false;
                }
            };
            w.project.getArtifact().setArtifactHandler(handler);
        } else {
            log("Attaching "+file+" as an attached artifact", Project.MSG_VERBOSE);

            String type = this.type;
            if(type==null)  type = getExtension(file.getName());

            w.projectHelper.attachArtifact(w.project,type,classifier,file);
        }
    }

    private String getExtension(String name) {
        int idx = name.lastIndexOf('.');
        return name.substring(idx+1);
    }
}
