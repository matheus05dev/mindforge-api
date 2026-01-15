package com.matheusdev.mindforge.workspace.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Workspace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkspaceType type;
}
