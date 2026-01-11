package com.dominator.bookify.controller.admin;

import com.dominator.bookify.dto.OrderPatchDTO;
import com.dominator.bookify.model.Order;
import com.dominator.bookify.model.TimeFrame;
import com.dominator.bookify.service.admin.AdminUserService;
import com.dominator.bookify.service.admin.OrderService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@AllArgsConstructor
@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {
    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<?> getAllOrders() {
        try {
            return ResponseEntity.ok().body(orderService.getAllOrders());
        } catch (ResponseStatusException e) {
            return ResponseEntity
                    .status(e.getStatusCode())
                    .body(Map.of("error", e.getReason()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(
            @PathVariable String id
    ) {
        try {
            return ResponseEntity.ok().body(orderService.getOrderById(id));
        } catch (ResponseStatusException e) {
            return ResponseEntity
                    .status(e.getStatusCode())
                    .body(Map.of("error", e.getReason()));
        }
    }




    @GetMapping("/quantity-sold")
    public ResponseEntity<?> getQuantitySoldByBook(
            @RequestParam(defaultValue = "ALL") TimeFrame period
            ) {
        try {
            return ResponseEntity.ok().body(orderService.getQuantitySold(period));
        } catch (ResponseStatusException e) {
            return ResponseEntity
                    .status(e.getStatusCode())
                    .body(Map.of("error", e.getReason()));
        }
    }

    @GetMapping("/top5")
    public ResponseEntity<?> getTop5BestSelling(
            @RequestParam(defaultValue = "ALL") TimeFrame period
    ) {
        try {
            return ResponseEntity.ok().body(orderService.getTop5BestSelling(period));
        } catch (ResponseStatusException e) {
            return ResponseEntity
                    .status(e.getStatusCode())
                    .body(Map.of("error", e.getReason()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateOrder(
            @PathVariable String id,
            @RequestBody Order newOrder
    ) {
        try{
            return ResponseEntity.ok().body(orderService.updateOrder(id, newOrder));
        }catch (ResponseStatusException e) {
            return ResponseEntity
                    .status(e.getStatusCode())
                    .body(Map.of("error", e.getReason()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createOrder(
            @RequestBody Order newOrder
    ) {
        try {
            return ResponseEntity.ok().body(orderService.createOrder(newOrder));
        } catch (ResponseStatusException e) {
            return ResponseEntity
                   .status(e.getStatusCode())
                   .body(Map.of("error", e.getReason()));
        }
    }

    @PostMapping("/{id}/set-cancel")
    public ResponseEntity<?> cancelOrder(
            @PathVariable String id
    ) {
       try{
           return ResponseEntity.ok().body(orderService.setCancelOrder(id));
       } catch (ResponseStatusException e ){
           return ResponseEntity
                  .status(e.getStatusCode())
                  .body(Map.of("error", e.getReason()));
       }
    }

    @PostMapping("/{id}/set-complete")
    public ResponseEntity<?> completeOrder(
            @PathVariable String id
    ) {
       try{
           return ResponseEntity.ok().body(orderService.setCompleteOrder(id));
       } catch (ResponseStatusException e ){
           return ResponseEntity
                  .status(e.getStatusCode())
                  .body(Map.of("error", e.getReason()));
       }
    }

    @PostMapping("/{id}/set-process")
    public ResponseEntity<?> processOrder(
            @PathVariable String id
    ) {
       try{
           return ResponseEntity.ok().body(orderService.setProcessOrder(id));
       } catch (ResponseStatusException e ){
           return ResponseEntity
                  .status(e.getStatusCode())
                  .body(Map.of("error", e.getReason()));
       }
    }

    @PostMapping("/{id}/set-ship")
    public ResponseEntity<?> setShipOrder(
            @PathVariable String id
    ) {
       try{
           return ResponseEntity.ok().body(orderService.setShipOrder(id));
       } catch (ResponseStatusException e ){
           return ResponseEntity
                  .status(e.getStatusCode())
                  .body(Map.of("error", e.getReason()));
       }
    }

    @PostMapping("/{id}/set-delivered")
    public ResponseEntity<?> setDeliveredOrder(
            @PathVariable String id
    ) {
       try{
           return ResponseEntity.ok().body(orderService.setDeliveredOrder(id));
       } catch (ResponseStatusException e ){
           return ResponseEntity
                  .status(e.getStatusCode())
                  .body(Map.of("error", e.getReason()));
       }
    }

    @PostMapping("/{id}/set-pending-refund")
    public ResponseEntity<?> setPendingRefundOrder(
            @PathVariable String id
    ) {
       try{
           return ResponseEntity.ok().body(orderService.setPendingRefundOrder(id));
       } catch (ResponseStatusException e ){
           return ResponseEntity
                  .status(e.getStatusCode())
                  .body(Map.of("error", e.getReason()));
       }
    }

    @PostMapping("/{id}/set-refund")
    public ResponseEntity<?> setRefundedOrder(
            @PathVariable String id
    ) {
       try{
           return ResponseEntity.ok().body(orderService.setRefundedOrder(id));
       } catch (ResponseStatusException e ){
           return ResponseEntity
                  .status(e.getStatusCode())
                  .body(Map.of("error", e.getReason()));
       }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> patchOrder(
            @PathVariable String id,
            @RequestBody OrderPatchDTO orderPatch
    ) {
        try {
            return ResponseEntity.ok().body(orderService.patchOrder(id, orderPatch));
        } catch (ResponseStatusException e) {
            return ResponseEntity
                   .status(e.getStatusCode())
                   .body(Map.of("error", e.getReason()));
        }
    }

}
