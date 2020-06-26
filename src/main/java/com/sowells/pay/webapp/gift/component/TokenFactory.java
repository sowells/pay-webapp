package com.sowells.pay.webapp.gift.component;

import lombok.Getter;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TokenFactory {
  @Getter
  @Value("${gift.token.size}")
  int GIFT_TOKEN_SIZE;

  public String create() {
    return RandomStringUtils.randomAlphanumeric(GIFT_TOKEN_SIZE).toLowerCase();
  }
}
