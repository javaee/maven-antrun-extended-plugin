package org.jvnet.maven.plugin.antrun;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.apache.tools.ant.taskdefs.condition.ConditionBase;
import org.apache.tools.ant.types.Path;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.Collection;
import java.util.HashSet;

/**
 * @author Kohsuke Kawaguchi
 * @author Paul Sterk
 */
public class ResolveAllTask extends ConditionBase {
    
    private File todir;
    
    private String pathId;
    
    private String property;

    private String groupId,artifactId,version,type="jar",classifier;

    private boolean ignoreDuplicates = false;

    public void setProperty(String property) {
        this.property = property;
    }
    
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }
    
    public void setTodir(File todir) {
        this.todir = todir;
        todir.mkdirs();
    }
    
    public void setPathId(String pathId) {
        this.pathId = pathId;
    }

    public void ignoreDuplicates(boolean ignoreDuplicates) {
        this.ignoreDuplicates = ignoreDuplicates;
    }

    public void execute() throws BuildException {  
        log("Starting ResolveAllTasks.execute ", Project.MSG_DEBUG);
        try {
            MavenComponentBag w = MavenComponentBag.get();
            Set<Artifact> artifacts;

            // TODO: we need to be able to specify scope of the resolution

            if(groupId==null && artifactId==null && version==null) {
                // if no clue is given whatsoever, use all the project dependencies
                artifacts = w.project.getArtifacts();
            } else {
                // otherwise pick up dependencies from the specified artifact
                ArtifactResolutionResult result = w.resolveTransitively(
                    groupId,
                    artifactId,
                    version,
                    type,
                    classifier);
                artifacts = result.getArtifacts();
                log("artifactId "+artifactId,  Project.MSG_DEBUG);
            }

            log("number of artifacts "+artifacts.size(), Project.MSG_DEBUG);
            // For each artifact, get the pom file and see if the value for
            // <packaging/> child element matches the Condition
            Path path = new Path(getProject());
            Collection duplicateFileLookup = new HashSet();
            for (Artifact artifact : artifacts) {
                CURRENT_ARTIFACT.set(artifact);
                if (countConditions() > 1) {
                    throw new BuildException("You must not nest more than one "
                        + "condition into <condition>");
                }
                if (countConditions()==1) {
                    Condition c = (Condition) getConditions().nextElement();
                    // The current Artifact is set as a ThreadLocal variable. Invoke
                    // the Condition.eval method to see if this Artifact matches the
                    // condition expression
                    if (!c.eval())
                        continue;   // rejected
                }
                handleArtifact(artifact, path, duplicateFileLookup);
            }
            if (pathId != null) {
                getProject().addReference(pathId, path);
            }
        } catch (Throwable t) {
            throw new BuildException(t);
        }
        log("Exiting ResolveAllTasks.execute ", Project.MSG_DEBUG);
    }

    private void handleArtifact(Artifact artifact, Path path, Collection duplicateFileLookup) throws IOException {
        log("Starting ResolveAllTasks.handleArtifact ", Project.MSG_DEBUG);
        // Check to see if the jar file already exists in the toDir directory tree
        if (! ignoreDuplicate(artifact, todir, duplicateFileLookup)) {
            File artifactFile = artifact.getFile();
            path.createPathElement().setLocation(artifactFile);
            // If todir is not null, copy each artifact to the todir directory
            if (todir != null) {
                File outFile = new File(todir,artifactFile.getName());
                FileUtils.copyFile(artifactFile,outFile);
            }
        }
        log("Exiting ResolveAllTasks.handleArtifact ", Project.MSG_DEBUG);
    }

    private boolean ignoreDuplicate(Artifact artifact, File dir, Collection duplicateFileLookup) {
        log("Entering ResolveAllTasks.ignoreDuplicate", Project.MSG_DEBUG);
        boolean ignoreDuplicate = false;
        if (ignoreDuplicates) {
            ignoreDuplicate = true;
        } else {
            log("artifact id: "+artifact.getId(), Project.MSG_DEBUG);
            String artifactName = getFullArtifactName(artifact);
            log("Check for duplicates using this artifactName: "+artifactName, Project.MSG_DEBUG);
            if (duplicateFileLookup.isEmpty()) {
                // populate the Map with files that currently exist in the path specified by
                // this.todir
                createDuplicateFileLookup(dir, duplicateFileLookup);
            }
            if (duplicateFileLookup.contains(artifactName)) {
                log("Found duplicate: " + artifactName, Project.MSG_DEBUG);
                ignoreDuplicate = true;
            }
        }
        log("Exiting ResolveAllTasks.ignoreDuplicate", Project.MSG_DEBUG);
        return ignoreDuplicate;
    }

    private String getFullArtifactName(Artifact artifact) {
        StringBuffer sb = new StringBuffer(artifact.getArtifactId());
        sb.append("-").append(artifact.getVersion());
        sb.append(".").append(artifact.getType());
        return sb.toString();
    }
    
    private void createDuplicateFileLookup(File dir, Collection<String> duplicateFileLookup) {
        log("Entering ResolveAllTasks.createDuplicateFileLookup", Project.MSG_DEBUG);
        String directoryName = dir.getAbsolutePath();
        // Check to see if this directory has already been processed
        if (duplicateFileLookup.contains(directoryName)) {
            // do not process
            log("Directory already processed", Project.MSG_DEBUG);
            log("Exiting ResolveAllTasks.createDuplicateFileLookup", Project.MSG_DEBUG);
            return;
        }
        log ("Traversing this directory: "+directoryName, Project.MSG_DEBUG);
        // Record the directory name to avoid recursive traversals
        duplicateFileLookup.add(directoryName);
        log("Directory name is: "+directoryName, Project.MSG_DEBUG);
        File[] files = dir.listFiles();
        int numFiles = files.length;
        log ("Number of files: "+numFiles, Project.MSG_DEBUG);
        for (int i = 0; i < numFiles; i++) {
            log ("index i: "+i, Project.MSG_DEBUG);
            File file = files[i];
            String fileName = file.getName();
            if (file.isDirectory()) {
                createDuplicateFileLookup(file, duplicateFileLookup);
            } else {                
                log ("Adding this file name: "+fileName, Project.MSG_DEBUG);
                duplicateFileLookup.add(fileName);
            }
        }
        File parent = dir.getParentFile();
        String parentDirPath = parent.getAbsolutePath();
        log ("parent dir path: "+parentDirPath, Project.MSG_DEBUG);
        // TODO: look up the target directory using Maven API
        if (parent != null && ! parentDirPath.endsWith("target")) {
            // Do not traverse parent above maven 'target' directory
            createDuplicateFileLookup(parent, duplicateFileLookup);
        }
        log("duplicateFileLookup size: "+duplicateFileLookup.size(), Project.MSG_DEBUG);
        log("Exiting ResolveAllTasks.createDuplicateFileLookup", Project.MSG_DEBUG);
    }
    
    public static final ThreadLocal<Artifact> CURRENT_ARTIFACT = new ThreadLocal<Artifact>();
}
