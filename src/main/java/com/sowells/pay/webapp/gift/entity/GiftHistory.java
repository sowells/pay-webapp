package com.sowells.pay.webapp.gift.entity;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Data
@Table(name = "t_gift_history")
public class GiftHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String historyId;
    private String requestId;
    private Long amount;
    private Long receiverId;
    @CreationTimestamp
    private Timestamp creationTime;
    @UpdateTimestamp
    private Timestamp updateTime;
}
