/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2010 Sun Microsystems, Inc. All rights reserved.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 * 
 * Contributor(s):
 * 
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

import org.apache.tools.ant.taskdefs.Expand;
import org.apache.tools.ant.util.FileUtils;

import java.io.File;

/**
 * Extends the unjar task to be able to specify dynamic destination directory.
 *
 * For example:
 *
 * <pre>
*
 *       <taskdef name="unjar" classname="org.jvnet.maven.plugin.antrun.UnJarTask"/>
 *       <unjar dest="/tmp/@filebasename@_jar">
 *           <fileset dir="/tmp/src">
 *               <include name="*.jar"/>
 *           </fileset>
 *       </unjar>
 *
 * produces:
 *
 *      [unjar] Expanding: /tmp/src/a.jar into /tmp/a_jar
 *      [unjar] Expanding: /tmp/src/b.jar into /tmp/b_jar
 *      [unjar] Expanding: /tmp/src/c.jar into /tmp/c_jar
 *      [unjar] Expanding: /tmp/src/d.jar into /tmp/d_jar
 *
 * </pre>
 * 
 * @author bhavanishankar@dev.java.net
 *
 */

public class UnJarTask extends Expand {

    @Override
    protected void expandFile(FileUtils fileUtils, File srcF, File dir) {
        String destDirName = dir.getAbsolutePath();
        if (destDirName.indexOf("@filebasename@") != -1) {
            String fileBaseName = getBaseName(srcF);
            destDirName = destDirName.replaceAll("@filebasename@", fileBaseName);
            File destDir = new File(destDirName);
            destDir.mkdirs();
            super.expandFile(fileUtils, srcF, destDir);
        } else {
            super.expandFile(fileUtils, srcF, dir);
        }
    }

    private String getBaseName(File file) {
        String srcFileName = file.getName();
        String fileBaseName = srcFileName.indexOf('.') != -1 ?
                srcFileName.substring(0, srcFileName.lastIndexOf('.')) : srcFileName;
        return fileBaseName;
    }
}
