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

import java.util.*;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 12/27/12
 */
class SimpleDcel<V extends Vertex, E extends HalfEdge, F extends Face> implements Dcel<V,E,F> {

    final private SimpleVertexList<V, E> vertices;

    final private SimpleHalfEdgeList<V,E,F> halfEdges;

    final private SimpleFaceList<E, F> faces;

    final private Envelope envelope;

    SimpleDcel(Envelope env, SimpleVertexList<V,E> vertices, SimpleHalfEdgeList<V,E,F> edges, SimpleFaceList<E,F> faces){
        this.envelope = env;
        this.vertices = vertices;
        this.halfEdges = edges;
        this.faces = faces;
    }



    @Override
    public F getUnboundedFace() {
        return this.faces.getUnboundedFace();
    }

    @Override
    public Set<F> getFaces() {
        return this.faces.toSet();
    }

    @Override
    public Set<E> getHalfEdges() {
        return this.halfEdges.toSet();
    }

    @Override
    public Set<V> getVertices() {
        return this.vertices.toSet();
    }

    @Override
    public Envelope getEnvelope() {
        return this.envelope;
    }

    @Override
    public E getIncidentEdge(V vertex) {
        return vertices.getIncidentEdge(vertex);
    }

    @Override
    public E getOuterComponent(F face) {
        return faces.getOuterComponent(face);
    }

    @Override
    public Collection<E> getInnerComponents(F face) {
        return faces.getInnerComponents(face);
    }

    @Override
    public V getOrigin(E he) {
        return halfEdges.getOrigin(he);
    }

    @Override
    public E getTwin(E he) {
        return halfEdges.getTwin(he);
    }

    @Override
    public F getIncidentFace(E he) {
        return halfEdges.getIncidentFace(he);
    }

    @Override
    public E getNext(E he) {
        return halfEdges.getNext(he);
    }

    @Override
    public E getPrevious(E he) {
        return halfEdges.getPrevious(he);
    }

    static class SimpleVertexList<V extends Vertex, E extends HalfEdge> {
        final private Map<V, E> vertices;

        SimpleVertexList(Map<V, E> vertices){
            this.vertices = vertices;
        }


        public E getIncidentEdge(V vertex) {
            E result =  vertices.get(vertex);
            if (result == null) {
                throw new IllegalStateException("Vertex " + vertex + " is not an element of the DCEL.");
            }
            return result;
        }

        public Set<V> toSet() {
            return vertices.keySet();
        }
    }

    static class SimpleFaceList<E extends HalfEdge, F extends Face> {

        final private Map<F,E> outerComponentMap;
        final private Map<F, List<E>> innerComponentsMap;
        final private F unboundedFace;

        SimpleFaceList(Map<F,E> outerComps, Map<F, List<E>> innerComps, F unboundedFace){
            this.outerComponentMap = outerComps;
            this.innerComponentsMap = innerComps;
            this.unboundedFace = unboundedFace;
        }


        public E getOuterComponent(F face) {
            E result = outerComponentMap.get(face);
//            if (result == null) {
//                throw new IllegalStateException("Face " + face + " is not an element of the DCEL.");
//            } -- null returned for the unboundedface
            return result;
        }

        public Collection<E> getInnerComponents(F face) {
            List<E> result = innerComponentsMap.get(face);
//            if (result == null) {
//                throw new IllegalStateException("Face " + face + " is not an element of the DCEL.");
//            } -- currently null is stored in the case that there are no innercomponents (the normal case).
            return result;
        }

        F getUnboundedFace() {
            return unboundedFace;
        }

        public Set<F> toSet() {
            return outerComponentMap.keySet();
        }
    }

    static class SimpleHalfEdgeList<V extends Vertex, E extends HalfEdge, F extends Face> {

        final private Map<E, HalfEdgeRecord<V,E,F>> halfEdgeRecordMap;

        SimpleHalfEdgeList(Map<E, HalfEdgeRecord<V,E,F>> halfEdgeRecordMap) {
            this.halfEdgeRecordMap = halfEdgeRecordMap;
        }

        private HalfEdgeRecord<V,E,F> getRecord(E he){
            HalfEdgeRecord<V,E,F> record = halfEdgeRecordMap.get(he);
            if (record == null) {
                throw new IllegalStateException("HalfEdge " + he + " is not an element of the DCEL");
            }
            return record;
        }


        public V getOrigin(E he) {
            return getRecord(he).origin;
        }


        public E getTwin(E he) {
            return getRecord(he).twin;
        }


        public F getIncidentFace(E he) {
            return getRecord(he).incidentFace;
        }


        public E getNext(E he) {
            return getRecord(he).next;
        }


        public E getPrevious(E he) {
            return getRecord(he).prev;
        }


        public Set<E> toSet() {
            return halfEdgeRecordMap.keySet();
        }
    }

    static class HalfEdgeRecord<V extends Vertex, E extends HalfEdge, F extends Face> {
        private V origin;
        private E twin;
        private F incidentFace;
        private E next;
        private E prev;

        public V getOrigin() {
            return origin;
        }

        public void setOrigin(V origin) {
            this.origin = origin;
        }

        public E getTwin() {
            return twin;
        }

        public void setTwin(E twin) {
            this.twin = twin;
        }

        public F getIncidentFace() {
            return incidentFace;
        }

        public void setIncidentFace(F incidentFace) {
            this.incidentFace = incidentFace;
        }

        public E getNext() {
            return next;
        }

        public void setNext(E next) {
            this.next = next;
        }

        public E getPrev() {
            return prev;
        }

        public void setPrev(E prev) {
            this.prev = prev;
        }
    }


}
