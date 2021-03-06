/*
 * Copyright 2018 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.plugin.mongo.interceptor;

import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.navercorp.pinpoint.bootstrap.context.DatabaseInfo;
import com.navercorp.pinpoint.bootstrap.context.MethodDescriptor;
import com.navercorp.pinpoint.bootstrap.context.SpanEventRecorder;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.interceptor.SpanEventSimpleAroundInterceptorForPlugin;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.bootstrap.plugin.jdbc.DatabaseInfoAccessor;
import com.navercorp.pinpoint.bootstrap.plugin.jdbc.MongoDatabaseInfo;
import com.navercorp.pinpoint.bootstrap.plugin.jdbc.UnKnownDatabaseInfo;
import com.navercorp.pinpoint.bootstrap.util.InterceptorUtils;
import com.navercorp.pinpoint.common.plugin.util.HostAndPort;
import com.navercorp.pinpoint.plugin.mongo.MongoConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.navercorp.pinpoint.plugin.mongo.interceptor.MongoWriteConcernInterceptor.getWriteConcern0;

/**
 * @author Roy Kim
 */
public class MongoDriverConnectInterceptor3_7 extends SpanEventSimpleAroundInterceptorForPlugin {

    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();

    public MongoDriverConnectInterceptor3_7(TraceContext traceContext, MethodDescriptor descriptor) {
        super(traceContext, descriptor);
    }

    @Override
    protected void doInBeforeTrace(SpanEventRecorder recorder, Object target, Object[] args) {
    }

    @Override
    protected void prepareAfterTrace(Object target, Object[] args, Object result, Throwable throwable) {
    }

    @Override
    protected void doInAfterTrace(SpanEventRecorder recorder, Object target, Object[] args, Object result,
            Throwable throwable) {

        final boolean success = InterceptorUtils.isSuccess(throwable);

        if (success) {
            // Must not check if current transaction is trace target or not. Connection can be made by other thread.
            final List<String> hostList = getHostList(args[0]);
            String readPreference = getReadPreference(args[0]);
            String writeConcern = getWriteConcern(args[0]);

            if (args == null) {
                return;
            }

            DatabaseInfo databaseInfo = createDatabaseInfo(hostList, readPreference, writeConcern);

            if (result instanceof DatabaseInfoAccessor) {
                ((DatabaseInfoAccessor) result)._$PINPOINT$_setDatabaseInfo(databaseInfo);
            }


            if (databaseInfo == null) {
                databaseInfo = UnKnownDatabaseInfo.INSTANCE;
            }

            recorder.recordServiceType(databaseInfo.getType());
            recorder.recordEndPoint(databaseInfo.getMultipleHost());
            recorder.recordDestinationId(databaseInfo.getDatabaseId());
        }

        recorder.recordApi(methodDescriptor);
        recorder.recordException(throwable);
    }

    private DatabaseInfo createDatabaseInfo(List<String> hostList, String readPreference, String writeConcern) {

        DatabaseInfo databaseInfo = new MongoDatabaseInfo(MongoConstants.MONGO, MongoConstants.MONGO_EXECUTE_QUERY,
                null, null, hostList, null, null, readPreference, writeConcern);

        if (isDebug) {
            logger.debug("parse DatabaseInfo:{}", databaseInfo);
        }

        return databaseInfo;
    }

    private List<String> getHostList(Object arg) {
        if (!(arg instanceof MongoClientSettings)) {
            return Collections.emptyList();
        }

        final MongoClientSettings mongoClientSettings = (MongoClientSettings) arg;

        List<ServerAddress> lists = mongoClientSettings.getClusterSettings().getHosts();

        final List<String> hostList = new ArrayList<String>();
        for(ServerAddress sa : lists) {
            final String hostAddress = HostAndPort.toHostAndPortString(sa.getHost(), sa.getPort());
            hostList.add(hostAddress);
        }

        return hostList;
    }

    private String getReadPreference(Object arg) {
        if (!(arg instanceof MongoClientSettings)) {
            return null;
        }

        final MongoClientSettings mongoClientSettings = (MongoClientSettings) arg;

        return mongoClientSettings.getReadPreference().getName();
    }
    private String getWriteConcern(Object arg) {
        if (!(arg instanceof MongoClientSettings)) {
            return null;
        }

        final MongoClientSettings mongoClientSettings = (MongoClientSettings) arg;

        return getWriteConcern0(mongoClientSettings.getWriteConcern());
    }
}
