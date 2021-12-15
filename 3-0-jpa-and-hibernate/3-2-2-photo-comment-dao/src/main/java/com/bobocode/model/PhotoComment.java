package com.bobocode.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * todo:
 * - implement equals and hashCode based on identifier field
 * <p>
 * - configure JPA entity
 * - specify table name: "photo_comment"
 * - configure auto generated identifier
 * - configure not nullable column: text
 * <p>
 * - map relation between Photo and PhotoComment using foreign_key column: "photo_id"
 * - configure relation as mandatory (not optional)
 */
@Getter
@Setter
@Entity
@Table(name = "photo_comment")
@EqualsAndHashCode(of = "id")
public class PhotoComment {
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String text;
    private LocalDateTime createdOn;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "photo_id", foreignKey = @ForeignKey(name = "photo_id"))
    private Photo photo;
}
