package com.sowells.pay.webapp.gift.entity;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.beans.factory.annotation.Value;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;

@Data
@Entity
@Table(name = "t_gift_order", uniqueConstraints=@UniqueConstraint(columnNames={"roomId", "token"}))
public class GiftOrder {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private String requestId;
  private String roomId;
  private String token;
  private long totalAmount;
  private int maxNumOfRecipients;
  private long creatorId;
  @CreationTimestamp
  private Timestamp createTime;
  private Timestamp expirationTime;
  @OneToMany(fetch = FetchType.EAGER)
  @JoinColumn(name = "requestId")
  private List<GiftHistory> histories;

  public boolean isVisible(long currentTime, long visible_period) {
    Timestamp boundary = new Timestamp(createTime.getTime() + visible_period);
    Timestamp current = new Timestamp(currentTime);
    if(current.after(boundary)) return false;
    else return true;
  }

  public boolean hasExpired(long currentTime) {
    Timestamp current = new Timestamp(currentTime);
    if(current.after(expirationTime)) return true;
    else return false;
  }
}
