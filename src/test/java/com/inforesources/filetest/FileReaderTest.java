package com.inforesources.filetest;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.inforesources.filetest.objects.TestRecord;

/**
 * A simple test harness for locally invoking your Lambda function handler.
 */
@RunWith(MockitoJUnitRunner.class)
public class FileReaderTest {

	private final String CONTENT_TYPE = "image/jpeg";
	private S3Event event;

	@Mock
	private AmazonS3 s3Client;
	@Mock
	private S3Object s3Object;

	@Captor
	private ArgumentCaptor<GetObjectRequest> getObjectRequest;

	@Before
	public void setUp() throws IOException {
		event = TestUtils.parse("/s3-event.put.json", S3Event.class);

		// TODO: customize your mock logic for s3 client
		ObjectMetadata objectMetadata = new ObjectMetadata();
		objectMetadata.setContentType(CONTENT_TYPE);

		/*
		 * when(s3Object.getObjectMetadata()).thenReturn(objectMetadata);
		 * when(s3Client.getObject(getObjectRequest.capture()))
		 * .thenReturn(s3Object);
		 */
	}

	private Context createContext() {

		TestContext ctx = new TestContext();

		// TODO: customize your context here if needed.
		ctx.setFunctionName("Your Function Name");

		return ctx;
	}

	@Test
	public void testFileReader() {

		/*
		 * FileReader handler = new FileReader(s3Client); Context ctx =
		 * createContext();
		 * 
		 * String output = handler.handleRequest(event, ctx);
		 * 
		 * // TODO: validate output here if needed.
		 * Assert.assertEquals(CONTENT_TYPE, output);
		 */
		TestRecord tr = TestRecord.parse("1 , kvs", ',');
		System.out.println(tr.toString());
	}

}
