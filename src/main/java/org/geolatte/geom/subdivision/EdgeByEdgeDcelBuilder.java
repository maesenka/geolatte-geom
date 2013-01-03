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
 * A {@DcelBuilder} that builds up the DCEL by adding edges.
 * <p/>
 * <p>This builder assumes that the no edge self-intersects, and no two edges intersect.</p>
 *
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 12/27/12
 */
public class EdgeByEdgeDcelBuilder implements DcelBuilder {

    final private Envelope envelope;
    final private Face unbounded;


    //status information on the build state
    final private VertexRecordList vertexRecordList = new VertexRecordList();
    final Map<HalfEdge, SimpleDcel.HalfEdgeRecord> halfEdgeRecordMap = new HashMap<HalfEdge, SimpleDcel.HalfEdgeRecord>();
    final private Map<Face, HalfEdge> outerComponentMap = new HashMap<Face, HalfEdge>();
    private int componentCounter = 0;

    public EdgeByEdgeDcelBuilder(Envelope envelope, Face unbounded) {
        this.envelope = envelope;
        this.unbounded = unbounded;
        outerComponentMap.put(unbounded, null);
    }

    public void addHalfEdge(Vertex origin, Vertex destination, Face leftFace, HalfEdge halfEdge) {
        VertexRecord originRecord = vertexRecordList.getVertexRecord(origin);
        VertexRecord destRecord = vertexRecordList.getVertexRecord(destination);
        updateVertexRecords(originRecord, destRecord, halfEdge);
        setOuterComponent(leftFace, halfEdge);
        updateHalfEdgeRecordMap(origin, destination, leftFace, halfEdge, originRecord, destRecord);
    }


    private void updateHalfEdgeRecordMap(Vertex origin, Vertex destination, Face leftFace, HalfEdge halfEdge, VertexRecord originRecord, VertexRecord destRecord) {
        SimpleDcel.HalfEdgeRecord currentRecord = new SimpleDcel.HalfEdgeRecord();
        currentRecord.setOrigin(origin);
        currentRecord.setIncidentFace(leftFace);
        halfEdgeRecordMap.put(halfEdge, currentRecord);
        //set next/prev at origin
        for(HalfEdge candidate: originRecord.incoming) {
            SimpleDcel.HalfEdgeRecord candidateRecord = halfEdgeRecordMap.get(candidate);
            if (candidateRecord != null && candidateRecord.getIncidentFace().equals(leftFace)) {
                candidateRecord.setNext(halfEdge);
                currentRecord.setPrev(candidate);
            }
        }
        //set next/prev at destination
        for(HalfEdge candidate: destRecord.outgoing) {
            SimpleDcel.HalfEdgeRecord candidateRecord = halfEdgeRecordMap.get(candidate);
            if (candidateRecord != null && candidateRecord.getIncidentFace().equals(leftFace)) {
                currentRecord.setNext(candidate);
                candidateRecord.setPrev(halfEdge);
            }
        }
        //set twin
        for (HalfEdge candidate: destRecord.outgoing) {
            SimpleDcel.HalfEdgeRecord candidateRecord = halfEdgeRecordMap.get(candidate);
            if (candidateRecord.getOrigin().equals(origin)) {
                candidateRecord.setTwin(halfEdge);
                currentRecord.setTwin(candidate);
                break;
            }
        }
        for (HalfEdge candidate: originRecord.incoming) {
            SimpleDcel.HalfEdgeRecord candidateRecord = halfEdgeRecordMap.get(candidate);
            if (candidateRecord.getOrigin().equals(destination)) {
                candidateRecord.setTwin(halfEdge);
                currentRecord.setTwin(candidate);
                break;
            }
        }

    }

    private void setOuterComponent(Face leftFace, HalfEdge halfEdge) {
        if (leftFace.isUnboundedFace()) {
            return;
        }
        outerComponentMap.put(leftFace, halfEdge);
    }

    /**
     * Add vertices to the vertexRecordList and register the HalfEdge as incoming, resp. outgoing.
     */
    private void updateVertexRecords(VertexRecord originRecord, VertexRecord destRecord, HalfEdge halfEdge) {


        if (isNewVertex(originRecord) && isNewVertex(destRecord)) {
            Integer comp = componentCounter++;
            originRecord.component = comp;
            destRecord.component = comp;
        } else if (isNewVertex(originRecord)){
            assert(destRecord.component != null);
            originRecord.component = destRecord.component;
        } else if (isNewVertex(destRecord)) {
            assert (originRecord.component != null);
            destRecord.component = originRecord.component;
        } else { //neither are new
            assert (originRecord.component != null);
            assert(destRecord.component != null);
            vertexRecordList.relabelComponents(originRecord.component, destRecord.component);
        }
        originRecord.outgoing.add(halfEdge);
        destRecord.incoming.add(halfEdge);
    }

    private boolean isNewVertex(VertexRecord vertexRecord) {
        return vertexRecord.outgoing.isEmpty() && vertexRecord.incoming.isEmpty();
    }

    @Override
    public Dcel toDcel() {
        return new SimpleDcel(this.envelope,
                mkSimpleVertexList(), mkSimpleEdgeList(),
                toSimpleFaceList());
    }

    private SimpleDcel.SimpleFaceList toSimpleFaceList() {
        Map<Face, List<HalfEdge>> innerComps = new HashMap<Face, List<HalfEdge>>();
        Set<Integer> visitedComponents = new TreeSet<Integer>();
        for (Map.Entry<Vertex, VertexRecord> entry : vertexRecordList.all()) {
            Integer currentComponent = entry.getValue().component;
            if (visitedComponents.contains(currentComponent)) {
                continue;
            }
            for( HalfEdge outgoing: entry.getValue().outgoing) {
                Face incidentFace = halfEdgeRecordMap.get(outgoing).getIncidentFace();
                HalfEdge boundary = outerComponentMap.get(incidentFace);
                if (boundary == null) {
                    addInnerComponent(innerComps, incidentFace, outgoing);
                    visitedComponents.add(currentComponent);
                    break;
                } else {
                    SimpleDcel.HalfEdgeRecord boundaryRecord = halfEdgeRecordMap.get(boundary);
                    VertexRecord vertexRecord = vertexRecordList.getVertexRecord(boundaryRecord.getOrigin());
                    if (!vertexRecord.component.equals(currentComponent)) {
                        addInnerComponent(innerComps, incidentFace, outgoing);
                        visitedComponents.add(currentComponent);
                        break;
                    }
                }
            }
        }
        return new SimpleDcel.SimpleFaceList(outerComponentMap, innerComps, unbounded);
    }

    private void addInnerComponent(Map<Face, List<HalfEdge>> innerComps, Face outerFace, HalfEdge boundary) {
        List<HalfEdge> innerEdges = innerComps.get(outerFace);
        if (innerEdges == null) {
            innerEdges = new ArrayList<HalfEdge>();
            innerComps.put(outerFace, innerEdges);
        }
        innerEdges.add(boundary);
    }

    private SimpleDcel.SimpleHalfEdgeList mkSimpleEdgeList() {
        return new SimpleDcel.SimpleHalfEdgeList(halfEdgeRecordMap);
    }

    private SimpleDcel.SimpleVertexList mkSimpleVertexList() {

        Map<Vertex, HalfEdge> verticesMap = new HashMap<Vertex, HalfEdge>();
        for (Map.Entry<Vertex, VertexRecord> entry : vertexRecordList.all()) {
            verticesMap.put(entry.getKey(), entry.getValue().outgoing.get(0));
        }
        return new SimpleDcel.SimpleVertexList(verticesMap);
    }

    private static class VertexRecordList {

        final private Map<Vertex, VertexRecord> vertexRecordMap = new HashMap<Vertex, VertexRecord>();

        VertexRecord getVertexRecord(Vertex v) {
            VertexRecord record = vertexRecordMap.get(v);
            if (record == null) {
                record = new VertexRecord();
                vertexRecordMap.put(v, record);
            }
            return record;
        }

        public void relabelComponents(Integer newComponent, Integer oldComponent) {
            for (VertexRecord record : vertexRecordMap.values()) {
                if (oldComponent.equals(record.component)) {
                    record.component = newComponent;
                }
            }
        }

        public Set<Map.Entry<Vertex, VertexRecord>> all() {
            return this.vertexRecordMap.entrySet();
        }


    }

    private static class VertexRecord {
        final List<HalfEdge> outgoing = new ArrayList<HalfEdge>();
        final List<HalfEdge> incoming = new ArrayList<HalfEdge>();
        Integer component;
    }
}

