package com.matheusdev.mindforge.knowledgeltem.model;

import com.matheusdev.mindforge.document.model.Document;
import com.matheusdev.mindforge.workspace.model.Workspace;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "knowledge_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(com.matheusdev.mindforge.core.tenant.listener.TenantEntityListener.class)
public class KnowledgeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private com.matheusdev.mindforge.core.tenant.model.Tenant tenant;

    @Column(name = "tenant_id", insertable = false, updatable = false)
    private Long tenantId;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id")
    private Workspace workspace;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @ElementCollection
    private List<String> tags;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @OneToMany(mappedBy = "knowledgeItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Document> documents = new ArrayList<>();
}
