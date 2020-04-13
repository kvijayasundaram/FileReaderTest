package com.inforesources.filetest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import com.inforesources.filetest.services.RecordParser;

public class FileReader implements RequestHandler<S3Event, String> {

	private AmazonS3 s3 = AmazonS3ClientBuilder.standard().build();

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
		String tableName = key.toLowerCase().substring(0, key.indexOf(".csv"));

		String url, user, password;
		url = System.getenv("DB_URL");
		user = System.getenv("DB_USER");
		password = System.getenv("DB_PASSWORD");
		System.getenv("DB_NAME");

		context.getLogger().log("DB_URL: " + url);

		try {
			S3Object response = s3.getObject(new GetObjectRequest(bucket, key));
			String contentType = response.getObjectMetadata().getContentType();
			context.getLogger().log("CONTENT TYPE: " + contentType);

			context.getLogger().log("BucketName: " + bucket);
			context.getLogger().log("fileName: " + key);
			context.getLogger().log("tableName: " + tableName);

			// this is a spring JDBC library object .
			SimpleJdbcInsert jdbcInsert = new SimpleJdbcInsert(
					pgDataSource(url, user, password, context))
							.withTableName(tableName);

			BufferedReader br = new BufferedReader(
					new InputStreamReader(response.getObjectContent()));
			List<String> headers = new ArrayList<String>();
			if (jdbcInsert != null) {
				br.lines().forEach(line -> {
					context.getLogger().log(">> " + line);
					if (headers.isEmpty()) {
						// for first time parse the header
						String[] headerFields = RecordParser.parse(line, ',');
						for (String s : headerFields) {
							headers.add(s);
						}
					} else {
						// parse the data lines
						String[] valuefields = RecordParser.parse(line, ',');
						Map<String, Object> parameters = new HashMap<String, Object>();
						// if header columns dont match the data columns dont
						// process it..
						if (headers.size() == valuefields.length) {
							for (int i = 0; i < headers.size(); i++) {
								parameters.put(headers.get(i), valuefields[i]);
							}
							jdbcInsert.execute(parameters);
						} else {
							context.getLogger().log(
									"<< Error: Header record is not matching the data lines >>");
						}
					}
				});
			}

		} catch (Exception e) {
			e.printStackTrace();
			context.getLogger().log(String.format(
					"<< Error: getting object %s from bucket %s. Make sure they exist and"
							+ " your bucket is in the same region as this function. >>",
					key, bucket));
			return "Error";
		}
		return "File Loaded";
	}
}