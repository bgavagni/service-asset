package service.asset;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.util.Collections;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.google.appengine.tools.cloudstorage.GcsFileMetadata;
import com.google.appengine.tools.cloudstorage.GcsFileOptions;
import com.google.appengine.tools.cloudstorage.GcsFilename;
import com.google.appengine.tools.cloudstorage.GcsInputChannel;
import com.google.appengine.tools.cloudstorage.GcsOutputChannel;
import com.google.appengine.tools.cloudstorage.GcsService;
import com.google.appengine.tools.cloudstorage.GcsServiceFactory;
import com.google.appengine.tools.cloudstorage.ListItem;
import com.google.appengine.tools.cloudstorage.ListOptions;
import com.google.appengine.tools.cloudstorage.ListResult;
import com.google.appengine.tools.cloudstorage.RetryParams;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

/**
 * Provides a set of REST APIs that facilitate uploading, storing and retrieving
 * assets.
 * 
 * @author Brett Gavagni
 * 
 *         Credits: GCS API Usage
 *         https://github.com/GoogleCloudPlatform/appengine-gcs-client/blob/master/java/example/src/main/java/com/google/appengine/demos/GcsExampleServlet.java
 * 
 */
@Path("/asset")
public class AssetService {
	/**
	 * Used below to determine the size of chucks to read in. Should be > 1kb and <
	 * 10MB
	 */
	private static final int BUFFER_SIZE = 2 * 1024 * 1024;

	/**
	 * This is where backoff parameters are configured. Here it is aggressively
	 * retrying with backoff, up to 10 times but taking no more that 15 seconds
	 * total to do so.
	 */
	private final GcsService gcsService = GcsServiceFactory.createGcsService(new RetryParams.Builder()
			.initialRetryDelayMillis(10).retryMaxAttempts(10).totalRetryPeriodMillis(15000).build());

	private static final Logger logger = Logger.getLogger(AssetService.class.getName());

	private static final String BUCKET_NAME = "bgavagni-service-asset.appspot.com";

	private static final String ID = "id";
	private static final String LENGTH = "length";
	private static final String FILENAME = "filename";

	private static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";

	/**
	 * Provides a HTTP endpoint to upload assets and returns a response with JSON
	 * content containing a unique identifier for the uploaded file.
	 * 
	 * @param inputStream
	 *            - the asset to upload
	 * @param file
	 *            - the file to store
	 * @return success response with JSON content representing the unique identifier
	 *         of the uploaded asset if file has been stored successfully, else
	 *         error response
	 */
	@POST
	@Path("/upload")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response upload(@FormDataParam("file") InputStream inputStream,
			@FormDataParam("file") FormDataContentDisposition file) {
		// Check for valid params.
		if ((inputStream == null) || (file == null)) {
			// Return 400 response.
			return Response.status(Status.BAD_REQUEST).build();
		}

		// Create a unique identifier for the uploaded asset.
		String id = UUID.randomUUID().toString().replace("-", "");

		try {
			// Add the uploaded asset filename as user metadata to the GCS object.
			GcsFileOptions gcsFileOptions = new GcsFileOptions.Builder().addUserMetadata(FILENAME, file.getFileName())
					.build();
			// Get the GCS file output channel.
			GcsOutputChannel outputChannel = gcsService.createOrReplace(new GcsFilename(BUCKET_NAME, id),
					gcsFileOptions);
			// Copy the uploaded asset to the GCS file.
			copy(inputStream, Channels.newOutputStream(outputChannel));
		} catch (Exception e) {
			logger.log(Level.WARNING, e.getMessage(), e);
			// Return 500 response.
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}

		// Return 200 response with JSON content representing the unique identifier of
		// the uploaded asset.
		return Response.ok(new JSONObject(Collections.singletonMap(ID, id)), MediaType.APPLICATION_JSON).build();
	}

	/**
	 * Provides an HTTP endpoint to download an asset by its unique identifier with
	 * the original filename in the response.
	 * 
	 * @param id
	 *            - the unique identifier of the stored asset
	 * @param resp
	 *            - the response containing the stored asset
	 * @return success response with attachment of the original filename in the
	 *         response, else error response
	 */
	@GET
	@Path("/retrieve")
	public Response retrieveByQueryParam(@QueryParam("id") String id, @Context HttpServletResponse resp) {
		return retrieveByPathParam(id, resp);
	}

	/**
	 * Provides an HTTP endpoint to download an asset by its unique identifier with
	 * the original filename in the response.
	 * 
	 * @param id
	 *            - the unique identifier of the stored asset
	 * @param resp
	 *            - the response containing the stored asset
	 * @return success response with attachment of the original filename in the
	 *         response, else error response
	 */
	@GET
	@Path("/retrieve/{id}")
	public Response retrieveByPathParam(@PathParam(value = "id") String id, @Context HttpServletResponse resp) {
		// Check for valid params.
		if ((id == null) || id.isEmpty()) {
			// Return 400 response.
			return Response.status(Status.BAD_REQUEST).build();
		}

		try {
			// Create the GCS file.
			GcsFilename gcsFilename = new GcsFilename(BUCKET_NAME, id);
			// Get the GCS file metadata.
			GcsFileMetadata gcsFileMetaData = gcsService.getMetadata(gcsFilename);
			// Set the response content length as the GCS file length.
			resp.setContentLength((int) gcsFileMetaData.getLength());
			// Set the response content-disposition to the asset original filename.
			resp.setHeader(HEADER_CONTENT_DISPOSITION,
					String.format("attachment; filename=\"%s\"", getOriginalFilename(gcsFilename)));
			// Get the GCS file for reading.
			GcsInputChannel readChannel = gcsService.openPrefetchingReadChannel(gcsFilename, 0, BUFFER_SIZE);
			// Copy the GCS file to the response.
			copy(Channels.newInputStream(readChannel), resp.getOutputStream());
		} catch (Exception e) {
			logger.log(Level.WARNING, e.getMessage(), e);
			// Return 500 response.
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}

		// Return 200 response.
		return Response.ok().build();
	}

	/**
	 * Provides an HTTP endpoint that returns a response with a JSON array
	 * containing all files in the stored in the system with their unique
	 * identifier, original filename, and the byte size of the file in each entry.
	 * 
	 * @return success response with JSON array containing all files in the stored
	 *         in the system with their unique identifier, original filename, and
	 *         the byte size of the file in each entry, else error response
	 */
	@GET
	@Path("/list")
	@Produces(MediaType.APPLICATION_JSON)
	public Response list() {
		// Create the JSON response content array.
		JSONArray jsonArray = new JSONArray();

		try {
			// Get the list of GCS files.
			ListResult result = gcsService.list(BUCKET_NAME, new ListOptions.Builder().build());
			// Iterate over the list of GCS files and add the JSON representation to the
			// array of each file.
			while (result.hasNext()) {
				// Get the next GCS file.
				ListItem item = result.next();
				// Get the GCS file unique identifer.
				String id = item.getName();
				// Get the original filename.
				String filename = getOriginalFilename(new GcsFilename(BUCKET_NAME, id));
				// Create the JSON representation for the GCS file.
				JSONObject json = new JSONObject();
				json.put(ID, id);
				json.put(FILENAME, filename);
				json.put(LENGTH, item.getLength());
				// Add the JSON to the array.
				jsonArray.put(json);
			}
		} catch (Exception e) {
			logger.log(Level.WARNING, e.getMessage(), e);
			// Return 500 response.
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}

		// Return 200 response with JSON array as content representing the stored
		// assets.
		return Response.ok(jsonArray, MediaType.APPLICATION_JSON).build();
	}

	/**
	 * Transfers the data from the inputStream to the outputStream. Then closes both
	 * streams.
	 * 
	 * @param input
	 *            - the source input stream to copy
	 * @param output
	 *            - the target output stream
	 * @throws IOException
	 *             if an error is encountered
	 */
	private void copy(InputStream input, OutputStream output) throws IOException {
		try {
			byte[] buffer = new byte[BUFFER_SIZE];
			int bytesRead = input.read(buffer);
			while (bytesRead != -1) {
				output.write(buffer, 0, bytesRead);
				bytesRead = input.read(buffer);
			}
		} finally {
			input.close();
			output.close();
		}
	}

	/**
	 * Get the original filename from the GCS file.
	 * 
	 * @param gcsFilename
	 *            - the GCS file
	 * @return original filename from the GCS file.
	 * @throws IOException
	 *             - if an error is encountered
	 */
	private String getOriginalFilename(GcsFilename gcsFilename) throws IOException {
		// Get the GCS file metadata.
		GcsFileOptions gcsFileOptions = gcsService.getMetadata(gcsFilename).getOptions();
		// Return the original filename from the GCS file user metadata.
		return gcsFileOptions.getUserMetadata().get(FILENAME);
	}
}
