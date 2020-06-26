package com.sowells.pay.webapp.gift.domain;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Positive;

@Data
public class GiftCreationRequest {
  @Positive(message = "Inputs must be positive.")
  long totalAmount; // 뿌린 금액
  @Max(300L)
  @Positive(message = "Inputs must be positive.")
  int maxNumOfRecipients; // 뿌릴 인원
}
