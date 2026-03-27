package com.medhead.poc.infrastructure.adapter.out.routing;

import com.medhead.poc.domain.model.GpsCoordinates;
import com.medhead.poc.domain.model.RouteInfo;
import com.medhead.poc.domain.port.out.DistanceCalculator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * OSRM-backed {@link DistanceCalculator}. Calls the local {@code osrm-routed}
 * HTTP server at {@code /route/v1/driving/{lon},{lat};{lon},{lat}} and
 * normalises the response from metres/seconds into kilometres/minutes.
 */
@Component
public class OsrmDistanceCalculator implements DistanceCalculator {

    private static final String ROUTE_PATH = "/route/v1/driving/%s,%s;%s,%s?overview=false";

    private final RestClient osrmRestClient;

    public OsrmDistanceCalculator(RestClient osrmRestClient) {
        this.osrmRestClient = osrmRestClient;
    }

    @Override
    public RouteInfo calculate(GpsCoordinates origin, GpsCoordinates destination) {
        String path = String.format(
                Locale.ROOT,
                ROUTE_PATH,
                formatCoordinate(origin.longitude()),
                formatCoordinate(origin.latitude()),
                formatCoordinate(destination.longitude()),
                formatCoordinate(destination.latitude())
        );

        OsrmRouteResponse response = osrmRestClient.get()
                .uri(path)
                .retrieve()
                .body(OsrmRouteResponse.class);

        if (response == null || response.routes() == null || response.routes().isEmpty()) {
            throw new IllegalStateException("OSRM returned no route for " + origin + " -> " + destination);
        }
        OsrmRoute first = response.routes().get(0);
        return new RouteInfo(first.distance() / 1000.0, first.duration() / 60.0);
    }

    private static String formatCoordinate(double value) {
        return String.format(Locale.ROOT, "%.6f", value);
    }

    private record OsrmRouteResponse(List<OsrmRoute> routes) {
    }

    private record OsrmRoute(double distance, double duration) {
    }
}
