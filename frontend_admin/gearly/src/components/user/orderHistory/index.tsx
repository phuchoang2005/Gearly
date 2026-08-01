import React from "react";
import { NumberField, useTable, DateField } from "@refinedev/antd";
import type { IUser, IOrder, IOrderFilterVariables } from "../../../interfaces";
import { HttpError, useNavigation, useTranslate } from "@refinedev/core";
import { Table, Typography } from "antd";
import { OrderStatus, OrderTableColumnItems } from "../../order";

type Props = {
    user?: IUser;
};

export const UserOrderHistory: React.FC<Props> = ({ user }) => {
    const t = useTranslate();
    const { show } = useNavigation();

    const { tableProps } = useTable<IOrder, HttpError, IOrderFilterVariables>({
        resource: "orders",
        initialSorter: [
            {
                field: "createdAt",
                order: "desc",
            },
        ],
        permanentFilter: user
            ? [
                {
                    field: "userId",     // match your Order model’s userId field
                    operator: "eq",
                    value: user.id,
                },
            ]
            : [],
        initialPageSize: 4,
        queryOptions: {
            enabled: Boolean(user),
        },
        syncWithLocation: false,
    });

    return (
        <Table
            {...tableProps}
            rowKey="id"
            onRow={(record) => ({
                onClick: () => {
                    show("orders", record.id);
                },
            })}
            pagination={{
                ...tableProps.pagination,
                hideOnSinglePage: true,
            }}
        >
            {/* Order ID */}
            <Table.Column
                title={`${t("orders.order", "Order")} #`}
                dataIndex="id"
                key="id"
                render={(value: string) => (
                    <Typography.Text style={{ whiteSpace: "nowrap" }}>
                        #{value}
                    </Typography.Text>
                )}
            />

            {/* Status */}
            <Table.Column
                key="orderStatus"
                dataIndex="orderStatus"
                title={t("orders.fields.status", "Status")}
                render={(status) => <OrderStatus status={status} />}
            />

            {/* Items */}
            <Table.Column<IOrder>
                key="items"
                dataIndex="items"
                title={t("orders.fields.items", "Items")}
                render={(_, record) => <OrderTableColumnItems order={record} />}
            />

            {/* Total Amount */}
            <Table.Column<IOrder>
                dataIndex="totalAmount"
                align="right"
                title={t("orders.fields.totalAmount", "Total Amount")}
                render={(amount: number) => (
                    <NumberField
                        value={amount}
                        style={{ whiteSpace: "nowrap" }}
                        options={{ style: "currency", currency: "USD" }}
                    />
                )}
            />

            {/* Date */}
            <Table.Column
                key="addedAt"
                dataIndex="addedAt"
                title={t("orders.fields.createdAt", "Date")}
                render={(value: string) => (
                    <DateField value={value} format="LLL" />
                )}
                sorter
            />
        </Table>
    );
};
