package com.logistics.fleet.service;

import com.logistics.fleet.config.AsyncFleetThreadPoolConfig;
import com.logistics.fleet.model.Driver;
import com.logistics.fleet.repository.DriverRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Multi-Threaded Geo-Spatial Driver Matching Service.
 * Evaluates distances, vehicle capacity scores, and routing estimates concurrently
 * across available drivers using CompletableFuture.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ParallelDriverMatchingService {

    private final DriverRepository driverRepository;

    @Qualifier(AsyncFleetThreadPoolConfig.FLEET_MATCH_EXECUTOR)
    private final Executor fleetMatchingExecutor;

    @Data
    @Builder
    public static class DriverMatchScore {
        private Driver driver;
        private double distanceKm;
        private int estimatedEtaMinutes;
        private double suitabilityScore;
    }

    /**
     * Calculates spatial match scores for all candidate drivers in parallel worker threads.
     */
    public List<DriverMatchScore> rankDriversConcurrently(
            Double targetLat,
            Double targetLon,
            Driver.VehicleType vehicleType) {

        List<Driver> candidates = driverRepository.findByStatusAndVehicleType(
                Driver.DriverStatus.AVAILABLE,
                vehicleType != null ? vehicleType : Driver.VehicleType.MOTORBIKE
        );

        if (candidates.isEmpty()) {
            return List.of();
        }

        log.info("Spawning parallel score calculation across {} candidate drivers", candidates.size());

        List<CompletableFuture<DriverMatchScore>> futures = candidates.stream()
                .map(driver -> CompletableFuture.supplyAsync(() -> {
                    log.debug("[Thread: {}] Computing match score for driver [{}]",
                            Thread.currentThread().getName(), driver.getFullName());

                    double distance = calculateHaversineDistanceKm(
                            driver.getCurrentLatitude() != null ? driver.getCurrentLatitude() : targetLat,
                            driver.getCurrentLongitude() != null ? driver.getCurrentLongitude() : targetLon,
                            targetLat,
                            targetLon
                    );

                    // Speed approximation: 30 km/h urban speed -> 2 minutes per km + 3 min fixed buffer
                    int eta = (int) Math.round(distance * 2.0 + 3.0);

                    // Score inversely proportional to distance (closer = higher score)
                    double score = Math.max(0.0, 100.0 - (distance * 5.0));

                    return DriverMatchScore.builder()
                            .driver(driver)
                            .distanceKm(Math.round(distance * 100.0) / 100.0)
                            .estimatedEtaMinutes(eta)
                            .suitabilityScore(Math.round(score * 10.0) / 10.0)
                            .build();
                }, fleetMatchingExecutor))
                .toList();

        // Wait for all driver computations to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        List<DriverMatchScore> results = new ArrayList<>();
        for (CompletableFuture<DriverMatchScore> f : futures) {
            try {
                results.add(f.get());
            } catch (Exception e) {
                log.error("Driver match calculation failed: {}", e.getMessage());
                Thread.currentThread().interrupt();
            }
        }

        results.sort(Comparator.comparingDouble(DriverMatchScore::getDistanceKm));
        return results;
    }

    private double calculateHaversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
