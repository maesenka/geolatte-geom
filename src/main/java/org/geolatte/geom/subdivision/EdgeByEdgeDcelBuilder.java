/*
 * This file is part of the GeoLatte project.
 *
 *     GeoLatte is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     GeoLatte is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with GeoLatte.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2010 - 2012 and Ownership of code is shared by:
 * Qmino bvba - Romeinsestraat 18 - 3001 Heverlee  (http://www.qmino.com)
 * Geovise bvba - Generaal Eisenhowerlei 9 - 2140 Antwerpen (http://www.geovise.com)
 */

package org.geolatte.geom.subdivision;

import org.geolatte.geom.*;

import java.util.*;

import static org.geolatte.geom.Vector.angle;
import static org.geolatte.geom.Vector.subtract;

/**
 * A {@DcelBuilder} that builds up the DCEL by adding edges.
 * <p/>
 * <p>This builder assumes that the no edge self-intersects, and no two edges intersect.</p>
 *
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 12/27/12
 */
public class EdgeByEdgeDcelBuilder implements DcelBuilder {

    final private GeometryPointEquality pointEquality = new GeometryPointEquality(new ExactCoordinatePointEquality(DimensionalFlag.d2D));
    final private Envelope extent;
    final private Face unboundedFace;
    final Map<Face, List<HalfEdgeWrapper>> faceHalfEdgeMap = new HashMap<Face, List<HalfEdgeWrapper>>();
    final Map<Vertex, VertexWrapper> vertices = new HashMap<Vertex, VertexWrapper>();

    private Map<HalfEdge, SimpleDcel.HalfEdgeRecord> halfEdgeMap;
    private Map<Face, HalfEdge> outerCompMap;
    private Set<Face> currentConnectComponentSet = new HashSet<Face>();
    private Set<Face> containingFaceCandidateSet = new HashSet<Face>();


    public EdgeByEdgeDcelBuilder(Envelope extent, Face unbounded){
        this.extent = extent;
        if (!unbounded.isUnboundedFace()) {
            throw new IllegalArgumentException("Require unbounded face as parameter.");
        }
        this.unboundedFace = unbounded;

   }

    /**
     * Adds an edge between origin and destination {@code Vertex}es.
     * <p/>
     * <p>The edge will be translated into twin {@HalfEdge}s.</p>
     *
     * @param origin      origin or start vertex
     * @param destination destination or end vertex
     * @param leftFace    the face incident and to the left of this edge
     * @throws IllegalArgumentException when any of its arguments is null, or when origin and destination are coincident
     */
    public void addHalfEdge(Vertex origin, Vertex destination, Face leftFace, HalfEdge halfEdge) {
        if (origin == null || destination == null || leftFace == null
                || halfEdge == null) {
            throw new IllegalArgumentException("No null parameters are allowed.");
        }
        if (origin.equals(destination) || pointEquality.equals(origin.getPoint(), destination.getPoint())) {
            throw new IllegalArgumentException("Tried to add edge with coincident origin and destination vertices.");
        }

        VertexWrapper vwOrigin = getVertexWrapper(origin);
        VertexWrapper vwDest = getVertexWrapper(destination);
        HalfEdgeWrapper wrapper = new HalfEdgeWrapper(halfEdge, vwOrigin, vwDest, leftFace);
        addToFaceHalfEdgeMap(leftFace, wrapper);
        vwOrigin.addOutGoing(wrapper);
    }

    private VertexWrapper getVertexWrapper(Vertex destination) {
        VertexWrapper vwDest = vertices.get(destination);
        if (vwDest == null) {
            vwDest = new VertexWrapper(destination);
            vertices.put(destination, vwDest);
        }
        return vwDest;
    }


    /**
     * Adds the halfedge in FaceHalfEdgeMap
     * <p/>
     * <p>This method respects the invariants that the HalfEdges in the list (value) have the face (key) as
     * left incident face.</p>
     *
     * @param face
     * @param halfEdgeWrapper
     */
    private void addToFaceHalfEdgeMap(Face face, HalfEdgeWrapper halfEdgeWrapper) {
        List<HalfEdgeWrapper> list = faceHalfEdgeMap.get(face);
        if (list == null) {
            list = new LinkedList<HalfEdgeWrapper>();
            faceHalfEdgeMap.put(face, list);
        }
        list.add(halfEdgeWrapper);
    }

    @Override
    public Dcel toDcel() {

        halfEdgeMap = createHalfEdgeMap();
        Map<Vertex, HalfEdge> vertexHalfEdgeMap = toSimpleVertexList();
        outerCompMap = toOuterCompMap();
        Map<Face, List<HalfEdge>> innerCompMap = toInnerCompMap();

        return new SimpleDcel(extent,
                new SimpleDcel.SimpleVertexList(vertexHalfEdgeMap),
                new SimpleDcel.SimpleHalfEdgeList(halfEdgeMap),
                new SimpleDcel.SimpleFaceList(outerCompMap, innerCompMap, unboundedFace)
        );
    }

    // methods that calculate the data structures neccessary for constructing a SimpleDcel instance.

    /**
     * Determines the innercomponents by performing a depth-first traversal of the graph
     * and in each step adding the left incident face to the currentConnectComponentSet and removing it from
     * the containingFaceCandidateSet (if present), and adding the right incident face to the containingFaceCandidateSet.
     *
     * @return
     */
    private Map<Face, List<HalfEdge>> toInnerCompMap() {
        Map<Face, List<HalfEdge>> result = new HashMap<Face, List<HalfEdge>>();
        for (VertexWrapper vw : vertices.values()) {
            if (vw.visited) {
                continue;
            }
            depthFirstVisit(vw);
            //if we reach this point, a single connnected component has been determined.
            assert (containingFaceCandidateSet.size() == 1);
            Face containingFace = containingFaceCandidateSet.iterator().next();
            List<HalfEdge> halfEdgeList = result.get(containingFace);
            if (halfEdgeList == null) {
                halfEdgeList = new ArrayList<HalfEdge>();
                result.put(containingFace, halfEdgeList);
            }
            Face inner = currentConnectComponentSet.iterator().next();
            halfEdgeList.add(outerCompMap.get(inner));
            currentConnectComponentSet.clear();
            containingFaceCandidateSet.clear();
        }
        return result;

    }

    private void depthFirstVisit(VertexWrapper vw) {
        vw.visited = true;
        for (HalfEdgeWrapper hew : vw.getOutgoingEdges()) {
            if (!hew.visited) {
                Face leftFace = hew.incidentFace;
                HalfEdge twin = halfEdgeMap.get(hew.halfEdge).twin;
                Face rightFace = halfEdgeMap.get(twin).incidentFace;
                currentConnectComponentSet.add(leftFace);
                containingFaceCandidateSet.remove(leftFace);
                if (!leftFace.equals(rightFace)) {
                    containingFaceCandidateSet.add(rightFace);
                }
                hew.visited = true;
            }
            if (!hew.destination.visited) {
                depthFirstVisit(hew.destination);
            }
        }
    }

    private Map<Face, HalfEdge> toOuterCompMap() {
        Map<Face, HalfEdge> result = new HashMap<Face, HalfEdge>(faceHalfEdgeMap.size());
        for ( Map.Entry<Face, List<HalfEdgeWrapper>> entry : faceHalfEdgeMap.entrySet()) {
            result.put(entry.getKey(), entry.getValue().get(0).halfEdge);
        }
        return result;
    }

    private Map<Vertex, HalfEdge> toSimpleVertexList() {
        Map<Vertex,HalfEdge> result = new HashMap<Vertex,HalfEdge>(vertices.size());
        for (VertexWrapper vw: vertices.values()) {
            result.put(vw.vertex, vw.getOutgoingEdges().get(0).halfEdge);
        }
        return result;
    }

    /**
     * Creates a map of HE's and their HE records.
     * @return
     */
    private Map<HalfEdge, SimpleDcel.HalfEdgeRecord> createHalfEdgeMap() {
        Map<HalfEdge, SimpleDcel.HalfEdgeRecord> halfEdgeMap = new HashMap<HalfEdge, SimpleDcel.HalfEdgeRecord>();
        for (Face face : faceHalfEdgeMap.keySet()) {
            List<HalfEdgeWrapper> faceHalfEdges = faceHalfEdgeMap.get(face);
            for (HalfEdgeWrapper hew : faceHalfEdges) {
                HalfEdge twin = findTwin(hew);
                SimpleDcel.HalfEdgeRecord record = new SimpleDcel.HalfEdgeRecord(hew.origin.vertex, twin, hew.incidentFace);
                record.next = findNext(hew, faceHalfEdges);
                halfEdgeMap.put(hew.halfEdge, record);

            }
            setPrevPointers(halfEdgeMap, faceHalfEdges.get(0).halfEdge);
        }
        return halfEdgeMap;
    }

    private void setPrevPointers(Map<HalfEdge, SimpleDcel.HalfEdgeRecord> halfEdgeMap, HalfEdge startHE) {
        HalfEdge current = startHE;
        SimpleDcel.HalfEdgeRecord currentRecord = halfEdgeMap.get(current);
        SimpleDcel.HalfEdgeRecord nextRecord = halfEdgeMap.get(currentRecord.next);
        while (nextRecord.prev == null) {
            nextRecord.prev = current;
            current = currentRecord.next;
            currentRecord = nextRecord;
            nextRecord = halfEdgeMap.get(currentRecord.next);
        }
    }

    //finds the next halfEdge for the
    private HalfEdge findNext(HalfEdgeWrapper hew, List<HalfEdgeWrapper> faceHalfEdges) {
        List<HalfEdgeWrapper> candidates = new ArrayList<HalfEdgeWrapper>(3);
        for (HalfEdgeWrapper candidate : faceHalfEdges) {
            if (hew.destination.equals(candidate.origin)) {
                candidates.add(candidate);
            }
        }
        Collections.sort(candidates, new EdgeComparator(hew));
        return candidates.get(0).halfEdge;
    }

    //finds the twin of this HalfEdge
    private HalfEdge findTwin(HalfEdgeWrapper wrapper) {
        for (HalfEdgeWrapper candidate : vertices.get(wrapper.destination.vertex).outgoingEdges){
            if (candidate.destination.equals(wrapper.origin)) return candidate.halfEdge;
        }
        throw new IllegalStateException("Couldn't find a twin for half-edge " + wrapper.halfEdge);
    }

    /**
     * A Comparator for edges based on their angle w.r.t. a base vector
     */
    private static class EdgeComparator implements Comparator<HalfEdgeWrapper> {
         private Point p0;
         EdgeComparator(HalfEdgeWrapper  base) {
             p0 = subtract(base.origin.getPoint(), base.destination.getPoint());
         }

        @Override
        public int compare(HalfEdgeWrapper o1, HalfEdgeWrapper o2) {
            Point p1 = subtract(o1.destination.getPoint(), o1.origin.getPoint());
            Point p2 = subtract(o2.destination.getPoint(), o2.origin.getPoint());
            return Double.compare(angle(p1, p0), angle(p2, p0));
        }
    }

    /**
     * Wraps the vertex and maintains state information during the construction of the DCEL
     */
    private static class VertexWrapper {
        final List<HalfEdgeWrapper> outgoingEdges = new ArrayList<HalfEdgeWrapper>(4);
        final Vertex vertex;
        boolean visited = false;

        VertexWrapper(Vertex vertex) {
            this.vertex = vertex;
        }

        void addOutGoing(HalfEdgeWrapper he) {
            outgoingEdges.add(he);
        }

        List<HalfEdgeWrapper> getOutgoingEdges() {
            return this.outgoingEdges;
        }

        Point getPoint() {
            return this.vertex.getPoint();
        }

    }

    /**
     * Wraps the HalfEdge and maintains state information during the construction of the DCEL
     */
    private static class HalfEdgeWrapper {
        final HalfEdge halfEdge;
        final VertexWrapper origin;
        final VertexWrapper destination;
        final Face incidentFace;
        boolean visited = false;

        HalfEdgeWrapper(HalfEdge halfEdge, VertexWrapper origin, VertexWrapper destination, Face incidentFace) {
            this.halfEdge = halfEdge;
            this.origin = origin;
            this.destination = destination;
            this.incidentFace = incidentFace;
        }


    }

}

