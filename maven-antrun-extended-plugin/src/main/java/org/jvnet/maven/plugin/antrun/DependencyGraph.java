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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.commons.collections.ListUtils;

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
import java.util.Queue;
import java.util.LinkedList;

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
     * If true, ignore the {@link Node} that have failed to load.
     */
    private final boolean tolerateBrokenPOMs;

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
    public DependencyGraph(Artifact root, boolean tolerateBrokenPOMs) throws ProjectBuildingException, ArtifactResolutionException, ArtifactNotFoundException {
        this.tolerateBrokenPOMs = tolerateBrokenPOMs;
        Queue<Node> q = new LinkedList<Node>();
        this.root = buildNode(root,q);
        visitBFS(q);
    }

    /**
     * Creates a full dependency graph with the given project at the top.
     */
    public DependencyGraph(MavenProject root, boolean tolerateBrokenPOMs) throws ProjectBuildingException, ArtifactResolutionException, ArtifactNotFoundException {
        this.tolerateBrokenPOMs = tolerateBrokenPOMs;
        Queue<Node> q = new LinkedList<Node>();
        this.root = buildNode(root,q);
        visitBFS(q);
    }

    /**
     * Completes the graph in a breadth-first fashion.
     */
    private void visitBFS(Queue<Node> q) throws ArtifactResolutionException, ArtifactNotFoundException, ProjectBuildingException {
        while(!q.isEmpty())
            q.poll().expand(this,q);
    }

    /**
     * Used to create a subgraph.
     * <p>
     * This method assumes that all nodes and edges are connected,
     * hence the 'private' access. Use {@link #createSubGraph(GraphVisitor)}
     * to construct a subset reliably.
     */
    private DependencyGraph(Node root, Collection<Node> nodes, Collection<Edge> edges, boolean tolerateBrokenPOMs) {
        this.tolerateBrokenPOMs = tolerateBrokenPOMs;
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
     * Gets the associated {@link Node}, or null if none exists.
     */
    public Node toNode(Artifact a) throws ProjectBuildingException, ArtifactResolutionException, ArtifactNotFoundException {
        String id = a.getGroupId()+':'+a.getArtifactId()+':'+a.getClassifier();
        return nodes.get(id);
    }

    /**
     * Gets the associated {@link Node}. If none exists, it will be created.
     *
     * <p>
     * Graph building has to be done breadth-first fashion so that the version
     * conflit resolution happens correctly &mdash; that is, if there exists dependency like
     * A -> B(1.0) -> C(2.0) and A -> C(2.2), then we want to pick up C(2.2), not C(2.0).
     * The criteria to do this in Maven is the length of the dependency chain, and that
     * can be done naturally by BFS. So we pass around a {@link Queue} to keep track of the remaining
     * {@link Node}s to be expanded.
     */
    private Node buildNode(Artifact a, Queue<Node> q) throws ProjectBuildingException, ArtifactResolutionException, ArtifactNotFoundException {
        String id = a.getGroupId()+':'+a.getArtifactId()+':'+a.getClassifier();

        Node n = nodes.get(id);
        if(n==null) {
            n = new Node(a,this,q);
            nodes.put(id, n);
        }
        return n;
    }

    private Node buildNode(MavenProject p, Queue<Node> q) throws ProjectBuildingException, ArtifactResolutionException, ArtifactNotFoundException {
        String id = p.getGroupId()+':'+p.getArtifactId()+":null";

        Node n = nodes.get(id);
        if(n==null) {
            n = new Node(p, q);
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
     * Accepts the visitor. Simply an alias for {@link #createSubGraph(GraphVisitor)}.
     */
    public void accept(GraphVisitor visitor) {
        createSubGraph(visitor);
    }

    /**
     * Creates a full subgraph rooted at the given node.
     */
    public DependencyGraph createSubGraph(Node root) {
        return createSubGraph(root,new DefaultGraphVisitor());
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

        return new DependencyGraph(node,nodes,edges, tolerateBrokenPOMs);
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
        return new DependencyGraph(root,nodes,edges, tolerateBrokenPOMs);
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

    private interface Resolver {
        File resolve() throws AbstractArtifactResolutionException;
    }

    private static final Resolver NULL = new Resolver() {
        public File resolve() {
            return null;
        }
    };

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
        public final String groupId,artifactId,version,type,classifier;

        private final MavenProject pom;
        private /*final*/ File artifactFile;
        private Resolver artifactResolver = NULL;

        /**
         * Represents the artifact that we want to fetch.
         */
        private final Artifact artifact;

        private Node(Artifact artifact, DependencyGraph g, Queue<Node> q) throws ProjectBuildingException, ArtifactResolutionException, ArtifactNotFoundException {
            groupId = artifact.getGroupId();
            artifactId = artifact.getArtifactId();
            version = artifact.getVersion();
            type = artifact.getType();
            classifier = artifact.getClassifier();
            this.artifact = artifact;

            if("system".equals(artifact.getScope())) {
                // system scoped artifacts don't have POM, so the attempt to load it will fail.
                pom = null;
            } else {
                pom = g.bag.mavenProjectBuilder.buildFromRepository(
                        // this create another Artifact instance whose type is 'pom'
                        g.bag.factory.createProjectArtifact(artifact.getGroupId(),artifact.getArtifactId(), artifact.getVersion()),
                        g.bag.project.getRemoteArtifactRepositories(),
                        g.bag.localRepository);
                q.add(this); // visit dependencies from this POM later
            }
        }

        private void checkArtifact(final Artifact artifact, final MavenComponentBag bag) {
            artifactResolver = new Resolver() {
                public File resolve() throws AbstractArtifactResolutionException {
                    if(bag.project.getArtifact()==artifact) {
                        // our own module. Trying to resolve this in the usual way is most likely to fail,
                        // so use what we have, if any.
                        artifactFile =artifact.getFile();
                        return artifactFile;
                    }
                    if(pom!=null) {
                        if (pom.getRemoteArtifactRepositories()==null) 
                            bag.resolveArtifact(artifact);
                        else {
                            //use repositories from pom and also MavenComponentBag
                            bag.resolveArtifact(artifact, 
                                                ListUtils.sum(pom.getRemoteArtifactRepositories(),
	                                                      bag.remoteRepositories));
                        }
		    }
                    else
                        bag.resolveArtifact(artifact);
                    artifactFile = artifact.getFile();
                    if(artifactFile==null)
                        throw new IllegalStateException("Artifact is not resolved yet: "+artifact);
                    return artifactFile;
                }
            };
        }

        private Node(MavenProject pom, Queue<Node> q) {
            this.pom = pom;
            groupId = pom.getGroupId();
            artifactId = pom.getArtifactId();
            version = pom.getVersion();
            type = pom.getPackaging(); // are these the same thing?
            classifier = null;
            artifact = pom.getArtifact();
            q.add(this); // visit dependencies from this POM later
        }

        private void expand(DependencyGraph g, Queue<Node> q) throws ArtifactResolutionException, ArtifactNotFoundException, ProjectBuildingException {
            checkArtifact(artifact,g.bag);
            loadDependencies(g,q);
        }

        private void loadDependencies(DependencyGraph g, Queue<Node> q) throws ProjectBuildingException, ArtifactResolutionException, ArtifactNotFoundException {
            for( Dependency d : (List<Dependency>)pom.getDependencies() ) {
                // the last boolean parameter is redundant, but the version that doesn't take this
                // has a bug. See MNG-2524
                Artifact a = g.bag.factory.createDependencyArtifact(
                        d.getGroupId(), d.getArtifactId(), VersionRange.createFromVersion(d.getVersion()),
                        d.getType(), d.getClassifier(), d.getScope(), false);

                // beware of Maven bug! make sure artifact got the value inherited from dependency
                assert a.getScope().equals(d.getScope());

                try {
                    new Edge(g,this,g.buildNode(a,q),d.getScope(),d.isOptional());
                } catch (ProjectBuildingException e) {
                    handleNodeResolutionException(g,e);
                } catch (ArtifactResolutionException e) {
                    handleNodeResolutionException(g,e);
                } catch (ArtifactNotFoundException e) {
                    handleNodeResolutionException(g,e);
                }
            }
        }

        private void handleNodeResolutionException(DependencyGraph g, Exception e) throws ProjectBuildingException {
            if (g.tolerateBrokenPOMs)
                System.err.println("Failed to parse dependencies of " + getId() + ". trail=" + getTrail(g));
            else
                throw new ProjectBuildingException(getId(), "Failed to parse dependencies of " + getId() + ". trail=" + getTrail(g), e);
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
         *      for system-scoped artifacts, this may null. If this node represents the current module
         *      being built, this field may or may not be null, depending on whether the artifact is
         *      already created in the current build or not.
         *      For all the other modules, this is never null.
         * @throws AbstractArtifactResolutionException
         *      Failed to resolve artifacat.
         */
        public File getArtifactFile() throws AbstractArtifactResolutionException {
            if(artifactFile==null)
                artifactFile = artifactResolver.resolve();
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

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Node node = (Node) o;

            if (!artifactId.equals(node.artifactId)) return false;
            if (classifier != null ? !classifier.equals(node.classifier) : node.classifier != null) return false;
            if (!groupId.equals(node.groupId)) return false;
            if (version != null ? !version.equals(node.version) : node.version != null) return false;

            return true;
        }

        public int hashCode() {
            int result;
            result = groupId.hashCode();
            result = 31 * result + artifactId.hashCode();
            result = 31 * result + (version != null ? version.hashCode() : 0);
            result = 31 * result + (classifier != null ? classifier.hashCode() : 0);
            return result;
        }

        /**
         * Builds the dependency trail from this node to the root node, in that order.
         *
         * This is useful as diagnostic information.
         */
        public List<Edge> getTrail(DependencyGraph graph) {
            List<Edge> trail = new ArrayList<Edge>();
            Node n = this;
            while(n!=graph.getRoot()) {
                List<Edge> list = n.getBackwardEdges(graph);
                if(list.isEmpty())
                    throw new AssertionError("Lost trail at "+trail+" from "+this+" with "+graph);
                Edge e = list.get(0);
                trail.add(e);
                n = e.src;
            }
            Collections.reverse(trail);
            return trail;
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
            if(scope==null) scope="compile";
            this.scope = scope;
            this.optional = optional;
            addEdge(g.forwardEdges,src);
            addEdge(g.backwardEdges,dst);
        }

        private void addEdge(Map<Node, List<Edge>> edgeSet, Node index) {
            List<Edge> l = edgeSet.get(index);
            if(l==null)
                edgeSet.put(index,l=new ArrayList<Edge>());
            for (Edge e : l) {
                if(e.src.equals(this.src) && e.dst.equals(this.dst))
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

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Edge that = (Edge) o;

            return dst.equals(that.dst) && src.equals(that.src);
        }

        public int hashCode() {
            int result;
            result = src.hashCode();
            result = 31 * result + dst.hashCode();
            return result;
        }
    }
}
