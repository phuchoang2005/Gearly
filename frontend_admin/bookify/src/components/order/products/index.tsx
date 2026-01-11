import React from "react";
import { Avatar, Table, Typography, theme } from "antd";
import { Flex } from "antd";
import { NumberField } from "@refinedev/antd";
import type { IOrder, IOrderItem } from "../../../interfaces";
import { getUniqueListWithCount } from "../../../utils";

type Props = {
    order?: IOrder;
};

export const OrderItemsTable: React.FC<Props> = ({ order }) => {
    const { token } = theme.useToken();

    // Aggregate duplicates by bookId
    const items: IOrderItem[] = order?.items ?? [];
    const uniqueItems = getUniqueListWithCount<IOrderItem>({
        list: items,
        field: "bookId",
    });

    // Compute grand total
    const grandTotal = uniqueItems.reduce(
        (sum, item) => sum + item.count * item.price,
        0,
    );

    return (
        <Table
            dataSource={uniqueItems}
            loading={!order}
            pagination={false}
            rowKey="bookId"
            footer={() => (
                <Flex justify="end" style={{ padding: "8px 16px" }}>
                    <Typography.Text style={{ marginRight: 16 }}>Total</Typography.Text>
                    <NumberField
                        value={grandTotal}
                        options={{ style: "currency", currency: "USD" }}
                    />
                </Flex>
            )}
            scroll={{ x: true }}
        >
            {/* Book title & avatar */}
            <Table.Column<typeof uniqueItems[number]>
                title="Book"
                dataIndex="title"
                key="title"
                render={(_, record) => (
                    <Flex align="center" gap={12}>
                        <Avatar
                            shape="square"
                            style={{ backgroundColor: token.colorPrimaryBg }}
                        >
                            {record.title.charAt(0).toUpperCase()}
                        </Avatar>
                        <Typography.Text>{record.title}</Typography.Text>
                    </Flex>
                )}
            />

            {/* Quantity */}
            <Table.Column
                title="Quantity"
                dataIndex="count"
                key="count"
                align="right"
            />

            {/* Unit Price */}
            <Table.Column
                title="Price"
                dataIndex="price"
                key="price"
                align="right"
                render={(value: number) => (
                    <NumberField
                        value={value}
                        options={{ style: "currency", currency: "USD" }}
                    />
                )}
            />

            {/* Line Total */}
            <Table.Column<typeof uniqueItems[number]>
                title="Total"
                key="total"
                align="right"
                render={(_, record) => (
                    <NumberField
                        value={record.count * record.price}
                        options={{ style: "currency", currency: "USD" }}
                    />
                )}
            />
        </Table>
    );
};
