package org.jvnet.maven.plugin.antrun;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;

import java.io.File;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * Graph of dependencies among Maven artifacts.
 *
 * <p>
 * This graph, which consists of interconnected {@link Node}s and {@link Edge}s,
 * represents a complete dependency graph rooted at the given Maven module.
 * The constructor recursively parses all POMs for the dependency and builds this information.
 *
 * <p>
 * For example, if you have 4 modules A,B,C, and D that has the dependencies among them as follows:
 * <pre>
 * A->B,C
 * B->D
 * C->D
 * </pre>
 * <p>
 * Then if you construct a graph from 'A', you'll get a graph of four nodes (each representing
 * maven module A,B,C, and D) and four edges (each representing dependencies among them.)
 *
 * <p>
 * Once constructed, a graph is accessible in several ways:
 *
 * <ul>
 * <li>
 * Start with {@link #getRoot() the root node} and traverse through edges like
 * {@link Node#getForward()}.
 *
 * <li>
 * Use {@link #accept(GraphVisitor)} and obtain a sub-graph that matches the given
 * criteria.
 * </ul>
 *
 *
 * @author Kohsuke Kawaguchi
 */
public final class DependencyGraph {
    private final MavenComponentBag bag = MavenComponentBag.get();

    private final Node root;
    /**
     * All {@link Node}s keyed by "groupId:artifactId:classifier"
     */
    private final Map<String,Node> nodes = new HashMap<String, Node>();

    /**
     * Creates a full dependency graph with the given artifact at the top.
     */
    public DependencyGraph(Artifact root) throws ProjectBuildingException, ArtifactResolutionException, ArtifactNotFoundException {
        this.root = toNode(root);
    }

    public DependencyGraph(MavenProject root) throws ProjectBuildingException, ArtifactResolutionException, ArtifactNotFoundException {
        this.root = toNode(root);
    }

    /**
     * Gets the root Node.
     */
    public Node getRoot() {
        return root;
    }

    /**
     * Gets the associated {@link Node}. If none exists, it will be created.
     */
    private Node toNode(Artifact a) throws ProjectBuildingException, ArtifactResolutionException, ArtifactNotFoundException {
        String id = a.getGroupId()+':'+a.getArtifactId()+':'+a.getClassifier();

        Node n = nodes.get(id);
        if(n==null) {
            n = new Node(a);
            nodes.put(id, n);
        }
        return n;
    }

    /**
     * Gets the associated {@link Node}. If none exists, it will be created.
     */
    private Node toNode(MavenProject p) throws ProjectBuildingException, ArtifactResolutionException, ArtifactNotFoundException {
        String id = p.getGroupId()+':'+p.getArtifactId()+":null";

        Node n = nodes.get(id);
        if(n==null) {
            n = new Node(p);
            nodes.put(id, n);
        }
        return n;
    }

    /**
     * Accepts the visitor and invoke its visitor methods.
     *
     * <p>
     * This method is convenient for obtaining a sub-graph of dependencies
     * by filtering out nodes/edges. For example, to obtain all the transitive
     * dependencies that exclude provided/test dependencies, you can do:
     *
     * <pre>
     * accept(new {@link ScopeFilter}("compile","runtime"))
     * </pre>
     *
     * @return
     *      Set of all visited nodes.
     */
    public Set<Node> accept(GraphVisitor visitor) {
        Set<Node> visited = new HashSet<Node>();
        Stack<Node> q = new Stack<Node>();
        q.push(root);

        while(!q.isEmpty()) {
            DependencyGraph.Node n = q.pop();
            if(visitor.visit(n)) {
                for (Edge e : n.forward) {
                    if(visitor.visit(e)) {
                        if(visited.add(e.dst))
                            q.push(e.dst);
                    }
                }
            }
        }

        return visited;
    }

    /**
     * Node, which represents an artifact.
     */
    public final class Node {
        /**
         * Basic properties of a module.
         * If {@link #pom} is non-null, this information is redundant, but it needs to be
         * kept separately for those rare cases where pom==null.
         */
        private final String groupId,artifactId,version;

        private final MavenProject pom;
        private /*final*/ File artifactFile;

        private final List<Edge> forward = new ArrayList<Edge>();
        private final List<Edge> backward = new ArrayList<Edge>();

        private Node(Artifact artifact) throws ProjectBuildingException, ArtifactResolutionException, ArtifactNotFoundException {
            groupId = artifact.getGroupId();
            artifactId = artifact.getArtifactId();
            version = artifact.getVersion();

            if("system".equals(artifact.getScope()))
                // system scoped artifacts don't have POM, so the attempt to load it will fail.
                pom = null;
            else {
                pom = bag.mavenProjectBuilder.buildFromRepository(artifact,
                        bag.project.getRemoteArtifactRepositories(),
                        bag.localRepository);
                loadDependencies();
                checkArtifact(artifact);
            }
        }

        private void checkArtifact(Artifact artifact) throws ArtifactResolutionException, ArtifactNotFoundException {
            bag.resolveArtifact(artifact);
            artifactFile = artifact.getFile();
            if(artifactFile==null)
                throw new IllegalStateException("Artifact is not resolved yet: "+artifact);
        }

        private Node(MavenProject pom) throws ProjectBuildingException, ArtifactResolutionException, ArtifactNotFoundException {
            this.pom = pom;
            groupId = pom.getGroupId();
            artifactId = pom.getArtifactId();
            version = pom.getVersion();
            checkArtifact(pom.getArtifact());
            loadDependencies();
        }

        private void loadDependencies() throws ProjectBuildingException, ArtifactResolutionException, ArtifactNotFoundException {
            for( Dependency d : (List<Dependency>)pom.getDependencies() ) {
                Artifact a = bag.factory.createArtifact(d.getGroupId(), d.getArtifactId(), d.getVersion(), d.getScope(), d.getType());
                new Edge(this,toNode(a),d.getScope(),d.isOptional());
            }
        }

        /**
         * Gets the parsed POM for this artifact.
         *
         * @return null
         *      if POM is not available for this module.
         *      That can happen for example for system-scoped artifacts.
         */
        public MavenProject getProject() {
            return pom;
        }

        /**
         * Gets the artifact file, like a jar.
         *
         * @return
         *      for system-scoped artifacts, this may null.
         *      For all the other modules, this is never null.
         */
        public File getArtifactFile() {
            return artifactFile;
        }

        /**
         * Gets the forward dependency edges (modules that this module depends on.)
         */
        public List<Edge> getForward() {
            return forward;
        }

        /**
         * Gets the nodes that this node depends on.
         */
        public List<Node> getForwardNodes() {
            return new AbstractList<Node>() {
                public Node get(int index) {
                    return forward.get(index).dst;
                }

                public int size() {
                    return forward.size();
                }
            };
        }

        /**
         * Gets the backward dependency edges (modules that depend on this module.)
         */
        public List<Edge> getBackward() {
            return backward;
        }

        /**
         * Gets the nodes that depend on this node.
         */
        public List<Node> getBackwardNodes() {
            return new AbstractList<Node>() {
                public Node get(int index) {
                    return backward.get(index).src;
                }

                public int size() {
                    return backward.size();
                }
            };
        }

        public String toString() {
            return groupId+':'+artifactId+':'+version;
        }
    }

    public final class Edge {
        /**
         * The module that depends on another.
         */
        public final Node src;
        /**
         * The module that is being dependent by another.
         */
        public final Node dst;

        /**
         * Dependency scope. Stuff like "compile", "runtime", etc.
         * Never null.
         */
        public final String scope;

        /**
         * True if this dependency is optional.
         */
        public final boolean optional;

        public Edge(Node src, Node dst, String scope, boolean optional) {
            this.src = src;
            this.dst = dst;
            this.scope = scope;
            this.optional = optional;
            src.forward.add(this);
            dst.backward.add(this);
        }

        public String toString() {
            StringBuilder buf = new StringBuilder();
            buf.append(src).append("--(").append(scope);
            if(optional)
                buf.append("/optional");
            buf.append(")-->").append(dst);
            return buf.toString();            
        }
    }
}
