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
import java.util.Set;

/**
 * A Doubly Connected Edge List (DCEL).
 *
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 12/27/12
 */
public interface Dcel<V extends Vertex, E extends HalfEdge, F extends Face> {

    public Envelope getEnvelope();

    public E getIncidentEdge(V vertex);

    public E getOuterComponent(F face);

    public Collection<E> getInnerComponents(F face);


    /**
     * Get the origin {@code Vertex} for the specified {@code HalfEdge}
     *
     * @param he the {@code HalfEdge}
     * @return the origin {@code Vertex} for the {@code HalfEdge} specified by the parameter <code>he</code>
     */
    public V getOrigin(E he);

    /**
     * Get the twin {@code HalfEdge} for the specified {@code HalfEdge}
     *
     * <p>The twin of a {@code HalfEdge} with origin v1 and destination v2 is the
     * {@code HalfEdge} with origin v2 and destination v1</p>
     *
     * @param he the {@code HalfEdge}
     * @return the origin {@code Vertex} for the {@code HalfEdge} specified by the parameter <code>he</code>
     */
    public E getTwin(E he);

    /**
     * Returns the {@code Face} that is bounded by the specified {@code HalfEdge}
     *
     * <p>The bounded {@code Face} lies to the left of this {@code HalfEdge}.</p>
     *
     * @param he the {@code HalfEdge}
     * @return the {@code Face} bounded by and to the left of the {@code HalfEdge} specified by the parameter <code>he</code>
     */
    public F getIncidentFace(E he);

    /**
     * Returns the next {@code HalfEdge} when traversing the boundaries of the incident {@code Face} of the specified {@code HalfEdge} in
     * counterclockwise fashion.
     *
     * <p>The incident {@code Face} is the {@code Face} returned by the {@code getIncidentFace(he)}.</p>
     *
     *
     * @param he the {@code HalfEdge}
     * @return the next {@code HalfEdge} in a counterclockwise traversal of the boundaries of the face incident to the specified {@code halfEdge}.
     *
     */
    public E getNext(E he);

    /**
     * Returns the previous {@code HalfEdge} when traversing the boundaries of the incident {@code Face} of the specified {@code HalfEdge} in
     * counterclockwise fashion.
     *
     * <p>The incident {@code Face} is the {@code Face} returned by the {@code getIncidentFace(he)}.</p>
     *
     *
     * @param he the {@code HalfEdge}
     * @return the previous {@code HalfEdge} in a counterclockwise traversal of the boundaries of the face incident to the specified {@code halfEdge}.
     *
     */
    public E getPrevious(E he);

    public F getUnboundedFace();

    public Set<F> getFaces();

    public Set<E> getHalfEdges();

    public Set<V> getVertices();
}
