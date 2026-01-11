export function calcCartTotals(items) {
    const itemsCount = items?.reduce((sum, i) => sum + i.quantity, 0) || 0;
    const subtotal = items?.reduce((sum, i) => sum + i.price * i.quantity, 0) || 0;
    const shipping = subtotal > 30 ? 0 : 15;
    const taxes = subtotal * 0.08;
    const discount = 0;
    const total = subtotal + taxes + shipping - discount;
    return { itemsCount, subtotal, shipping, taxes, discount, total };
}