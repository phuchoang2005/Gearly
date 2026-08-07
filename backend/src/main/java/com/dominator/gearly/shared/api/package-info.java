/**
 * <b>The shared wire vocabulary</b> — response shapes more than one context answers with.
 *
 * <p>Distinct from {@code shared.domain}, which is the shared <em>kernel</em>: value objects and
 * typed ids that contexts are modelled in terms of. Nothing here is a domain concept; these are
 * envelopes both frontends already parse.
 *
 * <p>Deliberately small, and it should stay that way. A response type belongs to the context
 * that answers with it; the only ones here are the ones that would otherwise be copied verbatim
 * into several contexts, where the copies could drift and break a client that treats them as one
 * shape.
 *
 * <p>It cannot live in {@code platform} — {@code contexts_do_not_depend_on_the_platform} would
 * fail every controller that returns one — and it must not depend on a context, which
 * {@code shared_kernel_depends_on_no_context} enforces for everything under {@code shared}.
 */
package com.dominator.gearly.shared.api;
