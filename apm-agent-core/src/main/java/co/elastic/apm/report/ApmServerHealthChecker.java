/*-
 * #%L
 * Elastic APM Java agent
 * %%
 * Copyright (C) 2018 Elastic and contributors
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package co.elastic.apm.report;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

class ApmServerHealthChecker implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ApmServerHealthChecker.class);
    private final OkHttpClient httpClient;
    private final ReporterConfiguration reporterConfiguration;

    ApmServerHealthChecker(OkHttpClient httpClient, ReporterConfiguration reporterConfiguration) {
        this.httpClient = httpClient;
        this.reporterConfiguration = reporterConfiguration;
    }

    @Override
    public void run() {
        boolean success;
        String message = null;
        try {
            final int status = httpClient.newCall(new Request.Builder()
                .url(reporterConfiguration.getServerUrls().get(0).toString() + "/healthcheck")
                .build())
                .execute()
                .code();
            success = status == 200;
            if (!success) {
                message = Integer.toString(status);
            }
        } catch (IOException e) {
            message = e.getMessage();
            success = false;
        }

        if (success) {
            logger.info("Elastic APM server is available");
        } else {
            logger.warn("Elastic APM server is not available ({})", message);
        }
    }
}
