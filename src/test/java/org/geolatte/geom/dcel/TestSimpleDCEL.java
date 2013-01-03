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

package org.geolatte.geom.dcel;

import org.geolatte.geom.Envelope;
import org.geolatte.geom.subdivision.*;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.geolatte.geom.dcel.THalfEdge.halfedge;
import static org.geolatte.geom.dcel.TVertex.vertex;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 12/28/12
 */
public class TestSimpleDCEL {

    //set up a test DCEL
    // -- Vertices first component
    TVertex v0000 =  vertex(1, 0d, 0d);
    TVertex v0500 =  vertex(2, 5d, 0d);
    TVertex v0510 =  vertex(3, 5d, 10d);
    TVertex v0010 =  vertex(4, 0d, 10d);
    TVertex v0208 =  vertex(5, 2d, 8d);
    TVertex v1000 =  vertex(6, 10d, 0d);
    TVertex v1010 =  vertex(7, 10d, 10d);
    TVertex v0810 =  vertex(8, 8d, 10d);
    TVertex v0812 =  vertex(9, 8d, 12d);
    
    //vertices embedded component
    TVertex v0301 =  vertex(10, 3d, 1d);
    TVertex v0401 =  vertex(11, 4d, 1d);
    TVertex v0403 =  vertex(12, 4d, 3d);
    TVertex v0303 =  vertex(13, 3d, 3d);
    
    //vertices disconnected component
    TVertex v2020 =  vertex(14, 20d, 20d);
    TVertex v2520 =  vertex(15, 25d, 20d);
    TVertex v2525 =  vertex(16, 25d, 25d);
    TVertex v2025 =  vertex(17, 20d, 25d);

    TFace f0 = new TFace(0, true); // the unbounded face
    TFace f1 = new TFace(1);
    TFace f2 = new TFace(2);
    TFace f3 = new TFace(3);
    TFace f4 = new TFace(4);

    Map<String, THalfEdge> heMap = new HashMap<String, THalfEdge>();

    Dcel dcel;

    @Before
    public void setUp() {
        heMap.put("e1.1", halfedge("e1.1",  v0000, v0500, f1, f0));
        heMap.put("e1.2", halfedge("e1.2", v0500, v0000, f0, f1));
        heMap.put("e2.1", halfedge("e2.1", v0500, v0510, f1, f2));
        heMap.put("e2.2", halfedge("e.2.2", v0510, v0500, f2, f1));
        heMap.put("e3.1", halfedge("e3.1", v0510, v0010, f1, f0));
        heMap.put("e3.2", halfedge("e3.2", v0010, v0510, f0, f1));
        heMap.put("e4.1", halfedge("e4.1", v0010, v0000, f1, f0));
        heMap.put("e4.2", halfedge("e4.2", v0000, v0010, f0, f1));
        heMap.put("e5.1", halfedge("e5.1", v0010, v0208, f1, f1));
        heMap.put("e5.2", halfedge("e5.2", v0208, v0010, f1, f1));
        heMap.put("e6.1", halfedge("e6.1", v0500, v1000, f3, f0));
        heMap.put("e6.2", halfedge("e6.2", v1000, v0500, f0, f3));
        heMap.put("e7.1", halfedge("e7.1", v1000, v1010, f3, f0));
        heMap.put("e7.2", halfedge("e7.2", v1010, v1000, f0, f3));
        heMap.put("e8.1", halfedge("e8.1", v0500, v1010, f2, f3));
        heMap.put("e8.2", halfedge("e8.2", v1010, v0500, f3, f2));
        heMap.put("e9.1", halfedge("e9.1", v1010, v0810, f2, f0));
        heMap.put("e9.2", halfedge("e9.2", v0810, v1010, f0, f2));
        heMap.put("e10.1", halfedge("e10.1", v0810, v0510, f2, f0));
        heMap.put("e10.2", halfedge("e10.2", v0510, v0810, f0, f2));
        heMap.put("e11.1", halfedge("e11.1", v0810, v0812, f0, f0));
        heMap.put("e11.2", halfedge("e11.2", v0812, v0810, f0, f0));

        //TODO -- add the interior and unconnected face edges.


        EdgeByEdgeDcelBuilder builder = new EdgeByEdgeDcelBuilder(new Envelope(0, 0, 100, 100), f0);
        for (THalfEdge he : heMap.values()) {
            builder.addHalfEdge(he.getOrigin(), he.getDestination(), he.getLeftFace(), he);
        }
        dcel = builder.toDcel();


    }

    @Test
    public void testOutgoingEdges() {
        DefaultOutgoingEdgesFinder finder = new DefaultOutgoingEdgesFinder(dcel);
        List<HalfEdge> outgoing = finder.getOutgoing(v0500, null);
        assertEquals(4, outgoing.size());
        assertTrue(outgoing.contains(heMap.get("e1.2")));
        assertTrue(outgoing.contains(heMap.get("e2.1")));
        assertTrue(outgoing.contains(heMap.get("e8.1")));
        assertTrue(outgoing.contains(heMap.get("e6.1")));

        outgoing = finder.getOutgoing(v0208, outgoing);
        assertEquals(1, outgoing.size());
        assertTrue(outgoing.contains(heMap.get("e5.2")));

        outgoing = finder.getOutgoing(v1010, outgoing);
        assertEquals(3, outgoing.size());
        assertTrue(outgoing.contains(heMap.get("e8.2")));
        assertTrue(outgoing.contains(heMap.get("e7.2")));
        assertTrue(outgoing.contains(heMap.get("e9.1")));
    }

    @Test
    public void testUnboundedFace() {
        assertEquals(f0, dcel.getUnboundedFace());

        boolean found = false;
        for (Face f: dcel.getFaces()){
            if( f.isUnboundedFace()) {
                assertEquals(f0, f);
                found = true;
            }
        }
        assertTrue(found);
    }



    @Test
    public void testFaceNextPreviousLinks() {
        Collection<HalfEdge> components = dcel.getFaces().getInnerComponents(f0);
        assertEquals(1, components.size());


        //assert that the e1.next.prev == e1 for each face
        for (Face f : dcel.getFaces()) {
            if(f.isUnboundedFace()) continue;
            HalfEdge start  = dcel.getFaces().getOuterComponent(f);
            HalfEdge current = start;
            HalfEdge next = dcel.getHalfEdges().getNext(current);
            do {
                assertEquals(current, dcel.getHalfEdges().getPrevious(next));
                current = next;
                next = dcel.getHalfEdges().getNext(current);
            }while (!next.equals(start));
        }


    }





















}
