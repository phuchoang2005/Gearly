import React from "react";
import {
    useTranslate,
    useExport,
    useNavigation,
    HttpError,
    getDefaultFilter,
    useGo,
} from "@refinedev/core";
import {
    List,
    useTable,
    getDefaultSortOrder,
    DateField,
    NumberField,
    FilterDropdown,
    ExportButton,
    CreateButton,
} from "@refinedev/antd";
import { Table, Typography, Select, InputNumber, theme } from "antd";
import { SearchOutlined } from "@ant-design/icons";
import {
    OrderActions,
    OrderStatus,
    OrderTableColumnItems,
    PaginationTotal,
} from "../../components";
import type { IOrder } from "../../interfaces";
import { ImportCsvOrdersButton } from "../../components/importCsv";
import { useLocation } from "react-router";

export const OrderList: React.FC = () => {
    const t = useTranslate();
    const go = useGo();
    const { pathname } = useLocation();
    const { createUrl, show } = useNavigation();
    const { token } = theme.useToken();

    const { tableProps, sorters, filters } = useTable<IOrder, HttpError>({
        resource: "orders",
        initialSorter: [{ field: "addedAt", order: "desc" }],
        filters: {
            initial: [{ field: "status", operator: "eq", value: "" }],
        },
        syncWithLocation: true,
    });

    const { isLoading, triggerExport } = useExport<IOrder>({
        resource: "orders",
        sorters,
        filters,
        pageSize: 50,
        maxItemCount: 1000,
        mapData: (order) => ({
            id: order.id,
            orderStatus: order.orderStatus,
            receiver: `${order.shippingInformation?.firstName ?? ""} ${order.shippingInformation?.lastName ?? ""}`.trim(),
            quantity: order.items?.reduce((sum, item) => sum + item.quantity, 0) || 0,
            paymentMethod: order.payment?.method?.toUpperCase() ?? "",
            totalAmount: order.totalAmount,
            addedAt: order.addedAt,
            doneAt: order.doneAt ?? "",
        }),
    });

    const getLastTransactionStatus = (record: IOrder): string | undefined => {
        return record?.payment?.transactions?.[record.payment.transactions.length - 1]?.status;
    };

    const getTotalQuantity = (record: IOrder): number => {
        return record.items?.reduce((sum, item) => sum + item.quantity, 0) || 0;
    };

    return (
        <List
            headerButtons={() => (
                <>
                    <CreateButton
                        key="create"
                        size="large"
                        onClick={() =>
                            go({
                                to: `${createUrl("orders")}`,
                                query: { to: pathname },
                                options: { keepQuery: true },
                                type: "replace",
                            })
                        }
                    >
                        {t("orders.actions.add", "Add")}
                    </CreateButton>
                    <ImportCsvOrdersButton />
                    <ExportButton onClick={triggerExport} loading={isLoading} />
                </>
            )}
        >
            <Table
                {...tableProps}
                rowKey="id"
                style={{ cursor: "pointer" }}
                onRow={(record) => ({
                    onClick: () => show("orders", record.id),
                })}
                pagination={{
                    ...tableProps.pagination,
                    showTotal: (total) => (
                        <PaginationTotal total={total} entityName="orders" />
                    ),
                }}
            >
                {/* ID */}
                <Table.Column
                    key="id"
                    dataIndex="id"
                    title={t("orders.fields.id", "Order ID")}
                    render={(val: string) => <Typography.Text>#{val}</Typography.Text>}
                    sorter
                    defaultSortOrder={getDefaultSortOrder("id", sorters)}
                    filterIcon={(filtered) => (
                        <SearchOutlined
                            style={{ color: filtered ? token.colorPrimary : undefined }}
                        />
                    )}
                    defaultFilteredValue={getDefaultFilter("id", filters, "eq")}
                    filterDropdown={(props) => (
                        <FilterDropdown {...props}>
                            <InputNumber
                                addonBefore="#"
                                style={{ width: "100%" }}
                                placeholder={t("orders.filter.id.placeholder", "Filter by ID")}
                            />
                        </FilterDropdown>
                    )}
                />

                {/* Status */}
                <Table.Column
                    key="orderStatus"
                    dataIndex="orderStatus"
                    title={t("orders.fields.orderStatus", "Status")}
                    render={(status: string) => <OrderStatus status={status} />}
                    sorter
                    defaultSortOrder={getDefaultSortOrder("orderStatus", sorters)}
                    filterIcon={(filtered) => (
                        <SearchOutlined
                            style={{ color: filtered ? token.colorPrimary : undefined }}
                        />
                    )}
                    defaultFilteredValue={getDefaultFilter("orderStatus", filters, "eq")}
                    filterDropdown={(props) => (
                        <FilterDropdown {...props}>
                            <Select
                                style={{ width: 200 }}
                                allowClear
                                placeholder={t("orders.filter.status.placeholder", "Filter status")}
                            >
                                {[
                                    "PENDING",
                                    "PROCESSING",
                                    "SHIPPED",
                                    "DELIVERED",
                                    "COMPLETED",
                                    "CANCELLED",
                                    "REFUNDED",
                                ].map((status) => (
                                    <Select.Option key={status} value={status}>
                                        {t(`enum.orderStatuses.${status}`, status)}
                                    </Select.Option>
                                ))}
                            </Select>
                        </FilterDropdown>
                    )}
                />

                {/* Receiver */}
                <Table.Column<IOrder>
                    key="receiver"
                    title={t("orders.fields.receiver", "Receiver")}
                    render={(record) => {
                        const info = record.shippingInformation;
                        return `${info?.firstName ?? ""} ${info?.lastName ?? ""}`.trim();
                    }}
                />

                {/* Quantity */}
                <Table.Column<IOrder>
                    key="quantity"
                    title={t("orders.fields.quantity", "Qty")}
                    render={(record) => getTotalQuantity(record)}
                />

                {/* Payment Method */}
                <Table.Column<IOrder>
                    key="paymentMethod"
                    title={t("orders.fields.paymentMethod", "Payment")}
                    dataIndex={["payment", "method"]}
                    render={(val) => val?.toUpperCase()}
                />

                {/* Items */}
                <Table.Column<IOrder>
                    key="items"
                    dataIndex="items"
                    title={t("orders.fields.items", "Items")}
                    render={(items, record) => <OrderTableColumnItems order={record} />}
                />

                {/* Total Amount */}
                <Table.Column
                    key="totalAmount"
                    dataIndex="totalAmount"
                    align="right"
                    title={t("orders.fields.totalAmount", "Total Amount")}
                    sorter
                    defaultSortOrder={getDefaultSortOrder("totalAmount", sorters)}
                    render={(val: number) => (
                        <NumberField
                            value={val}
                            options={{ style: "currency", currency: "USD" }}
                        />
                    )}
                />

                {/* Done At */}
                <Table.Column
                    key="doneAt"
                    dataIndex="doneAt"
                    title={t("orders.fields.doneAt", "Completed At")}
                    render={(value: string) => <DateField value={value} format="LLL" />}
                    sorter
                    defaultSortOrder={getDefaultSortOrder("doneAt", sorters)}
                />

                {/* Added At */}
                <Table.Column
                    key="addedAt"
                    dataIndex="addedAt"
                    title={t("orders.fields.addedAt", "Added At")}
                    render={(value: string) => <DateField value={value} format="LLL" />}
                    sorter
                    defaultSortOrder={getDefaultSortOrder("addedAt", sorters)}
                />

                {/* Actions */}
                <Table.Column<IOrder>
                    fixed="right"
                    title={t("table.actions", "Actions")}
                    key="actions"
                    render={(_, record) => <OrderActions record={record} />}
                />
            </Table>
        </List>
    );
};
