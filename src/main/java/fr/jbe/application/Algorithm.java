package fr.jbe.application;

import java.util.ArrayList;
import java.util.List;

public class Algorithm {
    
    private int scanLineCount = 1000;
    private double scanLineLength = 200;
    private boolean limitToScanLineLength = true;
    
    public Algorithm() {}
    
    public Algorithm(int scanLineCount, double scanLineLength, boolean limitToScanLineLength) {
        this.scanLineCount = scanLineCount;
        this.scanLineLength = scanLineLength;
        this.limitToScanLineLength = limitToScanLineLength;
    }

	/**
	 * Sweep around the given circle with the given distance and create the scan lines
	 * @param startX
	 * @param startY
	 * @return
	 */
	public List<Line> createScanLines(double startX, double startY) {
		
		List<Line> scanLines;
		
		double angleStart = 0;
		double angleEnd = Math.PI * 2;
		double step = Math.PI / scanLineCount;
		
		scanLines = new ArrayList<>();
		
		PVector scanLine = new PVector( startX, startY);
		
		for( double angle = angleStart; angle < angleEnd; angle += step) {

			double x = scanLine.x + Math.cos(angle) * scanLineLength;
			double y = scanLine.y + Math.sin(angle) * scanLineLength;
			
			Line line = new Line( scanLine, new PVector( x, y));
			
			scanLines.add( line);

		}
		
		return scanLines;
	}
	
	/**
	 * Get all the intersecting points for the given scan lines and the given scene lines.
     *
	 * @param scanLines
	 * @param sceneLines
	 * @return
	 */
	public List<PVector> getIntersectionPoints(List<Line> scanLines, List<Line> sceneLines) {

		List<PVector> points = new ArrayList<>();

		for (Line scanLine : scanLines) {

			List<PVector> intersections = getIntersections(scanLine, sceneLines);

			double x = 0;
			double y = 0;
			double dist = Double.MAX_VALUE;
			
			// find the intersection that is closest to the scanline
			if (intersections.size() > 0) {

				for (PVector item : intersections) {

					double currDist = scanLine.getStart().dist(item);

					if (currDist < dist) {
						x = item.x;
						y = item.y;

						dist = currDist;

					}
				}

				points.add(new PVector(x, y));
			} else {
			    points.add(scanLine.getEnd());
			}

		}

		return points;
	}

	/**
	 * Find intersecting lines
	 * @param scanLine
	 * @param sceneLines
	 * @return
	 */
	public List<PVector> getIntersections(Line scanLine, List<Line> sceneLines) {

		List<PVector> list = new ArrayList<>();

		PVector intersection;

		for (Line line : sceneLines) {

			// check if 2 lines intersect
			intersection = getLineIntersection(scanLine, line);

			// lines intersect => we have an end point
			PVector end = null;
			if (intersection != null) {

				end = new PVector(intersection.x, intersection.y);

			}

			// check if the intersection area should be limited to a visible area
			if (limitToScanLineLength) {

				// maximum scan line length
				double maxLength = scanLineLength;

				PVector start = scanLine.getStart();

				// no intersection found => full scan line length
				if (end == null) {

					end = new PVector(scanLine.getEnd().x, scanLine.getEnd().y);

				}
				// intersection found => limit to scan line length
				else if (start.dist(end) > maxLength) {

					end.normalize();
					end.mult(maxLength);

				}

			}

			// we have a valid line end, either an intersection with another line or we have the scan line limit
			if (end != null) {
				list.add(end);
			}

		}

		return list;
	}

	// find intersection point of 2 line segments
	//
	// http://stackoverflow.com/questions/563198/how-do-you-detect-where-two-line-segments-intersect
	// http://www.openprocessing.org/sketch/135314
	// http://www-cs.ccny.cuny.edu/~wolberg/capstone/intersection/Intersection%20point%20of%20two%20lines.html
	private PVector getLineIntersection(Line lineA, Line lineB) {
		double x1 = lineA.getStart().x;
		double y1 = lineA.getStart().y;
		double x2 = lineA.getEnd().x;
		double y2 = lineA.getEnd().y;

		double x3 = lineB.getStart().x;
		double y3 = lineB.getStart().y;
		double x4 = lineB.getEnd().x;
		double y4 = lineB.getEnd().y;

		double ax = x2 - x1;
		double ay = y2 - y1;
		double bx = x4 - x3;
		double by = y4 - y3;

		double denominator = ax * by - ay * bx;

		if (denominator == 0)
			return null;

		double cx = x3 - x1;
		double cy = y3 - y1;

		double t = (cx * by - cy * bx) / denominator;
		if (t < 0 || t > 1)
			return null;

		double u = (cx * ay - cy * ax) / denominator;
		if (u < 0 || u > 1)
			return null;

		return new PVector(x1 + t * ax, y1 + t * ay);
	}

    public int getScanLineCount() {
        return scanLineCount;
    }

    public void setScanLineCount(int scanLineCount) {
        this.scanLineCount = scanLineCount;
    }

    public double getScanLineLength() {
        return scanLineLength;
    }

    public void setScanLineLength(double scanLineLength) {
        this.scanLineLength = scanLineLength;
    }

    public boolean isLimitToScanLineLength() {
        return limitToScanLineLength;
    }

    public void setLimitToScanLineLength(boolean limitToScanLineLength) {
        this.limitToScanLineLength = limitToScanLineLength;
    }

}
