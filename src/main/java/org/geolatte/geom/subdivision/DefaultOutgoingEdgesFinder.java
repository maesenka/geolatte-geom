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
 * Copyright (C) 2010 - 2013 and Ownership of code is shared by:
 * Qmino bvba - Romeinsestraat 18 - 3001 Heverlee  (http://www.qmino.com)
 * Geovise bvba - Generaal Eisenhowerlei 9 - 2140 Antwerpen (http://www.geovise.com)
 */

package org.geolatte.geom.subdivision;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 1/3/13
 */
public class DefaultOutgoingEdgesFinder implements OutgoingEdgesFinder {

    final private Dcel dcel;
    final private Set<HalfEdge> visitedHalfEdges = new HashSet<HalfEdge>(20);

    public DefaultOutgoingEdgesFinder(Dcel dcel){
        this.dcel = dcel;
        visitedHalfEdges.clear();
    }

    @Override
    public List<HalfEdge> getOutgoing(Vertex v, List<HalfEdge> resultList) {
        if (resultList == null) {
            resultList = new ArrayList<HalfEdge>();
        } else {
            resultList.clear();
        }
        HalfEdge e = dcel.getVertices().getIncidentEdge(v);

        while (!visitedHalfEdges.contains(e)){
            visitedHalfEdges.add(e);
            resultList.add(e);
            HalfEdge eTwin = dcel.getHalfEdges().getTwin(e);
            e = dcel.getHalfEdges().getNext(eTwin);
        }
        return resultList;
    }

}
