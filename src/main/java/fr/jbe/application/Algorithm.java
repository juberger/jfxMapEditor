package fr.jbe.application;

import java.util.List;

public interface Algorithm {
    
    public static final int SCAN_LINE_COUNT = 1000;
    public static final double SCAN_LINE_LENGTH = 200;
    public static final boolean LIMIT_TO_SCAN_LINE_LENGTH = true;
    
    public List<Line> createScanLines(double startX, double startY);
    
    public List<PVector> getIntersectionPoints(List<Line> scanLines, List<Line> sceneLines);
    
    public PVector getLineIntersection(PVector location, Line line);
    
    public List<Line> getLineIntersectWith(Line scanLine, List<Line> sceneLines);
    
    public int getIntersectLineIndex(PVector location, List<Line> sceneLines);
}
