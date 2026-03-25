package com.c15tour.backend.service;

import com.c15tour.backend.service.osrm.OSRMResponse;
import com.c15tour.model.Coordinates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoutingServiceTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec<?> requestHeadersUriSpec;

    @Mock
    private RestClient.RequestHeadersSpec<?> requestHeadersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private RoutingService routingService;

    @BeforeEach
    void setUp() {
        routingService = new RoutingService(restClient);
    }

    @Test
    void calculateRoute_ShouldReturnNull_WhenCoordinatesListIsNull() {
        assertThat(routingService.calculateRoute(null)).isNull();
    }

    @Test
    void calculateRoute_ShouldReturnNull_WhenFewerThanTwoCoordinates() {
        Coordinates single = coordinates(47.2, -1.5);
        assertThat(routingService.calculateRoute(List.of(single))).isNull();
        verifyNoInteractions(restClient);
    }

    @Test
    @SuppressWarnings("unchecked")
    void calculateRoute_ShouldReturnResponse_WhenOsrmReturnsOk() {
        OSRMResponse mockResponse = new OSRMResponse("Ok", List.of(), List.of());

        when(restClient.get()).thenReturn((RestClient.RequestHeadersUriSpec) requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn((RestClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(OSRMResponse.class)).thenReturn(mockResponse);

        List<Coordinates> coords = List.of(coordinates(47.2, -1.5), coordinates(48.8, 2.3));
        OSRMResponse result = routingService.calculateRoute(coords);

        assertThat(result).isNotNull();
        assertThat(result.code()).isEqualTo("Ok");
    }

    @Test
    @SuppressWarnings("unchecked")
    void calculateRoute_ShouldReturnNull_WhenOsrmReturnsNonOkCode() {
        OSRMResponse errorResponse = new OSRMResponse("NoRoute", List.of(), List.of());

        when(restClient.get()).thenReturn((RestClient.RequestHeadersUriSpec) requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn((RestClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(OSRMResponse.class)).thenReturn(errorResponse);

        List<Coordinates> coords = List.of(coordinates(47.2, -1.5), coordinates(48.8, 2.3));
        assertThat(routingService.calculateRoute(coords)).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void calculateRoute_ShouldReturnNull_WhenRestClientThrowsException() {
        when(restClient.get()).thenReturn((RestClient.RequestHeadersUriSpec) requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn((RestClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(OSRMResponse.class)).thenThrow(new RestClientException("Network error"));

        List<Coordinates> coords = List.of(coordinates(47.2, -1.5), coordinates(48.8, 2.3));
        assertThat(routingService.calculateRoute(coords)).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void calculateRoute_WithoutDirections_ShouldDefaultToFalse() {
        OSRMResponse mockResponse = new OSRMResponse("Ok", List.of(), List.of());

        when(restClient.get()).thenReturn((RestClient.RequestHeadersUriSpec) requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn((RestClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(OSRMResponse.class)).thenReturn(mockResponse);

        List<Coordinates> coords = List.of(coordinates(47.2, -1.5), coordinates(48.8, 2.3));
        // Default overload should not throw
        assertThat(routingService.calculateRoute(coords)).isNotNull();
    }

    private Coordinates coordinates(double lat, double lon) {
        Coordinates c = new Coordinates();
        c.setLatitude(lat);
        c.setLongitude(lon);
        return c;
    }
}
