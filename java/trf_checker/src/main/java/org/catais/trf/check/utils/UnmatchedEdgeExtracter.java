package org.catais.trf.check.utils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;

public class UnmatchedEdgeExtracter {

	public static LineString toLineString(LineSegment lineseg, GeometryFactory fact) {
	    Coordinate[] coords = { lineseg.p0, lineseg.p1 };
	    return fact.createLineString(coords);
	}

	private boolean isDiff;   // hack to control how matching is done
	private Map lineMap = new TreeMap();

	public UnmatchedEdgeExtracter() {

	}

	public void add(Geometry geom)
	{
		// don't need to worry about orienting polygons
		add(CoordinateArrays.toCoordinateArrays(geom, false));
	}

	public void add(LineString line)
	{
		add(line.getCoordinates());
	}
	public void add(List coordArrays)
	{
		for (Iterator i = coordArrays.iterator(); i.hasNext(); ) {
			add((Coordinate[]) i.next());
		}
	}
	public void add(Coordinate[] coord)
	{
		for (int i = 0; i < coord.length - 1; i++) {
			LineSegment lineseg = new LineSegment(coord[i], coord[i + 1]);
			lineseg.normalize();

			Counter counter = (Counter) lineMap.get(lineseg);
			if (counter == null) {
				lineMap.put(lineseg, new Counter(1));
			}
			else {
				counter.increment();
			}
		}
	}
	
	/**
	 * This function operates in two different modes depending on the value of isDiff.
	 * If isDiff is true, the function returns true if the lineseg is present
	 * at all in the map.
	 * Is isDiff is false, the function returns true if the lineseg appears more than once
	 * in the map.
	 *
	 * @param lineseg
	 * @return true if the lineseg has a match
	 */
	public boolean isMatched(LineSegment lineseg)
	{
		Counter counter = (Counter) lineMap.get(lineseg);
		if (counter == null) return false;
		if (isDiff) {
			return true;
		}
		else {
			if (counter.getValue() > 1) return true;
			return false;
		}
	}
	
	/**
	 * Compute a list of all subsequences of segments in the
	 * LineString line which do not appear in the map.
	 */
	public void getDiffEdges(Geometry geom, List edgeList)
	{
		getEdges(CoordinateArrays.toCoordinateArrays(geom, false), true, edgeList);
	}
	
	/**
	 * Compute a list of all subsequences of segments in the
	 * LineString line which appear in the line only once.
	 */
	public void getUnmatchedEdges(Geometry geom, List edgeList)
	{
		getEdges(CoordinateArrays.toCoordinateArrays(geom, false), false, edgeList);
	}

	private void getEdges(List coordArrays, boolean isDiff, List edgeList)
	{
		for (Iterator i = coordArrays.iterator(); i.hasNext(); ) {
			getEdges((Coordinate[]) i.next(), isDiff, edgeList);
		}
	}
	
	private void getEdges(Coordinate[] coord, boolean isDiff, List edgeList)
	{
		this.isDiff = isDiff;
		GeometryFactory fact = new GeometryFactory();
		// start is the index of the start of each line segment in the list
		int start = 0;
		while (start < coord.length - 1) {
			int end = getUnmatchedSequenceEnd(coord, start);
			if (start < end) {
				Coordinate[] edgeCoord = new Coordinate[end - start + 1];
				int edgeIndex = 0;
				for (int i = start; i <= end; i++) {
					edgeCoord[edgeIndex++] = coord[i];
				}
				LineString edge = fact.createLineString(edgeCoord);
				edgeList.add(edge);
				start = end;
			}
			else {
				start++;
			}
		}
	}
	
	/**
	 * If no sequence matches, the value returned is equal to start
	 */
	public int getUnmatchedSequenceEnd(Coordinate[] coord, int start)
	{
		LineSegment lineseg = new LineSegment();
		int index = start;
		// loop while segments are unmatched
		while (index < coord.length - 1) {
			lineseg.setCoordinates(coord[index], coord[index + 1]);
			lineseg.normalize();
			// if this segment is matched, exit loop
			if (isMatched(lineseg)) {
				break;
			}
			index++;
		}
		return index;
	}
}
