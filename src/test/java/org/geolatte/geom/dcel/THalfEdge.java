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

import org.geolatte.geom.LineString;
import org.geolatte.geom.Point;
import org.geolatte.geom.subdivision.HalfEdge;

import static org.geolatte.geom.builder.DSL.*;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 12/28/12
 */
public class THalfEdge implements HalfEdge {

    final private LineString geometry;
    final private String id;
    final private TFace leftFace;
    final private TFace rightFace;
    final private TVertex origin;
    final private TVertex destination;

    public static THalfEdge halfedge(String id, TVertex o, TVertex d, TFace leftFace, TFace rightFace) {
        return new THalfEdge(id, o, d, leftFace, rightFace);
    }

    public THalfEdge(String id, TVertex o, TVertex d, TFace leftFace, TFace rightFace) {
        Point start = o.getPoint();
        Point end = o.getPoint();
        geometry = linestring(0, c(start.getX(), start.getY()), c(end.getX(), end.getY()));
        this.id = id;
        this.leftFace = leftFace;
        this.rightFace = rightFace;
        this.origin = o;
        this.destination = d;
    }

    @Override
    public LineString getGeometry() {
        return this.geometry;
    }

    public String getId() {
        return id;
    }

    public TFace getLeftFace() {
        return leftFace;
    }

    public TFace getRightFace() {
        return rightFace;
    }

    public TVertex getOrigin() {
        return origin;
    }

    public TVertex getDestination() {
        return destination;
    }

    public String toString() {
        return id;
    }
}
