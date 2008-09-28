/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.processor.interceptor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.model.InterceptorRef;
import org.apache.camel.model.ProcessorType;
import org.apache.camel.processor.DelegateProcessor;
import org.apache.camel.processor.Logger;
import org.apache.camel.spi.InterceptStrategy;
import org.apache.commons.logging.LogFactory;

/**
 * An interceptor for debugging and tracing routes
 *
 * @version $Revision$
 */
public class TraceInterceptor extends DelegateProcessor implements ExchangeFormatter {
    private final Logger logger = new Logger(LogFactory.getLog(TraceInterceptor.class), this);
    private final ProcessorType node;
    private final Tracer tracer;
    private TraceFormatter formatter;

    public TraceInterceptor(ProcessorType node, Processor target, TraceFormatter formatter, Tracer tracer) {
        super(target);
        this.tracer = tracer;
        this.node = node;
        this.formatter = formatter;

        // set logging level
        if (tracer.getLevel() != null) {
            logger.setLevel(tracer.getLevel());
        }
        if (tracer.getFormatter() != null) {
            this.formatter = tracer.getFormatter();
        }
    }

    /**
     * @deprecated will be removed in Camel 2.0
     */
    public TraceInterceptor(ProcessorType node, Processor target, TraceFormatter formatter) {
        this(node, target, formatter, new Tracer());
    }

    public TraceInterceptor(ProcessorType node, Processor target, Tracer tracer) {
        this(node, target, null, tracer);
    }

    @Override
    public String toString() {
        return "TraceInterceptor[" + node + "]";
    }

    public void process(final Exchange exchange) throws Exception {
        try {
            if (shouldLogNode(node) && shouldLogExchange(exchange)) {
                logExchange(exchange);
            }
            super.proceed(exchange);
        } catch (Exception e) {
            if (shouldLogException(exchange)) {
                logException(exchange, e);
            }
            throw e;
        }
    }

    public Object format(Exchange exchange) {
        return formatter.format(this, exchange);
    }

    // Properties
    //-------------------------------------------------------------------------
    public ProcessorType getNode() {
        return node;
    }

    public Logger getLogger() {
        return logger;
    }

    public TraceFormatter getFormatter() {
        return formatter;
    }

    // Implementation methods
    //-------------------------------------------------------------------------
    protected void logExchange(Exchange exchange) {
        logger.process(exchange);
    }

    protected void logException(Exchange exchange, Throwable throwable) {
        if (tracer.isTraceExceptions()) {
            logger.process(exchange, throwable);
        }
    }

    /**
     * Returns true if the given exchange should be logged in the trace list
     */
    protected boolean shouldLogExchange(Exchange exchange) {
        return (tracer == null || tracer.isEnabled())
            && (tracer.getTraceFilter() == null || tracer.getTraceFilter().matches(exchange));
    }

    /**
     * Returns true if the given exchange should be logged when an exception was thrown
     */
    protected boolean shouldLogException(Exchange exchange) {
        return tracer.isTraceExceptions();
    }


    /**
     * Returns true if the given node should be logged in the trace list
     */
    protected boolean shouldLogNode(ProcessorType node) {
        if (node == null) {
            return false;
        }
        if (!tracer.isTraceInterceptors() && (node instanceof InterceptStrategy || node instanceof InterceptorRef)) {
            return false;
        }
        return true;
    }

}