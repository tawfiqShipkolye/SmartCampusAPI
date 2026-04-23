package com.mycompany.tawfiq;

import com.mycompany.tawfiq.filters.LoggingFilter;
import com.mycompany.tawfiq.mappers.GenericThrowableMapper;
import com.mycompany.tawfiq.mappers.LinkedResourceNotFoundMapper;
import com.mycompany.tawfiq.mappers.RoomNotEmptyMapper;
import com.mycompany.tawfiq.mappers.SensorUnavailableMapper;
import com.mycompany.tawfiq.resources.DiscoveryResource;
import com.mycompany.tawfiq.resources.SensorRoomResource;
import com.mycompany.tawfiq.resources.SensorResource;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.net.URI;

public class Main {

    public static final String BASE_URI = "http://localhost:8080/api/v1/";

    public static void main(String[] args) throws Exception {
        ResourceConfig config = new ResourceConfig();

        config.register(DiscoveryResource.class);
        config.register(SensorRoomResource.class);
        config.register(SensorResource.class);
        config.register(RoomNotEmptyMapper.class);
        config.register(LinkedResourceNotFoundMapper.class);
        config.register(SensorUnavailableMapper.class);
        config.register(GenericThrowableMapper.class);
        config.register(LoggingFilter.class);

        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(
                URI.create(BASE_URI), config, false);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Stopping server...");
            server.shutdownNow();
        }));

        server.start();

        System.out.println("Smart Campus API started at: http://localhost:8080/api/v1/");
        System.out.println("Press ENTER to stop the server...");

        System.in.read();
        server.shutdownNow();
    }
}