package dev.shrekback.post.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class Adjustment {
    private int quantity;
    private boolean add;
    private String user;
    private LocalDateTime date = LocalDateTime.now();

    public Adjustment() {
        date = LocalDateTime.now();
    }

    public Adjustment(int num, boolean add, String id) {
        this();
        this.quantity = num;
        this.add = add;
        this.user = id;
    }
}
