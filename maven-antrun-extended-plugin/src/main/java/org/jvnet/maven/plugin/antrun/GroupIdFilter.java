package org.jvnet.maven.plugin.antrun;

/**
 * @author Kohsuke Kawaguchi
 */
public class GroupIdFilter  extends ListFilter {
    private String groupId;
    private String groupIdNot;

    public void setValue(String v) {
        groupId = v;
    }

    public void setNot(String v) {
        groupIdNot = v;
    }

    public boolean visit(DependencyGraph.Node node) {
        String p = node.getProject().getGroupId();
        if(groupId !=null && groupId.equals(p))
            return true;    // positive match
        if(groupIdNot !=null && !groupIdNot.equals(p))
            return true;    // negative match

        return false;
    }
}
