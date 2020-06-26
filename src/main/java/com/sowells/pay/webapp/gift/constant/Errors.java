package com.sowells.pay.webapp.gift.constant;

import lombok.Getter;

public enum Errors {
  INVALID_TOKEN("Invalid token."),
  NOT_ALLOWED_TO_CREATOR("This request is not allowed to the user who created this order."),
  EXPIRED("This gift has expired."),
  ALREADY_RECEIVED("This user already received."),
  ONLY_ALLOWED_TO_CREATOR("This request is only allowed to the user who created this order."),
  QUERY_PERIOD_PASSED("Queryable period of this gift has passed."),
  MUST_BE_POSITIVE("Inputs must be positive."),
  AMOUNT_MUST_GREATER_THAN_RECEIVERS("Amount must be greater than max number of receivers."),
  ALREADY_FULLY_CONSUMED("All money has already consumed."),
  NO_TOKEN_AVAILABLE("Failed to create token. All tokens have already been taken.");

  @Getter
  String message;
  Errors(String message) {
    this.message = message;
  }
}
