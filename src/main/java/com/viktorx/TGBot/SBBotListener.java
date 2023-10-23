package com.viktorx.TGBot;

import org.apache.hc.core5.http.impl.DefaultConnectionReuseStrategy;
import org.apache.hc.core5.http.impl.HttpProcessors;
import org.apache.hc.core5.http.impl.bootstrap.HttpServer;
import org.apache.hc.core5.http.impl.io.HttpService;
import org.apache.hc.core5.http.io.support.BasicHttpServerExpectationDecorator;

public class SBBotListener {
    HttpServer server;
    public static final SBBotListener INSTANCE = new SBBotListener();

    private SBBotListener() {
        HttpService httpService = new HttpService(
                httpProcessor != null ? httpProcessor : HttpProcessors.server(),
                handlerDecorator != null ? handlerDecorator.decorate(handler) : new BasicHttpServerExpectationDecorator(handler),
                DefaultConnectionReuseStrategy.INSTANCE,
                LoggingHttp1StreamListener.INSTANCE);
        server = new HttpServer(26565, httpService, null, )
    }
}
