package org.geolatte.geom.codec;

import org.geolatte.geom.*;
import org.geolatte.geom.codec.support.*;
import org.geolatte.geom.crs.*;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.geolatte.geom.codec.SimpleTokenizer.*;

class BaseWktGeometryParser<P extends Position> {

    private final static CoordinateReferenceSystem<C2D> DEFAULT_CRS = CoordinateReferenceSystems.PROJECTED_2D_METER;
    private final static Pattern EMPTY_PATTERN = Pattern.compile("empty", Pattern.CASE_INSENSITIVE);

    private final BaseWktDialect dialect;
    private final CoordinateReferenceSystem<P> overrideCrs;

    private CoordinateReferenceSystem<?> matchedSrid;

    SimpleTokenizer tokenizer;

    GeometryType type;
    GeometryBuilder builder;
    boolean hasMMark = false;

    enum Delimiter {CLOSE, OPEN, SEP, NO_DELIM}

    /**
     * The constructor of this AbstractWktDecoder. It sets the variant.
     *
     * @param wktDialect The <code>WktVariant</code> to be used by this decoder.
     */
    BaseWktGeometryParser(BaseWktDialect wktDialect, String wkt, CoordinateReferenceSystem<P> crs) {
        dialect = wktDialect;
        tokenizer = new SimpleTokenizer(wkt);
        this.overrideCrs = crs;
    }

    Geometry<P> parse() {
        matchesOptionalSrid();
        builder = matchesGeometryTaggedText();
        CoordinateReferenceSystem<P> adapted = adaptCrs();
        return builder.createGeometry(adapted);
    }

    protected GeometryBuilder matchesGeometryTaggedText() {
        matchesGeometryKeyword();
        GeometryBuilder builder = GeometryBuilder.create(type);
        matchesOptionalZMMarkers();
        matchesTaggedText(builder);
        return builder;
    }

    @SuppressWarnings("unchecked")
    protected CoordinateReferenceSystem<P> adaptCrs() {
        if (overrideCrs != null) {
            if (overrideCrs.getCoordinateDimension() < this.builder.getCoordinateDimension()) {
                throw new WktDecodeException("Target CoordinateReferenceSystem not compatible with coordinate dimension");
            }
            return overrideCrs;
        } else {
            CoordinateReferenceSystem<?> crs = this.matchedSrid != null ? matchedSrid : DEFAULT_CRS;
            return (CoordinateReferenceSystem<P>)widenCrsToCoordinateDimension(crs);
        }
    }

    protected CoordinateReferenceSystem<?> widenCrsToCoordinateDimension(CoordinateReferenceSystem<?> crs) {
        return  CoordinateReferenceSystems.adjustTo(crs, builder.getCoordinateDimension(), hasMMark);
    }

    protected void matchesOptionalSrid() {
        //do nothing
    }

    protected void setMatchedSrid(CoordinateReferenceSystem<?> crs){
        this.matchedSrid = crs;
    }

    protected void matchesGeometryKeyword() {
        for (Map.Entry<GeometryType, Pattern> entry : dialect.geometryTypePatternMap().entrySet()) {
            if (tokenizer.matchPattern(entry.getValue())) {
                type = entry.getKey();
                return;
            }
        }
        throw new WkbDecodeException("Expected geometryKeyword starting at position: " + tokenizer.currentPos());
    }

    protected void matchesOptionalZMMarkers() {
        //do nothing in base case
    }

    protected void matchesTaggedText(GeometryBuilder builder) {
        if (tokenizer.matchPattern(EMPTY_PATTERN)) {
            return;
        }
        matchesPositionText(builder);
    }

    protected void matchesPositionText(GeometryBuilder builder) {
        switch (type) {
            case POINT:
                builder.setPositions(matchesSinglePosition());
                break;
            case LINESTRING:
                builder.setPositions(matchesPositionList());
                break;
            case POLYGON:
            case MULTILINESTRING:
                builder.setPositions(matchesListOfPositionList());
                break;
            case MULTIPOLYGON:
                builder.setPositions(matchesPolygonList());
                break;
            case MULTIPOINT:
                builder.setPositions(matchesMultiPointList());
                break;
            case GEOMETRYCOLLECTION:
                matchGeometries((CollectionGeometryBuilder) builder);
                break;
            default:
                throw new WktDecodeException("Unknown geometry type");
        }

    }

    protected Holder matchesMultiPointList() {
        return matchesPositionList();
    }

    protected void matchGeometries(CollectionGeometryBuilder builder) {
        if (!tokenizer.matchesOpenList()) {
            throw new WktDecodeException("Expected '(' near position " + tokenizer.currentPos());
        }
        Delimiter d;
        do {
            builder.push(matchesGeometryTaggedText());
            d = matchesDelimiter();
            if (d == Delimiter.NO_DELIM) {
                throw new WktDecodeException(String.format("Expected ')' or ',' near %d", tokenizer.currentPos()));
            }
        } while (d != Delimiter.CLOSE);
    }

    protected PointHolder matchesPosition() {
        PointHolder pnt = new PointHolder();
        Delimiter d;
        do {
            pnt.push(tokenizer.fastReadNumber());
            d = matchesDelimiter();
        } while ( !( d == Delimiter.CLOSE || d == Delimiter.SEP) );
        tokenizer.back(1);
        return pnt;
    }

    protected PointHolder matchesSinglePosition() {
        if (!tokenizer.matchesOpenList()) {
            throw new WktDecodeException("Expected '(' near position " + tokenizer.currentPos());
        }
        PointHolder pnt = matchesPosition();
        if (!tokenizer.matchesCloseList()) {
            throw new WktDecodeException("Expected ')' near position " + tokenizer.currentPos());
        }
        return pnt;
    }

    protected LinearPositionsHolder matchesPositionList() {
        if (!tokenizer.matchesOpenList()) {
            throw new WktDecodeException("Expected '(' near position " + tokenizer.currentPos());
        }
        LinearPositionsHolder lph = new LinearPositionsHolder();
        Delimiter d;
        do {
            lph.push(matchesPosition());
            d = matchesDelimiter();
            if (d == Delimiter.NO_DELIM) {
                throw new WktDecodeException(String.format("Expected ')' or ',' near %d", tokenizer.currentPos()));
            }
        } while (d != Delimiter.CLOSE);
        return lph;
    }

    protected LinearPositionsListHolder matchesListOfPositionList() {
        if (!tokenizer.matchesOpenList()) {
            throw new WktDecodeException("Expected '(' near position " + tokenizer.currentPos());
        }
        LinearPositionsListHolder lplh = new LinearPositionsListHolder();
        Delimiter d;
        do {
            lplh.push(matchesPositionList());
            d = matchesDelimiter();
            if (d == Delimiter.NO_DELIM) {
                throw new WktDecodeException(String.format("Expected ')' or ',' near %d", tokenizer.currentPos()));
            }
        } while (d != Delimiter.CLOSE);
        return lplh;
    }

    protected PolygonListHolder matchesPolygonList() {
        if (!tokenizer.matchesOpenList()) {
            throw new WktDecodeException("Expected '(' near position " + tokenizer.currentPos());
        }
        PolygonListHolder plh = new PolygonListHolder();
        Delimiter d;
        do {
            plh.push(matchesListOfPositionList());
            d = matchesDelimiter();
            if (d == Delimiter.NO_DELIM) {
                throw new WktDecodeException(String.format("Expected ')' or ',' near %d", tokenizer.currentPos()));
            }
        } while (d != Delimiter.CLOSE);
        return plh;
    }

    protected Delimiter matchesDelimiter() {
        Optional<Character> match = tokenizer.matchesOneOf(openListChar, closeListChar, elementSeparator);
        if (match.isPresent()) {
            switch (match.get()) {
                case ',':
                    return Delimiter.SEP;
                case '(':
                    return Delimiter.OPEN;
                case ')':
                    return Delimiter.CLOSE;
                default:
                    return Delimiter.NO_DELIM;
            }
        } else {
            return Delimiter.NO_DELIM;
        }
    }

    static abstract class GeometryBuilder {
        static GeometryBuilder create(GeometryType type) {
            if (type == GeometryType.GEOMETRYCOLLECTION) {
                return new CollectionGeometryBuilder();
            } else {
                return new SimpleGeometryBuilder(type);
            }
        }

        abstract <P extends Position> Geometry<P> createGeometry(CoordinateReferenceSystem<P> crs);

        abstract int getCoordinateDimension();

        abstract void setPositions(Holder positions);
    }

    static class SimpleGeometryBuilder extends GeometryBuilder {
        final GeometryType type;
        Holder positions;

        public SimpleGeometryBuilder(GeometryType type) {
            this.type = type;
        }

        <P extends Position> Geometry<P> createGeometry(CoordinateReferenceSystem<P> crs) {
            if (positions == null || positions.isEmpty()) return Geometries.mkEmptyGeometry(type, crs);
            try {
                return positions.toGeometry(crs, type);
            } catch(Throwable t){
                throw new WktDecodeException("Failed to create geometry for WKT", t);
            }
        }

        @Override
        int getCoordinateDimension() {
            return positions == null || positions.isEmpty() ? 2 : positions.getCoordinateDimension();
        }

        @Override
        void setPositions(Holder positions) {
            this.positions = positions;
        }

    }

    static class CollectionGeometryBuilder extends GeometryBuilder {
        List<GeometryBuilder> components = new ArrayList<>();


        void push(GeometryBuilder builder) {
            components.add(builder);
        }

        <P extends Position> Geometry<P> createGeometry(CoordinateReferenceSystem<P> crs) {
            if (components.isEmpty()) return Geometries.mkEmptyGeometry(GeometryType.GEOMETRYCOLLECTION, crs);
            List<Geometry<P>> geoms = components.stream()
                    .map(c -> c.createGeometry(crs))
                    .collect(Collectors.toList());
            return Geometries.mkGeometryCollection(geoms);
        }

        @Override
        int getCoordinateDimension() {
            return components.isEmpty() ? 2 : components.get(0).getCoordinateDimension();
        }

        @Override
        void setPositions(Holder positions) {
            throw new IllegalStateException("Can't set positions directly on this instance");
        }
    }

}


