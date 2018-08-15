package eu.arrowhead.client.provider;

import eu.arrowhead.client.common.model.Car;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/*
   This class demonstrates the most commonly used capabilities of a JAX-RS REST library, specifically the Jersey implementation in this case.
   You can also implement your clients in different JAX-RS implementations, like Spring for example.
 */

@Path("example") //base path after the port
//Every REST method will consume and produce JSON payloads (not plain text, or XML for example)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RestResource {

  @GET
  @Produces(MediaType.TEXT_PLAIN) //individual methods can override class level annotations
  public String simpleGetRequest() {
    return "This is a REST resource!";
  }

  /*
    GET requests are usually used to get a resource from the database, often identified by a unique ID.
    QueryParameters are optional in Jersey, the color variable will be null if the client did not specify it.
    Full url: http://<server_address>:<server_port>/example/cars/BMW?color=blue
   */
  @GET
  @Path("cars/{brand}")
  public Response getCarResource(@PathParam("brand") String brand, @QueryParam("color") String color) {
    //Get a Car with a certain brand and color from the database, and return it to the client

    Car car = new Car(brand, color);
    //Response contains the status code, and the response entity
    return Response.status(Status.OK).entity(car).build();
  }

  //GetAllCars method

  /*
     POST requests are usually for creating/saving new resources. The resource is in the payload of the request, which will be
     automatically be deserialized by the JSON library (if the project is configured correctly).
   */
  @POST
  @Path("cars")
  public Response createCar(Car car) {
    //Save the car instance to the database, and return the saved instance to the client
    return Response.status(Status.CREATED).entity(car).build();
  }

  /*
     PUT requests are usually for updating existing resources. The ID is from the database, to identify the car instance.
     Usually PUT requests fully update a resource, meaning fields which are not specified by the client, will also be null
     in the database (overriding existing data). PATCH requests are used for partial updates.
   */
  @PUT
  @Path("cars/{id}")
  public Response updateCar(@PathParam("id") Long id, Car car) {
    Car carFromTheDatabase = new Car("Toyota", "White");
    carFromTheDatabase.setBrand(car.getBrand());
    carFromTheDatabase.setColor(car.getColor());

    //Save the modified resource, and return it to the user
    return Response.status(Status.ACCEPTED).entity(carFromTheDatabase).build();
  }

  /*
     And finally, DELETE requests are usually used to delete a resource from database.
   */
  @DELETE
  @Path("cars/{id}")
  public Response deleteCar(@PathParam("id") Long id) {
    //Get the car identified by the ID from the database, and delete it, respond with HTTP_OK if successful
    return Response.ok().build();
  }

}
