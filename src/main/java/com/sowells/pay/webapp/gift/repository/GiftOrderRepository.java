package com.sowells.pay.webapp.gift.repository;

import com.sowells.pay.webapp.gift.entity.GiftOrder;
import org.springframework.data.repository.CrudRepository;

public interface GiftOrderRepository extends CrudRepository<GiftOrder, String> {
  GiftOrder findByRoomIdAndToken(String roomId, String token);
}
