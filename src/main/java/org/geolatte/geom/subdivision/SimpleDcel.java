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

import org.geolatte.geom.Envelope;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 12/27/12
 */
class SimpleDcel implements Dcel {

    final private SimpleVertexList vertices;

    final private SimpleHalfEdgeList halfEdges;

    final private SimpleFaceList faces;

    final private Envelope envelope;

    SimpleDcel(Envelope env, SimpleVertexList vertices, SimpleHalfEdgeList edges, SimpleFaceList faces){
        this.envelope = env;
        this.vertices = vertices;
        this.halfEdges = edges;
        this.faces = faces;
    }

    @Override
    public VertexList getVertices() {
        return vertices;
    }

    @Override
    public FaceList getFaces() {
        return faces;
    }

    @Override
    public HalfEdgeList getHalfEdges() {
        return halfEdges;
    }

    @Override
    public Face getUnboundedFace() {
        return this.faces.getUnboundedFace();
    }

    @Override
    public Envelope getEnvelope() {
        return this.envelope;
    }

    static class SimpleVertexList implements VertexList{
        final private Map<Vertex, HalfEdge> vertices;

        SimpleVertexList(Map<Vertex, HalfEdge> vertices){
            this.vertices = vertices;
        }

        @Override
        public HalfEdge getIncidentEdge(Vertex vertex) {
            HalfEdge result =  vertices.get(vertex);
            if (result == null) {
                throw new IllegalStateException("Vertex " + vertex + " is not an element of the DCEL.");
            }
            return result;
        }

        @Override
        public Iterator<Vertex> iterator() {
            return vertices.keySet().iterator();
        }
    }

    static class SimpleFaceList implements FaceList{

        final private Map<Face,HalfEdge> outerComponentMap;
        final private Map<Face, List<HalfEdge>> innerComponentsMap;
        final private Face unboundedFace;

        SimpleFaceList(Map<Face,HalfEdge> outerComps, Map<Face, List<HalfEdge>> innerComps, Face unboundedFace){
            this.outerComponentMap = outerComps;
            this.innerComponentsMap = innerComps;
            this.unboundedFace = unboundedFace;
        }

        @Override
        public HalfEdge getOuterComponent(Face face) {
            HalfEdge result = outerComponentMap.get(face);
//            if (result == null) {
//                throw new IllegalStateException("Face " + face + " is not an element of the DCEL.");
//            } -- null returned for the unboundedface
            return result;
        }

        @Override
        public Collection<HalfEdge> getInnerComponents(Face face) {
            List<HalfEdge> result = innerComponentsMap.get(face);
//            if (result == null) {
//                throw new IllegalStateException("Face " + face + " is not an element of the DCEL.");
//            } -- currently null is stored in the case that there are no innercomponents (the normal case).
            return result;
        }

        Face getUnboundedFace() {
            return unboundedFace;
        }

        @Override
        public Iterator<Face> iterator() {
            return outerComponentMap.keySet().iterator();
        }
    }

    static class SimpleHalfEdgeList implements HalfEdgeList{

        final private Map<HalfEdge, HalfEdgeRecord> halfEdgeRecordMap;

        SimpleHalfEdgeList(Map<HalfEdge, HalfEdgeRecord> halfEdgeRecordMap) {
            this.halfEdgeRecordMap = halfEdgeRecordMap;
        }

        private HalfEdgeRecord getRecord(HalfEdge he){
            HalfEdgeRecord record = halfEdgeRecordMap.get(he);
            if (record == null) {
                throw new IllegalStateException("HalfEdge " + he + " is not an element of the DCEL");
            }
            return record;
        }

        @Override
        public Vertex getOrigin(HalfEdge he) {
            return getRecord(he).origin;
        }

        @Override
        public HalfEdge getTwin(HalfEdge he) {
            return getRecord(he).twin;
        }

        @Override
        public Face getIncidentFace(HalfEdge he) {
            return getRecord(he).incidentFace;
        }

        @Override
        public HalfEdge getNext(HalfEdge he) {
            return getRecord(he).next;
        }

        @Override
        public HalfEdge getPrevious(HalfEdge he) {
            return getRecord(he).prev;
        }

        @Override
        public Iterator<HalfEdge> iterator() {
            return halfEdgeRecordMap.keySet().iterator();
        }
    }

    static class HalfEdgeRecord {
        private Vertex origin;
        private HalfEdge twin;
        private Face incidentFace;
        private HalfEdge next;
        private HalfEdge prev;

        public Vertex getOrigin() {
            return origin;
        }

        public void setOrigin(Vertex origin) {
            this.origin = origin;
        }

        public HalfEdge getTwin() {
            return twin;
        }

        public void setTwin(HalfEdge twin) {
            this.twin = twin;
        }

        public Face getIncidentFace() {
            return incidentFace;
        }

        public void setIncidentFace(Face incidentFace) {
            this.incidentFace = incidentFace;
        }

        public HalfEdge getNext() {
            return next;
        }

        public void setNext(HalfEdge next) {
            this.next = next;
        }

        public HalfEdge getPrev() {
            return prev;
        }

        public void setPrev(HalfEdge prev) {
            this.prev = prev;
        }
    }


}
