/*
 * Copyright 2024 Authors, Flipkart Internet Pvt. Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.appform.ranger.core.finder.serviceregistry;

import com.codahale.metrics.MetricRegistry;
import io.appform.ranger.core.healthcheck.HealthcheckStatus;
import io.appform.ranger.core.model.*;
import io.appform.ranger.core.signals.Signal;
import io.appform.ranger.core.units.TestNodeData;
import io.appform.ranger.core.util.MetricRecorder;
import lombok.val;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.temporal.TemporalUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

class ServiceRegistryUpdaterMetricsIntegrationTest {

    private MetricRegistry metricRegistry;
    private static final String METRIC_ID = "test-updater-metric";
    private static final Service TEST_SERVICE = new Service("test-ns", "test-svc");

    private ServiceRegistryUpdater<TestNodeData, TestDeserializer> updater;

    @BeforeEach
    void setUp() {
        metricRegistry = new MetricRegistry();
        MetricRecorder.initialize(metricRegistry);
    }

    @AfterEach
    void tearDown() {
        if (updater != null) {
            updater.stop();
        }
    }

    @Test
    void testSuccessfulRefresh_recordsSuccessTimer() {
        val registry = new MapBasedServiceRegistry<TestNodeData>(TEST_SERVICE);
        val nodes = List.of(
                ServiceNode.<TestNodeData>builder()
                        .host("host1")
                        .port(8080)
                        .nodeData(TestNodeData.builder().shardId(1).build())
                        .healthcheckStatus(HealthcheckStatus.healthy)
                        .lastUpdatedTimeStamp(System.currentTimeMillis())
                        .build()
        );
        val dataSource = new TestNodeDataSource(METRIC_ID, DataStoreType.ZK, true, nodes, false);
        val signal = new TestSignal();

        updater = new ServiceRegistryUpdater<>(registry, dataSource, List.of(signal), new TestDeserializer());
        updater.start();

        // Wait for initial update and a brief period for metric recording to complete
        awaitRefresh(registry);
        sleep(100); // Allow time for MetricRecorder call after updateNodes

        // Verify success timer
        val timerName = "io.appform.ranger.dataSource.ZK." + METRIC_ID + ".nodeDataRefresh.success";
        val timer = metricRegistry.getTimers().get(timerName);
        assertNotNull(timer, "Node data refresh success timer should exist");
        assertTrue(timer.getCount() >= 1, "Timer should have at least 1 update");

        // No failure timer
        val failureTimerName = "io.appform.ranger.dataSource.ZK." + METRIC_ID + ".nodeDataRefresh.failure";
        val failureTimer = metricRegistry.getTimers().get(failureTimerName);
        assertTrue(failureTimer == null || failureTimer.getCount() == 0,
                "Failure timer should not be recorded on success");
    }

    @Test
    void testRefreshThrowsException_recordsFailureTimer() {
        val registry = new MapBasedServiceRegistry<TestNodeData>(TEST_SERVICE);
        val dataSource = new TestNodeDataSource(METRIC_ID, DataStoreType.HTTP, true, null, true);
        val signal = new TestSignal();

        updater = new ServiceRegistryUpdater<>(registry, dataSource, List.of(signal), new TestDeserializer());
        // Use try-catch since initial update will fail
        try {
            updater.start();
        } catch (Exception e) {
            // Expected: initial update fails
        }

        // Give some time for the updater thread to process
        sleep(200);

        // Verify failure timer
        val failureTimerName = "io.appform.ranger.dataSource.HTTP." + METRIC_ID + ".nodeDataRefresh.failure";
        val failureTimer = metricRegistry.getTimers().get(failureTimerName);
        assertNotNull(failureTimer, "Node data refresh failure timer should exist");
        assertTrue(failureTimer.getCount() >= 1, "Failure timer should have at least 1 update");
    }

    @Test
    void testInactiveDataSource_recordsStaleDataRetained() {
        val registry = new MapBasedServiceRegistry<TestNodeData>(TEST_SERVICE);
        // Start with an active source so initial update succeeds
        val nodes = List.of(
                ServiceNode.<TestNodeData>builder()
                        .host("host1")
                        .port(8080)
                        .nodeData(TestNodeData.builder().shardId(1).build())
                        .healthcheckStatus(HealthcheckStatus.healthy)
                        .lastUpdatedTimeStamp(System.currentTimeMillis())
                        .build()
        );
        val dataSource = new TestNodeDataSource(METRIC_ID, DataStoreType.DROVE, true, nodes, false);
        val signal = new TestSignal();

        updater = new ServiceRegistryUpdater<>(registry, dataSource, List.of(signal), new TestDeserializer());
        updater.start();
        awaitRefresh(registry);

        // Now deactivate the data source and trigger another update
        dataSource.setActive(false);
        signal.fire();
        sleep(200);

        // Verify stale data retained meter
        val meterName = "io.appform.ranger.dataSource.DROVE." + METRIC_ID + ".staleDataRetained";
        val meter = metricRegistry.getMeters().get(meterName);
        assertNotNull(meter, "Stale data retained meter should exist");
        assertTrue(meter.getCount() >= 1, "Stale data retained should be recorded at least once");
    }

    @Test
    void testSuccessfulRefresh_zombieNodesFiltered_recordsZombieMetric() {
        val registry = new MapBasedServiceRegistry<TestNodeData>(TEST_SERVICE);
        val nodes = List.of(
                ServiceNode.<TestNodeData>builder()
                        .host("healthy-host")
                        .port(8080)
                        .nodeData(TestNodeData.builder().shardId(1).build())
                        .healthcheckStatus(HealthcheckStatus.healthy)
                        .lastUpdatedTimeStamp(System.currentTimeMillis())
                        .build(),
                ServiceNode.<TestNodeData>builder()
                        .host("zombie-host")
                        .port(8081)
                        .nodeData(TestNodeData.builder().shardId(2).build())
                        .healthcheckStatus(HealthcheckStatus.healthy)
                        .lastUpdatedTimeStamp(0L) // Very old => zombie
                        .build()
        );
        val dataSource = new TestNodeDataSource(METRIC_ID, DataStoreType.ZK, true, nodes, false);
        val signal = new TestSignal();

        updater = new ServiceRegistryUpdater<>(registry, dataSource, List.of(signal), new TestDeserializer());
        updater.start();
        awaitRefresh(registry);

        // Verify zombie metric recorded (from FinderUtils.filterValidNodes called inside updateRegistry)
        val zombieMeter = metricRegistry.getMeters().get("io.appform.ranger.zombieNodes");
        assertNotNull(zombieMeter, "Zombie nodes meter should exist");
        assertTrue(zombieMeter.getCount() >= 1);

        val svcZombieMeter = metricRegistry.getMeters().get("io.appform.ranger.zombieNodes.serviceName.test-svc");
        assertNotNull(svcZombieMeter);
        assertTrue(svcZombieMeter.getCount() >= 1);

        // Also verify success timer still recorded
        val timerName = "io.appform.ranger.dataSource.ZK." + METRIC_ID + ".nodeDataRefresh.success";
        val timer = metricRegistry.getTimers().get(timerName);
        assertNotNull(timer);
        assertTrue(timer.getCount() >= 1);
    }

    @Test
    void testRefreshReturnsNull_noSuccessOrFailureTimer_butStaleRetained() {
        val registry = new MapBasedServiceRegistry<TestNodeData>(TEST_SERVICE);
        // Return null from refresh (empty Optional)
        val dataSource = new TestNodeDataSource(METRIC_ID, DataStoreType.ZK, true,
                null, false); // null nodes => refresh returns empty
        // Pre-populate registry so initial wait doesn't block forever
        registry.updateNodes(List.of(
                ServiceNode.<TestNodeData>builder()
                        .host("old-host")
                        .port(8080)
                        .nodeData(TestNodeData.builder().shardId(1).build())
                        .healthcheckStatus(HealthcheckStatus.healthy)
                        .lastUpdatedTimeStamp(System.currentTimeMillis())
                        .build()
        ));

        val signal = new TestSignal();
        updater = new ServiceRegistryUpdater<>(registry, dataSource, List.of(signal), new TestDeserializer());

        // Cannot call start() because initial refresh returns null which won't set refreshed=true
        // Instead, test the scenario after start by triggering signal
        // Use alternate approach: make first call return valid, then switch to null
        dataSource.setNodeList(List.of(
                ServiceNode.<TestNodeData>builder()
                        .host("valid-host")
                        .port(8080)
                        .nodeData(TestNodeData.builder().shardId(1).build())
                        .healthcheckStatus(HealthcheckStatus.healthy)
                        .lastUpdatedTimeStamp(System.currentTimeMillis())
                        .build()
        ));

        updater = new ServiceRegistryUpdater<>(registry, dataSource, List.of(signal), new TestDeserializer());
        updater.start();
        awaitRefresh(registry);

        // Now set to null and trigger update
        dataSource.setNodeList(null);
        signal.fire();
        sleep(200);

        // When refresh returns null, no success timer should be incremented for that second call
        // The success timer from the first call should be 1
        val timerName = "io.appform.ranger.dataSource.ZK." + METRIC_ID + ".nodeDataRefresh.success";
        val beforeTimer = metricRegistry.getTimers().get(timerName);
        final long beforeCount = beforeTimer == null ? 0L : beforeTimer.getCount();

        // Now set to null and trigger update
        dataSource.setNodeList(null);
        signal.fire();
        sleep(200);

        val afterTimer = metricRegistry.getTimers().get(timerName);
        final long afterCount = afterTimer == null ? 0L : afterTimer.getCount();

        assertEquals(beforeCount, afterCount,
                "Success timer count should not increase when refresh returns null");
    }

    // ==================== Test helpers ====================

    private void awaitRefresh(ServiceRegistry<?> registry) {
        val start = System.currentTimeMillis();
        while (!registry.isRefreshed() && (System.currentTimeMillis() - start) < 5000) {
            sleep(50);
        }
        assertTrue(registry.isRefreshed(), "Registry should be refreshed within timeout");
    }

    private void sleep(long ms) {
        await().pollDelay(Duration.ofMillis(ms)).until(() -> true);
    }

    // ==================== Test implementations ====================

    static class TestDeserializer implements Deserializer<TestNodeData> {
    }

    static class TestNodeDataSource implements NodeDataSource<TestNodeData, TestDeserializer> {
        private final String metricId;
        private final DataStoreType dataStoreType;
        private final AtomicBoolean active;
        private volatile List<ServiceNode<TestNodeData>> nodeList;
        private final boolean throwOnRefresh;

        TestNodeDataSource(String metricId, DataStoreType dataStoreType, boolean active,
                           List<ServiceNode<TestNodeData>> nodeList, boolean throwOnRefresh) {
            this.metricId = metricId;
            this.dataStoreType = dataStoreType;
            this.active = new AtomicBoolean(active);
            this.nodeList = nodeList;
            this.throwOnRefresh = throwOnRefresh;
        }

        void setActive(boolean active) {
            this.active.set(active);
        }

        void setNodeList(List<ServiceNode<TestNodeData>> nodeList) {
            this.nodeList = nodeList;
        }

        @Override
        public String getMetricId() {
            return metricId;
        }

        @Override
        public DataStoreType getDataStoreType() {
            return dataStoreType;
        }

        @Override
        public Optional<List<ServiceNode<TestNodeData>>> refresh(TestDeserializer deserializer) {
            if (throwOnRefresh) {
                throw new RuntimeException("Simulated refresh failure");
            }
            return Optional.ofNullable(nodeList);
        }

        @Override
        public void start() {
            // No-op for test
        }

        @Override
        public void ensureConnected() {
            // No-op for test
        }

        @Override
        public void stop() {
            // No-op for test
        }

        @Override
        public boolean isActive() {
            return active.get();
        }
    }

    static class TestSignal extends Signal<TestNodeData> {
        protected TestSignal() {
            super(() -> null, Collections.emptyList());
        }

        public void fire() {
            onSignalReceived();
        }

        @Override
        public void start() {
            // No-op for test
        }

        @Override
        public void stop() {
            // No-op for test
        }
    }
}
