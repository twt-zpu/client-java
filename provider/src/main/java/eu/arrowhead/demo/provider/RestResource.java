package eu.arrowhead.demo.provider;

import eu.arrowhead.common.api.server.ArrowheadHttpServer;
import eu.arrowhead.common.api.server.ArrowheadResource;
import eu.arrowhead.demo.model.Car;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

@Path("rest")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RestResource extends ArrowheadResource {

  public RestResource(ArrowheadHttpServer server) {
    super(server);
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  public String simpleGetRequest() {
    return "This is a REST resource!";
  }

  @GET
  @Path("cars/{brand}")
  public Response getCarResource(@Context SecurityContext context,
                                 @QueryParam("token") String token,
                                 @QueryParam("signature") String signature,
                                 @PathParam("brand") String brand,
                                 @QueryParam("color") String color) {
    return verifier.verifiedResponse(context, token, signature, () -> {
      Car car = new Car(brand, color);
      return Response.status(Status.OK).entity(car).build();
    });
  }

  @POST
  @Path("cars")
  public Response createCar(@Context SecurityContext context,
                            @QueryParam("token") String token,
                            @QueryParam("signature") String signature,
                            Car car) {
    return verifier.verifiedResponse(context, token, signature, () -> Response.status(Status.CREATED).entity(car).build());
  }

  @PUT
  @Path("cars/{id}")
  public Response updateCar(@Context SecurityContext context,
                            @QueryParam("token") String token,
                            @QueryParam("signature") String signature,
                            @PathParam("id") Long id,
                            Car car) {
    return verifier.verifiedResponse(context, token, signature, () -> {
      Car carFromDB = new Car("Toyota", "White");
      carFromDB.setBrand(carFromDB.getBrand());
      carFromDB.setColor(carFromDB.getColor());
      return Response.status(Status.ACCEPTED).entity(carFromDB).build();
    });
  }

  @DELETE
  @Path("cars/{id}")
  public Response deleteCar(@Context SecurityContext context,
                            @QueryParam("token") String token,
                            @QueryParam("signature") String signature,
                            @PathParam("id") Long id) {
    return verifier.verifiedResponse(context, token, signature, () -> Response.ok().build());
  }

}
