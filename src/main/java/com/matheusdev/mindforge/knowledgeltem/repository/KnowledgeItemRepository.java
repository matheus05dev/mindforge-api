package com.matheusdev.mindforge.knowledgeltem.repository;

import com.matheusdev.mindforge.knowledgeltem.model.KnowledgeItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeItemRepository extends JpaRepository<KnowledgeItem, Long> {

    // Encontra itens de conhecimento que contenham uma tag específica na sua lista de tags.
    // Esta é a forma correta e performática de consultar coleções de elementos.
    @Query("SELECT ki FROM KnowledgeItem ki JOIN ki.tags t WHERE t = :tag")
    List<KnowledgeItem> findByTag(@Param("tag") String tag);
}
