package com.marketplace.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderStatusControllerTests {

	@Test
	void statusesExposeEnumCodesAndDisplayLabels() throws Exception {
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new OrderStatusController()).build();

		mockMvc.perform(get("/api/orders/statuses"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.statuses[0].value").value("PLACED"))
				.andExpect(jsonPath("$.data.statuses[0].label").value("Placed"))
				.andExpect(jsonPath("$.data.statuses[1].value").value("CANCELLED"))
				.andExpect(jsonPath("$.data.statuses[1].label").value("Cancelled"))
				.andExpect(jsonPath("$.data.statuses[2].value").value("FULFILLED"))
				.andExpect(jsonPath("$.data.statuses[2].label").value("Fulfilled"));
	}
}
