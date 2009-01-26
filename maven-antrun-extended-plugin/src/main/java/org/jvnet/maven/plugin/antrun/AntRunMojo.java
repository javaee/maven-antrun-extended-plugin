package org.jvnet.maven.plugin.antrun;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.profiles.ProfileManager;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.taskdefs.Taskdef;
import org.apache.tools.ant.taskdefs.Typedef;

import java.beans.Introspector;
import java.io.File;
import java.util.List;
import java.net.URLClassLoader;
import java.net.MalformedURLException;
import java.net.URL;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
 * Maven AntRun Mojo.
 *
 * This plugin provides the capability of calling Ant tasks
 * from a POM by running the nested ant tasks inside the &lt;tasks/&gt;
 * parameter. It is encouraged to move the actual tasks to
 * a separate build.xml file and call that file with an
 * &lt;ant/&gt; task.
 *
 * @author <a href="mailto:kenney@apache.org">Kenney Westerhof</a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id: AntRunMojo.java 557316 2007-07-18 16:29:17Z kenney $
 * @configurator override
 * @goal run
 * @requiresDependencyResolution test
 */
// TODO: phase package
public class AntRunMojo
    extends AbstractAntMojo
{
    /**
     * The Maven project object
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The plugin dependencies.
     *
     * @parameter expression="${plugin.artifacts}"
     * @required
     * @readonly
     */
    private List pluginArtifacts;

    /**
     * The XML for the Ant task. You can add anything you can add
     * between &lt;target&gt; and &lt;/target&gt; in a build.xml.
     *
     * @parameter expression="${tasks}"
     */
    private Target tasks;
    
    /**
     * This folder is added to the list of those folders
     * containing source to be compiled. Use this if your
     * ant script generates source code.
     *
     * @parameter expression="${sourceRoot}"
     */
    private File sourceRoot;

    /**
     * This folder is added to the list of those folders
     * containing source to be compiled for testing. Use this if your
     * ant script generates test source code.
     *
     * @parameter expression="${testSourceRoot}"
     */
    private File testSourceRoot;

    /**
     * Used for resolving artifacts
     *
     * @component
     */
    private ArtifactResolver resolver;

    /**
     * Factory for creating artifact objects
     *
     * @component
     */
    private ArtifactFactory factory;

    /**
     * The local repository where the artifacts are located
     *
     * @parameter expression="${localRepository}"
     * @required
     */
    private ArtifactRepository localRepository;

    /**
     * The remote repositories where artifacts are located
     *
     * @parameter expression="${project.remoteArtifactRepositories}"
     */
    private List remoteRepositories;   

    /**
     * @component
     */
    private MavenProjectHelper projectHelper;
    
    /**
     * @component
     */
    protected ArtifactMetadataSource artifactMetadataSource;

    
    private MavenComponentBag bag;
    
    /**
     * @component
     */
    private ArtifactHandlerManager artifactHandlerManager;

    /**
     * @component
     */
    private MavenProjectBuilder mavenProjectBuilder;

// this is not available as a component
//    /**
//     * @component
//     */
//    private ProfileManager profileManager;

    /**
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
        throws MojoExecutionException
    {
        initArtifactResolverWrapper();
        /* Uncomment the following code to debug the MavenComponentBag        
        bag.setVerifyArtifact(false);
        try {
            bag.resolveTransitively("org.glassfish.web", "gf-web-connector", "10.0-SNAPSHOT", "jar", "runtime");
        } catch (Throwable t) {
            t.printStackTrace();
        }
         */
        try {
            executeTasks( tasks, project, pluginArtifacts );
        } finally {
            MavenComponentBag.reset();
        }

        if ( sourceRoot != null )
        {
            getLog().info( "Registering compile source root " + sourceRoot );
            project.addCompileSourceRoot( sourceRoot.toString() );
        }

        if ( testSourceRoot != null )
        {
            getLog().info( "Registering compile test source root " + testSourceRoot );
            project.addTestCompileSourceRoot( testSourceRoot.toString() );
        }
    }
    
    /*
     * This method is invoked to initialize the MavenComponentBag and
     * set it in ArtifactResolverWrapperThreadLocal.  This thread local class
     * can be used by other classes, such as Ant tasks, to obtain the
     * MavenComponentBag.
     */
    private void initArtifactResolverWrapper() {
        bag = new MavenComponentBag(resolver,
                factory,
                localRepository,
                remoteRepositories,
                project, projectHelper,
                artifactHandlerManager, 
                artifactMetadataSource,
                mavenProjectBuilder);
    }

    protected void configureProject(Project antProject) {
        // define all tasks
        for (Class task : TASKS) {
            typeDef(antProject, task, new Taskdef(), inferName(task, "Task"));
        }

        // define all filters
        for (Class filter : FILTERS) {
            typeDef(antProject, filter, new Typedef(), inferName(filter, "Filter"));
        }
        
        // expose basic properties
        antProject.setProperty("artifactId",project.getArtifactId());
        antProject.setProperty("groupId",project.getGroupId());
        antProject.setProperty("version",project.getVersion());
        antProject.setProperty("packaging",project.getPackaging());
    }

    private void typeDef(Project antProject, Class task, Typedef def, String name) {
        def.setName(name);
        def.setClassname(task.getName());
        def.setProject(antProject);
        def.execute();
    }

    private String inferName(Class task, String suffix) {
        String taskName = task.getSimpleName();
        if(taskName.endsWith(suffix)) // chop off suffix
            taskName = taskName.substring(0,taskName.length()-suffix.length());
        taskName = Introspector.decapitalize(taskName);
        return taskName;
    }

    private static final Class[] TASKS = new Class[] {
        ResolveArtifactTask.class,
        ResolveAllTask.class,
        AttachArtifactTask.class,
        GraphDefTask.class,
        IfTask.class
    };

    private static final Class[] FILTERS = new Class[] {
        RetentionSetFilter.class,
        ExcludeArtifactsTransitivelyFilter.class,
        GroupIdFilter.class,
        PackagingFilter.class,
        ManifestEntryFilter.class,
        DependencyExclusionFilter.class,
        RemoveSpecificArtifactsFilter.class,
        ScopeFilter.class,
        DumpGraphFilter.class,
        SubtractFilter.class,
        SubGraphFilter.class,
        FullGraphFilter.class,
        VisualizeFilter.class,
        GraphRefFilter.class
    };

    /*
        Mac JDKs don't have tools.jar (but they are just a part of rt.jar.
        Because of this, users of this plugin cannot add tools.jar to the dependency of antrun
        in a portable fashion --- doing so via plugin/dependencies/dependnecy as suggested in
        http://jira.codehaus.org/browse/MANTRUN-23 would break Mac builds.

        Typical error message is like this:
            [INFO] [antrun-extended:run {execution: default}]
            [INFO] Executing tasks
                [javac] Compiling 1 source file to /home/kohsuke/ws/gfv3/antrun/src/it/test-javac/target
            [INFO] ------------------------------------------------------------------------
            [ERROR] BUILD ERROR
            [INFO] ------------------------------------------------------------------------
            [INFO] An Ant BuildException has occured: Unable to find a javac compiler;
            com.sun.tools.javac.Main is not on the classpath.
            Perhaps JAVA_HOME does not point to the JDK

        So here we special-case tools.jar by locating them and adding them automatically to
        the classloader that loaded us.
     */
    static {
        try {
            Class.forName("com.sun.tools.javac.Main");
        } catch (ClassNotFoundException e) {
            // if not found, try to locate them
            File javaHome = new File(System.getProperty("java.home"));
            File toolsJar = new File(javaHome,"../lib/tools.jar");
            if(toolsJar.exists()) {
                ClassLoader cl = AntRunMojo.class.getClassLoader();
                if (cl instanceof URLClassLoader) {
                    URLClassLoader ucl = (URLClassLoader) cl;
                    try {
                        Method m = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                        m.setAccessible(true);
                        m.invoke(ucl,toolsJar.toURL());
                    } catch (MalformedURLException e1) {
                        // ignore the error and hope that the ant doesn't use javac 
                    } catch (InvocationTargetException e1) {
                        // ignore the error and hope that the ant doesn't use javac
                    } catch (NoSuchMethodException e1) {
                        // ignore the error and hope that the ant doesn't use javac
                    } catch (IllegalAccessException e1) {
                        // ignore the error and hope that the ant doesn't use javac
                    }
                }
            }
        }
    }
}
