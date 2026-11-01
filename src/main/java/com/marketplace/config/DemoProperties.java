package com.marketplace.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "marketplace.demo")
public class DemoProperties {

	private String adminEmail;
	private String adminPassword;
	private String vendorEmail;
	private String vendorPassword;
	private String customerEmail;
	private String customerPassword;
	private boolean activateExistingUsers = false;
	private int extraVendors = 2;
	private int extraCustomers = 9;

	public String getAdminEmail() {
		return adminEmail;
	}

	public void setAdminEmail(String adminEmail) {
		this.adminEmail = adminEmail;
	}

	public String getAdminPassword() {
		return adminPassword;
	}

	public void setAdminPassword(String adminPassword) {
		this.adminPassword = adminPassword;
	}

	public String getVendorEmail() {
		return vendorEmail;
	}

	public void setVendorEmail(String vendorEmail) {
		this.vendorEmail = vendorEmail;
	}

	public String getVendorPassword() {
		return vendorPassword;
	}

	public void setVendorPassword(String vendorPassword) {
		this.vendorPassword = vendorPassword;
	}

	public String getCustomerEmail() {
		return customerEmail;
	}

	public void setCustomerEmail(String customerEmail) {
		this.customerEmail = customerEmail;
	}

	public String getCustomerPassword() {
		return customerPassword;
	}

	public void setCustomerPassword(String customerPassword) {
		this.customerPassword = customerPassword;
	}

	public boolean isActivateExistingUsers() {
		return activateExistingUsers;
	}

	public void setActivateExistingUsers(boolean activateExistingUsers) {
		this.activateExistingUsers = activateExistingUsers;
	}

	public int getExtraVendors() { return extraVendors; }
	public void setExtraVendors(int extraVendors) { this.extraVendors = extraVendors; }
	public int getExtraCustomers() { return extraCustomers; }
	public void setExtraCustomers(int extraCustomers) { this.extraCustomers = extraCustomers; }
}
