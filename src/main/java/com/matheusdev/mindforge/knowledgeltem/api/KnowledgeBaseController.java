package com.matheusdev.mindforge.knowledgeltem.api;

import com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeItemRequest;
import com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeItemResponse;
import com.matheusdev.mindforge.knowledgeltem.mapper.KnowledgeItemMapper;
import com.matheusdev.mindforge.knowledgeltem.model.KnowledgeItem;
import com.matheusdev.mindforge.knowledgeltem.service.KnowledgeBaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
@Tag(name = "Knowledge Base", description = "Knowledge base management")
public class KnowledgeBaseController {

        private final KnowledgeBaseService service;
        private final KnowledgeItemMapper mapper;

        @Operation(summary = "Get all knowledge items", description = "Returns a list of all knowledge items")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved the list")
        })
        @GetMapping
        public ResponseEntity<List<KnowledgeItemResponse>> getAllItems() {
                List<KnowledgeItem> items = service.getAllItems();
                return ResponseEntity.ok(items.stream()
                                .map(mapper::toResponse)
                                .collect(Collectors.toList()));
        }

        @Operation(summary = "Get a knowledge item by ID", description = "Returns a single knowledge item")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved the item"),
                        @ApiResponse(responseCode = "404", description = "Item not found")
        })
        @GetMapping("/{id}")
        public ResponseEntity<KnowledgeItemResponse> getItemById(@PathVariable Long id) {
                KnowledgeItem item = service.getItemById(id);
                return ResponseEntity.ok(mapper.toResponse(item));
        }

        @Operation(summary = "Create a new knowledge item", description = "Creates a new knowledge item")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully created the item")
        })
        @PostMapping
        public ResponseEntity<KnowledgeItemResponse> createItem(@RequestBody KnowledgeItemRequest request) {
                KnowledgeItem item = mapper.toEntity(request);
                KnowledgeItem createdItem = service.createItem(item, request.getWorkspaceId());
                return ResponseEntity.ok(mapper.toResponse(createdItem));
        }

        @Operation(summary = "Update a knowledge item", description = "Updates an existing knowledge item")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully updated the item"),
                        @ApiResponse(responseCode = "404", description = "Item not found")
        })
        @PutMapping("/{id}")
        public ResponseEntity<KnowledgeItemResponse> updateItem(@PathVariable Long id,
                        @RequestBody KnowledgeItemRequest request) {
                KnowledgeItem item = mapper.toEntity(request);
                KnowledgeItem updatedItem = service.updateItem(id, item);
                return ResponseEntity.ok(mapper.toResponse(updatedItem));
        }

        @Operation(summary = "Delete a knowledge item", description = "Deletes a knowledge item")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "204", description = "Successfully deleted the item"),
                        @ApiResponse(responseCode = "404", description = "Item not found")
        })
        @DeleteMapping("/{id}")
        public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
                service.deleteItem(id);
                return ResponseEntity.noContent().build();
        }

        @Operation(summary = "Search knowledge items by tag", description = "Returns a list of knowledge items that have the specified tag")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved the list")
        })
        @GetMapping("/search")
        public ResponseEntity<List<KnowledgeItemResponse>> searchByTag(@RequestParam String tag) {
                List<KnowledgeItem> items = service.searchByTag(tag);
                return ResponseEntity.ok(items.stream()
                                .map(mapper::toResponse)
                                .collect(Collectors.toList()));
        }
}
