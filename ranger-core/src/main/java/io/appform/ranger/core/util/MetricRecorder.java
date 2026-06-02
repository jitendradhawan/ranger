package io.appform.ranger.core.util;

import com.codahale.metrics.MetricRegistry;
import io.appform.ranger.core.model.DataStoreType;
import lombok.experimental.UtilityClass;

import static java.util.concurrent.TimeUnit.*;

@UtilityClass
public class MetricRecorder {

  private static final String PACKAGE_PREFIX = "io.appform.ranger";
  public static final String ACTIVE = "active";
  private static final String INACTIVE = "inactive";
  private static final String NULL_OR_EMPTY_RESPONSE = "nullOrEmptyResponse";
  private static final String DATA_STORE_TYPE = "dataStoreType";
  private static final String DATA_SOURCE = "dataSource";
  private static final String SERVICE_NAME = "serviceName";
  private static final String ZOMBIE_NODES = "zombieNodes";
  private static final String HTTP_CALL = "httpCall";
  private static final String UNKNOWN_FAILURE = "unknownFailure";
  private static final String RESPONSE_PARSE_FAILURE = "responseParseFailure";
  private static final String NODE_DATA_REFRESH = "nodeDataRefresh";
  private static final String HEALTHY = "healthy";
  private static final String UNHEALTHY = "unhealthy";
  private static final String UPDATE = "update";

  public static final String SERVICES_LIST = "services";
  public static final String LIST_NODES = "listNodes";
  public static final String SUCCESS = "success";
  public static final String FAILURE = "failure";
  public static final String HEALTHCHECK = "healthcheck";
  public static final String NODE_DATA_SINK = "nodeDataSink";
  public static final String REGISTER_SERVICE = "registerService";
  public static final String SERIALIZATION = "serialization";
  public static final String DESERIALIZATION = "deserialization";
  public static final String STALE_DATA_RETAINED = "staleDataRetained";
  public static final String ZK_READ = "zkRead";

  private static MetricRegistry metricRegistry = new MetricRegistry();

  public static void initialize(MetricRegistry registry) {
    metricRegistry = registry;
  }

  public static void recordZombieNodeFound(String serviceName) {
    if (metricRegistry != null) {
      metricRegistry.meter(MetricRegistry.name(PACKAGE_PREFIX, ZOMBIE_NODES)).mark();
      metricRegistry.meter(MetricRegistry.name(PACKAGE_PREFIX, ZOMBIE_NODES, SERVICE_NAME, serviceName)).mark();
    }
  }

  public static void recordNoteDataSourceStatus(DataStoreType dataStoreType, String metricId, boolean active) {
    if (metricRegistry != null) {
      metricRegistry.meter(MetricRegistry.name(PACKAGE_PREFIX, DATA_STORE_TYPE, dataStoreType.name(),
              DATA_SOURCE, metricId, active ? ACTIVE : INACTIVE)).mark();
    }
  }

  public static void recordNodeDataRefreshSuccess(DataStoreType dataStoreType, String metricId, long elapsed) {
    if (metricRegistry != null) {
      metricRegistry.timer(MetricRegistry.name(PACKAGE_PREFIX, DATA_STORE_TYPE, dataStoreType.name(),
              DATA_SOURCE, metricId, NODE_DATA_REFRESH, SUCCESS)).update(elapsed, MILLISECONDS);
    }
  }

  public static void recordNodeDataRefreshFailure(String serviceName, DataStoreType dataStoreType,
                                                  String metricId, long elapsed) {
    if (metricRegistry != null) {
      metricRegistry.timer(MetricRegistry.name(PACKAGE_PREFIX, DATA_STORE_TYPE, dataStoreType.name(),
              DATA_SOURCE, metricId, NODE_DATA_REFRESH, FAILURE)).update(elapsed, MILLISECONDS);
      metricRegistry.timer(MetricRegistry.name(PACKAGE_PREFIX, DATA_STORE_TYPE, dataStoreType.name(),
                      DATA_SOURCE, metricId, SERVICE_NAME, serviceName, NODE_DATA_REFRESH, FAILURE))
              .update(elapsed, MILLISECONDS);
    }
  }

  public static void recordStaleDataRetained(String serviceName, DataStoreType dataStoreType, String metricId) {
    if (metricRegistry != null) {
      metricRegistry.meter(MetricRegistry.name(PACKAGE_PREFIX, DATA_STORE_TYPE, dataStoreType.name(),
              DATA_SOURCE, metricId, STALE_DATA_RETAINED)).mark();
      metricRegistry.meter(MetricRegistry.name(PACKAGE_PREFIX, DATA_STORE_TYPE, dataStoreType.name(),
              DATA_SOURCE, metricId, SERVICE_NAME, serviceName, STALE_DATA_RETAINED)).mark();
    }
  }

  public static void recordNodeDataSinkUpdateStatus(DataStoreType dataStoreType, String metricId, String status) {
    if (metricRegistry != null) {
      metricRegistry.meter(MetricRegistry.name(PACKAGE_PREFIX, DATA_STORE_TYPE, dataStoreType.name(),
              DATA_SOURCE, metricId, NODE_DATA_SINK, UPDATE, status)).mark();
    }
  }

  public static void recordHealthcheckFailure(DataStoreType dataStoreType, String metricId) {
    if (metricRegistry != null) {
      metricRegistry.meter(MetricRegistry.name(PACKAGE_PREFIX, DATA_STORE_TYPE, dataStoreType.name(),
              DATA_SOURCE, metricId, HEALTHCHECK, FAILURE)).mark();
    }
  }

  public static void recordHealthcheckStatus(DataStoreType dataStoreType, String metricId, boolean healthy) {
    if (metricRegistry != null) {
      metricRegistry.meter(MetricRegistry.name(PACKAGE_PREFIX, DATA_STORE_TYPE, dataStoreType.name(),
              DATA_SOURCE, metricId, HEALTHCHECK, "status", healthy ? HEALTHY : UNHEALTHY)).mark();
    }
  }

  public static void recordServicesFetchStatus(DataStoreType dataStoreType, String metricId, String success) {
    if (metricRegistry != null) {
      metricRegistry.meter(MetricRegistry.name(PACKAGE_PREFIX, DATA_STORE_TYPE, dataStoreType.name(),
              DATA_SOURCE, metricId, SERVICES_LIST, "fetch", success)).mark();
    }
  }

  public static void recordRemoteCallStatusCode(DataStoreType dataStoreType, String metricId,
                                                String remoteCall, int statusCode) {
    if (metricRegistry != null) {
      metricRegistry.meter(MetricRegistry.name(PACKAGE_PREFIX, DATA_STORE_TYPE, dataStoreType.name(),
              DATA_SOURCE, metricId, HTTP_CALL, remoteCall, "responseStatus", Integer.toString(statusCode))).mark();
    }
  }

  public static void recordCacheUpdateOnDroveEvent(DataStoreType dataStoreType, String metricId,
                                                   String eventName, String serviceName) {
    if (metricRegistry != null) {
      metricRegistry.meter(MetricRegistry.name(PACKAGE_PREFIX, DATA_STORE_TYPE, dataStoreType.name(),
              DATA_SOURCE, metricId, "cacheUpdateOnDroveEvent", eventName, SERVICE_NAME, serviceName)).mark();
    }
  }

  public static void recordServicesParseFailure(DataStoreType dataStoreType, String metricId) {
    if (metricRegistry != null) {
      metricRegistry.meter(MetricRegistry.name(PACKAGE_PREFIX, DATA_STORE_TYPE, dataStoreType.name(),
              DATA_SOURCE, metricId, HTTP_CALL, SERVICES_LIST, RESPONSE_PARSE_FAILURE)).mark();
    }
  }

  public static void recordListNodesParseFailure(DataStoreType dataStoreType, String metricId) {
    if (metricRegistry != null) {
      metricRegistry.meter(MetricRegistry.name(PACKAGE_PREFIX, DATA_STORE_TYPE, dataStoreType.name(),
              DATA_SOURCE, metricId, HTTP_CALL, LIST_NODES, RESPONSE_PARSE_FAILURE)).mark();
    }
  }

  public static void recordListNodesParseFailure(DataStoreType dataStoreType, String metricId, String serviceName) {
    if (metricRegistry != null) {
      metricRegistry.meter(MetricRegistry.name(PACKAGE_PREFIX, DATA_STORE_TYPE, dataStoreType.name(),
              DATA_SOURCE, metricId, HTTP_CALL, LIST_NODES, SERVICE_NAME, serviceName, RESPONSE_PARSE_FAILURE)).mark();
    }
  }


  public static void recordZookeeperReadUnknownFailure(DataStoreType dataStoreType, String metricId,
                                                       String operation, String exceptionName) {
    if (metricRegistry != null) {
      metricRegistry.meter(MetricRegistry.name(PACKAGE_PREFIX, DATA_STORE_TYPE, dataStoreType.name(),
              DATA_SOURCE, metricId, ZK_READ, operation, UNKNOWN_FAILURE)).mark();
      metricRegistry.meter(MetricRegistry.name(PACKAGE_PREFIX, DATA_STORE_TYPE, dataStoreType.name(),
              DATA_SOURCE, metricId, ZK_READ, operation, UNKNOWN_FAILURE, exceptionName)).mark();
    }
  }

  public static void recordRemoteCallUnknownFailure(DataStoreType dataStoreType, String metricId,
                                                    String httpMethodName, String exceptionName) {
    if (metricRegistry != null) {
      metricRegistry.meter(MetricRegistry.name(PACKAGE_PREFIX, DATA_STORE_TYPE, dataStoreType.name(),
              DATA_SOURCE, metricId, HTTP_CALL, httpMethodName, UNKNOWN_FAILURE)).mark();
      metricRegistry.meter(MetricRegistry.name(PACKAGE_PREFIX, DATA_STORE_TYPE, dataStoreType.name(),
              DATA_SOURCE, metricId, HTTP_CALL, httpMethodName, UNKNOWN_FAILURE, exceptionName)).mark();
    }
  }

  public static void recordRemoteCallUnknownFailure(DataStoreType dataStoreType, String metricId,
                                                    String httpMethodName, String exceptionName, String serviceName) {
    recordRemoteCallUnknownFailure(dataStoreType, metricId, httpMethodName, exceptionName);
    if (metricRegistry != null) {
      metricRegistry.meter(MetricRegistry.name(PACKAGE_PREFIX, DATA_STORE_TYPE, dataStoreType.name(),
              DATA_SOURCE, metricId, HTTP_CALL, httpMethodName, SERVICE_NAME, serviceName,
              UNKNOWN_FAILURE)).mark();
      metricRegistry.meter(MetricRegistry.name(PACKAGE_PREFIX, DATA_STORE_TYPE, dataStoreType.name(),
              DATA_SOURCE, metricId, HTTP_CALL, httpMethodName, SERVICE_NAME, serviceName,
              UNKNOWN_FAILURE, exceptionName)).mark();
    }
  }

  public static void recordNullOrEmptyServicesListResponse(DataStoreType dataStoreType, String metricId) {
    if (metricRegistry != null) {
      metricRegistry.meter(MetricRegistry.name(PACKAGE_PREFIX, DATA_STORE_TYPE, dataStoreType.name(),
              DATA_SOURCE, metricId, HTTP_CALL, SERVICES_LIST, NULL_OR_EMPTY_RESPONSE)).mark();
    }
  }

  public static void recordNullOrEmptyListNodeResponse(DataStoreType dataStoreType, String metricId) {
    if (metricRegistry != null) {
      metricRegistry.meter(MetricRegistry.name(PACKAGE_PREFIX, DATA_STORE_TYPE, dataStoreType.name(),
              DATA_SOURCE, metricId, HTTP_CALL, LIST_NODES, NULL_OR_EMPTY_RESPONSE)).mark();
    }
  }

  public static void recordNullOrEmptyListNodeResponse(DataStoreType dataStoreType, String metricId,
                                                       String serviceName) {
    if (metricRegistry != null) {
      metricRegistry.meter(MetricRegistry.name(PACKAGE_PREFIX, DATA_STORE_TYPE, dataStoreType.name(),
              DATA_SOURCE, metricId, HTTP_CALL, LIST_NODES, SERVICE_NAME, serviceName, NULL_OR_EMPTY_RESPONSE)).mark();
    }
  }

  public static void recordNodeDataSinkUnknownFailure(DataStoreType dataStoreType, String metricId,
                                                      String serviceName, String exceptionName) {
    if (metricRegistry != null) {
      metricRegistry.meter(MetricRegistry.name(PACKAGE_PREFIX, DATA_STORE_TYPE, dataStoreType.name(),
              DATA_SOURCE, metricId, NODE_DATA_SINK, UNKNOWN_FAILURE)).mark();
      metricRegistry.meter(MetricRegistry.name(PACKAGE_PREFIX, DATA_STORE_TYPE, dataStoreType.name(),
              DATA_SOURCE, metricId, NODE_DATA_SINK, SERVICE_NAME, serviceName, UNKNOWN_FAILURE)).mark();
      metricRegistry.meter(MetricRegistry.name(PACKAGE_PREFIX, DATA_STORE_TYPE, dataStoreType.name(),
              DATA_SOURCE, metricId, NODE_DATA_SINK, SERVICE_NAME, serviceName, UNKNOWN_FAILURE, exceptionName)).mark();
    }
  }

  public static void recordNodeDataSinkSerDeFailure(DataStoreType dataStoreType, String metricId,
                                                    String serDe, String serviceName, String exceptionName) {
    if (metricRegistry != null) {
      metricRegistry.meter(MetricRegistry.name(PACKAGE_PREFIX, DATA_STORE_TYPE, dataStoreType.name(),
              DATA_SOURCE, metricId, NODE_DATA_SINK, serDe, FAILURE)).mark();
      metricRegistry.meter(MetricRegistry.name(PACKAGE_PREFIX, DATA_STORE_TYPE, dataStoreType.name(),
              DATA_SOURCE, metricId, NODE_DATA_SINK, serDe, SERVICE_NAME, serviceName, FAILURE)).mark();
      metricRegistry.meter(MetricRegistry.name(PACKAGE_PREFIX, DATA_STORE_TYPE, dataStoreType.name(),
              DATA_SOURCE, metricId, NODE_DATA_SINK, serDe, SERVICE_NAME, serviceName, FAILURE, exceptionName)).mark();
    }
  }

  public static void recordNullOrEmptyRegisterServiceResponse(DataStoreType dataStoreType, String metricId,
                                                              String serviceName) {
    if (metricRegistry != null) {
      metricRegistry.meter(MetricRegistry.name(PACKAGE_PREFIX, DATA_STORE_TYPE, dataStoreType.name(),
              DATA_SOURCE, metricId, HTTP_CALL, REGISTER_SERVICE, NULL_OR_EMPTY_RESPONSE)).mark();
      metricRegistry.meter(MetricRegistry.name(PACKAGE_PREFIX, DATA_STORE_TYPE, dataStoreType.name(),
              DATA_SOURCE, metricId, HTTP_CALL, REGISTER_SERVICE, SERVICE_NAME, serviceName, NULL_OR_EMPTY_RESPONSE))
              .mark();
    }
  }

  public static void recordServiceNodesReturned(String serviceName, int serviceNodes) {
    if (metricRegistry != null) {
      metricRegistry.histogram(MetricRegistry.name(PACKAGE_PREFIX, SERVICE_NAME,
              serviceName, "nodesReturned")).update(serviceNodes);
    }
  }

  public static void recordServicesReturned(int services) {
    if (metricRegistry != null) {
      metricRegistry.histogram(MetricRegistry.name(PACKAGE_PREFIX,"servicesReturned"))
              .update(services);
    }
  }

}
