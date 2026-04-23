package com.mycompany.tawfiq.resources;

import com.mycompany.tawfiq.exceptions.LinkedResourceNotFoundException;
import com.mycompany.tawfiq.exceptions.SensorUnavailableException;
import com.mycompany.tawfiq.model.Sensor;
import com.mycompany.tawfiq.model.SensorReading;
import com.mycompany.tawfiq.storage.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;
    private final DataStore store = DataStore.getInstance();

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    @GET
    public Response getReadings() {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) throw new LinkedResourceNotFoundException("Sensor not found: " + sensorId);
        List<SensorReading> list = store.getReadings().getOrDefault(sensorId, List.of());
        return Response.ok(list).build();
    }

    @POST
    public Response addReading(SensorReading reading) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) throw new LinkedResourceNotFoundException("Sensor not found: " + sensorId);
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(
                    "Sensor " + sensorId + " is under MAINTENANCE and cannot accept readings.");
        }

        if (reading.getId() == null || reading.getId().isBlank()) {
            reading.setId(UUID.randomUUID().toString());
        }
        if (reading.getTimestamp() == 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        store.getReadings().get(sensorId).add(reading);

        // Side effect: update sensor's currentValue
        sensor.setCurrentValue(reading.getValue());

        return Response.status(Response.Status.CREATED).entity(reading).build();
    }
}