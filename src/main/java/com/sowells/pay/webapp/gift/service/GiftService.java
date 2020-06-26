package com.sowells.pay.webapp.gift.service;

import com.sowells.pay.webapp.gift.component.TokenFactory;
import com.sowells.pay.webapp.gift.constant.Errors;
import com.sowells.pay.webapp.gift.domain.GiftInfoResponse;
import com.sowells.pay.webapp.gift.entity.GiftHistory;
import com.sowells.pay.webapp.gift.entity.GiftOrder;
import com.sowells.pay.webapp.gift.exception.BadRequestException;
import com.sowells.pay.webapp.gift.exception.InternalException;
import com.sowells.pay.webapp.gift.repository.GiftHistoryRepository;
import com.sowells.pay.webapp.gift.repository.GiftOrderRepository;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.persistence.Transient;
import javax.transaction.Transactional;
import javax.validation.constraints.Positive;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GiftService {

  final GiftOrderRepository orderRepository;
  final GiftHistoryRepository historyRepository;
  final TokenFactory tokenFactory;

  @Value("${gift.order.expire.duration}")
  long EXPIRE_DURATION;

  @Value("${gift.order.visible.period}")
  private long VISIBLE_PERIOD_MILLIS;


  public GiftService(GiftOrderRepository orderRepository, GiftHistoryRepository historyRepository, TokenFactory tokenFactory) {
    this.orderRepository = orderRepository;
    this.historyRepository = historyRepository;
    this.tokenFactory = tokenFactory;
  }

  /**
   * 뿌리기 요청건에 대한 고유 token을 발급하고 응답값으로 내려줍니다.
   * 뿌릴 금액은 인원수에 맞게 분배하여 저장한다.
   * 제약 조건 :
   *   token은 3자리 문자열로 구성되며 예측이 불가능해야 한다.
   *   인원수 보다 많은 금액을 입력해야 한다.
   * @param userId 뿌리기 요청한 사용자 식별값
   * @param roomId 사용자가 속한 대화방 식별값
   * @param totalAmount 뿌릴 금액
   * @param maxNumOfReceivers 뿌릴 인원
   * @return 뿌리기 요청건에 발급된 고유 token (RoomId내에서 고유하다)
   */
  @Transactional
  // TODO : isolation level 설정
  public String add(@Positive long userId, @NonNull String roomId, @Positive long totalAmount, @Positive int maxNumOfReceivers) {
    String params = String.format(" userId: %d, roomId: %s", userId, roomId);
    if(totalAmount <= 0 || maxNumOfReceivers <= 0) throw new BadRequestException(Errors.MUST_BE_POSITIVE.getMessage()+params); // 금액과 인원 모두 양수여야 한다.
    if(totalAmount < maxNumOfReceivers) throw new BadRequestException(Errors.AMOUNT_MUST_GREATER_THAN_RECEIVERS.getMessage()+params); // 인원 수 보다 많은 금액을 입력해야 한다.

    String token = createToken(roomId);
    Timestamp expirationTime = new Timestamp(System.currentTimeMillis()+EXPIRE_DURATION);
    GiftOrder req = new GiftOrder();
    req.setRoomId(roomId);
    req.setTotalAmount(totalAmount);
    req.setMaxNumOfRecipients(maxNumOfReceivers);
    req.setToken(token);
    req.setCreatorId(userId);
    req.setExpirationTime(expirationTime);
    orderRepository.save(req);

    // 금액 분배하기
    MoneyDivider divider = new MoneyDivider(totalAmount, maxNumOfReceivers);
    for(int i = 0; i < maxNumOfReceivers; i++) {
      GiftHistory history = new GiftHistory();
      history.setRequestId(req.getRequestId());
      history.setAmount(divider.next());
      historyRepository.save(history);
    }

    return token;
  }

  protected String createToken(String roomId) {
    final int MAX_RETRY = 10;
    int retry = 0;
    while(retry++ < MAX_RETRY) {
      String token = tokenFactory.create();
      GiftOrder duplicateTokenReq = orderRepository.findByRoomIdAndToken(roomId, token);
      if(duplicateTokenReq == null) {
        return token;
      }
    }

    throw new InternalException(String.format(Errors.NO_TOKEN_AVAILABLE.getMessage()+" roomId: %s, retryCount: %d",roomId, MAX_RETRY));
  }

  /**
   * token에 해당하는 뿌리기 건 중 아직 누구에게도 할당되지 않은 분배건 하나를 API를 호출한 사용자에게 할당하고, 그 금액을 응답값으로 내려준다.
   * 제약조건 :
   *   뿌리기 당 한 사용자는 한번만 받을 수 있다.
   *   뿌리기가 호출된 대화방과 동일한 대화방에 속한 사용자만이 받을 수 있다.
   *   자신이 뿌리기한 건은 자신이 받을 수 없다.
   *   뿌린 건은 10분만 유효하고, 10분이 지난 요청에 대해서는 받기 실패 응답이 내려져야 한다.
   *   이미 선착순 인원이 다 받아간 경우 실패 응답이 내려져야 한다.
   * @param userId 받기 요청한 사용자 식별값
   * @param roomId 사용자가 속한 대화방 식별값
   * @param token 뿌리기 시 발급된 token
   * @return 받은 금액
   */
  @Transactional
  public long receive(@Positive long userId, @NonNull String roomId, @NonNull String token) {
    long currentTime = System.currentTimeMillis();
    GiftOrder order = orderRepository.findByRoomIdAndToken(roomId, token);
    String params = String.format(" userId: %d, roomId: %s, token: %s", userId, roomId, token);
    if(order == null) throw new BadRequestException(Errors.INVALID_TOKEN.getMessage()+params); // 뿌리기가 호출된 대화방과 동일한 대화방에 속한 사용자만이 받을 수 있다.
    if(order.getCreatorId() == userId) throw new BadRequestException(Errors.NOT_ALLOWED_TO_CREATOR.getMessage()+params); // 자신이 뿌리기한 건은 자신이 받을 수 없다
    if(order.hasExpired(currentTime)) throw new BadRequestException(Errors.EXPIRED.getMessage()+params); // 뿌린 건은 10분만 유효하고, 10분이 지난 요청에 대해서는 받기 실패 응답이 내려져야 한다.

    List<GiftHistory> histories = order.getHistories();
    boolean isAlreadyReceivedUser = histories.stream().filter(h ->h.getReceiverId() != null && userId == h.getReceiverId()).findAny().isPresent();
    if(isAlreadyReceivedUser) throw new BadRequestException(Errors.ALREADY_RECEIVED.getMessage()+params); // 뿌리기 당 한 사용자는 한번만 받을 수 있다.

    Optional<GiftHistory> optionalGiftHistory = histories.stream().filter(h -> h.getReceiverId() == null).findAny();
    if(!optionalGiftHistory.isPresent()) throw new BadRequestException(Errors.ALREADY_FULLY_CONSUMED.getMessage()+params); // 이미 선착순 인원이 다 받아간 경우 실패 응답이 내려져야 한다.
    GiftHistory unreceived = histories.stream().filter(h -> h.getReceiverId() == null).findAny().get();

    unreceived.setReceiverId(userId);
    historyRepository.save(unreceived);

    return unreceived.getAmount();
  }

  /**
   * 뿌리기 건을 조회하여 정보를 내려준다.
   * 제약조건:
   *  유효하지 않은 토큰은 실패 응답이 내려간다.
   *  뿌린 사람 자신만 조회할 수 있다.
   *  뿌린 건에 대한 조회는 7일 동안 할 수 있다.
   * @param userId, 조회 요청한 사용자 식별값
   * @param roomId, 사용자가 속한 대화방 식별값
   * @param token, 뿌리기 시 발급된 token
   * @return 뿌린 시각, 뿌린 금액, 받기 완료된 금액, 받기 완료된 정보 ([받은 금액, 받은 사용자 아이디] 리스트)
   */
  public GiftInfoResponse get(@Positive long userId, @NonNull String roomId, @NonNull String token) {
    long currentTime = System.currentTimeMillis();
    GiftOrder order = orderRepository.findByRoomIdAndToken(roomId, token);
    String params = String.format(" userId: %d, roomId: %s, token: %s", userId, roomId, token);
    if(order == null) throw new BadRequestException(Errors.INVALID_TOKEN.getMessage()+params); // 유효하지 않은 토큰은 실패 응답이 내려간다.
    if(order.getCreatorId() != userId) throw new BadRequestException(Errors.ONLY_ALLOWED_TO_CREATOR.getMessage()+params); // 뿌린 사람 자신만 조회할 수 있다.
    if(!order.isVisible(currentTime, VISIBLE_PERIOD_MILLIS)) throw new BadRequestException(Errors.QUERY_PERIOD_PASSED.getMessage()+params); // 뿌린 건에 대한 조회는 7일 동안 할 수 있다.

    GiftInfoResponse response = new GiftInfoResponse();
    response.setCreationTime(order.getCreateTime()); // 뿌린 시각
    response.setTotalAmount(order.getTotalAmount()); // 뿌린 금액
    List<GiftInfoResponse.Receiving> receivings = new ArrayList<>();
    long receivedAmount = 0;

    List<GiftHistory> receivedHistories = order.getHistories().stream().filter(h -> h.getReceiverId() != null).collect(Collectors.toList());
    for(GiftHistory history : receivedHistories) {
      GiftInfoResponse.Receiving receiving = response.new Receiving();
      receiving.setAmount(history.getAmount()); // 받은 금액
      receiving.setReceiverId(history.getReceiverId());  // 받은 사용자 아이디
      receivings.add(receiving);
      receivedAmount += history.getAmount();
    }

    response.setReceivedAmount(receivedAmount); // 받기 완료된 금액
    response.setRecevings(receivings);
    return response;
  }

  /**
   * 주어진 금액을 주어진 인원 수에 분배한다.
   * 한 사람이 얻을 수 잇는 최소 금액은 n등분 한 금액의 0.5배이다.
   * ex) 10000원을 4명이 나눠갖는 경우, 1250원 ~ 6250원 사이의 금액을 얻는다.
   */
  class MoneyDivider {
    private final int numOfReceivers;
    @Getter private final long minAmount;
    @Getter private final long maxAmount;
    private long[] amounts;
    private int index = 0;

    public MoneyDivider(long totalAmount, int numOfReceivers) {
      this.numOfReceivers = numOfReceivers;

      minAmount = totalAmount / numOfReceivers / 2; // 한 사람이 얻을 수 잇는 최소 금액은 n등분 한 금액의 0.5배이다.
      maxAmount = totalAmount / 2 + minAmount; // 한 사람당 얻을 수 있는 최소금액에 따라 얻을 수 있는 최대 금액이 계산된다.
      long remainingAmount = totalAmount;
      amounts = new long[numOfReceivers];

      for(int i = 0; i < numOfReceivers; i++) {
        long nextMax = nextMaxAmount(numOfReceivers - i - 1, remainingAmount);
        if(i < numOfReceivers - 1)
          amounts[i] = RandomUtils.nextLong(minAmount, Math.min(maxAmount+1, nextMax));
        else
          amounts[i] = remainingAmount;

        remainingAmount -= amounts[i];
      }
      log.debug("Money divided into {}.", amounts);
    }

    private long nextMaxAmount(int numOfRemainingReceivers, long remainingAmount) {
      return remainingAmount - numOfRemainingReceivers * minAmount;
    }

    public long next() {
      if(index >= numOfReceivers) throw new IndexOutOfBoundsException(Errors.ALREADY_FULLY_CONSUMED.getMessage());
      return amounts[index++];
    }
  }
}
