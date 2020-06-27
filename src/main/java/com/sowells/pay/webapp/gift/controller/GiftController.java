package com.sowells.pay.webapp.gift.controller;

import com.sowells.pay.webapp.gift.domain.GiftCreationRequest;
import com.sowells.pay.webapp.gift.domain.GiftInfoResponse;
import com.sowells.pay.webapp.gift.service.GiftService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/gift")
public class GiftController {
  final GiftService service;

  public GiftController(GiftService service) {
    this.service = service;
  }

  @PostMapping
  public String add(@RequestHeader("X-USER-ID") long userId, @RequestHeader("X-ROOM-ID") String roomId, @RequestBody GiftCreationRequest req) {
    return service.add(userId, roomId, req.getTotalAmount(), req.getMaxNumOfRecipients());
  }

  @PutMapping("/{token}")
  public long receive(@RequestHeader("X-USER-ID") long userId, @RequestHeader("X-ROOM-ID") String roomId, @PathVariable("token") String token) {
    return service.receive(userId, roomId, token);
  }

  @GetMapping("/{token}")
  public GiftInfoResponse list(@RequestHeader("X-USER-ID") long userId, @RequestHeader("X-ROOM-ID") String roomId, @PathVariable("token") String token) {
    return service.get(userId, roomId, token);
  }
}
