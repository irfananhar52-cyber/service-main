package com.irfan.demo;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willReturn;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DemoApplicationTests {

	private MockMvc mockMvc;

	private ProducerService producerService;

	private ConsumerService consumerService;

	@BeforeEach
	void setUp() {
		ProducerController producerController = new ProducerController();
		producerService = Mockito.mock(ProducerService.class);
		consumerService = Mockito.mock(ConsumerService.class);
		ReflectionTestUtils.setField(producerController, "producerService", producerService);
		ReflectionTestUtils.setField(producerController, "consumerService", consumerService);
		mockMvc = MockMvcBuilders.standaloneSetup(producerController).build();

		willDoNothing().given(producerService).sendMessage("hello");
		willReturn(List.of("2026-05-03T09:02:02 - hello")).given(consumerService).getReceivedMessages();
	}

	@Test
	void healthEndpointReturnsRunningStatus() throws Exception {
		mockMvc.perform(get("/health"))
			.andExpect(status().isOk())
			.andExpect(content().string("Application is running"));
	}

	@Test
	void sendEndpointSendsMessage() throws Exception {
		mockMvc.perform(get("/send").param("message", "hello"))
			.andExpect(status().isOk())
			.andExpect(content().string("Message sent successfully: hello"));

		then(producerService).should().sendMessage("hello");
	}

	@Test
	void receivedEndpointReturnsMessages() throws Exception {
		mockMvc.perform(get("/received"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0]").value("2026-05-03T09:02:02 - hello"));
	}

}
