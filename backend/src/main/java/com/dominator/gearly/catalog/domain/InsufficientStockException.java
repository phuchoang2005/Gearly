package com.dominator.gearly.catalog.domain;

import com.dominator.gearly.shared.domain.DomainRuleViolationException;
import com.dominator.gearly.shared.domain.Quantity;
import lombok.Getter;

/**
 * <b>The one stock rule.</b> Somebody asked for more units than exist.
 *
 * <p>This class is the whole point of the sprint's first backlog item. Before it, the same
 * check was written five times — {@code ProductService.decreaseStock},
 * {@code CustomerOrderService}'s placement loop, and {@code CartService} at three separate
 * call sites — each with its own phrasing, and two of them subtly different from the rest.
 * {@link #requireAtLeast} is now the only place in the codebase that compares a wanted
 * quantity against an available one, and both {@link Product} and {@link CatalogSnapshot}
 * call it rather than restating it.
 *
 * <h2>The message is built here, not by the caller</h2>
 * That is what makes the collapse real rather than cosmetic. Five call sites each composing
 * their own string is five rules again — and it showed: {@code CartService.addItems} produced
 * the truncated {@code "Only 2 Left for "}, naming neither the product nor anything after the
 * preposition, which the S8 suite pinned as a {@code KNOWN BUG}. One phrasing, built from the
 * quantity and the title the aggregate already knows, fixes that by construction.
 *
 * @see DomainRuleViolationException for why this is a 400 rather than a 409
 */
@Getter
public class InsufficientStockException extends DomainRuleViolationException {

    private final String title;
    private final Quantity available;
    private final Quantity requested;

    private InsufficientStockException(String title, Quantity available, Quantity requested) {
        super(describe(title, available));
        this.title = title;
        this.available = available;
        this.requested = requested;
    }

    /**
     * The single stock check. Silent when {@code available} covers {@code requested}.
     *
     * @throws InsufficientStockException when it does not
     */
    public static void requireAtLeast(Quantity available, Quantity requested, String title) {
        if (available.isLessThan(requested)) {
            throw new InsufficientStockException(title, available, requested);
        }
    }

    /**
     * Out of stock reads differently from short of stock — "0 left" is technically accurate
     * and useless. Both phrasings come from the messages the cart already produced, so the
     * storefront's error toast says very nearly what it said before; what is new is that
     * every path now says it, including the two that used to omit the product's name.
     */
    private static String describe(String title, Quantity available) {
        String named = title == null || title.isBlank() ? "This item" : "\"" + title + "\"";
        return available.isZero()
                ? named + " is out of stock!"
                : "Only " + available + " left for " + named + "!";
    }
}
