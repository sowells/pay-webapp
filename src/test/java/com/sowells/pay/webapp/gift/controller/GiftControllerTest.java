package com.sowells.pay.webapp.gift.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sowells.pay.webapp.PayWebApplication;
import com.sowells.pay.webapp.gift.domain.GiftCreationRequest;
import com.sowells.pay.webapp.gift.domain.GiftInfoResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.assertj.core.api.Assertions.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = PayWebApplication.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class GiftControllerTest {
  @Autowired
  MockMvc mockMvc;

  @Autowired
  GiftController giftController;

  ObjectMapper objectMapper = new ObjectMapper();

  final String HEADER_USER_ID = "X-USER-ID";
  final String HEADER_ROOM_ID = "X-ROOM-ID";

  @Test
  public void testCreationWorks() throws Exception {
    final long userId = 0;
    final String roomId = "room-0";
    GiftCreationRequest req = new GiftCreationRequest();
    req.setMaxNumOfRecipients(2);
    req.setTotalAmount(1000);

    mockMvc.perform(post("/gift")
      .contentType("application/json")
      .header(HEADER_USER_ID, userId)
      .header(HEADER_ROOM_ID, roomId)
      .content(objectMapper.writeValueAsString(req)))
      .andExpect(status().isOk());
  }

  @Test
  public void testCreationFailOnInvalidInput() throws Exception {
    final long userId = 0;
    final String roomId = "room-0";
    GiftCreationRequest req = new GiftCreationRequest();
    req.setMaxNumOfRecipients(2);
    req.setTotalAmount(1);

    String token = mockMvc.perform(post("/gift")
      .contentType("application/json")
      .header(HEADER_USER_ID, userId)
      .header(HEADER_ROOM_ID, roomId)
      .content(objectMapper.writeValueAsString(req)))
      .andExpect(status().isBadRequest())
      .andReturn().getResponse().getContentAsString();
    assertThat(token).isNotEmpty();
  }

  @Test
  public void testReceivingWorks() throws Exception {
    final long userId = 0;
    final String roomId = "room-0";
    GiftCreationRequest req = new GiftCreationRequest();
    req.setMaxNumOfRecipients(2);
    req.setTotalAmount(1000);

    String token = mockMvc.perform(post("/gift")
      .contentType("application/json")
      .header(HEADER_USER_ID, userId)
      .header(HEADER_ROOM_ID, roomId)
      .content(objectMapper.writeValueAsString(req)))
      .andReturn().getResponse().getContentAsString();

    long receiverId = 1;
    String receivedAmount = mockMvc.perform(put("/gift/"+token)
      .contentType("application/json")
      .header(HEADER_USER_ID, receiverId)
      .header(HEADER_ROOM_ID, roomId))
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();
    assertThat(receivedAmount).isNotEmpty();
    assertThat(Long.valueOf(receivedAmount)).isGreaterThan(0);
  }

  @Test
  public void testReceivingFailedOnInvalidInput() throws Exception {
    final long userId = 0;
    final String roomId = "room-0";
    final String wrongToken = "wro";
    mockMvc.perform(put("/gift/"+wrongToken)
      .contentType("application/json")
      .header(HEADER_USER_ID, userId)
      .header(HEADER_ROOM_ID, roomId))
      .andExpect(status().isBadRequest());
  }

  @Test
  public void testRetrievingWorks() throws Exception {
    final long userId = 0;
    final String roomId = "room-0";
    final long totalAmount = 1000;
    GiftCreationRequest req = new GiftCreationRequest();
    req.setMaxNumOfRecipients(2);
    req.setTotalAmount(totalAmount);

    String token = mockMvc.perform(post("/gift")
      .contentType("application/json")
      .header(HEADER_USER_ID, userId)
      .header(HEADER_ROOM_ID, roomId)
      .content(objectMapper.writeValueAsString(req)))
      .andReturn().getResponse().getContentAsString();

    String response = mockMvc.perform(get("/gift/"+token)
      .contentType("application/json")
      .header(HEADER_USER_ID, userId)
      .header(HEADER_ROOM_ID, roomId))
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();

    GiftInfoResponse giftInfo = objectMapper.readValue(response, GiftInfoResponse.class);
    assertThat(giftInfo).isNotNull();
    assertThat(giftInfo.getTotalAmount()).isEqualTo(totalAmount);
  }

  @Test
  public void testRetrievingFailOnInvalidInput() throws Exception {
    final long userId = 0;
    final String roomId = "room-0";
    final long totalAmount = 1000;
    GiftCreationRequest req = new GiftCreationRequest();
    req.setMaxNumOfRecipients(2);
    req.setTotalAmount(totalAmount);

    String token = mockMvc.perform(post("/gift")
      .contentType("application/json")
      .header(HEADER_USER_ID, userId)
      .header(HEADER_ROOM_ID, roomId)
      .content(objectMapper.writeValueAsString(req)))
      .andReturn().getResponse().getContentAsString();

    String wrongToken = token + "1";
    mockMvc.perform(get("/gift/"+wrongToken)
      .contentType("application/json")
      .header(HEADER_USER_ID, userId)
      .header(HEADER_ROOM_ID, roomId))
      .andExpect(status().isBadRequest());
  }
}
