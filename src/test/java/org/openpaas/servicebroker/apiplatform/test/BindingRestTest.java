package org.openpaas.servicebroker.apiplatform.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.openpaas.servicebroker.apiplatform.common.APIPlatformAPI;
import org.openpaas.servicebroker.common.BindingBody;
import org.openpaas.servicebroker.apiplatform.common.HttpClientUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.codec.Base64;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BindingRestTest {

private static Properties prop = new Properties();

private static String uri;
private static String parameters;
private static APIPlatformAPI api = new APIPlatformAPI();
private static String cookie;


	@BeforeClass
	public static void init() {

		System.out.println("== Started test Binding API ==");

		// Initialization
		// Get properties information
		String propFile = "test.properties";
 
		InputStream inputStream = BindingRestTest.class.getClassLoader().getResourceAsStream(propFile);
		
		try {
			prop.load(inputStream);
	 	} catch (IOException e) {
	 		
			e.printStackTrace();
	 		System.err.println(e);
	 	}
		
		//스토어 로그인
		uri = prop.getProperty("APIPlatformServer")+":"+prop.getProperty("APIPlatformPort")+prop.getProperty("URI.Login");
		parameters = "action=login&username=test-user&password=openpaas";
		cookie = api.login(uri, parameters);
		
	//어플리케이션 삭제된 케이스
		uri = prop.getProperty("APIPlatformServer")+":"+prop.getProperty("APIPlatformPort")+prop.getProperty("URI.RemoveApplication");
		parameters = "action=removeApplication&application="+prop.getProperty("removed_app_instance_id");
		api.removeApplication(uri, parameters, cookie);
		
	//사용등록 해제된 케이스
		//어플리케이션 생성
		uri = prop.getProperty("APIPlatformServer")+":"+prop.getProperty("APIPlatformPort")+prop.getProperty("URI.AddAnApplication");
		parameters = 
				"action=addApplication&application="+prop.getProperty("removed_subs_instance_id")+"&tier=Unlimited&description=&callbackUrl=";	
		api.addApplication(uri, parameters, cookie);
		//어플리케이션 아이디
		uri = prop.getProperty("APIPlatformServer")+":"+prop.getProperty("APIPlatformPort")+prop.getProperty("URI.GetApplications");
		parameters = "action=getApplications";
		int appId = api.getAppID(uri, parameters, cookie, prop.getProperty("removed_subs_instance_id"));
		
		//사용등록 해제
		uri = prop.getProperty("APIPlatformServer")+":"+prop.getProperty("APIPlatformPort")+prop.getProperty("URI.RemoveSubscription");
		parameters = "action=removeSubscription&name=Naver&version=1.0.0&provider=apicreator&applicationId="+appId;
		api.removeSubscription(uri, parameters, cookie);
	
		
	//라이프사이클 변경된 케이스
		//퍼블리셔 로그인
		String cookie2 =null;
		uri = prop.getProperty("APIPlatformServer")+":"+prop.getProperty("APIPlatformPort")+prop.getProperty("URI.PublisherLogin");
		parameters = "action=login&username=apipublisher&password=qwer1234";
		cookie2 = api.login(uri, parameters);
		
		//라이프사이클 변경 CREATED --> PUBLISHED
		uri = prop.getProperty("APIPlatformServer")+":"+prop.getProperty("APIPlatformPort")+prop.getProperty("URI.LifecycleChange");
		parameters ="action=updateStatus&name=PhoneVerification&version=2.0.0&provider=apicreator&status=PUBLISHED&publishToGateway=true&requireResubscription=true";
		api.lifecycleChange(uri, parameters, cookie2);
		
		//사용등록
		uri = prop.getProperty("APIPlatformServer")+":"+prop.getProperty("APIPlatformPort")+prop.getProperty("URI.AddSubscription");
		parameters = 
				"action=addAPISubscription&name=PhoneVerification&version=2.0.0&provider=apicreator&tier=Unlimited&applicationName="+prop.getProperty("lifecycle_change_instance_id");
		api.addSubscription(uri, parameters, cookie);
	
		
		//라이프사이클 변경 PUBLISHED -> CREATED
		uri = prop.getProperty("APIPlatformServer")+":"+prop.getProperty("APIPlatformPort")+prop.getProperty("URI.LifecycleChange");
		parameters ="action=updateStatus&name=PhoneVerification&version=2.0.0&provider=apicreator&status=CREATED&publishToGateway=true&requireResubscription=true";
		api.lifecycleChange(uri, parameters, cookie2);
		
	}
	
	
	//B001 정상적인 파라미터가 입력된경우 - 201 CREATED
	@Test
	public void B001_valid_parameters_test() {
		System.out.println("valid_parameters_test start");

		String instance_id = prop.getProperty("test_instance_id");
		String plan_id = prop.getProperty("test_plan_id");
		String service_id= prop.getProperty("test_service_id");
		String app_guid = UUID.randomUUID().toString();
		String binding_id = UUID.randomUUID().toString();
		
		BindingBody body =new BindingBody(service_id, plan_id, app_guid);
		
		HttpHeaders headers = new HttpHeaders();	
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("X-Broker-Api-Version", prop.getProperty("api_version"));
		headers.set("Authorization", "Basic " + new String(Base64.encode((prop.getProperty("auth_id") +":" + prop.getProperty("auth_password")).getBytes())));

		HttpEntity<BindingBody> entity = new HttpEntity<BindingBody>(body, headers);		
		ResponseEntity<String> response = null;

		String url = prop.getProperty("test_base_protocol") + prop.getProperty("test_base_url") + prop.getProperty("provision_path") + "/" + instance_id+"/"+prop.getProperty("binding_path")+"/"+binding_id;
		
		response = HttpClientUtils.sendBinding(url, entity, HttpMethod.PUT);
		System.out.println(response.getStatusCode());
		System.out.println(response.getBody());
		assertEquals(HttpStatus.CREATED, response.getStatusCode());
		assertTrue(response.getBody().contains("credentials"));
		
		System.out.println("valid_parameters_test end");
	}
	//B002 서비스ID가 잘못된 케이스 - 500 INTERNAL_SERVER_ERROR
	@Test
	public void B002_invalid_parameters_test_service() {
		System.out.println("Start - invalid_parameters_test_service");
		
		String instance_id = prop.getProperty("test_instance_id");
		String plan_id = prop.getProperty("test_plan_id");
		String service_id= prop.getProperty("test_service_id_fail");
		String app_guid = UUID.randomUUID().toString();
		String binding_id = UUID.randomUUID().toString();
		
		BindingBody body =new BindingBody(service_id, plan_id, app_guid);
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("X-Broker-Api-Version", prop.getProperty("api_version"));
		headers.set("Authorization", "Basic " + new String(Base64.encode((prop.getProperty("auth_id") +":" + prop.getProperty("auth_password")).getBytes())));

		HttpEntity<BindingBody> entity = new HttpEntity<BindingBody>(body, headers);		
		ResponseEntity<String> response = null;

		String url = prop.getProperty("test_base_protocol") + prop.getProperty("test_base_url") + prop.getProperty("provision_path") + "/" + instance_id+"/"+prop.getProperty("binding_path")+"/"+binding_id;
		
		response = HttpClientUtils.sendBinding(url, entity, HttpMethod.PUT);
		System.out.println(response.getStatusCode());
		System.out.println(response.getBody());
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertTrue(response.getBody().contains("Invalid ServiceID"));
		
		System.out.println("End - invalid_parameters_test_service");
	}
	//B003 플랜ID가 잘못된 케이스- 500 INTERNAL_SERVER_ERROR
	@Test
	public void B003_invalid_parameters_test_plan() {
		
		System.out.println("Start - invalid_parameters_test_plan");
		
		String instance_id = prop.getProperty("test_instance_id");
		String plan_id = prop.getProperty("test_plan_id_fail");
		String service_id= prop.getProperty("test_service_id");
		String app_guid = UUID.randomUUID().toString();
		String binding_id = UUID.randomUUID().toString();
		
		BindingBody body =new BindingBody(service_id, plan_id, app_guid);
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("X-Broker-Api-Version", prop.getProperty("api_version"));
		headers.set("Authorization", "Basic " + new String(Base64.encode((prop.getProperty("auth_id") +":" + prop.getProperty("auth_password")).getBytes())));

		HttpEntity<BindingBody> entity = new HttpEntity<BindingBody>(body, headers);		
		ResponseEntity<String> response = null;

		String url = prop.getProperty("test_base_protocol") + prop.getProperty("test_base_url") + prop.getProperty("provision_path") + "/" + instance_id+"/"+prop.getProperty("binding_path")+"/"+binding_id;
		
		response = HttpClientUtils.sendBinding(url, entity, HttpMethod.PUT);
		System.out.println(response.getStatusCode());
		System.out.println(response.getBody());
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertTrue(response.getBody().contains("Invalid PlanID"));
		
		System.out.println("End - invalid_parameters_test_plan");
	}
	
	
	//B007 API플랫폼에서 유저가 삭제된 경우 
	//직접 API플랫폼 관리콘솔에서 유저를 삭제하고 테스트 
//	@Test
	public void B007_not_found_apiplatform_user_test() {
		
		System.out.println("Start - not_found_apiplatform_user_test");
		
		String instance_id = prop.getProperty("removed_user_instance_id");
		String plan_id = prop.getProperty("test_plan_id");
		String service_id= prop.getProperty("test_service_id");
		String app_guid = UUID.randomUUID().toString();
		String binding_id = UUID.randomUUID().toString();
		
		BindingBody body =new BindingBody(service_id, plan_id, app_guid);
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("X-Broker-Api-Version", prop.getProperty("api_version"));
		headers.set("Authorization", "Basic " + new String(Base64.encode((prop.getProperty("auth_id") +":" + prop.getProperty("auth_password")).getBytes())));

		HttpEntity<BindingBody> entity = new HttpEntity<BindingBody>(body, headers);		
		ResponseEntity<String> response = null;

		String url = prop.getProperty("test_base_protocol") + prop.getProperty("test_base_url") + prop.getProperty("provision_path") + "/" + instance_id+"/"+prop.getProperty("binding_path")+"/"+binding_id;
		
		response = HttpClientUtils.sendBinding(url, entity, HttpMethod.PUT);
		System.out.println(response.getStatusCode());
		System.out.println(response.getBody());
		assertEquals(HttpStatus.CREATED, response.getStatusCode());
		assertTrue(response.getBody().contains("credentials"));
		
		System.out.println("End - not_found_apiplatform_user_test");
	}
	
	//B008 API플랫폼에 어플리케이션이 존재하지 않을 경우 - 500 INTERNAL_SERVER_ERROR
	@Test
	public void B008_not_found_apiplatform_application_test() {
		
		System.out.println("Start - not_found_apiplatform_application_test");
		
		String instance_id = prop.getProperty("removed_app_instance_id");
		String plan_id = prop.getProperty("test_plan_id");
		String service_id= prop.getProperty("test_service_id");
		String app_guid = UUID.randomUUID().toString();
		String binding_id = UUID.randomUUID().toString();
	
		BindingBody body =new BindingBody(service_id, plan_id, app_guid);
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("X-Broker-Api-Version", prop.getProperty("api_version"));
		headers.set("Authorization", "Basic " + new String(Base64.encode((prop.getProperty("auth_id") +":" + prop.getProperty("auth_password")).getBytes())));

		HttpEntity<BindingBody> entity = new HttpEntity<BindingBody>(body, headers);		
		ResponseEntity<String> response = null;
		
		String url = prop.getProperty("test_base_protocol") + prop.getProperty("test_base_url") + prop.getProperty("provision_path") + "/" + instance_id+"/"+prop.getProperty("binding_path")+"/"+binding_id;
		
		response = HttpClientUtils.sendBinding(url, entity, HttpMethod.PUT);
		
		System.out.println(response.getStatusCode());
		System.out.println(response.getBody());
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertTrue(response.getBody().contains("API Platform Application dose not exist."));

		System.out.println("End - not_found_apiplatform_application_test");
	}
	
	//B009 API플랫폼에서 API의 라이프사이클이 변경된 API를 바인딩 요청할 경우
	@Test
	public void B009_lifecycle_changed_API_test() {
		
		System.out.println("B009 Start - lifecycle_changed_API_test");
		
		String instance_id = prop.getProperty("lifecycle_change_instance_id");
		String plan_id = prop.getProperty("lifecycle_change_plan_id");
		String service_id= prop.getProperty("lifecycle_change_service_id");
		String app_guid = UUID.randomUUID().toString();
		String binding_id = UUID.randomUUID().toString();
		
		BindingBody body =new BindingBody(service_id, plan_id, app_guid);
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("X-Broker-Api-Version", prop.getProperty("api_version"));
		headers.set("Authorization", "Basic " + new String(Base64.encode((prop.getProperty("auth_id") +":" + prop.getProperty("auth_password")).getBytes())));

		HttpEntity<BindingBody> entity = new HttpEntity<BindingBody>(body, headers);		
		ResponseEntity<String> response = null;

		String url = prop.getProperty("test_base_protocol") + prop.getProperty("test_base_url") + prop.getProperty("provision_path") + "/" + instance_id+"/"+prop.getProperty("binding_path")+"/"+binding_id;
		
		response = HttpClientUtils.sendBinding(url, entity, HttpMethod.PUT);
		System.out.println(response.getStatusCode());
		System.out.println(response.getBody());
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertTrue(response.getBody().contains("APIPlatform API not published"));
		
		System.out.println("B009 End - lifecycle_changed_API_test");
	}
	
	//B010 API플랫폼에서 API의 사용등록이 해제된 경우 - 500 INTERNAL_SERVER_ERROR
	@Test
	public void B010_subscription_removed_API_test() {
		System.out.println("B010 Start - subscription_removed_API_test");
		
		String instance_id = prop.getProperty("removed_subs_instance_id");
		String plan_id = prop.getProperty("test_plan_id");
		String service_id= prop.getProperty("test_service_id");
		String app_guid = UUID.randomUUID().toString();
		String binding_id = UUID.randomUUID().toString();
	
		BindingBody body =new BindingBody(service_id, plan_id, app_guid);
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("X-Broker-Api-Version", prop.getProperty("api_version"));
		headers.set("Authorization", "Basic " + new String(Base64.encode((prop.getProperty("auth_id") +":" + prop.getProperty("auth_password")).getBytes())));

		HttpEntity<BindingBody> entity = new HttpEntity<BindingBody>(body, headers);		
		ResponseEntity<String> response = null;

		String url = prop.getProperty("test_base_protocol") + prop.getProperty("test_base_url") + prop.getProperty("provision_path") + "/" + instance_id+"/"+prop.getProperty("binding_path")+"/"+binding_id;
		
		response = HttpClientUtils.sendBinding(url, entity, HttpMethod.PUT);
		System.out.println(response.getStatusCode());
		System.out.println(response.getBody());
		
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertTrue(response.getBody().contains("no subscribed API."));

		System.out.println("B010 End - subscription_removed_API_test");
	}
	
	//B011 각각의 파라미터가 빈값으로 요청된 케이스
	//플랜아이디가 빈값으로 요청된 케이스 - 422 UNPROCESSABLE_ENTITY
	@Test
	public void B011_no_plan_id_test() {
		System.out.println("Start - no_plan_id_test");
		
		String instance_id = prop.getProperty("test_instance_id");
		String plan_id = null;
		String service_id= prop.getProperty("test_service_id");
		String app_guid = UUID.randomUUID().toString();
		String binding_id = UUID.randomUUID().toString();
	
		BindingBody body =new BindingBody(service_id, plan_id, app_guid);
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("X-Broker-Api-Version", prop.getProperty("api_version"));
		headers.set("Authorization", "Basic " + new String(Base64.encode((prop.getProperty("auth_id") +":" + prop.getProperty("auth_password")).getBytes())));

		HttpEntity<BindingBody> entity = new HttpEntity<BindingBody>(body, headers);		
		ResponseEntity<String> response = null;

		String url = prop.getProperty("test_base_protocol") + prop.getProperty("test_base_url") + prop.getProperty("provision_path") + "/" + instance_id+"/"+prop.getProperty("binding_path")+"/"+binding_id;
		
		response = HttpClientUtils.sendBinding(url, entity, HttpMethod.PUT);
		System.out.println(response.getStatusCode());
		System.out.println(response.getBody());
		
		assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
		assertTrue(response.getBody().contains("Missing required fields: planId"));

		System.out.println("End - no_plan_id_test");
	}
	//B012 서비스 아이디가 빈값으로 요청된 케이스 - 422 UNPROCESSABLE_ENTITY
	@Test
	public void B012_no_service_id_test() {
		System.out.println("Start - no_service_id_test");
		
		String instance_id = prop.getProperty("test_instance_id");
		String plan_id = prop.getProperty("test_plan_id");
		String service_id= null;
		String app_guid = UUID.randomUUID().toString();
		String binding_id = UUID.randomUUID().toString();
	
		BindingBody body =new BindingBody(service_id, plan_id, app_guid);
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("X-Broker-Api-Version", prop.getProperty("api_version"));
		headers.set("Authorization", "Basic " + new String(Base64.encode((prop.getProperty("auth_id") +":" + prop.getProperty("auth_password")).getBytes())));

		HttpEntity<BindingBody> entity = new HttpEntity<BindingBody>(body, headers);		
		ResponseEntity<String> response = null;

		String url = prop.getProperty("test_base_protocol") + prop.getProperty("test_base_url") + prop.getProperty("provision_path") + "/" + instance_id+"/"+prop.getProperty("binding_path")+"/"+binding_id;
		
		response = HttpClientUtils.sendBinding(url, entity, HttpMethod.PUT);
		System.out.println(response.getStatusCode());
		System.out.println(response.getBody());
		
		assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
		assertTrue(response.getBody().contains("Missing required fields: serviceDefinitionId"));

		System.out.println("End - no_service_id_test");
	}
	//B013 app guid 아이디가 빈값으로 요청된 케이스 - 422 UNPROCESSABLE_ENTITY
	@Test
	public void B013_no_app_guid_test() {
		System.out.println("Start - no_app_guid_test");
		
		String instance_id = prop.getProperty("test_instance_id");
		String plan_id = prop.getProperty("test_plan_id");
		String service_id= prop.getProperty("test_service_id");
		String app_guid = "";
		String binding_id = UUID.randomUUID().toString();
	
		BindingBody body =new BindingBody(service_id, plan_id, app_guid);
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("X-Broker-Api-Version", prop.getProperty("api_version"));
		headers.set("Authorization", "Basic " + new String(Base64.encode((prop.getProperty("auth_id") +":" + prop.getProperty("auth_password")).getBytes())));

		HttpEntity<BindingBody> entity = new HttpEntity<BindingBody>(body, headers);		
		ResponseEntity<String> response = null;

		String url = prop.getProperty("test_base_protocol") + prop.getProperty("test_base_url") + prop.getProperty("provision_path") + "/" + instance_id+"/"+prop.getProperty("binding_path")+"/"+binding_id;
		
		response = HttpClientUtils.sendBinding(url, entity, HttpMethod.PUT);
		System.out.println(response.getStatusCode());
		System.out.println(response.getBody());
		
		assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
		assertTrue(response.getBody().contains("Missing required fields: appGuid"));

		System.out.println("End - no_app_guid_test");
	}
	
	//B014 바인딩 아이디가 빈값으로 요청된 케이스 - 404 NOT_FOUND
	@Test
	public void B014_no_binding_id_test() {
		System.out.println("Start - no_binding_id_test");
		
		String instance_id = prop.getProperty("test_instance_id");
		String plan_id = prop.getProperty("test_plan_id");
		String service_id= prop.getProperty("test_service_id");
		String app_guid = UUID.randomUUID().toString();
		String binding_id = "";
	
		BindingBody body =new BindingBody(service_id, plan_id, app_guid);
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("X-Broker-Api-Version", prop.getProperty("api_version"));
		headers.set("Authorization", "Basic " + new String(Base64.encode((prop.getProperty("auth_id") +":" + prop.getProperty("auth_password")).getBytes())));

		HttpEntity<BindingBody> entity = new HttpEntity<BindingBody>(body, headers);		
		ResponseEntity<String> response = null;

		String url = prop.getProperty("test_base_protocol") + prop.getProperty("test_base_url") + prop.getProperty("provision_path") + "/" + instance_id+"/"+prop.getProperty("binding_path")+"/"+binding_id;
		
		response = HttpClientUtils.sendBinding(url, entity, HttpMethod.PUT);
		System.out.println(response.getStatusCode());
		System.out.println(response.getBody());
		
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertTrue(response.getBody().contains("No message available"));

		System.out.println("End - no_binding_id_test");
	}
	
	//B015 인스턴스 아이디가 빈값으로 요청된 케이스 - 404 NOT_FOUND
	@Test
	public void B015_no_instance_id_test() {
		System.out.println("Start - no_instance_id_test");
		
		String instance_id = "";
		String plan_id = prop.getProperty("test_plan_id");
		String service_id= prop.getProperty("test_service_id");
		String app_guid = UUID.randomUUID().toString();
		String binding_id = UUID.randomUUID().toString();
	
		BindingBody body =new BindingBody(service_id, plan_id, app_guid);
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("X-Broker-Api-Version", prop.getProperty("api_version"));
		headers.set("Authorization", "Basic " + new String(Base64.encode((prop.getProperty("auth_id") +":" + prop.getProperty("auth_password")).getBytes())));

		HttpEntity<BindingBody> entity = new HttpEntity<BindingBody>(body, headers);		
		ResponseEntity<String> response = null;

		String url = prop.getProperty("test_base_protocol") + prop.getProperty("test_base_url") + prop.getProperty("provision_path") + "/" + instance_id+"/"+prop.getProperty("binding_path")+"/"+binding_id;
		
		response = HttpClientUtils.sendBinding(url, entity, HttpMethod.PUT);
		System.out.println(response.getStatusCode());
		System.out.println(response.getBody());
		
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertTrue(response.getBody().contains("No message available"));

		System.out.println("End - no_instance_id_test");
	}
	
	//B016 DB에 존재하는 인스턴스 아이디로 요청이 들어왔을 때, 인스턴스의 조직아이디에 해당하는 유저정보가 DB의 users테이블에서 삭제된 케이스 - 404 NOT_FOUND
	//500 INTERNAL_SERVER_ERROR
	@Test
	public void B016_no_user_test() {
		System.out.println("Start - _no_user_test");
		
		String instance_id =  prop.getProperty("removed_user_instance_id_DB");
		String plan_id = prop.getProperty("test_plan_id");
		String service_id= prop.getProperty("test_service_id");
		String app_guid = UUID.randomUUID().toString();
		String binding_id = UUID.randomUUID().toString();
	
		BindingBody body =new BindingBody(service_id, plan_id, app_guid);
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("X-Broker-Api-Version", prop.getProperty("api_version"));
		headers.set("Authorization", "Basic " + new String(Base64.encode((prop.getProperty("auth_id") +":" + prop.getProperty("auth_password")).getBytes())));

		HttpEntity<BindingBody> entity = new HttpEntity<BindingBody>(body, headers);		
		ResponseEntity<String> response = null;

		String url = prop.getProperty("test_base_protocol") + prop.getProperty("test_base_url") + prop.getProperty("provision_path") + "/" + instance_id+"/"+prop.getProperty("binding_path")+"/"+binding_id;
		
		response = HttpClientUtils.sendBinding(url, entity, HttpMethod.PUT);
		System.out.println(response.getStatusCode());
		System.out.println(response.getBody());
		
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertTrue(response.getBody().contains("not found APIPlatform user information about InstanceID"));

		System.out.println("End - _no_user_test");
	}
}
