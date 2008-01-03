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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;

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
 * {@link Node#getForwardEdges(DependencyGraph)}.
 *
 * <li>
 * Use {@link #createSubGraph(GraphVisitor)} and obtain a sub-graph that matches the given
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
    private final Map<String,Node> nodes = new TreeMap<String,Node>();

    /**
     * Forward edges.
     *
     * Edges are kept on {@link DependencyGraph} so that we can
     * create multiple {@link DependencyGraph}s that share the same node set.
     */
    private final Map<Node,List<Edge>> forwardEdges = new HashMap<Node, List<Edge>>();
    private final Map<Node,List<Edge>> backwardEdges = new HashMap<Node, List<Edge>>();

    /**
     * Creates a full dependency graph with the given artifact at the top.
     */
    public DependencyGraph(Artifact root) throws ProjectBuildingException, ArtifactResolutionException, ArtifactNotFoundException {
        this.root = toNode(root);
    }

    /**
     * Creates a full dependency graph with the given project at the top.
     */
    public DependencyGraph(MavenProject root) throws ProjectBuildingException, ArtifactResolutionException, ArtifactNotFoundException {
        this.root = toNode(root);
    }

    /**
     * Used to create a subgraph.
     * <p>
     * This method assumes that all nodes and edges are connected,
     * hence the 'private' access. Use {@link #createSubGraph(GraphVisitor)}
     * to construct a subset reliably.
     */
    private DependencyGraph(Node root, Collection<Node> nodes, Collection<Edge> edges) {
        if(nodes.isEmpty())     root = null; // graph is empty

        this.root = root;
        if(root!=null) {
            Set<Node> reachable = new HashSet<Node>();
            reachable.add(root);    // root is always reachable

            if(!nodes.contains(root))
                throw new IllegalArgumentException("root "+root+" is not a part of nodes:"+nodes);
            for (Node n : nodes)
                this.nodes.put(n.getId(),n);
            for (Edge e : edges) {
                if(contains(e.src) && contains(e.dst)) {
                    e.addEdge(forwardEdges,e.src);
                    e.addEdge(backwardEdges,e.dst);
                    reachable.add(e.dst);
                }
            }

            // some nodes were unreachable
            if(reachable.size()!=this.nodes.size())
                throw new IllegalArgumentException();
        }
    }

    /**
     * Gets the root node.
     *
     * <p>
     * This is non-null unless this graph is {@link #isEmpty() empty}.
     */
    public Node getRoot() {
        return root;
    }

    /**
     * Returns true if the graph contains nothing at all.
     */
    public boolean isEmpty() {
        return root==null;
    }

    /**
     * Returns all nodes in this graph.
     */
    public Collection<Node> getAllNodes() {
        return nodes.values();
    }

    /**
     * Checks if the graph contains the given node.
     */
    public boolean contains(Node node) {
        return nodes.containsKey(node.getId());
    }

    /**
     * Gets the associated {@link Node}. If none exists, it will be created.
     */
    public Node toNode(Artifact a) throws ProjectBuildingException, ArtifactResolutionException, ArtifactNotFoundException {
        String id = a.getGroupId()+':'+a.getArtifactId()+':'+a.getClassifier();

        Node n = nodes.get(id);
        if(n==null) {
            n = new Node(a,this);
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
            n = new Node(p,this);
            nodes.put(id, n);
        }
        return n;
    }

    /**
     * Accepts the visitor and invoke its visitor methods to create a sub-graph.
     *
     * <p>
     * This method is convenient for obtaining a sub-graph of dependencies
     * by filtering out nodes/edges. For example, to obtain all the transitive
     * dependencies that exclude provided/test dependencies, you can do:
     *
     * <pre>
     * createSubgraph(new {@link ScopeFilter}("compile","runtime"))
     * </pre>
     *
     * @return
     *      A sub-graph of this graph that consists of nodes and edges for which the visitor returns true.
     *      Can be an empty graph, but never null.
     */
    public DependencyGraph createSubGraph(GraphVisitor visitor) {
        return createSubGraph(root,visitor);
    }

    /**
     * Visits the graph started at the given node, and creates a sub-graph
     * from visited nodes and edges.
     *
     * <p>
     * This is the slightly generalized version of {@link #createSubGraph(GraphVisitor)}
     */
    public DependencyGraph createSubGraph(Node node, GraphVisitor visitor) {
        Set<Node> visited = new HashSet<Node>();
        Set<Node> nodes = new HashSet<Node>();
        List<Edge> edges = new ArrayList<Edge>();
        Stack<Node> q = new Stack<Node>();
        q.push(node);

        while(!q.isEmpty()) {
            DependencyGraph.Node n = q.pop();
            if(visitor.visit(n)) {
                nodes.add(n);
                for (Edge e : n.getForwardEdges(this)) {
                    if(visitor.visit(e)) {
                        edges.add(e);
                        if(visited.add(e.dst))
                            q.push(e.dst);
                    }
                }
            }
        }

        return new DependencyGraph(node,nodes,edges);
    }

    /**
     * Creates a sub-graph from the given set of nodes (which must be subset of
     * nodes in the current graph) with all edges { (u,v) | u \in nodes, v \in nodes }  
     */
    public DependencyGraph createSubGraph(Node root, Collection<Node> nodes) {
        List<Edge> edges = new ArrayList<Edge>();
        for (List<Edge> el : forwardEdges.values()) {
            for (Edge e : el) {
                if(nodes.contains(e.src) && nodes.contains(e.dst))
                    edges.add(e);
            }
        }
        return new DependencyGraph(root,nodes,edges);
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("DependencyGraph[root=").append(root).append(",\n");
        buf.append("  nodes=[\n");
        for (Node node : nodes.values())
            buf.append("    ").append(node).append('\n');
        buf.append("  ]\n");
        buf.append("  edges=[\n");
        for (Map.Entry<Node, List<Edge>> n : forwardEdges.entrySet()) {
            for (Edge e : n.getValue()) {
                buf.append("    ").append(e).append('\n');
            }
        }
        buf.append("  ]\n]");
        return buf.toString();
    }

    /**
     * Node, which represents an artifact.
     *
     * <p>
     * A single {@link Node} can be used in multiple {@link DependencyGraph} objects,
     * so the graph traversal method all takes {@link DependencyGraph} object
     * to determine the context in which the operation works.
     */
    public static final class Node {
        /**
         * Basic properties of a module.
         * If {@link #pom} is non-null, this information is redundant, but it needs to be
         * kept separately for those rare cases where pom==null.
         */
        private final String groupId,artifactId,version,classifier;

        private final MavenProject pom;
        private /*final*/ File artifactFile;

        private Node(Artifact artifact, DependencyGraph g) throws ProjectBuildingException, ArtifactResolutionException, ArtifactNotFoundException {
            groupId = artifact.getGroupId();
            artifactId = artifact.getArtifactId();
            version = artifact.getVersion();
            classifier = artifact.getClassifier();

            if("system".equals(artifact.getScope()))
                // system scoped artifacts don't have POM, so the attempt to load it will fail.
                pom = null;
            else {
                pom = g.bag.mavenProjectBuilder.buildFromRepository(
                        // this create another Artifact instance whose type is 'pom'
                        g.bag.factory.createProjectArtifact(artifact.getGroupId(),artifact.getArtifactId(), artifact.getVersion()),
                        g.bag.project.getRemoteArtifactRepositories(),
                        g.bag.localRepository);
                loadDependencies(g);
                checkArtifact(artifact,g.bag);
            }
        }

        private void checkArtifact(Artifact artifact, MavenComponentBag bag) throws ArtifactResolutionException, ArtifactNotFoundException {
            bag.resolveArtifact(artifact);
            artifactFile = artifact.getFile();
            if(artifactFile==null)
                throw new IllegalStateException("Artifact is not resolved yet: "+artifact);
        }

        private Node(MavenProject pom, DependencyGraph g) throws ProjectBuildingException, ArtifactResolutionException, ArtifactNotFoundException {
            this.pom = pom;
            groupId = pom.getGroupId();
            artifactId = pom.getArtifactId();
            version = pom.getVersion();
            classifier = null;
            checkArtifact(pom.getArtifact(),g.bag);
            loadDependencies(g);
        }

        private void loadDependencies(DependencyGraph g) throws ProjectBuildingException, ArtifactResolutionException, ArtifactNotFoundException {
            for( Dependency d : (List<Dependency>)pom.getDependencies() ) {
                Artifact a = g.bag.factory.createArtifact(d.getGroupId(), d.getArtifactId(), d.getVersion(), d.getScope(), d.getType());
                new Edge(g,this,g.toNode(a),d.getScope(),d.isOptional());
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
        public List<Edge> getForwardEdges(DependencyGraph g) {
            return getEdges(g.forwardEdges);
        }

        /**
         * Gets the backward dependency edges (modules that depend on this module.)
         */
        public List<Edge> getBackwardEdges(DependencyGraph g) {
            return getEdges(g.backwardEdges);
        }

        private List<Edge> getEdges(Map<Node, List<Edge>> allEdges) {
            List<Edge> edges = allEdges.get(this);
            if(edges==null) return Collections.emptyList();
            return edges;
        }

        /**
         * Gets the nodes that the given node depends on.
         */
        public List<Node> getForwardNodes(final DependencyGraph g) {
            return new AbstractList<Node>() {
                final List<Edge> forward = getForwardEdges(g);
                public Node get(int index) {
                    return forward.get(index).dst;
                }

                public int size() {
                    return forward.size();
                }
            };
        }

        /**
         * Gets the nodes that depend on the given node.
         */
        public List<Node> getBackwardNodes(final DependencyGraph g) {
            return new AbstractList<Node>() {
                final List<Edge> backward = getBackwardEdges(g);
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

        public String getId() {
            return groupId+':'+artifactId+':'+classifier;
        }
    }

    public static final class Edge {
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

        public Edge(DependencyGraph g, Node src, Node dst, String scope, boolean optional) {
            this.src = src;
            this.dst = dst;
            this.scope = scope;
            this.optional = optional;
            addEdge(g.forwardEdges,src);
            addEdge(g.forwardEdges,dst);
        }

        private void addEdge(Map<Node, List<Edge>> edgeSet, Node index) {
            List<Edge> l = edgeSet.get(index);
            if(l==null)
                edgeSet.put(index,l=new ArrayList<Edge>());
            for (Edge e : l) {
                if(e.src==this.src && e.dst==this.dst)
                    return; // duplicate
            }
            l.add(this);
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
