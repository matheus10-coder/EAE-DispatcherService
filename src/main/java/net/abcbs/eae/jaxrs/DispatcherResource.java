package net.abcbs.eae.jaxrs;

import java.io.File;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import net.abcbs.eae.dispatcher.DispatcherDTO;
import net.abcbs.eae.dispatcher.diag.DiagReviewDispatcher;
import net.abcbs.eae.dispatcher.retroterms.RetroTermsDispatcher;
import net.abcbs.eae.orchestratorapi.OrchestratorCalls;
import net.abcbs.eae.orchestratorapi.OrchestratorDTO;
import net.abcbs.eae.queryprocessing.QueryProcessing;
import net.abcbs.eae.utils.Constants;
import net.abcbs.eae.utils.RetrieveIsSharedProperties;
import net.abcbs.issh.util.pub.common.IsSharedApplicationDataObject;

@Path("/Dispatcher")
@OpenAPIDefinition(
        servers = {
                @Server(
						description = "localhost",
						url = "localhost:9080/RPADispatcherService/resources"),
				@Server(
						description = "development",
						url = "https://isshareddev.abcbs.net/RPADispatcherService/resources"),
				@Server(
						description = "stage",
						url = "https://issharedstg.abcbs.net/RPADispatcherService/resources"),
				@Server(
						description = "production",
						url = "https://isshared.abcbs.net/RPADispatcherService/resources")
})
@SecurityScheme(name = "basicAuth",
type = SecuritySchemeType.HTTP,
scheme = "basic",
description = "Username and Password are used for authorization")

public class DispatcherResource {
	// Data object to get database information
	private static IsSharedApplicationDataObject isSharedApplicationDataObject = null;
	// Instantiate logger
	private static final Logger logger = LogManager.getLogger(DispatcherResource.class);
	private static String jndi = "db2NodeDB";
    private static String schema = "BCBSDB26";
	
	static {
		try {
			logger.info("Starting RPADispatcherService");
			isSharedApplicationDataObject = new IsSharedApplicationDataObject(Constants.SYSTEM_NAME, Constants.AUTH_KEY, Constants.AUTH_PASS_PHRASE_DEV);
		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}
	}

	// Currently, this will just output the base endpoint
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "Base endpoint of the web service",
	security = @SecurityRequirement(name = "basicAuth"),
	description = "The base endpoint of the web service, all other endpoints are built off of it",
	responses = {
			@ApiResponse(
					description = "Base endpoint message",
					content = @Content(mediaType = "application/json",
					schema = @Schema(implementation = JsonPayload.class))),
			@ApiResponse(responseCode = "401", description = "Credentials not authorized") }
			)
	public JsonPayload getDispatcherBaseEndpoint() {
		logger.info("RPADispatcherService base endpoint");
		JsonPayload payload = new JsonPayload();
		payload.setMessage("RPADispatcherService base endpoint");
		return payload;
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "Start a dispatcher process",
    security = @SecurityRequirement(name = "basicAuth"),
    description = "Run a dispatcher process for an RPA solution that has been coded into this webservice",
    responses = {
            @ApiResponse(
                    description = "Returns a JSON response with a message and parameters",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = JsonPayload.class))),
            @ApiResponse(responseCode = "401", description = "Credentials not authorized") }
            )
	public JsonPayload runDispatcher(DispatcherDTO dispatcher) {
	    JsonPayload payload = new JsonPayload();
	    
	    if (StringUtils.isBlank(dispatcher.getProcess()) && StringUtils.isBlank(dispatcher.getAdditionalReference())) {
	        payload.setMessage("Required data is blank. Please check these properties: process, additionalReference");
            return payload;
	    }
	    
	    if (dispatcher.getProcess().equalsIgnoreCase("retroterms")) {
	        RetroTermsDispatcher retro = new RetroTermsDispatcher();
	        payload.setMessage(retro.run(dispatcher.getLimit(), dispatcher.getAdditionalReference()));
	    } else if (dispatcher.getProcess().equalsIgnoreCase("diag")) {
	        DiagReviewDispatcher diag = new DiagReviewDispatcher();
	        payload.setMessage(diag.run(dispatcher.getLimit(),dispatcher.getAdditionalReference()));
	    }
	    
	    return payload;
	}

	// GET request with path parameter, to execute specified query
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/{query_id}/{query_subid}")
	@Operation(summary = "Run a query",
	security = @SecurityRequirement(name = "basicAuth"),
	description = "Runs a query with the provided identifiers",
	responses = {
			@ApiResponse(
					description = "Returns a JSON response with a message and parameters",
					content = @Content(mediaType = "application/json",
					schema = @Schema(implementation = JsonPayload.class))),
			@ApiResponse(responseCode = "401", description = "Credentials not authorized") }
			)
	public JsonPayload executeQueryByID(
			@Parameter(description = "An identifier that represents the line of business", required = true)
			@PathParam("query_id") String queryId, 
			@Parameter(description = "A sub identifier that is meant to be specific to the line of business", required = true)
			@PathParam("query_subid") String querySubid, 
			@Parameter(description = "An optional identifier used for queries that require a specific day", required = false)
			@DefaultValue("") @QueryParam("day") String day) {
		logger.info("Endpoint path hit: {}/{}", queryId, querySubid);
		JsonPayload payload = new JsonPayload();
		try {
			QueryProcessing queryProcessing = new QueryProcessing();
			String returnMessage = "";
			// Check if MAPPO query is being requested
			if (queryId.equals("MAPPO")) {
				if (querySubid.equals("Prof")) {
					if (!day.isEmpty()) {
						returnMessage = queryProcessing.executeQuery(isSharedApplicationDataObject.getDb2JndiName(), isSharedApplicationDataObject.getDb2Schema(), "Prof/Monday");
						payload.setDay(day);
					}
					else {
						returnMessage = queryProcessing.executeQuery(isSharedApplicationDataObject.getDb2JndiName(), isSharedApplicationDataObject.getDb2Schema(), "Prof/Other");
					}
				}
				else if (querySubid.equals("Fac")) {
					if (!day.isEmpty()) {
						returnMessage = queryProcessing.executeQuery(isSharedApplicationDataObject.getDb2JndiName(), isSharedApplicationDataObject.getDb2Schema(), "Fac/Monday");
						payload.setDay(day);
					}
					else {
						returnMessage = queryProcessing.executeQuery(isSharedApplicationDataObject.getDb2JndiName(), isSharedApplicationDataObject.getDb2Schema(), "Fac/Other");
					}
				}
			}

			// if BAA Claims 
			else if (queryId.equals("BAA")) {
				//Diag Query is requested
				if (querySubid.equals("Diag")) {
					returnMessage = queryProcessing.executeQuery(isSharedApplicationDataObject.getOracleBaJndiName(), isSharedApplicationDataObject.getDb2Schema(), "BAA/Diag");
				}
				// Check if HVA query is being requested
				else if (querySubid.equals("HVA")) {
					returnMessage = queryProcessing.executeQuery(isSharedApplicationDataObject.getDb2JndiName(), isSharedApplicationDataObject.getDb2Schema(), "HVA/Modern");
					//returnMessage = queryProcessing.executeQuery(jndi, schema, "HVA/Modern");
				}
				// check if M2 query is requested
				else if (querySubid.equals("M2")) {
                    returnMessage = queryProcessing.executeQuery(isSharedApplicationDataObject.getOracleBaJndiName(), isSharedApplicationDataObject.getDb2Schema(), "BAA/M2");
				}
				// check if Covid query is requested
                else if (querySubid.equals("Covid")) {
                    returnMessage = queryProcessing.executeQuery(isSharedApplicationDataObject.getOracleBaJndiName(), isSharedApplicationDataObject.getDb2Schema(), "BAA/Covid");
                }
			}

			payload.setMessage(returnMessage);
			payload.setQueryID(queryId);
			payload.setQuerySubID(querySubid);

		}
		catch (Exception e) {
			String stackTrace = ExceptionUtils.getStackTrace(e);
			payload.setMessage("Exception: " + stackTrace);
		}
		return payload;
	}

	// Method to test if file directory is accessible 
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/test/file-location") 
	@Operation(summary = "Test file location paths",
	security = @SecurityRequirement(name = "basicAuth"),
	description = "Test if the file location that the CSV files are dropped into still has read/write permissions, and can also check if files are in the directory",
	responses = {
			@ApiResponse(
					description = "Returns a JSON response showing the file directory path, if there are read/write permissions, as well as any files that are in the directory",
					content = @Content(mediaType = "application/json",
					schema = @Schema(implementation = JsonPayload.class))),
			@ApiResponse(responseCode = "401", description = "Credentials not authorized") }
			)
	public JsonPayload testFileDirectory() {
		logger.info("File location test input hit");
		RetrieveIsSharedProperties filePath = new RetrieveIsSharedProperties();
		final String DIR_FOUND = "directory found: ";
		String dirTest = "directory not found";
		String endpoint = filePath.buildTargetPath();
		File fileRoot = null;
		File fileSubroot = null;
		String read = "false";
		String write = "false";
		JsonPayload payload = new JsonPayload();
		File file = new File(endpoint);
		String[] paths = endpoint.split("/"); 
		fileRoot = new File(paths[1]);
		fileSubroot = new File(paths[2]);

		// Test if different locations can be found
		if (file.exists()) {
			dirTest = DIR_FOUND + endpoint;
			if (file.canRead()) {
				read = "true";
				String[] files = file.list();
				payload.setFiles(files);
			}	
			if (file.canWrite()) {
				write = "true";
			}
		} else if (fileSubroot.exists()) {
			dirTest = DIR_FOUND + fileSubroot.toString();
			if (fileSubroot.canRead()) {
				read = "true";
				String[] files = fileSubroot.list();
				payload.setFiles(files);
			}
			if (fileSubroot.canWrite()) {
				write = "true";
			}
		} else if (fileRoot.exists()) {
			dirTest = DIR_FOUND + fileRoot.toString();
			if (fileRoot.canRead()) {
				read = "true";
				String[] files = fileRoot.list();
				payload.setFiles(files);
			}
			if (fileRoot.canWrite()) {
				write = "true";
			}
		}

		payload.setMessage(dirTest);
		payload.setRead(read);
		payload.setWrite(write);

		return payload;
	}
	
	// make web service calls to this service to add items to Orchestrator queue
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "load items in UiPath Orchestrator queue",
		security = @SecurityRequirement(name = "basicAuth"),
		description = "Creates queue items in UiPath Orchestrator",
		responses = {
				@ApiResponse(description = "message response", content = @Content(mediaType = "application/json")),
				@ApiResponse(responseCode = "401", description = "Credentials not authorized") }
	)
	@Path("/orchestrator")
	public JsonPayload loadOrchestrator(OrchestratorDTO orchestrator) {
		logger.info("loadOrchestrator endpoint hit. Add items to Orchestrator queue directly");
		JsonPayload payload = new JsonPayload();

		// check to see if JSON consumed has necessary properties
		if (StringUtils.isBlank(orchestrator.getFolder()) || StringUtils.isBlank(orchestrator.getTenant())
				|| StringUtils.isBlank(orchestrator.getQueueName()) || orchestrator.getContent().isEmpty()) {
			payload.setMessage("Required data is blank. Please check these properties: queueName, tenant, folder, content");
			return payload;
		} else {
			OrchestratorCalls orchApi = new OrchestratorCalls(orchestrator.getTenant());
			payload.setMessage(orchApi.addDataToQueue(orchestrator));
			orchApi.closeClient();
			
			return payload;
		}
	}
}
