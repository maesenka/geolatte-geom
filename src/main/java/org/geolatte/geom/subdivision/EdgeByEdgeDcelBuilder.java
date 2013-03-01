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
import org.geolatte.geom.Point;

import java.util.*;

import static org.geolatte.geom.Vector.*;

/**
 * A {@DcelBuilder} that builds up the DCEL by adding edges.
 * <p/>
 * <p>This builder assumes that the no edge self-intersects, and no two edges intersect.</p>
 *
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 12/27/12
 */
public class EdgeByEdgeDcelBuilder<V extends Vertex, E extends HalfEdge, F extends Face> implements DcelBuilder<V,E,F> {

    final private Envelope envelope;
    final private F unbounded;

    final private MinAngleFinder<E> minAngleFinder = new MinAngleFinder<E>();

    //status information on the build state
    final private VertexRecordList<V,E> vertexRecordList = new VertexRecordList<V,E>();
    final Map<E, SimpleDcel.HalfEdgeRecord<V,E,F>> halfEdgeRecordMap = new HashMap<E, SimpleDcel.HalfEdgeRecord<V,E,F>>();
    final private Map<F, E> outerComponentMap = new HashMap<F, E>();
    private int componentCounter = 0;

    public EdgeByEdgeDcelBuilder(Envelope envelope, F unbounded) {
        this.envelope = envelope;
        this.unbounded = unbounded;
        outerComponentMap.put(unbounded, null);
    }

    public void addHalfEdge(V origin, V destination, F leftFace, E halfEdge) {
        VertexRecord<E> originRecord = vertexRecordList.getVertexRecord(origin);
        VertexRecord<E> destRecord = vertexRecordList.getVertexRecord(destination);
        updateVertexRecords(originRecord, destRecord, halfEdge);
        setOuterComponent(leftFace, halfEdge);
        updateHalfEdgeRecordMap(origin, destination, leftFace, halfEdge, originRecord, destRecord);
    }


    private void updateHalfEdgeRecordMap(V origin, V destination, F leftFace, E halfEdge, VertexRecord<E> originRecord, VertexRecord<E> destRecord) {
        SimpleDcel.HalfEdgeRecord<V,E,F> halfEdgeRecord = new SimpleDcel.HalfEdgeRecord<V,E,F>();
        halfEdgeRecord.setOrigin(origin);
        halfEdgeRecord.setIncidentFace(leftFace);
        halfEdgeRecordMap.put(halfEdge, halfEdgeRecord);

        for(E candidate: originRecord.incoming) {
            SimpleDcel.HalfEdgeRecord<V,E,F> candidateRecord = halfEdgeRecordMap.get(candidate);
            if (candidateRecord != null && candidateRecord.getIncidentFace().equals(leftFace)) {
                if (candidateRecord.getNext() == null) {
                    candidateRecord.setNext(halfEdge);
                    halfEdgeRecord.setPrev(candidate);
                    break;
                } else {
                    HalfEdge minAngle = this.minAngleFinder.minAngleFromBase(origin.getPoint(), candidate, candidateRecord.getNext(), halfEdge);
                    if (minAngle.equals(halfEdge)) {
                        halfEdgeRecordMap.get(candidateRecord.getNext()).setPrev(null);
                        candidateRecord.setNext(halfEdge);
                        halfEdgeRecord.setPrev(candidate);
                        break;
                    }
                }
            }
        }
        //set next/prev at destination
        for(E candidate: destRecord.outgoing) {
            SimpleDcel.HalfEdgeRecord<V,E,F> candidateRecord = halfEdgeRecordMap.get(candidate);
            if (candidateRecord != null && candidateRecord.getIncidentFace().equals(leftFace)) {
                if (candidateRecord.getPrev() == null){
                    halfEdgeRecord.setNext(candidate);
                    candidateRecord.setPrev(halfEdge);
                    break;
                } else {
                    HalfEdge minAngle = this.minAngleFinder.minAngleFromBase(destination.getPoint(), candidate, candidateRecord.getPrev(), halfEdge);
                    if (minAngle.equals(halfEdge)){
                        halfEdgeRecordMap.get(candidateRecord.getPrev()).setNext(null);
                        candidateRecord.setPrev(halfEdge);
                        halfEdgeRecord.setNext(candidate);
                        break;
                    }
                }
            }
        }
        //set twin
        for (E candidate: destRecord.outgoing) {
            SimpleDcel.HalfEdgeRecord<V,E,F> candidateRecord = halfEdgeRecordMap.get(candidate);
            if (candidateRecord.getOrigin().equals(origin)) {
                candidateRecord.setTwin(halfEdge);
                halfEdgeRecord.setTwin(candidate);
                break;
            }
        }
        for (E candidate: originRecord.incoming) {
            SimpleDcel.HalfEdgeRecord<V,E,F> candidateRecord = halfEdgeRecordMap.get(candidate);
            if (candidateRecord.getOrigin().equals(destination)) {
                candidateRecord.setTwin(halfEdge);
                halfEdgeRecord.setTwin(candidate);
                break;
            }
        }

    }

    private void setOuterComponent(F leftFace, E halfEdge) {
        if (leftFace.isUnboundedFace()) {
            return;
        }
        outerComponentMap.put(leftFace, halfEdge);
    }

    /**
     * Add vertices to the vertexRecordList and register the HalfEdge as incoming, resp. outgoing.
     */
    private void updateVertexRecords(VertexRecord<E> originRecord, VertexRecord<E> destRecord, E halfEdge) {


        if (isNewVertex(originRecord) && isNewVertex(destRecord)) {
            Integer comp = componentCounter++;
            originRecord.component = comp;
            vertexRecordList.addToComponentRecordMap(originRecord);
            destRecord.component = comp;
            vertexRecordList.addToComponentRecordMap(destRecord);
        } else if (isNewVertex(originRecord)){
            assert(destRecord.component != null);
            originRecord.component = destRecord.component;
            vertexRecordList.addToComponentRecordMap(originRecord);
        } else if (isNewVertex(destRecord)) {
            assert (originRecord.component != null);
            destRecord.component = originRecord.component;
            vertexRecordList.addToComponentRecordMap(destRecord);
        } else { //neither are new
            assert (originRecord.component != null);
            assert(destRecord.component != null);
            vertexRecordList.relabelComponents(destRecord.component, originRecord.component);
        }
        originRecord.outgoing.add(halfEdge);
        destRecord.incoming.add(halfEdge);
    }

    private boolean isNewVertex(VertexRecord vertexRecord) {
        return vertexRecord.outgoing.isEmpty() && vertexRecord.incoming.isEmpty();
    }

    @Override
    public Dcel<V,E,F> toDcel() {
        return new SimpleDcel<V,E,F>(this.envelope,
                mkSimpleVertexList(), mkSimpleEdgeList(),
                toSimpleFaceList());
    }

    private SimpleDcel.SimpleFaceList<E,F> toSimpleFaceList() {
        Map<F, List<E>> innerComps = new HashMap<F, List<E>>();
        Set<Integer> visitedComponents = new TreeSet<Integer>();
        for (Map.Entry<V, VertexRecord<E>> entry : vertexRecordList.all()) {
            Integer currentComponent = entry.getValue().component;
            if (visitedComponents.contains(currentComponent)) {
                continue;
            }
            for( E outgoing: entry.getValue().outgoing) {
                F incidentFace = halfEdgeRecordMap.get(outgoing).getIncidentFace();
                E boundary = outerComponentMap.get(incidentFace);
                if (boundary == null) {
                    addInnerComponent(innerComps, incidentFace, outgoing);
                    visitedComponents.add(currentComponent);
                    break;
                } else {
                    SimpleDcel.HalfEdgeRecord<V,E,F> boundaryRecord = halfEdgeRecordMap.get(boundary);
                    VertexRecord vertexRecord = vertexRecordList.getVertexRecord(boundaryRecord.getOrigin());
                    if (!vertexRecord.component.equals(currentComponent)) {
                        addInnerComponent(innerComps, incidentFace, outgoing);
                        visitedComponents.add(currentComponent);
                        break;
                    }
                }
            }
        }
        return new SimpleDcel.SimpleFaceList<E,F>(outerComponentMap, innerComps, unbounded);
    }

    private void addInnerComponent(Map<F, List<E>> innerComps, F outerFace, E boundary) {
        List<E> innerEdges = innerComps.get(outerFace);
        if (innerEdges == null) {
            innerEdges = new ArrayList<E>();
            innerComps.put(outerFace, innerEdges);
        }
        innerEdges.add(boundary);
    }

    private SimpleDcel.SimpleHalfEdgeList<V,E,F> mkSimpleEdgeList() {
        return new SimpleDcel.SimpleHalfEdgeList<V,E,F>(halfEdgeRecordMap);
    }

    private SimpleDcel.SimpleVertexList<V,E> mkSimpleVertexList() {

        Map<V, E> verticesMap = new HashMap<V, E>();
        for (Map.Entry<V, VertexRecord<E>> entry : vertexRecordList.all()) {
            verticesMap.put(entry.getKey(), entry.getValue().outgoing.get(0));
        }
        return new SimpleDcel.SimpleVertexList<V,E>(verticesMap);
    }

    private static class VertexRecordList<V extends Vertex,E extends HalfEdge> {

        final private Map<V, VertexRecord<E>> vertexRecordMap = new HashMap<V, VertexRecord<E>>();
        final private HashMap<Integer, HashSet<VertexRecord<E>>> componentToRecordMap = new HashMap<Integer, HashSet<VertexRecord<E>>>();

        VertexRecord<E> getVertexRecord(V v) {
            VertexRecord<E> record = vertexRecordMap.get(v);
            if (record == null) {
                record = new VertexRecord<E>();
                vertexRecordMap.put(v, record);
            }
            return record;
        }

        public void removeFromComponentRecordMap(VertexRecord<E> record) {
            HashSet<VertexRecord<E>> vertexRecords = componentToRecordMap.get(record.component);
            if (vertexRecords != null) {
                vertexRecords.remove(record);
            }
        }

        public void addToComponentRecordMap(VertexRecord<E> record) {
            HashSet<VertexRecord<E>> vertexRecords = componentToRecordMap.get(record.component);
            if (vertexRecords == null) {
                vertexRecords = new HashSet<VertexRecord<E>>();
                componentToRecordMap.put(record.component, vertexRecords);
            }
            vertexRecords.add(record);
        }

        public void relabelComponents(Integer oldComponent, Integer newComponent) {
            if (!oldComponent.equals(newComponent)) {
                HashSet<VertexRecord<E>> vertexRecords = componentToRecordMap.get(oldComponent);
                if (vertexRecords != null) {
                    for (VertexRecord<E> record : new ArrayList<VertexRecord<E>>(vertexRecords)) {
                        removeFromComponentRecordMap(record);
                        record.component = newComponent;
                        addToComponentRecordMap(record);
                    }
                }
            }
        }

        public Set<Map.Entry<V, VertexRecord<E>>> all() {
            return this.vertexRecordMap.entrySet();
        }


    }

    private static class VertexRecord<E extends HalfEdge> {
        final List<E> outgoing = new ArrayList<E>();
        final List<E> incoming = new ArrayList<E>();
        Integer component;
    }

    private static class MinAngleFinder<E extends HalfEdge>  {
        /**
         * Creates a vector from an HalfEdge, starting at the specified point
         * @param base
         * @param point
         * @return
         */
        private Point mkVector(E base, Point point) {
            if (getFirstPoint(base).equals(point)) {
                return subtract(getSecondPoint(base), getFirstPoint(base));
            } else {
                return subtract(getBeforeLastPoint(base), getLastPoint(base));
            }
        }

        private Point getLastPoint(E he) {
            return he.getGeometry().getPointN(he.getGeometry().getNumPoints() - 1);
        }

        private Point getBeforeLastPoint(E he) {
            return he.getGeometry().getPointN(he.getGeometry().getNumPoints() - 2);
        }

        private Point getFirstPoint(E he) {
            return he.getGeometry().getPointN(0);
        }

        private Point getSecondPoint(E he) {
            return he.getGeometry().getPointN(1);
        }




        public HalfEdge minAngleFromBase(Point origin, E base, E e1, E e2) {
            Point p0 = mkVector(base, origin);
            Point p1 = mkVector(e1, origin);
            Point p2 = mkVector(e2, origin);
            return angle(p1, p0) < angle(p2, p0)  ? e1 : e2;
        }
    }

}

