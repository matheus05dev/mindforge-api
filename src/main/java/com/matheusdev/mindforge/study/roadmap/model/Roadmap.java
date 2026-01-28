package com.matheusdev.mindforge.study.roadmap.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "roadmaps")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(com.matheusdev.mindforge.core.tenant.listener.TenantEntityListener.class)
public class Roadmap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String targetAudience; // e.g., "Beginner", "Advanced"

    private Long userId; // For now simplified, later User entity relation

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private com.matheusdev.mindforge.core.tenant.model.Tenant tenant;

    @Column(name = "tenant_id", insertable = false, updatable = false)
    private Long tenantId;

    @OneToMany(mappedBy = "roadmap", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<RoadmapItem> items = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
