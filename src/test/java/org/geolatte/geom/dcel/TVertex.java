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

import org.geolatte.geom.Point;
import org.geolatte.geom.Points;
import org.geolatte.geom.subdivision.Vertex;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 12/28/12
 */
public class TVertex implements Vertex {
    private final Point point;
    private final int id;

    public static TVertex vertex(int id, double x, double y) {
        return new TVertex(id, x, y);
    }

    TVertex(int id, double x, double y) {
        point = Points.create2D(x, y);
        this.id = id;
    }

    @Override
    public Point getPoint() {
        return point;
    }

    public int getId() {
        return id;
    }
}
