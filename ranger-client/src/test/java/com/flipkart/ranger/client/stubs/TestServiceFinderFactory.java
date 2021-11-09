/*
 * Copyright 2015 Flipkart Internet Pvt. Ltd.
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
package com.flipkart.ranger.client.stubs;

import com.flipkart.ranger.core.finder.ServiceFinder;
import com.flipkart.ranger.core.finder.SimpleUnshardedServiceFinder;
import com.flipkart.ranger.core.finder.serviceregistry.ListBasedServiceRegistry;
import com.flipkart.ranger.core.finderhub.ServiceFinderFactory;
import com.flipkart.ranger.core.model.Criteria;
import com.flipkart.ranger.core.model.Deserializer;
import com.flipkart.ranger.core.model.Service;

public class TestServiceFinderFactory  implements ServiceFinderFactory<TestShardInfo, Criteria<TestShardInfo>, ListBasedServiceRegistry<TestShardInfo>> {

    @Override
    public ServiceFinder<TestShardInfo, Criteria<TestShardInfo>, ListBasedServiceRegistry<TestShardInfo>> buildFinder(Service service) {
        SimpleUnshardedServiceFinder<TestShardInfo, Criteria<TestShardInfo>> finder = new TestSimpleUnshardedServiceFinder<TestShardInfo, Criteria<TestShardInfo>>()
                .withNamespace(service.getNamespace())
                .withServiceName(service.getServiceName())
                .withDeserializer(new Deserializer<TestShardInfo>() {
                    @Override
                    public int hashCode() {
                        return super.hashCode();
                    }
                })
                .build();
        finder.start();
        return finder;
    }
}

