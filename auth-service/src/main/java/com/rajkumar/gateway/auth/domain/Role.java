package com.rajkumar.gateway.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Reference data; rows are inserted by the Flyway migration, never by the application. */
@Entity
@Table(name = "roles")
public class Role {

    @Id
    private Short id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 30)
    private RoleName name;

    protected Role() {
    }

    public Role(Short id, RoleName name) {
        this.id = id;
        this.name = name;
    }

    public Short getId() {
        return id;
    }

    public RoleName getName() {
        return name;
    }
}
