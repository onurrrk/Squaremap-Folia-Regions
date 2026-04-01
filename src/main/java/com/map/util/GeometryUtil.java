package com.map.util;

import xyz.jpenilla.squaremap.api.Point;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

public class GeometryUtil {

    public static int extractX(long chunkKey) {
        return (int) chunkKey;
    }

    public static int extractZ(long chunkKey) {
        return (int) (chunkKey >>> 32);
    }

    private static double calculateTurn(Point a, Point b, Point c) {
        return (b.x() - a.x()) * (c.z() - a.z()) - (b.z() - a.z()) * (c.x() - a.x());
    }

    public static List<Point> computeConvexHull(List<Point> inputVertices) {
        if (inputVertices.size() < 3) return inputVertices;

        HashSet<Point> distinct = new HashSet<>(inputVertices);
        List<Point> sortedVertices = new ArrayList<>(distinct);
        sortedVertices.sort(Comparator.comparingDouble(Point::x).thenComparingDouble(Point::z));

        List<Point> topHalf = new ArrayList<>();
        List<Point> bottomHalf = new ArrayList<>();

        for (Point pt : sortedVertices) {
            while (topHalf.size() >= 2 && calculateTurn(topHalf.get(topHalf.size() - 2), topHalf.get(topHalf.size() - 1), pt) >= 0) {
                topHalf.remove(topHalf.size() - 1);
            }
            topHalf.add(pt);
        }

        for (int i = sortedVertices.size() - 1; i >= 0; i--) {
            Point pt = sortedVertices.get(i);
            while (bottomHalf.size() >= 2 && calculateTurn(bottomHalf.get(bottomHalf.size() - 2), bottomHalf.get(bottomHalf.size() - 1), pt) >= 0) {
                bottomHalf.remove(bottomHalf.size() - 1);
            }
            bottomHalf.add(pt);
        }

        topHalf.remove(topHalf.size() - 1);
        bottomHalf.remove(bottomHalf.size() - 1);
        topHalf.addAll(bottomHalf);

        Point previous = null;
        topHalf.add(topHalf.get(0));

        List<Point> finalShape = new ArrayList<>();
        for (Point current : topHalf) {
            if (previous != null && previous.x() != current.x() && previous.z() != current.z()) {
                Point alt1 = Point.of(previous.x(), current.z());
                if (distinct.contains(alt1)) {
                    finalShape.add(alt1);
                } else {
                    Point alt2 = Point.of(current.x(), previous.z());
                    if (distinct.contains(alt2)) {
                        finalShape.add(alt2);
                    }
                }
            }
            previous = current;
            finalShape.add(current);
        }
        
        finalShape.remove(finalShape.size() - 1);
        return finalShape;
    }
}