package com.sowells.pay.webapp.gift.service;

import com.sowells.pay.webapp.PayWebApplication;
import com.sowells.pay.webapp.gift.constant.Errors;
import com.sowells.pay.webapp.gift.domain.GiftInfoResponse;
import com.sowells.pay.webapp.gift.entity.GiftOrder;
import com.sowells.pay.webapp.gift.exception.BadRequestException;
import com.sowells.pay.webapp.gift.exception.InternalException;
import com.sowells.pay.webapp.gift.repository.GiftOrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.*;

@Slf4j
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest(classes = PayWebApplication.class)
class GiftServiceTest {

  @Autowired GiftService giftService;

  @Autowired
  GiftOrderRepository giftOrderRepository;

  @Value("${gift.order.expire.duration}")
  long EXPIRE_DURATION;

  @Value("${gift.order.visible.period}")
  long VISIBLE_PERIOD;

  @Value("${gift.token.size}")
  int GIFT_TOKEN_SIZE;

  @BeforeEach
  void setUp() {
  }

  // 생성 - 정상 케이스
  @Test
  void testCreationWorks() {
    testCreationWorks(0, "room-0", 10000, 2);
    testCreationWorks(0, "room-0", 10000, 100);
    testCreationWorks(1, "room-1", 100000, 200);
  }

  public void testCreationWorks(long testUserId, String testRoomId, long amount, int maxNumOfReceivers) {
    String token = giftService.add(testUserId, testRoomId, amount, maxNumOfReceivers);

    // 토큰을 리턴하는가
    assertThat(token).isNotEmpty().hasSize(GIFT_TOKEN_SIZE);

    // DB에 정상 적재되었는가
    GiftOrder order = giftOrderRepository.findByRoomIdAndToken(testRoomId, token);
    assertThat(order).isNotNull();
    assertThat(order.getCreatorId()).isEqualTo(testUserId);
    assertThat(order.getRoomId()).isEqualTo(testRoomId);
    assertThat(order.getTotalAmount()).isEqualTo(amount);
    assertThat(order.getMaxNumOfRecipients()).isEqualTo(maxNumOfReceivers);
  }

  @Test
  void testCreationShouldFailedOnInvalidInput() {
    long testUserId = 0;
    String testRoomId = "room-0";

    assertThatThrownBy(() -> {
      giftService.add(testUserId, testRoomId, -1, -1);
    }).isInstanceOf(BadRequestException.class).hasMessageContaining(Errors.MUST_BE_POSITIVE.getMessage());

    assertThatThrownBy(() -> {
      giftService.add(testUserId, testRoomId, 1, 0);
    }).isInstanceOf(BadRequestException.class).hasMessageContaining(Errors.MUST_BE_POSITIVE.getMessage());

    assertThatThrownBy(() -> {
      giftService.add(testUserId, testRoomId, 1, 2);
    }).isInstanceOf(BadRequestException.class).hasMessageContaining(Errors.AMOUNT_MUST_GREATER_THAN_RECEIVERS.getMessage());
  }

  // 생성 - 이미 모든 토큰이 소진된 경우 실패한다.
  @Test
  void testCreationFailIfNoTokenAvailable() {
    final int numChars = 36; // 문자 26개 + 숫자 10개
    assertThatThrownBy(() -> {
      for(int i = 0; i < GIFT_TOKEN_SIZE * numChars + 5; i++) {
        giftService.add(0, "room-0", 10, 2);
      }
    }).isInstanceOf(InternalException.class).hasMessageContaining(Errors.NO_TOKEN_AVAILABLE.getMessage());
  }

  // 받기 - 정상 받기된 경우 받은 금액을 리턴해야 한다.
  @Test
  void testReceivingWorks() throws InterruptedException {
    final String testRoomId = "room-0";
    final String token = giftService.add(0, testRoomId, 1000, 2);

    final long receiverId = 1;
    long receivedAmount = giftService.receive(receiverId, testRoomId, token);
    // 금액을 리턴하는가
    assertThat(receivedAmount).isGreaterThan(0);

    GiftOrder order = giftOrderRepository.findByRoomIdAndToken(testRoomId, token);

    // 받기 처리가 DB까지 정상 수행되었는가
    assertThat(order.getHistories()).isNotEmpty();
    boolean historySaved = order.getHistories().stream().filter(h -> h.getReceiverId() != null && h.getReceiverId() == receiverId).findAny().isPresent();
    assertThat(historySaved).isTrue();

    // 받기 되지 않은 나머지 1건이 남아있는가
    boolean unreceivedExist = order.getHistories().stream().filter(h -> h.getReceiverId() == null).findAny().isPresent();
    assertThat(unreceivedExist).isTrue();
  }

  // 받기 - 뿌리기 당 한 사용자는 한번만 받을 수 있다.
  @Test
  void testOneCanReceiveOnlyOnce() {
    final String testRoomId = "room-0";
    final String token = giftService.add(0, testRoomId, 1000, 2);

    final long receiverId = 1;
    giftService.receive(receiverId, testRoomId, token);

    assertThatThrownBy(() -> {
      giftService.receive(receiverId, testRoomId, token);
    }).isInstanceOf(BadRequestException.class).hasMessageContaining(Errors.ALREADY_RECEIVED.getMessage());
  }

  // 받기 - 뿌리기가 호출된 대화방과 동일한 대화방에 속한 사용자만이 받을 수 있다.
  @Test
  void testReceiverMustBeInSameRoom() {
    final String token = giftService.add(0, "room-0", 1000, 2);

    assertThatThrownBy(() -> {
      giftService.receive(1, "room-1", token);
    }).isInstanceOf(BadRequestException.class).hasMessageContaining(Errors.INVALID_TOKEN.getMessage());
  }

  // 받기 - 자신이 뿌리기한 건은 자신이 받을 수 없다.
  @Test
  void testCreatorNotAllowedToReceive() {
    final long creatorId = 0;
    final String testRoomId = "room-0";
    final String token = giftService.add(creatorId, testRoomId, 1000, 2);

    assertThatThrownBy(() -> {
      giftService.receive(creatorId, testRoomId, token);
    }).isInstanceOf(BadRequestException.class).hasMessageContaining(Errors.NOT_ALLOWED_TO_CREATOR.getMessage());
  }

  // 받기 - 이미 선착순 인원이 다 받아간 경우 실패 응답이 내려져야 한다.
  @Test
  void testCannotReceivedIfAlreadyFullyConsumed() {
    final String testRoomId = "room-0";

    final String token = giftService.add(0, testRoomId, 1000, 2);
    giftService.receive(1, testRoomId, token);
    giftService.receive(2, testRoomId, token);

    assertThatThrownBy(() -> {
      giftService.receive(3, testRoomId, token);
    }).isInstanceOf(BadRequestException.class).hasMessageContaining(Errors.ALREADY_FULLY_CONSUMED.getMessage());

  }

  // 받기 - 뿌린 건은 10분만 유효하고, 10분이 지난 요청에 대해서는 받기 실패 응답이 내려져야 한다.
  @Test
  void testCannotReceiveIfExpired() throws InterruptedException {
    final String testRoomId = "room-0";
    final String token = giftService.add(0, testRoomId, 1000, 2);

    Thread.sleep(EXPIRE_DURATION);
    assertThatThrownBy(() -> {
      giftService.receive(1, testRoomId, token);
    }).isInstanceOf(BadRequestException.class).hasMessageContaining(Errors.EXPIRED.getMessage());
  }

  // 조회 - 정상 케이스
  @Test
  void testRetrievingWorks() {
    final long creatorId = 0;
    final String testRoomId = "room-0";
    final long totalAmount = 1000;
    final String token = giftService.add(creatorId, testRoomId, totalAmount, 2);
    testRetrievingWorks(1, creatorId, testRoomId, token);
    testRetrievingWorks(2, creatorId, testRoomId, token);
  }

  void testRetrievingWorks(long receiverId, long creatorId, String testRoomId, String token) {
    // 기존 받기 완료된 금액
    long totalReceivedAmount = giftService.get(creatorId, testRoomId, token).getReceivedAmount();

    long receivedAmount = giftService.receive(receiverId, testRoomId, token);

    GiftInfoResponse res = giftService.get(creatorId, testRoomId, token);

    // 받기 완료된 금액이 리턴되는지 확인
    assertThat(res.getReceivedAmount()).isEqualTo(totalReceivedAmount+receivedAmount);

    // 받은 금액과 일치하는가
    GiftInfoResponse.Receiving receiving = res.getRecevings().stream().filter(h -> h.getReceiverId() == receiverId).findAny().get();

    // 받은 사람 리스트에 노출되는지 확인
    assertThat(receiving).isNotNull();
    assertThat(receiving.getAmount()).isEqualTo(receivedAmount);
  }

  // 유효하지 않은 토큰은 실패 응답이 내려간다.
  @Test
  void testRetrievingFailOnInvalidToken() {
    final long testUserId = 0;
    final String testRoomId = "room-0";
    final String token = giftService.add(testUserId, testRoomId, 1000, 2);

    assertThatCode(() -> {
     giftService.get(testUserId, testRoomId, token+1);
    }).isInstanceOf(BadRequestException.class).hasMessageContaining(Errors.INVALID_TOKEN.getMessage());
  }

  // 뿌린 사람 자신만 조회할 수 있다.
  @Test
  void testRetrievingOnlyAllowedToCreator() {
    final long testUserId = 0;
    final String testRoomId = "room-0";
    final String token = giftService.add(testUserId, testRoomId, 1000, 2);

    assertThatCode(() -> {
      giftService.get(1, testRoomId, token);
    }).isInstanceOf(BadRequestException.class).hasMessageContaining(Errors.ONLY_ALLOWED_TO_CREATOR.getMessage());
  }

  // 뿌린 건에 대한 조회는 7일 동안 할 수 있다.
  @Test
  void testRetrievingFailIfQueryPeriodPassed() throws InterruptedException {
    final long testUserId = 0;
    final String testRoomId = "room-0";
    final String token = giftService.add(testUserId, testRoomId, 1000, 2);

    Thread.sleep(VISIBLE_PERIOD+1000);

    assertThatCode(() -> {
      giftService.get(testUserId, testRoomId, token);
    }).isInstanceOf(BadRequestException.class).hasMessageContaining(Errors.QUERY_PERIOD_PASSED.getMessage());
  }

  @Test
  void testMoneyDividerMustBeConsistent() {
    testMoneyDividerMustBeConsistent(10000, 2);
    testMoneyDividerMustBeConsistent(10000, 2);
    testMoneyDividerMustBeConsistent(10000, 3);
    testMoneyDividerMustBeConsistent(10000, 100);
  }

  private void testMoneyDividerMustBeConsistent(long totalAmount, int numOfReceivers) {
    GiftService.MoneyDivider moneyDivider = giftService.new MoneyDivider(totalAmount, numOfReceivers);
    long sumAmount = 0;
    for(int i = 0; i < numOfReceivers; i++) {
      long amount = moneyDivider.next();
      assertThat(amount).isLessThanOrEqualTo(moneyDivider.getMaxAmount());
      assertThat(amount).isGreaterThanOrEqualTo(moneyDivider.getMinAmount());
      sumAmount += amount;
    }
    assertThat(sumAmount).isEqualTo(totalAmount);
  }
}