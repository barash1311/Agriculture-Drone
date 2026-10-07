package com.agridrone.safety;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.agridrone.safety.domain.navigation.DistanceCalculator;

import org.junit.Test;

public class DistanceCalculatorTest {

    @Test
    public void testIdenticalCoordinatesReturnZero() {
        double dist = DistanceCalculator.calculateDistanceMeters(37.7749, -122.4194, 37.7749, -122.4194);
        assertEquals(0.0, dist, 0.001);
    }

    @Test
    public void testKnownCoordinatePair() {
        double dist = DistanceCalculator.calculateDistanceMeters(37.7749, -122.4194, 37.8044, -122.2711);
        assertEquals(13440.0, dist, 150.0); // within 150m across 13.4km
    }

    @Test
    public void testShortAgriculturalDistance() {
        double dist = DistanceCalculator.calculateDistanceMeters(37.7749, -122.4194, 37.7759, -122.4194);
        assertEquals(111.0, dist, 3.0);
    }

    @Test
    public void testNullCoordinates() {
        double dist = DistanceCalculator.calculateDistanceMeters(null, -122.4194, 37.7749, -122.4194);
        assertTrue(Double.isNaN(dist));
    }
}
