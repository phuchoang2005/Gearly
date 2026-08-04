/**
 * <b>Platform — cross-cutting, not a bounded context.</b> The wiring every context sits
 * on: Spring configuration, the security filter chain and JWT plumbing, the global
 * exception handler, CORS, OpenAPI and the transaction manager.
 *
 * <p><b>Relationships:</b> inverted. Platform is allowed to know about every context —
 * that is its job — but no context may depend on {@code platform}. A domain rule that
 * finds itself needing something from here is misplaced.
 *
 * <p><b>What lives here that used to be scattered:</b> {@code MongoTransactionManager}
 * (which is what makes {@code @Transactional} do anything at all), the
 * {@code OptimisticLockingFailureException} → 409 mapping, and the exception→status
 * translation table.
 *
 * <p>Populated progressively; finalized in <b>Sprint 13</b>.
 */
package com.dominator.gearly.platform;
