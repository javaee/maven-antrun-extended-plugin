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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ProjectComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * Filter a {@link DependencyGraph} based on configuration by Ant.
 * 
 * @author psterk
 */
public abstract class GraphFilter extends ProjectComponent {
    protected final List<GraphFilter> children = new ArrayList<GraphFilter>();


    public abstract DependencyGraph process();

    /**
     * Adds another child. Ant will invoke this for each child element given in build script.
     */
    public void add(GraphFilter child) {
        children.add(child);
    }

    /**
     * Evaluate the n-th child {@link GraphFilter}. If omitted, it returns the input graph,
     * so that the full graph can be given as an input implicitly. Whether this defaulting
     * is a good idea or not, it's hard to say.
     */
    protected DependencyGraph evaluateChild(int index) {
        if(children.size()<=index)
            return CURRENT_INPUT.get();
        else
            return children.get(index).process();
    }

    /**
     * Short for {@code evaluateChild(0)}, for those fitlers that only have one child.
     */
    protected final DependencyGraph evaluateChild() {
        if(children.size()>1)
            throw new BuildException("Too many children in "+getClass().getName());
        return evaluateChild(0);
    }

    /*package*/ static final ThreadLocal<DependencyGraph> CURRENT_INPUT = new ThreadLocal<DependencyGraph>();

}
