/**
 * <b>Assistant — generic subdomain.</b> The AI shopping assistant and its WebSocket
 * transport, behind an {@code AiAssistant} port so the model provider is swappable and
 * the rest of the system never imports an LLM client.
 *
 * <p><b>Relationships:</b> reads the catalog through the same {@code CatalogSnapshot} ACL
 * everything else uses. It answers questions; it never writes.
 *
 * <p><b>Known limitation, logged not fixed:</b> chat memory is a JVM-local
 * {@code ConcurrentHashMap}, so conversations do not survive a restart and will not
 * survive scaling out. Externalizing it is a follow-up.
 *
 * <p>Filled in by <b>Sprint 13</b>.
 */
package com.dominator.gearly.assistant;
