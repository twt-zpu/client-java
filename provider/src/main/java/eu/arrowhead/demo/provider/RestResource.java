package eu.arrowhead.demo.provider;

import eu.arrowhead.common.api.server.ArrowheadHttpServer;
import eu.arrowhead.common.api.server.ArrowheadResource;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.DataNotFoundException;
import eu.arrowhead.demo.model.Car;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class demonstrates the most commonly used capabilities of a JAX-RS REST library, specifically the Jersey implementation in this case.
 * You can also implement your clients in different JAX-RS implementations, like Spring for example.
 */
@Path("rest")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RestResource extends ArrowheadResource {

  private static Integer idCounter = 0;
  private static final ConcurrentHashMap<Integer, Car> cars = new ConcurrentHashMap<>();

  public RestResource(ArrowheadHttpServer server) throws ArrowheadException {
    super(server);
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  public String simpleGetRequest() {
    return "This is an example REST resource!";
  }

  /**
   * GET requests are usually used to get a resource from the database, often identified by a unique ID.
   * Example URL: http://<server_address>:<server_port>/example/cars/5
   * @param context
   * @param token
   * @param signature
   * @param id
   * @return
   */
  @GET
  @Path("cars/{id}")
  public Response getCarById(@Context SecurityContext context,
                                 @QueryParam("token") String token,
                                 @QueryParam("signature") String signature,
                                 @PathParam("id") Integer id) {
    return verifier.verifiedResponse(context, token, signature, () -> {
      Car retrievedCar = cars.get(id);
      if (retrievedCar != null) {
        return Response.status(Status.OK).entity(retrievedCar).build();
      } else {
        return Response.status(Status.OK).build();
      }
    });
  }

  /**
   * Get all cars, optional filter parameters are the brand and color of the cars.
   * QueryParameters are optional in Jersey, for example brand will be null if not specified.
   * Example URL: http://<server_address>:<server_port>/example/cars?brand=Volvo&color=red
   * @param context
   * @param token
   * @param signature
   * @param brand
   * @param color
   * @return
   */
  @GET
  @Path("cars")
  public Response getCars(@Context SecurityContext context,
                          @QueryParam("token") String token,
                          @QueryParam("signature") String signature,
                          @QueryParam("brand") String brand,
                          @QueryParam("color") String color) {
    return verifier.verifiedResponse(context, token, signature, () -> {
      List<Car> returnedCars = new ArrayList<>();
      for (Map.Entry<Integer, Car> mapEntry : cars.entrySet()) {
        returnedCars.add(mapEntry.getValue());
      }

      if (brand != null) {
        returnedCars.removeIf(car -> !brand.equals(car.getBrand()));
      }

      if (color != null) {
        returnedCars.removeIf(car -> !color.equals(car.getColor()));
      }

      return Response.status(Status.OK).entity(returnedCars).build();
    });
  }

  /**
   * Return the complete Map with IDs included
   * @param context
   * @param token
   * @param signature
   * @return
   */
  @GET
  @Path("raw")
  public Response getAll(@Context SecurityContext context,
                         @QueryParam("token") String token,
                         @QueryParam("signature") String signature) {
    return verifier.verifiedResponse(context, token, signature, () -> Response.status(Status.OK).entity(cars).build());
  }

  /**
   * POST requests are usually for creating/saving new resources. The resource is in the payload of the request, which will be
   * automatically be deserialized by the JSON library (if the project is configured correctly).
   * @param context
   * @param token
   * @param signature
   * @param car
   * @return
   */
  @POST
  @Path("cars")
  public Response createCar(@Context SecurityContext context,
                            @QueryParam("token") String token,
                            @QueryParam("signature") String signature,
                            Car car) {
    return verifier.verifiedResponse(context, token, signature, () -> {
      cars.put(idCounter, car);
      idCounter++;
      return Response.status(Status.CREATED).entity(car).build();
    });
  }

  /**
   * PUT requests are usually for updating existing resources. The ID is from the database, to identify the car instance.
   * Usually PUT requests fully update a resource, meaning fields which are not specified by the client, will also be null
   * in the database (overriding existing data). PATCH requests are used for partial updates.
   * @param context
   * @param token
   * @param signature
   * @param id
   * @param updatedCar
   * @return
   */
  @PUT
  @Path("cars/{id}")
  public Response updateCar(@Context SecurityContext context,
                            @QueryParam("token") String token,
                            @QueryParam("signature") String signature,
                            @PathParam("id") Integer id,
                            Car updatedCar) {
    return verifier.verifiedResponse(context, token, signature, () -> {
      Car carFromTheDatabase = cars.get(id);
      if (carFromTheDatabase != null) {
        throw new DataNotFoundException("Car with id " + id + " not found in the database!");
      }
      cars.put(id, updatedCar);
      return Response.status(Status.ACCEPTED).entity(updatedCar).build();
    });
  }

  /**
   * And finally, DELETE requests are usually used to delete a resource from database.
   * @param context
   * @param token
   * @param signature
   * @param id
   * @return
   */
  @DELETE
  @Path("cars/{id}")
  public Response deleteCar(@Context SecurityContext context,
                            @QueryParam("token") String token,
                            @QueryParam("signature") String signature,
                            @PathParam("id") Integer id) {
    return verifier.verifiedResponse(context, token, signature, () -> {
      cars.remove(id);
      return Response.ok().build();
    });
  }

}
