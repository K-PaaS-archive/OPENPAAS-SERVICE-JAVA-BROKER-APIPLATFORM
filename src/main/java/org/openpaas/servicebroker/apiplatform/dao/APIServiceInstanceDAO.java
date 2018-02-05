package org.openpaas.servicebroker.apiplatform.dao;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.openpaas.servicebroker.apiplatform.model.APIServiceInstance;
import org.openpaas.servicebroker.apiplatform.model.APIUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;


@Repository
public class APIServiceInstanceDAO {

	private static final Logger logger = LoggerFactory.getLogger(APIServiceInstanceDAO.class);

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	public APIServiceInstanceDAO(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}
	
	@Autowired
	private Environment env;
	
	public APIUser getAPIUserByOrgID(String organizationGuid){
		logger.info("getAPIUserByOrgID() start");
		logger.debug("oranization_guid : "+organizationGuid);
		String sql = "SELECT * FROM apiplatform_users WHERE organization_guid='"+organizationGuid+"'";
		
		APIUser apiUser = new APIUser();
		
		RowMapper mapper = new RowMapper(){
			public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
				APIUser apiUser = new APIUser();
				apiUser.setOrganization_guid(rs.getString("organization_guid"));
				apiUser.setUser_id(rs.getString("user_id"));
				apiUser.setUser_password(rs.getString("user_password"));
				
				return apiUser;
				}
			};

		apiUser = (APIUser)jdbcTemplate.queryForObject(sql, mapper);

		logger.info("getAPIUserByOrgID() finished");
		
	return apiUser;
	}
	
	public APIServiceInstance getAPIServiceByInstance(String serviceInstanceId){
		logger.info("getServiceByInstanceID() start");
		logger.debug("instance_id : "+serviceInstanceId);
		String sql = "SELECT * FROM apiplatform_services WHERE instance_id='"+serviceInstanceId+"'";
		
		APIServiceInstance apiServiceInstance = new APIServiceInstance();
		
		RowMapper mapper = new RowMapper(){
			public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
				APIServiceInstance apiService = new APIServiceInstance();
				apiService.setInstance_id(rs.getString("instance_id"));
				apiService.setOrganization_id(rs.getString("organization_guid"));
				apiService.setSpace_guid(rs.getString("space_guid"));
				apiService.setService_id(rs.getString("service_id"));
				apiService.setPlan_id(rs.getString("plan_id"));
				apiService.setDelyn(rs.getString("delyn"));
				return apiService;
				}
			};

			apiServiceInstance = (APIServiceInstance)jdbcTemplate.queryForObject(sql, mapper);

		logger.info("getAPIServiceByInstanceID() finished");
		
	return apiServiceInstance;
	}
	
	public APIServiceInstance getAPIInfoByInstanceID(String serviceInstanceId){
		logger.info("getServiceByInstanceID() start");
		logger.debug("instance_id : "+serviceInstanceId);
		String sql = 
				"SELECT s.organization_guid,s.instance_id,s.space_guid,s.service_id,s.plan_id,u.user_id,u.user_password,s.delyn FROM apiplatform_services s "
				+ "INNER JOIN apiplatform_users u ON u.organization_guid = s.organization_guid WHERE instance_id= '"+serviceInstanceId+"'";
		
		APIServiceInstance apiService = new APIServiceInstance();
		
		RowMapper mapper = new RowMapper(){
			public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
				APIServiceInstance apiService = new APIServiceInstance();
				apiService.setInstance_id(rs.getString("instance_id"));
				apiService.setOrganization_id(rs.getString("organization_guid"));
				apiService.setSpace_guid(rs.getString("space_guid"));
				apiService.setService_id(rs.getString("service_id"));
				apiService.setPlan_id(rs.getString("plan_id"));
				apiService.setUser_id(rs.getString("user_id"));
				apiService.setUser_password(rs.getString("user_password"));
				apiService.setDelyn(rs.getString("delyn"));
				return apiService;
				}
			};

			apiService = (APIServiceInstance)jdbcTemplate.queryForObject(sql, mapper);

		logger.info("getAPIServiceByInstanceID() finished");
		
	return apiService;
	}

	public void insertAPIUser(String oranizationGuid, String userId, String userPassword){
		
		logger.info("insertAPIUser() start");
		String sql ="INSERT INTO apiplatform_users (organization_guid,user_id,user_password, createtimestamp) VALUES "
				+ "('"+oranizationGuid+"','"+userId+"','"+userPassword+"',utc_timestamp())";
		
		jdbcTemplate.execute(sql);

		logger.info("insertAPIUser() finished");
	}
	
	public void insertAPIServiceInstance(String oranizationGuid, String serviceInstanceId,String spaceGuid,String serviceId,String planId){
		
		logger.info("insertAPIServiceInstance() start");
		String sql ="INSERT INTO apiplatform_services (organization_guid,instance_id,space_guid,service_id,plan_id,delyn,createtimestamp) VALUES "
				+ "('"+oranizationGuid+"','"+serviceInstanceId+"','"+spaceGuid+"','"+serviceId+"','"+planId+"','N',utc_timestamp())";
	
		jdbcTemplate.execute(sql);
		
		logger.info("insertAPIServiceInstance() finished");
	}
	
	public void updateAPIServiceInstance(String serviceInstanceId, String planId){
		
		logger.info("updateAPIServiceInstance() start");
		String sql ="UPDATE apiplatform_services SET plan_id = '"+planId+"' WHERE instance_id = '"+serviceInstanceId+"'";

		jdbcTemplate.update(sql);
		
		logger.info("updateAPIServiceInstance() finished");
	}
	
	public void deleteAPIServiceInstance(String oranizationGuid,String serviceInstanceId, String serviceId,String planId ){
		
		logger.info("deleteAPIServiceInstance() start");
		String sql ="UPDATE apiplatform_services SET delyn = 'Y', deletetimestamp = utc_timestamp() WHERE instance_id = '"+serviceInstanceId+"' AND "
				+ "organization_guid = '"+oranizationGuid+"' AND service_id = '"+serviceId+"' AND plan_id = '"+planId+"'";
		
		jdbcTemplate.update(sql);
		
		logger.info("deleteAPIServiceInstance() finished");
	}
	
	public void test_beforeBindingTest(String serviceInstanceId){
		
		String sql ="UPDATE apiplatform_services SET delyn = 'N' WHERE instance_id = '"+serviceInstanceId+"'";
		jdbcTemplate.update(sql);
	}
}
