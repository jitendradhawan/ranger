package io.appform.ranger.core.util;

import com.codahale.metrics.MetricRegistry;
import lombok.experimental.UtilityClass;

@UtilityClass
public class MetricRecorder {

  private static final String PACKAGE_PREFIX = "io.appform.ranger";

  private static MetricRegistry metricRegistry = new MetricRegistry();

  public static void initialize(MetricRegistry registry) {
    metricRegistry = registry;
  }

  public static void recordZombieNodeFound(String serviceName) {
    metricRegistry.meter(MetricRegistry.name(PACKAGE_PREFIX, "zombieNodes")).mark();
    metricRegistry.meter(MetricRegistry.name(PACKAGE_PREFIX, "zombieNodes", "service", serviceName)).mark();
  }

  public static void recordZkConnection(boolean zkConnectionActive) {
    metricRegistry.meter(MetricRegistry.name(PACKAGE_PREFIX,"zkConnection", zkConnectionActive ? "active" : "inactive")).mark();
  }

  public static void recordHttpUpstreamAvailability(String clientId, boolean httpUpstreamAvailable) {
    metricRegistry.meter(MetricRegistry.name(PACKAGE_PREFIX,"httpUpstream", clientId, httpUpstreamAvailable ? "active" : "inactive")).mark();
  }
}
