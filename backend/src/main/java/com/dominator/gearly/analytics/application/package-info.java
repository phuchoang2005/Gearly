/**
 * Query services for the admin dashboard. Each method runs an aggregation and returns a
 * DTO; none of them load a domain aggregate.
 *
 * <p><b>Layer contract:</b> {@code MongoTemplate} is permitted here and nowhere else in
 * the codebase.
 *
 * @see com.dominator.gearly.analytics
 */
package com.dominator.gearly.analytics.application;
