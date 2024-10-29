package org.consumer.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Entity
public class FailureRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Use IDENTITY for automatic incrementing
    private Integer id; // Renamed from bookId to id for clarity and to represent the entity correctly

    private String topic;
    private Integer key;
    private String errorRecord;
    private Integer partition;
    private Long offsetValue; // Renamed to follow Java naming conventions
    private String exception;
    private String status;
}
