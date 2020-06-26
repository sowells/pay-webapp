package com.sowells.pay.webapp.gift.domain;

import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

@Data
public class GiftInfoResponse {
  Timestamp creationTime; // 뿌린 시각
  long totalAmount; // 뿌린 금액
  long receivedAmount; // 받기 완료된 금액
  List<Receiving> recevings; // 받기 완료된 정보

  @Data
  public class Receiving {
    long receiverId; // 받은 사용자 아이디
    long amount; // 받은 금액
  }
}
