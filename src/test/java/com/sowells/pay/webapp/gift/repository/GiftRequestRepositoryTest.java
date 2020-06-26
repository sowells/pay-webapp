package com.sowells.pay.webapp.gift.repository;

import com.sowells.pay.webapp.gift.entity.GiftOrder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.sql.Timestamp;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Slf4j
public class GiftRequestRepositoryTest {
	@Autowired
	GiftOrderRepository repository;

	@Test
	public void addRequestTest() {
		long expirationTime = System.currentTimeMillis() + 600000;
		GiftOrder req = new GiftOrder();
		req.setToken("abc");
		req.setRoomId("abc");
		req.setTotalAmount(10000);
		req.setMaxNumOfRecipients(1);
		req.setExpirationTime(new Timestamp(expirationTime));
		req.setCreatorId(1);

		assertThatCode(() -> {
			repository.save(req);
		}).doesNotThrowAnyException();

		GiftOrder savedReq = repository.findById(req.getRequestId()).get();
		assertThat(savedReq).isNotNull();
		log.info("The result of saving gift request is {}.", savedReq);
		assertThat(savedReq.equals(req)).isTrue();
	}
}
