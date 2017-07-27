package eu.arrowhead.ArrowheadProvider;

import java.security.Principal;
import java.security.Signature;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import com.google.gson.Gson;

import eu.arrowhead.ArrowheadProvider.common.model.ArrowheadSystem;
import eu.arrowhead.ArrowheadProvider.common.model.ErrorMessage;
import eu.arrowhead.ArrowheadProvider.common.model.RawTokenInfo;

@Path("temperature")
public class TemperatureResource {

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public Response getIt(@Context SecurityContext context, @QueryParam("token") String token,
			@QueryParam("signature") String signature) {
		String temperature = "21";
		if (context.isSecure()) {
			try {
				Principal consumerPrincipal = context.getUserPrincipal();
				String consumerName = consumerPrincipal.getName().substring(3,
						consumerPrincipal.getName().indexOf(" ") - 1);

				ArrowheadSystem consumer = new ArrowheadSystem();
				String[] consumerNameParts = consumerName.split("\\.");
				consumer.setSystemName(consumerNameParts[0]);
				consumer.setSystemGroup(consumerNameParts[1]);

				byte[] tokenbytes = Base64.getDecoder().decode(token);
				byte[] signaturebytes = Base64.getDecoder().decode(signature);

				Signature signatureInstance = Signature.getInstance("SHA1withRSA");
				signatureInstance.initVerify(ProviderMain.authorizationKey);
				signatureInstance.update(tokenbytes);

				boolean verifies = signatureInstance.verify(signaturebytes);

				if (!verifies) {
					ErrorMessage error = new ErrorMessage("Token validation failed", 401);
					return Response.status(401).entity(error).build();
				}

				Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
				cipher.init(Cipher.DECRYPT_MODE, ProviderMain.privateKey);
				byte[] byteToken = cipher.doFinal(tokenbytes);

				Gson gson = new Gson();
				String json = new String(byteToken, "UTF-8");
				RawTokenInfo rawTokenInfo = gson.fromJson(json, RawTokenInfo.class);

				ArrowheadSystem consumerWithToken = new ArrowheadSystem();
				String[] rawTokenInfoParts = rawTokenInfo.getC().split("\\.");
				consumerWithToken.setSystemName(rawTokenInfoParts[0]);
				consumerWithToken.setSystemGroup(rawTokenInfoParts[1]);

				long endTime = rawTokenInfo.getE();
				long currentTime = System.currentTimeMillis();

				if (consumer.equals(consumerWithToken)) {
					if (endTime == 0L || (endTime > currentTime)) {
						return Response.status(200).entity(temperature).build();
					}
					ErrorMessage error = new ErrorMessage("Authorization token has expired", 401);
					return Response.status(401).entity(error).build();
					
				} else {
					ErrorMessage error = new ErrorMessage("Permission denied", 401);
					return Response.status(401).entity(error).build();
				}
				
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		return Response.status(200).entity(temperature).build();
	}
}
