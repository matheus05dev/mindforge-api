package com.matheusdev.mindforge.mindmap.model;

import com.matheusdev.mindforge.workspace.model.Workspace;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MindMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id")
    private Workspace workspace;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String nodesJson;

    @Column(columnDefinition = "TEXT")
    private String edgesJson;
}
