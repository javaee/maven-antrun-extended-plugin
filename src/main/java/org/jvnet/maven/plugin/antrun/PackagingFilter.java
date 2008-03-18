package org.jvnet.maven.plugin.antrun;


/**
 * Filter out a {@link DependencyGraph} by only traversing the given packaging.
 *
 * @author Paul Sterk
 */
public final class PackagingFilter extends ListFilter {
    private String packaging;
    private String packagingNot;

    public void setValue(String v) {
        packaging = v;
    }

    public void setNot(String v) {
        packagingNot = v;
    }

    public boolean visit(DependencyGraph.Node node) {
        String p = node.getProject().getPackaging();
        if(packaging!=null && packaging.equals(p))
            return true;    // positive match
        if(packagingNot!=null && !packagingNot.equals(p))
            return true;    // negative match

        return false;
    }
}
