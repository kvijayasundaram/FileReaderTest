package com.inforesources.filetest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.inforesources.filetest.objects.TestRecord;

public class FileReader implements RequestHandler<S3Event, String> {

	private AmazonS3 s3 = AmazonS3ClientBuilder.standard().build();

	/*
	 * public Connection connect(String url, String user, String password,
	 * Context context) { Connection conn = null; try { conn =
	 * DriverManager.getConnection(url, user, password); context.getLogger()
	 * .log("Connected to the PostgreSQL server successfully."); } catch
	 * (SQLException e) { System.out.println(e.getMessage());
	 * context.getLogger() .log("Error connecting to PostgreSQL" +
	 * e.getMessage()); }
	 * 
	 * return conn; }
	 */

	public DataSource pgDataSource(String url, String user, String password,
			Context context) {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("org.postgresql.Driver");
		dataSource.setUrl(url);
		dataSource.setUsername(user);
		dataSource.setPassword(password);
		context.getLogger().log(">>returning the Datasource");
		return dataSource;
	}

	public FileReader() {
	}

	// Test purpose only.
	FileReader(AmazonS3 s3) {
		this.s3 = s3;
	}

	@Override
	public String handleRequest(S3Event event, Context context) {
		context.getLogger().log("Received event: " + event);

		// Get the object from the event and show its content type
		String bucket = event.getRecords().get(0).getS3().getBucket().getName();
		String key = event.getRecords().get(0).getS3().getObject().getKey();

		String url, user, password;
		url = System.getenv("DB_URL");
		user = System.getenv("DB_USER");
		password = System.getenv("DB_PASSWORD");
		System.getenv("DB_NAME");

		context.getLogger().log("DB_URL: " + url);

		SimpleJdbcInsert jdbcInsert = new SimpleJdbcInsert(
				pgDataSource(url, user, password, context))
						.withTableName("test");

		try {
			S3Object response = s3.getObject(new GetObjectRequest(bucket, key));
			String contentType = response.getObjectMetadata().getContentType();
			context.getLogger().log("CONTENT TYPE: " + contentType);

			BufferedReader br = new BufferedReader(
					new InputStreamReader(response.getObjectContent()));

			/*
			 * Connection conn = connect(url, user, password, context); if (conn
			 * != null) { statement stmt = conn.prepareStatement(
			 * "insert into test (id, name) values(?, ?)"); br.lines()
			 * .forEach(line -> context.getLogger().log(">> " + line)); } else {
			 * context.getLogger().log(">>unable to get connection"); }
			 */

			if (jdbcInsert != null) {
				br.lines().forEach(line -> {
					context.getLogger().log(">> " + line);
					TestRecord tr = TestRecord.parse(line, ',');
					Map<String, Object> parameters = new HashMap<String, Object>();
					parameters.put("id", tr.getId());
					parameters.put("name", tr.getName());
					jdbcInsert.execute(parameters);
				});
			}

		} catch (Exception e) {
			e.printStackTrace();
			context.getLogger().log(String.format(
					"Error getting object %s from bucket %s. Make sure they exist and"
							+ " your bucket is in the same region as this function.",
					key, bucket));
			return "Error";
		}
		return "File Loaded";
	}
}