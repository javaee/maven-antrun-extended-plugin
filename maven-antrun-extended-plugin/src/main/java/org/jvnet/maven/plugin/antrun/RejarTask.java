package org.jvnet.maven.plugin.antrun;

import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Jar;
import org.apache.tools.ant.types.ZipFileSet;
import org.apache.tools.zip.ZipOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * &lt;jar> task extended to correctly merge manifest metadata.
 *
 * TODO: this contains HK2 knowledge, so it should be moved to HK2.
 *
 * @author Kohsuke Kawaguchi
 */
public class RejarTask extends Jar {
//
// these fields only have a life-span within the execute method.
//
    /**
     * Merged metadata files in <tt>META-INF</tt>
     */
    private final Map<String,ByteArrayOutputStream> metadata = new HashMap<String,ByteArrayOutputStream>();

    public void execute() throws BuildException {
        // we want to put metadata files earlier in the file for faster runtime access,
        // and for that we require two passes.
        doubleFilePass = true;

        super.execute();
    }

    protected void initZipOutputStream(ZipOutputStream zOut) throws IOException, BuildException {
        if (!skipWriting) {
            // write out the merged metadata and service entries
            for (Map.Entry<String,ByteArrayOutputStream> e : metadata.entrySet()) {
                super.zipFile(
                    new ByteArrayInputStream(e.getValue().toByteArray()),
                    zOut, e.getKey(),
                    System.currentTimeMillis(), null,
                    ZipFileSet.DEFAULT_FILE_MODE);
            }
        }

        super.initZipOutputStream(zOut);
    }

    protected void zipFile(InputStream is, ZipOutputStream zOut, String vPath, long lastModified, File fromArchive, int mode) throws IOException {
        boolean isInhabitantsFile = vPath.startsWith("META-INF/inhabitants/");

        if (isInhabitantsFile || vPath.startsWith("META-INF/services/"))  {
            // merging happens in the first pass.
            // in the second pass, ignore them.
            if(skipWriting) {
                ByteArrayOutputStream stream = metadata.get(vPath);
                if(stream==null)
                    metadata.put(vPath,stream= new ByteArrayOutputStream());
                if(isInhabitantsFile) {
                    // print where the lines came from
                    stream.write(("# from "+fromArchive.getName()+"\n").getBytes());
                }
                IOUtils.copy(is,stream);
            }
            return;
        }

        // merge inhabitants file
        super.zipFile(is, zOut, vPath, lastModified, fromArchive, mode);
    }
}
